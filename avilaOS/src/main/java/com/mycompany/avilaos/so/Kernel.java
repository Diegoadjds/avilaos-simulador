package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.hardware.CPU;
import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.AccionInstruccion;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.MotivoBloqueo;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Núcleo del sistema operativo del computador. Es el que DECIDE: admite procesos según la memoria, elige
 * quién ejecuta, bloquea, desbloquea y termina. Maneja el modelo de siete estados: además de las colas del
 * Proyecto 1 tiene las de procesos suspendidos (Listo/Suspendido y Bloqueado/Suspendido), que administra
 * junto con el planificador a mediano plazo.
 *
 * Cada ciclo el Computador llama, en este orden:
 *   faseInicio()  ->  cpu.alCiclo()  ->  faseFin()
 */
public class Kernel implements ContextoEjecucion {
    private PCB ultimoEjecutado;
    private String ultimaInstruccionTexto = "";
    private ModoCPU ultimoModoEjecutado = ModoCPU.SISTEMA_OPERATIVO;
    private long cicloUltimoEjecutado;

    private final int idComputador;
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private final RegistroEventos registro;
    private final GestorES gestorES;
    private final MetricasComputador metricas = new MetricasComputador();
    private final MedianoPlazo medianoPlazo;

    // Colas compartidas entre hilos (la GUI las lee)
    private final ListaSegura<PCB> nuevos = new ListaSegura<>();
    private final ListaSegura<PCB> listos = new ListaSegura<>();
    private final ListaSegura<PCB> bloqueados = new ListaSegura<>();
    private final ListaSegura<PCB> listosSuspendidos = new ListaSegura<>();
    private final ListaSegura<PCB> bloqueadosSuspendidos = new ListaSegura<>();
    private final ListaSegura<PCB> terminados = new ListaSegura<>();

    private final Planificador planificador;
    private volatile PoliticaPlanificacion tipoPolitica;
    private volatile int quantum;
    private volatile long cicloActual;

    public Kernel(int idComputador, MemoriaPrincipal ram, CPU cpu, RegistroEventos registro, GestorES gestorES,
            PoliticaPlanificacion politicaInicial, int quantum) {
        this.idComputador = idComputador;
        this.ram = ram;
        this.cpu = cpu;
        this.registro = registro;
        this.gestorES = gestorES;
        this.tipoPolitica = politicaInicial;
        this.quantum = quantum;
        this.planificador = new Planificador(politicaInicial.crear(quantum), listos);
        this.medianoPlazo = new MedianoPlazo(this, gestorES);
        this.cpu.setContexto(this);
    }

    // =====================================================================
    //  Ciclo de reloj
    // =====================================================================

    /** Antes de que el CPU ejecute: respuestas del disco, mediano plazo, admisión de nuevos y planificación. */
    public void faseInicio(long ciclo) {
        cicloActual = ciclo;
        procesarSolicitudesCompletadas();
        avanzarBloqueosPorTiempo();
        medianoPlazo.reactivar();
        admitirNuevos();
        medianoPlazo.suspenderParaAdmitir();
        planificar();
    }

    /** Después de que el CPU ejecutó: resultado de la instrucción, deadlines y métricas. */
    public void faseFin(long ciclo) {
        PCB p = cpu.getProcesoActual();
        ResultadoInstruccion r = cpu.getUltimoResultado();
        metricas.registrarCiclo(r != null);

        if (p != null && r != null) {
            // Foto para la interfaz: el proceso puede salir de la CPU (bloqueo o fin) en este mismo ciclo
            ultimoEjecutado = p;
            ultimaInstruccionTexto = p.getComportamiento().ultimaInstruccion(p);
            ultimoModoEjecutado = r.getModo();
            cicloUltimoEjecutado = ciclo;
            if (p.getInstruccionesRestantes() <= 0 || r.getAccion() == AccionInstruccion.TERMINAR) {
                terminar(p, false);
            } else if (r.getAccion() == AccionInstruccion.BLOQUEAR) {
                bloquear(p, r);
            }
        }
        avanzarDeadlines();
    }

