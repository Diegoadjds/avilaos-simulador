package com.mycompany.avilaos.modelo;

/**
 * Bloque de control del proceso. Como cada proceso tiene un único hilo (modelo monohilo),
 * el PCB integra al TCB: aquí viven el PC, el MAR y el estado de ejecución.
 *
 * Proyecto 2: además de los datos del Proyecto 1, un proceso de E/S de archivos guarda la
 * operación solicitada y la ruta del archivo, y un proceso suspendido guarda dónde quedó
 * en el área de intercambio. Por eso el PCB es también {@link PropietarioBloques}.
 *
 * Los campos que cambian son volatile porque la GUI los lee desde otro hilo.
 * Convención de prioridad: número MENOR = prioridad MÁS ALTA (1 es la más alta).
 */
public class PCB implements PropietarioBloques {

    // ---- Datos fijos ----
    private final int id;
    private final String nombre;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int memoria;
    private final int deadlineInicial;
    private final int instruccionesTotales;
    private final int ciclosEntreEs;
    private final int duracionEs;
    private final TipoOperacionES operacion;
    private final String rutaArchivo;
    private final int tamanoBloques;
    private final String nuevoNombre;
    private final String usuario;
    private final Permiso permiso;
    private final ComportamientoProceso comportamiento;

    // ---- Estado dinámico ----
    private volatile EstadoProceso estado = EstadoProceso.NUEVO;
    private volatile MotivoBloqueo motivoBloqueo = MotivoBloqueo.NINGUNO;
    private volatile int deadlineRestante;
    private volatile int instruccionesRestantes;
    private volatile int pc;
    private volatile int mar;
    private volatile int instruccionesDesdeEs;
    private volatile int ciclosBloqueoRestantes;
    private volatile long ordenCola;

    // ---- E/S de disco y suspensión ----
    private volatile SolicitudDisco solicitudActual;
    private volatile SolicitudDisco solicitudSwap;
    private volatile int primerBloqueSwap = -1;
    /** Se está escribiendo en el swap (sigue ocupando RAM hasta que el disco termine). */
    private volatile boolean suspendiendo;
    /** Se está leyendo del swap (ya tiene su RAM reservada). */
    private volatile boolean reactivando;
    /** Su evento ocurrió mientras se escribía en el swap: al terminar queda Listo/Suspendido. */
    private volatile boolean eventoDuranteSuspension;
    private volatile String errorES;

    // ---- Datos para métricas ----
    private volatile long cicloLlegada;
    private volatile long cicloPrimeraEjecucion = -1;
    private volatile long cicloFin;
    private volatile long cicloInicioBloqueo;
    private volatile long tiempoBloqueado;
    private volatile boolean terminoPorDeadline;
    private volatile int vecesSuspendido;

    public PCB(EspecificacionProceso esp, ComportamientoProceso comportamiento) {
        this.id = GeneradorId.nuevoId();
        this.nombre = esp.nombre().trim();
        this.tipo = esp.tipo();
        this.prioridad = esp.prioridad();
        this.memoria = esp.memoria();
        this.deadlineInicial = esp.deadline();
        this.instruccionesTotales = esp.instrucciones();
        this.ciclosEntreEs = esp.ciclosEntreEs();
        this.duracionEs = esp.duracionEs();
        this.operacion = esp.operacion();
        this.rutaArchivo = esp.ruta();
        this.tamanoBloques = esp.tamanoBloques();
        this.nuevoNombre = esp.nuevoNombre();
        this.usuario = esp.usuario() == null ? ModoUsuario.NOMBRE_ADMIN : esp.usuario();
        this.permiso = esp.permiso() == null ? Permiso.PRIVADO : esp.permiso();
        this.comportamiento = comportamiento;
        this.deadlineRestante = esp.deadline();
        this.instruccionesRestantes = esp.instrucciones();
    }

    // ---- Operaciones ----

    /** Avanza una instrucción: PC y MAR +1 y una instrucción menos por ejecutar. */
    public void avanzarInstruccion() {
        pc++;
        mar++;
        instruccionesRestantes--;
    }

    /** Resta un ciclo al deadline y devuelve lo que queda. */
    public int decrementarDeadline() {
        return --deadlineRestante;
    }

    public void incrementarInstruccionesDesdeEs() { instruccionesDesdeEs++; }

