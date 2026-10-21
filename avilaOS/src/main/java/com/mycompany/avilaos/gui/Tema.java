package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.TipoProceso;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;

/**
 * Aspecto visual de ÁvilaOS: tema oscuro hecho solo con Swing estándar (Nimbus con una paleta propia
 * más unos pocos componentes dibujados con Java2D). Aquí viven los colores, las tipografías y los
 * ajustes comunes; las pantallas solo los usan. No contiene lógica del simulador.
 */
public final class Tema {

    // ---- Paleta ----
    public static final Color FONDO = new Color(0x0D1520);
    public static final Color PANEL = new Color(0x141F2E);
    public static final Color TARJETA = new Color(0x1B293B);
    public static final Color TARJETA_ALT = new Color(0x213249);
    public static final Color CAMPO = new Color(0x0F1825);
    public static final Color BORDE = new Color(0x2E4360);
    public static final Color TEXTO = new Color(0xE6EDF6);
    public static final Color TEXTO_SUAVE = new Color(0x93A6BD);
    public static final Color TEXTO_APAGADO = new Color(0x5B6D84);

    public static final Color ACENTO = new Color(0x3E8EF7);
    public static final Color VERDE = new Color(0x3DBE8B);
    public static final Color NARANJA = new Color(0xF2A154);
    public static final Color ROJO = new Color(0xEE6B60);
    public static final Color AZUL = new Color(0x5AB0F5);
    public static final Color MORADO = new Color(0xAE92F2);
    public static final Color CIAN = new Color(0x4FD1D9);
    public static final Color GRIS = new Color(0x8A9AAE);

    /** Colores distintos y legibles sobre fondo oscuro (uno por computador en la gráfica). */
    public static final Color[] SERIES = {AZUL, NARANJA, VERDE, MORADO, ROJO, CIAN};

    private static final String FAMILIA = elegirFamilia();

    private Tema() {
    }

