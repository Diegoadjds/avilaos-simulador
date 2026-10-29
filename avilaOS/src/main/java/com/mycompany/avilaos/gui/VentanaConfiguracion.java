package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.persistencia.BufferEscenario;
import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.persistencia.Escenario;
import com.mycompany.avilaos.persistencia.RepositorioEscenario;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Vista de configuración: cantidad de computadores, RAM, políticas iniciales, ciclo, quantum y latencia de red.
 * Permite cargar un escenario desde un archivo JSON (parámetros + buffers y procesos iniciales) y guardar
 * la configuración actual para usarla en futuras simulaciones.
 */
public class VentanaConfiguracion extends JFrame {

    private static final int MAX_COMPUTADORES = Escenario.MAX_COMPUTADORES;

    private final JSpinner spComputadores = new JSpinner(new SpinnerNumberModel(2, 2, MAX_COMPUTADORES, 1));
    private final JTextField txtCiclo = new JTextField("500", 8);
    private final JTextField txtLatencia = new JTextField("3", 8);
    private final JTextField txtQuantum = new JTextField("4", 8);
    private final JLabel[] lblComputador = new JLabel[MAX_COMPUTADORES];
    private final JTextField[] txtRam = new JTextField[MAX_COMPUTADORES];
    @SuppressWarnings("unchecked")
    private final JComboBox<PoliticaPlanificacion>[] cmbPolitica = new JComboBox[MAX_COMPUTADORES];
    private final JPanel filas = new JPanel(new GridLayout(0, 3, 8, 4));
    private final JLabel lblCarga = new JLabel();
    private final JButton btnQuitarCarga = new BotonModerno("Quitar carga");

    /** Buffers y procesos iniciales leídos de un archivo (null si no se cargó ninguno). */
    private Escenario cargaInicial;
    private String nombreArchivoCarga = "";

    public VentanaConfiguracion() {
        super("ÁvilaOS – Configuración de la simulación");

        JPanel archivo = new JPanel(new GridLayout(2, 1));
        archivo.setBorder(Tema.borde("Escenario (archivo JSON)"));
        JButton btnCargar = new BotonModerno("Cargar escenario…");
        btnCargar.setToolTipText("Lee parámetros, buffers y procesos iniciales desde un archivo JSON");
        btnCargar.addActionListener(e -> cargarEscenario());
        JButton btnGuardar = new BotonModerno("Guardar escenario…");
        btnGuardar.setToolTipText("Guarda los parámetros de esta ventana (y la carga inicial, si hay) en un archivo JSON");
        btnGuardar.addActionListener(e -> guardarEscenario());
        btnQuitarCarga.addActionListener(e -> {
            cargaInicial = null;
            actualizarEtiquetaCarga();
            pack();
        });
        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filaBotones.add(btnCargar);
        filaBotones.add(btnGuardar);
        JPanel filaCarga = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filaCarga.add(lblCarga);
        filaCarga.add(btnQuitarCarga);
        archivo.add(filaBotones);
        archivo.add(filaCarga);
        actualizarEtiquetaCarga();

        JPanel globales = new JPanel(new GridBagLayout());
        globales.setBorder(Tema.borde("Parámetros generales"));
        int y = 0;
        fila(globales, y++, "Cantidad de computadores (2 a " + MAX_COMPUTADORES + "):", spComputadores);
        fila(globales, y++, "Duración de un ciclo (ms):", txtCiclo);
        fila(globales, y++, "Latencia de red (ciclos):", txtLatencia);
        fila(globales, y++, "Quantum de Round Robin (ciclos):", txtQuantum);

        filas.setBorder(Tema.borde("Computadores"));
        filas.add(new JLabel("Computador"));
        filas.add(new JLabel("RAM"));
        filas.add(new JLabel("Política inicial"));
        for (int i = 0; i < MAX_COMPUTADORES; i++) {
            lblComputador[i] = new JLabel("Computador " + (i + 1));
            txtRam[i] = new JTextField("100", 8);
            cmbPolitica[i] = new JComboBox<>(PoliticaPlanificacion.values());
            ComboModerno.aplicar(cmbPolitica[i]);
            filas.add(lblComputador[i]);
            filas.add(txtRam[i]);
            filas.add(cmbPolitica[i]);
        }
        spComputadores.addChangeListener(e -> mostrarFilas());
        mostrarFilas();

        JButton btnIniciar = new BotonModerno("Abrir simulador", BotonModerno.Tipo.PRIMARIO, Iconos.de(Iconos.Forma.PLAY));
        btnIniciar.addActionListener(e -> abrirSimulador());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnIniciar);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.add(archivo, BorderLayout.NORTH);
        norte.add(globales, BorderLayout.CENTER);
        centro.add(norte, BorderLayout.NORTH);
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

