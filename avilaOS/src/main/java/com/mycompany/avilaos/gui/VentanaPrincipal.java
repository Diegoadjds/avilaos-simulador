package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** Ventana principal de la simulación. */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal(ClusterAvilaOS cluster) {
        super("ÁvilaOS – Simulador de sistema operativo distribuido");
        PanelPrincipal panel = new PanelPrincipal(cluster);
        setContentPane(panel);
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
        setLocationRelativeTo(null);
        panel.iniciarRefresco();
    }
}
