package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.estructuras.ListaEnlazada;

/**
 * Nodo de un árbol JSON. Se programó a mano (sobre nuestra ListaEnlazada) para no depender
 * de colecciones de java.util ni de librerías externas.
 *
 * Un nodo es de uno de seis tipos ({@link Tipo}). Los objetos guardan sus claves en el mismo
 * orden en que se escribieron, para que los archivos generados sean legibles y estables.
 */
public final class JsonNodo {

    /** Tipos de valor que admite JSON. */
    public enum Tipo { OBJETO, ARREGLO, CADENA, NUMERO, BOOLEANO, NULO }

    private final Tipo tipo;
    private String texto;
    private double numero;
    private boolean booleano;
    private final ListaEnlazada<String> claves = new ListaEnlazada<>();
    private final ListaEnlazada<JsonNodo> hijos = new ListaEnlazada<>();

    private JsonNodo(Tipo tipo) {
        this.tipo = tipo;
    }

    // ------------------------------------------------------------------ fábricas

    public static JsonNodo objeto() {
        return new JsonNodo(Tipo.OBJETO);
    }

    public static JsonNodo arreglo() {
        return new JsonNodo(Tipo.ARREGLO);
    }

    public static JsonNodo cadena(String valor) {
        JsonNodo n = new JsonNodo(Tipo.CADENA);
        n.texto = valor;
        return n;
    }

    public static JsonNodo numero(long valor) {
        JsonNodo n = new JsonNodo(Tipo.NUMERO);
        n.numero = valor;
        n.texto = Long.toString(valor);
        return n;
    }

    /** Usado por el analizador: guarda el número ya convertido y su texto original. */
    static JsonNodo numero(double valor, String textoOriginal) {
        JsonNodo n = new JsonNodo(Tipo.NUMERO);
        n.numero = valor;
        n.texto = textoOriginal;
        return n;
    }

    public static JsonNodo booleano(boolean valor) {
        JsonNodo n = new JsonNodo(Tipo.BOOLEANO);
        n.booleano = valor;
        return n;
    }

    public static JsonNodo nulo() {
        return new JsonNodo(Tipo.NULO);
    }

    // ------------------------------------------------------------------ construcción

    /** Agrega (o, si el nodo es objeto, asocia a una clave) un hijo. Devuelve este nodo para encadenar. */
    public JsonNodo poner(String clave, JsonNodo valor) {
        exigir(Tipo.OBJETO);
        claves.agregarAlFinal(clave);
        hijos.agregarAlFinal(valor);
        return this;
    }

    public JsonNodo poner(String clave, long valor) {
        return poner(clave, numero(valor));
    }

    public JsonNodo poner(String clave, String valor) {
        return poner(clave, cadena(valor));
    }

    public JsonNodo agregar(JsonNodo valor) {
        exigir(Tipo.ARREGLO);
        hijos.agregarAlFinal(valor);
        return this;
    }

    // ------------------------------------------------------------------ consulta

    public Tipo getTipo() { return tipo; }

    /** Valor de una clave de un objeto, o null si no existe. */
    public JsonNodo get(String clave) {
        exigir(Tipo.OBJETO);
        for (int i = 0; i < claves.tamano(); i++) {
            if (claves.obtener(i).equals(clave)) {
                return hijos.obtener(i);
            }
        }
        return null;
    }

    public boolean contiene(String clave) {
        return get(clave) != null;
    }

    /** Cantidad de hijos (elementos de un arreglo o pares de un objeto). */
    public int tamano() { return hijos.tamano(); }

    public JsonNodo obtener(int indice) { return hijos.obtener(indice); }

    public String claveEn(int indice) {
        exigir(Tipo.OBJETO);
        return claves.obtener(indice);
    }

    public String getTexto() {
        exigir(Tipo.CADENA);
        return texto;
    }

    public boolean getBooleano() {
        exigir(Tipo.BOOLEANO);
        return booleano;
    }

    /** true si es un número sin parte decimal que cabe en un int. */
    public boolean esEntero() {
        return tipo == Tipo.NUMERO
                && numero == Math.rint(numero)
                && numero >= Integer.MIN_VALUE && numero <= Integer.MAX_VALUE;
    }

    public int getEntero() {
        if (!esEntero()) {
            throw new IllegalStateException("El nodo no es un entero");
        }
        return (int) numero;
    }

    /** Nombre legible del tipo, para los mensajes de error ("un texto", "un número", ...). */
    public String describirTipo() {
        return switch (tipo) {
            case OBJETO -> "un objeto { }";
            case ARREGLO -> "una lista [ ]";
            case CADENA -> "un texto";
            case NUMERO -> "un número";
            case BOOLEANO -> "un verdadero/falso";
            case NULO -> "null";
        };
    }

    private void exigir(Tipo esperado) {
        if (tipo != esperado) {
            throw new IllegalStateException("Se esperaba " + esperado + " pero el nodo es " + tipo);
        }
    }

    // ------------------------------------------------------------------ escritura

    /** Convierte el árbol a texto JSON con sangría de 2 espacios. */
    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        escribir(sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    /** Un contenedor se escribe en una sola línea si todos sus hijos son valores simples. */
    private boolean esSimple() {
        for (int i = 0; i < hijos.tamano(); i++) {
            Tipo t = hijos.obtener(i).tipo;
            if (t == Tipo.OBJETO || t == Tipo.ARREGLO) {
                return false;
            }
        }
        return true;
    }

    private void escribir(StringBuilder sb, int nivel) {
        switch (tipo) {
            case CADENA -> escribirCadena(sb, texto);
            case NUMERO -> sb.append(texto);
            case BOOLEANO -> sb.append(booleano);
            case NULO -> sb.append("null");
            case ARREGLO -> escribirContenedor(sb, nivel, '[', ']');
            case OBJETO -> escribirContenedor(sb, nivel, '{', '}');
        }
    }

    private void escribirContenedor(StringBuilder sb, int nivel, char abre, char cierra) {
        if (hijos.estaVacia()) {
            sb.append(abre).append(cierra);
            return;
        }
        boolean enLinea = esSimple();
        sb.append(abre);
        for (int i = 0; i < hijos.tamano(); i++) {
            if (i > 0) {
                sb.append(enLinea ? ", " : ",");
            }
            if (enLinea) {
                if (i == 0) {
                    sb.append(' ');
                }
            } else {
                sb.append('\n');
                sangria(sb, nivel + 1);
            }
            if (tipo == Tipo.OBJETO) {
                escribirCadena(sb, claves.obtener(i));
                sb.append(": ");
            }
            hijos.obtener(i).escribir(sb, nivel + 1);
        }
        if (enLinea) {
            sb.append(' ');
        } else {
            sb.append('\n');
            sangria(sb, nivel);
        }
        sb.append(cierra);
    }

    private static void sangria(StringBuilder sb, int nivel) {
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
    }

    private static void escribirCadena(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
