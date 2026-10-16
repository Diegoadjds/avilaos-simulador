package com.mycompany.avilaos.modelo;

/**
 * Estrategia: define QUÉ hace un proceso en cada instrucción. Así el CPU no sabe
 * nada de tipos de proceso: solo pide "ejecuta una instrucción".
 * Implementaciones: CpuBound, IoBound, (pendiente) Productor y Consumidor.
 */
public interface ComportamientoProceso {

    /** Ejecuta UNA instrucción (un ciclo) del proceso y dice qué pasó. */
    ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto);
}
