package com.mycompany.avilaos.modelo;

/**
 * Bloque de control del proceso. Como cada proceso tiene un único hilo (modelo monohilo),
 * el PCB integra al TCB: aquí viven el PC, el MAR y el estado de ejecución.
 *
 * Los campos que cambian son volatile porque la GUI los lee desde otro hilo.
 * Convención de prioridad: número MENOR = prioridad MÁS ALTA (1 es la más alta).
 */
public class PCB {

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
    private final int idBuffer;
    private final int ciclosPorElemento;
    private final int elementosRequeridos;
    private final ComportamientoProceso comportamiento;

    // ---- Estado dinámico ----
    private volatile EstadoProceso estado = EstadoProceso.NUEVO;
    private volatile MotivoBloqueo motivoBloqueo = MotivoBloqueo.NINGUNO;
    private volatile RecursoBloqueante recursoBloqueo;
    private volatile int idComputador;
    private volatile GestorDesbloqueo gestorDesbloqueo;
    private volatile int deadlineRestante;
    private volatile int instruccionesRestantes;
    private volatile int pc;
    private volatile int mar;
    private volatile int instruccionesDesdeEs;
    private volatile int ciclosBloqueoRestantes;
    private volatile int elementosProcesados;
    private volatile long ordenCola;

    // ---- Datos para métricas ----
    private volatile long cicloLlegada;
    private volatile long cicloPrimeraEjecucion = -1;
    private volatile long cicloFin;
    private volatile long cicloInicioBloqueo;
    private volatile long tiempoBloqueado;
    private volatile boolean terminoPorDeadline;

    public PCB(EspecificacionProceso esp, ComportamientoProceso comportamiento) {
        this.id = GeneradorId.nuevoId();
        this.nombre = esp.nombre();
        this.tipo = esp.tipo();
        this.prioridad = esp.prioridad();
        this.memoria = esp.memoria();
        this.deadlineInicial = esp.deadline();
        this.instruccionesTotales = esp.instrucciones();
        this.ciclosEntreEs = esp.ciclosEntreEs();
        this.duracionEs = esp.duracionEs();
        this.idBuffer = esp.idBuffer();
        this.ciclosPorElemento = esp.ciclosPorElemento();
        this.elementosRequeridos = esp.elementosRequeridos();
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

    /** Lo llaman los semáforos al hacer semSignal sobre un proceso que esperaba. */
    public void solicitarDesbloqueo() {
        GestorDesbloqueo g = gestorDesbloqueo;
        if (g != null) {
            g.desbloquear(this);
        }
    }

    public void asignarComputador(int idComputador, GestorDesbloqueo gestor) {
        this.idComputador = idComputador;
        this.gestorDesbloqueo = gestor;
    }

    public void incrementarInstruccionesDesdeEs() { instruccionesDesdeEs++; }
    public void reiniciarInstruccionesDesdeEs() { instruccionesDesdeEs = 0; }
    public void incrementarElementosProcesados() { elementosProcesados++; }
    public void sumarTiempoBloqueado(long ciclos) { tiempoBloqueado += ciclos; }

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
    public int getIdBuffer() { return idBuffer; }
    public int getCiclosPorElemento() { return ciclosPorElemento; }
    public int getElementosRequeridos() { return elementosRequeridos; }
    public ComportamientoProceso getComportamiento() { return comportamiento; }

    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public RecursoBloqueante getRecursoBloqueo() { return recursoBloqueo; }
    public int getIdComputador() { return idComputador; }
    public int getDeadlineRestante() { return deadlineRestante; }
    public int getInstruccionesRestantes() { return instruccionesRestantes; }
    /** Tiempo restante de ejecución (en instrucciones/ciclos de CPU). */
    public int getTiempoRestante() { return instruccionesRestantes; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public int getInstruccionesDesdeEs() { return instruccionesDesdeEs; }
    public int getCiclosBloqueoRestantes() { return ciclosBloqueoRestantes; }
    public int getElementosProcesados() { return elementosProcesados; }
    public long getOrdenCola() { return ordenCola; }
    public long getCicloLlegada() { return cicloLlegada; }
    public long getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public long getCicloFin() { return cicloFin; }
    public long getCicloInicioBloqueo() { return cicloInicioBloqueo; }
    public long getTiempoBloqueado() { return tiempoBloqueado; }
    public boolean isTerminoPorDeadline() { return terminoPorDeadline; }

    // ---- Setters (los usa el kernel) ----
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setMotivoBloqueo(MotivoBloqueo motivo) { this.motivoBloqueo = motivo; }
    public void setRecursoBloqueo(RecursoBloqueante recurso) { this.recursoBloqueo = recurso; }
    public void setCiclosBloqueoRestantes(int ciclos) { this.ciclosBloqueoRestantes = ciclos; }
    public void setOrdenCola(long orden) { this.ordenCola = orden; }
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
