package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.ModoCPU;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.sistema.Computador;
import com.mycompany.avilaos.so.Kernel;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Vista 2 de UN computador: CPU con el PCB completo del proceso en ejecución, modo del procesador,
 * colas, memoria y política de planificación. Solo observa: la lógica vive en el kernel.
 */
public class PanelComputador extends JPanel {

    private static final String SIN_DATO = "—";

    private final Computador computador;
    private final JComboBox<PoliticaPlanificacion> cmbPolitica = new JComboBox<>(PoliticaPlanificacion.values());
    private final BarraUso barRam = new BarraUso();
    private final Insignia insigniaModo = new Insignia();

    // Campos del PCB del proceso que está en la CPU
    private final CampoPCB cProceso = new CampoPCB("Proceso");
    private final CampoPCB cTipo = new CampoPCB("Tipo");
    private final CampoPCB cEstado = new CampoPCB("Estado");
    private final CampoPCB cComputador = new CampoPCB("Computador");
    private final CampoPCB cPrioridad = new CampoPCB("Prioridad");
    private final CampoPCB cMemoria = new CampoPCB("Memoria");
    private final CampoPCB cDeadline = new CampoPCB("Deadline");
    private final CampoPCB cRestante = new CampoPCB("Restante");
    private final CampoPCB cPc = new CampoPCB("PC");
    private final CampoPCB cMar = new CampoPCB("MAR");
    private final CampoPCB cInstrucciones = new CampoPCB("Instrucciones");
    private final CampoPCB cElementos = new CampoPCB("Elementos");

    private final ModeloTablaPCB mNuevos = new ModeloTablaPCB(ModeloTablaPCB.NORMAL);
    private final ModeloTablaPCB mListos = new ModeloTablaPCB(ModeloTablaPCB.NORMAL);
    private final ModeloTablaPCB mBloqueados = new ModeloTablaPCB(ModeloTablaPCB.BLOQUEADOS);
    private final ModeloTablaPCB mTerminados = new ModeloTablaPCB(ModeloTablaPCB.TERMINADOS);
    private final JScrollPane spNuevos;
    private final JScrollPane spListos;
    private final JScrollPane spBloqueados;
    private final JScrollPane spTerminados;

    public PanelComputador(Computador computador) {
        super(new BorderLayout(0, 8));
        this.computador = computador;
        setBorder(Tema.borde(""));

        // --- Cabecera: nombre, política y RAM ---
        JLabel titulo = new JLabel("Computador " + computador.getId());
        titulo.setFont(Tema.fuente(Font.BOLD, 18f));
        titulo.setForeground(Tema.TEXTO);
        ComboModerno.aplicar(cmbPolitica);
        cmbPolitica.setSelectedItem(computador.getKernel().getPolitica());
        cmbPolitica.addActionListener(e -> {
            PoliticaPlanificacion elegida = (PoliticaPlanificacion) cmbPolitica.getSelectedItem();
            if (elegida != null && elegida != computador.getKernel().getPolitica()) {
                computador.cambiarPolitica(elegida);
            }
        });
        JLabel lblPolitica = new JLabel("Política");
        lblPolitica.setForeground(Tema.TEXTO_SUAVE);
        JPanel politica = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        Tema.transparente(politica);
        politica.add(lblPolitica);
        politica.add(cmbPolitica);
        JPanel fila1 = new JPanel(new BorderLayout());
        Tema.transparente(fila1);
        fila1.add(titulo, BorderLayout.WEST);
        fila1.add(politica, BorderLayout.EAST);

        JLabel lblRam = new JLabel("RAM");
        lblRam.setForeground(Tema.TEXTO_SUAVE);
        lblRam.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        JPanel fila2 = new JPanel(new BorderLayout());
        Tema.transparente(fila2);
        fila2.add(lblRam, BorderLayout.WEST);
        fila2.add(barRam, BorderLayout.CENTER);

        // --- CPU: modo del procesador y PCB completo del proceso en ejecución ---
        Tarjeta cpu = new Tarjeta(new BorderLayout(0, 6), Tema.TARJETA, Tema.BORDE);
        cpu.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        JLabel lblCpu = new JLabel("CPU");
        lblCpu.setFont(Tema.fuente(Font.BOLD, 12f));
        lblCpu.setForeground(Tema.TEXTO_SUAVE);
        JPanel cabCpu = new JPanel(new BorderLayout());
        Tema.transparente(cabCpu);
        cabCpu.add(lblCpu, BorderLayout.WEST);
        cabCpu.add(insigniaModo, BorderLayout.EAST);
        JPanel pcb = new JPanel(new GridLayout(2, 6, 10, 6));
        Tema.transparente(pcb);
        pcb.add(cProceso);
        pcb.add(cTipo);
        pcb.add(cEstado);
        pcb.add(cComputador);
        pcb.add(cPrioridad);
        pcb.add(cMemoria);
        pcb.add(cDeadline);
        pcb.add(cRestante);
        pcb.add(cPc);
        pcb.add(cMar);
        pcb.add(cInstrucciones);
        pcb.add(cElementos);
        cpu.add(cabCpu, BorderLayout.NORTH);
        cpu.add(pcb, BorderLayout.CENTER);

        // --- Colas ---
        spNuevos = crearTabla("Nuevos", mNuevos);
        spListos = crearTabla("Listos", mListos);
        spBloqueados = crearTabla("Bloqueados", mBloqueados);
        spTerminados = crearTabla("Terminados", mTerminados);
        JPanel colas = new JPanel(new GridLayout(4, 1, 8, 8));
        Tema.transparente(colas);
        colas.add(spNuevos);
        colas.add(spListos);
        colas.add(spBloqueados);
        colas.add(spTerminados);

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        Tema.transparente(norte);
        norte.add(fila1);
        norte.add(Box.createVerticalStrut(6));
        norte.add(fila2);
        norte.add(Box.createVerticalStrut(10));
        norte.add(cpu);
        add(norte, BorderLayout.NORTH);
        add(colas, BorderLayout.CENTER);
        setPreferredSize(new Dimension(640, 700));
        refrescar();
    }

