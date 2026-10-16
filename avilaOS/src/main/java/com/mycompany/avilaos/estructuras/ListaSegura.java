package com.mycompany.avilaos.estructuras;

import java.util.concurrent.Semaphore;

/**
 * {@link ListaEnlazada} protegida con un Semaphore de Java (exclusión mutua).
 * Es la estructura para todo lo que se comparte entre hilos: colas del kernel,
 * log de eventos, listas de observadores, etc.
 */
public class ListaSegura<T> {

    private final ListaEnlazada<T> lista = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1);

    public void agregarAlFinal(T dato) {
        mutex.acquireUninterruptibly();
        try {
            lista.agregarAlFinal(dato);
        } finally {
            mutex.release();
        }
    }

    public void agregarAlInicio(T dato) {
        mutex.acquireUninterruptibly();
        try {
            lista.agregarAlInicio(dato);
        } finally {
            mutex.release();
        }
    }

    public void insertarOrdenado(T dato, Comparador<T> comparador) {
        mutex.acquireUninterruptibly();
        try {
            lista.insertarOrdenado(dato, comparador);
        } finally {
            mutex.release();
        }
    }

    public T primero() {
        mutex.acquireUninterruptibly();
        try {
            return lista.primero();
        } finally {
            mutex.release();
        }
    }

    public T extraerPrimero() {
        mutex.acquireUninterruptibly();
        try {
            return lista.extraerPrimero();
        } finally {
            mutex.release();
        }
    }

    public boolean eliminar(T dato) {
        mutex.acquireUninterruptibly();
        try {
            return lista.eliminar(dato);
        } finally {
            mutex.release();
        }
    }

    public boolean contiene(T dato) {
        mutex.acquireUninterruptibly();
        try {
            return lista.contiene(dato);
        } finally {
            mutex.release();
        }
    }

    public int tamano() {
        mutex.acquireUninterruptibly();
        try {
            return lista.tamano();
        } finally {
            mutex.release();
        }
    }

    public boolean estaVacia() {
        return tamano() == 0;
    }

    public void limpiar() {
        mutex.acquireUninterruptibly();
        try {
            lista.limpiar();
        } finally {
            mutex.release();
        }
    }

    public void ordenar(Comparador<T> comparador) {
        mutex.acquireUninterruptibly();
        try {
            lista.ordenar(comparador);
        } finally {
            mutex.release();
        }
    }

    public T[] aArreglo(T[] destino) {
        mutex.acquireUninterruptibly();
        try {
            return lista.aArreglo(destino);
        } finally {
            mutex.release();
        }
    }
}
