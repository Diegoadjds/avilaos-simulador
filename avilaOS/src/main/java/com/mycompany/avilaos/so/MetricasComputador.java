package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.modelo.PCB;

/**
 * Métricas de UN computador. Solo las escribe el hilo de ese computador (por eso los
 * contadores volatile son suficientes) y las lee la GUI.
 * Pendiente: equidad (por ejemplo, índice de Jain) y métricas del productor-consumidor por buffer.
 */
public class MetricasComputador {

    /** Cada cuántos ciclos se guarda un punto del historial de utilización (para la gráfica). */
    private static final int VENTANA = 10;

    private volatile long ciclosTotales;
    private volatile long ciclosOcupada;
    private volatile long completados;
    private volatile long vencidos;
    private volatile long sumaRespuesta;
    private volatile long conRespuesta;
    private volatile long sumaBloqueoSemaforo;
    private volatile long bloqueosSemaforo;

    private int ocupadosVentana;
    private int ciclosVentana;
    private final ListaSegura<Double> historialUtilizacion = new ListaSegura<>();

    public void registrarCiclo(boolean cpuOcupada) {
        ciclosTotales++;
        if (cpuOcupada) {
            ciclosOcupada++;
            ocupadosVentana++;
        }
        ciclosVentana++;
        if (ciclosVentana == VENTANA) {
            historialUtilizacion.agregarAlFinal((double) ocupadosVentana / VENTANA);
            ocupadosVentana = 0;
            ciclosVentana = 0;
        }
    }

    public void registrarTerminacion(PCB pcb) {
        if (pcb.isTerminoPorDeadline()) {
            vencidos++;
        } else {
            completados++;
        }
    }

    public void registrarRespuesta(long ciclos) {
        sumaRespuesta += ciclos;
        conRespuesta++;
    }

    public void registrarBloqueoSemaforo(long ciclos) {
        sumaBloqueoSemaforo += ciclos;
        bloqueosSemaforo++;
    }

    /** Fracción de ciclos con el CPU ocupado (0..1). */
    public double getUtilizacion() {
        return ciclosTotales == 0 ? 0 : (double) ciclosOcupada / ciclosTotales;
    }

    /** Procesos completados por ciclo de reloj. */
    public double getThroughput() {
        return ciclosTotales == 0 ? 0 : (double) completados / ciclosTotales;
    }

    /** Ciclos promedio entre la llegada de un proceso y su primera ejecución. */
    public double getTiempoRespuestaPromedio() {
        return conRespuesta == 0 ? 0 : (double) sumaRespuesta / conRespuesta;
    }

    /** Fracción de procesos finalizados que terminaron antes de vencer su deadline. */
    public double getTasaCumplimientoDeadline() {
        long total = completados + vencidos;
        return total == 0 ? 1.0 : (double) completados / total;
    }

    public double getTiempoPromedioBloqueoSemaforo() {
        return bloqueosSemaforo == 0 ? 0 : (double) sumaBloqueoSemaforo / bloqueosSemaforo;
    }

    public long getCiclosTotales() { return ciclosTotales; }
    public long getCompletados() { return completados; }
    public long getVencidos() { return vencidos; }

    /** Utilización por ventana de 10 ciclos: los puntos de la gráfica de este computador. */
    public Double[] getHistorialUtilizacion() {
        return historialUtilizacion.aArreglo(new Double[0]);
    }
}
