package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.so.MetricasDisco;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Gráfica del desplazamiento TOTAL acumulado del cabezal contra el tiempo (ciclos). Cada tramo se pinta del color de
 * la política que estaba activa, así se ve el efecto de cambiar de política en caliente. Dibujada con Java2D.
 */
public class GraficoDesplazamiento extends JPanel {

    private final MetricasDisco metricas;

    public GraficoDesplazamiento(MetricasDisco metricas) {
        this.metricas = metricas;
        setPreferredSize(new java.awt.Dimension(600, 260));
        setBackground(Tema.CAMPO);
    }

    static Color colorPolitica(PoliticaPlanificacionDisco p) {
        return Tema.SERIES[p.ordinal() % Tema.SERIES.length];
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Tema.fuente(Font.PLAIN, 12f));
        int w = getWidth();
        int h = getHeight();
        int mi = 62;
        int md = 20;
        int ms = 36;
        int mb = 40;
        int pw = w - mi - md;
        int ph = h - ms - mb;
        if (pw < 40 || ph < 40) {
            g.dispose();
            return;
        }
        MetricasDisco.Punto[] curva = metricas.getCurva();
        long maxCiclo = 10;
        long maxDesp = 10;
        for (MetricasDisco.Punto p : curva) {
            maxCiclo = Math.max(maxCiclo, p.ciclo());
            maxDesp = Math.max(maxDesp, p.desplazamiento());
        }
        maxDesp = (maxDesp / 10 + 1) * 10;

        g.setColor(Tema.TEXTO);
        g.drawString("Desplazamiento acumulado del cabezal vs. tiempo", mi, 20);
        for (int i = 0; i <= 4; i++) {
            int y = ms + ph - ph * i / 4;
            g.setColor(Tema.BORDE);
            g.drawLine(mi, y, mi + pw, y);
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(String.valueOf(maxDesp * i / 4), 14, y + 4);
        }
        for (int k = 0; k <= 5; k++) {
            int x = mi + pw * k / 5;
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(String.valueOf(maxCiclo * k / 5), x - 10, ms + ph + 16);
        }
        g.drawLine(mi, ms, mi, ms + ph);
        g.drawLine(mi, ms + ph, mi + pw, ms + ph);
        g.drawString("Ciclos de reloj", mi + pw / 2 - 35, h - 8);
        g.drawString("Pistas", 8, ms - 8);

        // Una columna de píxeles = como mucho un punto: así la curva larga no cuesta nada de dibujar.
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int px = mi;
        int py = ms + ph;
        int ultimoX = -1;
        for (int i = 0; i < curva.length; i++) {
            MetricasDisco.Punto p = curva[i];
            int x = mi + (int) (pw * (double) p.ciclo() / maxCiclo);
            int y = ms + ph - (int) (ph * (double) p.desplazamiento() / maxDesp);
            if (x == ultimoX && i < curva.length - 1) {
                continue;
            }
            g.setColor(colorPolitica(p.politica()));
            g.drawLine(px, py, x, y);
            px = x;
            py = y;
            ultimoX = x;
        }

        // Leyenda: solo las políticas que se usaron.
        int lx = mi + pw;
        PoliticaPlanificacionDisco[] ps = PoliticaPlanificacionDisco.values();
        for (int i = ps.length - 1; i >= 0; i--) {
            if (metricas.getAtendidas(ps[i]) == 0 && metricas.getDesplazamiento(ps[i]) == 0) {
                continue;
            }
            String nombre = ps[i].toString();
            lx -= g.getFontMetrics().stringWidth(nombre) + 34;
            g.setColor(colorPolitica(ps[i]));
            g.fillRoundRect(lx, 12, 16, 6, 4, 4);
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(nombre, lx + 22, 20);
        }
        g.dispose();
    }
}
