package com.mycompany.avilaos.estructuras;

/**
 * Criterio de ordenamiento propio (reemplaza a java.util.Comparator, que no usamos
 * para no depender del framework de colecciones).
 */
@FunctionalInterface
public interface Comparador<T> {

    /** Negativo si {@code a} va antes que {@code b}, 0 si empatan, positivo si va después. */
    int comparar(T a, T b);
}
