package com.mycompany.avilaos.so.disco;

import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * SCAN (el "ascensor") y LOOK. El cabezal avanza en su dirección atendiendo lo que encuentra; cuando no
 * queda nada adelante, da la vuelta. SCAN llega hasta el último bloque del disco antes de dar la vuelta;
 * LOOK da la vuelta en la última solicitud pendiente.
 */
public class PoliticaScan implements PoliticaDisco {

    private final boolean hastaElExtremo;

    public PoliticaScan(boolean hastaElExtremo) {
        this.hastaElExtremo = hastaElExtremo;
    }

    @Override
    public PoliticaPlanificacionDisco getTipo() {
        return hastaElExtremo ? PoliticaPlanificacionDisco.SCAN : PoliticaPlanificacionDisco.LOOK;
    }

    @Override
    public SeleccionDisco seleccionar(SolicitudDisco[] cola, int cabezal, SentidoCabezal sentido, int limiteSuperior) {
        int signo = sentido.getSigno();
        // 1) La más cercana que está adelante (en la dirección actual).
        SolicitudDisco adelante = null;
        for (SolicitudDisco s : cola) {
            int delta = (s.posicionPara(cabezal) - cabezal) * signo;
            if (delta >= 0 && (adelante == null || delta < (adelante.posicionPara(cabezal) - cabezal) * signo)) {
                adelante = s;
            }
        }
        if (adelante != null) {
            return SeleccionDisco.directa(adelante, cabezal);
        }
        // 2) No queda nada adelante: se da la vuelta. La primera que encuentra al volver es la más cercana al cabezal.
        SolicitudDisco atras = cola[0];
        for (SolicitudDisco s : cola) {
            if (Math.abs(s.posicionPara(cabezal) - cabezal) < Math.abs(atras.posicionPara(cabezal) - cabezal)) {
                atras = s;
            }
        }
        if (!hastaElExtremo) {
            return SeleccionDisco.directa(atras, cabezal);
        }
        // SCAN: primero hasta el extremo; al volver, la primera es la que está más cerca de ese extremo.
        int extremo = sentido == SentidoCabezal.ARRIBA ? limiteSuperior : 0;
        SolicitudDisco primeraAlVolver = cola[0];
        for (SolicitudDisco s : cola) {
            if (Math.abs(s.posicionPara(cabezal) - extremo) < Math.abs(primeraAlVolver.posicionPara(cabezal) - extremo)) {
                primeraAlVolver = s;
            }
        }
        return SeleccionDisco.porExtremo(primeraAlVolver, extremo, cabezal);
    }
}
