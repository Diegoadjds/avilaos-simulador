package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.hardware.MemoriaPrincipal;
import com.mycompany.avilaos.modelo.PCB;

/**
 * Planificador a mediano plazo: decide qué procesos salen de la RAM hacia el área de intercambio del disco y
 * cuáles vuelven. Tanto suspender como reactivar son solicitudes de E/S que pasan por la cola del disco.
 *
 * Criterios (a justificar en la defensa):
 * - Víctima a suspender: de los procesos BLOQUEADOS, el de MENOR prioridad (número de prioridad más alto); si hay
 *   empate, el que lleva más tiempo bloqueado. Un proceso bloqueado no usa la CPU, y el de menor prioridad es el
 *   que menos daño hace si tarda más en volver; el más antiguo bloqueado es el que más probablemente tarde.
 * - Reactivación: entre los Listo/Suspendido que caben en la RAM libre, el de MAYOR prioridad (número menor) y, si
 *   empatan, el que llegó primero. Se reactiva antes de admitir procesos nuevos, para que un proceso que ya
 *   tiene trabajo hecho no quede relegado detrás de los que apenas llegan.
 */
public class MedianoPlazo {

    private final Kernel kernel;
    private final GestorES gestorES;
    private long cicloUltimoAviso = -1;

    MedianoPlazo(Kernel kernel, GestorES gestorES) {
        this.kernel = kernel;
        this.gestorES = gestorES;
    }

    /** Si hay RAM libre, trae de vuelta a los Listo/Suspendido (el de mayor prioridad primero). */
    void reactivar() {
        MemoriaPrincipal ram = kernel.getRam();
        while (true) {
            PCB mejor = null;
            for (PCB p : kernel.colaListosSuspendidos().aArreglo(new PCB[0])) {
                if (p.isReactivando() || p.getMemoria() > ram.getLibre()) {
                    continue;
                }
                if (mejor == null || p.getPrioridad() < mejor.getPrioridad()
                        || (p.getPrioridad() == mejor.getPrioridad() && p.getCicloLlegada() < mejor.getCicloLlegada())) {
                    mejor = p;
                }
            }
            if (mejor == null || !kernel.iniciarReactivacion(mejor)) {
                return;
            }
        }
    }

    /**
     * Si el proceso nuevo más antiguo no cabe en la RAM, suspende procesos bloqueados hasta que (contando la
     * memoria que ya se está liberando) quepa.
     */
    void suspenderParaAdmitir() {
        MemoriaPrincipal ram = kernel.getRam();
        PCB necesitado = null;
        for (PCB p : kernel.colaNuevos().aArreglo(new PCB[0])) {
            if (p.getMemoria() > ram.getLibre()) {
                necesitado = p;
                break;
            }
        }
        if (necesitado == null) {
            return;
        }
        int enCamino = 0;
        for (PCB p : kernel.colaBloqueados().aArreglo(new PCB[0])) {
            if (p.isSuspendiendo()) {
                enCamino += p.getMemoria();
            }
        }
        ListaEnlazada<PCB> descartados = new ListaEnlazada<>();
        while (ram.getLibre() + enCamino < necesitado.getMemoria()) {
            PCB victima = elegirVictima(descartados);
            if (victima == null) {
                avisarSinVictima(necesitado);
                return;
            }
            if (kernel.iniciarSuspension(victima, necesitado)) {
                enCamino += victima.getMemoria();
            } else {
                descartados.agregarAlFinal(victima); // sin espacio de swap para ese proceso: se prueba con otro
            }
        }
    }

    /** El bloqueado de menor prioridad (número más alto) y, si empatan, el que lleva más tiempo bloqueado. */
    private PCB elegirVictima(ListaEnlazada<PCB> descartados) {
        PCB mejor = null;
        for (PCB p : kernel.colaBloqueados().aArreglo(new PCB[0])) {
            if (p.isSuspendiendo() || p.isEventoDuranteSuspension() || descartados.contiene(p)) {
                continue;
            }
            if (mejor == null || p.getPrioridad() > mejor.getPrioridad()
                    || (p.getPrioridad() == mejor.getPrioridad()
                    && p.getCicloInicioBloqueo() < mejor.getCicloInicioBloqueo())) {
                mejor = p;
            }
        }
        return mejor;
    }

    private void avisarSinVictima(PCB necesitado) {
        long ciclo = kernel.getCicloActual();
        if (ciclo - cicloUltimoAviso >= 50) { // para no llenar el log: máximo un aviso cada 50 ciclos
            cicloUltimoAviso = ciclo;
            kernel.registrarEvento("Computador " + kernel.getIdComputador() + ": " + necesitado.getEtiqueta()
                    + " espera RAM y no hay procesos bloqueados que se puedan suspender (o no hay espacio de swap)");
        }
    }

    MetricasDisco getMetricasDisco() {
        return gestorES.getPlanificadorDisco().getMetricas();
    }
}
