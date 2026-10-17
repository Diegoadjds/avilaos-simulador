package com.mycompany.avilaos;

import com.mycompany.avilaos.gui.VentanaConfiguracion;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada de ÁvilaOS: abre la ventana de configuración. */
public class AvilaOS {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // si falla, se usa el aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> new VentanaConfiguracion().setVisible(true));
    }
}
