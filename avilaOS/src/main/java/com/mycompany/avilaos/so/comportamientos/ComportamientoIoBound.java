package com.mycompany.avilaos.so.comportamientos;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.MotivoBloqueo;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;

/**
 * Proceso muy consumidor de E/S: cada "ciclosEntreEs" instrucciones solicita una E/S
 * (instrucción en modo SO) y queda bloqueado "duracionEs" ciclos.
 */
public class ComportamientoIoBound implements ComportamientoProceso {

    @Override
    public ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto) {
        if (pcb.getCiclosEntreEs() <= 0) {
            return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
        }
        pcb.incrementarInstruccionesDesdeEs();
        if (pcb.getInstruccionesDesdeEs() >= pcb.getCiclosEntreEs()) {
            pcb.reiniciarInstruccionesDesdeEs();
            contexto.registrarEvento("Proceso " + pcb + " solicita E/S (" + pcb.getDuracionEs() + " ciclos)");
            return ResultadoInstruccion.bloquearPorTiempo(
                    ModoCPU.SISTEMA_OPERATIVO, MotivoBloqueo.ENTRADA_SALIDA, pcb.getDuracionEs());
        }
        return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
    }
}
