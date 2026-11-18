package com.mycompany.avilaos.so.disco;

import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * Una política de planificación de disco. El {@link com.mycompany.avilaos.so.PlanificadorDisco} NO sabe
 * cuál usa: solo le entrega la cola y recibe la decisión. Agregar una política nueva = crear una clase que
 * implemente esta interfaz y una constante en {@link PoliticaPlanificacionDisco}; el planificador no se toca.
 */
public interface PoliticaDisco {

    PoliticaPlanificacionDisco getTipo();

    /**
     * Elige la próxima solicitud.
     *
     * @param cola          solicitudes esperando, en orden de llegada (nunca vacía)
     * @param cabezal       bloque donde está el cabezal
     * @param sentido       dirección en que se venía moviendo
     * @param limiteSuperior último bloque del disco que el cabezal puede recorrer
     */
    SeleccionDisco seleccionar(SolicitudDisco[] cola, int cabezal, SentidoCabezal sentido, int limiteSuperior);
}
