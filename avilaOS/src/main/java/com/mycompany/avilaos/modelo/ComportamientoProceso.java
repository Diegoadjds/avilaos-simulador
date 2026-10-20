package com.mycompany.avilaos.modelo;

/**
 * Estrategia: define QUÉ hace un proceso en cada instrucción. Así el CPU no sabe
 * nada de tipos de proceso: solo pide "ejecuta una instrucción".
 * Implementaciones: CpuBound, IoBound, (pendiente) Productor y Consumidor.
 */
public interface ComportamientoProceso {

    /** Ejecuta UNA instrucción (un ciclo) del proceso y dice qué pasó. */
    ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto);

    /**
     * El kernel lo llama cuando termina al proceso a la fuerza (deadline vencido), para que
     * devuelva lo que tenía tomado. Un proceso que muere dentro de la región crítica, o entre
     * un semWait y su semSignal, dejaría el buffer bloqueado para siempre.
     * Por defecto no hace nada: solo los productores y consumidores toman semáforos.
     *
     * @param esperabaEnCola true si el proceso estaba en la cola de un semáforo, o sea que el
     *                       último semWait que pidió todavía no se le había concedido
     */
    default void liberarRecursos(PCB pcb, boolean esperabaEnCola, ContextoEjecucion contexto) {
    }
}
