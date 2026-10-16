package com.mycompany.avilaos.so.planificacion;

import com.mycompany.avilaos.estructuras.Comparador;
import com.mycompany.avilaos.modelo.PCB;

/** First Come First Served: por orden de llegada a la cola de listos, sin expropiación. */
public class PoliticaFCFS implements PoliticaPlanificador {

    private static final Comparador<PCB> ORDEN = (a, b) -> Long.compare(a.getOrdenCola(), b.getOrdenCola());

    @Override
    public PoliticaPlanificacion getTipo() { return PoliticaPlanificacion.FCFS; }

    @Override
    public Comparador<PCB> getCriterioOrden() { return ORDEN; }

    @Override
    public boolean debeExpropiar(PCB enCpu, PCB candidato, int ciclosEnCpu) { return false; }
}
