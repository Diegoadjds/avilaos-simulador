package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/** Panel con fondo redondeado de un color (y contorno opcional). Lo que lleva dentro debe ser transparente. */
public class Tarjeta extends JPanel {

    private static final int RADIO = 14;

    private final Color relleno;
    private final Color contorno;

    public Tarjeta(LayoutManager layout, Color relleno, Color contorno) {
        super(layout);
        this.relleno = relleno;
        this.contorno = contorno;
        setOpaque(false);
    }

    public Tarjeta(LayoutManager layout) {
        this(layout, Tema.TARJETA, Tema.BORDE);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, RADIO, RADIO);
        g.setColor(relleno);
        g.fill(forma);
        if (contorno != null) {
            g.setColor(contorno);
            g.draw(forma);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
