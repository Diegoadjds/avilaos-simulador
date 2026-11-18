package com.mycompany.avilaos.modelo;

/** Dirección en que se mueve el cabezal del disco. */
public enum SentidoCabezal {
    ARRIBA("↑ hacia bloques mayores", 1),
    ABAJO("↓ hacia bloques menores", -1);

    private final String etiqueta;
    private final int signo;

    SentidoCabezal(String etiqueta, int signo) {
        this.etiqueta = etiqueta;
        this.signo = signo;
    }

    /** +1 si sube y -1 si baja: sirve para avanzar la posición del cabezal. */
    public int getSigno() {
        return signo;
    }

    public SentidoCabezal invertido() {
        return this == ARRIBA ? ABAJO : ARRIBA;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
