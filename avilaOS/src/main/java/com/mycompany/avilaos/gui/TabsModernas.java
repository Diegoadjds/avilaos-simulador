package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Pestañas planas con el tema oscuro: la pestaña activa se rellena de color y el resto quedan apagadas. */
public class TabsModernas extends BasicTabbedPaneUI {

    /** Aplica el estilo a un panel de pestañas. */
    public static void aplicar(JTabbedPane panel) {
        panel.setUI(new TabsModernas());
        panel.setFont(Tema.fuente(Font.BOLD, 13f));
        panel.setOpaque(true);
        panel.setBackground(Tema.FONDO);
        panel.setForeground(Tema.TEXTO);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(0, 0, 4, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabInsets = new Insets(6, 18, 6, 18);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintTabBackground(Graphics g0, int tabPlacement, int i, int x, int y, int w, int h,
            boolean seleccionada) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(seleccionada ? Tema.ACENTO : Tema.PANEL);
        g.fill(new RoundRectangle2D.Float(x + 2, y + 2, w - 4, h - 4, 12, 12));
        g.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int i, int x, int y, int w, int h,
            boolean seleccionada) {
        // sin borde
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int i,
            Rectangle iconRect, Rectangle textRect, boolean seleccionada) {
        // sin indicador de foco
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int i,
            String titulo, Rectangle textRect, boolean seleccionada) {
        g.setFont(font);
        g.setColor(seleccionada ? Color.WHITE : Tema.TEXTO_SUAVE);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString(titulo, textRect.x, textRect.y + metrics.getAscent());
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // sin borde alrededor del contenido
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }
}
