package com.mycompany.avilaos.modelo;

/**
 * Lo implementa el Kernel. Permite que un semáforo (que puede ser ejecutado por un
 * proceso de OTRO computador) despierte a un proceso sin conocer el kernel concreto:
 * basta con llamar a {@link PCB#solicitarDesbloqueo()}.
 */
public interface GestorDesbloqueo {

    /** Pide que el proceso salga de Bloqueado a Listo (se hace en el próximo ciclo). */
    void desbloquear(PCB pcb);
}
