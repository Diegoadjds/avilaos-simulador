package com.mycompany.avilaos.sincronizacion;

import com.mycompany.avilaos.modelo.ComportamientoProceso;
import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;

/**
 * Consumidor del Anexo, convertido en máquina de estados (1 instrucción por ciclo):
 *   [acceso remoto: bloqueo de latenciaRed ciclos]
 *   semWait(llenos); semWait(mutex); extraer(); semSignal(mutex); semSignal(vacíos)   (modo SO)
 *   consumir() x ciclosPorElemento   (usuario)
 */
public class ComportamientoConsumidor implements ComportamientoProceso {

    private enum Paso { ACCESO_REMOTO, WAIT_LLENOS, WAIT_MUTEX, EXTRAER, SIGNAL_MUTEX, SIGNAL_VACIOS, CONSUMIR }

    private final BufferAcotado buffer;
    private final boolean remoto;
    private final ProveedorLatencia red;
    private Paso paso;
    private int trabajo = 0;
    private volatile String ultima = "—"; // última instrucción ejecutada (la lee la interfaz)

    public ComportamientoConsumidor(BufferAcotado buffer, boolean remoto, ProveedorLatencia red) {
        this.buffer = buffer;
        this.remoto = remoto;
        this.red = red;
        this.paso = inicioDeCiclo();
    }

    @Override
    public String ultimaInstruccion(PCB pcb) {
        return ultima;
    }

    private Paso inicioDeCiclo() {
        return remoto ? Paso.ACCESO_REMOTO : Paso.WAIT_LLENOS;
    }

    /**
     * Deadline vencido a mitad del ciclo: devuelve lo que el consumidor tenía tomado.
     * "paso" es la PRÓXIMA instrucción, así que indica cuántos semWait ya se hicieron.
     * Si todavía no extrajo, el elemento que había reservado (llenos) queda disponible para otro.
     */
    @Override
    public void liberarRecursos(PCB pcb, boolean esperabaEnCola, ContextoEjecucion ctx) {
        switch (paso) {
            case WAIT_MUTEX -> {          // ya hizo semWait(llenos)
                if (!esperabaEnCola) {
                    ComportamientoProductor.soltar(buffer.getLlenos(), pcb, ctx, buffer);
                }
            }
            case EXTRAER -> {             // ya hizo semWait(mutex), y antes semWait(llenos)
                if (!esperabaEnCola) {
                    ComportamientoProductor.soltar(buffer.getMutex(), pcb, ctx, buffer);
                }
                ComportamientoProductor.soltar(buffer.getLlenos(), pcb, ctx, buffer);
            }
            case SIGNAL_MUTEX -> {        // ya sacó el elemento: se completa el ciclo
                ComportamientoProductor.soltar(buffer.getMutex(), pcb, ctx, buffer);
                ComportamientoProductor.soltar(buffer.getVacios(), pcb, ctx, buffer);
            }
            case SIGNAL_VACIOS -> ComportamientoProductor.soltar(buffer.getVacios(), pcb, ctx, buffer);
            default -> {
                // ACCESO_REMOTO, WAIT_LLENOS y CONSUMIR: todavía no había tomado nada
            }
        }
        paso = inicioDeCiclo();
    }

    @Override
    public ResultadoInstruccion ejecutarInstruccion(PCB pcb, ContextoEjecucion ctx) {
        switch (paso) {
            case ACCESO_REMOTO:
                paso = Paso.WAIT_LLENOS;
                ultima = "acceso remoto";
                return ComportamientoProductor.accesoRemoto(pcb, ctx, buffer, red);
            case WAIT_LLENOS:
                paso = Paso.WAIT_MUTEX;
                ultima = "semWait(llenos)";
                return ComportamientoProductor.esperar(buffer.getLlenos(), pcb, ctx, buffer);
            case WAIT_MUTEX:
                paso = Paso.EXTRAER;
                ultima = "semWait(mutex)";
                return ComportamientoProductor.esperar(buffer.getMutex(), pcb, ctx, buffer);
            case EXTRAER:
                buffer.extraer();
                pcb.incrementarElementosProcesados();
                ctx.registrarEvento("Proceso " + pcb + " consume un elemento de " + buffer.getNombre()
                        + " (" + buffer.getElementos() + "/" + buffer.getCapacidad() + ")");
                paso = Paso.SIGNAL_MUTEX;
                ultima = "extraer";
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
            case SIGNAL_MUTEX:
                buffer.getMutex().semSignal();
                paso = Paso.SIGNAL_VACIOS;
                ultima = "semSignal(mutex)";
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
            case SIGNAL_VACIOS:
                buffer.getVacios().semSignal();
                paso = Paso.CONSUMIR;
                ultima = "semSignal(vacíos)";
                return ResultadoInstruccion.continuar(ModoCPU.SISTEMA_OPERATIVO);
            default: // CONSUMIR
                trabajo++;
                ultima = "consumir (" + trabajo + "/" + pcb.getCiclosPorElemento() + ")";
                if (trabajo >= pcb.getCiclosPorElemento()) {
                    trabajo = 0;
                    paso = inicioDeCiclo();
                    if (pcb.getElementosProcesados() >= pcb.getElementosRequeridos()) {
                        return ResultadoInstruccion.terminar(ModoCPU.USUARIO);
                    }
                }
                return ResultadoInstruccion.continuar(ModoCPU.USUARIO);
        }
    }
}
