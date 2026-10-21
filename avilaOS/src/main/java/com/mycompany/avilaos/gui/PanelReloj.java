package com.mycompany.avilaos.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.JComponent;

/**
 * Reloj analógico que marca el paso de los ciclos del reloj global (es una vista: solo muestra el ciclo
 * que se le indica con {@link #setCiclo(long)}).
 *
 * La aguja naranja (larga) da una vuelta cada 60 ciclos y la aguja clara (corta) una vuelta cada 720.
 */
public class PanelReloj extends JComponent {

    /** Ciclos que tarda la aguja larga en dar una vuelta. */
    public static final int CICLOS_POR_VUELTA = 60;
    /** Ciclos que tarda la aguja corta en dar una vuelta. */
    public static final int CICLOS_VUELTA_CORTA = 720;

    private volatile long ciclo;

    public PanelReloj() {
        setPreferredSize(new Dimension(92, 92));
        setToolTipText("Reloj de la simulación: la aguja naranja da una vuelta cada " + CICLOS_POR_VUELTA
                + " ciclos y la clara cada " + CICLOS_VUELTA_CORTA + " ciclos");
        setOpaque(false);
    }

    /** Actualiza el ciclo mostrado (solo vuelve a dibujar si cambió). */
    public void setCiclo(long ciclo) {
        if (ciclo != this.ciclo) {
            this.ciclo = ciclo;
            repaint();
        }
    }

    public long getCiclo() {
        return ciclo;
    }

    /** Ángulo (en grados, 0 = arriba, sentido horario) de la aguja larga para un ciclo dado. */
    static double anguloLarga(long ciclo) {
        return (ciclo % CICLOS_POR_VUELTA) * 360.0 / CICLOS_POR_VUELTA;
    }

    /** Ángulo (en grados, 0 = arriba, sentido horario) de la aguja corta para un ciclo dado. */
    static double anguloCorta(long ciclo) {
        return (ciclo % CICLOS_VUELTA_CORTA) * 360.0 / CICLOS_VUELTA_CORTA;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int lado = Math.min(getWidth(), getHeight()) - 6;
        if (lado < 30) {
            g.dispose();
            return;
        }
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        double r = lado / 2.0;

        // Esfera y aro
        g.setColor(new Color(0x0F1A29));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(0x34506F));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));

        // Marcas: una por cada 5 ciclos; más largas en 0, 15, 30 y 45
        for (int i = 0; i < 60; i += 1) {
            boolean cuarto = i % 15 == 0;
            boolean cinco = i % 5 == 0;
            if (!cinco) {
                continue;
            }
            double a = Math.toRadians(i * 6.0);
            double r1 = r - (cuarto ? 11 : 8);
            double r2 = r - 4;
            g.setStroke(new BasicStroke(cuarto ? 2.2f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(cuarto ? Tema.TEXTO : Tema.TEXTO_SUAVE);
            g.draw(new Line2D.Double(cx + Math.sin(a) * r1, cy - Math.cos(a) * r1,
                    cx + Math.sin(a) * r2, cy - Math.cos(a) * r2));
        }

        // Números 0, 15, 30 y 45 (ciclos de la vuelta)
        g.setFont(Tema.fuente(Font.PLAIN, Math.max(8f, (float) (r * 0.22))));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO_APAGADO);
        String[] nums = {"0", "15", "30", "45"};
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(k * 90.0);
            double rn = r - 21;
            String t = nums[k];
            double tx = cx + Math.sin(a) * rn - fm.stringWidth(t) / 2.0;
            double ty = cy - Math.cos(a) * rn + fm.getAscent() / 2.0 - 1;
            g.drawString(t, (float) tx, (float) ty);
        }

        // Aguja corta (clara)
        dibujarAguja(g, cx, cy, anguloCorta(ciclo), r * 0.48, 3.6f, Tema.TEXTO);
        // Aguja larga (naranja)
        dibujarAguja(g, cx, cy, anguloLarga(ciclo), r * 0.78, 2.2f, Tema.NARANJA);

        // Centro
        g.setColor(Tema.NARANJA);
        g.fill(new Ellipse2D.Double(cx - 4, cy - 4, 8, 8));
        g.setColor(new Color(0x0F1A29));
        g.fill(new Ellipse2D.Double(cx - 1.5, cy - 1.5, 3, 3));
        g.dispose();
    }

    private static void dibujarAguja(Graphics2D g, double cx, double cy, double grados, double largo,
            float grosor, Color color) {
        double a = Math.toRadians(grados);
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        g.draw(new Line2D.Double(cx - Math.sin(a) * largo * 0.14, cy + Math.cos(a) * largo * 0.14,
                cx + Math.sin(a) * largo, cy - Math.cos(a) * largo));
    }
}