    private static JScrollPane crearTabla(String titulo, ModeloTablaPCB modelo) {
        JTable tabla = new JTable(modelo);
        Tema.estilizarTabla(tabla);
        tabla.setRowHeight(22);
        int[] anchos = {28, 56, 82, 30, 32, 56, 34, 170};
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane sp = new JScrollPane(tabla);
        sp.setColumnHeaderView(tabla.getTableHeader());
        Tema.estilizarScroll(sp);
        sp.setBorder(BorderFactory.createCompoundBorder(Tema.borde(titulo), BorderFactory.createLineBorder(Tema.BORDE)));
        return sp;
    }

    private static void titulo(JScrollPane sp, String texto) {
        ((BordeSeccion) ((javax.swing.border.CompoundBorder) sp.getBorder()).getOutsideBorder()).setTitulo(texto);
        sp.repaint();
    }

    /** Se llama desde el Timer de la ventana (hilo de Swing). */
    public void refrescar() {
        Kernel k = computador.getKernel();
        PCB p = k.getProcesoEnCpu();
        if (p == null) {
            cProceso.poner("CPU ociosa", Tema.TEXTO_SUAVE);
            cTipo.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cEstado.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cComputador.poner(String.valueOf(computador.getId()), Tema.TEXTO);
            cPrioridad.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cMemoria.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cDeadline.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cRestante.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cPc.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cMar.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cInstrucciones.poner(SIN_DATO, Tema.TEXTO_SUAVE);
            cElementos.poner(SIN_DATO, Tema.TEXTO_SUAVE);
        } else {
            EstadoProceso estado = p.getEstado();
            cProceso.poner(p.getEtiqueta() + " · " + p.getNombre(), Tema.TEXTO);
            cTipo.poner(p.getTipo().toString(), Tema.colorTipo(p.getTipo()));
            cEstado.poner(estado.toString(), Tema.colorEstado(estado));
            cComputador.poner(String.valueOf(p.getIdComputador()), Tema.TEXTO);
            cPrioridad.poner(String.valueOf(p.getPrioridad()), Tema.TEXTO);
            cMemoria.poner(String.valueOf(p.getMemoria()), Tema.TEXTO);
            cDeadline.poner(String.valueOf(p.getDeadlineRestante()),
                    p.getDeadlineRestante() <= 5 ? Tema.ROJO : Tema.TEXTO);
            cRestante.poner(String.valueOf(p.getTiempoRestante()), Tema.TEXTO);
            cPc.poner(String.valueOf(p.getPc()), Tema.TEXTO);
            cMar.poner(String.valueOf(p.getMar()), Tema.TEXTO);
            cInstrucciones.poner(String.valueOf(p.getInstruccionesTotales()), Tema.TEXTO);
            boolean pc = p.getTipo() == TipoProceso.PRODUCTOR || p.getTipo() == TipoProceso.CONSUMIDOR;
            cElementos.poner(pc ? p.getElementosProcesados() + " / " + p.getElementosRequeridos() : SIN_DATO,
                    pc ? Tema.TEXTO : Tema.TEXTO_SUAVE);
        }
        ModoCPU modo = k.getModoCpu();
        if (modo == ModoCPU.USUARIO) {
            insigniaModo.poner("Modo: programa de usuario", Tema.VERDE);
        } else {
            insigniaModo.poner("Modo: sistema operativo", Tema.NARANJA);
        }

        MemoriaPrincipal ram = k.getRam();
        barRam.poner(ram.getUsada(), ram.getTotal(),
                ram.getUsada() + " usada · " + ram.getLibre() + " libre (total " + ram.getTotal() + ")");

        PCB[] nuevos = k.getNuevos();
        PCB[] listos = k.getListos();
        PCB[] bloqueados = k.getBloqueados();
        PCB[] terminados = k.getTerminados();
        mNuevos.setDatos(nuevos);
        mListos.setDatos(listos);
        mBloqueados.setDatos(bloqueados);
        mTerminados.setDatos(terminados);
        titulo(spNuevos, "Nuevos (" + nuevos.length + ")");
        titulo(spListos, "Listos (" + listos.length + ")");
        titulo(spBloqueados, "Bloqueados (" + bloqueados.length + ")");
        titulo(spTerminados, "Terminados (" + terminados.length + ")");
    }

    /** Un dato del PCB: título pequeño y valor debajo. */
    private static class CampoPCB extends JPanel {

        private final JLabel valor = new JLabel(SIN_DATO);

        CampoPCB(String nombre) {
            super(new BorderLayout(0, 1));
            Tema.transparente(this);
            JLabel titulo = new JLabel(nombre);
            titulo.setFont(Tema.fuente(Font.PLAIN, 11f));
            titulo.setForeground(Tema.TEXTO_SUAVE);
            valor.setFont(Tema.fuente(Font.BOLD, 13f));
            valor.setForeground(Tema.TEXTO);
            add(titulo, BorderLayout.NORTH);
            add(valor, BorderLayout.CENTER);
        }

        void poner(String texto, Color color) {
            if (!texto.equals(valor.getText())) {
                valor.setText(texto);
            }
            valor.setForeground(color);
        }
    }
}
