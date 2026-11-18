package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.persistencia.EscenarioArchivos;
import com.mycompany.avilaos.persistencia.EscenarioProcesos;
import com.mycompany.avilaos.persistencia.RepositorioArchivos;
import com.mycompany.avilaos.persistencia.RepositorioProcesos;
import com.mycompany.avilaos.sistema.ConfiguracionSistema;
import com.mycompany.avilaos.sistema.Montaje;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Pantalla de arranque: parámetros base del computador y del disco, y la carga de los DOS archivos JSON (sistema de
 * archivos y procesos). Lo que traen los JSON manda sobre estos campos. El simulador se abre con lo que haya, pero
 * el reloj NO arranca hasta que los dos JSON estén cargados.
 */
public class VentanaConfiguracion extends JFrame {

    private final ConfiguracionSistema base = new ConfiguracionSistema();
    private final JTextField txtCiclo = new JTextField(String.valueOf(base.getDuracionCicloMs()), 8);
    private final JTextField txtRam = new JTextField(String.valueOf(base.getRam()), 8);
    private final JTextField txtQuantum = new JTextField(String.valueOf(base.getQuantum()), 8);
    private final JTextField txtBloques = new JTextField(String.valueOf(base.getBloquesDisco()), 8);
    private final JTextField txtSwap = new JTextField(String.valueOf(base.getBloquesSwap()), 8);
    private final JTextField txtCabezal = new JTextField(String.valueOf(base.getCabezalInicial()), 8);
    private final JTextField txtVelocidad = new JTextField(String.valueOf(base.getVelocidadCabezal()), 8);
    private final JTextField txtVentana = new JTextField(String.valueOf(base.getVentanaAgrupacion()), 8);
    private final JComboBox<PoliticaPlanificacion> cmbCpu = new JComboBox<>(PoliticaPlanificacion.values());
    private final JComboBox<PoliticaPlanificacionDisco> cmbDisco = new JComboBox<>(PoliticaPlanificacionDisco.values());
    private final Insignia insigniaFs = new Insignia();
    private final Insignia insigniaProcesos = new Insignia();

    private EscenarioArchivos archivos;
    private String nombreArchivos = "";
    private EscenarioProcesos procesos;
    private String nombreProcesos = "";

