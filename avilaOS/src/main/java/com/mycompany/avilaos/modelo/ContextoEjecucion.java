package com.mycompany.avilaos.modelo;

/**
 * Lo que un {@link ComportamientoProceso} puede consultar del sistema mientras ejecuta.
 * Se puede ampliar (por ejemplo, con acceso a los buffers o a la latencia de red).
 */
public interface ContextoEjecucion {

    long getCicloActual();

    int getIdComputador();

    /** Escribe una línea en el log global de eventos. */
    void registrarEvento(String mensaje);
}
