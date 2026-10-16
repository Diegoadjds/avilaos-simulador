package com.mycompany.avilaos.modelo;

import java.util.concurrent.Semaphore;

/** Genera IDs de proceso únicos en TODO el sistema distribuido. */
public final class GeneradorId {

    private static final Semaphore MUTEX = new Semaphore(1);
    private static int siguiente = 1;

    private GeneradorId() {
    }

    public static int nuevoId() {
        MUTEX.acquireUninterruptibly();
        try {
            return siguiente++;
        } finally {
            MUTEX.release();
        }
    }

    /** Solo para reiniciar la simulación. */
    public static void reiniciar() {
        MUTEX.acquireUninterruptibly();
        try {
            siguiente = 1;
        } finally {
            MUTEX.release();
        }
    }
}
