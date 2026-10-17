package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.sincronizacion.BufferAcotado;
import com.mycompany.avilaos.sistema.ClusterAvilaOS;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Vista 1 (parte buffers): el usuario crea buffers de capacidad limitada en un computador anfitrión. */
public class DialogoBuffer extends JDialog {

    private final ClusterAvilaOS cluster;
    private final JTextField txtCapacidad = new JTextField("5", 8);
    private final JComboBox<String> cmbComputador = new JComboBox<>();
    private final JLabel lblEstado = new JLabel(" ");

    public DialogoBuffer(Window padre, ClusterAvilaOS cluster) {
        super(padre, "Nuevo buffer", ModalityType.APPLICATION_MODAL);
        this.cluster = cluster;
        for (int i = 1; i <= cluster.getNumComputadores(); i++) {
            cmbComputador.addItem("Computador " + i);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = 0;
        form.add(new JLabel("Capacidad (elementos):"), g);
        g.gridx = 1;
        form.add(txtCapacidad, g);
        g.gridx = 0;
        g.gridy = 1;
        form.add(new JLabel("Computador anfitrión:"), g);
        g.gridx = 1;
        form.add(cmbComputador, g);
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        form.add(new JLabel("<html>El buffer ocupa tanta RAM como su capacidad<br>en el computador anfitrión.</html>"), g);

        JButton btnCrear = new JButton("Crear");
        btnCrear.addActionListener(e -> crear());
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(lblEstado);
        botones.add(btnCrear);
        botones.add(btnCerrar);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnCrear);
        pack();
        setLocationRelativeTo(padre);
    }

    private void crear() {
        try {
            int capacidad = Entradas.entero(txtCapacidad, "Capacidad", 1, 100000);
            int computador = cmbComputador.getSelectedIndex() + 1;
            BufferAcotado b = cluster.crearBuffer(capacidad, computador);
            lblEstado.setText("Creado " + b.getNombre());
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        }
    }
}
