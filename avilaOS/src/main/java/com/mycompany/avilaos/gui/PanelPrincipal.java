package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.modelo.ModoUsuario;
import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.persistencia.EscenarioArchivos;
import com.mycompany.avilaos.persistencia.EscenarioProcesos;
import com.mycompany.avilaos.persistencia.RepositorioArchivos;
import com.mycompany.avilaos.persistencia.RepositorioProcesos;
import com.mycompany.avilaos.sistema.Montaje;
import com.mycompany.avilaos.sistema.SesionUsuario;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Contenido de la ventana principal: controles del reloj, modo de uso (administrador / usuario), carga de los dos
 * JSON, y las pestañas Simulación (computador + disco + log), Archivos y Métricas. Solo OBSERVA al sistema y le da
 * órdenes; las reglas (permisos, validaciones) viven en el sistema, no aquí.
 */
public class PanelPrincipal extends JPanel {

    private static final int REFRESCO_MS = 150;
    private static final int PESTANA_METRICAS = 2;

    private final SistemaAvilaOS sistema;
    private final Montaje montaje;
    private final PanelComputador panelComputador;
    private final PanelDisco panelDisco;
    private final PanelArchivos panelArchivos;
    private final PanelMetricas panelMetricas;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelReloj reloj = new PanelReloj();
    private final JLabel lblCiclo = new JLabel("0");
    private final JLabel lblEstado = new JLabel("Detenida");
    private final Insignia insigniaModo = new Insignia();
    private final Insignia insigniaFs = new Insignia();
    private final Insignia insigniaProcesos = new Insignia();
    private final BotonModerno btnIniciar = new BotonModerno("Iniciar", BotonModerno.Tipo.PRIMARIO,
            Iconos.de(Iconos.Forma.PLAY));
    private final BotonModerno btnPausa = new BotonModerno("Pausar", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.PAUSA));
    private final BotonModerno btnPaso = new BotonModerno("Paso (+1 ciclo)", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.PASO));
    private final BotonModerno btnCargarFs = new BotonModerno("Cargar sistema de archivos…", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.CARGAR));
    private final BotonModerno btnCargarProcesos = new BotonModerno("Cargar procesos…", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.CARGAR));
    private final BotonModerno btnGuardarFs = new BotonModerno("Guardar sistema de archivos…", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.GUARDAR));
    private final BotonModerno btnProceso = new BotonModerno("Nuevo proceso…", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.MAS));
    private final JSpinner spCiclo;
    private final JSpinner spQuantum;
    private final JSpinner spVelocidad;
    private final JSpinner spVentana;
    private final JComboBox<String> cmbModo = new JComboBox<>(new String[]{"Administrador", "Usuario"});
    private final JTextField txtUsuario = new JTextField("ana", 8);
    private final BotonModerno btnAplicarModo = new BotonModerno("Aplicar modo");
    private final Timer temporizador;

    public PanelPrincipal(SistemaAvilaOS sistema, Montaje montaje) {
        super(new BorderLayout(0, 10));
        this.sistema = sistema;
        this.montaje = montaje;
        setBackground(Tema.FONDO);
        setBorder(Tema.relleno(10, 12, 10, 12));

        // --- Reloj y ciclo actual ---
        lblCiclo.setFont(Tema.fuente(Font.BOLD, 30f));
        lblCiclo.setForeground(Tema.TEXTO);
        JLabel lblCicloTitulo = new JLabel("CICLO");
        lblCicloTitulo.setFont(Tema.fuente(Font.BOLD, 11f));
        lblCicloTitulo.setForeground(Tema.TEXTO_SUAVE);
        lblEstado.setFont(Tema.fuente(Font.PLAIN, 12f));
        lblEstado.setForeground(Tema.TEXTO_SUAVE);
        JPanel textoReloj = new JPanel();
        textoReloj.setLayout(new BoxLayout(textoReloj, BoxLayout.Y_AXIS));
        Tema.transparente(textoReloj);
        textoReloj.add(lblCicloTitulo);
        textoReloj.add(lblCiclo);
        textoReloj.add(lblEstado);
        JPanel bloqueReloj = new JPanel(new BorderLayout(12, 0));
        Tema.transparente(bloqueReloj);
        bloqueReloj.add(reloj, BorderLayout.WEST);
        bloqueReloj.add(textoReloj, BorderLayout.CENTER);

        // --- Fila 1: ejecución y parámetros ---
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tema.transparente(fila1);
        fila1.add(btnIniciar);
        fila1.add(btnPausa);
        fila1.add(btnPaso);
        fila1.add(Box.createHorizontalStrut(10));
        fila1.add(etiqueta("Ciclo (ms)"));
        spCiclo = new JSpinner(new SpinnerNumberModel(sistema.getConfiguracion().getDuracionCicloMs(), 10, 10000, 10));
        spCiclo.addChangeListener(e -> sistema.setDuracionCicloMs((Integer) spCiclo.getValue()));
        fila1.add(spCiclo);
        fila1.add(etiqueta("Quantum RR"));
        spQuantum = new JSpinner(new SpinnerNumberModel(sistema.getConfiguracion().getQuantum(), 1, 1000, 1));
        spQuantum.addChangeListener(e -> sistema.setQuantum((Integer) spQuantum.getValue()));
        fila1.add(spQuantum);
        fila1.add(etiqueta("Cabezal (pos/ciclo)"));
        spVelocidad = new JSpinner(new SpinnerNumberModel(sistema.getConfiguracion().getVelocidadCabezal(), 1, 500, 1));
        spVelocidad.addChangeListener(e -> sistema.setVelocidadCabezal((Integer) spVelocidad.getValue()));
        fila1.add(spVelocidad);
        fila1.add(etiqueta("Ventana de agrupación"));
        spVentana = new JSpinner(new SpinnerNumberModel(sistema.getConfiguracion().getVentanaAgrupacion(), 0, 1000, 1));
        spVentana.addChangeListener(e -> sistema.setVentanaAgrupacion((Integer) spVentana.getValue()));
        fila1.add(spVentana);

        // --- Fila 2: modo de uso ---
        ComboModerno.aplicar(cmbModo);
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tema.transparente(fila2);
        fila2.add(etiqueta("Modo"));
        fila2.add(cmbModo);
        fila2.add(etiqueta("Usuario"));
        fila2.add(txtUsuario);
        fila2.add(btnAplicarModo);
        fila2.add(insigniaModo);
        fila2.add(Box.createHorizontalStrut(10));
        fila2.add(insigniaFs);
        fila2.add(insigniaProcesos);
        cmbModo.addActionListener(e -> txtUsuario.setEnabled(cmbModo.getSelectedIndex() == 1));
        txtUsuario.setEnabled(false);
        btnAplicarModo.addActionListener(e -> aplicarModo());

        // --- Fila 3: JSON, procesos, reinicio ---
        btnCargarFs.setToolTipText("Lee el JSON con el disco y su árbol de archivos (obligatorio antes de iniciar el reloj)");
        btnCargarFs.addActionListener(e -> cargarArchivos());
        btnCargarProcesos.setToolTipText("Lee el JSON con la configuración y los procesos (obligatorio antes de iniciar el reloj)");
        btnCargarProcesos.addActionListener(e -> cargarProcesos());
        btnGuardarFs.setToolTipText("Guarda en un JSON el estado actual del sistema de archivos");
        btnGuardarFs.addActionListener(e -> guardarArchivos());
        btnProceso.setToolTipText("Crea un proceso de CPU o de E/S común (los de archivo se crean desde la pestaña Archivos)");
        btnProceso.addActionListener(e -> new DialogoProceso(SwingUtilities.getWindowAncestor(this), sistema).setVisible(true));
        BotonModerno btnNueva = new BotonModerno("Nueva simulación", BotonModerno.Tipo.PELIGRO,
                Iconos.de(Iconos.Forma.REINICIAR));
        btnNueva.setToolTipText("Empieza otra simulación: conservando el disco actual o volviendo a la configuración");
        btnNueva.addActionListener(e -> nuevaSimulacion());
        BotonModerno btnReiniciar = new BotonModerno("Reiniciar", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.REINICIAR));
        btnReiniciar.setToolTipText("Repite esta simulación desde el ciclo 0 con los mismos JSON");
        btnReiniciar.addActionListener(e -> reiniciar());
        JPanel fila3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tema.transparente(fila3);
        fila3.add(btnCargarFs);
        fila3.add(btnCargarProcesos);
        fila3.add(btnGuardarFs);
        fila3.add(btnProceso);
        JPanel filaNueva = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        Tema.transparente(filaNueva);
        filaNueva.add(btnReiniciar);
        filaNueva.add(btnNueva);
        JPanel fila3Completa = new JPanel(new BorderLayout());
        Tema.transparente(fila3Completa);
        fila3Completa.add(fila3, BorderLayout.WEST);
        fila3Completa.add(filaNueva, BorderLayout.EAST);

        JPanel filas = new JPanel(new GridLayout(3, 1));
        Tema.transparente(filas);
        filas.add(fila1);
        filas.add(fila2);
        filas.add(fila3Completa);

        Tarjeta barra = new Tarjeta(new BorderLayout(14, 0), Tema.PANEL, Tema.BORDE);
        barra.setBorder(Tema.relleno(8, 14, 8, 10));
        barra.add(bloqueReloj, BorderLayout.WEST);
        barra.add(filas, BorderLayout.CENTER);

        btnIniciar.addActionListener(e -> {
            try {
                sistema.iniciar();
            } catch (IllegalStateException ex) {
                Entradas.error(this, ex.getMessage());
            }
        });
        btnPausa.addActionListener(e -> {
            if (sistema.getReloj().isPausado()) {
                sistema.reanudar();
            } else {
                sistema.pausar();
            }
        });
        btnPaso.addActionListener(e -> {
            try {
                if (!sistema.getReloj().isCorriendo()) {
                    sistema.iniciarPausado();
                }
                sistema.avanzarUnCiclo();
            } catch (IllegalStateException ex) {
                Entradas.error(this, ex.getMessage());
            }
        });

        // --- Pestaña de simulación: computador a la izquierda (alto completo); disco y log apilados a la derecha ---
        panelComputador = new PanelComputador(sistema);
        panelDisco = new PanelDisco(sistema);
        PanelLog panelLog = new PanelLog(sistema.getRegistro());
        panelLog.setPreferredSize(new Dimension(300, 150));
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelDisco, panelLog);
        derecha.setResizeWeight(0.8);
        derecha.setBorder(null);
        derecha.setOpaque(false);
        JSplitPane simulacion = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelComputador, derecha);
        simulacion.setResizeWeight(0.58);
        javax.swing.SwingUtilities.invokeLater(() -> simulacion.setDividerLocation(0.58));
        simulacion.setBorder(Tema.relleno(10, 0, 0, 0));
        simulacion.setOpaque(false);

        panelArchivos = new PanelArchivos(sistema);
        JPanel contenedorArchivos = new JPanel(new BorderLayout());
        contenedorArchivos.setBackground(Tema.FONDO);
        contenedorArchivos.setBorder(Tema.relleno(10, 0, 0, 0));
        contenedorArchivos.add(panelArchivos, BorderLayout.CENTER);

        panelMetricas = new PanelMetricas(sistema);

        TabsModernas.aplicar(pestanas);
        pestanas.addTab("Simulación", simulacion);
        pestanas.addTab("Archivos", contenedorArchivos);
        pestanas.addTab("Métricas", panelMetricas);

        add(barra, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);

        temporizador = new Timer(REFRESCO_MS, e -> refrescar());
        aplicarPermisos();
        refrescar();
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    // =====================================================================
    //  Modo de uso
    // =====================================================================

    private void aplicarModo() {
        SesionUsuario sesion = sistema.getSesion();
        try {
            if (cmbModo.getSelectedIndex() == 0) {
                sesion.entrarComoAdministrador();
            } else {
                sesion.entrarComoUsuario(txtUsuario.getText());
            }
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
            return;
        }
        aplicarPermisos();
    }

    /** Habilita u oculta los controles según el modo. La regla real vive en el sistema; esto solo la hace visible. */
    private void aplicarPermisos() {
        SesionUsuario sesion = sistema.getSesion();
        boolean admin = sesion.esAdministrador();
        insigniaModo.poner(admin ? "Modo: ADMINISTRADOR" : "Modo: USUARIO (" + sesion.getUsuario() + ")",
                admin ? Tema.VERDE : Tema.NARANJA);
        panelComputador.setPuedeCambiarPolitica(admin);
        panelDisco.setPuedeCambiarPolitica(admin);
        btnProceso.setEnabled(admin);
        btnGuardarFs.setEnabled(admin);
        spCiclo.setEnabled(admin);
        spQuantum.setEnabled(admin);
        spVelocidad.setEnabled(admin);
        spVentana.setEnabled(admin);
        pestanas.setEnabledAt(PESTANA_METRICAS, admin);
        pestanas.setToolTipTextAt(PESTANA_METRICAS, admin ? null : "Modo usuario: las métricas son solo del administrador");
        if (!admin && pestanas.getSelectedIndex() == PESTANA_METRICAS) {
            pestanas.setSelectedIndex(0);
        }
    }

    // =====================================================================
    //  Cargar y guardar JSON; reiniciar
    // =====================================================================

    private boolean sinIniciar() {
        return !sistema.getReloj().isCorriendo() && sistema.getReloj().getCicloActual() == 0;
    }

    private void cargarArchivos() {
        Path ruta = DialogosEscenario.elegirParaAbrir(this);
        if (ruta == null) {
            return;
        }
        try {
            EscenarioArchivos fs = RepositorioArchivos.cargar(ruta);
            reemplazarMontaje(montaje.conArchivos(fs, ruta.getFileName().toString()));
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
            EscenarioProcesos pr = RepositorioProcesos.cargar(ruta);
            reemplazarMontaje(montaje.conProcesos(pr, ruta.getFileName().toString()));
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    private void guardarArchivos() {
        Path ruta = DialogosEscenario.elegirParaGuardar(this, "sistema-de-archivos.json");
        if (ruta == null) {
            return;
        }
        try {
            EscenarioArchivos foto = sistema.capturarArchivos();
            RepositorioArchivos.guardar(ruta, foto);
            DialogosEscenario.informar(this, "Sistema de archivos guardado en:\n" + ruta);
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    /** Arma otra simulación (ciclo 0) con el montaje dado y la muestra en la misma ventana. */
    private void reemplazarMontaje(Montaje nuevo) {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        if (!(ventana instanceof VentanaPrincipal)) {
            return;
        }
        try {
            ((VentanaPrincipal) ventana).mostrar(nuevo);
        } catch (ErrorEscenario | IllegalArgumentException ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    private void reiniciar() {
        int r = JOptionPane.showConfirmDialog(this,
                "Se va a repetir la simulación desde el ciclo 0 con los mismos JSON.\n"
                + "Se pierde el progreso actual y los procesos creados después de iniciar. ¿Continuar?",
                "Reiniciar simulación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            reemplazarMontaje(montaje);
        }
    }

    private void nuevaSimulacion() {
        Object[] opciones = {"Conservar el disco actual", "Volver a la configuración", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this,
                "Se termina esta simulación.\n¿Quieres empezar la nueva con el disco tal como quedó (archivos creados,\n"
                + "modificados o eliminados) o volver a la pantalla de configuración?",
                "Nueva simulación", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (r == 0) {
            reemplazarMontaje(montaje.conArchivos(sistema.capturarArchivos(), "disco de la simulación anterior")
                    .conProcesos(null, ""));
        } else if (r == 1) {
            Window ventana = SwingUtilities.getWindowAncestor(this);
            detener();
            if (ventana != null) {
                ventana.dispose();
            }
            new VentanaConfiguracion().setVisible(true);
        }
    }

    // =====================================================================
    //  Refresco
    // =====================================================================

    public void iniciarRefresco() {
        temporizador.start();
    }

    public void refrescar() {
        RelojGlobal r = sistema.getReloj();
        long ciclo = r.getCicloActual();
        boolean listo = sistema.estaListoParaIniciar();
        lblCiclo.setText(String.valueOf(ciclo));
        this.reloj.setCiclo(ciclo);
        lblEstado.setText(!r.isCorriendo() ? (listo ? "Detenida" : "Detenida · faltan JSON") : r.isPausado() ? "En pausa" : "En marcha");
        btnIniciar.setEnabled(!r.isCorriendo() && listo);
        btnIniciar.setToolTipText(listo ? null : "Primero hay que cargar los DOS JSON: sistema de archivos y procesos");
        btnPausa.setEnabled(r.isCorriendo());
        btnPausa.setText(r.isPausado() ? "Reanudar" : "Pausar");
        btnPausa.setIcon(Iconos.de(r.isPausado() ? Iconos.Forma.PLAY : Iconos.Forma.PAUSA));
        btnPaso.setEnabled(listo && (!r.isCorriendo() || r.isPausado()));
        boolean admin = sistema.getSesion().esAdministrador();
        boolean puedeCargar = sinIniciar() && admin;
        btnCargarFs.setEnabled(puedeCargar);
        btnCargarProcesos.setEnabled(puedeCargar);
        insigniaFs.poner(sistema.isArchivosCargados() ? "Archivos: " + montaje.getNombreArchivos() : "Archivos: sin cargar",
                sistema.isArchivosCargados() ? Tema.VERDE : Tema.ROJO);
        insigniaProcesos.poner(sistema.isProcesosCargados() ? "Procesos: " + montaje.getNombreProcesos() : "Procesos: sin cargar",
                sistema.isProcesosCargados() ? Tema.VERDE : Tema.ROJO);
        panelComputador.refrescar();
        panelDisco.refrescar();
        if (pestanas.getSelectedIndex() == 1) {
            panelArchivos.refrescar();
        }
        if (admin && pestanas.getSelectedIndex() == PESTANA_METRICAS) {
            panelMetricas.refrescar();
        }
    }

    /** Detiene el refresco y el reloj de la simulación. */
    public void detener() {
        temporizador.stop();
        sistema.detener();
    }
}
