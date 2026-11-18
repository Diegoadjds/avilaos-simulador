package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.sistema.Montaje;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** Ventana principal de la simulación. */
public class VentanaPrincipal extends JFrame {

    private PanelPrincipal panel;

    /** @throws ErrorEscenario si los JSON del montaje no encajan entre sí */
    public VentanaPrincipal(Montaje montaje) throws ErrorEscenario {
        super("ÁvilaOS – Simulador de sistema operativo");
        mostrar(montaje);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.detener();
                dispose();
                System.exit(0);
            }
        });
        setSize(1440, 900);
        setMinimumSize(new java.awt.Dimension(1150, 720));
        setLocationRelativeTo(null);
    }

    /** Muestra una simulación nueva (ciclo 0) armada con el montaje; la anterior se detiene. */
    public void mostrar(Montaje montaje) throws ErrorEscenario {
        SistemaAvilaOS sistema = montaje.construir();
        if (panel != null) {
            panel.detener();
        }
        panel = new PanelPrincipal(sistema, montaje);
        setContentPane(panel);
        revalidate();
        repaint();
        panel.iniciarRefresco();
    }
}
