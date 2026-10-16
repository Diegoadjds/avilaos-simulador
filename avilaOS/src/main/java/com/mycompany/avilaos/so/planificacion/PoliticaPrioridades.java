package com.mycompany.avilaos.so.planificacion;

import com.mycompany.avilaos.estructuras.Comparador;
import com.mycompany.avilaos.modelo.PCB;

/** Prioridades apropiativas: número menor = prioridad más alta. Empates por orden de llegada. */
public class PoliticaPrioridades implements PoliticaPlanificador {

    private static final Comparador<PCB> ORDEN = (a, b) -> {
        int c = Integer.compare(a.getPrioridad(), b.getPrioridad());
        return c != 0 ? c : Long.compare(a.getOrdenCola(), b.getOrdenCola());
    };

    @Override
    public PoliticaPlanificacion getTipo() { return PoliticaPlanificacion.PRIORIDADES; }

    @Override
    public Comparador<PCB> getCriterioOrden() { return ORDEN; }

    @Override
    public boolean debeExpropiar(PCB enCpu, PCB candidato, int ciclosEnCpu) {
        return candidato.getPrioridad() < enCpu.getPrioridad();
    }
}
