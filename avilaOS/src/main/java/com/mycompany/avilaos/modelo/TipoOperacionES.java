package com.mycompany.avilaos.modelo;

/**
 * Operación de E/S que se le pide al disco. Las cuatro primeras son el CRUD sobre archivos;
 * SUSPENDER y REACTIVAR las genera el planificador a mediano plazo (swap-out / swap-in).
 */
public enum TipoOperacionES {
    CREAR("Crear", "CREATE"),
    LEER("Leer", "READ"),
    ACTUALIZAR("Actualizar", "UPDATE"),
    ELIMINAR("Eliminar", "DELETE"),
    SUSPENDER("Suspender", null),
    REACTIVAR("Reactivar", null);

    private final String etiqueta;
    private final String codigoJson;

    TipoOperacionES(String etiqueta, String codigoJson) {
        this.etiqueta = etiqueta;
        this.codigoJson = codigoJson;
    }

    /** Nombre de la operación en el archivo JSON de procesos (null si no se puede pedir desde un JSON). */
    public String getCodigoJson() {
        return codigoJson;
    }

    /** true para las cuatro operaciones CRUD (las que pide un proceso de E/S de archivos). */
    public boolean esCrud() {
        return codigoJson != null;
    }

    /** true para las operaciones que escriben en el disco (el disco las trata igual que las lecturas). */
    public boolean modifica() {
        return this == CREAR || this == ACTUALIZAR || this == ELIMINAR;
    }

    /** Traduce el texto del JSON (READ, CREATE...) o el nombre del enum; null si no es válido. */
    public static TipoOperacionES desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toUpperCase();
        for (TipoOperacionES op : values()) {
            if (op.name().equals(t) || (op.codigoJson != null && op.codigoJson.equals(t))) {
                return op;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
