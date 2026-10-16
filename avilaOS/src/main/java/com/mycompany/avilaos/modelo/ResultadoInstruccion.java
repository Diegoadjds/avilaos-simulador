package com.mycompany.avilaos.modelo;

/** Resultado de ejecutar una instrucción: en qué modo corrió y qué sigue para el proceso. */
public final class ResultadoInstruccion {

    private final ModoCPU modo;
    private final AccionInstruccion accion;
    private final MotivoBloqueo motivo;
    private final int ciclosBloqueo;
    private final RecursoBloqueante recurso;

    private ResultadoInstruccion(ModoCPU modo, AccionInstruccion accion, MotivoBloqueo motivo,
            int ciclosBloqueo, RecursoBloqueante recurso) {
        this.modo = modo;
        this.accion = accion;
        this.motivo = motivo;
        this.ciclosBloqueo = ciclosBloqueo;
        this.recurso = recurso;
    }

    public static ResultadoInstruccion continuar(ModoCPU modo) {
        return new ResultadoInstruccion(modo, AccionInstruccion.CONTINUAR, MotivoBloqueo.NINGUNO, 0, null);
    }

    public static ResultadoInstruccion terminar(ModoCPU modo) {
        return new ResultadoInstruccion(modo, AccionInstruccion.TERMINAR, MotivoBloqueo.NINGUNO, 0, null);
    }

    /** Bloqueo que termina solo tras {@code ciclos} ciclos (E/S o acceso remoto). */
    public static ResultadoInstruccion bloquearPorTiempo(ModoCPU modo, MotivoBloqueo motivo, int ciclos) {
        return new ResultadoInstruccion(modo, AccionInstruccion.BLOQUEAR, motivo, ciclos, null);
    }

    /** Bloqueo en la cola de un recurso (semáforo); termina con un semSignal. */
    public static ResultadoInstruccion bloquearEnRecurso(ModoCPU modo, MotivoBloqueo motivo,
            RecursoBloqueante recurso) {
        return new ResultadoInstruccion(modo, AccionInstruccion.BLOQUEAR, motivo, 0, recurso);
    }

    public ModoCPU getModo() { return modo; }
    public AccionInstruccion getAccion() { return accion; }
    public MotivoBloqueo getMotivo() { return motivo; }
    public int getCiclosBloqueo() { return ciclosBloqueo; }
    public RecursoBloqueante getRecurso() { return recurso; }
}
