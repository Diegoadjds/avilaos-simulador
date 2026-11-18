package com.mycompany.avilaos.modelo;

/** Resultado de ejecutar una instrucción: en qué modo corrió y qué sigue para el proceso. */
public final class ResultadoInstruccion {

    private final ModoCPU modo;
    private final AccionInstruccion accion;
    private final MotivoBloqueo motivo;
    private final int ciclosBloqueo;

    private ResultadoInstruccion(ModoCPU modo, AccionInstruccion accion, MotivoBloqueo motivo, int ciclosBloqueo) {
        this.modo = modo;
        this.accion = accion;
        this.motivo = motivo;
        this.ciclosBloqueo = ciclosBloqueo;
    }

    public static ResultadoInstruccion continuar(ModoCPU modo) {
        return new ResultadoInstruccion(modo, AccionInstruccion.CONTINUAR, MotivoBloqueo.NINGUNO, 0);
    }

    public static ResultadoInstruccion terminar(ModoCPU modo) {
        return new ResultadoInstruccion(modo, AccionInstruccion.TERMINAR, MotivoBloqueo.NINGUNO, 0);
    }

    /** Bloqueo que termina solo tras {@code ciclos} ciclos (E/S simulada de un proceso I/O bound). */
    public static ResultadoInstruccion bloquearPorTiempo(ModoCPU modo, MotivoBloqueo motivo, int ciclos) {
        return new ResultadoInstruccion(modo, AccionInstruccion.BLOQUEAR, motivo, ciclos);
    }

    /** Bloqueo que termina cuando el disco atiende la solicitud del proceso. */
    public static ResultadoInstruccion bloquearPorDisco(ModoCPU modo) {
        return new ResultadoInstruccion(modo, AccionInstruccion.BLOQUEAR, MotivoBloqueo.ESPERA_DISCO, 0);
    }

    public ModoCPU getModo() { return modo; }
    public AccionInstruccion getAccion() { return accion; }
    public MotivoBloqueo getMotivo() { return motivo; }
    public int getCiclosBloqueo() { return ciclosBloqueo; }
}