    public void reiniciarInstruccionesDesdeEs() { instruccionesDesdeEs = 0; }

    public void sumarTiempoBloqueado(long ciclos) { tiempoBloqueado += ciclos; }

    public void sumarSuspension() { vecesSuspendido++; }

    // ---- PropietarioBloques (bloques de swap de un proceso suspendido) ----

    @Override
    public String getEtiquetaBloques() {
        return "swap " + getEtiqueta();
    }

    @Override
    public int getColorId() {
        return id;
    }

    // ---- Getters ----
    public int getId() { return id; }
    /** Etiqueta corta para el log y la GUI, por ejemplo "P3". */
    public String getEtiqueta() { return "P" + id; }
    public String getNombre() { return nombre; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoria() { return memoria; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getCiclosEntreEs() { return ciclosEntreEs; }
    public int getDuracionEs() { return duracionEs; }
    public TipoOperacionES getOperacion() { return operacion; }
    public String getRutaArchivo() { return rutaArchivo; }
    public int getTamanoBloques() { return tamanoBloques; }
    public String getNuevoNombre() { return nuevoNombre; }
    public String getUsuario() { return usuario; }
    public Permiso getPermiso() { return permiso; }
    public ComportamientoProceso getComportamiento() { return comportamiento; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public int getDeadlineRestante() { return deadlineRestante; }
    public int getInstruccionesRestantes() { return instruccionesRestantes; }
    /** Tiempo restante de ejecución (en instrucciones/ciclos de CPU). */
    public int getTiempoRestante() { return instruccionesRestantes; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public int getInstruccionesDesdeEs() { return instruccionesDesdeEs; }
    public int getCiclosBloqueoRestantes() { return ciclosBloqueoRestantes; }
    public long getOrdenCola() { return ordenCola; }
    public SolicitudDisco getSolicitudActual() { return solicitudActual; }
    public SolicitudDisco getSolicitudSwap() { return solicitudSwap; }
    public int getPrimerBloqueSwap() { return primerBloqueSwap; }
    public boolean isSuspendiendo() { return suspendiendo; }
    public boolean isReactivando() { return reactivando; }
    public boolean isEventoDuranteSuspension() { return eventoDuranteSuspension; }
    public String getErrorES() { return errorES; }
    public long getCicloLlegada() { return cicloLlegada; }
    public long getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public long getCicloFin() { return cicloFin; }
    public long getCicloInicioBloqueo() { return cicloInicioBloqueo; }
    public long getTiempoBloqueado() { return tiempoBloqueado; }
    public boolean isTerminoPorDeadline() { return terminoPorDeadline; }
    public int getVecesSuspendido() { return vecesSuspendido; }

    // ---- Setters (los usa el kernel) ----
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setMotivoBloqueo(MotivoBloqueo motivo) { this.motivoBloqueo = motivo; }
    public void setCiclosBloqueoRestantes(int ciclos) { this.ciclosBloqueoRestantes = ciclos; }
    public void setOrdenCola(long orden) { this.ordenCola = orden; }
    public void setSolicitudActual(SolicitudDisco solicitud) { this.solicitudActual = solicitud; }
    public void setSolicitudSwap(SolicitudDisco solicitud) { this.solicitudSwap = solicitud; }
    public void setPrimerBloqueSwap(int bloque) { this.primerBloqueSwap = bloque; }
    public void setSuspendiendo(boolean valor) { this.suspendiendo = valor; }
    public void setReactivando(boolean valor) { this.reactivando = valor; }
    public void setEventoDuranteSuspension(boolean valor) { this.eventoDuranteSuspension = valor; }
    public void setErrorES(String error) { this.errorES = error; }
    public void setCicloLlegada(long ciclo) { this.cicloLlegada = ciclo; }
    public void setCicloPrimeraEjecucion(long ciclo) { this.cicloPrimeraEjecucion = ciclo; }
    public void setCicloFin(long ciclo) { this.cicloFin = ciclo; }
    public void setCicloInicioBloqueo(long ciclo) { this.cicloInicioBloqueo = ciclo; }
    public void setTerminoPorDeadline(boolean valor) { this.terminoPorDeadline = valor; }

    @Override
    public String toString() {
        return getEtiqueta() + " (" + nombre + ")";
    }
}
