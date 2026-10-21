package com.mycompany.avilaos;

import com.mycompany.avilaos.gui.Tema;
import com.mycompany.avilaos.gui.VentanaConfiguracion;
import javax.swing.SwingUtilities;

/** Punto de entrada de ÁvilaOS: aplica el tema visual y abre la ventana de configuración. */
public class AvilaOS {

    public static void main(String[] args) {
        Tema.instalar();
        SwingUtilities.invokeLater(() -> new VentanaConfiguracion().setVisible(true));
    }
}
