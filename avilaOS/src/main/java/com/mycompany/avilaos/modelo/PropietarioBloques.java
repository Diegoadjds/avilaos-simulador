package com.mycompany.avilaos.modelo;

/**
 * Quien ocupa bloques del disco: un archivo del sistema de archivos o un proceso suspendido
 * (en el área de intercambio). Así el mapa de bloques puede decir "qué archivo o proceso
 * suspendido ocupa cada bloque" sin conocer ni a los archivos ni a los procesos.
 */
public interface PropietarioBloques {

    /** Texto corto para el mapa del disco (nombre del archivo o "swap P3"). */
    String getEtiquetaBloques();

    /** Número que la interfaz traduce a un color (el mismo dueño siempre tiene el mismo color). */
    int getColorId();
}
