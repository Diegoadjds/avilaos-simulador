package com.mycompany.avilaos.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Borde de una sección de la pantalla: contorno redondeado fino y un título pequeño arriba a la izquierda.
 * El título se puede cambiar (por ejemplo, para mostrar cuántos procesos hay en una cola).
 */
public class BordeSeccion extends AbstractBorder {

    private static final int RADIO = 12;
    private static final int ALTO_TITULO = 26;

    private volatile String titulo;

    public BordeSeccion(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    @Override
    public void paintBorder(Component c, Graphics g0, int x, int y, int ancho, int alto) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Tema.BORDE);
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, ancho - 1f, alto - 1f, RADIO, RADIO));
        if (!titulo.isEmpty()) {
            g.setFont(Tema.fuente(java.awt.Font.BOLD, 12f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(titulo, x + 12, y + 7 + fm.getAscent());
        }
        g.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(titulo.isEmpty() ? 10 : ALTO_TITULO + 4, 10, 10, 10);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets i = getBorderInsets(c);
        insets.set(i.top, i.left, i.bottom, i.right);
        return insets;
    }

    /** Color del contorno, por si una pantalla quiere resaltarlo. */
    public Color getColor() {
        return Tema.BORDE;
    }
}
