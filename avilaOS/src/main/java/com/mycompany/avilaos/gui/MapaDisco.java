package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.Disco;
import com.mycompany.avilaos.hardware.MapaBloques;
import com.mycompany.avilaos.modelo.PropietarioBloques;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/**
 * Dibuja el disco como una cuadrícula de bloques numerados: libres (oscuros) y ocupados (con el color del archivo
 * o del proceso suspendido que los ocupa), el área de intercambio al final (marcada con un borde punteado), la
 * posición del cabezal (recuadro blanco con flecha de dirección) y los bloques que las solicitudes pendientes
 * quieren visitar (punto naranja). Solo observa: toma "fotos" del disco en cada refresco.
 */
public class MapaDisco extends JComponent {

    private final SistemaAvilaOS sistema;
    private PropietarioBloques[] duenos = new PropietarioBloques[0];
    private int[] siguientes = new int[0];
    private boolean[] pendiente = new boolean[0];
    private int bloquesDatos;
    private int cabezal;
    private SentidoCabezal sentido = SentidoCabezal.ARRIBA;
    private int objetivoActual = -1;

    // Geometría calculada al dibujar (también la usa el tooltip)
    private int columnas = 1;
    private int celdaAncho = 12;
    private int celdaAlto = 12;
    private boolean mostrarNumeros = true;
    private int origenX;
    private int origenY;

    public MapaDisco(SistemaAvilaOS sistema) {
        this.sistema = sistema;
        setOpaque(false);
        setPreferredSize(new Dimension(420, 260));
        setToolTipText("");
    }

    /** Muestra u oculta el número dentro de cada cuadrito (el tooltip siempre lo da). */
    public void setMostrarNumeros(boolean mostrar) {
        this.mostrarNumeros = mostrar;
        repaint();
    }

    public boolean isMostrarNumeros() { return mostrarNumeros; }

    /** Toma una foto del disco (hilo de Swing). */
    public void actualizar() {
        Disco d = sistema.getDisco();
        MapaBloques mapa = d.getMapa();
        duenos = mapa.fotoDuenos();
        siguientes = mapa.fotoSiguientes();
        bloquesDatos = mapa.getBloquesDatos();
        cabezal = d.getCabezal();
        sentido = d.getSentido();
        boolean[] marcas = new boolean[duenos.length];
        for (SolicitudDisco s : sistema.getPlanificadorDisco().getCola()) {
            if (s.getBloqueObjetivo() >= 0 && s.getBloqueObjetivo() < marcas.length) {
                marcas[s.getBloqueObjetivo()] = true;
            }
        }
        SolicitudDisco actual = d.getSolicitudActual();
        objetivoActual = actual == null ? -1 : actual.getBloqueObjetivo();
        pendiente = marcas;
        repaint();
    }

