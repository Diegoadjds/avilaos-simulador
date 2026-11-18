package com.mycompany.avilaos.modelo;

/**
 * Tipo de proceso. En el Proyecto 2 ya no hay productores/consumidores: aparece el proceso
 * de E/S de archivos, que es el único que habla con el disco (CRUD sobre el sistema de archivos).
 */
public enum TipoProceso {
    CPU_BOUND("CPU bound"),
    IO_BOUND("I/O bound"),
    ES_ARCHIVO("E/S de archivo");

    private final String etiqueta;

    TipoProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
