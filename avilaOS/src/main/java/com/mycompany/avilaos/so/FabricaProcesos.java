package com.mycompany.avilaos.so;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.sincronizacion.ComportamientoConsumidor;
import com.mycompany.avilaos.sincronizacion.ComportamientoProductor;
import com.mycompany.avilaos.sincronizacion.ProveedorLatencia;
import com.mycompany.avilaos.so.comportamientos.ComportamientoCpuBound;
import com.mycompany.avilaos.so.comportamientos.ComportamientoIoBound;

/** Construye el PCB (con su comportamiento) a partir de lo que definió el usuario. */
public final class FabricaProcesos {

    private FabricaProcesos() {
    }

    /** Procesos CPU bound e I/O bound. */
    public static PCB crear(EspecificacionProceso esp) {
        ComportamientoProceso comportamiento = switch (esp.tipo()) {
            case CPU_BOUND -> new ComportamientoCpuBound();
            case IO_BOUND -> new ComportamientoIoBound();
            case PRODUCTOR, CONSUMIDOR -> throw new IllegalArgumentException(
                    "Un productor/consumidor necesita un buffer: use crear(esp, buffer, remoto, red)");
        };
        return new PCB(esp, comportamiento);
    }

    /**
     * Productor o consumidor. Su cantidad de instrucciones se calcula sola (la que se
     * escriba en la especificación se ignora): por elemento son ciclosPorElemento de
     * usuario + 5 operaciones de SO (+1 si el acceso es remoto).
     */
    public static PCB crear(EspecificacionProceso esp, BufferAcotado buffer, boolean remoto,
            ProveedorLatencia red) {
        int porElemento = esp.ciclosPorElemento() + 5 + (remoto ? 1 : 0);
        EspecificacionProceso ajustada = new EspecificacionProceso(esp.nombre(), esp.tipo(),
                esp.elementosRequeridos() * porElemento, esp.memoria(), esp.prioridad(), esp.deadline(),
                0, 0, esp.idBuffer(), esp.ciclosPorElemento(), esp.elementosRequeridos(), esp.idComputador());
        ComportamientoProceso comportamiento = esp.tipo() == TipoProceso.PRODUCTOR
                ? new ComportamientoProductor(buffer, remoto, red)
                : new ComportamientoConsumidor(buffer, remoto, red);
        return new PCB(ajustada, comportamiento);
    }
}
