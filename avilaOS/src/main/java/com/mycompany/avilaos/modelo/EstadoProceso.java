package com.mycompany.avilaos.modelo;

/**
 * Estados del ciclo de vida de un proceso: modelo de SIETE estados.
 * Los dos estados suspendidos son los procesos que el planificador a mediano plazo
 * sacó de la RAM y escribió en el área de intercambio (swap) del disco.
 */
public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecución"),
    BLOQUEADO("Bloqueado"),
    LISTO_SUSPENDIDO("Listo/Suspendido"),
    BLOQUEADO_SUSPENDIDO("Bloqueado/Suspendido"),
    TERMINADO("Terminado");

    private final String etiqueta;

    EstadoProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** true si el proceso no está en la RAM sino en el área de intercambio. */
    public boolean esSuspendido() {
        return this == LISTO_SUSPENDIDO || this == BLOQUEADO_SUSPENDIDO;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
