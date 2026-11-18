package com.mycompany.avilaos.modelo;

/** Modo de uso de ÁvilaOS: el administrador puede todo; el usuario tiene acceso restringido. */
public enum ModoUsuario {
    ADMINISTRADOR("Administrador"),
    USUARIO("Usuario");

    /** Nombre del dueño de los archivos del sistema y del usuario del modo administrador. */
    public static final String NOMBRE_ADMIN = "admin";

    private final String etiqueta;

    ModoUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
