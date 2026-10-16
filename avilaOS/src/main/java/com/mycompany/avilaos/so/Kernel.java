package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.hardware.CPU;
import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.AccionInstruccion;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.GestorDesbloqueo;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.MotivoBloqueo;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Núcleo del sistema operativo de UN computador. Es el que DECIDE: admite procesos
 * según la memoria, elige quién ejecuta, bloquea, desbloquea y termina.
 *
 * Cada ciclo el Computador llama, en este orden:
 *   faseInicio()  ->  cpu.alCiclo()  ->  faseFin()
 */
public class Kernel implements ContextoEjecucion, GestorDesbloqueo {

    private final int idComputador;
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private final RegistroEventos registro;
    private final MetricasComputador metricas = new MetricasComputador();

    // Colas compartidas entre hilos (la GUI las lee, otros computadores pueden despertar procesos)
    private final ListaSegura<PCB> nuevos = new ListaSegura<>();
    private final ListaSegura<PCB> listos = new ListaSegura<>();
    private final ListaSegura<PCB> bloqueados = new ListaSegura<>();
    private final ListaSegura<PCB> terminados = new ListaSegura<>();
    /** Desbloqueos pedidos por semáforos; se aplican al inicio del siguiente ciclo. */
    private final ListaSegura<PCB> pendientesDesbloqueo = new ListaSegura<>();

    private final Planificador planificador;
    private volatile PoliticaPlanificacion tipoPolitica;
    private volatile int quantum;
    private volatile long cicloActual;

    public Kernel(int idComputador, MemoriaPrincipal ram, CPU cpu, RegistroEventos registro,
            PoliticaPlanificacion politicaInicial, int quantum) {
        this.idComputador = idComputador;
        this.ram = ram;
        this.cpu = cpu;
        this.registro = registro;
        this.tipoPolitica = politicaInicial;
        this.quantum = quantum;
        this.planificador = new Planificador(politicaInicial.crear(quantum), listos);
        this.cpu.setContexto(this);
    }

    // =====================================================================
    //  Ciclo de reloj
    // =====================================================================

    /** Antes de que el CPU ejecute: desbloqueos, admisión de nuevos y planificación. */
    public void faseInicio(long ciclo) {
        cicloActual = ciclo;
        procesarDesbloqueos();
        avanzarBloqueosPorTiempo();
        admitirNuevos();
        planificar();
    }

    /** Después de que el CPU ejecutó: resultado de la instrucción, deadlines y métricas. */
    public void faseFin(long ciclo) {
        PCB p = cpu.getProcesoActual();
        ResultadoInstruccion r = cpu.getUltimoResultado();
        metricas.registrarCiclo(r != null);

        if (p != null && r != null) {
            if (p.getInstruccionesRestantes() <= 0 || r.getAccion() == AccionInstruccion.TERMINAR) {
                terminar(p, false);
            } else if (r.getAccion() == AccionInstruccion.BLOQUEAR) {
                bloquear(p, r);
            }
        }
        avanzarDeadlines();
    }

    // =====================================================================
    //  Llegada de procesos (la llama la GUI / el cluster)
    // =====================================================================

    /** Recibe un proceso nuevo. Devuelve false si nunca podría caber en la RAM. */
    public boolean recibirProceso(PCB pcb) {
        if (pcb.getMemoria() > ram.getTotal()) {
            registrar("rechaza " + pcb + ": necesita " + pcb.getMemoria()
                    + " y la RAM total es " + ram.getTotal());
            return false;
        }
        pcb.asignarComputador(idComputador, this);
        pcb.setEstado(EstadoProceso.NUEVO);
        pcb.setCicloLlegada(cicloActual);
        nuevos.agregarAlFinal(pcb);
        registrar("recibe " + pcb + " en la cola de nuevos (memoria " + pcb.getMemoria() + ")");
        return true;
    }

    /** Lo llaman los semáforos (vía PCB.solicitarDesbloqueo) desde cualquier computador. */
    @Override
    public void desbloquear(PCB pcb) {
        pendientesDesbloqueo.agregarAlFinal(pcb);
    }

    // =====================================================================
    //  Pasos internos
    // =====================================================================

    private void procesarDesbloqueos() {
        PCB p;
        while ((p = pendientesDesbloqueo.extraerPrimero()) != null) {
            despertar(p);
        }
    }

