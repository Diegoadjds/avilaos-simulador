package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.persistencia.Escenario;
import com.mycompany.avilaos.persistencia.RepositorioEscenario;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import com.mycompany.avilaos.modelo.GeneradorId;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.GridLayout;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Contenido de la ventana principal: controles globales, un panel por computador (Vista 2),
 * buffers, log y métricas. Solo OBSERVA al cluster y le da órdenes; no tiene lógica del SO.
 */
public class PanelPrincipal extends JPanel {

    private static final int REFRESCO_MS = 150;

    private final ClusterAvilaOS cluster;
    private final PanelComputador[] paneles;
    private final PanelBuffers panelBuffers;
    private final PanelMetricas panelMetricas;
    private final PanelReloj reloj = new PanelReloj();
    private final JLabel lblCiclo = new JLabel("0");
    private final JLabel lblEstado = new JLabel("Detenida");
    private final BotonModerno btnIniciar = new BotonModerno("Iniciar", BotonModerno.Tipo.PRIMARIO,
            Iconos.de(Iconos.Forma.PLAY));
    private final BotonModerno btnPausa = new BotonModerno("Pausar", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.PAUSA));
    private final BotonModerno btnPaso = new BotonModerno("Paso (+1 ciclo)", BotonModerno.Tipo.NORMAL,
            Iconos.de(Iconos.Forma.PASO));
    private final Timer temporizador;

    public PanelPrincipal(ClusterAvilaOS cluster) {
        super(new BorderLayout(0, 10));
        this.cluster = cluster;
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
        fila1.add(etiqueta("Duración del ciclo (ms)"));
        JSpinner spCiclo = new JSpinner(new SpinnerNumberModel(
                cluster.getConfiguracion().getDuracionCicloMs(), 10, 10000, 10));
        spCiclo.addChangeListener(e -> cluster.setDuracionCicloMs((Integer) spCiclo.getValue()));
        fila1.add(spCiclo);
        fila1.add(etiqueta("Quantum RR"));
        JSpinner spQuantum = new JSpinner(new SpinnerNumberModel(cluster.getConfiguracion().getQuantum(), 1, 1000, 1));
        spQuantum.addChangeListener(e -> cluster.setQuantum((Integer) spQuantum.getValue()));
        fila1.add(spQuantum);
        fila1.add(etiqueta("Latencia de red (ciclos)"));
        JSpinner spLatencia = new JSpinner(new SpinnerNumberModel(cluster.getConfiguracion().getLatenciaRed(), 0, 1000, 1));
        spLatencia.addChangeListener(e -> cluster.setLatenciaRed((Integer) spLatencia.getValue()));
        fila1.add(spLatencia);

        // --- Fila 2: crear, guardar y volver al inicio ---
        BotonModerno btnProceso = new BotonModerno("Nuevo proceso…", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.MAS));
        btnProceso.addActionListener(e -> new DialogoProceso(SwingUtilities.getWindowAncestor(this), cluster).setVisible(true));
        BotonModerno btnBuffer = new BotonModerno("Nuevo buffer…", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.MAS));
        btnBuffer.addActionListener(e -> new DialogoBuffer(SwingUtilities.getWindowAncestor(this), cluster).setVisible(true));
        BotonModerno btnGuardar = new BotonModerno("Guardar escenario…", BotonModerno.Tipo.NORMAL,
                Iconos.de(Iconos.Forma.GUARDAR));
        btnGuardar.setToolTipText("Guarda en un archivo JSON la configuración actual y todos los buffers y procesos creados");
        btnGuardar.addActionListener(e -> guardarEscenario());
        BotonModerno btnNueva = new BotonModerno("Nueva simulación", BotonModerno.Tipo.PELIGRO,
                Iconos.de(Iconos.Forma.REINICIAR));
        btnNueva.setToolTipText("Termina esta simulación y vuelve a la pantalla de configuración inicial");
        btnNueva.addActionListener(e -> nuevaSimulacion());
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tema.transparente(fila2);
        fila2.add(btnProceso);
        fila2.add(btnBuffer);
        fila2.add(btnGuardar);
        JPanel filaNueva = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        Tema.transparente(filaNueva);
        filaNueva.add(btnNueva);
        JPanel fila2Completa = new JPanel(new BorderLayout());
        Tema.transparente(fila2Completa);
        fila2Completa.add(fila2, BorderLayout.WEST);
        fila2Completa.add(filaNueva, BorderLayout.EAST);

        JPanel filas = new JPanel(new GridLayout(2, 1));
        Tema.transparente(filas);
        filas.add(fila1);
        filas.add(fila2Completa);

        Tarjeta barra = new Tarjeta(new BorderLayout(14, 0), Tema.PANEL, Tema.BORDE);
        barra.setBorder(Tema.relleno(8, 14, 8, 10));
        barra.add(bloqueReloj, BorderLayout.WEST);
        barra.add(filas, BorderLayout.CENTER);

        btnIniciar.addActionListener(e -> cluster.iniciar());
        btnPausa.addActionListener(e -> {
            if (cluster.getReloj().isPausado()) {
                cluster.reanudar();
            } else {
                cluster.pausar();
            }
        });
        btnPaso.addActionListener(e -> {
            RelojGlobal r = cluster.getReloj();
            if (!r.isCorriendo()) {
                cluster.iniciarPausado();
            }
            cluster.avanzarUnCiclo();
        });

        // --- Pestaña de simulación ---
        int n = cluster.getNumComputadores();
        paneles = new PanelComputador[n];
        JPanel rejilla = new JPanel(new GridLayout(0, n <= 2 ? n : 2, 10, 10));
        rejilla.setBackground(Tema.FONDO);
        for (int i = 0; i < n; i++) {
            paneles[i] = new PanelComputador(cluster.getComputador(i + 1));
            rejilla.add(paneles[i]);
        }
        panelBuffers = new PanelBuffers(cluster);
        PanelLog panelLog = new PanelLog(cluster.getRegistro());
        JSplitPane abajo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelBuffers, panelLog);
        abajo.setResizeWeight(0.55);
        abajo.setPreferredSize(new Dimension(800, 190));

        JScrollPane scrollRejilla = new JScrollPane(rejilla);
        Tema.estilizarScroll(scrollRejilla);
        scrollRejilla.setBorder(null);
        scrollRejilla.getViewport().setBackground(Tema.FONDO);
        JPanel simulacion = new JPanel(new BorderLayout(0, 10));
        simulacion.setBackground(Tema.FONDO);
        simulacion.setBorder(Tema.relleno(10, 0, 0, 0));
        simulacion.add(scrollRejilla, BorderLayout.CENTER);
        simulacion.add(abajo, BorderLayout.SOUTH);

        panelMetricas = new PanelMetricas(cluster);

        JTabbedPane pestanas = new JTabbedPane();
        TabsModernas.aplicar(pestanas);
        pestanas.addTab("Simulación", simulacion);
        pestanas.addTab("Métricas", panelMetricas);

        add(barra, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);

        temporizador = new Timer(REFRESCO_MS, e -> refrescar());
        refrescar();
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    /** Termina la simulación actual (previa confirmación) y vuelve a la pantalla de configuración. */
    private void nuevaSimulacion() {
        int r = JOptionPane.showConfirmDialog(this,
                "Se va a terminar la simulación actual y se volverá a la pantalla de configuración.\n"
                + "Si no la guardaste como escenario, se pierde. ¿Continuar?",
                "Nueva simulación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        if (!cluster.getReloj().isCorriendo()) {
            cluster.iniciarPausado(); // así existen los hilos de los componentes y detener() los apaga
        }
        detener();
        GeneradorId.reiniciar(); // los procesos de la nueva simulación vuelven a empezar en P1
        Window ventana = SwingUtilities.getWindowAncestor(this);
        if (ventana != null) {
            ventana.dispose();
        }
        new VentanaConfiguracion().setVisible(true);
    }

    /** Guarda en un archivo JSON la configuración vigente y todos los buffers y procesos creados. */
    private void guardarEscenario() {
        Escenario escenario = Escenario.capturar(cluster);
        Path ruta = DialogosEscenario.elegirParaGuardar(this, "escenario.json");
        if (ruta == null) {
            return;
        }
        try {
            RepositorioEscenario.guardar(ruta, escenario);
            DialogosEscenario.informar(this, "Escenario guardado en:\n" + ruta + "\n(" + escenario.describirCarga() + ")");
        } catch (ErrorEscenario ex) {
            DialogosEscenario.error(this, ex.getMessage());
        }
    }

    /** Arranca el refresco periódico de la pantalla. */
    public void iniciarRefresco() {
        temporizador.start();
    }

    public void refrescar() {
        RelojGlobal r = cluster.getReloj();
        long ciclo = r.getCicloActual();
        lblCiclo.setText(String.valueOf(ciclo));
        this.reloj.setCiclo(ciclo);
        lblEstado.setText(!r.isCorriendo() ? "Detenida" : r.isPausado() ? "En pausa" : "En marcha");
        btnIniciar.setEnabled(!r.isCorriendo());
        btnPausa.setEnabled(r.isCorriendo());
        btnPausa.setText(r.isPausado() ? "Reanudar" : "Pausar");
        btnPausa.setIcon(Iconos.de(r.isPausado() ? Iconos.Forma.PLAY : Iconos.Forma.PAUSA));
        btnPaso.setEnabled(!r.isCorriendo() || r.isPausado());
        for (PanelComputador p : paneles) {
            p.refrescar();
        }
        panelBuffers.refrescar();
        panelMetricas.refrescar();
    }

    /** Detiene el refresco y el reloj de la simulación. */
    public void detener() {
        temporizador.stop();
        cluster.detener();
    }
}
