package com.mycompany.avilaos.so.planificacion;

import com.mycompany.avilaos.estructuras.Comparador;
import com.mycompany.avilaos.modelo.PCB;

/** Earliest Deadline First (apropiativa): gana el que tenga menos deadline restante. */
public class PoliticaEDF implements PoliticaPlanificador {

    private static final Comparador<PCB> ORDEN = (a, b) -> {
        int c = Integer.compare(a.getDeadlineRestante(), b.getDeadlineRestante());
        return c != 0 ? c : Long.compare(a.getOrdenCola(), b.getOrdenCola());
    };

    @Override
    public PoliticaPlanificacion getTipo() { return PoliticaPlanificacion.EDF; }

    @Override
    public Comparador<PCB> getCriterioOrden() { return ORDEN; }

    @Override
    public boolean debeExpropiar(PCB enCpu, PCB candidato, int ciclosEnCpu) {
        return candidato.getDeadlineRestante() < enCpu.getDeadlineRestante();
    }
}
