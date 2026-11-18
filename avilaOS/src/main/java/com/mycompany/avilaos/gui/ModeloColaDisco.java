package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.SolicitudDisco;
import javax.swing.table.AbstractTableModel;

/** Tabla de la cola de E/S del disco: la solicitud en atención (primera fila) y las que esperan, con su operación. */
public class ModeloColaDisco extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Estado", "Proceso", "Operación", "Bloque", "Espera"};

    private SolicitudDisco[] filas = new SolicitudDisco[0];
    private int enAtencion;
    private long cicloActual;

    /** @param atendida solicitud que el disco está atendiendo ahora (puede ser null) */
    public void setDatos(SolicitudDisco atendida, SolicitudDisco[] cola, long ciclo) {
        enAtencion = atendida == null ? 0 : 1;
        filas = new SolicitudDisco[cola.length + enAtencion];
        if (atendida != null) {
            filas[0] = atendida;
        }
        System.arraycopy(cola, 0, filas, enAtencion, cola.length);
        cicloActual = ciclo;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() { return filas.length; }

    @Override
    public int getColumnCount() { return COLUMNAS.length; }

    @Override
    public String getColumnName(int c) { return COLUMNAS[c]; }

    @Override
    public Object getValueAt(int fila, int col) {
        SolicitudDisco s = filas[fila];
        switch (col) {
            case 0: return fila < enAtencion ? "atendiendo" : "en cola";
            case 1: return s.getProceso().getEtiqueta() + " " + s.getProceso().getNombre();
            case 2: return s.getOperacion();
            case 3: return s.getBloqueObjetivo() < 0 ? "-" : String.valueOf(s.getBloqueObjetivo());
            default: return fila < enAtencion ? s.getCicloInicio() - s.getCicloLlegada()
                    : Math.max(0, cicloActual - s.getCicloLlegada());
        }
    }
}
