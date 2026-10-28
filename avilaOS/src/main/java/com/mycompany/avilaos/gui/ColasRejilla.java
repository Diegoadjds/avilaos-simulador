package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.PCB;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Las 4 colas de un computador (Nuevos, Listos, Bloqueados, Terminados) en una rejilla de 2x2, en vez de una
 * debajo de otra. Cada cola conserva todos los elementos del PCB como columnas (ID, nombre, tipo, prioridad,
 * memoria, deadline, restante y motivo/resultado); solo cambian los textos a una versión corta y, al pasar el
 * mouse sobre una fila, se ve el PCB completo. Solo muestra: los datos los entrega el kernel.
 */
public class ColasRejilla extends JPanel {

    private static final int ALTO_FILA = 20;
    private static final int FILAS_VISIBLES = 3;

    private final Cuadro nuevos = new Cuadro("Nuevos", ModeloTablaPCB.NORMAL);
    private final Cuadro listos = new Cuadro("Listos", ModeloTablaPCB.NORMAL);
    private final Cuadro bloqueados = new Cuadro("Bloqueados", ModeloTablaPCB.BLOQUEADOS);
    private final Cuadro terminados = new Cuadro("Terminados", ModeloTablaPCB.TERMINADOS);

    public ColasRejilla() {
        super(new GridBagLayout());
        Tema.transparente(this);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        // Fila de arriba: Nuevos | Listos
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.5;
        c.insets = new Insets(0, 0, 6, 6);
        add(nuevos, c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 6, 0);
        add(listos, c);
        // Fila de abajo: Bloqueados | Terminados (llevan una columna más, por eso un poco más anchas)
        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0.5;
        c.insets = new Insets(0, 0, 0, 6);
        add(bloqueados, c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        add(terminados, c);
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing). */
    public void actualizar(PCB[] listosNuevos, PCB[] enListos, PCB[] enBloqueados, PCB[] enTerminados) {
        nuevos.poner(listosNuevos);
        listos.poner(enListos);
        bloqueados.poner(enBloqueados);
        terminados.poner(enTerminados);
    }

    /** Una cola: borde con título y contador, y la tabla de sus procesos. */
    private static class Cuadro extends JPanel {

        private final String titulo;
        private final ModeloTablaPCB modelo;
        private final BordeCompacto borde;

        Cuadro(String titulo, int modo) {
            super(new BorderLayout());
            this.titulo = titulo;
            modelo = new ModeloTablaPCB(modo, true);
            JTable tabla = new JTable(modelo) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    PCB p = modelo.getProceso(rowAtPoint(e.getPoint()));
                    return p == null ? null : detalle(p);
                }
            };
            Tema.estilizarTabla(tabla);
            tabla.setRowHeight(ALTO_FILA);
            tabla.getTableHeader().setPreferredSize(new Dimension(10, 22));
            RenderizadorTabla celdas = new RenderizadorTabla() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int f, int c) {
                    super.getTableCellRendererComponent(t, v, sel, foc, f, c);
                    setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
                    setFont(getFont().deriveFont(11.5f));
                    return this;
                }
            };
            tabla.setDefaultRenderer(Object.class, celdas);
            tabla.setDefaultRenderer(Integer.class, celdas);
            tabla.setDefaultRenderer(Long.class, celdas);

            int[] anchos = modo == ModeloTablaPCB.NORMAL
                    ? new int[]{24, 56, 32, 30, 30, 40, 34}
                    : new int[]{24, 52, 30, 28, 28, 38, 32, 72};
            for (int i = 0; i < tabla.getColumnCount(); i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            }
            JScrollPane sp = new JScrollPane(tabla);
            sp.setColumnHeaderView(tabla.getTableHeader());
            Tema.estilizarScroll(sp);
            sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            sp.setPreferredSize(new Dimension(10, 22 + FILAS_VISIBLES * ALTO_FILA + 2));

            borde = new BordeCompacto(titulo);
            setBorder(borde);
            setOpaque(false);
            add(sp, BorderLayout.CENTER);
            poner(new PCB[0]);
        }

        void poner(PCB[] datos) {
            modelo.setDatos(datos);
            borde.setTitulo(titulo + " (" + datos.length + ")");
            repaint();
        }
    }

    /** Tooltip con el PCB completo de la fila. */
    private static String detalle(PCB p) {
        StringBuilder s = new StringBuilder("<html><b>").append(p.getEtiqueta()).append(" · ").append(p.getNombre()).append("</b><br>");
        s.append("Tipo: ").append(p.getTipo()).append("<br>");
        s.append("Prioridad: ").append(p.getPrioridad()).append(" · Memoria: ").append(p.getMemoria()).append("<br>");
        s.append("Deadline restante: ").append(p.getDeadlineRestante()).append(" de ").append(p.getDeadlineInicial()).append("<br>");
        s.append("Instrucciones restantes: ").append(p.getTiempoRestante()).append(" de ").append(p.getInstruccionesTotales()).append("<br>");
        s.append("PC: ").append(p.getPc()).append(" · MAR: ").append(p.getMar());
        if (p.getEstado() == EstadoProceso.BLOQUEADO) {
            s.append("<br>Bloqueado: ").append(p.getMotivoBloqueo());
        }
        if (p.getEstado() == EstadoProceso.TERMINADO) {
            s.append("<br>").append(p.isTerminoPorDeadline() ? "Terminó por deadline vencido" : "Completado");
        }
        return s.append("</html>").toString();
    }
}
