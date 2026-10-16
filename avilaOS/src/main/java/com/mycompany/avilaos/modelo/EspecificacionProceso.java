package com.mycompany.avilaos.modelo;

/**
 * Lo que el usuario define en la Vista 1 (o lo que se lee del archivo de configuración).
 * Es solo datos: el PCB se construye a partir de esto con FabricaProcesos.
 *
 * @param ciclosEntreEs     (I/O bound) cada cuántas instrucciones pide una E/S
 * @param duracionEs        (I/O bound) cuántos ciclos queda bloqueado esperando la E/S
 * @param idBuffer          (productor/consumidor) buffer con el que trabaja
 * @param ciclosPorElemento (productor/consumidor) cada cuántos ciclos produce/consume
 * @param elementosRequeridos (productor/consumidor) cuántos elementos necesita para terminar
 * @param idComputador      1..N, o {@link #AUTOMATICO} para que ÁvilaOS decida
 */
public record EspecificacionProceso(
        String nombre,
        TipoProceso tipo,
        int instrucciones,
        int memoria,
        int prioridad,
        int deadline,
        int ciclosEntreEs,
        int duracionEs,
        int idBuffer,
        int ciclosPorElemento,
        int elementosRequeridos,
        int idComputador) {

    public static final int AUTOMATICO = -1;

    public static EspecificacionProceso cpuBound(String nombre, int instrucciones, int memoria,
            int prioridad, int deadline, int idComputador) {
        return new EspecificacionProceso(nombre, TipoProceso.CPU_BOUND, instrucciones, memoria,
                prioridad, deadline, 0, 0, 0, 0, 0, idComputador);
    }

    public static EspecificacionProceso ioBound(String nombre, int instrucciones, int memoria,
            int prioridad, int deadline, int ciclosEntreEs, int duracionEs, int idComputador) {
        return new EspecificacionProceso(nombre, TipoProceso.IO_BOUND, instrucciones, memoria,
                prioridad, deadline, ciclosEntreEs, duracionEs, 0, 0, 0, idComputador);
    }

    /** Validación de tipo/rango para la GUI. Devuelve el mensaje de error, o null si es válida. */
    public String validar() {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre no puede estar vacío";
        }
        if (tipo == null) {
            return "Debe indicar el tipo de proceso";
        }
        if (instrucciones < 1) {
            return "La cantidad de instrucciones debe ser mayor que 0";
        }
        if (memoria < 1) {
            return "La memoria requerida debe ser mayor que 0";
        }
        if (prioridad < 1) {
            return "La prioridad debe ser mayor o igual que 1 (1 = la más alta)";
        }
        if (deadline < 1) {
            return "El deadline debe ser mayor que 0";
        }
        if (tipo == TipoProceso.IO_BOUND && (ciclosEntreEs < 1 || duracionEs < 1)) {
            return "Un proceso I/O bound necesita ciclos entre E/S y duración de E/S mayores que 0";
        }
        if ((tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR)
                && (idBuffer < 1 || ciclosPorElemento < 1 || elementosRequeridos < 1)) {
            return "Un productor/consumidor necesita buffer, ciclos por elemento y elementos requeridos mayores que 0";
        }
        if (idComputador != AUTOMATICO && idComputador < 1) {
            return "El computador debe ser 1..N o automático";
        }
        return null;
    }
}