    // ---- Tipografías ----

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, estilo, Math.round(tamano)).deriveFont(estilo, tamano);
    }

    private static String elegirFamilia() {
        String[] preferidas = {"Segoe UI", "Inter", "Helvetica Neue", "Noto Sans", "DejaVu Sans"};
        String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String p : preferidas) {
            for (String d : disponibles) {
                if (d.equalsIgnoreCase(p)) {
                    return d;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    // ---- Instalación ----

    /**
     * Activa Nimbus con la paleta oscura. Debe llamarse antes de crear cualquier ventana.
     * Si Nimbus no está disponible, el programa sigue con el aspecto por defecto.
     */
    public static void instalar() {
        try {
            UIManager.put("control", new ColorUIResource(PANEL));
            UIManager.put("info", new ColorUIResource(TARJETA_ALT));
            UIManager.put("nimbusBase", new ColorUIResource(new Color(0x1F3048)));
            UIManager.put("nimbusBlueGrey", new ColorUIResource(new Color(0x2A3E58)));
            UIManager.put("nimbusLightBackground", new ColorUIResource(CAMPO));
            UIManager.put("nimbusBorder", new ColorUIResource(BORDE));
            UIManager.put("nimbusFocus", new ColorUIResource(ACENTO));
            UIManager.put("nimbusSelectionBackground", new ColorUIResource(new Color(0x2D63B8)));
            UIManager.put("nimbusSelectedText", new ColorUIResource(Color.WHITE));
            UIManager.put("nimbusDisabledText", new ColorUIResource(TEXTO_APAGADO));
            UIManager.put("nimbusOrange", new ColorUIResource(NARANJA));
            UIManager.put("nimbusRed", new ColorUIResource(ROJO));
            UIManager.put("nimbusGreen", new ColorUIResource(VERDE));
            UIManager.put("nimbusInfoBlue", new ColorUIResource(ACENTO));
            UIManager.put("nimbusAlertYellow", new ColorUIResource(NARANJA));
            UIManager.put("text", new ColorUIResource(TEXTO));
            UIManager.put("defaultFont", new FontUIResource(fuente(Font.PLAIN, 13f)));

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }

            // Ajustes que Nimbus lee al crear cada componente (se aplican después de instalarlo).
            UIManager.put("Table.background", new ColorUIResource(CAMPO));
            UIManager.put("Table.alternateRowColor", new ColorUIResource(new Color(0x142034)));
            UIManager.put("Table.foreground", new ColorUIResource(TEXTO));
            UIManager.put("Table.gridColor", new ColorUIResource(new Color(0x1E2D44)));
            UIManager.put("Table.showGrid", Boolean.FALSE);
            UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
            UIManager.put("TextArea.background", new ColorUIResource(CAMPO));
            UIManager.put("TextArea.foreground", new ColorUIResource(TEXTO));
            UIManager.put("ScrollPane.background", new ColorUIResource(PANEL));
            UIManager.put("SplitPane.dividerSize", 8);
            UIManager.put("SplitPane.background", new ColorUIResource(PANEL));
            UIManager.put("OptionPane.background", new ColorUIResource(PANEL));
            UIManager.put("Panel.background", new ColorUIResource(PANEL));
            UIManager.put("ToolTip.background", new ColorUIResource(TARJETA_ALT));
            UIManager.put("ToolTip.foreground", new ColorUIResource(TEXTO));
        } catch (Exception e) {
            // si falla, se usa el aspecto por defecto
        }
    }

    // ---- Ayudas para armar pantallas ----

    /** Borde de sección: contorno redondeado y título arriba a la izquierda. */
    public static BordeSeccion borde(String titulo) {
        return new BordeSeccion(titulo);
    }

    /** Relleno uniforme alrededor de un contenedor. */
    public static Border relleno(int arriba, int izquierda, int abajo, int derecha) {
        return BorderFactory.createEmptyBorder(arriba, izquierda, abajo, derecha);
    }

    /** Hace transparentes los componentes dados, para que se vea el fondo de la tarjeta que los contiene. */
    public static void transparente(JComponent... componentes) {
        for (JComponent c : componentes) {
            c.setOpaque(false);
        }
    }

    /** Aplica el estilo de tabla (filas, encabezado, colores por contenido) a una tabla. */
    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(24);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new java.awt.Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(CAMPO);
        tabla.setForeground(TEXTO);
        tabla.setSelectionBackground(new Color(0x2D63B8));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setDefaultRenderer(Object.class, new RenderizadorTabla());
        tabla.setDefaultRenderer(Integer.class, new RenderizadorTabla());
        tabla.setDefaultRenderer(Long.class, new RenderizadorTabla());
        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setReorderingAllowed(false);
        cabecera.setDefaultRenderer(new RenderizadorTabla.Cabecera());
        cabecera.setPreferredSize(new java.awt.Dimension(10, 26));
    }

    /** Aplica a un JScrollPane el aspecto plano de las tablas y listas. */
    public static void estilizarScroll(JScrollPane panel) {
        panel.setBorder(BorderFactory.createLineBorder(BORDE));
        panel.getViewport().setBackground(CAMPO);
        panel.setBackground(PANEL);
        panel.getVerticalScrollBar().setUnitIncrement(16);
    }

    /** Color de un componente apagado (deshabilitado) respecto al activo. */
    public static Color apagar(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 110);
    }

    /** Color asociado a cada tipo de proceso. */
    public static Color colorTipo(TipoProceso tipo) {
        switch (tipo) {
            case CPU_BOUND:
                return AZUL;
            case IO_BOUND:
                return MORADO;
            case PRODUCTOR:
                return VERDE;
            default:
                return CIAN;
        }
    }

    /** Color asociado a cada estado de proceso. */
    public static Color colorEstado(EstadoProceso estado) {
        switch (estado) {
            case EJECUCION:
                return VERDE;
            case LISTO:
                return AZUL;
            case BLOQUEADO:
                return NARANJA;
            case TERMINADO:
                return GRIS;
            default:
                return TEXTO;
        }
    }

    /** Mezcla dos colores (t = 0 devuelve a, t = 1 devuelve b). */
    public static Color mezclar(Color a, Color b, float t) {
        float u = 1f - t;
        return new Color(Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }
}
