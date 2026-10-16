package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.hardware.CPU;
import com.mycompany.avilaos.hardware.ComponenteRelojable;
import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.so.Kernel;
import com.mycompany.avilaos.so.RegistroEventos;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Un computador completo: ensambla hardware (CPU, RAM) + su propio kernel.
 * Todos los computadores del cluster son instancias de ESTA clase; agregar uno
 * más es solo crear otra instancia.
 */
public class Computador implements ComponenteRelojable {

    private final int id;
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private final Kernel kernel;

    public Computador(int id, int tamanoRam, PoliticaPlanificacion politica, int quantum,
            RegistroEventos registro) {
        this.id = id;
        this.ram = new MemoriaPrincipal(tamanoRam);
        this.cpu = new CPU();
        this.kernel = new Kernel(id, ram, cpu, registro, politica, quantum);
    }

    /** Un ciclo de este computador: el SO decide, el CPU ejecuta una instrucción, el SO procesa el resultado. */
    @Override
    public void alCiclo(long ciclo) {
        kernel.faseInicio(ciclo);
        cpu.alCiclo(ciclo);
        kernel.faseFin(ciclo);
    }

    public boolean recibirProceso(PCB pcb) { return kernel.recibirProceso(pcb); }

    public void cambiarPolitica(PoliticaPlanificacion politica) { kernel.cambiarPolitica(politica); }

    public void setQuantum(int quantum) { kernel.setQuantum(quantum); }

    public int getId() { return id; }
    public MemoriaPrincipal getRam() { return ram; }
    public CPU getCpu() { return cpu; }
    public Kernel getKernel() { return kernel; }
}
