package com.mycompany.avilaos.modelo;

/**
 * Lo que un {@link ComportamientoProceso} puede consultar del sistema mientras ejecuta:
 * el ciclo actual, el log de eventos y el servicio de E/S de archivos del sistema operativo.
 */
public interface ContextoEjecucion {

    long getCicloActual();

    int getIdComputador();

    /** Escribe una línea en el log global de eventos. */
    void registrarEvento(String mensaje);

    /**
     * Llamada al sistema de un proceso de E/S: valida su operación sobre el sistema de archivos,
     * reserva lo necesario y encola la solicitud en la cola de E/S del disco.
     *
     * @throws ErrorOperacion si la operación no se puede realizar (el proceso termina con error)
     */
    SolicitudDisco solicitarArchivo(PCB pcb) throws ErrorOperacion;
}
