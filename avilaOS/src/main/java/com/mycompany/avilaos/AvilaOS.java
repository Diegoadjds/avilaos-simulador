package com.mycompany.avilaos;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.sistema.ConfiguracionCluster;
import com.mycompany.avilaos.so.MetricasComputador;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * PUNTO DE ENTRADA TEMPORAL (sin GUI) para probar el núcleo por consola.
 * Cuando exista la GUI, este main solo debe abrir la ventana principal.
 */
public class AvilaOS {

    public static void main(String[] args) throws InterruptedException {
        ConfiguracionCluster cfg = ConfiguracionCluster.porDefecto(2);
        cfg.setDuracionCicloMs(100);
        cfg.setQuantum(3);
        cfg.setPolitica(0, PoliticaPlanificacion.FCFS);
        cfg.setPolitica(1, PoliticaPlanificacion.ROUND_ROBIN);

        ClusterAvilaOS cluster = new ClusterAvilaOS(cfg);

        // Computador 1 (FCFS, RAM 100). "C" (80) no cabe hasta que termine algún proceso.
        cluster.crearProceso(EspecificacionProceso.cpuBound("A", 8, 30, 2, 60, 1));
        cluster.crearProceso(EspecificacionProceso.ioBound("B", 10, 30, 1, 60, 3, 4, 1));
        cluster.crearProceso(EspecificacionProceso.cpuBound("C", 5, 80, 3, 60, 1));

        // Computador 2 (Round Robin, quantum 3). "F" tiene deadline corto y va a vencer.
        cluster.crearProceso(EspecificacionProceso.cpuBound("D", 9, 20, 2, 80, 2));
        cluster.crearProceso(EspecificacionProceso.cpuBound("E", 9, 20, 1, 80, 2));
        cluster.crearProceso(EspecificacionProceso.cpuBound("F", 12, 20, 3, 8, 2));

        cluster.iniciar();
        Thread.sleep(2500);
        cluster.getComputador(1).cambiarPolitica(PoliticaPlanificacion.EDF); // cambio en caliente
        Thread.sleep(3500);
        cluster.detener();
        Thread.sleep(300);

        System.out.println("\n===== MÉTRICAS =====");
        for (int i = 1; i <= cluster.getNumComputadores(); i++) {
            MetricasComputador m = cluster.getComputador(i).getKernel().getMetricas();
            System.out.printf("Computador %d: utilización=%.0f%%  throughput=%.3f proc/ciclo  "
                    + "respuesta=%.1f ciclos  cumplimiento deadline=%.0f%%%n",
                    i, m.getUtilizacion() * 100, m.getThroughput(),
                    m.getTiempoRespuestaPromedio(), m.getTasaCumplimientoDeadline() * 100);
        }
    }
}
