package com.mycompany.avilaos.so.disco;

import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * C-SCAN (SCAN circular) y C-LOOK. El cabezal solo atiende mientras sube; al llegar al final regresa al
 * principio de un salto (sin atender y sin contar ese regreso en el desplazamiento) y vuelve a subir.
 * C-SCAN sube hasta el último bloque del disco antes de saltar al 0; C-LOOK salta directo a la solicitud
 * más baja.
 */
public class PoliticaCScan implements PoliticaDisco {

    private final boolean hastaElExtremo;

    public PoliticaCScan(boolean hastaElExtremo) {
        this.hastaElExtremo = hastaElExtremo;
    }

    @Override
    public PoliticaPlanificacionDisco getTipo() {
        return hastaElExtremo ? PoliticaPlanificacionDisco.C_SCAN : PoliticaPlanificacionDisco.C_LOOK;
    }

    @Override
    public SeleccionDisco seleccionar(SolicitudDisco[] cola, int cabezal, SentidoCabezal sentido, int limiteSuperior) {
        SolicitudDisco adelante = null;
        for (SolicitudDisco s : cola) {
            int p = s.posicionPara(cabezal);
            if (p >= cabezal && (adelante == null || p < adelante.posicionPara(cabezal))) {
                adelante = s;
            }
        }
        if (adelante != null) {
            return SeleccionDisco.directa(adelante, cabezal);
        }
        // Nada más arriba: regresa al principio y atiende desde la más baja.
        SolicitudDisco masBaja = cola[0];
        for (SolicitudDisco s : cola) {
            if (s.posicionPara(cabezal) < masBaja.posicionPara(cabezal)) {
                masBaja = s;
            }
        }
        if (hastaElExtremo) {
            return SeleccionDisco.circular(masBaja, limiteSuperior, 0, cabezal);
        }
        return SeleccionDisco.saltoDirecto(masBaja, cabezal);
    }
}
