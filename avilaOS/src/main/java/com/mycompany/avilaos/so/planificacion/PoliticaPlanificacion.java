package com.mycompany.avilaos.so.planificacion;

/** Políticas disponibles. Sirve también para llenar el selector (combo) de la GUI. */
public enum PoliticaPlanificacion {
    FCFS("FCFS"),
    EDF("EDF (Earliest Deadline First)"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDADES("Prioridades apropiativas");

    private final String etiqueta;

    PoliticaPlanificacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Fábrica: crea la implementación de la política. */
    public PoliticaPlanificador crear(int quantum) {
        return switch (this) {
            case FCFS -> new PoliticaFCFS();
            case EDF -> new PoliticaEDF();
            case ROUND_ROBIN -> new PoliticaRoundRobin(quantum);
            case PRIORIDADES -> new PoliticaPrioridades();
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
