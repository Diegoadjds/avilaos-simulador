package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.modelo.ContextoEjecucion;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.ResultadoInstruccion;

/**
 * Procesador: es hardware, no decide nada. El kernel le carga un proceso y en cada
 * ciclo el CPU ejecuta UNA instrucción de ese proceso y guarda el resultado
 * para que el kernel lo lea.
 */
public class CPU implements ComponenteRelojable {

    private volatile PCB procesoActual;
    private volatile ModoCPU modo = ModoCPU.SISTEMA_OPERATIVO;
    private volatile ResultadoInstruccion ultimoResultado;
    private volatile int ciclosEnCpu;
    private ContextoEjecucion contexto;

    /** El kernel se registra como contexto al construirse. */
    public void setContexto(ContextoEjecucion contexto) {
        this.contexto = contexto;
    }

    public void cargar(PCB pcb) {
        this.procesoActual = pcb;
        this.ciclosEnCpu = 0;
        this.ultimoResultado = null;
    }

    public PCB descargar() {
        PCB p = procesoActual;
        procesoActual = null;
        ciclosEnCpu = 0;
        return p;
    }

    @Override
    public void alCiclo(long ciclo) {
        ultimoResultado = null;
        PCB p = procesoActual;
        if (p == null) {
            modo = ModoCPU.SISTEMA_OPERATIVO; // CPU ociosa: corre el ciclo ocioso del SO
            return;
        }
        ResultadoInstruccion r = p.getComportamiento().ejecutarInstruccion(p, contexto);
        p.avanzarInstruccion();
        ciclosEnCpu++;
        modo = r.getModo();
        ultimoResultado = r;
    }

    public PCB getProcesoActual() { return procesoActual; }
    public ModoCPU getModo() { return modo; }
    public ResultadoInstruccion getUltimoResultado() { return ultimoResultado; }
    /** Ciclos consecutivos del proceso actual en CPU (para el quantum de Round Robin). */
    public int getCiclosEnCpu() { return ciclosEnCpu; }
}
