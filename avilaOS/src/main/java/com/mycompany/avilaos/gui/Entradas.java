package com.mycompany.avilaos.gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/** Validación de tipo de dato y rango para los campos de texto. Lanza IllegalArgumentException con el mensaje a mostrar. */
public final class Entradas {

    private Entradas() {
    }

    public static int entero(JTextField campo, String nombre, int min, int max) {
        String t = campo.getText().trim();
        if (t.isEmpty()) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException(nombre + ": no puede estar vacío");
        }
        int valor;
        try {
            valor = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException(nombre + ": debe ser un número entero (se escribió \"" + t + "\")");
        }
        if (valor < min || valor > max) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException(nombre + ": debe estar entre " + min + " y " + max);
        }
        return valor;
    }

    public static String texto(JTextField campo, String nombre, int maxLargo) {
        String t = campo.getText().trim();
        if (t.isEmpty()) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException(nombre + ": no puede estar vacío");
        }
        if (t.length() > maxLargo) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException(nombre + ": máximo " + maxLargo + " caracteres");
        }
        return t;
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Dato inválido", JOptionPane.ERROR_MESSAGE);
    }
}
