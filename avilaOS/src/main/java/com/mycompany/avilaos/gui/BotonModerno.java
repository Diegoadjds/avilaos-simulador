package com.mycompany.avilaos.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * Botón plano con esquinas redondeadas y cambio de color al pasar el mouse o al presionarlo.
 * Se comporta como un JButton normal (mismos eventos); solo cambia cómo se dibuja.
 */
public class BotonModerno extends JButton {

    /** Importancia visual del botón. */
    public enum Tipo { PRIMARIO, NORMAL, PELIGRO }

    private static final int RADIO = 10;

    private final Tipo tipo;
    private boolean encima;

    public BotonModerno(String texto) {
        this(texto, Tipo.NORMAL, null);
    }

    public BotonModerno(String texto, Tipo tipo) {
        this(texto, tipo, null);
    }

    public BotonModerno(String texto, Tipo tipo, Icon icono) {
        super(texto, icono);
        this.tipo = tipo;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Tema.fuente(Font.BOLD, 12.5f));
        setForeground(tipo == Tipo.PRIMARIO ? Color.WHITE : Tema.TEXTO);
        setIconTextGap(8);
        setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 32));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fondo = colorFondo();
        Color borde = colorBorde();
        RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, RADIO, RADIO);
        g.setColor(fondo);
        g.fill(forma);
        g.setColor(borde);
        g.draw(forma);
        g.dispose();
        setForeground(colorTexto());
        super.paintComponent(g0);
    }

    private Color colorTexto() {
        if (!isEnabled()) {
            return Tema.TEXTO_APAGADO;
        }
        return tipo == Tipo.PRIMARIO ? Color.WHITE : (tipo == Tipo.PELIGRO ? Tema.ROJO : Tema.TEXTO);
    }

    private Color colorFondo() {
        boolean presionado = getModel().isArmed() && getModel().isPressed();
        if (!isEnabled()) {
            return Tema.mezclar(Tema.PANEL, Tema.TARJETA, 0.4f);
        }
        switch (tipo) {
            case PRIMARIO:
                return presionado ? new Color(0x2A6FD0) : encima ? new Color(0x5AA2FA) : Tema.ACENTO;
            case PELIGRO:
                return presionado ? new Color(0x3A2630) : encima ? new Color(0x35222B) : new Color(0x2A1D26);
            default:
                return presionado ? new Color(0x182740) : encima ? new Color(0x2A405E) : Tema.TARJETA_ALT;
        }
    }

    private Color colorBorde() {
        if (!isEnabled()) {
            return Tema.mezclar(Tema.PANEL, Tema.BORDE, 0.5f);
        }
        switch (tipo) {
            case PRIMARIO:
                return encima ? new Color(0x8CBFFB) : new Color(0x5AA2FA);
            case PELIGRO:
                return new Color(0x6B3A3F);
            default:
                return encima ? new Color(0x4A6890) : Tema.BORDE;
        }
    }
}
