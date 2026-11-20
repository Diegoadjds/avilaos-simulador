package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.hardware.Disco;
import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.GeneradorId;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.modelo.TipoOperacionES;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.persistencia.EscenarioArchivos;
import com.mycompany.avilaos.persistencia.EscenarioProcesos;
import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.so.FabricaProcesos;
import com.mycompany.avilaos.so.GestorES;
import com.mycompany.avilaos.so.MetricasDisco;
import com.mycompany.avilaos.so.PlanificadorDisco;
import com.mycompany.avilaos.so.RegistroEventos;
import com.mycompany.avilaos.so.SistemaArchivos;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * ÁvilaOS completo: reloj global + computador + disco + sistema de archivos + log. Es la "fachada" que usa la
 * interfaz: la interfaz observa y le da órdenes, pero no contiene lógica. Las reglas de cada modo de usuario
 * (administrador / usuario) se aplican aquí, no en las ventanas.
 *
 * El reloj solo arranca cuando se cargaron los DOS archivos JSON: el del sistema de archivos (que inicializa el
 * disco) y el de procesos (que define la configuración y los procesos).
 */
public class SistemaAvilaOS {

    private final ConfiguracionSistema configuracion;
    private final RelojGlobal reloj;
    private final RegistroEventos registro = new RegistroEventos();
    private final Disco disco;
    private final PlanificadorDisco planificadorDisco;
    private final SistemaArchivos archivos;
    private final GestorES gestorES;
    private final Computador computador;
    private final SesionUsuario sesion = new SesionUsuario();

    private volatile boolean archivosCargados;
    private volatile boolean procesosCargados;

    /** @throws IllegalArgumentException si la configuración no es válida */
    public SistemaAvilaOS(ConfiguracionSistema configuracion) {
        String error = configuracion.validar();
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        GeneradorId.reiniciar();
        SolicitudDisco.reiniciarIds();
        this.configuracion = configuracion;
        this.reloj = new RelojGlobal(configuracion.getDuracionCicloMs());
        this.disco = new Disco(configuracion.getBloquesDisco(), configuracion.getBloquesSwap(),
                configuracion.getCabezalInicial(), configuracion.getVelocidadCabezal());
        this.planificadorDisco = new PlanificadorDisco(configuracion.getPoliticaDisco(), configuracion.getBloquesDisco(),
                disco.getMapa().getTotal(), registro);
        this.planificadorDisco.setVentanaAgrupacion(configuracion.getVentanaAgrupacion());
        this.disco.setControlador(planificadorDisco);
        this.archivos = new SistemaArchivos(disco.getMapa());
        this.gestorES = new GestorES(archivos, planificadorDisco);
        this.computador = new Computador(1, configuracion.getRam(), configuracion.getPoliticaCpu(),
                configuracion.getQuantum(), gestorES, registro);
        // El computador y el disco son dos componentes del reloj: cada uno corre en su propio hilo.
        reloj.registrar(computador);
        reloj.registrar(disco);
    }

    // =====================================================================
    //  Carga de los dos JSON (antes de iniciar el reloj)
    // =====================================================================

    /** Construye el árbol de directorios y archivos del JSON sobre el disco. Solo antes de iniciar el reloj. */
    public synchronized void cargarArchivos(EscenarioArchivos escenario) throws ErrorEscenario {
        exigirSinIniciar("cargar el sistema de archivos");
        if (archivosCargados) {
            throw new ErrorEscenario("El sistema de archivos ya fue cargado en esta simulación");
        }
        if (escenario.getTotalBloques() != configuracion.getBloquesDisco()) {
            throw new ErrorEscenario("El disco de esta simulación tiene " + configuracion.getBloquesDisco()
                    + " bloques y el archivo define " + escenario.getTotalBloques());
        }
        try {
            // Los directorios se crean antes que su contenido: se recorre por niveles de profundidad.
            EscenarioArchivos.Entrada[] entradas = escenario.getEntradas();
            int maxProfundidad = 1;
            for (EscenarioArchivos.Entrada e : entradas) {
                maxProfundidad = Math.max(maxProfundidad, profundidad(e.ruta()));
            }
            for (int nivel = 1; nivel <= maxProfundidad; nivel++) {
                for (EscenarioArchivos.Entrada e : entradas) {
                    if (profundidad(e.ruta()) != nivel) {
                        continue;
                    }
                    if (e.tipo() == com.mycompany.avilaos.modelo.TipoNodo.DIRECTORIO) {
                        archivos.cargarDirectorio(e.ruta(), e.dueno(), e.permiso(), e.creadoPor());
                    } else {
                        archivos.cargarArchivo(e.ruta(), e.primerBloque(), e.bloques(), e.cadena(), e.dueno(),
                                e.permiso(), e.creadoPor());
                    }
                }
            }
        } catch (ErrorOperacion e) {
            throw new ErrorEscenario("Sistema de archivos inválido: " + e.getMessage());
        }
        archivosCargados = true;
        registro.registrar(0, "Sistema de archivos cargado: " + archivos.preorden().length + " elementos, disco de "
                + configuracion.getBloquesDisco() + " bloques, cabezal en " + disco.getCabezal());
    }

