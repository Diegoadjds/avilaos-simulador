package com.mycompany.avilaos.estructuras;

/**
 * Lista simplemente enlazada propia. NO es segura entre hilos: si la comparten
 * varios hilos, usar {@link ListaSegura}.
 */
public class ListaEnlazada<T> {

    private static class Nodo<E> {
        E dato;
        Nodo<E> siguiente;

        Nodo(E dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamano++;
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /** Inserta manteniendo el orden del comparador (estable: los empates quedan después). */
    public void insertarOrdenado(T dato, Comparador<T> comparador) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else if (comparador.comparar(dato, cabeza.dato) < 0) {
            nuevo.siguiente = cabeza;
            cabeza = nuevo;
        } else {
            Nodo<T> p = cabeza;
            while (p.siguiente != null && comparador.comparar(p.siguiente.dato, dato) <= 0) {
                p = p.siguiente;
            }
            nuevo.siguiente = p.siguiente;
            p.siguiente = nuevo;
            if (nuevo.siguiente == null) {
                cola = nuevo;
            }
        }
        tamano++;
    }

    /** Devuelve el primer elemento sin sacarlo (null si está vacía). */
    public T primero() {
        return cabeza == null ? null : cabeza.dato;
    }

    /** Saca y devuelve el primer elemento (null si está vacía). */
    public T extraerPrimero() {
        if (cabeza == null) {
            return null;
        }
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        if (cabeza == null) {
            cola = null;
        }
        tamano--;
        return dato;
    }

    /** Elimina el elemento comparando por referencia (==). */
    public boolean eliminar(T dato) {
        Nodo<T> previo = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (actual.dato == dato) {
                if (previo == null) {
                    cabeza = actual.siguiente;
                } else {
                    previo.siguiente = actual.siguiente;
                }
                if (actual == cola) {
                    cola = previo;
                }
                tamano--;
                return true;
            }
            previo = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    public boolean contiene(T dato) {
        Nodo<T> n = cabeza;
        while (n != null) {
            if (n.dato == dato) {
                return true;
            }
            n = n.siguiente;
        }
        return false;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + ", tamaño " + tamano);
        }
        Nodo<T> n = cabeza;
        for (int i = 0; i < indice; i++) {
            n = n.siguiente;
        }
        return n.dato;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void limpiar() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    /** Ordenamiento por inserción (estable), programado a mano sobre los nodos. */
    public void ordenar(Comparador<T> comparador) {
        if (tamano < 2) {
            return;
        }
        Nodo<T> ordenada = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            Nodo<T> siguiente = actual.siguiente;
            if (ordenada == null || comparador.comparar(actual.dato, ordenada.dato) < 0) {
                actual.siguiente = ordenada;
                ordenada = actual;
            } else {
                Nodo<T> p = ordenada;
                while (p.siguiente != null && comparador.comparar(p.siguiente.dato, actual.dato) <= 0) {
                    p = p.siguiente;
                }
                actual.siguiente = p.siguiente;
                p.siguiente = actual;
            }
            actual = siguiente;
        }
        cabeza = ordenada;
        Nodo<T> n = cabeza;
        while (n.siguiente != null) {
            n = n.siguiente;
        }
        cola = n;
    }

    /**
     * Copia los elementos a un arreglo (útil para que la GUI recorra una "foto" de la lista).
     * Uso típico: {@code lista.aArreglo(new PCB[0])}.
     */
    @SuppressWarnings("unchecked")
    public T[] aArreglo(T[] destino) {
        T[] r = destino.length >= tamano
                ? destino
                : (T[]) java.lang.reflect.Array.newInstance(destino.getClass().getComponentType(), tamano);
        Nodo<T> n = cabeza;
        int i = 0;
        while (n != null) {
            r[i++] = n.dato;
            n = n.siguiente;
        }
        if (r.length > tamano) {
            r[tamano] = null;
        }
        return r;
    }
}
