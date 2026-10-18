package com.mycompany.avilaos.persistencia;

/**
 * Se lanza cuando un archivo de escenario no se puede leer, escribir o no es válido.
 * El mensaje está pensado para mostrarse tal cual al usuario en la interfaz.
 */
public class ErrorEscenario extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorEscenario(String mensaje) {
        super(mensaje);
    }

    public ErrorEscenario(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