    /**
     * Elige el tamaño de las celdas. Con números, las celdas son rectángulos más anchos que altos (así caben tres
     * dígitos con letra de 11 px): se busca el ancho mayor, entre 40 y 24 px, con el que toda la cuadrícula cabe en
     * el alto disponible. Sin números (o si no hay espacio) son cuadrados del mayor tamaño que quepa.
     */
    private void calcularGeometria(int ancho, int alto) {
        int n = Math.max(1, duenos.length);
        boolean encontrado = false;
        if (mostrarNumeros) {
            for (int cw = 40; cw >= 24 && !encontrado; cw--) {
                int cols = Math.max(1, ancho / cw);
                int filas = (n + cols - 1) / cols;
                int ch = Math.min(alto / filas, 22);
                if (ch >= 15) {
                    celdaAncho = cw;
                    celdaAlto = ch;
                    encontrado = true;
                }
            }
        }
        if (!encontrado) {
            int mejor = 6;
            for (int c = 26; c >= 6; c--) {
                int cols = Math.max(1, ancho / c);
                int filas = (n + cols - 1) / cols;
                if (filas * c <= alto) {
                    mejor = c;
                    break;
                }
            }
            celdaAncho = mejor;
            celdaAlto = mejor;
        }
        columnas = Math.max(1, ancho / celdaAncho);
        int filas = (n + columnas - 1) / columnas;
        origenX = (ancho - columnas * celdaAncho) / 2;
        origenY = Math.max(0, (alto - filas * celdaAlto) / 2);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int n = duenos.length;
        calcularGeometria(getWidth(), getHeight());
        float tamano = Math.min(11.5f, celdaAlto - 4f);
        g.setFont(Tema.fuente(Font.BOLD, tamano));
        FontMetrics fm = g.getFontMetrics();
        boolean conNumeros = mostrarNumeros && celdaAlto >= 14
                && fm.stringWidth(String.valueOf(Math.max(0, n - 1))) + 4 <= celdaAncho;
        for (int i = 0; i < n; i++) {
            int x = origenX + (i % columnas) * celdaAncho;
            int y = origenY + (i / columnas) * celdaAlto;
            PropietarioBloques dueno = duenos[i];
            boolean swap = i >= bloquesDatos;
            Color relleno = dueno == null ? (swap ? new Color(0x182A44) : Tema.CAMPO) : Tema.colorBloque(dueno.getColorId());
            g.setColor(relleno);
            g.fillRoundRect(x + 1, y + 1, celdaAncho - 2, celdaAlto - 2, 4, 4);
            if (swap) {
                g.setColor(dueno == null ? new Color(0x34517E) : Tema.CIAN);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 2f}, 0f));
                g.drawRoundRect(x + 1, y + 1, celdaAncho - 3, celdaAlto - 3, 4, 4);
            }
            if (conNumeros) {
                String t = String.valueOf(i);
                g.setColor(dueno == null ? Tema.TEXTO_SUAVE : new Color(0x101820));
                g.drawString(t, x + (celdaAncho - fm.stringWidth(t)) / 2, y + (celdaAlto + fm.getAscent() - fm.getDescent()) / 2);
            }
            if (pendiente.length > i && pendiente[i]) {
                g.setColor(Tema.NARANJA);
                g.fillOval(x + celdaAncho - 7, y + 2, 5, 5);
            }
            if (i == objetivoActual) {
                g.setColor(Tema.NARANJA);
                g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(x + 1, y + 1, celdaAncho - 3, celdaAlto - 3, 4, 4);
            }
        }
        // Cabezal: recuadro blanco y flecha en el sentido de movimiento
        if (cabezal >= 0 && cabezal < n) {
            int x = origenX + (cabezal % columnas) * celdaAncho;
            int y = origenY + (cabezal / columnas) * celdaAlto;
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f));
            g.drawRoundRect(x, y, celdaAncho - 1, celdaAlto - 1, 5, 5);
            int cx = x + celdaAncho / 2;
            int cy = y + celdaAlto / 2;
            int d = Math.max(3, Math.min(celdaAncho, celdaAlto) / 4);
            int dy = sentido == SentidoCabezal.ARRIBA ? -1 : 1;
            g.fillPolygon(new int[]{cx, cx - d, cx + d}, new int[]{cy + dy * d, cy - dy * d / 2, cy - dy * d / 2}, 3);
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int col = (e.getX() - origenX) / celdaAncho;
        int fila = (e.getY() - origenY) / celdaAlto;
        int i = fila * columnas + col;
        if (e.getX() < origenX || e.getY() < origenY || col >= columnas || i < 0 || i >= duenos.length) {
            return null;
        }
        StringBuilder t = new StringBuilder("<html><b>Bloque ").append(i).append("</b>");
        t.append(i >= bloquesDatos ? " (área de intercambio)" : " (área de datos)").append("<br>");
        PropietarioBloques d = duenos[i];
        if (d == null) {
            t.append("Libre");
        } else {
            t.append("Ocupado por ").append(d.getEtiquetaBloques()).append("<br>Siguiente bloque de la cadena: ")
                    .append(siguientes[i] < 0 ? "ninguno (último)" : String.valueOf(siguientes[i]));
        }
        if (i == cabezal) {
            t.append("<br>El cabezal está aquí (").append(sentido).append(")");
        }
        return t.append("</html>").toString();
    }
}
