package com.mycompany.avilaos.hardware;

import java.util.concurrent.Semaphore;

/**
 * RAM de un computador. En el Proyecto 1 solo se controla cuánta memoria está usada
 * y cuánta libre (sin particiones ni asignación contigua). Los buffers también
 * reservan aquí su capacidad, en la RAM de su computador anfitrión.
 */
public class MemoriaPrincipal {

    private final int total;
    private volatile int usada = 0;
    private final Semaphore mutex = new Semaphore(1);

    public MemoriaPrincipal(int total) {
        this.total = total;
    }

    /** Reserva memoria si hay espacio suficiente. Devuelve false si no cabe. */
    public boolean reservar(int cantidad) {
        mutex.acquireUninterruptibly();
        try {
            if (cantidad < 0 || usada + cantidad > total) {
                return false;
            }
            usada += cantidad;
            return true;
        } finally {
            mutex.release();
        }
    }

    public void liberar(int cantidad) {
        mutex.acquireUninterruptibly();
        try {
            usada = Math.max(0, usada - cantidad);
        } finally {
            mutex.release();
        }
    }

    public int getTotal() { return total; }
    public int getUsada() { return usada; }
    public int getLibre() { return total - usada; }
}
