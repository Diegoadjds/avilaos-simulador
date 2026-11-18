package com.mycompany.avilaos.modelo;

/**
 * Estrategia: define QUÉ hace un proceso en cada instrucción. Así el CPU no sabe
 * nada de tipos de proceso: solo pide "ejecuta una instrucción".
 * Implementaciones: CpuBound, IoBound y ComportamientoArchivo (proceso de E/S de archivos).
 */
public interface ComportamientoProceso {

    /** Ejecuta UNA instrucción (un ciclo) del proceso y dice qué pasó. */
    ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto);

    /**
     * Texto corto con la última instrucción que ejecutó el proceso, para mostrarla en la interfaz.
     * Por defecto es "instrucción k de N"; los procesos de E/S indican la llamada al sistema.
     */
    default String ultimaInstruccion(PCB pcb) {
        if (pcb.getPc() == 0) {
            return "—";
        }
        return "instrucción " + pcb.getPc() + " de " + pcb.getInstruccionesTotales();
    }
}
