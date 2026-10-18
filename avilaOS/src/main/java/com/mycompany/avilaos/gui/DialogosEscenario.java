package com.mycompany.avilaos.gui;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Selectores de archivo y mensajes comunes para abrir y guardar escenarios (.json). */
public final class DialogosEscenario {

    private static final FileNameExtensionFilter FILTRO =
            new FileNameExtensionFilter("Escenario de ÁvilaOS (*.json)", "json");

    /** Recuerda la última carpeta usada durante la sesión. */
    private static File ultimaCarpeta;

    private DialogosEscenario() {
    }

    /** Pide un archivo existente. Devuelve null si el usuario cancela. */
    public static Path elegirParaAbrir(Component padre) {
        JFileChooser selector = new JFileChooser(ultimaCarpeta);
        selector.setDialogTitle("Cargar escenario");
        selector.setFileFilter(FILTRO);
        if (selector.showOpenDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        ultimaCarpeta = selector.getCurrentDirectory();
        return selector.getSelectedFile().toPath();
    }

    /**
     * Pide dónde guardar. Agrega la extensión .json si falta y pregunta antes de sobrescribir.
     * Devuelve null si el usuario cancela.
     */
    public static Path elegirParaGuardar(Component padre, String nombreSugerido) {
        JFileChooser selector = new JFileChooser(ultimaCarpeta);
        selector.setDialogTitle("Guardar escenario");
        selector.setFileFilter(FILTRO);
        selector.setSelectedFile(new File(ultimaCarpeta, nombreSugerido));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        ultimaCarpeta = selector.getCurrentDirectory();
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".json")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".json");
        }
        if (archivo.exists()) {
            int respuesta = JOptionPane.showConfirmDialog(padre,
                    "El archivo \"" + archivo.getName() + "\" ya existe. ¿Desea reemplazarlo?",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return archivo.toPath();
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Escenario", JOptionPane.ERROR_MESSAGE);
    }

    public static void informar(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Escenario", JOptionPane.INFORMATION_MESSAGE);
    }
}
