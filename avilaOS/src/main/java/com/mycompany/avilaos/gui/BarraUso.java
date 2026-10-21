package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** Barra horizontal de uso (por ejemplo, de la RAM) con un texto centrado. Cambia de color si se llena. */
public class BarraUso extends JComponent {

    private int valor;
    private int maximo = 1;
    private String texto = "";

    public BarraUso() {
        setPreferredSize(new Dimension(200, 22));
        setOpaque(false);
    }

    /** Actualiza la barra (solo vuelve a dibujar si algo cambió). */
    public void poner(int valor, int maximo, String texto) {
        int m = Math.max(1, maximo);
        if (valor != this.valor || m != this.maximo || !texto.equals(this.texto)) {
            this.valor = valor;
            this.maximo = m;
            this.texto = texto;
            repaint();
        }
    }

    /** Fracción usada (0..1). */
    double fraccion() {
        return Math.max(0, Math.min(1, (double) valor / maximo));
    }

    private Color colorRelleno() {
        double f = fraccion();
        if (f >= 0.9) {
            return Tema.ROJO;
        }
        if (f >= 0.7) {
            return Tema.NARANJA;
        }
        return Tema.ACENTO;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D pista = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h, h);
        g.setColor(Tema.CAMPO);
        g.fill(pista);
        int relleno = (int) Math.round((w - 1) * fraccion());
        if (relleno > 0) {
            g.setClip(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h, h));
            g.setColor(Tema.mezclar(Tema.CAMPO, colorRelleno(), 0.75f));
            g.fillRect(0, 0, Math.max(relleno, h / 2), h);
            g.setClip(null);
        }
        g.setColor(Tema.BORDE);
        g.draw(pista);
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        g.drawString(texto, (w - fm.stringWidth(texto)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
