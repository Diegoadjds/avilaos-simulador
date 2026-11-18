package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * La decisión de una política de disco: qué solicitud atender y por qué posiciones tiene que
 * pasar el cabezal para llegar a su primer bloque. La ruta es una lista de paradas; una parada
 * marcada como salto significa que el cabezal aparece ahí de golpe (el regreso de C-SCAN al
 * bloque 0), sin recorrer el camino y sin sumar desplazamiento.
 */
public final class SeleccionDisco {

    private final SolicitudDisco solicitud;
    private final int[] ruta;
    private final boolean[] salto;

    private SeleccionDisco(SolicitudDisco solicitud, int[] ruta, boolean[] salto) {
        this.solicitud = solicitud;
        this.ruta = ruta;
        this.salto = salto;
    }

    /** Ir directo al bloque de la solicitud. */
    public static SeleccionDisco directa(SolicitudDisco solicitud, int cabezal) {
        return new SeleccionDisco(solicitud, new int[]{solicitud.posicionPara(cabezal)}, new boolean[]{false});
    }

    /** SCAN: llegar hasta el extremo del disco, dar la vuelta y recién ahí ir a la solicitud. */
    public static SeleccionDisco porExtremo(SolicitudDisco solicitud, int extremo, int cabezal) {
        return new SeleccionDisco(solicitud, new int[]{extremo, solicitud.posicionPara(cabezal)},
                new boolean[]{false, false});
    }

    /** C-SCAN: llegar al extremo, saltar al otro extremo (gratis) y seguir hasta la solicitud. */
    public static SeleccionDisco circular(SolicitudDisco solicitud, int extremo, int otroExtremo, int cabezal) {
        return new SeleccionDisco(solicitud, new int[]{extremo, otroExtremo, solicitud.posicionPara(cabezal)},
                new boolean[]{false, true, false});
    }

    /** C-LOOK: saltar directo desde la última solicitud hasta la primera del otro lado. */
    public static SeleccionDisco saltoDirecto(SolicitudDisco solicitud, int cabezal) {
        return new SeleccionDisco(solicitud, new int[]{solicitud.posicionPara(cabezal)}, new boolean[]{true});
    }

    public SolicitudDisco getSolicitud() { return solicitud; }

    public int[] getRuta() { return ruta.clone(); }

    public boolean[] getSalto() { return salto.clone(); }
}
