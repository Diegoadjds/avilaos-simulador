package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
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
 * Crea un proceso CPU bound o I/O bound (los del Proyecto 1). Todos los campos se validan en tipo y rango.
 * Solo el administrador gestiona procesos: en modo usuario el sistema lo rechaza con la explicación.
 */
public class DialogoProceso extends JDialog {

    private final SistemaAvilaOS sistema;
    private final JTextField txtNombre = new JTextField("Proceso", 14);
    private final JComboBox<TipoProceso> cmbTipo = new JComboBox<>(
            new TipoProceso[]{TipoProceso.CPU_BOUND, TipoProceso.IO_BOUND});
    private final JTextField txtInstrucciones = new JTextField("30", 8);
    private final JTextField txtMemoria = new JTextField("16", 8);
    private final JTextField txtPrioridad = new JTextField("5", 8);
    private final JTextField txtDeadline = new JTextField("500", 8);
    private final JTextField txtCiclosEs = new JTextField("5", 8);
    private final JTextField txtDuracionEs = new JTextField("4", 8);
    private final JLabel lblEstado = new JLabel(" ");

    public DialogoProceso(Window padre, SistemaAvilaOS sistema) {
        super(padre, "Nuevo proceso (CPU / I/O bound)", ModalityType.APPLICATION_MODAL);
        this.sistema = sistema;
        ComboModerno.aplicar(cmbTipo);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        int y = 0;
        fila(form, y++, "Nombre:", txtNombre);
        fila(form, y++, "Tipo:", cmbTipo);
        fila(form, y++, "Instrucciones:", txtInstrucciones);
        fila(form, y++, "Memoria requerida:", txtMemoria);
        fila(form, y++, "Prioridad (1 = la más alta):", txtPrioridad);
        fila(form, y++, "Deadline (ciclos):", txtDeadline);
        fila(form, y++, "I/O bound – cada cuántas instrucciones pide E/S:", txtCiclosEs);
        fila(form, y++, "I/O bound – ciclos bloqueado por cada E/S:", txtDuracionEs);
        cmbTipo.addActionListener(e -> actualizarCampos());
        actualizarCampos();

        JButton btnCrear = new BotonModerno("Crear", BotonModerno.Tipo.PRIMARIO, Iconos.de(Iconos.Forma.MAS));
        btnCrear.addActionListener(e -> crear());
        JButton btnCerrar = new BotonModerno("Cerrar");
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

    static void fila(JPanel p, int y, String texto, JComponent campo) {
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
        boolean io = cmbTipo.getSelectedItem() == TipoProceso.IO_BOUND;
        txtCiclosEs.setEnabled(io);
        txtDuracionEs.setEnabled(io);
    }

    private void crear() {
        try {
            TipoProceso tipo = (TipoProceso) cmbTipo.getSelectedItem();
            boolean io = tipo == TipoProceso.IO_BOUND;
            String nombre = Entradas.texto(txtNombre, "Nombre", EspecificacionProceso.MAX_NOMBRE);
            int instrucciones = Entradas.entero(txtInstrucciones, "Instrucciones", 1, EspecificacionProceso.MAX_INSTRUCCIONES);
            int memoria = Entradas.entero(txtMemoria, "Memoria requerida", 1, EspecificacionProceso.MAX_MEMORIA);
            int prioridad = Entradas.entero(txtPrioridad, "Prioridad", 1, EspecificacionProceso.MAX_PRIORIDAD);
            int deadline = Entradas.entero(txtDeadline, "Deadline", 1, EspecificacionProceso.MAX_DEADLINE);
            EspecificacionProceso esp;
            if (io) {
                int cada = Entradas.entero(txtCiclosEs, "Instrucciones entre E/S", 1, 100000);
                int duracion = Entradas.entero(txtDuracionEs, "Duración de la E/S", 1, 100000);
                esp = EspecificacionProceso.ioBound(nombre, instrucciones, memoria, prioridad, deadline, cada, duracion);
            } else {
                esp = EspecificacionProceso.cpuBound(nombre, instrucciones, memoria, prioridad, deadline);
            }
            PCB pcb = sistema.crearProcesoGeneral(esp);
            lblEstado.setText("Creado " + pcb.getEtiqueta());
        } catch (IllegalArgumentException | ErrorOperacion ex) {
            Entradas.error(this, ex.getMessage());
        }
    }
}
