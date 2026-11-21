package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.TipoOperacionES;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Crea un proceso de E/S de archivos: el usuario indica la operación (crear, leer, actualizar, eliminar) y la ruta del
 * archivo o directorio. La interfaz NO toca el sistema de archivos: solo crea el proceso, que después pasa por las
 * colas del computador y pide la operación al disco. Si el modo de usuario no permite la operación, el sistema la
 * rechaza y aquí se explica el motivo; la simulación no se interrumpe.
 */
public class DialogoProcesoArchivo extends javax.swing.JDialog {

    private final SistemaAvilaOS sistema;
    private final JTextField txtNombre = new JTextField(14);
    private final JComboBox<TipoOperacionES> cmbOperacion = new JComboBox<>(new TipoOperacionES[]{
        TipoOperacionES.CREAR, TipoOperacionES.LEER, TipoOperacionES.ACTUALIZAR, TipoOperacionES.ELIMINAR});
    private final JTextField txtRuta = new JTextField(24);
    private final JTextField txtTamano = new JTextField("1", 8);
    private final JTextField txtNuevoNombre = new JTextField(14);
    private final JTextField txtDueno = new JTextField(14);
    private final JComboBox<Permiso> cmbPermiso = new JComboBox<>(Permiso.values());
    private final JTextField txtMemoria = new JTextField("16", 8);
    private final JTextField txtPrioridad = new JTextField("5", 8);
    private final JTextField txtDeadline = new JTextField("500", 8);
    private final JTextField txtInstrucciones = new JTextField("3", 8);
    private final JLabel lblEstado = new JLabel(" ");
    private static int contador = 1;

    public DialogoProcesoArchivo(Window padre, SistemaAvilaOS sistema, TipoOperacionES operacion, String rutaSugerida) {
        super(padre, "Nueva operación de E/S sobre archivos", ModalityType.APPLICATION_MODAL);
        this.sistema = sistema;
        ComboModerno.aplicar(cmbOperacion);
        ComboModerno.aplicar(cmbPermiso);
        cmbOperacion.setSelectedItem(operacion);
        txtRuta.setText(rutaSugerida == null ? "/" : rutaSugerida);
        cmbPermiso.setSelectedItem(sistema.getSesion().esAdministrador() ? Permiso.PUBLICO : Permiso.PRIVADO);
        txtNombre.setText("ES-" + contador);

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        int y = 0;
        DialogoProceso.fila(form, y++, "Usuario:", new JLabel(sistema.getSesion().getUsuario() + " ("
                + sistema.getSesion().getModo() + ")"));
        DialogoProceso.fila(form, y++, "Nombre del proceso:", txtNombre);
        DialogoProceso.fila(form, y++, "Operación:", cmbOperacion);
        DialogoProceso.fila(form, y++, "Ruta (por ejemplo /sistema/boot.bin):", txtRuta);
        DialogoProceso.fila(form, y++, "Crear – tamaño en bloques (0 = directorio):", txtTamano);
        DialogoProceso.fila(form, y++, "Crear – permiso:", cmbPermiso);
        DialogoProceso.fila(form, y++, "Crear – dueño del elemento (solo admin; vacío = yo):", txtDueno);
        DialogoProceso.fila(form, y++, "Actualizar – nombre nuevo (vacío = solo reescribir):", txtNuevoNombre);
        DialogoProceso.fila(form, y++, "Memoria requerida:", txtMemoria);
        DialogoProceso.fila(form, y++, "Prioridad (1 = la más alta):", txtPrioridad);
        DialogoProceso.fila(form, y++, "Deadline (ciclos):", txtDeadline);
        DialogoProceso.fila(form, y++, "Instrucciones (mínimo 2):", txtInstrucciones);
        cmbOperacion.addActionListener(e -> actualizarCampos());
        actualizarCampos();

        JButton btnCrear = new BotonModerno("Crear proceso", BotonModerno.Tipo.PRIMARIO, Iconos.de(Iconos.Forma.MAS));
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

    /** Deja el diálogo listo para crear un directorio (tamaño 0). */
    public void prepararDirectorio() {
        txtTamano.setText("0");
        txtNombre.setText("ES-dir-" + contador);
    }

    /** Habilita solo los campos que aplican a la operación elegida. */
    private void actualizarCampos() {
        TipoOperacionES op = (TipoOperacionES) cmbOperacion.getSelectedItem();
        txtTamano.setEnabled(op == TipoOperacionES.CREAR);
        cmbPermiso.setEnabled(op == TipoOperacionES.CREAR);
        txtDueno.setEnabled(op == TipoOperacionES.CREAR && sistema.getSesion().esAdministrador());
        txtNuevoNombre.setEnabled(op == TipoOperacionES.ACTUALIZAR);
    }

    private void crear() {
        try {
            TipoOperacionES op = (TipoOperacionES) cmbOperacion.getSelectedItem();
            String nombre = Entradas.texto(txtNombre, "Nombre del proceso", EspecificacionProceso.MAX_NOMBRE);
            String ruta = Entradas.texto(txtRuta, "Ruta", 200);
            int tamano = op == TipoOperacionES.CREAR
                    ? Entradas.entero(txtTamano, "Tamaño en bloques", 0, EspecificacionProceso.MAX_BLOQUES_ARCHIVO) : 0;
            String nuevoNombre = op == TipoOperacionES.ACTUALIZAR && !txtNuevoNombre.getText().isBlank()
                    ? txtNuevoNombre.getText().trim() : null;
            String dueno = op == TipoOperacionES.CREAR && sistema.getSesion().esAdministrador()
                    && !txtDueno.getText().isBlank() ? txtDueno.getText().trim() : null;
            int memoria = Entradas.entero(txtMemoria, "Memoria requerida", 1, EspecificacionProceso.MAX_MEMORIA);
            int prioridad = Entradas.entero(txtPrioridad, "Prioridad", 1, EspecificacionProceso.MAX_PRIORIDAD);
            int deadline = Entradas.entero(txtDeadline, "Deadline", 1, EspecificacionProceso.MAX_DEADLINE);
            int instrucciones = Entradas.entero(txtInstrucciones, "Instrucciones", 2, EspecificacionProceso.MAX_INSTRUCCIONES);
            PCB pcb = sistema.crearProcesoArchivo(nombre, op, ruta, tamano, nuevoNombre,
                    (Permiso) cmbPermiso.getSelectedItem(), dueno, memoria, prioridad, deadline, instrucciones);
            contador++;
            txtNombre.setText("ES-" + contador);
            lblEstado.setText("Creado " + pcb.getEtiqueta() + " (" + op + " " + ruta + ")");
        } catch (IllegalArgumentException ex) {
            Entradas.error(this, ex.getMessage());
        } catch (ErrorOperacion ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación rechazada",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }
}
