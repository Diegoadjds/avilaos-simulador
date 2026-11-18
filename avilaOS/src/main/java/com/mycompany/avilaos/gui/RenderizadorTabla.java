package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Dibuja las celdas de las tablas con el tema oscuro: filas alternas, números centrados y colores
 * según el contenido (tipo de proceso, motivo de bloqueo, resultado, deadline a punto de vencer).
 * Solo cambia cómo se ve; los datos los entrega cada modelo de tabla.
 */
public class RenderizadorTabla extends DefaultTableCellRenderer {

    /** Deadline restante a partir del cual el número se resalta en rojo. */
    private static final int DEADLINE_CRITICO = 5;

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
            boolean enfocada, int fila, int columna) {
        super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        setFont(Tema.fuente(Font.PLAIN, 12f));
        String texto = valor == null ? "" : valor.toString();
        String nombre = tabla.getColumnName(columna);

        setHorizontalAlignment(centrada(nombre) ? SwingConstants.CENTER : SwingConstants.LEFT);

        if (seleccionada) {
            return this;
        }
        setBackground(fila % 2 == 0 ? Tema.CAMPO : new Color(0x142034));
        setForeground(colorDe(nombre, valor, texto));
        if ("ID".equals(nombre) || columna == 0 && !centrada(nombre)) {
            setFont(Tema.fuente(Font.BOLD, 12f));
        }
        if ("Resultado".equals(nombre) && texto.contains("vencido")) {
            setFont(Tema.fuente(Font.BOLD, 12f));
        }
        return this;
    }

    /** Las columnas de texto van a la izquierda; las de números y valores cortos, centradas. */
    static boolean centrada(String columna) {
        switch (columna) {
            case "":
            case "ID":
            case "Nombre":
            case "Tipo":
            case "Motivo":
            case "Resultado":
            case "Operación":
            case "Proceso":
            case "Ruta":
            case "Dueño":
            case "Creado por":
            case "Permiso":
            case "Política":
                return false;
            default:
                return true;
        }
    }

    /** Color del texto según la columna y el valor. */
    private static Color colorDe(String columna, Object valor, String texto) {
        switch (columna) {
            case "ID":
                return Tema.AZUL;
            case "Tipo":
                if (texto.startsWith("CPU")) {
                    return Tema.AZUL;
                }
                if (texto.startsWith("I/O")) {
                    return Tema.MORADO;
                }
                if (texto.startsWith("E/S")) {
                    return Tema.CIAN;
                }
                return Tema.TEXTO;
            case "Motivo":
                return Tema.NARANJA;
            case "Resultado":
                return texto.contains("vencido") || texto.startsWith("Error") ? Tema.ROJO : Tema.VERDE;
            case "Deadline":
            case "Deadl.":
                return valor instanceof Number n && n.intValue() <= DEADLINE_CRITICO ? Tema.ROJO : Tema.TEXTO;
            case "Vencidos":
                return valor != null && !"0".equals(texto) ? Tema.ROJO : Tema.TEXTO;
            default:
                return Tema.TEXTO;
        }
    }

    /** Encabezado plano con el texto pequeño y apagado. */
    public static class Cabecera extends DefaultTableCellRenderer {

        public Cabecera() {
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                boolean enfocada, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setBackground(Tema.TARJETA_ALT);
            setForeground(Tema.TEXTO_SUAVE);
            setFont(Tema.fuente(Font.BOLD, 11f));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                    BorderFactory.createEmptyBorder(0, 4, 0, 4)));
            setHorizontalAlignment(centrada(tabla.getColumnName(columna)) ? JLabel.CENTER : JLabel.LEFT);
            return this;
        }
    }
}
