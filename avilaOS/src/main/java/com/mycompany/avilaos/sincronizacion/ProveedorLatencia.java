package com.mycompany.avilaos.sincronizacion;

/** Da la latencia de red (en ciclos) vigente; así se puede cambiar en ejecución. */
public interface ProveedorLatencia {

    int getLatenciaRed();
}