    /** Lee y valida los campos de la ventana (más la carga inicial, si hay) y arma el escenario. */
    private Escenario construirEscenario() {
        int n = (Integer) spComputadores.getValue();
        int ciclo = Entradas.entero(txtCiclo, "Duración del ciclo", Escenario.CICLO_MIN_MS, Escenario.CICLO_MAX_MS);
        int latencia = Entradas.entero(txtLatencia, "Latencia de red", 0, Escenario.LATENCIA_MAX);
        int quantum = Entradas.entero(txtQuantum, "Quantum", 1, Escenario.QUANTUM_MAX);
        int[] ram = new int[n];
        PoliticaPlanificacion[] politicas = new PoliticaPlanificacion[n];
        for (int i = 0; i < n; i++) {
            ram[i] = Entradas.entero(txtRam[i], "RAM del computador " + (i + 1), 1, Escenario.RAM_MAX);
            politicas[i] = (PoliticaPlanificacion) cmbPolitica[i].getSelectedItem();
        }
        BufferEscenario[] buffers = cargaInicial == null ? new BufferEscenario[0] : cargaInicial.getBuffers();
        EspecificacionProceso[] procesos = cargaInicial == null
                ? new EspecificacionProceso[0] : cargaInicial.getProcesos();
        Escenario escenario = new Escenario(ciclo, latencia, quantum, ram, politicas, buffers, procesos);
        String error = escenario.validar();
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return escenario;
    }

    private void abrirSimulador() {
        try {
            Escenario escenario = construirEscenario();
            ClusterAvilaOS cluster = escenario.crearCluster();
            new VentanaPrincipal(cluster, escenario).setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        }
    }

    private void cargarEscenario() {
        Path ruta = DialogosEscenario.elegirParaAbrir(this);
        if (ruta == null) {
            return;
        }
        try {
            aplicarAlFormulario(RepositorioEscenario.cargar(ruta), ruta.getFileName().toString());
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, "No se pudo cargar el escenario.\n\n" + ex.getMessage());
        }
    }

    private void guardarEscenario() {
        try {
            Escenario escenario = construirEscenario();
            Path ruta = DialogosEscenario.elegirParaGuardar(this, "escenario.json");
            if (ruta == null) {
                return;
            }
            RepositorioEscenario.guardar(ruta, escenario);
            DialogosEscenario.informar(this, "Escenario guardado en:\n" + ruta + "\n(" + escenario.describirCarga() + ")");
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    /** Vuelca un escenario leído de archivo en los campos de la ventana. */
    private void aplicarAlFormulario(Escenario escenario, String nombreArchivo) {
        int n = escenario.getNumComputadores();
        spComputadores.setValue(n);
        txtCiclo.setText(String.valueOf(escenario.getDuracionCicloMs()));
        txtLatencia.setText(String.valueOf(escenario.getLatenciaRed()));
        txtQuantum.setText(String.valueOf(escenario.getQuantum()));
        for (int i = 0; i < n; i++) {
            txtRam[i].setText(String.valueOf(escenario.getRam(i + 1)));
            cmbPolitica[i].setSelectedItem(escenario.getPolitica(i + 1));
        }
        boolean hayCarga = escenario.getBuffers().length + escenario.getProcesos().length > 0;
        cargaInicial = hayCarga ? escenario : null;
        nombreArchivoCarga = nombreArchivo;
        actualizarEtiquetaCarga();
        pack();
    }

    private void actualizarEtiquetaCarga() {
        if (cargaInicial == null) {
            lblCarga.setText("Carga inicial: ninguna");
        } else {
            String nombre = nombreArchivoCarga.length() > 36
                    ? nombreArchivoCarga.substring(0, 33) + "…" : nombreArchivoCarga;
            lblCarga.setText("Carga inicial: " + cargaInicial.describirCarga() + " (" + nombre + ")");
        }
        btnQuitarCarga.setVisible(cargaInicial != null);
        revalidate();
        repaint();
    }
}
