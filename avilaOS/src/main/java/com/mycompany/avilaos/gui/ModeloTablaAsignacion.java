package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.so.NodoFS;
import java.awt.Color;
import javax.swing.table.AbstractTableModel;

/** Tabla de asignación de archivos: color en el disco, nombre, bloques, primer bloque, dueño y proceso que lo creó. */
public class ModeloTablaAsignacion extends AbstractTableModel {

    public static final String[] COLUMNAS = {"Color", "Nombre", "Bloques", "Primer bloque", "Dueño", "Permiso", "Creado por"};

    private NodoFS[] filas = new NodoFS[0];

    public void setDatos(NodoFS[] archivos) {
        this.filas = archivos;
        fireTableDataChanged();
    }

    public NodoFS getNodo(int fila) {
        return fila >= 0 && fila < filas.length ? filas[fila] : null;
    }

    @Override
    public int getRowCount() { return filas.length; }

    @Override
    public int getColumnCount() { return COLUMNAS.length; }

    @Override
    public String getColumnName(int c) { return COLUMNAS[c]; }

    @Override
    public Class<?> getColumnClass(int c) { return c == 0 ? Color.class : Object.class; }

    @Override
    public Object getValueAt(int fila, int col) {
        NodoFS n = filas[fila];
        switch (col) {
            case 0: return Tema.colorBloque(n.getColorId());
            case 1: return n.getRuta();
            case 2: return n.getBloques();
            case 3: return n.getPrimerBloque() < 0 ? "-" : String.valueOf(n.getPrimerBloque());
            case 4: return n.getDueno();
            case 5: return n.getPermiso();
            default: return n.getCreadoPor() == null ? "-" : n.getCreadoPor();
        }
    }
}