    // =====================================================================
    //  Llegada de procesos
    // =====================================================================

    /** Recibe un proceso nuevo. Devuelve false si nunca podría caber en la RAM. */
    public boolean recibirProceso(PCB pcb) {
        if (pcb.getMemoria() > ram.getTotal()) {
            registrar("rechaza " + pcb + ": necesita " + pcb.getMemoria()
                    + " y la RAM total es " + ram.getTotal());
            return false;
        }
        pcb.setEstado(EstadoProceso.NUEVO);
        pcb.setCicloLlegada(cicloActual);
        nuevos.agregarAlFinal(pcb);
        registrar("recibe " + pcb + " en la cola de nuevos (memoria " + pcb.getMemoria() + ")");
        return true;
    }

    // =====================================================================
    //  Respuestas del disco
    // =====================================================================

    /** Atiende, en orden, las solicitudes que el disco terminó en ciclos anteriores. */
    private void procesarSolicitudesCompletadas() {
        for (SolicitudDisco s : gestorES.getPlanificadorDisco().sacarCompletadas(cicloActual)) {
            if (s.isCancelada()) {
                continue; // su proceso murió mientras el disco la atendía
            }
            PCB p = s.getProceso();
            switch (s.getOperacion()) {
                case SUSPENDER -> completarSuspension(p);
                case REACTIVAR -> completarReactivacion(p);
                default -> completarArchivo(p, s);
            }
        }
    }

    /** El disco terminó una operación de archivo: se hace efectiva y el proceso despierta (Listo o Listo/Suspendido). */
    private void completarArchivo(PCB p, SolicitudDisco s) {
        gestorES.confirmarArchivo(s);
        p.setSolicitudActual(null);
        registrar("el disco terminó la operación " + s.getOperacion() + " de " + p);
        if (p.getEstado() == EstadoProceso.BLOQUEADO) {
            if (p.isSuspendiendo()) {
                p.setEventoDuranteSuspension(true); // se está escribiendo en el swap: queda Listo/Suspendido al terminar
            } else {
                despertar(p);
            }
        } else if (p.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            despertarSuspendido(p);
        }
    }

    private void completarSuspension(PCB p) {
        if (!bloqueados.eliminar(p)) {
            return;
        }
        ram.liberar(p.getMemoria());
        p.setSuspendiendo(false);
        p.setSolicitudSwap(null);
        p.sumarSuspension();
        medianoPlazo.getMetricasDisco().registrarSuspension();
        if (p.isEventoDuranteSuspension()) {
            p.setEventoDuranteSuspension(false);
            registrarFinBloqueo(p);
            p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            listosSuspendidos.agregarAlFinal(p);
            registrar("suspende a " + p + " (swap en el bloque " + p.getPrimerBloqueSwap()
                    + "): su evento ya ocurrió, queda Listo/Suspendido (RAM " + ram.getUsada() + "/" + ram.getTotal() + ")");
        } else {
            p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            bloqueadosSuspendidos.agregarAlFinal(p);
            registrar("suspende a " + p + " (swap en el bloque " + p.getPrimerBloqueSwap()
                    + "): queda Bloqueado/Suspendido (RAM " + ram.getUsada() + "/" + ram.getTotal() + ")");
        }
    }

    private void completarReactivacion(PCB p) {
        if (!listosSuspendidos.eliminar(p)) {
            return;
        }
        p.setReactivando(false);
        gestorES.liberarSwap(p);
        medianoPlazo.getMetricasDisco().registrarReactivacion();
        p.setEstado(EstadoProceso.LISTO);
        planificador.agregarListo(p);
        registrar("reactiva a " + p + ": vuelve a la RAM y pasa a Listo (RAM " + ram.getUsada() + "/" + ram.getTotal() + ")");
    }

    // =====================================================================
    //  Pasos internos
    // =====================================================================

