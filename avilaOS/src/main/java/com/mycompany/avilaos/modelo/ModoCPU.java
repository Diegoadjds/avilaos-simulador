package com.mycompany.avilaos.modelo;

/** Modo de ejecución del procesador. */
public enum ModoCPU {
    USUARIO("Usuario"),
    SISTEMA_OPERATIVO("Sistema operativo");

    private final String etiqueta;

    ModoCPU(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
