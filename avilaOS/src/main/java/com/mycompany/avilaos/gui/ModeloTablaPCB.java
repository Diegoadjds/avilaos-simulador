package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.modelo.PCB;
import javax.swing.table.AbstractTableModel;

/** Tabla con los elementos del PCB de los procesos de una cola. */
public class ModeloTablaPCB extends AbstractTableModel {

    public static final int NORMAL = 0;
    public static final int BLOQUEADOS = 1;
    public static final int TERMINADOS = 2;
    /** Suspendidos: igual que los bloqueados, para saber también por qué esperan (si aún esperan algo). */
    public static final int SUSPENDIDOS = 3;

    private static final String[] BASE = {"ID", "Nombre", "Tipo", "Prio", "Mem", "Deadline", "Rest."};

    private final int modo;
    private final boolean compacto;
    private final String[] columnas;
    private PCB[] datos = new PCB[0];

    public ModeloTablaPCB(int modo) {
        this(modo, false);
    }

    /**
     * Con {@code compacto} las mismas columnas del PCB usan textos más cortos ("CPU" en vez de "CPU bound",
     * "Sem. llenos" en vez del motivo completo), para que quepan en una tabla angosta. Los datos son los mismos.
     */
    public ModeloTablaPCB(int modo, boolean compacto) {
        this.modo = modo;
        this.compacto = compacto;
        columnas = new String[(modo == NORMAL ? BASE.length : BASE.length + 1)];
        System.arraycopy(BASE, 0, columnas, 0, BASE.length);
        if (compacto) {
            columnas[5] = "Deadl.";
        }
        if (modo != NORMAL) {
            columnas[BASE.length] = modo == TERMINADOS ? "Resultado" : "Motivo";
        }
    }

    /** Proceso que se muestra en una fila (para el tooltip), o null si la fila no existe. */
    public PCB getProceso(int fila) {
        return fila >= 0 && fila < datos.length ? datos[fila] : null;
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
            case 2: return compacto ? tipoCorto(p) : p.getTipo();
            case 3: return p.getPrioridad();
            case 4: return p.getMemoria();
            case 5: return p.getDeadlineRestante();
            case 6: return p.getTiempoRestante();
            default:
                if (modo == BLOQUEADOS) {
                    return (p.isSuspendiendo() ? "→swap " : "") + detalleBloqueo(p, compacto);
                }
                if (modo == SUSPENDIDOS) {
                    return p.getMotivoBloqueo() == com.mycompany.avilaos.modelo.MotivoBloqueo.NINGUNO
                            ? (p.isReactivando() ? "←swap" : "en swap " + p.getPrimerBloqueSwap())
                            : detalleBloqueo(p, compacto);
                }
                if (p.getErrorES() != null) {
                    return compacto ? "Error E/S" : "Error de E/S: " + p.getErrorES();
                }
                return p.isTerminoPorDeadline() ? (compacto ? "Dl. vencido" : "Deadline vencido") : "Completado";
        }
    }

    private static String tipoCorto(PCB p) {
        switch (p.getTipo()) {
            case CPU_BOUND: return "CPU";
            case IO_BOUND: return "I/O";
            default: return "E/S arch.";
        }
    }

    /**
     * Por qué está bloqueado un proceso: espera una E/S simulada (ciclos que faltan) o está esperando que el disco
     * atienda su operación sobre un archivo (y cuál).
     */
    static String detalleBloqueo(PCB p, boolean compacto) {
        int faltan = p.getCiclosBloqueoRestantes();
        switch (p.getMotivoBloqueo()) {
            case ENTRADA_SALIDA:
                String ciclos = faltan <= 0 ? (compacto ? "sale ya" : "sale en el próximo ciclo")
                        : faltan == 1 ? "falta 1" + (compacto ? "" : " ciclo")
                        : "faltan " + faltan + (compacto ? "" : " ciclos");
                return "E/S: " + ciclos;
            case ESPERA_DISCO:
                String op = p.getOperacion() == null ? "" : p.getOperacion().toString().toLowerCase();
                String ruta = p.getRutaArchivo() == null ? "" : p.getRutaArchivo();
                return compacto ? "Disco: " + op + " " + nombreCorto(ruta)
                        : "esperando al disco (" + op + " " + ruta + ")";
            default:
                return "-";
        }
    }

    private static String nombreCorto(String ruta) {
        return ruta.substring(ruta.lastIndexOf('/') + 1);
    }
}
