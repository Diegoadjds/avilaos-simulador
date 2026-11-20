package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import com.mycompany.avilaos.so.MetricasComputador;
import com.mycompany.avilaos.so.MetricasDisco;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Métricas de la simulación: rendimiento del procesador, rendimiento del disco por política (desplazamiento y
 * espera), suspensiones y reactivaciones, y la gráfica del desplazamiento acumulado del cabezal.
 */
public class PanelMetricas extends JPanel {

    private static final String[] COL_CPU = {"Utilización CPU", "Throughput (proc/ciclo)", "Respuesta prom. (ciclos)",
        "Cumplimiento deadline", "Completados", "Vencidos", "Fallidos de E/S"};
    private static final String[] COL_DISCO = {"Política de disco", "Solicitudes atendidas", "Desplazamiento (pistas)",
        "Espera prom. (ciclos)"};

    private final SistemaAvilaOS sistema;
    private final Tabla cpu = new Tabla(COL_CPU, 1);
    private final Tabla disco = new Tabla(COL_DISCO, PoliticaPlanificacionDisco.values().length + 1);
    private final Tabla resumen = new Tabla(new String[]{"Utilización del disco", "Desplazamiento total",
        "Suspensiones (swap-out)", "Reactivaciones (swap-in)"}, 1);
    private final GraficoDesplazamiento grafico;

    public PanelMetricas(SistemaAvilaOS sistema) {
        super(new BorderLayout(0, 10));
        setBackground(Tema.FONDO);
        setBorder(Tema.relleno(12, 0, 0, 0));
        this.sistema = sistema;
        this.grafico = new GraficoDesplazamiento(sistema.getMetricasDisco());

        JPanel arriba = new JPanel(new GridLayout(2, 1, 0, 8));
        arriba.setOpaque(false);
        arriba.add(envolverFija(cpu, "Rendimiento del procesador"));
        arriba.add(envolverFija(resumen, "Disco y suspensión de procesos"));
        arriba.setPreferredSize(new Dimension(600, 200));
        add(arriba, BorderLayout.NORTH);

        JPanel graf = new JPanel(new BorderLayout());
        graf.setBorder(Tema.borde("Desplazamiento acumulado del cabezal"));
        graf.setOpaque(false);
        grafico.setBorder(BorderFactory.createEmptyBorder(14, 6, 6, 6));
        graf.add(grafico, BorderLayout.CENTER);
        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.setOpaque(false);
        centro.add(envolver(disco, "Disco por política (desplazamiento y espera)"));
        centro.add(graf);
        add(centro, BorderLayout.CENTER);
    }

    /** Tabla de una sola fila: encabezado y fila fijos, sin barra de desplazamiento. */
    private static JPanel envolverFija(Tabla modelo, String titulo) {
        JTable t = new JTable(modelo);
        Tema.estilizarTabla(t);
        t.setRowHeight(26);
        JPanel interior = new JPanel(new BorderLayout());
        interior.setBackground(Tema.FONDO);
        interior.add(t.getTableHeader(), BorderLayout.NORTH);
        interior.add(t, BorderLayout.CENTER);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(Tema.borde(titulo), BorderFactory.createLineBorder(Tema.BORDE)));
        p.add(interior, BorderLayout.NORTH);
        return p;
    }

    private static JScrollPane envolver(Tabla modelo, String titulo) {
        JTable t = new JTable(modelo);
        Tema.estilizarTabla(t);
        t.setRowHeight(26);
        JScrollPane sp = new JScrollPane(t);
        sp.setColumnHeaderView(t.getTableHeader());
        Tema.estilizarScroll(sp);
        sp.setBorder(BorderFactory.createCompoundBorder(Tema.borde(titulo), BorderFactory.createLineBorder(Tema.BORDE)));
        return sp;
    }

    public void refrescar() {
        MetricasComputador m = sistema.getComputador().getKernel().getMetricas();
        cpu.poner(0, String.format("%.1f%%", m.getUtilizacion() * 100), String.format("%.3f", m.getThroughput()),
                String.format("%.1f", m.getTiempoRespuestaPromedio()),
                String.format("%.0f%%", m.getTasaCumplimientoDeadline() * 100), String.valueOf(m.getCompletados()),
                String.valueOf(m.getVencidos()), String.valueOf(m.getFallidosES()));
        MetricasDisco d = sistema.getMetricasDisco();
        resumen.poner(0, String.format("%.1f%%", d.getUtilizacion() * 100), String.valueOf(d.getDesplazamientoTotal()),
                String.valueOf(d.getSuspensiones()), String.valueOf(d.getReactivaciones()));
        PoliticaPlanificacionDisco[] ps = PoliticaPlanificacionDisco.values();
        for (int i = 0; i < ps.length; i++) {
            disco.poner(i, ps[i].toString(), String.valueOf(d.getAtendidas(ps[i])),
                    String.valueOf(d.getDesplazamiento(ps[i])), String.format("%.1f", d.getEsperaPromedio(ps[i])));
        }
        disco.poner(ps.length, "TOTAL", String.valueOf(d.getAtendidas()), String.valueOf(d.getDesplazamientoTotal()),
                String.format("%.1f", d.getEsperaPromedio()));
        grafico.repaint();
    }

    /** Tabla de solo texto: la interfaz solo escribe las celdas ya formateadas. */
    private static class Tabla extends AbstractTableModel {

        private final String[] columnas;
        private final String[][] celdas;

        Tabla(String[] columnas, int filas) {
            this.columnas = columnas;
            this.celdas = new String[filas][columnas.length];
            for (String[] f : celdas) {
                for (int c = 0; c < f.length; c++) {
                    f[c] = "";
                }
            }
        }

        void poner(int fila, String... valores) {
            System.arraycopy(valores, 0, celdas[fila], 0, columnas.length);
            fireTableRowsUpdated(fila, fila);
        }

        @Override
        public int getRowCount() { return celdas.length; }

        @Override
        public int getColumnCount() { return columnas.length; }

        @Override
        public String getColumnName(int c) { return columnas[c]; }

        @Override
        public Object getValueAt(int f, int c) { return celdas[f][c]; }
    }
}
