package com.mycompany.avilaos.sincronizacion;

import com.mycompany.avilaos.modelo.MotivoBloqueo;

/**
 * Buffer de capacidad limitada del problema productor-consumidor (Stallings, fig. 5.13).
 * Tres semáforos: mutex (s = 1), llenos (n = 0: elementos disponibles) y vacíos (e = capacidad).
 * Reside en la RAM de su computador anfitrión.
 */
public class BufferAcotado {

    private final int id;
    private final int capacidad;
    private final int idComputadorAnfitrion;
    private final Semaforo mutex;
    private final Semaforo llenos;
    private final Semaforo vacios;
    private volatile int elementos;
    private volatile int producidos;
    private volatile int consumidos;

    public BufferAcotado(int id, int capacidad, int idComputadorAnfitrion) {
        this.id = id;
        this.capacidad = capacidad;
        this.idComputadorAnfitrion = idComputadorAnfitrion;
        this.mutex = new Semaforo("mutex", MotivoBloqueo.SEMAFORO_MUTEX, 1);
        this.llenos = new Semaforo("llenos", MotivoBloqueo.SEMAFORO_LLENOS, 0);
        this.vacios = new Semaforo("vacíos", MotivoBloqueo.SEMAFORO_VACIOS, capacidad);
    }

    /** anyadir(): solo se llama dentro de la región crítica (con el mutex tomado). */
    public void anyadir() {
        elementos++;
        producidos++;
    }

    /** extraer(): solo se llama dentro de la región crítica (con el mutex tomado). */
    public void extraer() {
        elementos--;
        consumidos++;
    }

    public int getId() { return id; }
    public String getNombre() { return "B" + id; }
    public int getCapacidad() { return capacidad; }
    public int getIdComputadorAnfitrion() { return idComputadorAnfitrion; }
    public Semaforo getMutex() { return mutex; }
    public Semaforo getLlenos() { return llenos; }
    public Semaforo getVacios() { return vacios; }
    public int getElementos() { return elementos; }
    public int getProducidos() { return producidos; }
    public int getConsumidos() { return consumidos; }

    @Override
    public String toString() {
        return getNombre() + " (cap. " + capacidad + ", en Computador " + idComputadorAnfitrion + ")";
    }
}
