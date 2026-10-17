package com.mycompany.avilaos;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.sistema.ConfiguracionCluster;
import com.mycompany.avilaos.so.MetricasComputador;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Prueba por consola (sin GUI) del núcleo y del productor-consumidor local y remoto.
 * Útil para depurar sin abrir ventanas.
 */
public class DemoConsola {

    public static void main(String[] args) throws InterruptedException {
        ConfiguracionCluster cfg = ConfiguracionCluster.porDefecto(2);
        cfg.setDuracionCicloMs(30);
        cfg.setLatenciaRed(3);
        cfg.setQuantum(3);
        cfg.setPolitica(0, PoliticaPlanificacion.ROUND_ROBIN);
        cfg.setPolitica(1, PoliticaPlanificacion.FCFS);
        ClusterAvilaOS cluster = new ClusterAvilaOS(cfg);

        BufferAcotado b1 = cluster.crearBuffer(2, 1); // buffer pequeño en el Computador 1

        // Productor local (Comp. 1) y productor remoto (Comp. 2); un consumidor remoto y uno local
        cluster.crearProceso(pc("ProdLocal", TipoProceso.PRODUCTOR, b1.getId(), 1, 6));
        cluster.crearProceso(pc("ProdRemoto", TipoProceso.PRODUCTOR, b1.getId(), 2, 6));
        cluster.crearProceso(pc("ConsRemoto", TipoProceso.CONSUMIDOR, b1.getId(), 2, 6));
        cluster.crearProceso(pc("ConsLocal", TipoProceso.CONSUMIDOR, b1.getId(), 1, 6));

        cluster.iniciar();
        Thread.sleep(9000);
        cluster.detener();
        Thread.sleep(300);

        System.out.println("\n===== BUFFER " + b1.getNombre() + " =====");
        System.out.println("producidos=" + b1.getProducidos() + " consumidos=" + b1.getConsumidos()
                + " enBuffer=" + b1.getElementos()
                + " | mutex=" + b1.getMutex().getValor()
                + " llenos=" + b1.getLlenos().getValor()
                + " vacios=" + b1.getVacios().getValor());
        System.out.println("\n===== MÉTRICAS =====");
        for (int i = 1; i <= cluster.getNumComputadores(); i++) {
            MetricasComputador m = cluster.getComputador(i).getKernel().getMetricas();
            System.out.printf("Computador %d: util=%.0f%% completados=%d vencidos=%d bloqueoSem=%.1f equidad=%.2f%n",
                    i, m.getUtilizacion() * 100, m.getCompletados(), m.getVencidos(),
                    m.getTiempoPromedioBloqueoSemaforo(), cluster.getComputador(i).getKernel().getEquidad());
        }
    }

    private static EspecificacionProceso pc(String nombre, TipoProceso tipo, int buffer, int computador, int elementos) {
        return new EspecificacionProceso(nombre, tipo, 1, 10, 1, 5000, 0, 0, buffer, 2, elementos, computador);
    }
}
