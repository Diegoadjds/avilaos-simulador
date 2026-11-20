package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.Disco;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Vista del disco: política de planificación (con selector), posición y dirección del cabezal, mapa de bloques con
 * el área de intercambio y la cola de E/S con la operación de cada solicitud. Solo observa; el cambio de política
 * se le pide al sistema, que revisa el modo de usuario.
 */
public class PanelDisco extends JPanel {

    private final SistemaAvilaOS sistema;
    private final JComboBox<PoliticaPlanificacionDisco> cmbPolitica = new JComboBox<>(PoliticaPlanificacionDisco.values());
    private final Insignia insigniaCabezal = new Insignia();
    private final JLabel lblResumen = new JLabel(" ");
    private final MapaDisco mapa;
    private final ModeloColaDisco modeloCola = new ModeloColaDisco();
    private final BordeCompacto bordeCola = new BordeCompacto("Cola de E/S del disco");
    private boolean sincronizando;

    public PanelDisco(SistemaAvilaOS sistema) {
        super(new BorderLayout(0, 8));
        this.sistema = sistema;
        this.mapa = new MapaDisco(sistema);
        setBorder(Tema.borde(""));

        JLabel titulo = new JLabel("Disco");
        titulo.setFont(Tema.fuente(Font.BOLD, 18f));
        titulo.setForeground(Tema.TEXTO);
        ComboModerno.aplicar(cmbPolitica);
        cmbPolitica.setSelectedItem(sistema.getPlanificadorDisco().getPolitica());
        cmbPolitica.addActionListener(e -> {
            PoliticaPlanificacionDisco elegida = (PoliticaPlanificacionDisco) cmbPolitica.getSelectedItem();
            if (!sincronizando && elegida != null && elegida != sistema.getPlanificadorDisco().getPolitica()) {
                try {
                    sistema.cambiarPoliticaDisco(elegida);
                } catch (ErrorOperacion ex) {
                    Entradas.error(this, ex.getMessage());
                    sincronizar();
                }
            }
        });
        JLabel lblPolitica = new JLabel("Política de disco");
        lblPolitica.setForeground(Tema.TEXTO_SUAVE);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        Tema.transparente(derecha);
        BotonModerno btnNumeros = new BotonModerno("Números: sí", BotonModerno.Tipo.NORMAL);
        btnNumeros.addActionListener(e -> {
            boolean ver = !mapa.isMostrarNumeros();
            mapa.setMostrarNumeros(ver);
            btnNumeros.setText(ver ? "Números: sí" : "Números: no");
        });
        derecha.add(btnNumeros);
        derecha.add(lblPolitica);
        derecha.add(cmbPolitica);
        JPanel fila1 = new JPanel(new BorderLayout());
        Tema.transparente(fila1);
        fila1.add(titulo, BorderLayout.WEST);
        fila1.add(derecha, BorderLayout.EAST);

        lblResumen.setForeground(Tema.TEXTO_SUAVE);
        lblResumen.setFont(Tema.fuente(Font.PLAIN, 12f));
        JPanel fila2 = new JPanel(new BorderLayout());
        Tema.transparente(fila2);
        fila2.add(insigniaCabezal, BorderLayout.WEST);
        fila2.add(lblResumen, BorderLayout.CENTER);

        JPanel norte = new JPanel(new BorderLayout(0, 6));
        Tema.transparente(norte);
        norte.add(fila1, BorderLayout.NORTH);
        norte.add(fila2, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        JPanel centroMapa = new JPanel(new BorderLayout(0, 4));
        centroMapa.setOpaque(false);
        centroMapa.setBorder(new BordeCompacto("Mapa de bloques"));
        centroMapa.add(mapa, BorderLayout.CENTER);
        centroMapa.add(leyenda(), BorderLayout.SOUTH);

        JTable tabla = new JTable(modeloCola);
        Tema.estilizarTabla(tabla);
        tabla.setRowHeight(21);
        int[] anchos = {70, 130, 80, 50, 50};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane sp = new JScrollPane(tabla);
        sp.setColumnHeaderView(tabla.getTableHeader());
        Tema.estilizarScroll(sp);
        sp.setPreferredSize(new Dimension(10, 130));
        JPanel cola = new JPanel(new BorderLayout());
        cola.setOpaque(false);
        cola.setBorder(bordeCola);
        cola.add(sp, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        Tema.transparente(centro);
        centro.add(centroMapa, BorderLayout.CENTER);
        centro.add(cola, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520, 520));
    }

    private JPanel leyenda() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        Tema.transparente(p);
        p.add(item(Tema.CAMPO, "libre"));
        p.add(item(Tema.colorBloque(3), "archivo (color propio)"));
        p.add(item(Tema.CIAN, "área de intercambio (swap)"));
        p.add(item(Tema.NARANJA, "solicitud pendiente"));
        p.add(item(java.awt.Color.WHITE, "cabezal"));
        return p;
    }

    private static JLabel item(java.awt.Color color, String texto) {
        JLabel l = new JLabel(texto, new IconoColor(color), JLabel.LEFT);
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setFont(Tema.fuente(Font.PLAIN, 11f));
        return l;
    }

    /** Cuadradito de color para la leyenda. */
    private static class IconoColor implements javax.swing.Icon {
        private final java.awt.Color color;

        IconoColor(java.awt.Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRoundRect(x, y, 10, 10, 3, 3);
            g.setColor(Tema.BORDE);
            g.drawRoundRect(x, y, 10, 10, 3, 3);
        }

        @Override
        public int getIconWidth() { return 12; }

        @Override
        public int getIconHeight() { return 12; }
    }

    public void setPuedeCambiarPolitica(boolean puede) {
        cmbPolitica.setEnabled(puede);
        cmbPolitica.setToolTipText(puede ? null : "Modo usuario: solo el administrador cambia las políticas");
    }

    private void sincronizar() {
        sincronizando = true;
        cmbPolitica.setSelectedItem(sistema.getPlanificadorDisco().getPolitica());
        sincronizando = false;
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing). */
    public void refrescar() {
        Disco d = sistema.getDisco();
        if (cmbPolitica.getSelectedItem() != sistema.getPlanificadorDisco().getPolitica()) {
            sincronizar();
        }
        insigniaCabezal.poner("Cabezal en el bloque " + d.getCabezal() + "  " + d.getSentido(), Tema.ACENTO);
        SolicitudDisco actual = d.getSolicitudActual();
        lblResumen.setText(" " + d.getVelocidad() + " pos/ciclo · libres " + d.getMapa().getLibres(false) + "/"
                + d.getMapa().getBloquesDatos() + " · swap " + d.getMapa().getLibres(true)
                + "/" + d.getMapa().getBloquesSwap()
                + (actual == null ? " · disco ocioso" : " · atendiendo " + actual));
        SolicitudDisco[] cola = sistema.getPlanificadorDisco().getCola();
        modeloCola.setDatos(actual, cola, sistema.getReloj().getCicloActual());
        bordeCola.setTitulo("Cola de E/S del disco (" + (cola.length + (actual == null ? 0 : 1)) + ")");
        mapa.actualizar();
        repaint();
    }
}
