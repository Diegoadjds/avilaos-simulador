package com.mycompany.avilaos.so.comportamientos;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * Proceso de E/S de archivos. Su primera instrucción es la llamada al sistema (modo SO) que pide al disco la
 * operación CRUD sobre la ruta del PCB: el proceso queda Bloqueado por E/S y libera la CPU. Cuando el disco
 * termina y el proceso vuelve a ejecutar, el resto de sus instrucciones son de usuario (procesar el resultado).
 * Si el sistema de archivos rechaza la operación (ruta inexistente, sin permiso, sin bloques libres...), el
 * proceso termina con ese error y la simulación sigue.
 */
public class ComportamientoArchivo implements ComportamientoProceso {

    @Override
    public ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion contexto) {
        if (pcb.getPc() > 0) {
            return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
        }
        try {
            SolicitudDisco solicitud = contexto.solicitarArchivo(pcb);
            contexto.registrarEvento("Proceso " + pcb + " pide al disco " + pcb.getOperacion() + " "
                    + pcb.getRutaArchivo() + (solicitud.getCantidadBloques() > 0
                    ? " (" + solicitud.getCantidadBloques() + " bloque(s), primero: " + solicitud.getBloqueObjetivo() + ")"
                    : " (solo metadatos)"));
            return ResultadoInstruccion.bloquearPorDisco(ModoCPU.SISTEMA_OPERATIVO);
        } catch (ErrorOperacion e) {
            pcb.setErrorES(e.getMessage());
            contexto.registrarEvento("Proceso " + pcb + " falló al pedir " + pcb.getOperacion() + " "
                    + pcb.getRutaArchivo() + ": " + e.getMessage());
            return ResultadoInstruccion.terminar(ModoCPU.SISTEMA_OPERATIVO);
        }
    }

    @Override
    public String ultimaInstruccion(PCB pcb) {
        if (pcb.getPc() == 0) {
            return "—";
        }
        if (pcb.getPc() == 1) {
            return "llamada al sistema: " + pcb.getOperacion() + " " + pcb.getRutaArchivo();
        }
        return "procesa el resultado (instrucción " + pcb.getPc() + " de " + pcb.getInstruccionesTotales() + ")";
    }
}
