package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * Lo que el disco (hardware) necesita del sistema operativo. El disco no sabe de colas ni de
 * políticas: cuando queda libre le pregunta al controlador cuál es la próxima solicitud.
 * Lo implementa el planificador de disco, que vive en el SO.
 */
public interface ControladorDisco {

    /**
     * El disco está libre: ¿qué atiende ahora?
     *
     * @return la decisión (solicitud y ruta del cabezal), o null si no hay nada que atender todavía
     */
    SeleccionDisco seleccionar(int cabezal, SentidoCabezal sentido, long ciclo);

    /** El disco terminó de leer o escribir todos los bloques de la solicitud. */
    void alCompletar(SolicitudDisco solicitud, long ciclo);

    /** Al final de cada ciclo del disco, para las métricas (desplazamiento acumulado, ocupación). */
    void alFinalizarCiclo(long ciclo, long desplazamientoTotal, boolean ocupado);
}
