package com.mycompany.avilaos.sincronizacion;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.modelo.MotivoBloqueo;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.RecursoBloqueante;
import java.util.concurrent.Semaphore;

/**
 * Semáforo de ÁvilaOS (de conteo; con valor inicial 1 funciona como binario/mutex).
 * Sigue la definición de Stallings: semWait resta 1 y bloquea si el valor queda negativo;
 * semSignal suma 1 y despierta a uno si el valor es <= 0 (un valor negativo = procesos esperando).
 *
 * OJO: este semáforo es del SISTEMA SIMULADO (bloquea PCBs). El java.util.concurrent.Semaphore
 * que usa por dentro es solo el mutex de Java que protege sus propios campos entre hilos.
 */
public class Semaforo implements RecursoBloqueante {

    private final String nombre;
    private final MotivoBloqueo motivo;
    private int valor;
    private final ListaEnlazada<PCB> espera = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1);

    public Semaforo(String nombre, MotivoBloqueo motivo, int valorInicial) {
        this.nombre = nombre;
        this.motivo = motivo;
        this.valor = valorInicial;
    }

    /**
     * @return true si el proceso puede continuar; false si quedó en la cola del semáforo
     *         (la instrucción debe devolver un bloqueo en este recurso).
     */
    public boolean semWait(PCB pcb) {
        mutex.acquireUninterruptibly();
        try {
            valor--;
            if (valor < 0) {
                espera.agregarAlFinal(pcb);
                return false;
            }
            return true;
        } finally {
            mutex.release();
        }
    }

    /** Suma 1 y, si había procesos esperando, despierta al primero (puede ser de otro computador). */
    public void semSignal() {
        PCB despertar = null;
        mutex.acquireUninterruptibly();
        try {
            valor++;
            if (valor <= 0) {
                despertar = espera.extraerPrimero();
            }
        } finally {
            mutex.release();
        }
        if (despertar != null) {
            despertar.solicitarDesbloqueo(); // fuera del mutex, para no cruzar locks con el kernel
        }
    }

    /**
     * Si un proceso muere esperando (deadline), sale de la cola y se devuelve su "wait".
     *
     * @return false si el proceso ya no estaba en la cola: un semSignal ya lo había despertado,
     *         así que ya tiene el permiso y quien lo termina debe devolverlo.
     */
    @Override
    public boolean cancelarEspera(PCB pcb) {
        mutex.acquireUninterruptibly();
        try {
            if (espera.eliminar(pcb)) {
                valor++;
                return true;
            }
            return false;
        } finally {
            mutex.release();
        }
    }

    public int getValor() {
        mutex.acquireUninterruptibly();
        try {
            return valor;
        } finally {
            mutex.release();
        }
    }

    public PCB[] getEsperando() {
        mutex.acquireUninterruptibly();
        try {
            return espera.aArreglo(new PCB[0]);
        } finally {
            mutex.release();
        }
    }

    @Override
    public String getNombre() { return nombre; }

    public MotivoBloqueo getMotivo() { return motivo; }
}
