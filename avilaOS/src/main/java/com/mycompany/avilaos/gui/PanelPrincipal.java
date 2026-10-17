package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.RelojGlobal;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
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
    private final JLabel lblCiclo = new JLabel("Ciclo: 0");
    private final JButton btnIniciar = new JButton("Iniciar");
    private final JButton btnPausa = new JButton("Pausar");
    private final JButton btnPaso = new JButton("Paso (+1 ciclo)");
    private final Timer temporizador;

    public PanelPrincipal(ClusterAvilaOS cluster) {
        super(new BorderLayout(6, 6));
        this.cluster = cluster;

        // --- Controles globales ---
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        lblCiclo.setFont(lblCiclo.getFont().deriveFont(Font.BOLD, 16f));
        controles.add(lblCiclo);
        controles.add(btnIniciar);
        controles.add(btnPausa);
        controles.add(btnPaso);

        controles.add(new JLabel("  Duración del ciclo (ms):"));
        JSpinner spCiclo = new JSpinner(new SpinnerNumberModel(
                cluster.getConfiguracion().getDuracionCicloMs(), 10, 10000, 10));
        spCiclo.addChangeListener(e -> cluster.setDuracionCicloMs((Integer) spCiclo.getValue()));
        controles.add(spCiclo);

        controles.add(new JLabel("  Quantum RR:"));
        JSpinner spQuantum = new JSpinner(new SpinnerNumberModel(cluster.getConfiguracion().getQuantum(), 1, 1000, 1));
        spQuantum.addChangeListener(e -> cluster.setQuantum((Integer) spQuantum.getValue()));
        controles.add(spQuantum);

        controles.add(new JLabel("  Latencia de red (ciclos):"));
        JSpinner spLatencia = new JSpinner(new SpinnerNumberModel(cluster.getConfiguracion().getLatenciaRed(), 0, 1000, 1));
        spLatencia.addChangeListener(e -> cluster.setLatenciaRed((Integer) spLatencia.getValue()));
        controles.add(spLatencia);

        JButton btnProceso = new JButton("Nuevo proceso…");
        btnProceso.addActionListener(e -> new DialogoProceso(SwingUtilities.getWindowAncestor(this), cluster).setVisible(true));
        JButton btnBuffer = new JButton("Nuevo buffer…");
        btnBuffer.addActionListener(e -> new DialogoBuffer(SwingUtilities.getWindowAncestor(this), cluster).setVisible(true));
        controles.add(btnProceso);
        controles.add(btnBuffer);

        btnIniciar.addActionListener(e -> cluster.iniciar());
        btnPausa.addActionListener(e -> {
            if (cluster.getReloj().isPausado()) {
                cluster.reanudar();
            } else {
                cluster.pausar();
            }
        });
        btnPaso.addActionListener(e -> {
            RelojGlobal reloj = cluster.getReloj();
            if (!reloj.isCorriendo()) {
                cluster.iniciarPausado();
            }
            cluster.avanzarUnCiclo();
        });

        // --- Pestaña de simulación ---
        int n = cluster.getNumComputadores();
        paneles = new PanelComputador[n];
        JPanel rejilla = new JPanel(new GridLayout(0, n <= 2 ? n : 2, 8, 8));
        for (int i = 0; i < n; i++) {
            paneles[i] = new PanelComputador(cluster.getComputador(i + 1));
            rejilla.add(paneles[i]);
        }
        panelBuffers = new PanelBuffers(cluster);
        PanelLog panelLog = new PanelLog(cluster.getRegistro());
        JSplitPane abajo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelBuffers, panelLog);
        abajo.setResizeWeight(0.55);
        abajo.setPreferredSize(new Dimension(800, 210));

        JPanel simulacion = new JPanel(new BorderLayout(6, 6));
        simulacion.add(new JScrollPane(rejilla), BorderLayout.CENTER);
        simulacion.add(abajo, BorderLayout.SOUTH);

        panelMetricas = new PanelMetricas(cluster);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Simulación", simulacion);
        pestanas.addTab("Métricas", panelMetricas);

        add(controles, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);

        temporizador = new Timer(REFRESCO_MS, e -> refrescar());
        refrescar();
    }

    /** Arranca el refresco periódico de la pantalla. */
    public void iniciarRefresco() {
        temporizador.start();
    }

    public void refrescar() {
        RelojGlobal reloj = cluster.getReloj();
        lblCiclo.setText("Ciclo: " + reloj.getCicloActual());
        btnIniciar.setEnabled(!reloj.isCorriendo());
        btnPausa.setEnabled(reloj.isCorriendo());
        btnPausa.setText(reloj.isPausado() ? "Reanudar" : "Pausar");
        btnPaso.setEnabled(!reloj.isCorriendo() || reloj.isPausado());
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
