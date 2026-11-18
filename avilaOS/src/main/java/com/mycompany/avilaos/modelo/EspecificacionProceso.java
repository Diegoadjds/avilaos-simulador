package com.mycompany.avilaos.modelo;

/**
 * Lo que el usuario define para un proceso (en la interfaz o en el JSON de procesos).
 * Es solo datos: el PCB se construye a partir de esto con FabricaProcesos.
 *
 * @param ciclosEntreEs (I/O bound) cada cuántas instrucciones pide una E/S simulada
 * @param duracionEs    (I/O bound) cuántos ciclos queda bloqueado esperando esa E/S
 * @param operacion     (E/S de archivo) operación CRUD que pide al disco
 * @param ruta          (E/S de archivo) ruta del archivo o directorio, por ejemplo /sistema/boot.bin
 * @param tamanoBloques (E/S de archivo, CREAR) tamaño en bloques; 0 crea un directorio
 * @param nuevoNombre   (E/S de archivo, ACTUALIZAR) nombre nuevo, o null para solo reescribir el archivo
 * @param usuario       dueño del proceso: "admin" o el usuario en cuyo nombre trabaja
 * @param permiso       (E/S de archivo, CREAR) permiso del archivo nuevo
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
        TipoOperacionES operacion,
        String ruta,
        int tamanoBloques,
        String nuevoNombre,
        String usuario,
        Permiso permiso) {

    public static final int MAX_NOMBRE = 30;
    public static final int MAX_INSTRUCCIONES = 1_000_000;
    public static final int MAX_MEMORIA = 1_000_000;
    public static final int MAX_PRIORIDAD = 99;
    public static final int MAX_DEADLINE = 10_000_000;
    public static final int MAX_BLOQUES_ARCHIVO = 500;

    public static EspecificacionProceso cpuBound(String nombre, int instrucciones, int memoria,
            int prioridad, int deadline) {
        return new EspecificacionProceso(nombre, TipoProceso.CPU_BOUND, instrucciones, memoria,
                prioridad, deadline, 0, 0, null, null, 0, null, ModoUsuario.NOMBRE_ADMIN, Permiso.PUBLICO);
    }

    public static EspecificacionProceso ioBound(String nombre, int instrucciones, int memoria,
            int prioridad, int deadline, int ciclosEntreEs, int duracionEs) {
        return new EspecificacionProceso(nombre, TipoProceso.IO_BOUND, instrucciones, memoria,
                prioridad, deadline, ciclosEntreEs, duracionEs, null, null, 0, null,
                ModoUsuario.NOMBRE_ADMIN, Permiso.PUBLICO);
    }

    public static EspecificacionProceso archivo(String nombre, int instrucciones, int memoria,
            int prioridad, int deadline, TipoOperacionES operacion, String ruta, int tamanoBloques,
            String nuevoNombre, String usuario, Permiso permiso) {
        return new EspecificacionProceso(nombre, TipoProceso.ES_ARCHIVO, instrucciones, memoria,
                prioridad, deadline, 0, 0, operacion, ruta, tamanoBloques, nuevoNombre, usuario, permiso);
    }

    /** Validación de tipo/rango para la interfaz y los JSON. Devuelve el mensaje de error, o null si es válida. */
    public String validar() {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre no puede estar vacío";
        }
        if (nombre.trim().length() > MAX_NOMBRE) {
            return "El nombre admite máximo " + MAX_NOMBRE + " caracteres";
        }
        if (tipo == null) {
            return "Debe indicar el tipo de proceso";
        }
        if (instrucciones < 1 || instrucciones > MAX_INSTRUCCIONES) {
            return "Las instrucciones deben estar entre 1 y " + MAX_INSTRUCCIONES;
        }
        if (memoria < 1 || memoria > MAX_MEMORIA) {
            return "La memoria requerida debe estar entre 1 y " + MAX_MEMORIA;
        }
        if (prioridad < 1 || prioridad > MAX_PRIORIDAD) {
            return "La prioridad debe estar entre 1 (la más alta) y " + MAX_PRIORIDAD;
        }
        if (deadline < 1 || deadline > MAX_DEADLINE) {
            return "El deadline debe estar entre 1 y " + MAX_DEADLINE + " ciclos";
        }
        if (tipo == TipoProceso.IO_BOUND && (ciclosEntreEs < 1 || duracionEs < 1
                || ciclosEntreEs > 100_000 || duracionEs > 100_000)) {
            return "Un proceso I/O bound necesita ciclos entre E/S y duración de E/S entre 1 y 100000";
        }
        if (tipo == TipoProceso.ES_ARCHIVO) {
            return validarArchivo();
        }
        return null;
    }

    private String validarArchivo() {
        if (instrucciones < 2) {
            return "Un proceso de E/S necesita al menos 2 instrucciones (una para pedir la operación "
                    + "al disco y otra para terminar)";
        }
        if (operacion == null || !operacion.esCrud()) {
            return "Debe indicar la operación: CREATE, READ, UPDATE o DELETE";
        }
        if (ruta == null || ruta.isBlank() || !ruta.startsWith("/")) {
            return "La ruta debe empezar con / (por ejemplo /sistema/boot.bin)";
        }
        if (tamanoBloques < 0 || tamanoBloques > MAX_BLOQUES_ARCHIVO) {
            return "El tamaño del archivo debe estar entre 0 y " + MAX_BLOQUES_ARCHIVO + " bloques";
        }
        if (operacion == TipoOperacionES.CREAR && (ruta.equals("/") || ruta.endsWith("/"))) {
            return "La ruta del elemento a crear debe terminar en su nombre";
        }
        if (usuario == null || usuario.isBlank()) {
            return "Debe indicar el usuario dueño del proceso";
        }
        return null;
    }
}
