package com.mycompany.avilaos.modelo;

/**
 * Subestados del estado BLOQUEADO. Un proceso bloqueado siempre tiene un motivo,
 * que es lo que la GUI muestra en la cola de bloqueados.
 *
 * Justificación (para la defensa): cada motivo tiene un mecanismo de salida distinto.
 * - ENTRADA_SALIDA y ACCESO_REMOTO salen solos tras N ciclos (los cuenta el kernel).
 * - Los SEMAFORO_* salen cuando otro proceso hace semSignal sobre ese semáforo.
 * Se distinguen los tres semáforos del productor-consumidor porque el estudio de
 * "tiempo bloqueado en semáforos" y la GUI necesitan saber en cuál espera.
 */
public enum MotivoBloqueo {
    NINGUNO("-", false),
    ENTRADA_SALIDA("Esperando E/S", false),
    ACCESO_REMOTO("Acceso remoto a buffer (red)", false),
    SEMAFORO_MUTEX("Semáforo mutex (región crítica)", true),
    SEMAFORO_VACIOS("Semáforo vacíos (buffer lleno)", true),
    SEMAFORO_LLENOS("Semáforo llenos (buffer vacío)", true);

    private final String etiqueta;
    private final boolean semaforo;

    MotivoBloqueo(String etiqueta, boolean semaforo) {
        this.etiqueta = etiqueta;
        this.semaforo = semaforo;
    }

    /** true si el bloqueo termina por un semSignal de otro proceso. */
    public boolean esSemaforo() {
        return semaforo;
    }

    /** true si el bloqueo termina solo, tras una cantidad fija de ciclos. */
    public boolean esPorTiempo() {
        return this == ENTRADA_SALIDA || this == ACCESO_REMOTO;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
