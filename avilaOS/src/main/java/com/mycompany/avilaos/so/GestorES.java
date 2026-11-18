package com.mycompany.avilaos.so;

import com.mycompany.avilaos.hardware.MapaBloques;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.modelo.TipoOperacionES;

/**
 * Gestor de E/S del sistema operativo: el único que convierte lo que piden los procesos (operaciones sobre
 * archivos) y el planificador a mediano plazo (suspender y reactivar) en solicitudes para la cola del disco.
 * Une tres piezas: el sistema de archivos, el mapa de bloques (con el swap) y el planificador de disco.
 */
public class GestorES {

    private final SistemaArchivos archivos;
    private final PlanificadorDisco planificadorDisco;
    private final MapaBloques mapa;

    public GestorES(SistemaArchivos archivos, PlanificadorDisco planificadorDisco) {
        this.archivos = archivos;
        this.planificadorDisco = planificadorDisco;
        this.mapa = archivos.getMapa();
    }

    // =====================================================================
    //  Operaciones sobre archivos (las pide un proceso de E/S al obtener la CPU)
    // =====================================================================

    /** Valida y reserva la operación en el sistema de archivos y encola la solicitud en el disco. */
    public SolicitudDisco solicitarArchivo(PCB proceso, long ciclo) throws ErrorOperacion {
        SistemaArchivos.Operacion op = archivos.iniciar(proceso);
        SolicitudDisco solicitud = new SolicitudDisco(proceso, op.tipo(), op.bloques(), op);
        proceso.setSolicitudActual(solicitud);
        planificadorDisco.encolar(solicitud, ciclo);
        return solicitud;
    }

    /** El disco terminó una solicitud de archivo: el cambio se hace efectivo en el sistema de archivos. */
    public void confirmarArchivo(SolicitudDisco solicitud) {
        archivos.confirmar((SistemaArchivos.Operacion) solicitud.getDetalle());
    }

    // =====================================================================
    //  Swap (las pide el planificador a mediano plazo)
    // =====================================================================

    /**
     * Reserva en el área de intercambio un bloque por cada unidad de memoria del proceso y encola la
     * escritura. Devuelve null si no queda espacio de swap (y no reserva nada).
     */
    public SolicitudDisco solicitarSuspension(PCB proceso, long ciclo) {
        int[] bloques = mapa.reservar(proceso.getMemoria(), true, proceso);
        if (bloques == null) {
            return null;
        }
        proceso.setPrimerBloqueSwap(bloques[0]);
        SolicitudDisco solicitud = new SolicitudDisco(proceso, TipoOperacionES.SUSPENDER, bloques, null);
        proceso.setSolicitudSwap(solicitud);
        planificadorDisco.encolar(solicitud, ciclo);
        return solicitud;
    }

    /** Encola la lectura de los bloques de swap del proceso para traerlo de vuelta a la RAM. */
    public SolicitudDisco solicitarReactivacion(PCB proceso, long ciclo) {
        int[] bloques = mapa.cadena(proceso.getPrimerBloqueSwap());
        SolicitudDisco solicitud = new SolicitudDisco(proceso, TipoOperacionES.REACTIVAR, bloques, null);
        proceso.setSolicitudSwap(solicitud);
        planificadorDisco.encolar(solicitud, ciclo);
        return solicitud;
    }

    /** Devuelve los bloques de swap del proceso (ya volvió a la RAM o terminó). */
    public void liberarSwap(PCB proceso) {
        if (proceso.getPrimerBloqueSwap() >= 0) {
            mapa.liberarCadena(proceso.getPrimerBloqueSwap());
            proceso.setPrimerBloqueSwap(-1);
        }
        proceso.setSolicitudSwap(null);
    }

    // =====================================================================
    //  Proceso terminado a la fuerza (deadline)
    // =====================================================================

    /** Cancela lo que el proceso tenía pendiente en el disco y deshace sus reservas. */
    public void abortarProceso(PCB proceso) {
        SolicitudDisco archivo = proceso.getSolicitudActual();
        if (archivo != null) {
            planificadorDisco.cancelar(archivo);
            if (archivo.getDetalle() != null) {
                archivos.abortar((SistemaArchivos.Operacion) archivo.getDetalle());
            }
            proceso.setSolicitudActual(null);
        }
        SolicitudDisco swap = proceso.getSolicitudSwap();
        if (swap != null) {
            planificadorDisco.cancelar(swap);
        }
        liberarSwap(proceso);
    }

    public SistemaArchivos getArchivos() { return archivos; }

    public PlanificadorDisco getPlanificadorDisco() { return planificadorDisco; }
}
