package com.mycompany.avilaos.modelo;

/**
 * Una solicitud a la cola de E/S del disco. Es lo que el planificador de disco ordena y lo
 * que el disco atiende: recorre los bloques indicados (la cadena del archivo) uno por ciclo.
 * Las operaciones de directorios no tocan bloques: solo gastan un ciclo (bloques vacío).
 */
public final class SolicitudDisco {

    private static final Object CANDADO_IDS = new Object();
    private static int siguienteId = 1;

    private final int id;
    private final PCB proceso;
    private final TipoOperacionES operacion;
    private final int[] bloques;
    /** Información propia de quien creó la solicitud (el gestor de E/S guarda aquí el nodo afectado). */
    private final Object detalle;
    private volatile long cicloLlegada;
    private volatile long cicloInicio = -1;
    private volatile long cicloFin = -1;
    private volatile boolean cancelada;
    /** Política con la que se atendió (para contar las solicitudes por política). */
    private volatile String politicaUsada = "";

    public SolicitudDisco(PCB proceso, TipoOperacionES operacion, int[] bloques, Object detalle) {
        synchronized (CANDADO_IDS) {
            this.id = siguienteId++;
        }
        this.proceso = proceso;
        this.operacion = operacion;
        this.bloques = bloques.clone();
        this.detalle = detalle;
    }

    /** Solo para reiniciar la simulación. */
    public static void reiniciarIds() {
        synchronized (CANDADO_IDS) {
            siguienteId = 1;
        }
    }

    public int getId() { return id; }
    public PCB getProceso() { return proceso; }
    public TipoOperacionES getOperacion() { return operacion; }
    public int[] getBloques() { return bloques.clone(); }
    public int getCantidadBloques() { return bloques.length; }
    public int getBloque(int indice) { return bloques[indice]; }
    public Object getDetalle() { return detalle; }

    /** Primer bloque que debe visitar el cabezal, o -1 si la operación no toca bloques. */
    public int getBloqueObjetivo() {
        return bloques.length == 0 ? -1 : bloques[0];
    }

    /** Posición que usa una política para medir distancias: si no toca bloques, la del cabezal. */
    public int posicionPara(int cabezal) {
        return bloques.length == 0 ? cabezal : bloques[0];
    }

    public long getCicloLlegada() { return cicloLlegada; }
    public void setCicloLlegada(long ciclo) { this.cicloLlegada = ciclo; }
    public long getCicloInicio() { return cicloInicio; }
    public void setCicloInicio(long ciclo) { this.cicloInicio = ciclo; }
    public long getCicloFin() { return cicloFin; }
    public void setCicloFin(long ciclo) { this.cicloFin = ciclo; }
    public boolean isCancelada() { return cancelada; }
    public void cancelar() { this.cancelada = true; }
    public String getPoliticaUsada() { return politicaUsada; }
    public void setPoliticaUsada(String politica) { this.politicaUsada = politica; }

    @Override
    public String toString() {
        return "S" + id + " " + operacion + " " + (proceso == null ? "-" : proceso.getEtiqueta())
                + (bloques.length == 0 ? "" : " bloque " + bloques[0]);
    }
}
