package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sistema.Computador;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Gráfica con la utilización del procesador de cada computador respecto al tiempo (dibujada con Java2D). */
public class GraficoUtilizacion extends JPanel {

    private static final Color[] COLORES = Tema.SERIES;
    private static final int CICLOS_POR_PUNTO = 10;

    private final Computador[] computadores;

    public GraficoUtilizacion(Computador[] computadores) {
        this.computadores = computadores;
        setPreferredSize(new java.awt.Dimension(600, 260));
        setBackground(Tema.CAMPO);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Tema.fuente(java.awt.Font.PLAIN, 12f));
        int w = getWidth();
        int h = getHeight();
        int mi = 50;
        int md = 20;
        int ms = 34;
        int mb = 40;
        int pw = w - mi - md;
        int ph = h - ms - mb;
        if (pw < 40 || ph < 40) {
            g.dispose();
            return;
        }
        Color fg = Tema.TEXTO_SUAVE;
        Color rejilla = Tema.BORDE;

        g.setColor(Tema.TEXTO);
        g.drawString("Utilización del procesador vs. tiempo (cada punto = promedio de " + CICLOS_POR_PUNTO + " ciclos)", mi, 18);
        for (int i = 0; i <= 4; i++) {
            int y = ms + ph - ph * i / 4;
            g.setColor(rejilla);
            g.drawLine(mi, y, mi + pw, y);
            g.setColor(fg);
            g.drawString((i * 25) + "%", 12, y + 4);
        }
        g.drawLine(mi, ms, mi, ms + ph);
        g.drawLine(mi, ms + ph, mi + pw, ms + ph);

        Double[][] series = new Double[computadores.length][];
        int maxPuntos = 6;
        for (int i = 0; i < computadores.length; i++) {
            series[i] = computadores[i].getKernel().getMetricas().getHistorialUtilizacion();
            maxPuntos = Math.max(maxPuntos, series[i].length);
        }
        for (int k = 0; k <= 5; k++) {
            int x = mi + pw * k / 5;
            g.setColor(fg);
            g.drawString(String.valueOf(maxPuntos * CICLOS_POR_PUNTO * k / 5), x - 10, ms + ph + 16);
        }
        g.drawString("Ciclos de reloj", mi + pw / 2 - 35, h - 8);

        for (int i = 0; i < computadores.length; i++) {
            // trazos de distinto grosor para que se vean aunque dos series coincidan
            g.setStroke(new BasicStroke(Math.max(2f, 6f - 1.5f * i)));
            g.setColor(COLORES[i % COLORES.length]);
            int px = mi;
            int py = ms + ph;
            for (int j = 0; j < series[i].length; j++) {
                int x = mi + (int) (pw * (j + 1.0) / maxPuntos);
                int y = ms + ph - (int) (ph * series[i][j]);
                g.drawLine(px, py, x, y);
                g.fillOval(x - 3, y - 3, 6, 6);
                px = x;
                py = y;
            }
            // leyenda en la fila del título, de derecha a izquierda, para no tapar las líneas
            int lx = w - md - (computadores.length - i) * 125;
            int ly = 16;
            g.setStroke(new BasicStroke(1f));
            g.fillRoundRect(lx, ly - 6, 16, 6, 4, 4);
            g.setColor(fg);
            g.drawString("Computador " + computadores[i].getId(), lx + 22, ly);
        }
        g.dispose();
    }
}
