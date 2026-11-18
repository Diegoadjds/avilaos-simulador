package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.PCB;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Las 6 colas del modelo de siete estados (Nuevos, Listos, Bloqueados, Listo/Suspendido, Bloqueado/Suspendido y
 * Terminados; el séptimo estado, Ejecución, es el proceso que está en la CPU) en una rejilla de 3x2. Cada cola conserva
 * todos los elementos del PCB como columnas y, al pasar el mouse sobre una fila, se ve el PCB completo. Solo muestra:
 * los datos los entrega el kernel.
 */
public class ColasRejilla extends JPanel {

    private static final int ALTO_FILA = 20;
    private static final int FILAS_VISIBLES = 3;

    private final Cuadro nuevos = new Cuadro("Nuevos (cola a largo plazo)", ModeloTablaPCB.NORMAL);
    private final Cuadro listos = new Cuadro("Listos", ModeloTablaPCB.NORMAL);
    private final Cuadro bloqueados = new Cuadro("Bloqueados", ModeloTablaPCB.BLOQUEADOS);
    private final Cuadro listosSusp = new Cuadro("Listo/Suspendido", ModeloTablaPCB.SUSPENDIDOS);
    private final Cuadro bloqueadosSusp = new Cuadro("Bloqueado/Suspendido", ModeloTablaPCB.SUSPENDIDOS);
    private final Cuadro terminados = new Cuadro("Terminados", ModeloTablaPCB.TERMINADOS);

    public ColasRejilla() {
        super(new GridLayout(3, 1, 0, 6));
        Tema.transparente(this);
        add(fila(nuevos, listos));
        add(fila(bloqueados, terminados));
        add(fila(listosSusp, bloqueadosSusp));
    }

    /** Dos colas lado a lado con el mismo ancho. */
    private static JPanel fila(Cuadro izquierda, Cuadro derecha) {
        JPanel fila = new JPanel(new GridBagLayout());
        Tema.transparente(fila);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 6);
        fila.add(izquierda, c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(derecha, c);
        return fila;
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing). */
    public void actualizar(PCB[] enNuevos, PCB[] enListos, PCB[] enBloqueados, PCB[] enListosSusp,
            PCB[] enBloqueadosSusp, PCB[] enTerminados) {
        nuevos.poner(enNuevos);
        listos.poner(enListos);
        bloqueados.poner(enBloqueados);
        listosSusp.poner(enListosSusp);
        bloqueadosSusp.poner(enBloqueadosSusp);
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
                    ? new int[]{24, 56, 44, 28, 30, 40, 34}
                    : new int[]{24, 52, 44, 28, 30, 40, 30, 120};
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
        s.append("Tipo: ").append(p.getTipo()).append(" · Estado: ").append(p.getEstado()).append("<br>");
        s.append("Prioridad: ").append(p.getPrioridad()).append(" · Memoria: ").append(p.getMemoria()).append("<br>");
        s.append("Deadline restante: ").append(p.getDeadlineRestante()).append(" de ").append(p.getDeadlineInicial()).append("<br>");
        s.append("Instrucciones restantes: ").append(p.getTiempoRestante()).append(" de ").append(p.getInstruccionesTotales()).append("<br>");
        s.append("PC: ").append(p.getPc()).append(" · MAR: ").append(p.getMar()).append("<br>");
        if (p.getOperacion() != null) {
            s.append("Operación: ").append(p.getOperacion()).append(" ").append(p.getRutaArchivo())
                    .append(" (usuario ").append(p.getUsuario()).append(")<br>");
        }
        s.append("Última instrucción: ").append(p.getComportamiento().ultimaInstruccion(p));
        if (p.getEstado() == EstadoProceso.BLOQUEADO || p.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            s.append("<br>").append(ModeloTablaPCB.detalleBloqueo(p, false));
        }
        if (p.getEstado().esSuspendido()) {
            s.append("<br>Swap: bloque ").append(p.getPrimerBloqueSwap()).append(" (").append(p.getMemoria()).append(" bloques)");
        }
        if (p.getEstado() == EstadoProceso.TERMINADO) {
            s.append("<br>").append(p.getErrorES() != null ? "Error de E/S: " + p.getErrorES()
                    : p.isTerminoPorDeadline() ? "Terminó por deadline vencido" : "Completado");
        }
        return s.append("</html>").toString();
    }
}
