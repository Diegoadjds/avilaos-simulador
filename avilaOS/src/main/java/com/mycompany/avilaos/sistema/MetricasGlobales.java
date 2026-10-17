package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.so.Equidad;
import com.mycompany.avilaos.so.MetricasComputador;

/** Métricas del sistema completo, calculadas a partir de las de cada computador. */
public final class MetricasGlobales {

    private final double utilizacionPromedio;
    private final double throughput;
    private final double tiempoRespuestaPromedio;
    private final double tasaCumplimientoDeadline;
    private final double tiempoPromedioBloqueoSemaforo;
    private final double equidad;
    private final long completados;
    private final long vencidos;

    private MetricasGlobales(double util, double thr, double resp, double cumpl, double bloq,
            double equidad, long completados, long vencidos) {
        this.utilizacionPromedio = util;
        this.throughput = thr;
        this.tiempoRespuestaPromedio = resp;
        this.tasaCumplimientoDeadline = cumpl;
        this.tiempoPromedioBloqueoSemaforo = bloq;
        this.equidad = equidad;
        this.completados = completados;
        this.vencidos = vencidos;
    }

    public static MetricasGlobales calcular(Computador[] computadores, long cicloActual) {
        double sumaUtil = 0;
        long comp = 0, venc = 0, sumaResp = 0, conResp = 0, sumaBloq = 0, bloqueos = 0;
        PCB[][] porComputador = new PCB[computadores.length][];
        int totalProcesos = 0;
        for (int i = 0; i < computadores.length; i++) {
            MetricasComputador m = computadores[i].getKernel().getMetricas();
            sumaUtil += m.getUtilizacion();
            comp += m.getCompletados();
            venc += m.getVencidos();
            sumaResp += m.getSumaRespuesta();
            conResp += m.getConRespuesta();
            sumaBloq += m.getSumaBloqueoSemaforo();
            bloqueos += m.getBloqueosSemaforo();
            porComputador[i] = computadores[i].getKernel().getTodosLosProcesos();
            totalProcesos += porComputador[i].length;
        }
        PCB[] todos = new PCB[totalProcesos];
        int n = 0;
        for (PCB[] arr : porComputador) {
            for (PCB p : arr) {
                todos[n++] = p;
            }
        }
        return new MetricasGlobales(
                computadores.length == 0 ? 0 : sumaUtil / computadores.length,
                cicloActual == 0 ? 0 : (double) comp / cicloActual,
                conResp == 0 ? 0 : (double) sumaResp / conResp,
                (comp + venc) == 0 ? 1.0 : (double) comp / (comp + venc),
                bloqueos == 0 ? 0 : (double) sumaBloq / bloqueos,
                Equidad.jain(todos, cicloActual),
                comp, venc);
    }

    public double getUtilizacionPromedio() { return utilizacionPromedio; }
    public double getThroughput() { return throughput; }
    public double getTiempoRespuestaPromedio() { return tiempoRespuestaPromedio; }
    public double getTasaCumplimientoDeadline() { return tasaCumplimientoDeadline; }
    public double getTiempoPromedioBloqueoSemaforo() { return tiempoPromedioBloqueoSemaforo; }
    public double getEquidad() { return equidad; }
    public long getCompletados() { return completados; }
    public long getVencidos() { return vencidos; }
}
