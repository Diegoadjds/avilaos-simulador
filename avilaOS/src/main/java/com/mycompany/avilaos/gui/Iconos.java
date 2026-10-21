package com.mycompany.avilaos.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Iconos sencillos dibujados con Java2D (no hay imágenes ni librerías). Toman el color del texto del botón. */
public final class Iconos implements Icon {

    /** Formas disponibles. */
    public enum Forma { PLAY, PAUSA, PASO, MAS, GUARDAR, CARGAR, REINICIAR }

    private static final int TAM = 16;

    private final Forma forma;

    private Iconos(Forma forma) {
        this.forma = forma;
    }

    public static Icon de(Forma forma) {
        return new Iconos(forma);
    }

    @Override
    public int getIconWidth() {
        return TAM;
    }

    @Override
    public int getIconHeight() {
        return TAM;
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        Color color = c.isEnabled() ? c.getForeground() : Tema.TEXTO_APAGADO;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (forma) {
            case PLAY -> g.fill(triangulo(4, 2, 4, 14, 14, 8));
            case PAUSA -> {
                g.fill(new RoundRectangle2D.Float(3, 2, 4, 12, 2, 2));
                g.fill(new RoundRectangle2D.Float(9, 2, 4, 12, 2, 2));
            }
            case PASO -> {
                g.fill(triangulo(2, 2, 2, 14, 10, 8));
                g.fill(new RoundRectangle2D.Float(11, 2, 3, 12, 2, 2));
            }
            case MAS -> {
                g.drawLine(8, 3, 8, 13);
                g.drawLine(3, 8, 13, 8);
            }
            case GUARDAR -> {
                g.drawLine(8, 2, 8, 10);
                g.drawLine(4, 7, 8, 11);
                g.drawLine(12, 7, 8, 11);
                g.drawLine(3, 14, 13, 14);
            }
            case CARGAR -> {
                g.drawLine(8, 11, 8, 3);
                g.drawLine(4, 7, 8, 3);
                g.drawLine(12, 7, 8, 3);
                g.drawLine(3, 14, 13, 14);
            }
            case REINICIAR -> {
                g.draw(new Arc2D.Float(3, 3, 10, 10, 60, 280, Arc2D.OPEN));
                g.fill(triangulo(9, 0.5f, 9, 6.5f, 13.5f, 3.5f));
            }
            default -> {
            }
        }
        g.dispose();
    }

    private static Path2D triangulo(float x1, float y1, float x2, float y2, float x3, float y3) {
        Path2D p = new Path2D.Float();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.closePath();
        return p;
    }
}
