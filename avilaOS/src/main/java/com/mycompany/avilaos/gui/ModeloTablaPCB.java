package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.PCB;
import javax.swing.table.AbstractTableModel;

/** Tabla con los elementos del PCB de los procesos de una cola. */
public class ModeloTablaPCB extends AbstractTableModel {

    public static final int NORMAL = 0;
    public static final int BLOQUEADOS = 1;
    public static final int TERMINADOS = 2;

    private static final String[] BASE = {"ID", "Nombre", "Tipo", "Prio", "Mem", "Deadline", "Rest."};

    private final int modo;
    private final String[] columnas;
    private PCB[] datos = new PCB[0];

    public ModeloTablaPCB(int modo) {
        this.modo = modo;
        if (modo == NORMAL) {
            columnas = BASE;
        } else {
            columnas = new String[BASE.length + 1];
            System.arraycopy(BASE, 0, columnas, 0, BASE.length);
            columnas[BASE.length] = modo == BLOQUEADOS ? "Motivo" : "Resultado";
        }
    }

    public void setDatos(PCB[] nuevos) {
        this.datos = nuevos;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() { return datos.length; }

    @Override
    public int getColumnCount() { return columnas.length; }

    @Override
    public String getColumnName(int c) { return columnas[c]; }

    @Override
    public Object getValueAt(int fila, int col) {
        if (fila >= datos.length) {
            return "";
        }
        PCB p = datos[fila];
        switch (col) {
            case 0: return p.getEtiqueta();
            case 1: return p.getNombre();
            case 2: return p.getTipo();
            case 3: return p.getPrioridad();
            case 4: return p.getMemoria();
            case 5: return p.getDeadlineRestante();
            case 6: return p.getTiempoRestante();
            default:
                return modo == BLOQUEADOS
                        ? p.getMotivoBloqueo().toString()
                        : (p.isTerminoPorDeadline() ? "Deadline vencido" : "Completado");
        }
    }
}
