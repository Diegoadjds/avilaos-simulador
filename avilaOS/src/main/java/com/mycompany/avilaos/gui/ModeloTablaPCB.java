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
            columnas[BASE.length] = modo == BLOQUEADOS ? "Motivo" : "Resultado";
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
                    return detalleBloqueo(p, compacto);
                }
                return p.isTerminoPorDeadline() ? (compacto ? "Dl. vencido" : "Deadline vencido") : "Completado";
        }
    }

    private static String tipoCorto(PCB p) {
        switch (p.getTipo()) {
            case CPU_BOUND: return "CPU";
            case IO_BOUND: return "I/O";
            case PRODUCTOR: return "Prod";
            default: return "Cons";
        }
    }

    /**
     * Dónde y por qué está bloqueado un proceso: el semWait del semáforo y el buffer en el que espera,
     * o los ciclos que faltan para que termine una E/S o un acceso remoto.
     */
    static String detalleBloqueo(PCB p, boolean compacto) {
        int faltan = p.getCiclosBloqueoRestantes();
        String ciclos = faltan <= 0 ? (compacto ? "sale ya" : "sale en el próximo ciclo")
                : faltan == 1 ? "falta 1" + (compacto ? "" : " ciclo")
                : "faltan " + faltan + (compacto ? "" : " ciclos");
        String buffer = "B" + p.getIdBuffer();
        switch (p.getMotivoBloqueo()) {
            case ENTRADA_SALIDA: return "E/S: " + ciclos;
            case ACCESO_REMOTO: return (compacto ? "Red: " : "acceso remoto: ") + ciclos;
            case SEMAFORO_MUTEX: return semaforo("mutex", buffer, compacto);
            case SEMAFORO_VACIOS: return semaforo("vacíos", buffer, compacto);
            case SEMAFORO_LLENOS: return semaforo("llenos", buffer, compacto);
            default: return "-";
        }
    }

    private static String semaforo(String nombre, String buffer, boolean compacto) {
        return compacto ? "semWait(" + nombre + ") " + buffer
                : "bloqueado en semWait(" + nombre + ") de " + buffer;
    }
}
