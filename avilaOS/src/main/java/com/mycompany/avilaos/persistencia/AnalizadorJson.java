package com.mycompany.avilaos.persistencia;

/**
 * Analizador (parser) de JSON de descenso recursivo. Convierte un texto en un árbol de
 * {@link JsonNodo} y, si el texto está mal formado, lanza {@link ErrorEscenario} indicando
 * la línea y la columna del problema.
 */
public final class AnalizadorJson {

    /** Profundidad máxima de anidamiento: evita desbordar la pila con archivos maliciosos o corruptos. */
    private static final int PROFUNDIDAD_MAXIMA = 32;

    private final String s;
    private int pos = 0;

    private AnalizadorJson(String texto) {
        this.s = texto;
    }

    public static JsonNodo analizar(String texto) throws ErrorEscenario {
        if (texto == null) {
            throw new ErrorEscenario("El archivo está vacío");
        }
        // Algunos editores de Windows (Bloc de notas) guardan UTF-8 con una marca BOM al inicio.
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        AnalizadorJson p = new AnalizadorJson(texto);
        p.saltarEspacios();
        if (p.pos >= p.s.length()) {
            throw new ErrorEscenario("El archivo está vacío");
        }
        JsonNodo raiz = p.valor(0);
        p.saltarEspacios();
        if (p.pos < p.s.length()) {
            throw p.error("hay texto sobrante después del final del JSON");
        }
        return raiz;
    }

    // ------------------------------------------------------------------ gramática

    private JsonNodo valor(int profundidad) throws ErrorEscenario {
        if (profundidad > PROFUNDIDAD_MAXIMA) {
            throw error("el JSON está anidado demasiado profundo");
        }
        if (pos >= s.length()) {
            throw error("el archivo termina inesperadamente");
        }
        char c = s.charAt(pos);
        if (c == '{') {
            return objeto(profundidad);
        }
        if (c == '[') {
            return arreglo(profundidad);
        }
        if (c == '"') {
            return JsonNodo.cadena(cadena());
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return numero();
        }
        if (s.startsWith("true", pos)) {
            pos += 4;
            return JsonNodo.booleano(true);
        }
        if (s.startsWith("false", pos)) {
            pos += 5;
            return JsonNodo.booleano(false);
        }
        if (s.startsWith("null", pos)) {
            pos += 4;
            return JsonNodo.nulo();
        }
        throw error("se esperaba un valor y se encontró '" + c + "'");
    }

    private JsonNodo objeto(int profundidad) throws ErrorEscenario {
        JsonNodo nodo = JsonNodo.objeto();
        pos++; // {
        saltarEspacios();
        if (pos < s.length() && s.charAt(pos) == '}') {
            pos++;
            return nodo;
        }
        while (true) {
            saltarEspacios();
            if (pos >= s.length() || s.charAt(pos) != '"') {
                throw error("se esperaba el nombre de un campo entre comillas");
            }
            String clave = cadena();
            if (nodo.contiene(clave)) {
                throw error("el campo \"" + clave + "\" está repetido");
            }
            saltarEspacios();
            esperar(':');
            saltarEspacios();
            nodo.poner(clave, valor(profundidad + 1));
            saltarEspacios();
            if (pos >= s.length()) {
                throw error("falta cerrar la llave '}'");
            }
            char c = s.charAt(pos++);
            if (c == '}') {
                return nodo;
            }
            if (c != ',') {
                pos--;
                throw error("se esperaba ',' o '}' y se encontró '" + c + "'");
            }
        }
    }

    private JsonNodo arreglo(int profundidad) throws ErrorEscenario {
        JsonNodo nodo = JsonNodo.arreglo();
        pos++; // [
        saltarEspacios();
        if (pos < s.length() && s.charAt(pos) == ']') {
            pos++;
            return nodo;
        }
        while (true) {
            saltarEspacios();
            nodo.agregar(valor(profundidad + 1));
            saltarEspacios();
            if (pos >= s.length()) {
                throw error("falta cerrar el corchete ']'");
            }
            char c = s.charAt(pos++);
            if (c == ']') {
                return nodo;
            }
            if (c != ',') {
                pos--;
                throw error("se esperaba ',' o ']' y se encontró '" + c + "'");
            }
        }
    }

    private String cadena() throws ErrorEscenario {
        pos++; // comilla de apertura
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= s.length()) {
                throw error("falta cerrar las comillas de un texto");
            }
            char c = s.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                pos--;
                throw error("un texto contiene un salto de línea o un carácter de control sin escapar");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= s.length()) {
                throw error("el texto termina en una barra invertida");
            }
            char e = s.charAt(pos++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > s.length()) {
                        throw error("secuencia \\u incompleta");
                    }
                    int codigo = 0;
                    for (int i = 0; i < 4; i++) {
                        int d = Character.digit(s.charAt(pos + i), 16);
                        if (d < 0) {
                            pos += i;
                            throw error("secuencia \\u inválida (se esperaban 4 dígitos hexadecimales)");
                        }
                        codigo = codigo * 16 + d;
                    }
                    pos += 4;
                    sb.append((char) codigo);
                }
                default -> {
                    pos--;
                    throw error("secuencia de escape inválida: \\" + e);
                }
            }
        }
    }

    private JsonNodo numero() throws ErrorEscenario {
        int inicio = pos;
        if (s.charAt(pos) == '-') {
            pos++;
        }
        int digitosEnteros = digitos();
        if (digitosEnteros == 0) {
            throw error("número mal formado");
        }
        if (digitosEnteros > 1 && s.charAt(inicio + (s.charAt(inicio) == '-' ? 1 : 0)) == '0') {
            pos = inicio;
            throw error("los números no pueden empezar con ceros a la izquierda");
        }
        if (pos < s.length() && s.charAt(pos) == '.') {
            pos++;
            if (digitos() == 0) {
                throw error("número mal formado: faltan dígitos después del punto");
            }
        }
        if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
            pos++;
            if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                pos++;
            }
            if (digitos() == 0) {
                throw error("número mal formado: faltan dígitos en el exponente");
            }
        }
        String t = s.substring(inicio, pos);
        return JsonNodo.numero(Double.parseDouble(t), t);
    }

    private int digitos() {
        int n = 0;
        while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
            pos++;
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ utilidades

    private void saltarEspacios() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                return;
            }
        }
    }

    private void esperar(char esperado) throws ErrorEscenario {
        if (pos >= s.length() || s.charAt(pos) != esperado) {
            throw error("se esperaba '" + esperado + "'");
        }
        pos++;
    }

    private ErrorEscenario error(String detalle) {
        int linea = 1;
        int columna = 1;
        for (int i = 0; i < pos && i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                linea++;
                columna = 1;
            } else {
                columna++;
            }
        }
        return new ErrorEscenario("JSON mal formado (línea " + linea + ", columna " + columna + "): " + detalle);
    }
}
