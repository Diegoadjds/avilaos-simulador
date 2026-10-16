package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificador;
import java.util.concurrent.Semaphore;

/**
 * Planificador a corto plazo de UN computador. Maneja la cola de listos y delega
 * todas las decisiones de orden y expropiación en la política activa (que se puede
 * cambiar en caliente).
 */
public class Planificador {

    private final ListaSegura<PCB> listos;
    private volatile PoliticaPlanificador politica;
    private final Semaphore mutexOrden = new Semaphore(1);
    private long contadorOrden = 0;

    public Planificador(PoliticaPlanificador politica, ListaSegura<PCB> listos) {
        this.politica = politica;
        this.listos = listos;
    }

    /** Mete un proceso a la cola de listos en la posición que indique la política. */
    public void agregarListo(PCB pcb) {
        pcb.setOrdenCola(siguienteOrden());
        listos.insertarOrdenado(pcb, politica.getCriterioOrden());
    }

    /** Saca al próximo proceso y reordena la cola de listos (con nuestro propio ordenamiento). */
    public PCB seleccionarSiguiente() {
        PCB elegido = listos.extraerPrimero();
        if (elegido != null) {
            listos.ordenar(politica.getCriterioOrden());
        }
        return elegido;
    }

    public boolean debeExpropiar(PCB enCpu, int ciclosEnCpu) {
        PCB candidato = listos.primero();
        return candidato != null && politica.debeExpropiar(enCpu, candidato, ciclosEnCpu);
    }

    /** Cambia la política en ejecución y reordena la cola de listos con el criterio nuevo. */
    public void cambiarPolitica(PoliticaPlanificador nueva) {
        this.politica = nueva;
        listos.ordenar(nueva.getCriterioOrden());
    }

    public PoliticaPlanificador getPolitica() {
        return politica;
    }

    private long siguienteOrden() {
        mutexOrden.acquireUninterruptibly();
        try {
            return contadorOrden++;
        } finally {
            mutexOrden.release();
        }
    }
}
