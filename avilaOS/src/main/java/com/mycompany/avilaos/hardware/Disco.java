package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.modelo.SentidoCabezal;
import com.mycompany.avilaos.modelo.SolicitudDisco;

/**
 * Disco simulado: un componente más del hardware que obedece al reloj. En cada ciclo avanza
 * exactamente un paso de trabajo: o mueve el cabezal hasta {@code velocidad} posiciones, o lee/escribe
 * UN bloque (un ciclo por bloque). Atiende una sola solicitud a la vez.
 *
 * El disco no decide el orden: cuando queda libre le pregunta a su {@link ControladorDisco}
 * (el planificador de disco del SO) cuál es la próxima solicitud y por dónde debe pasar el cabezal.
 */
public class Disco implements ComponenteRelojable {

    private final MapaBloques mapa;
    private volatile int velocidad;
    private volatile int cabezal;
    private volatile SentidoCabezal sentido = SentidoCabezal.ARRIBA;
    private volatile ControladorDisco controlador;

    // Solicitud en curso
    private volatile SolicitudDisco actual;
    private int[] ruta = new int[0];
    private boolean[] salto = new boolean[0];
    private int paradaActual;
    private int bloquesLeidos;

    private volatile long desplazamientoTotal;

    public Disco(int bloquesDatos, int bloquesSwap, int cabezalInicial, int velocidad) {
        this.mapa = new MapaBloques(bloquesDatos, bloquesSwap);
        this.cabezal = cabezalInicial;
        this.velocidad = Math.max(1, velocidad);
    }

    public void setControlador(ControladorDisco controlador) {
        this.controlador = controlador;
    }

    @Override
    public void alCiclo(long ciclo) {
        ControladorDisco c = controlador;
        if (c == null) {
            return;
        }
        SolicitudDisco enCurso = actual;
        if (enCurso != null && enCurso.isCancelada()) {
            actual = null; // su proceso murió (deadline): se abandona la operación
            enCurso = null;
        }
        if (enCurso == null) {
            SeleccionDisco seleccion = c.seleccionar(cabezal, sentido, ciclo);
            if (seleccion != null) {
                comenzar(seleccion, ciclo);
            }
        }
        if (actual != null) {
            trabajar(ciclo);
        }
        c.alFinalizarCiclo(ciclo, desplazamientoTotal, actual != null);
    }

    private void comenzar(SeleccionDisco seleccion, long ciclo) {
        SolicitudDisco s = seleccion.getSolicitud();
        s.setCicloInicio(ciclo);
        ruta = seleccion.getRuta();
        salto = seleccion.getSalto();
        paradaActual = 0;
        bloquesLeidos = 0;
        actual = s;
    }

    /** Un ciclo de trabajo: mueve el cabezal hacia la próxima parada o, si ya llegó, lee/escribe un bloque. */
    private void trabajar(long ciclo) {
        int presupuesto = velocidad;
        boolean seMovio = false;
        while (paradaActual < ruta.length) {
            int destino = ruta[paradaActual];
            if (salto[paradaActual]) {
                cabezal = destino; // salto circular: instantáneo y sin desplazamiento
                paradaActual++;
                continue;
            }
            int distancia = Math.abs(destino - cabezal);
            if (distancia == 0) {
                paradaActual++;
                continue;
            }
            if (presupuesto == 0) {
                return;
            }
            int paso = Math.min(presupuesto, distancia);
            sentido = destino > cabezal ? SentidoCabezal.ARRIBA : SentidoCabezal.ABAJO;
            cabezal += sentido.getSigno() * paso;
            presupuesto -= paso;
            desplazamientoTotal += paso;
            seMovio = true;
            if (cabezal == destino) {
                paradaActual++;
            } else {
                return;
            }
        }
        if (seMovio) {
            return; // llegó a la pista: el bloque se lee en el ciclo siguiente
        }
        procesarBloque(ciclo);
    }

    /** Lee/escribe el bloque donde está el cabezal; si era el último de la solicitud, la completa. */
    private void procesarBloque(long ciclo) {
        SolicitudDisco s = actual;
        bloquesLeidos++;
        if (bloquesLeidos < s.getCantidadBloques()) {
            ruta = new int[]{s.getBloque(bloquesLeidos)};
            salto = new boolean[]{false};
            paradaActual = 0;
            return;
        }
        actual = null;
        s.setCicloFin(ciclo);
        controlador.alCompletar(s, ciclo);
    }

    public MapaBloques getMapa() { return mapa; }
    public int getCabezal() { return cabezal; }
    public SentidoCabezal getSentido() { return sentido; }
    public void setSentido(SentidoCabezal sentido) { this.sentido = sentido; }
    public int getVelocidad() { return velocidad; }
    public void setVelocidad(int velocidad) { this.velocidad = Math.max(1, velocidad); }
    public SolicitudDisco getSolicitudActual() { return actual; }
    public long getDesplazamientoTotal() { return desplazamientoTotal; }
    public int getBloquesDatos() { return mapa.getBloquesDatos(); }
}
