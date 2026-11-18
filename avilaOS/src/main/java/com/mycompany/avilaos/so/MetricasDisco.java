package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;

/**
 * Métricas del disco y de la suspensión de procesos. Las escriben el hilo del disco y el del computador
 * (cada contador lo escribe un solo hilo) y las lee la interfaz.
 *
 * Para comparar las políticas, el desplazamiento y la espera se acumulan POR POLÍTICA (la que estaba
 * activa cuando ocurrió), y además se guarda la curva "desplazamiento acumulado vs. ciclo".
 */
public class MetricasDisco {

    private static final int MAX_PUNTOS = 20_000;

    /** Un punto de la curva: ciclo, desplazamiento acumulado y política activa. */
    public record Punto(long ciclo, long desplazamiento, PoliticaPlanificacionDisco politica) {
    }

    private final int numPoliticas = PoliticaPlanificacionDisco.values().length;
    private final long[] atendidasPorPolitica = new long[numPoliticas];
    private final long[] desplazamientoPorPolitica = new long[numPoliticas];
    private final long[] esperaPorPolitica = new long[numPoliticas];
    private final ListaSegura<Punto> curva = new ListaSegura<>();

    private volatile long desplazamientoTotal;
    private volatile long ciclosOcupado;
    private volatile long ciclosTotales;
    private volatile long sumaEspera;
    private volatile long atendidas;
    private volatile long suspensiones;
    private volatile long reactivaciones;
    private volatile long ultimoPuntoDesplazamiento = -1;

    /** Cada ciclo del disco: guarda el desplazamiento nuevo y un punto de la curva si cambió. */
    public void registrarCiclo(long ciclo, long desplazamientoAcumulado, boolean ocupado,
            PoliticaPlanificacionDisco politica) {
        ciclosTotales++;
        if (ocupado) {
            ciclosOcupado++;
        }
        long delta = desplazamientoAcumulado - desplazamientoTotal;
        if (delta > 0) {
            desplazamientoPorPolitica[politica.ordinal()] += delta;
        }
        desplazamientoTotal = desplazamientoAcumulado;
        if (desplazamientoAcumulado != ultimoPuntoDesplazamiento) {
            agregarPunto(new Punto(ciclo, desplazamientoAcumulado, politica));
            ultimoPuntoDesplazamiento = desplazamientoAcumulado;
        }
    }

    /** Marca en la curva el momento en que se cambió de política (para ver el cambio en el gráfico). */
    public void registrarCambioPolitica(long ciclo, PoliticaPlanificacionDisco politica) {
        agregarPunto(new Punto(ciclo, desplazamientoTotal, politica));
    }

    private void agregarPunto(Punto p) {
        curva.agregarAlFinal(p);
        if (curva.tamano() > MAX_PUNTOS) {
            curva.extraerPrimero();
        }
    }

    /** Una solicitud empieza a atenderse: su espera es lo que estuvo en la cola. */
    public void registrarInicioAtencion(long espera, PoliticaPlanificacionDisco politica) {
        sumaEspera += espera;
        esperaPorPolitica[politica.ordinal()] += espera;
        atendidas++;
        atendidasPorPolitica[politica.ordinal()]++;
    }

    public void registrarSuspension() { suspensiones++; }

    public void registrarReactivacion() { reactivaciones++; }

    public long getDesplazamientoTotal() { return desplazamientoTotal; }

    /** Ciclos promedio que una solicitud esperó en la cola de E/S antes de que el disco empezara a atenderla. */
    public double getEsperaPromedio() {
        return atendidas == 0 ? 0 : (double) sumaEspera / atendidas;
    }

    public double getEsperaPromedio(PoliticaPlanificacionDisco p) {
        long n = atendidasPorPolitica[p.ordinal()];
        return n == 0 ? 0 : (double) esperaPorPolitica[p.ordinal()] / n;
    }

    public long getAtendidas() { return atendidas; }

    public long getAtendidas(PoliticaPlanificacionDisco p) { return atendidasPorPolitica[p.ordinal()]; }

    public long getDesplazamiento(PoliticaPlanificacionDisco p) { return desplazamientoPorPolitica[p.ordinal()]; }

    /** Fracción de ciclos con el disco trabajando (0..1). */
    public double getUtilizacion() {
        return ciclosTotales == 0 ? 0 : (double) ciclosOcupado / ciclosTotales;
    }

    public long getSuspensiones() { return suspensiones; }

    public long getReactivaciones() { return reactivaciones; }

    public Punto[] getCurva() { return curva.aArreglo(new Punto[0]); }
}
