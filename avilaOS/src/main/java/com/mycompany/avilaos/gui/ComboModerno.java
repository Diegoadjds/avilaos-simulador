package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Da a un JComboBox el aspecto plano oscuro del tema (campo oscuro y flecha dibujada). */
public final class ComboModerno {

    private ComboModerno() {
    }

    /** Aplica el estilo al combo (no cambia sus datos ni su comportamiento). */
    public static void aplicar(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new Flecha();
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean enfocado) {
                g.setColor(Tema.CAMPO);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        });
        combo.setBackground(Tema.CAMPO);
        combo.setForeground(Tema.TEXTO);
        combo.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        combo.setFont(Tema.fuente(java.awt.Font.PLAIN, 13f));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean enfocado) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, false);
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                boolean resaltado = seleccionado && indice >= 0;
                setBackground(resaltado ? Tema.ACENTO : Tema.CAMPO);
                setForeground(resaltado ? Color.WHITE : Tema.TEXTO);
                return this;
            }
        });
    }

    /** Botón de la flecha: triángulo pequeño sobre fondo del campo. */
    private static final class Flecha extends JButton {

        Flecha() {
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFocusable(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(24, 20);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Tema.CAMPO);
            g.fillRect(0, 0, getWidth(), getHeight());
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            Path2D flecha = new Path2D.Double();
            flecha.moveTo(cx - 4, cy - 2);
            flecha.lineTo(cx + 4, cy - 2);
            flecha.lineTo(cx, cy + 3);
            flecha.closePath();
            g.setColor(Tema.TEXTO_SUAVE);
            g.fill(flecha);
            g.dispose();
        }
    }
}
