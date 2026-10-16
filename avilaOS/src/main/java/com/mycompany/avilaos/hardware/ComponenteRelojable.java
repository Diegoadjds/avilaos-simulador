package com.mycompany.avilaos.hardware;

/**
 * Todo componente que reacciona al reloj global (Computador, CPU y, en el Proyecto 2,
 * DMA, controlador de E/S, etc.) implementa esta interfaz.
 */
public interface ComponenteRelojable {

    /** Avanza exactamente un ciclo de ejecución. */
    void alCiclo(long ciclo);
}
