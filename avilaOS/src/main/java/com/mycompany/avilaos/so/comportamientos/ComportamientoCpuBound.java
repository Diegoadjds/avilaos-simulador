package com.mycompany.avilaos.so.comportamientos;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;

/** Proceso muy consumidor de CPU: todas sus instrucciones son de usuario y nunca se bloquea. */
public class ComportamientoCpuBound implements ComportamientoProceso {

    @Override
    public ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto) {
        return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
    }
}
