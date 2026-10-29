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
     * Texto corto con la última instrucción que ejecutó el proceso, para mostrarla en la interfaz.
     * Por defecto (CPU bound e I/O bound) es "instrucción k de N"; los productores y consumidores
     * indican el paso del algoritmo (por ejemplo "semWait(mutex)" o "producir (2/4)").
     */
    default String ultimaInstruccion(PCB pcb) {
        if (pcb.getPc() == 0) {
            return "—";
        }
        return "instrucción " + pcb.getPc() + " de " + pcb.getInstruccionesTotales();
    }

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
