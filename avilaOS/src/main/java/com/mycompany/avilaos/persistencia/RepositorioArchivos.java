package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.TipoNodo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda y carga el JSON del sistema de archivos (formato del enunciado: diskConfig + fileSystem). Al guardar agrega
 * dos campos opcionales: "chain" (los bloques exactos de cada archivo) y "createdBy", para que el estado se pueda
 * restaurar idéntico. Todos los errores de formato salen como {@link ErrorEscenario} con un mensaje claro.
 */
public final class RepositorioArchivos {

    private RepositorioArchivos() {
    }

    public static void guardar(Path ruta, EscenarioArchivos escenario) throws ErrorEscenario {
        String error = escenario.validar();
        if (error != null) {
            throw new ErrorEscenario("No se puede guardar: " + error);
        }
        try {
            Files.writeString(ruta, aJson(escenario), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErrorEscenario("No se pudo escribir el archivo \"" + ruta.getFileName() + "\": " + e.getMessage(), e);
        }
    }

    public static EscenarioArchivos cargar(Path ruta) throws ErrorEscenario {
        return desdeJson(Lector.leerArchivo(ruta));
    }

    public static String aJson(EscenarioArchivos e) {
        JsonNodo raiz = JsonNodo.objeto();
        JsonNodo disco = JsonNodo.objeto()
                .poner("totalBlocks", e.getTotalBloques())
                .poner("initialHead", e.getCabezalInicial());
        if (e.getVelocidadCabezal() != EscenarioArchivos.VELOCIDAD_NO_DEFINIDA) {
            disco.poner("headSpeed", e.getVelocidadCabezal());
        }
        raiz.poner("diskConfig", disco);
        JsonNodo lista = JsonNodo.arreglo();
        for (EscenarioArchivos.Entrada en : e.getEntradas()) {
            JsonNodo o = JsonNodo.objeto();
            o.poner("path", en.ruta());
            o.poner("type", en.tipo().getCodigoJson());
            if (en.tipo() == TipoNodo.ARCHIVO) {
                o.poner("startBlock", en.primerBloque());
                o.poner("blocks", en.bloques());
                if (en.cadena() != null) {
                    JsonNodo cadena = JsonNodo.arreglo();
                    for (int b : en.cadena()) {
                        cadena.agregar(JsonNodo.numero(b));
                    }
                    o.poner("chain", cadena);
                }
            }
            o.poner("owner", en.dueno());
            o.poner("permissions", en.permiso().getCodigoJson());
            if (en.creadoPor() != null) {
                o.poner("createdBy", en.creadoPor());
            }
            lista.agregar(o);
        }
        raiz.poner("fileSystem", lista);
        return raiz.aTexto();
    }

    public static EscenarioArchivos desdeJson(String texto) throws ErrorEscenario {
        JsonNodo raiz = AnalizadorJson.analizar(texto);
        if (raiz.getTipo() != JsonNodo.Tipo.OBJETO) {
            throw new ErrorEscenario("El archivo debe contener un objeto JSON { ... } con diskConfig y fileSystem");
        }
        JsonNodo disco = Lector.objeto(raiz, "diskConfig", "diskConfig");
        int total = Lector.entero(disco, "totalBlocks", "diskConfig.totalBlocks");
        int cabezal = Lector.entero(disco, "initialHead", "diskConfig.initialHead");
        int velocidad = disco.contiene("headSpeed")
                ? Lector.entero(disco, "headSpeed", "diskConfig.headSpeed") : EscenarioArchivos.VELOCIDAD_NO_DEFINIDA;
        JsonNodo lista = Lector.lista(raiz, "fileSystem", "fileSystem");
        EscenarioArchivos.Entrada[] entradas = new EscenarioArchivos.Entrada[lista.tamano()];
        for (int i = 0; i < entradas.length; i++) {
            String donde = "fileSystem[" + (i + 1) + "]";
            JsonNodo o = lista.obtener(i);
            if (o.getTipo() != JsonNodo.Tipo.OBJETO) {
                throw new ErrorEscenario(donde + ": debe ser un objeto { } y es " + o.describirTipo());
            }
            String ruta = Lector.cadena(o, "path", donde + ".path");
            String textoTipo = Lector.cadena(o, "type", donde + ".type");
            TipoNodo tipo = TipoNodo.desdeTexto(textoTipo);
            if (tipo == null) {
                throw new ErrorEscenario(donde + ".type: \"" + textoTipo + "\" no es válido (use DIRECTORY o FILE)");
            }
            int inicio = -1;
            int bloques = 0;
            int[] cadena = null;
            if (tipo == TipoNodo.ARCHIVO) {
                bloques = Lector.entero(o, "blocks", donde + ".blocks");
                if (o.contiene("chain")) {
                    JsonNodo c = Lector.lista(o, "chain", donde + ".chain");
                    cadena = new int[c.tamano()];
                    for (int k = 0; k < cadena.length; k++) {
                        JsonNodo n = c.obtener(k);
                        if (n.getTipo() != JsonNodo.Tipo.NUMERO || !n.esEntero()) {
                            throw new ErrorEscenario(donde + ".chain[" + (k + 1) + "]: debe ser un número de bloque entero");
                        }
                        cadena[k] = n.getEntero();
                    }
                    inicio = cadena.length > 0 ? cadena[0] : -1;
                } else {
                    inicio = Lector.entero(o, "startBlock", donde + ".startBlock");
                }
            }
            String dueno = o.contiene("owner") ? Lector.cadena(o, "owner", donde + ".owner")
                    : EscenarioArchivos.duenoPorDefecto();
            Permiso permiso = Permiso.PUBLICO;
            if (o.contiene("permissions")) {
                String t = Lector.cadena(o, "permissions", donde + ".permissions");
                permiso = Permiso.desdeTexto(t);
                if (permiso == null) {
                    throw new ErrorEscenario(donde + ".permissions: \"" + t + "\" no es válido (use public o private)");
                }
            }
            String creadoPor = o.contiene("createdBy") ? Lector.cadena(o, "createdBy", donde + ".createdBy")
                    : "carga inicial";
            entradas[i] = new EscenarioArchivos.Entrada(ruta, tipo, inicio, bloques, cadena, dueno, permiso, creadoPor);
        }
        EscenarioArchivos escenario = new EscenarioArchivos(total, cabezal, velocidad, entradas);
        String error = escenario.validar();
        if (error != null) {
            throw new ErrorEscenario("Sistema de archivos inválido: " + error);
        }
        return escenario;
    }
}
