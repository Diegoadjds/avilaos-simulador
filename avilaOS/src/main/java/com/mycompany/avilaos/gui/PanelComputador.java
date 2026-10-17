package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.sistema.Computador;
import com.mycompany.avilaos.so.Kernel;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

/** Vista 2 de UN computador: CPU, modo, colas, memoria y política de planificación. Solo observa. */
public class PanelComputador extends JPanel {

    private static final Color VERDE = new Color(30, 140, 60);
    private static final Color NARANJA = new Color(210, 120, 0);

    private final Computador computador;
    private final JComboBox<PoliticaPlanificacion> cmbPolitica = new JComboBox<>(PoliticaPlanificacion.values());
    private final JLabel lblProceso = etiqueta();
    private final JLabel lblModo = etiqueta();
    private final JLabel lblPc = etiqueta();
    private final JLabel lblPrioridad = etiqueta();
    private final JLabel lblDeadline = etiqueta();
    private final JLabel lblRestante = etiqueta();
    private final JProgressBar barRam = new JProgressBar();

    private final ModeloTablaPCB mNuevos = new ModeloTablaPCB(ModeloTablaPCB.NORMAL);
    private final ModeloTablaPCB mListos = new ModeloTablaPCB(ModeloTablaPCB.NORMAL);
    private final ModeloTablaPCB mBloqueados = new ModeloTablaPCB(ModeloTablaPCB.BLOQUEADOS);
    private final ModeloTablaPCB mTerminados = new ModeloTablaPCB(ModeloTablaPCB.TERMINADOS);
    private final JScrollPane spNuevos;
    private final JScrollPane spListos;
    private final JScrollPane spBloqueados;
    private final JScrollPane spTerminados;

    public PanelComputador(Computador computador) {
        super(new BorderLayout(6, 6));
        this.computador = computador;
        setBorder(BorderFactory.createTitledBorder("Computador " + computador.getId()));

        // --- Cabecera: política y RAM ---
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cabecera.add(new JLabel("Política:"));
        cmbPolitica.setSelectedItem(computador.getKernel().getPolitica());
        cmbPolitica.addActionListener(e -> {
            PoliticaPlanificacion elegida = (PoliticaPlanificacion) cmbPolitica.getSelectedItem();
            if (elegida != null && elegida != computador.getKernel().getPolitica()) {
                computador.cambiarPolitica(elegida);
            }
        });
        cabecera.add(cmbPolitica);
        cabecera.add(new JLabel("   RAM:"));
        barRam.setStringPainted(true);
        barRam.setPreferredSize(new Dimension(230, 20));
        cabecera.add(barRam);

        // --- CPU ---
        JPanel cpu = new JPanel(new GridLayout(2, 3, 10, 2));
        cpu.setBorder(BorderFactory.createTitledBorder("CPU"));
        cpu.add(lblProceso);
        cpu.add(lblModo);
        cpu.add(lblPc);
        cpu.add(lblPrioridad);
        cpu.add(lblDeadline);
        cpu.add(lblRestante);

        // --- Colas ---
        spNuevos = crearTabla("Nuevos", mNuevos);
        spListos = crearTabla("Listos", mListos);
        spBloqueados = crearTabla("Bloqueados", mBloqueados);
        spTerminados = crearTabla("Terminados", mTerminados);
        JPanel fila1 = new JPanel(new GridLayout(1, 2, 6, 6));
        fila1.add(spNuevos);
        fila1.add(spListos);
        JPanel colas = new JPanel(new GridLayout(3, 1, 6, 6));
        colas.add(fila1);
        colas.add(spBloqueados);
        colas.add(spTerminados);

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(cabecera);
        norte.add(Box.createVerticalStrut(4));
        norte.add(cpu);
        add(norte, BorderLayout.NORTH);
        add(colas, BorderLayout.CENTER);
        setPreferredSize(new Dimension(600, 560));
        refrescar();
    }

    private static JLabel etiqueta() {
        JLabel l = new JLabel();
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JScrollPane crearTabla(String titulo, ModeloTablaPCB modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(18);
        tabla.setFillsViewportHeight(true);
        int[] anchos = {30, 75, 70, 32, 36, 50, 40, 170};
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane sp = new JScrollPane(tabla);
        sp.setColumnHeaderView(tabla.getTableHeader());
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }

    private static void titulo(JScrollPane sp, String texto) {
        ((TitledBorder) sp.getBorder()).setTitle(texto);
        sp.repaint();
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing). */
    public void refrescar() {
        Kernel k = computador.getKernel();
        PCB p = k.getProcesoEnCpu();
        if (p == null) {
            lblProceso.setText("Proceso: — (CPU ociosa)");
            lblPc.setText("PC: —");
            lblPrioridad.setText("Prioridad: —");
            lblDeadline.setText("Deadline: —");
            lblRestante.setText("Restante: —");
        } else {
            lblProceso.setText("Proceso: " + p.getEtiqueta() + " (" + p.getNombre() + ")");
            lblPc.setText("PC: " + p.getPc() + "  MAR: " + p.getMar());
            lblPrioridad.setText("Prioridad: " + p.getPrioridad());
            lblDeadline.setText("Deadline: " + p.getDeadlineRestante());
            lblRestante.setText("Restante: " + p.getTiempoRestante());
        }
        ModoCPU modo = k.getModoCpu();
        lblModo.setText("Modo: " + (modo == ModoCPU.USUARIO ? "Programa de usuario" : "Sistema operativo"));
        lblModo.setForeground(modo == ModoCPU.USUARIO ? VERDE : NARANJA);

        MemoriaPrincipal ram = k.getRam();
        barRam.setMaximum(ram.getTotal());
        barRam.setValue(ram.getUsada());
        barRam.setString(ram.getUsada() + " usada / " + ram.getLibre() + " libre (total " + ram.getTotal() + ")");

        PCB[] nuevos = k.getNuevos();
        PCB[] listos = k.getListos();
        PCB[] bloqueados = k.getBloqueados();
        PCB[] terminados = k.getTerminados();
        mNuevos.setDatos(nuevos);
        mListos.setDatos(listos);
        mBloqueados.setDatos(bloqueados);
        mTerminados.setDatos(terminados);
        titulo(spNuevos, "Nuevos (" + nuevos.length + ")");
        titulo(spListos, "Listos (" + listos.length + ")");
        titulo(spBloqueados, "Bloqueados (" + bloqueados.length + ")");
        titulo(spTerminados, "Terminados (" + terminados.length + ")");
    }
}
