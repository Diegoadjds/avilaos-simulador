package com.mycompany.avilaos.so.disco;

/** Políticas de disco disponibles. Sirve también para llenar el selector de la interfaz. */
public enum PoliticaPlanificacionDisco {
    FIFO("FIFO"),
    SSTF("SSTF"),
    SCAN("SCAN"),
    C_SCAN("C-SCAN"),
    LOOK("LOOK"),
    C_LOOK("C-LOOK");

    private final String etiqueta;

    PoliticaPlanificacionDisco(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Fábrica: crea la implementación de la política. */
    public PoliticaDisco crear() {
        return switch (this) {
            case FIFO -> new PoliticaFifo();
            case SSTF -> new PoliticaSstf();
            case SCAN -> new PoliticaScan(true);
            case LOOK -> new PoliticaScan(false);
            case C_SCAN -> new PoliticaCScan(true);
            case C_LOOK -> new PoliticaCScan(false);
        };
    }

    /** Acepta "FIFO", "C-SCAN", "c_scan"...; null si no es una política válida. */
    public static PoliticaPlanificacionDisco desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (PoliticaPlanificacionDisco p : values()) {
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
