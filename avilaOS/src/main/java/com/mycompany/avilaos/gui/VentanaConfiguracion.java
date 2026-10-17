package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.sistema.ConfiguracionCluster;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** Vista de configuración: cantidad de computadores, RAM, políticas iniciales, ciclo, quantum y latencia de red. */
public class VentanaConfiguracion extends JFrame {

    private static final int MAX_COMPUTADORES = 6;

    private final JSpinner spComputadores = new JSpinner(new SpinnerNumberModel(2, 2, MAX_COMPUTADORES, 1));
    private final JTextField txtCiclo = new JTextField("500", 8);
    private final JTextField txtLatencia = new JTextField("3", 8);
    private final JTextField txtQuantum = new JTextField("4", 8);
    private final JLabel[] lblComputador = new JLabel[MAX_COMPUTADORES];
    private final JTextField[] txtRam = new JTextField[MAX_COMPUTADORES];
    @SuppressWarnings("unchecked")
    private final JComboBox<PoliticaPlanificacion>[] cmbPolitica = new JComboBox[MAX_COMPUTADORES];
    private final JPanel filas = new JPanel(new GridLayout(0, 3, 8, 4));

    public VentanaConfiguracion() {
        super("ÁvilaOS – Configuración de la simulación");

        JPanel globales = new JPanel(new GridBagLayout());
        globales.setBorder(BorderFactory.createTitledBorder("Parámetros generales"));
        int y = 0;
        fila(globales, y++, "Cantidad de computadores (2 a " + MAX_COMPUTADORES + "):", spComputadores);
        fila(globales, y++, "Duración de un ciclo (ms):", txtCiclo);
        fila(globales, y++, "Latencia de red (ciclos):", txtLatencia);
        fila(globales, y++, "Quantum de Round Robin (ciclos):", txtQuantum);

        filas.setBorder(BorderFactory.createTitledBorder("Computadores"));
        filas.add(new JLabel("Computador"));
        filas.add(new JLabel("RAM"));
        filas.add(new JLabel("Política inicial"));
        for (int i = 0; i < MAX_COMPUTADORES; i++) {
            lblComputador[i] = new JLabel("Computador " + (i + 1));
            txtRam[i] = new JTextField("100", 8);
            cmbPolitica[i] = new JComboBox<>(PoliticaPlanificacion.values());
            filas.add(lblComputador[i]);
            filas.add(txtRam[i]);
            filas.add(cmbPolitica[i]);
        }
        spComputadores.addChangeListener(e -> mostrarFilas());
        mostrarFilas();

        JButton btnIniciar = new JButton("Abrir simulador");
        btnIniciar.addActionListener(e -> abrirSimulador());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnIniciar);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        centro.add(globales, BorderLayout.NORTH);
        centro.add(filas, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(centro, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnIniciar);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private static void fila(JPanel p, int y, String texto, java.awt.Component campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridy = y;
        g.gridx = 0;
        p.add(new JLabel(texto), g);
        g.gridx = 1;
        p.add(campo, g);
    }

    private void mostrarFilas() {
        int n = (Integer) spComputadores.getValue();
        for (int i = 0; i < MAX_COMPUTADORES; i++) {
            boolean visible = i < n;
            lblComputador[i].setVisible(visible);
            txtRam[i].setVisible(visible);
            cmbPolitica[i].setVisible(visible);
        }
        filas.revalidate();
        filas.repaint();
        pack();
    }

    private void abrirSimulador() {
        try {
            int n = (Integer) spComputadores.getValue();
            int ciclo = Entradas.entero(txtCiclo, "Duración del ciclo", 10, 10000);
            int latencia = Entradas.entero(txtLatencia, "Latencia de red", 0, 1000);
            int quantum = Entradas.entero(txtQuantum, "Quantum", 1, 1000);
            ConfiguracionCluster cfg = new ConfiguracionCluster(n);
            cfg.setDuracionCicloMs(ciclo);
            cfg.setLatenciaRed(latencia);
            cfg.setQuantum(quantum);
            for (int i = 0; i < n; i++) {
                cfg.setRam(i, Entradas.entero(txtRam[i], "RAM del computador " + (i + 1), 1, 1000000));
                cfg.setPolitica(i, (PoliticaPlanificacion) cmbPolitica[i].getSelectedItem());
            }
            ClusterAvilaOS cluster = new ClusterAvilaOS(cfg);
            new VentanaPrincipal(cluster).setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        }
    }
}
