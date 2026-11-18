package com.mycompany.avilaos.modelo;

/**
 * Subestados de BLOQUEADO (y de Bloqueado/Suspendido). Un proceso bloqueado siempre tiene un
 * motivo, que es lo que la interfaz muestra en la cola.
 *
 * Cada motivo sale de forma distinta: ENTRADA_SALIDA sale solo tras N ciclos (los cuenta el
 * kernel); ESPERA_DISCO sale cuando el disco termina la solicitud del proceso.
 */
public enum MotivoBloqueo {
    NINGUNO("-"),
    ENTRADA_SALIDA("Esperando E/S"),
    ESPERA_DISCO("Esperando al disco");

    private final String etiqueta;

    MotivoBloqueo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** true si el bloqueo termina solo, tras una cantidad fija de ciclos. */
    public boolean esPorTiempo() {
        return this == ENTRADA_SALIDA;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