    /** Cuenta los ciclos de los bloqueos por tiempo (E/S simulada) y despierta a los que cumplieron. */
    private void avanzarBloqueosPorTiempo() {
        for (PCB p : bloqueados.aArreglo(new PCB[0])) {
            if (p.getMotivoBloqueo().esPorTiempo() && !p.isEventoDuranteSuspension()) {
                if (p.getCiclosBloqueoRestantes() <= 0) {
                    if (p.isSuspendiendo()) {
                        p.setEventoDuranteSuspension(true);
                    } else {
                        despertar(p);
                    }
                } else {
                    p.setCiclosBloqueoRestantes(p.getCiclosBloqueoRestantes() - 1);
                }
            }
        }
        for (PCB p : bloqueadosSuspendidos.aArreglo(new PCB[0])) {
            if (p.getMotivoBloqueo().esPorTiempo()) {
                if (p.getCiclosBloqueoRestantes() <= 0) {
                    despertarSuspendido(p);
                } else {
                    p.setCiclosBloqueoRestantes(p.getCiclosBloqueoRestantes() - 1);
                }
            }
        }
    }

    private void despertar(PCB p) {
        if (!bloqueados.eliminar(p)) {
            return; // ya fue terminado (por ejemplo, por deadline)
        }
        registrarFinBloqueo(p);
        registrar(p + " sale de bloqueado (" + p.getMotivoBloqueo() + ") y pasa a listos");
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        p.setEstado(EstadoProceso.LISTO);
        planificador.agregarListo(p);
    }

    /** Su evento ocurrió estando en el swap: pasa de Bloqueado/Suspendido a Listo/Suspendido. */
    private void despertarSuspendido(PCB p) {
        if (!bloqueadosSuspendidos.eliminar(p)) {
            return;
        }
        registrarFinBloqueo(p);
        registrar(p + " termina su espera (" + p.getMotivoBloqueo() + ") estando suspendido: pasa a Listo/Suspendido");
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
        listosSuspendidos.agregarAlFinal(p);
    }

    /** Suma, al proceso, los ciclos que estuvo bloqueado hasta el ciclo actual. Llamar antes de borrar el motivo. */
    private void registrarFinBloqueo(PCB p) {
        p.sumarTiempoBloqueado(cicloActual - p.getCicloInicioBloqueo());
    }

    /** Planificación a largo plazo: Nuevo -> Listo solo si hay memoria suficiente. */
    private void admitirNuevos() {
        for (PCB p : nuevos.aArreglo(new PCB[0])) {
            if (ram.reservar(p.getMemoria())) {
                nuevos.eliminar(p);
                p.setEstado(EstadoProceso.LISTO);
                planificador.agregarListo(p);
                registrar("admite " + p + " (RAM " + ram.getUsada() + "/" + ram.getTotal() + ")");
            }
        }
    }

    /** Planificación a corto plazo: expropia si la política lo pide y asigna CPU si está libre. */
    private void planificar() {
        PCB enCpu = cpu.getProcesoActual();
        if (enCpu != null && planificador.debeExpropiar(enCpu, cpu.getCiclosEnCpu())) {
            cpu.descargar();
            enCpu.setEstado(EstadoProceso.LISTO);
            planificador.agregarListo(enCpu);
            registrar("expropia a " + enCpu + " (" + tipoPolitica + ")");
        }
        if (cpu.getProcesoActual() == null) {
            PCB siguiente = planificador.seleccionarSiguiente();
            if (siguiente != null) {
                siguiente.setEstado(EstadoProceso.EJECUCION);
                if (siguiente.getCicloPrimeraEjecucion() < 0) {
                    siguiente.setCicloPrimeraEjecucion(cicloActual);
                    metricas.registrarRespuesta(cicloActual - siguiente.getCicloLlegada());
                }
                cpu.cargar(siguiente);
                registrar("selecciona el proceso " + siguiente + " [" + tipoPolitica + "]");
            }
        }
    }

    private void bloquear(PCB p, ResultadoInstruccion r) {
        cpu.descargar();
        p.setEstado(EstadoProceso.BLOQUEADO);
        p.setMotivoBloqueo(r.getMotivo());
        p.setCiclosBloqueoRestantes(r.getCiclosBloqueo());
        p.setCicloInicioBloqueo(cicloActual);
        bloqueados.agregarAlFinal(p);
        registrar("bloquea a " + p + ": " + r.getMotivo());
    }

