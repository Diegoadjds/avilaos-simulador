package com.mycompany.avilaos.so.disco;

import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/** FIFO: se atiende en orden de llegada, sin mirar dónde está el cabezal. */
public class PoliticaFifo implements PoliticaDisco {

    @Override
    public PoliticaPlanificacionDisco getTipo() { return PoliticaPlanificacionDisco.FIFO; }

    @Override
    public SeleccionDisco seleccionar(SolicitudDisco[] cola, int cabezal, SentidoCabezal sentido, int limiteSuperior) {
        return SeleccionDisco.directa(cola[0], cabezal);
    }
}