    private static int profundidad(String ruta) {
        int n = 0;
        for (int i = 0; i < ruta.length(); i++) {
            if (ruta.charAt(i) == '/') {
                n++;
            }
        }
        return n;
    }

    /** Crea los procesos del JSON en el computador. Solo antes de iniciar el reloj. */
    public synchronized void cargarProcesos(EscenarioProcesos escenario) throws ErrorEscenario {
        exigirSinIniciar("cargar los procesos");
        if (procesosCargados) {
            throw new ErrorEscenario("Los procesos ya fueron cargados en esta simulación");
        }
        for (EspecificacionProceso esp : escenario.getProcesos()) {
            try {
                crearProceso(esp);
            } catch (ErrorOperacion e) {
                throw new ErrorEscenario("Proceso " + esp.nombre() + ": " + e.getMessage());
            }
        }
        procesosCargados = true;
        registro.registrar(0, "Procesos cargados: " + escenario.getProcesos().length);
    }

    private void exigirSinIniciar(String accion) throws ErrorEscenario {
        if (reloj.isCorriendo() || reloj.getCicloActual() > 0) {
            throw new ErrorEscenario("No se puede " + accion + " con la simulación en marcha: use Reiniciar");
        }
    }

    public boolean estaListoParaIniciar() {
        return archivosCargados && procesosCargados;
    }

    // =====================================================================
    //  Creación de procesos
    // =====================================================================

    /** Crea el proceso (sin revisar el modo de usuario; lo usan la carga de JSON y las operaciones ya autorizadas). */
    public PCB crearProceso(EspecificacionProceso esp) throws ErrorOperacion {
        String error = esp.validar();
        if (error != null) {
            throw new ErrorOperacion(error);
        }
        PCB pcb = FabricaProcesos.crear(esp);
        if (!computador.recibirProceso(pcb)) {
            throw new ErrorOperacion("El proceso necesita " + esp.memoria() + " de memoria y la RAM total es "
                    + computador.getRam().getTotal());
        }
        return pcb;
    }

    /** Proceso CPU bound o I/O bound creado desde la interfaz: solo el administrador gestiona procesos. */
    public PCB crearProcesoGeneral(EspecificacionProceso esp) throws ErrorOperacion {
        String motivo = sesion.motivoSiNoPuedeAdministrar("crear procesos que no sean de E/S sobre archivos propios");
        if (motivo != null) {
            throw new ErrorOperacion(motivo);
        }
        if (esp.tipo() == TipoProceso.ES_ARCHIVO) {
            throw new ErrorOperacion("Use crearProcesoArchivo para los procesos de E/S de archivos");
        }
        return crearProceso(esp);
    }

    /**
     * Proceso de E/S de archivos creado desde la interfaz, a nombre del usuario de la sesión. Si el modo de usuario
     * no permite la operación se rechaza con la explicación; nada se interrumpe.
     */
    public PCB crearProcesoArchivo(String nombre, TipoOperacionES operacion, String ruta, int tamanoBloques,
            String nuevoNombre, Permiso permiso, int memoria, int prioridad, int deadline, int instrucciones)
            throws ErrorOperacion {
        return crearProcesoArchivo(nombre, operacion, ruta, tamanoBloques, nuevoNombre, permiso, null, memoria,
                prioridad, deadline, instrucciones);
    }