    /** Resta un ciclo al deadline de todos los procesos vivos (también los suspendidos) y termina a los que llegan a 0. */
    private void avanzarDeadlines() {
        PCB[] vivos = todosLosVivos();
        for (PCB p : vivos) {
            if (p.decrementarDeadline() <= 0) {
                terminar(p, true);
            }
        }
    }

    /** Termina un proceso (normal o por deadline), libera su memoria y su swap y revisa la cola de nuevos. */
    private void terminar(PCB p, boolean porDeadline) {
        EstadoProceso previo = p.getEstado();
        switch (previo) {
            case NUEVO -> nuevos.eliminar(p);
            case LISTO -> listos.eliminar(p);
            case EJECUCION -> cpu.descargar();
            case BLOQUEADO -> {
                if (!bloqueados.eliminar(p)) {
                    return;
                }
                registrarFinBloqueo(p);
            }
            case LISTO_SUSPENDIDO -> listosSuspendidos.eliminar(p);
            case BLOQUEADO_SUSPENDIDO -> {
                bloqueadosSuspendidos.eliminar(p);
                registrarFinBloqueo(p);
            }
            default -> {
                return;
            }
        }
        // Si tenía operaciones pendientes en el disco se cancelan y se deshace lo reservado (bloques, marcas, swap).
        gestorES.abortarProceso(p);
        boolean enRam = previo == EstadoProceso.LISTO || previo == EstadoProceso.EJECUCION
                || previo == EstadoProceso.BLOQUEADO || p.isReactivando();
        if (enRam) {
            ram.liberar(p.getMemoria());
        }
        p.setSuspendiendo(false);
        p.setReactivando(false);
        p.setEventoDuranteSuspension(false);
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        p.setEstado(EstadoProceso.TERMINADO);
        p.setCicloFin(cicloActual);
        p.setTerminoPorDeadline(porDeadline);
        terminados.agregarAlFinal(p);
        metricas.registrarTerminacion(p);
        registrar(porDeadline
                ? "termina a " + p + " por DEADLINE vencido"
                : p.getErrorES() != null ? "termina " + p + " con error de E/S: " + p.getErrorES()
                : "termina " + p + " (completado)");
        // Se liberó memoria: si hay procesos esperando, se revisa su admisión en el próximo ciclo.
        admitirNuevos();
    }

    // =====================================================================
    //  Operaciones que usa el planificador a mediano plazo
    // =====================================================================

    /** Pide al disco que escriba al proceso bloqueado en el swap. Devuelve false si no hay espacio de swap. */
    boolean iniciarSuspension(PCB victima, PCB paraAdmitir) {
        SolicitudDisco s = gestorES.solicitarSuspension(victima, cicloActual);
        if (s == null) {
            return false;
        }
        victima.setSuspendiendo(true);
        registrar("Se suspende al proceso " + victima.getEtiqueta() + " para admitir a " + paraAdmitir.getEtiqueta()
                + " (prioridad " + victima.getPrioridad() + ", bloqueado desde el ciclo "
                + victima.getCicloInicioBloqueo() + ", " + victima.getMemoria() + " bloques de swap)");
        return true;
    }

    /** Reserva la RAM del proceso suspendido y pide al disco que lo lea del swap. */
    boolean iniciarReactivacion(PCB p) {
        if (!ram.reservar(p.getMemoria())) {
            return false;
        }
        p.setReactivando(true);
        gestorES.solicitarReactivacion(p, cicloActual);
        registrar("Se reactiva al proceso " + p.getEtiqueta() + " (hay " + ram.getLibre() + " de RAM libre y necesita "
                + p.getMemoria() + ")");
        return true;
    }

    // =====================================================================
    //  Política de planificación (cambiable en ejecución)
    // =====================================================================

    public void cambiarPolitica(PoliticaPlanificacion tipo) {
        this.tipoPolitica = tipo;
        planificador.cambiarPolitica(tipo.crear(quantum));
        registrar("cambia su política de planificación a " + tipo);
    }

