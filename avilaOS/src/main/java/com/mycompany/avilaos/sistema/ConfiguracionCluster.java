package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Parámetros de la simulación. Es solo datos; la GUI de configuración los llena y
 * (pendiente) se guardan/cargan en JSON o CSV.
 */
public class ConfiguracionCluster {

    private int duracionCicloMs = 500;
    private int latenciaRed = 3;
    private int quantum = 4;
    private int[] ramPorComputador;
    private PoliticaPlanificacion[] politicas;

    public ConfiguracionCluster(int numComputadores) {
        if (numComputadores < 2) {
            throw new IllegalArgumentException("El cluster necesita al menos 2 computadores");
        }
        ramPorComputador = new int[numComputadores];
        politicas = new PoliticaPlanificacion[numComputadores];
        for (int i = 0; i < numComputadores; i++) {
            ramPorComputador[i] = 100;
            politicas[i] = PoliticaPlanificacion.FCFS;
        }
    }

    public static ConfiguracionCluster porDefecto(int numComputadores) {
        return new ConfiguracionCluster(numComputadores);
    }

    public int getNumComputadores() { return ramPorComputador.length; }
    public int getDuracionCicloMs() { return duracionCicloMs; }
    public void setDuracionCicloMs(int ms) { this.duracionCicloMs = ms; }
    public int getLatenciaRed() { return latenciaRed; }
    public void setLatenciaRed(int ciclos) { this.latenciaRed = ciclos; }
    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }

    /** Índices 0..N-1 (el computador 1 es el índice 0). */
    public int getRam(int indice) { return ramPorComputador[indice]; }
    public void setRam(int indice, int ram) { ramPorComputador[indice] = ram; }
    public PoliticaPlanificacion getPolitica(int indice) { return politicas[indice]; }
    public void setPolitica(int indice, PoliticaPlanificacion politica) { politicas[indice] = politica; }
}
