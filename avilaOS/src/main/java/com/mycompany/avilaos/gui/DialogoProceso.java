package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.TipoProceso;
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Vista 1: el usuario define un proceso (nombre, instrucciones, memoria, prioridad, deadline, tipo,
 * computador y los datos propios del tipo). Todos los campos se validan en tipo y rango.
 */
public class DialogoProceso extends JDialog {

    private final ClusterAvilaOS cluster;
    private final JTextField txtNombre = new JTextField("Proceso", 14);
    private final JComboBox<TipoProceso> cmbTipo = new JComboBox<>(TipoProceso.values());
    private final JTextField txtInstrucciones = new JTextField("30", 8);
    private final JTextField txtMemoria = new JTextField("10", 8);
    private final JTextField txtPrioridad = new JTextField("1", 8);
    private final JTextField txtDeadline = new JTextField("200", 8);
    private final JComboBox<String> cmbComputador = new JComboBox<>();
    private final JTextField txtCiclosEs = new JTextField("5", 8);
    private final JTextField txtDuracionEs = new JTextField("4", 8);
    private final JComboBox<BufferAcotado> cmbBuffer = new JComboBox<>();
    private final JTextField txtCiclosElemento = new JTextField("2", 8);
    private final JTextField txtElementos = new JTextField("10", 8);
    private final JLabel lblEstado = new JLabel(" ");

    public DialogoProceso(Window padre, ClusterAvilaOS cluster) {
        super(padre, "Nuevo proceso", ModalityType.APPLICATION_MODAL);
        this.cluster = cluster;

        cmbComputador.addItem("Automático (lo decide ÁvilaOS)");
        for (int i = 1; i <= cluster.getNumComputadores(); i++) {
            cmbComputador.addItem("Computador " + i);
        }
        for (BufferAcotado b : cluster.getBuffers()) {
            cmbBuffer.addItem(b);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        int y = 0;
        fila(form, y++, "Nombre:", txtNombre);
        fila(form, y++, "Tipo:", cmbTipo);
        fila(form, y++, "Instrucciones:", txtInstrucciones);
        fila(form, y++, "Memoria requerida:", txtMemoria);
        fila(form, y++, "Prioridad (1 = la más alta):", txtPrioridad);
        fila(form, y++, "Deadline (ciclos):", txtDeadline);
        fila(form, y++, "Computador:", cmbComputador);
        fila(form, y++, "I/O bound – cada cuántas instrucciones pide E/S:", txtCiclosEs);
        fila(form, y++, "I/O bound – ciclos bloqueado por cada E/S:", txtDuracionEs);
        fila(form, y++, "Productor/consumidor – buffer:", cmbBuffer);
        fila(form, y++, "Productor/consumidor – ciclos por elemento:", txtCiclosElemento);
        fila(form, y++, "Productor/consumidor – elementos requeridos:", txtElementos);

        cmbTipo.addActionListener(e -> actualizarCampos());
        actualizarCampos();

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

    private static void fila(JPanel p, int y, String texto, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridy = y;
        g.gridx = 0;
        p.add(new JLabel(texto), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(campo, g);
    }

    /** Habilita solo los campos que aplican al tipo elegido. */
    private void actualizarCampos() {
        TipoProceso t = (TipoProceso) cmbTipo.getSelectedItem();
        boolean io = t == TipoProceso.IO_BOUND;
        boolean pc = t == TipoProceso.PRODUCTOR || t == TipoProceso.CONSUMIDOR;
        txtInstrucciones.setEnabled(!pc); // en productor/consumidor se calcula solo
        txtCiclosEs.setEnabled(io);
        txtDuracionEs.setEnabled(io);
        cmbBuffer.setEnabled(pc);
        txtCiclosElemento.setEnabled(pc);
        txtElementos.setEnabled(pc);
    }

    private void crear() {
        try {
            TipoProceso tipo = (TipoProceso) cmbTipo.getSelectedItem();
            boolean io = tipo == TipoProceso.IO_BOUND;
            boolean pc = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;

            String nombre = Entradas.texto(txtNombre, "Nombre", 30);
            int instrucciones = pc ? 1 : Entradas.entero(txtInstrucciones, "Instrucciones", 1, 1000000);
            int memoria = Entradas.entero(txtMemoria, "Memoria requerida", 1, 1000000);
            int prioridad = Entradas.entero(txtPrioridad, "Prioridad", 1, 99);
            int deadline = Entradas.entero(txtDeadline, "Deadline", 1, 10000000);
            int computador = cmbComputador.getSelectedIndex() == 0
                    ? EspecificacionProceso.AUTOMATICO : cmbComputador.getSelectedIndex();

            int ciclosEs = io ? Entradas.entero(txtCiclosEs, "Instrucciones entre E/S", 1, 100000) : 0;
            int duracionEs = io ? Entradas.entero(txtDuracionEs, "Duración de la E/S", 1, 100000) : 0;

            int idBuffer = 0;
            int ciclosElemento = 0;
            int elementos = 0;
            if (pc) {
                BufferAcotado b = (BufferAcotado) cmbBuffer.getSelectedItem();
                if (b == null) {
                    throw new IllegalArgumentException("Primero cree un buffer (botón \"Nuevo buffer…\")");
                }
                idBuffer = b.getId();
                ciclosElemento = Entradas.entero(txtCiclosElemento, "Ciclos por elemento", 1, 100000);
                elementos = Entradas.entero(txtElementos, "Elementos requeridos", 1, 100000);
            }

            EspecificacionProceso esp = new EspecificacionProceso(nombre, tipo, instrucciones, memoria,
                    prioridad, deadline, ciclosEs, duracionEs, idBuffer, ciclosElemento, elementos, computador);
            PCB pcb = cluster.crearProceso(esp);
            lblEstado.setText("Creado " + pcb.getEtiqueta() + " en Computador " + pcb.getIdComputador());
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        }
    }
}
