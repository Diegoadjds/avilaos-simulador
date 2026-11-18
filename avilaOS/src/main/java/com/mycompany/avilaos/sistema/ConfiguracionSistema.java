package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.so.PlanificadorDisco;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Parámetros de la simulación (solo datos). Incluye lo del Proyecto 1 (duración del ciclo, RAM, política de CPU,
 * quantum) y lo nuevo del Proyecto 2: tamaño del disco y del área de intercambio, política de disco inicial,
 * posición inicial del cabezal, su velocidad y la ventana de agrupación de solicitudes.
 */
public class ConfiguracionSistema {

    public static final int CICLO_MIN_MS = 10;
    public static final int CICLO_MAX_MS = 10_000;
    public static final int RAM_MIN = 1;
    public static final int RAM_MAX = 1_000_000;
    public static final int QUANTUM_MAX = 1000;
    public static final int BLOQUES_MIN = 100;
    public static final int BLOQUES_MAX = 500;
    public static final int SWAP_MIN = 1;
    public static final int SWAP_MAX = 500;
    public static final int VELOCIDAD_MAX = 500;
    public static final int VENTANA_MAX = 1000;

    private int duracionCicloMs = 300;
    private int ram = 1024;
    private PoliticaPlanificacion politicaCpu = PoliticaPlanificacion.FCFS;
    private int quantum = 4;
    private int bloquesDisco = 200;
    private int bloquesSwap = 100;
    private PoliticaPlanificacionDisco politicaDisco = PoliticaPlanificacionDisco.FIFO;
    private int cabezalInicial = 50;
    private int velocidadCabezal = 10;
    private int ventanaAgrupacion = PlanificadorDisco.VENTANA_POR_DEFECTO;

    /** Copia independiente (cada simulación recibe la suya, para poder reiniciar con la configuración original). */
    public ConfiguracionSistema copia() {
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.duracionCicloMs = duracionCicloMs;
        c.ram = ram;
        c.politicaCpu = politicaCpu;
        c.quantum = quantum;
        c.bloquesDisco = bloquesDisco;
        c.bloquesSwap = bloquesSwap;
        c.politicaDisco = politicaDisco;
        c.cabezalInicial = cabezalInicial;
        c.velocidadCabezal = velocidadCabezal;
        c.ventanaAgrupacion = ventanaAgrupacion;
        return c;
    }

    /** Devuelve el primer problema de los parámetros, o null si son válidos. */
    public String validar() {
        if (duracionCicloMs < CICLO_MIN_MS || duracionCicloMs > CICLO_MAX_MS) {
            return "La duración del ciclo debe estar entre " + CICLO_MIN_MS + " y " + CICLO_MAX_MS + " ms";
        }
        if (ram < RAM_MIN || ram > RAM_MAX) {
            return "La RAM debe estar entre " + RAM_MIN + " y " + RAM_MAX;
        }
        if (politicaCpu == null) {
            return "Falta la política de planificación de CPU";
        }
        if (quantum < 1 || quantum > QUANTUM_MAX) {
            return "El quantum debe estar entre 1 y " + QUANTUM_MAX + " ciclos";
        }
        if (bloquesDisco < BLOQUES_MIN || bloquesDisco > BLOQUES_MAX) {
            return "El número de bloques del disco debe estar entre " + BLOQUES_MIN + " y " + BLOQUES_MAX;
        }
        if (bloquesSwap < SWAP_MIN || bloquesSwap > SWAP_MAX) {
            return "El área de intercambio debe tener entre " + SWAP_MIN + " y " + SWAP_MAX + " bloques";
        }
        if (politicaDisco == null) {
            return "Falta la política de planificación de disco";
        }
        if (cabezalInicial < 0 || cabezalInicial >= bloquesDisco) {
            return "El cabezal debe empezar entre el bloque 0 y el " + (bloquesDisco - 1);
        }
        if (velocidadCabezal < 1 || velocidadCabezal > VELOCIDAD_MAX) {
            return "La velocidad del cabezal debe estar entre 1 y " + VELOCIDAD_MAX + " posiciones por ciclo";
        }
        if (ventanaAgrupacion < 0 || ventanaAgrupacion > VENTANA_MAX) {
            return "La ventana de agrupación debe estar entre 0 y " + VENTANA_MAX + " ciclos";
        }
        return null;
    }

    public int getDuracionCicloMs() { return duracionCicloMs; }
    public void setDuracionCicloMs(int ms) { this.duracionCicloMs = ms; }
    public int getRam() { return ram; }
    public void setRam(int ram) { this.ram = ram; }
    public PoliticaPlanificacion getPoliticaCpu() { return politicaCpu; }
    public void setPoliticaCpu(PoliticaPlanificacion politica) { this.politicaCpu = politica; }
    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public int getBloquesDisco() { return bloquesDisco; }
    public void setBloquesDisco(int bloques) { this.bloquesDisco = bloques; }
    public int getBloquesSwap() { return bloquesSwap; }
    public void setBloquesSwap(int bloques) { this.bloquesSwap = bloques; }
    public PoliticaPlanificacionDisco getPoliticaDisco() { return politicaDisco; }
    public void setPoliticaDisco(PoliticaPlanificacionDisco politica) { this.politicaDisco = politica; }
    public int getCabezalInicial() { return cabezalInicial; }
    public void setCabezalInicial(int bloque) { this.cabezalInicial = bloque; }
    public int getVelocidadCabezal() { return velocidadCabezal; }
    public void setVelocidadCabezal(int posiciones) { this.velocidadCabezal = posiciones; }
    public int getVentanaAgrupacion() { return ventanaAgrupacion; }
    public void setVentanaAgrupacion(int ciclos) { this.ventanaAgrupacion = ciclos; }
}
