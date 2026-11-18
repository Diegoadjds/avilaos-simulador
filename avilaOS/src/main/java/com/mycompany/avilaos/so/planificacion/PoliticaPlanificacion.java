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

    /** Acepta el nombre del enum o variantes comunes (RR, PRIORITY...); null si no es una política válida. */
    public static PoliticaPlanificacion desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        switch (t) {
            case "RR":
                return ROUND_ROBIN;
            case "PRIORIDAD":
            case "PRIORITY":
            case "PRIORITIES":
                return PRIORIDADES;
            default:
                break;
        }
        for (PoliticaPlanificacion p : values()) {
            if (p.name().equals(t)) {
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
