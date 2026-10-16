package com.mycompany.avilaos.modelo;

/** Tipo de proceso que el usuario puede crear en la Vista 1. */
public enum TipoProceso {
    CPU_BOUND("CPU bound"),
    IO_BOUND("I/O bound"),
    PRODUCTOR("Productor"),
    CONSUMIDOR("Consumidor");

    private final String etiqueta;

    TipoProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
