package com.mycompany.avilaos.so;

/** La GUI implementa esto para recibir cada línea nueva del log de eventos. */
public interface ObservadorEventos {

    /** OJO: se llama desde hilos de la simulación; en Swing usar SwingUtilities.invokeLater. */
    void alRegistrarEvento(String linea);
}
