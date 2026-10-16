package com.mycompany.avilaos.so;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.so.comportamientos.ComportamientoCpuBound;
import com.mycompany.avilaos.so.comportamientos.ComportamientoIoBound;

/** Construye el PCB (con su comportamiento) a partir de lo que definió el usuario. */
public final class FabricaProcesos {

    private FabricaProcesos() {
    }

    public static PCB crear(EspecificacionProceso esp) {
        ComportamientoProceso comportamiento = switch (esp.tipo()) {
            case CPU_BOUND -> new ComportamientoCpuBound();
            case IO_BOUND -> new ComportamientoIoBound();
            // TODO (sincronización): crear ComportamientoProductor / ComportamientoConsumidor
            // (implementan ComportamientoProceso) y devolverlos aquí.
            case PRODUCTOR, CONSUMIDOR -> throw new UnsupportedOperationException(
                    "Productor/consumidor aún no implementado (módulo de sincronización)");
        };
        return new PCB(esp, comportamiento);
    }
}
