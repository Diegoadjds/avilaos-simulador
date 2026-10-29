package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.persistencia.Escenario;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** Ventana principal de la simulación. */
public class VentanaPrincipal extends JFrame {

    private final Escenario escenarioInicial;
    private PanelPrincipal panel;

    /**
     * @param cluster          la simulación que se muestra
     * @param escenarioInicial el escenario con el que se creó, para poder repetirla con "Reiniciar"
     */
    public VentanaPrincipal(ClusterAvilaOS cluster, Escenario escenarioInicial) {
        super("ÁvilaOS – Simulador de sistema operativo distribuido");
        this.escenarioInicial = escenarioInicial;
        mostrar(cluster);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.detener();
                dispose();
                System.exit(0);
            }
        });
        setSize(1400, 860);
        setMinimumSize(new java.awt.Dimension(1100, 700));
        setLocationRelativeTo(null);
    }

    /** Muestra una simulación en esta ventana (la primera vez, o la nueva tras "Reiniciar"). */
    public void mostrar(ClusterAvilaOS cluster) {
        panel = new PanelPrincipal(cluster, escenarioInicial);
        setContentPane(panel);
        revalidate();
        repaint();
        panel.iniciarRefresco();
    }
}
