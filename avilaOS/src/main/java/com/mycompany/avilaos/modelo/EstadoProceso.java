package com.mycompany.avilaos.modelo;

/** Estados del ciclo de vida de un proceso (modelo de 5 estados). */
public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecución"),
    BLOQUEADO("Bloqueado"),
    TERMINADO("Terminado");

    private final String etiqueta;

    EstadoProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
