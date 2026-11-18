package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.hardware.ControladorDisco;
import com.mycompany.avilaos.hardware.SeleccionDisco;
import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.so.disco.PoliticaDisco;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import java.util.concurrent.Semaphore;

/**
 * Planificador de disco: dueño de la ÚNICA cola de E/S del disco local. Cuando el disco queda libre,
 * le entrega la solicitud que la política activa elige. No conoce ninguna política concreta, solo la
 * interfaz {@link PoliticaDisco} (se puede cambiar en pleno funcionamiento).
 *
 * Reglas para que la simulación sea determinista aunque el computador y el disco corran en hilos distintos:
 * - una solicitud solo se puede elegir a partir del ciclo siguiente a su llegada;
 * - con el disco ocioso se espera una ventana de agrupación (como el "plugging" de Linux) para que la política
 *   tenga varias solicitudes entre las cuales elegir; si el disco venía ocupado, elige de inmediato.
 */
public class PlanificadorDisco implements ControladorDisco {

    public static final int VENTANA_POR_DEFECTO = 10;

    private final ListaEnlazada<SolicitudDisco> cola = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1);
    private final ListaSegura<SolicitudDisco> completadas = new ListaSegura<>();
    private final int bloquesDatos;
    private final int bloquesTotales;
    private final RegistroEventos registro;
    private final MetricasDisco metricas = new MetricasDisco();

    private volatile PoliticaPlanificacionDisco tipoPolitica;
    private volatile PoliticaDisco politica;
    private volatile int ventanaAgrupacion = VENTANA_POR_DEFECTO;
    private volatile long cicloActual;
    private volatile long cicloUltimoFin = -100;

    public PlanificadorDisco(PoliticaPlanificacionDisco inicial, int bloquesDatos, int bloquesTotales,
            RegistroEventos registro) {
        this.tipoPolitica = inicial;
        this.politica = inicial.crear();
        this.bloquesDatos = bloquesDatos;
        this.bloquesTotales = bloquesTotales;
        this.registro = registro;
    }

    // =====================================================================
    //  Cola de E/S
    // =====================================================================

    /** Una solicitud llega a la cola de E/S del disco. */
    public void encolar(SolicitudDisco solicitud, long ciclo) {
        solicitud.setCicloLlegada(ciclo);
        mutex.acquireUninterruptibly();
        try {
            cola.agregarAlFinal(solicitud);
        } finally {
            mutex.release();
        }
        registro.registrar(ciclo, "Cola de E/S del disco: llega " + solicitud + " ("
                + solicitud.getOperacion() + ")");
    }

    /** Saca una solicitud de la cola (o la marca para que el disco la abandone) cuando su proceso murió. */
    public void cancelar(SolicitudDisco solicitud) {
        solicitud.cancelar();
        mutex.acquireUninterruptibly();
        try {
            cola.eliminar(solicitud);
        } finally {
            mutex.release();
        }
    }

    /** Las solicitudes que el disco ya terminó antes del ciclo indicado (las recoge el kernel). */
    public SolicitudDisco[] sacarCompletadas(long cicloActual) {
        SolicitudDisco[] todas = completadas.aArreglo(new SolicitudDisco[0]);
        ListaEnlazada<SolicitudDisco> listas = new ListaEnlazada<>();
        for (SolicitudDisco s : todas) {
            if (s.getCicloFin() < cicloActual && completadas.eliminar(s)) {
                listas.agregarAlFinal(s);
            }
        }
        return listas.aArreglo(new SolicitudDisco[0]);
    }

    /** Foto de la cola de E/S, en orden de llegada. */
    public SolicitudDisco[] getCola() {
        mutex.acquireUninterruptibly();
        try {
            return cola.aArreglo(new SolicitudDisco[0]);
        } finally {
            mutex.release();
        }
    }

    // =====================================================================
    //  ControladorDisco (lo llama el hilo del disco)
    // =====================================================================

    @Override
    public SeleccionDisco seleccionar(int cabezal, SentidoCabezal sentido, long ciclo) {
        cicloActual = ciclo;
        mutex.acquireUninterruptibly();
        try {
            SolicitudDisco[] elegibles = elegibles(ciclo);
            if (elegibles.length == 0) {
                return null;
            }
            boolean veniaOcupado = cicloUltimoFin == ciclo - 1;
            if (!veniaOcupado && ciclo - elegibles[0].getCicloLlegada() < ventanaAgrupacion) {
                return null; // agrupando solicitudes
            }
            SeleccionDisco seleccion = politica.seleccionar(elegibles, cabezal, sentido, limiteSuperior(elegibles));
            SolicitudDisco s = seleccion.getSolicitud();
            cola.eliminar(s);
            s.setPoliticaUsada(tipoPolitica.toString());
            metricas.registrarInicioAtencion(ciclo - s.getCicloLlegada(), tipoPolitica);
            registro.registrar(ciclo, "El disco atiende la solicitud de " + s.getProceso().getEtiqueta()
                    + " en el bloque " + (s.getBloqueObjetivo() < 0 ? cabezal : s.getBloqueObjetivo())
                    + " (" + tipoPolitica + ", " + s.getOperacion() + ", cabezal en " + cabezal + ")");
            return seleccion;
        } finally {
            mutex.release();
        }
    }

    /** Solicitudes que ya llegaron en un ciclo anterior (las demás todavía no se pueden elegir). */
    private SolicitudDisco[] elegibles(long ciclo) {
        ListaEnlazada<SolicitudDisco> lista = new ListaEnlazada<>();
        for (int i = 0; i < cola.tamano(); i++) {
            SolicitudDisco s = cola.obtener(i);
            if (s.getCicloLlegada() < ciclo && !s.isCancelada()) {
                lista.agregarAlFinal(s);
            }
        }
        return lista.aArreglo(new SolicitudDisco[0]);
    }

    /** El cabezal solo recorre el área de intercambio si hay una solicitud de swap esperando. */
    private int limiteSuperior(SolicitudDisco[] elegibles) {
        for (SolicitudDisco s : elegibles) {
            if (s.getBloqueObjetivo() >= bloquesDatos) {
                return bloquesTotales - 1;
            }
        }
        return bloquesDatos - 1;
    }

    @Override
    public void alCompletar(SolicitudDisco solicitud, long ciclo) {
        cicloUltimoFin = ciclo;
        completadas.agregarAlFinal(solicitud);
        registro.registrar(ciclo, "El disco termina " + solicitud + " (espera " + (solicitud.getCicloInicio()
                - solicitud.getCicloLlegada()) + " ciclos, servicio " + (ciclo - solicitud.getCicloInicio() + 1)
                + " ciclos)");
    }

    @Override
    public void alFinalizarCiclo(long ciclo, long desplazamientoTotal, boolean ocupado) {
        cicloActual = ciclo;
        metricas.registrarCiclo(ciclo, desplazamientoTotal, ocupado, tipoPolitica);
    }

    // =====================================================================
    //  Política (se puede cambiar mientras el sistema corre)
    // =====================================================================

    public void cambiarPolitica(PoliticaPlanificacionDisco nueva) {
        if (nueva == tipoPolitica) {
            return;
        }
        mutex.acquireUninterruptibly();
        try {
            this.politica = nueva.crear();
            this.tipoPolitica = nueva;
        } finally {
            mutex.release();
        }
        metricas.registrarCambioPolitica(cicloActual, nueva);
        registro.registrar(cicloActual, "El planificador de disco cambia su política a " + nueva);
    }

    public PoliticaPlanificacionDisco getPolitica() { return tipoPolitica; }

    public int getVentanaAgrupacion() { return ventanaAgrupacion; }

    public void setVentanaAgrupacion(int ciclos) { this.ventanaAgrupacion = Math.max(0, ciclos); }

    public MetricasDisco getMetricas() { return metricas; }
}
