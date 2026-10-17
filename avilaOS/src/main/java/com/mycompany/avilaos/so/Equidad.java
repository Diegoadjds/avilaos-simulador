package com.mycompany.avilaos.so;

import com.mycompany.avilaos.modelo.EstadoProceso;
import com.mycompany.avilaos.modelo.PCB;

/**
 * Equidad con el índice de Jain sobre la "parte de CPU" que recibió cada proceso:
 * x = instrucciones ejecutadas / ciclos transcurridos desde su llegada.
 * Vale 1.0 cuando todos reciben lo mismo y tiende a 1/n cuando uno acapara el CPU.
 */
public final class Equidad {

    private Equidad() {
    }

    public static double jain(PCB[] procesos, long cicloActual) {
        double suma = 0;
        double sumaCuadrados = 0;
        int n = 0;
        for (PCB p : procesos) {
            long fin = p.getEstado() == EstadoProceso.TERMINADO ? p.getCicloFin() : cicloActual;
            long vida = fin - p.getCicloLlegada();
            if (vida <= 0) {
                continue;
            }
            double x = (double) (p.getInstruccionesTotales() - p.getInstruccionesRestantes()) / vida;
            suma += x;
            sumaCuadrados += x * x;
            n++;
        }
        return (n == 0 || sumaCuadrados == 0) ? 1.0 : (suma * suma) / (n * sumaCuadrados);
    }
}
