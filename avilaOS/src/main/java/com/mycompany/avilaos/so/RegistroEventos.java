package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaSegura;

/** Log global de eventos del sistema (compartido por todos los computadores). */
public class RegistroEventos {

    private static final int MAXIMO_LINEAS = 2000;

    private final ListaSegura<String> lineas = new ListaSegura<>();
    private final ListaSegura<ObservadorEventos> observadores = new ListaSegura<>();
    private volatile boolean imprimirEnConsola = true;

    public void registrar(long ciclo, String mensaje) {
        String linea = "[Ciclo " + ciclo + "] " + mensaje;
        lineas.agregarAlFinal(linea);
        if (lineas.tamano() > MAXIMO_LINEAS) {
            lineas.extraerPrimero();
        }
        if (imprimirEnConsola) {
            System.out.println(linea);
        }
        for (ObservadorEventos o : observadores.aArreglo(new ObservadorEventos[0])) {
            o.alRegistrarEvento(linea);
        }
    }

    public void agregarObservador(ObservadorEventos observador) {
        observadores.agregarAlFinal(observador);
    }

    public String[] getLineas() {
        return lineas.aArreglo(new String[0]);
    }

    public void setImprimirEnConsola(boolean valor) {
        this.imprimirEnConsola = valor;
    }
}
