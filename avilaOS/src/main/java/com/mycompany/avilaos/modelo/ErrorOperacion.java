package com.mycompany.avilaos.modelo;

/** Una operación de E/S que no se puede realizar (ruta inexistente, sin permiso, sin bloques libres...). */
public class ErrorOperacion extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorOperacion(String mensaje) {
        super(mensaje);
    }
}
