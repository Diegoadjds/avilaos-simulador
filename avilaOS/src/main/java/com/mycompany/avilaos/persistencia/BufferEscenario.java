package com.mycompany.avilaos.persistencia;

/**
 * Buffer de la carga inicial de un escenario.
 * Los buffers se numeran en el orden en que aparecen (el primero es B1, el segundo B2, ...);
 * los procesos productores y consumidores los referencian por ese número.
 *
 * @param capacidad  elementos que caben en el buffer (también es la RAM que ocupa)
 * @param computador computador anfitrión (1..N)
 */
public record BufferEscenario(int capacidad, int computador) {
}
