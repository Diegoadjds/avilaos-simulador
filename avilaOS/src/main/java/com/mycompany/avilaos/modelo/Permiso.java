package com.mycompany.avilaos.modelo;

/** Permiso de un archivo o directorio: privado (solo su dueño y el administrador) o público (todos lo leen). */
public enum Permiso {
    PRIVADO("private", "Privado"),
    PUBLICO("public", "Público");

    private final String codigoJson;
    private final String etiqueta;

    Permiso(String codigoJson, String etiqueta) {
        this.codigoJson = codigoJson;
        this.etiqueta = etiqueta;
    }

    public String getCodigoJson() {
        return codigoJson;
    }

    /** Traduce "public"/"private" (sin importar mayúsculas); null si no es válido. */
    public static Permiso desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toLowerCase();
        for (Permiso p : values()) {
            if (p.codigoJson.equals(t) || p.name().toLowerCase().equals(t)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
