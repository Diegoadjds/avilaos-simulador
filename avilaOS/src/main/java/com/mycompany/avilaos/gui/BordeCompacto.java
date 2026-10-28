package com.mycompany.avilaos.gui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Borde de una cola dentro de la rejilla 2x2: igual que {@link BordeSeccion} pero con título más pequeño
 * y menos margen, para ocupar poco espacio. El título se puede cambiar (muestra cuántos procesos hay).
 */
public class BordeCompacto extends AbstractBorder {

    private static final int RADIO = 10;
    private static final int ALTO_TITULO = 22;
    private static final int MARGEN = 7;

    private volatile String titulo;

    public BordeCompacto(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    @Override
    public void paintBorder(Component c, Graphics g0, int x, int y, int ancho, int alto) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Tema.BORDE);
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, ancho - 1f, alto - 1f, RADIO, RADIO));
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString(titulo, x + 10, y + 5 + fm.getAscent());
        g.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(ALTO_TITULO, MARGEN, MARGEN, MARGEN);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(ALTO_TITULO, MARGEN, MARGEN, MARGEN);
        return insets;
    }
}
