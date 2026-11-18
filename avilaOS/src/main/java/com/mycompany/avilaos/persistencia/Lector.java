package com.mycompany.avilaos.persistencia;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ayudas para leer campos de un JsonNodo con mensajes de error claros (ruta del campo y tipo encontrado). */
final class Lector {

    private Lector() {
    }

    static String leerArchivo(Path ruta) throws ErrorEscenario {
        try {
            return Files.readString(ruta, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new ErrorEscenario("El archivo \"" + ruta.getFileName() + "\" no está codificado en UTF-8", e);
        } catch (IOException e) {
            throw new ErrorEscenario("No se pudo leer el archivo \"" + ruta.getFileName() + "\": " + e.getMessage(), e);
        }
    }

    static int entero(JsonNodo objeto, String clave, String ruta) throws ErrorEscenario {
        JsonNodo n = objeto.get(clave);
        if (n == null) {
            throw new ErrorEscenario("Falta el campo " + ruta);
        }
        if (n.getTipo() != JsonNodo.Tipo.NUMERO) {
            throw new ErrorEscenario(ruta + ": debe ser un número entero y es " + n.describirTipo());
        }
        if (!n.esEntero()) {
            throw new ErrorEscenario(ruta + ": debe ser un número entero (sin decimales y dentro del rango permitido)");
        }
        return n.getEntero();
    }

    static String cadena(JsonNodo objeto, String clave, String ruta) throws ErrorEscenario {
        JsonNodo n = objeto.get(clave);
        if (n == null) {
            throw new ErrorEscenario("Falta el campo " + ruta);
        }
        if (n.getTipo() != JsonNodo.Tipo.CADENA) {
            throw new ErrorEscenario(ruta + ": debe ser un texto entre comillas y es " + n.describirTipo());
        }
        return n.getTexto();
    }

    static JsonNodo lista(JsonNodo objeto, String clave, String ruta) throws ErrorEscenario {
        JsonNodo n = objeto.get(clave);
        if (n == null) {
            throw new ErrorEscenario("Falta el campo " + ruta);
        }
        if (n.getTipo() != JsonNodo.Tipo.ARREGLO) {
            throw new ErrorEscenario(ruta + ": debe ser una lista [ ] y es " + n.describirTipo());
        }
        return n;
    }

    static JsonNodo objeto(JsonNodo padre, String clave, String ruta) throws ErrorEscenario {
        JsonNodo n = padre.get(clave);
        if (n == null) {
            throw new ErrorEscenario("Falta el campo " + ruta);
        }
        if (n.getTipo() != JsonNodo.Tipo.OBJETO) {
            throw new ErrorEscenario(ruta + ": debe ser un objeto { } y es " + n.describirTipo());
        }
        return n;
    }
}
