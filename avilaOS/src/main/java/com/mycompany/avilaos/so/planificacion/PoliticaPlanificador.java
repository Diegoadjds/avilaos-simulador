package com.mycompany.avilaos.so.planificacion;

import com.mycompany.avilaos.estructuras.Comparador;
import com.mycompany.avilaos.modelo.PCB;

/**
 * Una política de planificación. El Planificador NO sabe qué política usa: solo le pregunta
 * (1) cómo se ordena la cola de listos y (2) cuándo hay que expropiar al proceso en CPU.
 * Agregar una política nueva = crear una clase que implemente esta interfaz
 * (y una constante en {@link PoliticaPlanificacion}); el Planificador no se toca.
 */
public interface PoliticaPlanificador {

    PoliticaPlanificacion getTipo();

    /** Orden de la cola de listos: el que quede primero es el próximo en ejecutarse. */
    Comparador<PCB> getCriterioOrden();

    /**
     * ¿Se debe sacar de la CPU al proceso actual para darle paso al primero de la cola?
     *
     * @param enCpu      proceso que está ejecutando
     * @param candidato  primero de la cola de listos (nunca null)
     * @param ciclosEnCpu ciclos consecutivos que lleva ejecutando
     */
    boolean debeExpropiar(PCB enCpu, PCB candidato, int ciclosEnCpu);

    default int getQuantum() {
        return 0;
    }

    default void setQuantum(int quantum) {
    }
}
