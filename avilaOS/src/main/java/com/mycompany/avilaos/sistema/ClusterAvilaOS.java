package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.so.FabricaProcesos;
import com.mycompany.avilaos.so.RegistroEventos;

/**
 * El sistema distribuido completo: reloj global + N computadores + log de eventos.
 * Es la "fachada" que usará la GUI: la GUI observa y le da órdenes, pero no contiene lógica.
 */
public class ClusterAvilaOS {

    private final ConfiguracionCluster configuracion;
    private final RelojGlobal reloj;
    private final RegistroEventos registro = new RegistroEventos();
    private final Computador[] computadores;

    public ClusterAvilaOS(ConfiguracionCluster configuracion) {
        this.configuracion = configuracion;
        this.reloj = new RelojGlobal(configuracion.getDuracionCicloMs());
        int n = configuracion.getNumComputadores();
        this.computadores = new Computador[n];
        for (int i = 0; i < n; i++) {
            computadores[i] = new Computador(i + 1, configuracion.getRam(i),
                    configuracion.getPolitica(i), configuracion.getQuantum(), registro);
            reloj.registrar(computadores[i]);
        }
    }

    /**
     * Crea el proceso y lo envía a su computador (manual o automático).
     *
     * @throws IllegalArgumentException si los datos no son válidos (la GUI muestra el mensaje)
     */
    public PCB crearProceso(EspecificacionProceso esp) {
        String error = esp.validar();
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        int destino = esp.idComputador() == EspecificacionProceso.AUTOMATICO
                ? elegirComputadorMenosCargado()
                : esp.idComputador();
        if (destino < 1 || destino > computadores.length) {
            throw new IllegalArgumentException("No existe el computador " + destino);
        }
        PCB pcb = FabricaProcesos.crear(esp);
        computadores[destino - 1].recibirProceso(pcb);
        return pcb;
    }

    /** Balanceo de carga simple (opcional según el enunciado): el computador con menos procesos vivos. */
    private int elegirComputadorMenosCargado() {
        int mejor = 0;
        for (int i = 1; i < computadores.length; i++) {
            if (computadores[i].getKernel().getCarga() < computadores[mejor].getKernel().getCarga()) {
                mejor = i;
            }
        }
        return mejor + 1;
    }

    public void iniciar() { reloj.iniciar(); }
    public void pausar() { reloj.pausar(); }
    public void reanudar() { reloj.reanudar(); }
    public void detener() { reloj.detener(); }

    public void setDuracionCicloMs(int ms) {
        configuracion.setDuracionCicloMs(ms);
        reloj.setDuracionCicloMs(ms);
    }

    public void setQuantum(int quantum) {
        configuracion.setQuantum(quantum);
        for (Computador c : computadores) {
            c.setQuantum(quantum);
        }
    }

    /** Computador por su número (1..N). */
    public Computador getComputador(int id) { return computadores[id - 1]; }
    public int getNumComputadores() { return computadores.length; }
    public RelojGlobal getReloj() { return reloj; }
    public RegistroEventos getRegistro() { return registro; }
    public ConfiguracionCluster getConfiguracion() { return configuracion; }
}