    /** Igual, indicando el dueño del elemento que se crea (solo el administrador puede elegir uno distinto de sí). */
    public PCB crearProcesoArchivo(String nombre, TipoOperacionES operacion, String ruta, int tamanoBloques,
            String nuevoNombre, Permiso permiso, String dueno, int memoria, int prioridad, int deadline,
            int instrucciones) throws ErrorOperacion {
        EspecificacionProceso esp = EspecificacionProceso.archivo(nombre, instrucciones, memoria, prioridad, deadline,
                operacion, ruta, tamanoBloques, nuevoNombre, sesion.getUsuario(), permiso, dueno);
        String error = esp.validar();
        if (error != null) {
            throw new ErrorOperacion(error);
        }
        String rechazo = archivos.verificarPermiso(operacion, ruta, nuevoNombre, sesion.getUsuario(), esp.dueno());
        if (rechazo != null) {
            registro.registrar(reloj.getCicloActual(), "Operación rechazada (" + operacion + " " + ruta + " por "
                    + sesion.getUsuario() + "): " + rechazo);
            throw new ErrorOperacion(rechazo);
        }
        return crearProceso(esp);
    }

    // =====================================================================
    //  Políticas (solo administrador)
    // =====================================================================

    public void cambiarPoliticaDisco(PoliticaPlanificacionDisco politica) throws ErrorOperacion {
        String motivo = sesion.motivoSiNoPuedeAdministrar("cambiar la política de planificación de disco");
        if (motivo != null) {
            throw new ErrorOperacion(motivo);
        }
        planificadorDisco.cambiarPolitica(politica);
    }

    public void cambiarPoliticaCpu(PoliticaPlanificacion politica) throws ErrorOperacion {
        String motivo = sesion.motivoSiNoPuedeAdministrar("cambiar la política de planificación de CPU");
        if (motivo != null) {
            throw new ErrorOperacion(motivo);
        }
        computador.cambiarPolitica(politica);
    }

    public void setQuantum(int quantum) {
        configuracion.setQuantum(quantum);
        computador.setQuantum(quantum);
    }

    public void setVelocidadCabezal(int posicionesPorCiclo) {
        configuracion.setVelocidadCabezal(posicionesPorCiclo);
        disco.setVelocidad(posicionesPorCiclo);
    }

    public void setVentanaAgrupacion(int ciclos) {
        configuracion.setVentanaAgrupacion(ciclos);
        planificadorDisco.setVentanaAgrupacion(ciclos);
    }

    // =====================================================================
    //  Control del reloj
    // =====================================================================

    private void exigirCargados() {
        if (!estaListoParaIniciar()) {
            throw new IllegalStateException("Antes de iniciar el reloj hay que cargar los DOS archivos JSON: "
                    + (archivosCargados ? "" : "el del sistema de archivos") + (!archivosCargados && !procesosCargados ? " y " : "")
                    + (procesosCargados ? "" : "el de procesos"));
        }
    }

    /** @throws IllegalStateException si falta cargar alguno de los dos JSON */
    public void iniciar() {
        exigirCargados();
        reloj.iniciar();
    }

    /** @throws IllegalStateException si falta cargar alguno de los dos JSON */
    public void iniciarPausado() {
        exigirCargados();
        reloj.iniciar(true);
    }

    public void pausar() { reloj.pausar(); }

    public void reanudar() { reloj.reanudar(); }

    public void avanzarUnCiclo() { reloj.avanzarUnCiclo(); }

    public void detener() {
        if (!reloj.isCorriendo()) {
            reloj.iniciar(true); // así existen los hilos de los componentes y detener() los apaga
        }
        reloj.detener();
    }

    public void setDuracionCicloMs(int ms) {
        configuracion.setDuracionCicloMs(ms);
        reloj.setDuracionCicloMs(ms);
    }

    // =====================================================================
    //  Persistencia del sistema de archivos
    // =====================================================================

    /** Foto del sistema de archivos actual (para guardarla en JSON o para reiniciar con el disco modificado). */
    public EscenarioArchivos capturarArchivos() {
        return EscenarioArchivos.capturar(archivos, configuracion.getBloquesDisco(), disco.getCabezal(),
                disco.getVelocidad());
    }

    // =====================================================================
    //  Consultas
    // =====================================================================

    public ConfiguracionSistema getConfiguracion() { return configuracion; }
    public RelojGlobal getReloj() { return reloj; }
    public RegistroEventos getRegistro() { return registro; }
    public Disco getDisco() { return disco; }
    public PlanificadorDisco getPlanificadorDisco() { return planificadorDisco; }
    public MetricasDisco getMetricasDisco() { return planificadorDisco.getMetricas(); }
    public SistemaArchivos getArchivos() { return archivos; }
    public Computador getComputador() { return computador; }
    public SesionUsuario getSesion() { return sesion; }
    public boolean isArchivosCargados() { return archivosCargados; }
    public boolean isProcesosCargados() { return procesosCargados; }
}
