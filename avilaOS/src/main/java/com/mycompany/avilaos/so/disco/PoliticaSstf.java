package com.mycompany.avilaos.so.disco;

import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/** SSTF (Shortest Seek Time First): la solicitud más cercana al cabezal; si empatan, la que llegó antes. */
public class PoliticaSstf implements PoliticaDisco {

    @Override
    public PoliticaPlanificacionDisco getTipo() { return PoliticaPlanificacionDisco.SSTF; }

    @Override
    public SeleccionDisco seleccionar(SolicitudDisco[] cola, int cabezal, SentidoCabezal sentido, int limiteSuperior) {
        SolicitudDisco mejor = cola[0];
        int mejorDistancia = Math.abs(mejor.posicionPara(cabezal) - cabezal);
        for (int i = 1; i < cola.length; i++) {
            int d = Math.abs(cola[i].posicionPara(cabezal) - cabezal);
            if (d < mejorDistancia) {
                mejor = cola[i];
                mejorDistancia = d;
            }
        }
        return SeleccionDisco.directa(mejor, cabezal);
    }
}