    /** Cuenta los ciclos de los bloqueos por tiempo (E/S, red) y despierta a los que cumplieron. */
    private void avanzarBloqueosPorTiempo() {
        for (PCB p : bloqueados.aArreglo(new PCB[0])) {
            if (p.getMotivoBloqueo().esPorTiempo()) {
                if (p.getCiclosBloqueoRestantes() <= 0) {
                    despertar(p);
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
        long bloqueo = cicloActual - p.getCicloInicioBloqueo();
        p.sumarTiempoBloqueado(bloqueo);
        if (p.getMotivoBloqueo().esSemaforo()) {
            metricas.registrarBloqueoSemaforo(bloqueo);
        }
        registrar(p + " sale de bloqueado (" + p.getMotivoBloqueo() + ") y pasa a listos");
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        p.setRecursoBloqueo(null);
        p.setEstado(EstadoProceso.LISTO);
        planificador.agregarListo(p);
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
        p.setRecursoBloqueo(r.getRecurso());
        p.setCiclosBloqueoRestantes(r.getCiclosBloqueo());
        p.setCicloInicioBloqueo(cicloActual);
        bloqueados.agregarAlFinal(p);
        registrar("bloquea a " + p + ": " + r.getMotivo());
    }

    /** Resta un ciclo al deadline de todos los procesos vivos y termina a los que llegan a 0. */
    private void avanzarDeadlines() {
        PCB[] a = nuevos.aArreglo(new PCB[0]);
        PCB[] b = listos.aArreglo(new PCB[0]);
        PCB[] c = bloqueados.aArreglo(new PCB[0]);
        PCB enCpu = cpu.getProcesoActual();
        PCB[] todos = new PCB[a.length + b.length + c.length + 1];
        int n = 0;
        for (PCB p : a) { todos[n++] = p; }
        for (PCB p : b) { todos[n++] = p; }
        for (PCB p : c) { todos[n++] = p; }
        if (enCpu != null) { todos[n++] = enCpu; }
        for (int i = 0; i < n; i++) {
            if (todos[i].decrementarDeadline() <= 0) {
                terminar(todos[i], true);
            }
        }
    }

    /** Termina un proceso (normal o por deadline), libera su memoria y revisa la cola de nuevos. */
    private void terminar(PCB p, boolean porDeadline) {
        EstadoProceso previo = p.getEstado();
        switch (previo) {
            case NUEVO -> nuevos.eliminar(p);
            case LISTO -> listos.eliminar(p);
            case EJECUCION -> cpu.descargar();
            case BLOQUEADO -> {
                if (!bloqueados.eliminar(p)) {
                    return; // lo despertó un semSignal justo ahora; se reintenta el próximo ciclo
                }
                if (p.getRecursoBloqueo() != null) {
                    p.getRecursoBloqueo().cancelarEspera(p);
                }
            }
            default -> {
                return;
            }
        }
        if (previo != EstadoProceso.NUEVO) {
            ram.liberar(p.getMemoria());
        }
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        p.setRecursoBloqueo(null);
        p.setEstado(EstadoProceso.TERMINADO);
        p.setCicloFin(cicloActual);
        p.setTerminoPorDeadline(porDeadline);
        terminados.agregarAlFinal(p);
        metricas.registrarTerminacion(p);
        registrar(porDeadline
                ? "termina a " + p + " por DEADLINE vencido"
                : "termina " + p + " (completado)");
        admitirNuevos(); // se liberó memoria: ¿cabe alguno de los nuevos?
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

    private void registrar(String mensaje) {
        registro.registrar(cicloActual, "Computador " + idComputador + " " + mensaje);
    }

    // =====================================================================
    //  Consultas para la GUI ("fotos" de las colas, seguras entre hilos)
    // =====================================================================

    public PCB[] getNuevos() { return nuevos.aArreglo(new PCB[0]); }
    public PCB[] getListos() { return listos.aArreglo(new PCB[0]); }
    public PCB[] getBloqueados() { return bloqueados.aArreglo(new PCB[0]); }
    public PCB[] getTerminados() { return terminados.aArreglo(new PCB[0]); }
    public PCB getProcesoEnCpu() { return cpu.getProcesoActual(); }
    public ModoCPU getModoCpu() { return cpu.getModo(); }
    public PoliticaPlanificacion getPolitica() { return tipoPolitica; }
    public int getQuantum() { return quantum; }
    public MemoriaPrincipal getRam() { return ram; }
    public MetricasComputador getMetricas() { return metricas; }

    /** Cantidad de procesos vivos en este computador (para el balanceo de carga automático). */
    public int getCarga() {
        return nuevos.tamano() + listos.tamano() + bloqueados.tamano()
                + (cpu.getProcesoActual() != null ? 1 : 0);
    }
}