    public void setQuantum(int quantum) {
        this.quantum = Math.max(1, quantum);
        planificador.getPolitica().setQuantum(this.quantum);
    }

    // =====================================================================
    //  ContextoEjecucion
    // =====================================================================

    @Override
    public long getCicloActual() { return cicloActual; }

    @Override
    public int getIdComputador() { return idComputador; }

    @Override
    public void registrarEvento(String mensaje) {
        registro.registrar(cicloActual, mensaje);
    }

    @Override
    public SolicitudDisco solicitarArchivo(PCB pcb) throws ErrorOperacion {
        return gestorES.solicitarArchivo(pcb, cicloActual);
    }

    private void registrar(String mensaje) {
        registro.registrar(cicloActual, "Computador " + idComputador + " " + mensaje);
    }

    // =====================================================================
    //  Consultas ("fotos" de las colas, seguras entre hilos)
    // =====================================================================

    public PCB[] getNuevos() { return nuevos.aArreglo(new PCB[0]); }
    public PCB[] getListos() { return listos.aArreglo(new PCB[0]); }
    public PCB[] getBloqueados() { return bloqueados.aArreglo(new PCB[0]); }
    public PCB[] getListosSuspendidos() { return listosSuspendidos.aArreglo(new PCB[0]); }
    public PCB[] getBloqueadosSuspendidos() { return bloqueadosSuspendidos.aArreglo(new PCB[0]); }
    public PCB[] getTerminados() { return terminados.aArreglo(new PCB[0]); }
    public PCB getProcesoEnCpu() { return cpu.getProcesoActual(); }

    /** Último proceso que ejecutó una instrucción (aunque ya haya salido de la CPU), o null si aún ninguno. */
    public PCB getUltimoEjecutado() { return ultimoEjecutado; }
    public String getUltimaInstruccionTexto() { return ultimaInstruccionTexto; }
    public ModoCPU getUltimoModoEjecutado() { return ultimoModoEjecutado; }
    public long getCicloUltimoEjecutado() { return cicloUltimoEjecutado; }
    public ModoCPU getModoCpu() { return cpu.getModo(); }
    public PoliticaPlanificacion getPolitica() { return tipoPolitica; }
    public int getQuantum() { return quantum; }
    public MemoriaPrincipal getRam() { return ram; }
    public MetricasComputador getMetricas() { return metricas; }
    public MedianoPlazo getMedianoPlazo() { return medianoPlazo; }

    /** Procesos que todavía no terminaron, en cualquier cola (y el que está en la CPU). */
    private PCB[] todosLosVivos() {
        PCB[][] colas = {getNuevos(), getListos(), getBloqueados(), getListosSuspendidos(),
            getBloqueadosSuspendidos()};
        PCB enCpu = cpu.getProcesoActual();
        int total = enCpu != null ? 1 : 0;
        for (PCB[] c : colas) {
            total += c.length;
        }
        PCB[] todos = new PCB[total];
        int n = 0;
        for (PCB[] c : colas) {
            for (PCB p : c) {
                todos[n++] = p;
            }
        }
        if (enCpu != null) {
            todos[n++] = enCpu;
        }
        return todos;
    }

    /** Todos los procesos que ha recibido el computador (en cualquier estado). */
    public PCB[] getTodosLosProcesos() {
        PCB[] vivos = todosLosVivos();
        PCB[] fin = getTerminados();
        PCB[] todos = new PCB[vivos.length + fin.length];
        System.arraycopy(vivos, 0, todos, 0, vivos.length);
        System.arraycopy(fin, 0, todos, vivos.length, fin.length);
        return todos;
    }

    public double getEquidad() {
        return Equidad.jain(getTodosLosProcesos(), cicloActual);
    }

    // ---- Acceso a las colas para el planificador a mediano plazo (mismo paquete) ----
    ListaSegura<PCB> colaNuevos() { return nuevos; }
    ListaSegura<PCB> colaBloqueados() { return bloqueados; }
    ListaSegura<PCB> colaListosSuspendidos() { return listosSuspendidos; }
}
