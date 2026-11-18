package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sistema.ConfiguracionSistema;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * El contenido del JSON de procesos y configuración: duración del ciclo, tamaño del swap, política de disco inicial,
 * la RAM y la política de CPU del computador, y la lista de procesos a crear. Son solo datos validados.
 */
public final class EscenarioProcesos {

    /** Valor de "diskBatchWindow" cuando el JSON no lo trae. */
    public static final int VENTANA_NO_DEFINIDA = -1;

    private final int duracionCicloMs;
    private final int bloquesSwap;
    private final PoliticaPlanificacionDisco politicaDisco;
    private final int ram;
    private final PoliticaPlanificacion politicaCpu;
    private final int quantum;
    private final int ventanaAgrupacion;
    private final EspecificacionProceso[] procesos;

    public EscenarioProcesos(int duracionCicloMs, int bloquesSwap, PoliticaPlanificacionDisco politicaDisco, int ram,
            PoliticaPlanificacion politicaCpu, int quantum, int ventanaAgrupacion, EspecificacionProceso[] procesos) {
        this.duracionCicloMs = duracionCicloMs;
        this.bloquesSwap = bloquesSwap;
        this.politicaDisco = politicaDisco;
        this.ram = ram;
        this.politicaCpu = politicaCpu;
        this.quantum = quantum;
        this.ventanaAgrupacion = ventanaAgrupacion;
        this.procesos = procesos.clone();
    }

    public int getDuracionCicloMs() { return duracionCicloMs; }
    public int getBloquesSwap() { return bloquesSwap; }
    public PoliticaPlanificacionDisco getPoliticaDisco() { return politicaDisco; }
    public int getRam() { return ram; }
    public PoliticaPlanificacion getPoliticaCpu() { return politicaCpu; }
    public int getQuantum() { return quantum; }
    public int getVentanaAgrupacion() { return ventanaAgrupacion; }
    public EspecificacionProceso[] getProcesos() { return procesos.clone(); }

    /** Copia a la configuración lo que este archivo define. El quantum 0 (política sin quantum) conserva el actual. */
    public void aplicarEn(ConfiguracionSistema cfg) {
        cfg.setDuracionCicloMs(duracionCicloMs);
        cfg.setBloquesSwap(bloquesSwap);
        cfg.setPoliticaDisco(politicaDisco);
        cfg.setRam(ram);
        cfg.setPoliticaCpu(politicaCpu);
        if (quantum >= 1) {
            cfg.setQuantum(quantum);
        }
        if (ventanaAgrupacion != VENTANA_NO_DEFINIDA) {
            cfg.setVentanaAgrupacion(ventanaAgrupacion);
        }
    }

    /** Devuelve el primer problema del contenido, o null si es válido. */
    public String validar() {
        if (duracionCicloMs < ConfiguracionSistema.CICLO_MIN_MS || duracionCicloMs > ConfiguracionSistema.CICLO_MAX_MS) {
            return "globalConfig.cycleDurationMs debe estar entre " + ConfiguracionSistema.CICLO_MIN_MS + " y "
                    + ConfiguracionSistema.CICLO_MAX_MS;
        }
        if (bloquesSwap < ConfiguracionSistema.SWAP_MIN || bloquesSwap > ConfiguracionSistema.SWAP_MAX) {
            return "globalConfig.swapSizeBlocks debe estar entre " + ConfiguracionSistema.SWAP_MIN + " y "
                    + ConfiguracionSistema.SWAP_MAX;
        }
        if (ram < ConfiguracionSistema.RAM_MIN || ram > ConfiguracionSistema.RAM_MAX) {
            return "computer.ramCapacity debe estar entre " + ConfiguracionSistema.RAM_MIN + " y "
                    + ConfiguracionSistema.RAM_MAX;
        }
        if (quantum < 0 || quantum > ConfiguracionSistema.QUANTUM_MAX) {
            return "computer.quantum debe estar entre 0 y " + ConfiguracionSistema.QUANTUM_MAX;
        }
        if (politicaCpu == PoliticaPlanificacion.ROUND_ROBIN && quantum < 1) {
            return "computer.quantum debe ser al menos 1 con la política Round Robin";
        }
        if (ventanaAgrupacion != VENTANA_NO_DEFINIDA
                && (ventanaAgrupacion < 0 || ventanaAgrupacion > ConfiguracionSistema.VENTANA_MAX)) {
            return "globalConfig.diskBatchWindow debe estar entre 0 y " + ConfiguracionSistema.VENTANA_MAX;
        }
        for (int i = 0; i < procesos.length; i++) {
            String error = procesos[i].validar();
            if (error != null) {
                return "processes[" + (i + 1) + "] (" + procesos[i].nombre() + "): " + error;
            }
            if (procesos[i].memoria() > ram) {
                return "processes[" + (i + 1) + "] (" + procesos[i].nombre() + "): necesita " + procesos[i].memoria()
                        + " de memoria y la RAM del computador es " + ram;
            }
        }
        return null;
    }

    public boolean tieneProcesosDeArchivo() {
        for (EspecificacionProceso p : procesos) {
            if (p.tipo() == TipoProceso.ES_ARCHIVO) {
                return true;
            }
        }
        return false;
    }
}
