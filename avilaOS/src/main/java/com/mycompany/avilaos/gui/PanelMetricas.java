package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.sistema.Computador;
import com.mycompany.avilaos.sistema.MetricasGlobales;
import com.mycompany.avilaos.so.MetricasComputador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/** Métricas de rendimiento por computador y del sistema completo, más la gráfica de utilización. */
public class PanelMetricas extends JPanel {

    private static final String[] COLUMNAS = {"", "Utilización", "Throughput (proc/ciclo)", "Respuesta prom. (ciclos)",
        "Cumplimiento deadline", "Equidad (Jain)", "Bloqueo prom. en semáforos (ciclos)", "Completados", "Vencidos"};

    private final ClusterAvilaOS cluster;
    private final Modelo modelo;
    private final GraficoUtilizacion grafico;

    public PanelMetricas(ClusterAvilaOS cluster) {
        super(new BorderLayout(0, 10));
        setBackground(Tema.FONDO);
        setBorder(Tema.relleno(12, 0, 0, 0));
        this.cluster = cluster;
        this.modelo = new Modelo(cluster.getNumComputadores() + 1);
        this.grafico = new GraficoUtilizacion(cluster.getComputadores());

        JTable tabla = new JTable(modelo);
        Tema.estilizarTabla(tabla);
        tabla.setRowHeight(26);
        int[] anchos = {110, 90, 170, 170, 160, 110, 240, 90, 80};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane sp = new JScrollPane(tabla);
        sp.setColumnHeaderView(tabla.getTableHeader());
        Tema.estilizarScroll(sp);
        sp.setBorder(BorderFactory.createCompoundBorder(Tema.borde("Métricas de rendimiento"),
                BorderFactory.createLineBorder(Tema.BORDE)));
        sp.setPreferredSize(new Dimension(600, 52 + 28 * (cluster.getNumComputadores() + 2)));
        add(sp, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(Tema.borde("Utilización del procesador"));
        centro.setOpaque(false);
        grafico.setBorder(BorderFactory.createEmptyBorder(14, 6, 6, 6));
        centro.add(grafico, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    public void refrescar() {
        Computador[] cs = cluster.getComputadores();
        for (int i = 0; i < cs.length; i++) {
            MetricasComputador m = cs[i].getKernel().getMetricas();
            modelo.fila(i, "Computador " + cs[i].getId(), m.getUtilizacion(), m.getThroughput(),
                    m.getTiempoRespuestaPromedio(), m.getTasaCumplimientoDeadline(), cs[i].getKernel().getEquidad(),
                    m.getTiempoPromedioBloqueoSemaforo(), m.getCompletados(), m.getVencidos());
        }
        MetricasGlobales g = cluster.getMetricasGlobales();
        modelo.fila(cs.length, "SISTEMA", g.getUtilizacionPromedio(), g.getThroughput(),
                g.getTiempoRespuestaPromedio(), g.getTasaCumplimientoDeadline(), g.getEquidad(),
                g.getTiempoPromedioBloqueoSemaforo(), g.getCompletados(), g.getVencidos());
        modelo.fireTableDataChanged();
        grafico.repaint();
    }

    private static class Modelo extends AbstractTableModel {

        private final String[][] celdas;

        Modelo(int filas) {
            celdas = new String[filas][COLUMNAS.length];
            for (String[] f : celdas) {
                for (int c = 0; c < f.length; c++) {
                    f[c] = "";
                }
            }
        }

        void fila(int i, String nombre, double util, double thr, double resp, double cumpl, double equidad,
                double bloq, long completados, long vencidos) {
            celdas[i][0] = nombre;
            celdas[i][1] = String.format("%.1f%%", util * 100);
            celdas[i][2] = String.format("%.3f", thr);
            celdas[i][3] = String.format("%.1f", resp);
            celdas[i][4] = String.format("%.0f%%", cumpl * 100);
            celdas[i][5] = String.format("%.2f", equidad);
            celdas[i][6] = String.format("%.1f", bloq);
            celdas[i][7] = String.valueOf(completados);
            celdas[i][8] = String.valueOf(vencidos);
        }

        @Override
        public int getRowCount() { return celdas.length; }

        @Override
        public int getColumnCount() { return COLUMNAS.length; }

        @Override
        public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int f, int c) { return celdas[f][c]; }
    }
}
