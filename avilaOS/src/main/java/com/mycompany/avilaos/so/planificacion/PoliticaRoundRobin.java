package com.mycompany.avilaos.so.planificacion;

import com.mycompany.avilaos.estructuras.Comparador;
import com.mycompany.avilaos.modelo.PCB;

/** Round Robin: cola FIFO y expropiación al agotarse el quantum (configurable en ejecución). */
public class PoliticaRoundRobin implements PoliticaPlanificador {

    private static final Comparador<PCB> ORDEN = (a, b) -> Long.compare(a.getOrdenCola(), b.getOrdenCola());

    private volatile int quantum;

    public PoliticaRoundRobin(int quantum) {
        this.quantum = Math.max(1, quantum);
    }

    @Override
    public PoliticaPlanificacion getTipo() { return PoliticaPlanificacion.ROUND_ROBIN; }

    @Override
    public Comparador<PCB> getCriterioOrden() { return ORDEN; }

    @Override
    public boolean debeExpropiar(PCB enCpu, PCB candidato, int ciclosEnCpu) {
        return ciclosEnCpu >= quantum;
    }

    @Override
    public int getQuantum() { return quantum; }

    @Override
    public void setQuantum(int quantum) { this.quantum = Math.max(1, quantum); }
}
