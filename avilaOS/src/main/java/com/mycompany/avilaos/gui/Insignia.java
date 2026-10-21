package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** Etiqueta con forma de píldora y un color de acento (por ejemplo, para el modo del procesador). */
public class Insignia extends JLabel {

    private Color color = Tema.GRIS;

    public Insignia() {
        setFont(Tema.fuente(Font.BOLD, 11.5f));
        setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        setOpaque(false);
    }

    /** Cambia el texto y el color (solo vuelve a dibujar si algo cambió). */
    public void poner(String texto, Color color) {
        if (!texto.equals(getText()) || !color.equals(this.color)) {
            this.color = color;
            setForeground(color);
            setText(texto);
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 22));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                getHeight(), getHeight());
        g.setColor(Tema.mezclar(Tema.TARJETA, color, 0.18f));
        g.fill(forma);
        g.setColor(Tema.mezclar(Tema.TARJETA, color, 0.55f));
        g.draw(forma);
        g.dispose();
        super.paintComponent(g0);
    }
}
