package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.so.FabricaProcesos;
import com.mycompany.avilaos.so.RegistroEventos;

/**
 * El sistema distribuido completo: reloj global + N computadores + buffers + log de eventos.
 * Es la "fachada" que usa la GUI: la GUI observa y le da órdenes, pero no contiene lógica.
 */
public class ClusterAvilaOS {

    private final ConfiguracionCluster configuracion;
    private final RelojGlobal reloj;
    private final RegistroEventos registro = new RegistroEventos();
    private final Computador[] computadores;
    private final ListaSegura<BufferAcotado> buffers = new ListaSegura<>();
    private int siguienteIdBuffer = 1;

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

    // =====================================================================
    //  Creación de buffers y procesos
    // =====================================================================

    /**
     * Crea un buffer en la RAM del computador anfitrión.
     *
     * @throws IllegalArgumentException si los datos no son válidos o no hay RAM libre
     */
    public synchronized BufferAcotado crearBuffer(int capacidad, int idComputador) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad del buffer debe ser mayor que 0");
        }
        if (idComputador < 1 || idComputador > computadores.length) {
            throw new IllegalArgumentException("No existe el computador " + idComputador);
        }
        Computador host = computadores[idComputador - 1];
        int libre = host.getRam().getLibre();
        if (!host.getRam().reservar(capacidad)) {
            throw new IllegalArgumentException("El computador " + idComputador
                    + " no tiene RAM libre suficiente (libre: " + libre + ", necesita: " + capacidad + ")");
        }
        BufferAcotado buffer = new BufferAcotado(siguienteIdBuffer++, capacidad, idComputador);
        buffers.agregarAlFinal(buffer);
        registro.registrar(reloj.getCicloActual(), "Se crea el buffer " + buffer.getNombre()
                + " (capacidad " + capacidad + ") en el Computador " + idComputador);
        return buffer;
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
                ? elegirComputadorMenosCargado(esp.memoria())
                : esp.idComputador();
        if (destino < 1 || destino > computadores.length) {
            throw new IllegalArgumentException("No existe el computador " + destino);
        }
        PCB pcb;
        if (esp.tipo() == TipoProceso.PRODUCTOR || esp.tipo() == TipoProceso.CONSUMIDOR) {
            BufferAcotado buffer = getBuffer(esp.idBuffer());
            if (buffer == null) {
                throw new IllegalArgumentException("No existe el buffer B" + esp.idBuffer());
            }
            boolean remoto = buffer.getIdComputadorAnfitrion() != destino;
            pcb = FabricaProcesos.crear(esp, buffer, remoto, configuracion);
        } else {
            pcb = FabricaProcesos.crear(esp);
        }
        if (!computadores[destino - 1].recibirProceso(pcb)) {
            throw new IllegalArgumentException("El proceso necesita " + esp.memoria()
                    + " de memoria y la RAM total del computador " + destino + " es menor");
        }
        return pcb;
    }

    /** Balanceo de carga simple (opcional según el enunciado): el computador con menos procesos vivos y RAM suficiente. */
    private int elegirComputadorMenosCargado(int memoria) {
        int mejor = -1;
        for (int i = 0; i < computadores.length; i++) {
            if (computadores[i].getRam().getTotal() < memoria) {
                continue;
            }
            if (mejor < 0 || computadores[i].getKernel().getCarga() < computadores[mejor].getKernel().getCarga()) {
                mejor = i;
            }
        }
        if (mejor < 0) {
            throw new IllegalArgumentException("Ningún computador tiene RAM suficiente para " + memoria);
        }
        return mejor + 1;
    }

    // =====================================================================
    //  Control del reloj y parámetros en ejecución
    // =====================================================================

    public void iniciar() { reloj.iniciar(); }
    public void iniciarPausado() { reloj.iniciar(true); }
    public void pausar() { reloj.pausar(); }
    public void reanudar() { reloj.reanudar(); }
    public void avanzarUnCiclo() { reloj.avanzarUnCiclo(); }
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

    public void setLatenciaRed(int ciclos) { configuracion.setLatenciaRed(ciclos); }

    // =====================================================================
    //  Consultas
    // =====================================================================

    /** Computador por su número (1..N). */
    public Computador getComputador(int id) { return computadores[id - 1]; }
    public Computador[] getComputadores() { return computadores; }
    public int getNumComputadores() { return computadores.length; }
    public RelojGlobal getReloj() { return reloj; }
    public RegistroEventos getRegistro() { return registro; }
    public ConfiguracionCluster getConfiguracion() { return configuracion; }

    public BufferAcotado[] getBuffers() { return buffers.aArreglo(new BufferAcotado[0]); }

    /** Buffer por su número (B1 = 1) o null si no existe. */
    public BufferAcotado getBuffer(int id) {
        for (BufferAcotado b : getBuffers()) {
            if (b.getId() == id) {
                return b;
            }
        }
        return null;
    }

    public MetricasGlobales getMetricasGlobales() {
        return MetricasGlobales.calcular(computadores, reloj.getCicloActual());
    }
}
