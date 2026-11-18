package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.TipoOperacionES;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import com.mycompany.avilaos.so.NodoFS;
import com.mycompany.avilaos.so.SistemaArchivos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Sistema de archivos en pantalla: el JTree con los directorios y archivos, la información del elemento elegido
 * (nombre, tamaño, dueño, permisos, primer bloque), la tabla de asignación y los botones del CRUD. Los botones no
 * operan sobre el disco: abren el diálogo que crea el PROCESO de E/S que hará la operación.
 */
public class PanelArchivos extends JPanel {

    private final SistemaAvilaOS sistema;
    private final SistemaArchivos archivos;
    private final ModeloArbolArchivos modeloArbol;
    private final JTree arbol;
    private final ModeloTablaAsignacion modeloTabla = new ModeloTablaAsignacion();
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel[] valores = new JLabel[10];
    private static final String[] ETIQUETAS = {"Nombre", "Ruta", "Tipo", "Tamaño (bloques)", "Dueño", "Permisos",
        "Primer bloque", "Cadena de bloques", "Creado por", "Estado de uso"};
    private long versionMostrada = -1;
    private boolean primeraVez = true;

    public PanelArchivos(SistemaAvilaOS sistema) {
        super(new BorderLayout(10, 0));
        this.sistema = sistema;
        this.archivos = sistema.getArchivos();
        setOpaque(false);
        modeloArbol = new ModeloArbolArchivos(archivos);
        arbol = new JTree(modeloArbol);
        arbol.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        arbol.setRootVisible(true);
        arbol.setBackground(Tema.CAMPO);
        arbol.setRowHeight(22);
        arbol.setCellRenderer(new RenderizadorArbol());
        arbol.addTreeSelectionListener(e -> mostrarInformacion());

        JScrollPane spArbol = new JScrollPane(arbol);
        Tema.estilizarScroll(spArbol);
        JPanel izquierda = new JPanel(new BorderLayout(0, 8));
        izquierda.setOpaque(false);
        izquierda.setBorder(Tema.borde("Sistema de archivos (JTree)"));
        izquierda.add(spArbol, BorderLayout.CENTER);
        izquierda.add(botones(), BorderLayout.SOUTH);
        izquierda.setPreferredSize(new Dimension(380, 400));

        JPanel info = new JPanel(new GridLayout(ETIQUETAS.length, 2, 8, 3));
        info.setOpaque(false);
        info.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 10));
        for (int i = 0; i < ETIQUETAS.length; i++) {
            JLabel e = new JLabel(ETIQUETAS[i]);
            e.setForeground(Tema.TEXTO_SUAVE);
            valores[i] = new JLabel("—");
            valores[i].setForeground(Tema.TEXTO);
            valores[i].setFont(Tema.fuente(Font.BOLD, 12.5f));
            info.add(e);
            info.add(valores[i]);
        }
        JPanel panelInfo = new JPanel(new BorderLayout());
        panelInfo.setOpaque(false);
        panelInfo.setBorder(Tema.borde("Información del elemento seleccionado"));
        panelInfo.add(info, BorderLayout.CENTER);

        Tema.estilizarTabla(tabla);
        tabla.setRowHeight(22);
        tabla.setDefaultRenderer(Color.class, new RenderizadorColor());
        int[] anchos = {46, 190, 60, 90, 70, 70, 190};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane spTabla = new JScrollPane(tabla);
        spTabla.setColumnHeaderView(tabla.getTableHeader());
        Tema.estilizarScroll(spTabla);
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setOpaque(false);
        panelTabla.setBorder(Tema.borde("Tabla de asignación de archivos (JTable)"));
        panelTabla.add(spTabla, BorderLayout.CENTER);

        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelInfo, panelTabla);
        derecha.setResizeWeight(0.38);
        derecha.setBorder(null);
        derecha.setOpaque(false);
        add(izquierda, BorderLayout.WEST);
        add(derecha, BorderLayout.CENTER);
        refrescar();
    }

    private JPanel botones() {
        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        p.add(boton("Leer", TipoOperacionES.LEER, "Crea un proceso de E/S que lee el elemento elegido"));
        p.add(boton("Crear archivo…", TipoOperacionES.CREAR, "Crea un proceso de E/S que crea un archivo dentro del directorio elegido"));
        p.add(boton("Crear directorio…", TipoOperacionES.CREAR, "Crea un proceso de E/S que crea un directorio (0 bloques)"));
        p.add(boton("Actualizar / renombrar…", TipoOperacionES.ACTUALIZAR, "Crea un proceso de E/S que reescribe el elemento o le cambia el nombre"));
        p.add(boton("Eliminar", TipoOperacionES.ELIMINAR, "Crea un proceso de E/S que elimina el elemento (y su contenido si es un directorio)"));
        return p;
    }

    private BotonModerno boton(String texto, TipoOperacionES operacion, String ayuda) {
        BotonModerno b = new BotonModerno(texto);
        b.setToolTipText(ayuda);
        b.addActionListener(e -> {
            NodoFS sel = seleccionado();
            String ruta = sel == null ? "/" : sel.getRuta();
            if (operacion == TipoOperacionES.CREAR) {
                String base = sel == null ? "/" : sel.esDirectorio() ? sel.getRuta() : SistemaArchivos.rutaPadre(sel.getRuta());
                ruta = (base.equals("/") ? "" : base) + (texto.contains("directorio") ? "/nuevo-directorio" : "/nuevo-archivo.txt");
            }
            DialogoProcesoArchivo d = new DialogoProcesoArchivo(SwingUtilities.getWindowAncestor(this), sistema,
                    operacion, ruta);
            if (texto.contains("directorio")) {
                d.prepararDirectorio();
            }
            d.setVisible(true);
        });
        return b;
    }

    private NodoFS seleccionado() {
        TreePath ruta = arbol.getSelectionPath();
        return ruta == null ? null : (NodoFS) ruta.getLastPathComponent();
    }

    private void mostrarInformacion() {
        NodoFS n = seleccionado();
        if (n == null) {
            for (JLabel v : valores) {
                v.setText("—");
            }
            return;
        }
        valores[0].setText(n.getPadre() == null ? "/ (raíz)" : n.getNombre());
        valores[1].setText(n.getRuta());
        valores[2].setText(n.getTipo().toString());
        valores[3].setText(n.esDirectorio() ? "— (directorio)" : String.valueOf(n.getBloques()));
        valores[4].setText(n.getDueno());
        valores[5].setText(n.getPermiso().toString());
        valores[6].setText(n.getPrimerBloque() < 0 ? "—" : String.valueOf(n.getPrimerBloque()));
        valores[7].setText(n.esDirectorio() ? "—" : cadena(archivos.bloquesDe(n)));
        valores[8].setText(n.getCreadoPor() == null ? "—" : n.getCreadoPor());
        valores[9].setText(n.isEnCreacion() ? "creándose" : n.isEscribiendo() ? "modificándose"
                : n.getLecturasEnCurso() > 0 ? "leyéndose (" + n.getLecturasEnCurso() + ")" : "libre");
    }

    private static String cadena(int[] bloques) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bloques.length; i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            if (i == 8 && bloques.length > 10) {
                sb.append("… → ").append(bloques[bloques.length - 1]);
                break;
            }
            sb.append(bloques[i]);
        }
        return sb.toString();
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing): redibuja árbol y tabla solo si algo cambió. */
    public void refrescar() {
        long v = archivos.getVersion();
        if (v != versionMostrada) {
            versionMostrada = v;
            NodoFS sel = seleccionado();
            String rutaSel = sel == null ? null : sel.getRuta();
            ListaRutas expandidas = new ListaRutas();
            for (int i = 0; i < arbol.getRowCount(); i++) {
                TreePath tp = arbol.getPathForRow(i);
                if (arbol.isExpanded(tp)) {
                    expandidas.agregar(((NodoFS) tp.getLastPathComponent()).getRuta());
                }
            }
            modeloArbol.recargar();
            restaurar(new TreePath(modeloArbol.getRoot()), expandidas, rutaSel, primeraVez);
            primeraVez = false;
            modeloTabla.setDatos(archivos.archivos());
        }
        mostrarInformacion();
        tabla.repaint();
        arbol.repaint();
    }

    /** Vuelve a expandir lo que estaba expandido y a seleccionar lo que estaba seleccionado. */
    private void restaurar(TreePath ruta, ListaRutas expandidas, String rutaSel, boolean todo) {
        NodoFS nodo = (NodoFS) ruta.getLastPathComponent();
        if (todo || nodo.getPadre() == null || expandidas.contiene(nodo.getRuta())) {
            arbol.expandPath(ruta);
        }
        if (rutaSel != null && rutaSel.equals(nodo.getRuta())) {
            arbol.setSelectionPath(ruta);
        }
        for (NodoFS h : archivos.hijos(nodo)) {
            restaurar(ruta.pathByAddingChild(h), expandidas, rutaSel, todo);
        }
    }

    /** Lista simple de rutas (no se usan colecciones de java.util). */
    private static final class ListaRutas {
        private final com.mycompany.avilaos.estructuras.ListaEnlazada<String> rutas =
                new com.mycompany.avilaos.estructuras.ListaEnlazada<>();

        void agregar(String r) { rutas.agregarAlFinal(r); }

        boolean contiene(String r) {
            for (String x : rutas.aArreglo(new String[0])) {
                if (x.equals(r)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Dibuja cada elemento del árbol con su ícono, su color en el disco y su estado. */
    private static class RenderizadorArbol extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree arbol, Object valor, boolean seleccionado, boolean expandido,
                boolean hoja, int fila, boolean enfocado) {
            super.getTreeCellRendererComponent(arbol, valor, seleccionado, expandido, hoja, fila, false);
            NodoFS n = (NodoFS) valor;
            setOpaque(false);
            setFont(Tema.fuente(Font.PLAIN, 12.5f));
            String texto = n.getPadre() == null ? "/" : n.getNombre();
            if (!n.esDirectorio()) {
                texto += "  (" + n.getBloques() + " bl.)";
            }
            texto += "  [" + n.getDueno() + (n.getPermiso() == com.mycompany.avilaos.modelo.Permiso.PRIVADO ? ", privado" : "") + "]";
            if (n.isEnCreacion()) {
                texto += "  · creándose";
            } else if (n.isEscribiendo()) {
                texto += "  · modificándose";
            } else if (n.getLecturasEnCurso() > 0) {
                texto += "  · leyéndose";
            }
            setText(texto);
            setIcon(new IconoNodo(n.esDirectorio() ? new Color(0xE0B84A) : Tema.colorBloque(n.getColorId()), n.esDirectorio()));
            setForeground(seleccionado ? Color.WHITE : Tema.TEXTO);
            setBackgroundSelectionColor(new Color(0x2D63B8));
            return this;
        }
    }

    /** Ícono sencillo: carpeta amarilla o hoja del color del archivo. */
    private static class IconoNodo implements javax.swing.Icon {
        private final Color color;
        private final boolean carpeta;

        IconoNodo(Color color, boolean carpeta) {
            this.color = color;
            this.carpeta = carpeta;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            if (carpeta) {
                g.fillRoundRect(x, y + 4, 16, 11, 3, 3);
                g.fillRoundRect(x, y + 2, 7, 4, 2, 2);
            } else {
                g.fillRoundRect(x + 2, y + 1, 12, 15, 3, 3);
                g.setColor(new Color(0x101820));
                g.drawLine(x + 5, y + 6, x + 11, y + 6);
                g.drawLine(x + 5, y + 9, x + 11, y + 9);
            }
        }

        @Override
        public int getIconWidth() { return 18; }

        @Override
        public int getIconHeight() { return 18; }
    }

    /** Pinta la celda de color de la tabla de asignación. */
    private static class RenderizadorColor extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foc, int f, int c) {
            super.getTableCellRendererComponent(t, "", sel, false, f, c);
            setBackground(valor instanceof Color col ? col : Tema.CAMPO);
            setBorder(BorderFactory.createMatteBorder(3, 6, 3, 6, f % 2 == 0 ? Tema.CAMPO : new Color(0x142034)));
            return this;
        }
    }
}
