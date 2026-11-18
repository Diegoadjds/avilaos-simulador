package com.mycompany.avilaos.modelo;

/** Tipo de elemento del sistema de archivos. */
public enum TipoNodo {
    DIRECTORIO("Directorio", "DIRECTORY"),
    ARCHIVO("Archivo", "FILE");

    private final String etiqueta;
    private final String codigoJson;

    TipoNodo(String etiqueta, String codigoJson) {
        this.etiqueta = etiqueta;
        this.codigoJson = codigoJson;
    }

    public String getCodigoJson() {
        return codigoJson;
    }

    public static TipoNodo desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toUpperCase();
        for (TipoNodo tipo : values()) {
            if (tipo.codigoJson.equals(t) || tipo.name().equals(t)) {
                return tipo;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
