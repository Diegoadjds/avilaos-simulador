package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.sistema.ConfiguracionCluster;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Un escenario de simulación: todo lo que hace falta para repetir una simulación.
 * Es solo datos (sin hilos ni lógica del sistema operativo) y es lo que se escribe y se lee
 * del archivo JSON:
 *
 *   - duración del ciclo, latencia de red y quantum,
 *   - número de computadores, y para cada uno su RAM y su política de planificación inicial,
 *   - la carga inicial: buffers (capacidad y anfitrión) y procesos (con todos sus atributos).
 *
 * Es inmutable. {@link #validar()} comprueba tipos, rangos y que las referencias sean coherentes
 * (por ejemplo, que un productor apunte a un buffer que existe).
 */
public final class Escenario {

    public static final int VERSION = 1;

    public static final int MIN_COMPUTADORES = 2;
    public static final int MAX_COMPUTADORES = 6;
    public static final int CICLO_MIN_MS = 10;
    public static final int CICLO_MAX_MS = 10000;
    public static final int LATENCIA_MAX = 1000;
    public static final int QUANTUM_MAX = 1000;
    public static final int RAM_MAX = 1_000_000;
    public static final int CAPACIDAD_BUFFER_MAX = 100_000;

    private final int duracionCicloMs;
    private final int latenciaRed;
    private final int quantum;
    private final int[] ram;
    private final PoliticaPlanificacion[] politicas;
    private final BufferEscenario[] buffers;
    private final EspecificacionProceso[] procesos;

    public Escenario(int duracionCicloMs, int latenciaRed, int quantum, int[] ram,
            PoliticaPlanificacion[] politicas, BufferEscenario[] buffers, EspecificacionProceso[] procesos) {
        this.duracionCicloMs = duracionCicloMs;
        this.latenciaRed = latenciaRed;
        this.quantum = quantum;
        this.ram = ram.clone();
        this.politicas = politicas.clone();
        this.buffers = buffers.clone();
        this.procesos = procesos.clone();
    }

    // ------------------------------------------------------------------ consulta

    public int getDuracionCicloMs() { return duracionCicloMs; }
    public int getLatenciaRed() { return latenciaRed; }
    public int getQuantum() { return quantum; }
    public int getNumComputadores() { return ram.length; }

    /** RAM del computador número {@code numero} (1..N). */
    public int getRam(int numero) { return ram[numero - 1]; }

    /** Política inicial del computador número {@code numero} (1..N). */
    public PoliticaPlanificacion getPolitica(int numero) { return politicas[numero - 1]; }

    public BufferEscenario[] getBuffers() { return buffers.clone(); }
    public EspecificacionProceso[] getProcesos() { return procesos.clone(); }

    // ------------------------------------------------------------------ validación

    /** Devuelve el primer problema encontrado (en español, listo para mostrar) o null si es válido. */
    public String validar() {
        int n = ram.length;
        if (n < MIN_COMPUTADORES || n > MAX_COMPUTADORES) {
            return "El número de computadores debe estar entre " + MIN_COMPUTADORES + " y "
                    + MAX_COMPUTADORES + " (el escenario tiene " + n + ")";
        }
        if (politicas.length != n) {
            return "Debe haber exactamente una política por computador";
        }
        if (duracionCicloMs < CICLO_MIN_MS || duracionCicloMs > CICLO_MAX_MS) {
            return "La duración del ciclo debe estar entre " + CICLO_MIN_MS + " y " + CICLO_MAX_MS + " ms";
        }
        if (latenciaRed < 0 || latenciaRed > LATENCIA_MAX) {
            return "La latencia de red debe estar entre 0 y " + LATENCIA_MAX + " ciclos";
        }
        if (quantum < 1 || quantum > QUANTUM_MAX) {
            return "El quantum debe estar entre 1 y " + QUANTUM_MAX + " ciclos";
        }
        for (int i = 0; i < n; i++) {
            if (ram[i] < 1 || ram[i] > RAM_MAX) {
                return "La RAM del computador " + (i + 1) + " debe estar entre 1 y " + RAM_MAX;
            }
            if (politicas[i] == null) {
                return "Falta la política de planificación del computador " + (i + 1);
            }
        }
        String errorCarga = validarBuffers();
        if (errorCarga != null) {
            return errorCarga;
        }
        return validarProcesos();
    }

    private String validarBuffers() {
        int n = ram.length;
        int[] reservada = new int[n];
        for (int i = 0; i < buffers.length; i++) {
            BufferEscenario b = buffers[i];
            String quien = "Buffer B" + (i + 1) + ": ";
            if (b == null) {
                return quien + "datos incompletos";
            }
            if (b.capacidad() < 1 || b.capacidad() > CAPACIDAD_BUFFER_MAX) {
                return quien + "la capacidad debe estar entre 1 y " + CAPACIDAD_BUFFER_MAX;
            }
            if (b.computador() < 1 || b.computador() > n) {
                return quien + "el computador anfitrión debe estar entre 1 y " + n
                        + " (se indicó " + b.computador() + ")";
            }
            reservada[b.computador() - 1] += b.capacidad();
            if (reservada[b.computador() - 1] > ram[b.computador() - 1]) {
                return quien + "los buffers del computador " + b.computador() + " suman "
                        + reservada[b.computador() - 1] + " y su RAM es solo " + ram[b.computador() - 1];
            }
        }
        return null;
    }

    private String validarProcesos() {
        int n = ram.length;
        for (int i = 0; i < procesos.length; i++) {
            EspecificacionProceso p = procesos[i];
            if (p == null) {
                return "Proceso #" + (i + 1) + ": datos incompletos";
            }
            String quien = "Proceso #" + (i + 1) + " (" + p.nombre() + "): ";
            String error = p.validar();
            if (error == null) {
                error = validarLimites(p);
            }
            if (error != null) {
                return quien + error;
            }
            boolean esProdCons = p.tipo() == TipoProceso.PRODUCTOR || p.tipo() == TipoProceso.CONSUMIDOR;
            if (esProdCons && p.idBuffer() > buffers.length) {
                return quien + "usa el buffer B" + p.idBuffer() + " pero el escenario solo tiene "
                        + buffers.length + " buffer(s)";
            }
            if (p.idComputador() == EspecificacionProceso.AUTOMATICO) {
                if (ramMaxima() < p.memoria()) {
                    return quien + "necesita " + p.memoria() + " de memoria y ningún computador tiene tanta RAM";
                }
            } else if (p.idComputador() > n) {
                return quien + "el computador " + p.idComputador() + " no existe (hay " + n + ")";
            } else if (p.memoria() > ram[p.idComputador() - 1]) {
                return quien + "necesita " + p.memoria() + " de memoria y la RAM del computador "
                        + p.idComputador() + " es " + ram[p.idComputador() - 1];
            }
        }
        return null;
    }

    /** Los mismos límites superiores que aplican los diálogos de la interfaz. */
    private static String validarLimites(EspecificacionProceso p) {
        if (p.nombre().trim().length() > 30) {
            return "el nombre admite máximo 30 caracteres";
        }
        if (p.instrucciones() > 1_000_000) {
            return "las instrucciones no pueden pasar de 1000000";
        }
        if (p.memoria() > RAM_MAX) {
            return "la memoria no puede pasar de " + RAM_MAX;
        }
        if (p.prioridad() > 99) {
            return "la prioridad debe estar entre 1 y 99";
        }
        if (p.deadline() > 10_000_000) {
            return "el deadline no puede pasar de 10000000 ciclos";
        }
        if (p.ciclosEntreEs() > 100_000 || p.duracionEs() > 100_000
                || p.ciclosPorElemento() > 100_000 || p.elementosRequeridos() > 100_000) {
            return "los valores de E/S y de productor/consumidor no pueden pasar de 100000";
        }
        return null;
    }

    private int ramMaxima() {
        int max = 0;
        for (int r : ram) {
            max = Math.max(max, r);
        }
        return max;
    }

    // ------------------------------------------------------------------ puente con el simulador

    /**
     * Toma una "foto" de la simulación en curso: su configuración vigente (parámetros y política
     * actual de cada computador) más todos los buffers y procesos creados hasta ahora.
     * Al cargar ese escenario, todos los procesos se crean juntos al principio (ciclo 0).
     */
    public static Escenario capturar(ClusterAvilaOS cluster) {
        ConfiguracionCluster cfg = cluster.getConfiguracion();
        int n = cluster.getNumComputadores();
        int[] ram = new int[n];
        PoliticaPlanificacion[] politicas = new PoliticaPlanificacion[n];
        for (int i = 0; i < n; i++) {
            ram[i] = cluster.getComputador(i + 1).getRam().getTotal();
            politicas[i] = cluster.getComputador(i + 1).getKernel().getPolitica();
        }
        BufferAcotado[] vivos = cluster.getBuffers();
        BufferEscenario[] buffers = new BufferEscenario[vivos.length];
        for (int i = 0; i < vivos.length; i++) {
            buffers[i] = new BufferEscenario(vivos[i].getCapacidad(), vivos[i].getIdComputadorAnfitrion());
        }
        return new Escenario(cfg.getDuracionCicloMs(), cfg.getLatenciaRed(), cfg.getQuantum(),
                ram, politicas, buffers, cluster.getEspecificacionesCreadas());
    }

    /** Configuración del clúster que describe este escenario (aún sin buffers ni procesos). */
    public ConfiguracionCluster crearConfiguracion() {
        ConfiguracionCluster cfg = new ConfiguracionCluster(ram.length);
        cfg.setDuracionCicloMs(duracionCicloMs);
        cfg.setLatenciaRed(latenciaRed);
        cfg.setQuantum(quantum);
        for (int i = 0; i < ram.length; i++) {
            cfg.setRam(i, ram[i]);
            cfg.setPolitica(i, politicas[i]);
        }
        return cfg;
    }

    /** Crea los buffers y luego los procesos de la carga inicial dentro de un clúster ya construido. */
    public void cargarEn(ClusterAvilaOS cluster) {
        for (BufferEscenario b : buffers) {
            cluster.crearBuffer(b.capacidad(), b.computador());
        }
        for (EspecificacionProceso p : procesos) {
            cluster.crearProceso(p);
        }
    }

    /** Construye el clúster completo (configuración + carga inicial), listo para iniciar. */
    public ClusterAvilaOS crearCluster() {
        String error = validar();
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        ClusterAvilaOS cluster = new ClusterAvilaOS(crearConfiguracion());
        cargarEn(cluster);
        return cluster;
    }

    /** Resumen corto para mostrar en la interfaz. */
    public String describirCarga() {
        return buffers.length + " buffer(s) y " + procesos.length + " proceso(s)";
    }
}
