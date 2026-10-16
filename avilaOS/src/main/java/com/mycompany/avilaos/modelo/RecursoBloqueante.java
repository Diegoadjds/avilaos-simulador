package com.mycompany.avilaos.modelo;

/**
 * Algo en cuya cola puede esperar un proceso bloqueado (por ejemplo, un semáforo).
 * El kernel lo usa para sacar al proceso de esa cola si el proceso es terminado
 * mientras espera (por ejemplo, por vencimiento de su deadline).
 */
public interface RecursoBloqueante {

    String getNombre();

    /** Quita al proceso de la cola de espera del recurso. */
    void cancelarEspera(PCB pcb);
}
