package com.mycompany.avilaos.sincronizacion;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.MotivoBloqueo;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;

/**
 * Productor del Anexo, convertido en máquina de estados (1 instrucción por ciclo):
 *   producir() x ciclosPorElemento   (usuario)
 *   [acceso remoto: bloqueo de latenciaRed ciclos]   (solo si el buffer está en otro computador)
 *   semWait(vacíos); semWait(mutex); anyadir(); semSignal(mutex); semSignal(llenos)   (modo SO)
 * El orden de los semWait NO se puede invertir (interbloqueo).
 */
public class ComportamientoProductor implements ComportamientoProceso {

    private enum Paso { PRODUCIR, ACCESO_REMOTO, WAIT_VACIOS, WAIT_MUTEX, ANYADIR, SIGNAL_MUTEX, SIGNAL_LLENOS }

    private final BufferAcotado buffer;
    private final boolean remoto;
    private final ProveedorLatencia red;
    private Paso paso = Paso.PRODUCIR;
    private int trabajo = 0;
    private volatile String ultima = "—"; // última instrucción ejecutada (la lee la interfaz)

    public ComportamientoProductor(BufferAcotado buffer, boolean remoto, ProveedorLatencia red) {
        this.buffer = buffer;
        this.remoto = remoto;
        this.red = red;
    }

    @Override
    public ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion ctx) {
        switch (paso) {
            case PRODUCIR:
                trabajo++;
                ultima = "producir (" + trabajo + "/" + pcb.getCiclosPorElemento() + ")";
                if (trabajo >= pcb.getCiclosPorElemento()) {
                    trabajo = 0;
                    paso = remoto ? Paso.ACCESO_REMOTO : Paso.WAIT_VACIOS;
                }
                return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
            case ACCESO_REMOTO:
                paso = Paso.WAIT_VACIOS;
                ultima = "acceso remoto";
                return accesoRemoto(pcb, ctx, buffer, red);
            case WAIT_VACIOS:
                paso = Paso.WAIT_MUTEX;
                ultima = "semWait(vacíos)";
                return esperar(buffer.getVacios(), pcb, ctx, buffer);
            case WAIT_MUTEX:
                paso = Paso.ANYADIR;
                ultima = "semWait(mutex)";
                return esperar(buffer.getMutex(), pcb, ctx, buffer);
            case ANYADIR:
                buffer.anyadir();
                pcb.incrementarElementosProcesados();
                ctx.registrarEvento("Proceso " + pcb + " produce un elemento en " + buffer.getNombre()
                        + " (" + buffer.getElementos() + "/" + buffer.getCapacidad() + ")");
                paso = Paso.SIGNAL_MUTEX;
                ultima = "anyadir";
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
            case SIGNAL_MUTEX:
                buffer.getMutex().semSignal();
                paso = Paso.SIGNAL_LLENOS;
                ultima = "semSignal(mutex)";
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
            default: // SIGNAL_LLENOS
                buffer.getLlenos().semSignal();
                paso = Paso.PRODUCIR;
                ultima = "semSignal(llenos)";
                if (pcb.getElementosProcesados() >= pcb.getElementosRequeridos()) {
                    return ResultadoInstruccion.terminar(ModoCPU.SISTEMA_OPERATIVO);
                }
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
        }
    }

    @Override
    public String ultimaInstruccion(PCB pcb) {
        return ultima;
    }

    /**
     * Deadline vencido a mitad del ciclo: devuelve lo que el productor tenía tomado.
     * "paso" es la PRÓXIMA instrucción, así que indica cuántos semWait ya se hicieron.
     * Primero se suelta el mutex y después el semáforo de conteo, como en el flujo normal.
     */
    @Override
    public void liberarRecursos(PCB pcb, boolean esperabaEnCola, ContextoEjecucion ctx) {
        switch (paso) {
            case WAIT_MUTEX -> {          // ya hizo semWait(vacíos)
                if (!esperabaEnCola) {
                    soltar(buffer.getVacios(), pcb, ctx, buffer);
                }
            }
            case ANYADIR -> {             // ya hizo semWait(mutex), y antes semWait(vacíos)
                if (!esperabaEnCola) {
                    soltar(buffer.getMutex(), pcb, ctx, buffer);
                }
                soltar(buffer.getVacios(), pcb, ctx, buffer);
            }
            case SIGNAL_MUTEX -> {        // el elemento ya está en el buffer: se completa el ciclo
                soltar(buffer.getMutex(), pcb, ctx, buffer);
                soltar(buffer.getLlenos(), pcb, ctx, buffer);
            }
            case SIGNAL_LLENOS -> soltar(buffer.getLlenos(), pcb, ctx, buffer);
            default -> {
                // PRODUCIR, ACCESO_REMOTO y WAIT_VACIOS: todavía no había tomado nada
            }
        }
        paso = Paso.PRODUCIR;
    }

    // ---- Utilidades compartidas con el consumidor ----

    /** semSignal hecho por el kernel a nombre de un proceso que murió con el semáforo tomado. */
    static void soltar(Semaforo semaforo, PCB pcb, ContextoEjecucion ctx, BufferAcotado buffer) {
        ctx.registrarEvento("Proceso " + pcb + " vence su deadline con el semáforo '" + semaforo.getNombre()
                + "' del buffer " + buffer.getNombre() + " tomado: el sistema operativo lo libera");
        semaforo.semSignal();
    }

    static ResultadoInstruccion accesoRemoto(PCB pcb, ContextoEjecucion ctx, BufferAcotado buffer,
            ProveedorLatencia red) {
        int latencia = red.getLatenciaRed();
        ctx.registrarEvento("Proceso " + pcb + " accede al buffer " + buffer.getNombre()
                + " de forma remota (latencia " + latencia + " ciclos)");
        if (latencia <= 0) {
            return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
        }
        return ResultadoInstruccion.bloquearPorTiempo(ModoCPU.SISTEMA_OPERATIVO,
                MotivoBloqueo.ACCESO_REMOTO, latencia);
    }

    static ResultadoInstruccion esperar(Semaforo semaforo, PCB pcb, ContextoEjecucion ctx, BufferAcotado buffer) {
        if (semaforo.semWait(pcb)) {
            return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
        }
        ctx.registrarEvento("Proceso " + pcb + " se bloquea en el semáforo '" + semaforo.getNombre()
                + "' del buffer " + buffer.getNombre());
        return ResultadoInstruccion.bloquearEnRecurso(ModoCPU.SISTEMA_OPERATIVO, semaforo.getMotivo(), semaforo);
    }
}
