package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.estructuras.ListaSegura;
import java.util.concurrent.Semaphore;

/**
 * Reloj global del sistema distribuido. Corre en su propio hilo y cada ciclo:
 *   1) despierta a TODOS los componentes registrados (cada uno corre en su propio hilo),
 *   2) espera a que todos terminen su ciclo (barrera hecha con semáforos),
 *   3) duerme la duración de un ciclo.
 * Así, en cada ciclo cada computador avanza exactamente un paso.
 */
public class RelojGlobal implements Runnable {

    /** Hilo dedicado a un componente: espera la señal de inicio, ejecuta y avisa que terminó. */
    private static class Trabajador implements Runnable {
        private final ComponenteRelojable componente;
        private final Semaphore inicio = new Semaphore(0);
        private final Semaphore fin = new Semaphore(0);
        private volatile boolean activo = true;
        private volatile long ciclo;

        Trabajador(ComponenteRelojable componente) {
            this.componente = componente;
        }

        @Override
        public void run() {
            while (true) {
                inicio.acquireUninterruptibly();
                if (!activo) {
                    return;
                }
                try {
                    componente.alCiclo(ciclo);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } finally {
                    fin.release();
                }
            }
        }
    }

    private final ListaSegura<Trabajador> trabajadores = new ListaSegura<>();
    private final Semaphore exclusionCiclo = new Semaphore(1);
    private volatile long ciclo = 0;
    private volatile int duracionCicloMs;
    private volatile boolean corriendo = false;
    private volatile boolean pausado = false;
    private Thread hilo;

    public RelojGlobal(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
    }

    /** Registra un componente y le crea su hilo. Hacerlo antes de iniciar el reloj. */
    public void registrar(ComponenteRelojable componente) {
        Trabajador t = new Trabajador(componente);
        Thread h = new Thread(t, "Componente-" + componente.getClass().getSimpleName());
        h.setDaemon(true);
        trabajadores.agregarAlFinal(t);
        h.start();
    }

    public synchronized void iniciar() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        pausado = false;
        hilo = new Thread(this, "RelojGlobal");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void pausar() { pausado = true; }

    public void reanudar() { pausado = false; }

    /** Con el reloj pausado, ejecuta un solo ciclo (útil para depurar paso a paso). */
    public void avanzarUnCiclo() {
        if (pausado) {
            ejecutarCiclo();
        }
    }

    public void detener() {
        corriendo = false;
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    @Override
    public void run() {
        while (corriendo) {
            if (pausado) {
                dormir(50);
                continue;
            }
            ejecutarCiclo();
            dormir(duracionCicloMs);
        }
        // Al detenerse, se apaga a los hilos de los componentes.
        for (Trabajador t : trabajadores.aArreglo(new Trabajador[0])) {
            t.activo = false;
            t.inicio.release();
        }
    }

    private void ejecutarCiclo() {
        exclusionCiclo.acquireUninterruptibly();
        try {
            Trabajador[] ts = trabajadores.aArreglo(new Trabajador[0]);
            ciclo++;
            for (Trabajador t : ts) {
                t.ciclo = ciclo;
                t.inicio.release();
            }
            for (Trabajador t : ts) {
                t.fin.acquireUninterruptibly();
            }
        } finally {
            exclusionCiclo.release();
        }
    }

    private void dormir(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            corriendo = false;
        }
    }

    public long getCicloActual() { return ciclo; }
    public int getDuracionCicloMs() { return duracionCicloMs; }
    public void setDuracionCicloMs(int ms) { this.duracionCicloMs = ms; }
    public boolean isPausado() { return pausado; }
    public boolean isCorriendo() { return corriendo; }
}