    public VentanaConfiguracion() {
        super("ÁvilaOS – Configuración de la simulación");
        ComboModerno.aplicar(cmbCpu);
        ComboModerno.aplicar(cmbDisco);
        cmbCpu.setSelectedItem(base.getPoliticaCpu());
        cmbDisco.setSelectedItem(base.getPoliticaDisco());

        // --- Los dos JSON ---
        BotonModerno btnFs = new BotonModerno("Cargar sistema de archivos (JSON)…", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.CARGAR));
        btnFs.addActionListener(e -> cargarArchivos());
        BotonModerno btnProcesos = new BotonModerno("Cargar procesos y configuración (JSON)…", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.CARGAR));
        btnProcesos.addActionListener(e -> cargarProcesos());
        JPanel json = new JPanel(new GridBagLayout());
        json.setBorder(Tema.borde("Archivos JSON (los dos son obligatorios para iniciar el reloj)"));
        celda(json, 0, 0, btnFs);
        celda(json, 1, 0, insigniaFs);
        celda(json, 0, 1, btnProcesos);
        celda(json, 1, 1, insigniaProcesos);
        actualizarInsignias();

        // --- Parámetros base ---
        JPanel cpu = new JPanel(new GridBagLayout());
        cpu.setBorder(Tema.borde("Computador"));
        fila(cpu, 0, "Duración de un ciclo (ms):", txtCiclo);
        fila(cpu, 1, "Memoria RAM:", txtRam);
        fila(cpu, 2, "Política de planificación de CPU:", cmbCpu);
        fila(cpu, 3, "Quantum de Round Robin (ciclos):", txtQuantum);

        JPanel disco = new JPanel(new GridBagLayout());
        disco.setBorder(Tema.borde("Disco"));
        fila(disco, 0, "Bloques de datos (100 a 500):", txtBloques);
        fila(disco, 1, "Bloques de swap:", txtSwap);
        fila(disco, 2, "Política de planificación de disco:", cmbDisco);
        fila(disco, 3, "Posición inicial del cabezal:", txtCabezal);
        fila(disco, 4, "Velocidad del cabezal (posiciones/ciclo):", txtVelocidad);
        fila(disco, 5, "Ventana de agrupación (ciclos):", txtVentana);

        JLabel nota = new JLabel("<html>Si un JSON define alguno de estos valores, el JSON manda.</html>");
        nota.setForeground(Tema.TEXTO_SUAVE);
        nota.setFont(Tema.fuente(Font.PLAIN, 12f));

        BotonModerno btnAbrir = new BotonModerno("Abrir simulador", BotonModerno.Tipo.PRIMARIO, Iconos.de(Iconos.Forma.PLAY));
        btnAbrir.addActionListener(e -> abrirSimulador());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnAbrir);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        JPanel pila = new JPanel(new BorderLayout(0, 8));
        pila.add(json, BorderLayout.NORTH);
        JPanel medio = new JPanel(new BorderLayout(0, 8));
        medio.add(cpu, BorderLayout.NORTH);
        medio.add(disco, BorderLayout.CENTER);
        medio.add(nota, BorderLayout.SOUTH);
        pila.add(medio, BorderLayout.CENTER);
        centro.add(pila, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(centro, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnAbrir);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private static void celda(JPanel p, int x, int y, java.awt.Component c) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = x;
        g.gridy = y;
        p.add(c, g);
    }

    private static void fila(JPanel p, int y, String texto, java.awt.Component campo) {
        JLabel l = new JLabel(texto);
        celda(p, 0, y, l);
        celda(p, 1, y, campo);
    }

    private void actualizarInsignias() {
        insigniaFs.poner(archivos == null ? "sin cargar" : nombreArchivos, archivos == null ? Tema.ROJO : Tema.VERDE);
        insigniaProcesos.poner(procesos == null ? "sin cargar" : nombreProcesos, procesos == null ? Tema.ROJO : Tema.VERDE);
    }

    private void cargarArchivos() {
        Path ruta = DialogosEscenario.elegirParaAbrir(this);
        if (ruta == null) {
            return;
        }
        try {
            archivos = RepositorioArchivos.cargar(ruta);
            nombreArchivos = ruta.getFileName().toString();
            archivos.aplicarEn(base);
            txtBloques.setText(String.valueOf(base.getBloquesDisco()));
            txtCabezal.setText(String.valueOf(base.getCabezalInicial()));
            txtVelocidad.setText(String.valueOf(base.getVelocidadCabezal()));
            actualizarInsignias();
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    private void cargarProcesos() {
        Path ruta = DialogosEscenario.elegirParaAbrir(this);
        if (ruta == null) {
            return;
        }
        try {
            procesos = RepositorioProcesos.cargar(ruta);
            nombreProcesos = ruta.getFileName().toString();
            procesos.aplicarEn(base);
            txtCiclo.setText(String.valueOf(base.getDuracionCicloMs()));
            txtRam.setText(String.valueOf(base.getRam()));
            txtQuantum.setText(String.valueOf(base.getQuantum()));
            txtSwap.setText(String.valueOf(base.getBloquesSwap()));
            txtVentana.setText(String.valueOf(base.getVentanaAgrupacion()));
            cmbCpu.setSelectedItem(base.getPoliticaCpu());
            cmbDisco.setSelectedItem(base.getPoliticaDisco());
            actualizarInsignias();
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    /** Lee y valida los campos, arma el montaje y abre la ventana principal. */
    private void abrirSimulador() {
        ConfiguracionSistema cfg = new ConfiguracionSistema();
        try {
            cfg.setDuracionCicloMs(Entradas.entero(txtCiclo, "La duración del ciclo", ConfiguracionSistema.CICLO_MIN_MS,
                    ConfiguracionSistema.CICLO_MAX_MS));
            cfg.setRam(Entradas.entero(txtRam, "La RAM", ConfiguracionSistema.RAM_MIN, ConfiguracionSistema.RAM_MAX));
            cfg.setQuantum(Entradas.entero(txtQuantum, "El quantum", 1, ConfiguracionSistema.QUANTUM_MAX));
            cfg.setBloquesDisco(Entradas.entero(txtBloques, "Los bloques del disco", ConfiguracionSistema.BLOQUES_MIN,
                    ConfiguracionSistema.BLOQUES_MAX));
            cfg.setBloquesSwap(Entradas.entero(txtSwap, "Los bloques de swap", ConfiguracionSistema.SWAP_MIN,
                    ConfiguracionSistema.SWAP_MAX));
            cfg.setCabezalInicial(Entradas.entero(txtCabezal, "La posición del cabezal", 0, ConfiguracionSistema.BLOQUES_MAX - 1));
            cfg.setVelocidadCabezal(Entradas.entero(txtVelocidad, "La velocidad del cabezal", 1,
                    ConfiguracionSistema.VELOCIDAD_MAX));
            cfg.setVentanaAgrupacion(Entradas.entero(txtVentana, "La ventana de agrupación", 0,
                    ConfiguracionSistema.VENTANA_MAX));
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
            return;
        }
        cfg.setPoliticaCpu((PoliticaPlanificacion) cmbCpu.getSelectedItem());
        cfg.setPoliticaDisco((PoliticaPlanificacionDisco) cmbDisco.getSelectedItem());
        try {
            Montaje montaje = new Montaje(cfg, archivos, nombreArchivos, procesos, nombreProcesos);
            VentanaPrincipal ventana = new VentanaPrincipal(montaje);
            ventana.setVisible(true);
            dispose();
        } catch (ErrorEscenario | IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        }
    }
}
