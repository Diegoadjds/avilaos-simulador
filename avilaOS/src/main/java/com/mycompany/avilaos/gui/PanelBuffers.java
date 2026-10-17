package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.sincronizacion.Semaforo;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/** Estado de cada buffer: elementos, capacidad, valores de sus semáforos y procesos esperando. */
public class PanelBuffers extends JPanel {

    private static final String[] COLUMNAS = {"Buffer", "Anfitrión", "Elementos", "mutex", "llenos", "vacíos",
        "Prod.", "Cons.", "Procesos bloqueados"};

    private final ClusterAvilaOS cluster;
    private final Modelo modelo = new Modelo();

    public PanelBuffers(ClusterAvilaOS cluster) {
        super(new BorderLayout());
        this.cluster = cluster;
        setBorder(BorderFactory.createTitledBorder("Buffers compartidos"));
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(20);
        tabla.setFillsViewportHeight(true);
        int[] anchos = {45, 95, 70, 45, 50, 50, 40, 40, 260};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane sp = new JScrollPane(tabla);
        sp.setColumnHeaderView(tabla.getTableHeader());
        add(sp, BorderLayout.CENTER);
    }

    public void refrescar() {
        modelo.setDatos(cluster.getBuffers());
    }

    private static String esperando(Semaforo s) {
        PCB[] w = s.getEsperando();
        if (w.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.getNombre()).append(": ");
        for (int i = 0; i < w.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(w[i].getEtiqueta());
        }
        return sb.append("   ").toString();
    }

    private static class Modelo extends AbstractTableModel {

        private BufferAcotado[] datos = new BufferAcotado[0];

        void setDatos(BufferAcotado[] d) {
            datos = d;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return datos.length; }

        @Override
        public int getColumnCount() { return COLUMNAS.length; }

        @Override
        public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int f, int c) {
            BufferAcotado b = datos[f];
            switch (c) {
                case 0: return b.getNombre();
                case 1: return "Computador " + b.getIdComputadorAnfitrion();
                case 2: return b.getElementos() + " / " + b.getCapacidad();
                case 3: return b.getMutex().getValor();
                case 4: return b.getLlenos().getValor();
                case 5: return b.getVacios().getValor();
                case 6: return b.getProducidos();
                case 7: return b.getConsumidos();
                default: return esperando(b.getMutex()) + esperando(b.getLlenos()) + esperando(b.getVacios());
            }
        }
    }
}
