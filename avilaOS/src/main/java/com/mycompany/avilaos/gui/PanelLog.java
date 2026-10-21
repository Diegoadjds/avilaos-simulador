package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.so.ObservadorEventos;
import com.mycompany.avilaos.so.RegistroEventos;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/** Log global de eventos del sistema, en tiempo real. */
public class PanelLog extends JPanel implements ObservadorEventos {

    private static final int MAX_LINEAS = 1500;
    private static final int LINEAS_A_BORRAR = 300;

    private final JTextArea area = new JTextArea();
    private final JCheckBox chkAutoScroll = new JCheckBox("Auto-scroll", true);

    public PanelLog(RegistroEventos registro) {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(Tema.relleno(0, 6, 0, 0), Tema.borde("Log de eventos")));
        setOpaque(false);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(Tema.CAMPO);
        area.setForeground(Tema.TEXTO);
        area.setCaretColor(Tema.CAMPO);
        area.setBorder(Tema.relleno(4, 6, 4, 6));
        for (String linea : registro.getLineas()) {
            area.append(linea + "\n");
        }
        BotonModerno btnLimpiar = new BotonModerno("Limpiar");
        btnLimpiar.addActionListener(e -> area.setText(""));
        JPanel sur = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        Tema.transparente(sur, chkAutoScroll);
        chkAutoScroll.setForeground(Tema.TEXTO_SUAVE);
        sur.add(chkAutoScroll);
        sur.add(btnLimpiar);
        JScrollPane sp = new JScrollPane(area);
        Tema.estilizarScroll(sp);
        sp.setBorder(BorderFactory.createEmptyBorder(14, 8, 0, 8));
        add(sp, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        registro.agregarObservador(this);
    }

    /** Llega desde hilos de la simulación: se pasa al hilo de Swing. */
    @Override
    public void alRegistrarEvento(String linea) {
        SwingUtilities.invokeLater(() -> {
            area.append(linea + "\n");
            if (area.getLineCount() > MAX_LINEAS) {
                try {
                    area.replaceRange("", 0, area.getLineEndOffset(LINEAS_A_BORRAR));
                } catch (BadLocationException e) {
                    area.setText("");
                }
            }
            if (chkAutoScroll.isSelected()) {
                area.setCaretPosition(area.getDocument().getLength());
            }
        });
    }
}
