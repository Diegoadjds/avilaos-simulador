package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lee y escribe escenarios en archivos JSON (UTF-8). Es la única clase que conoce el formato
 * del archivo; el resto del sistema solo trabaja con {@link Escenario}.
 *
 * Formato (versión 1):
 * <pre>
 * {
 *   "version": 1,
 *   "duracionCicloMs": 200, "latenciaRed": 3, "quantum": 3,
 *   "computadores": [ { "ram": 100, "politica": "ROUND_ROBIN" }, ... ],
 *   "buffers":  [ { "capacidad": 3, "computador": 1 }, ... ],
 *   "procesos": [ { "nombre": "A", "tipo": "CPU_BOUND", "instrucciones": 20, "memoria": 30,
 *                   "prioridad": 2, "deadline": 200, "computador": 1 }, ... ]
 * }
 * </pre>
 * Los campos de cada proceso dependen de su tipo (ver {@link #procesoAJson}). El campo
 * "computador" de un proceso puede ser un número o el texto "auto" (lo asigna ÁvilaOS).
 * "buffers" y "procesos" son opcionales.
 */
public final class RepositorioEscenario {

    private RepositorioEscenario() {
    }

    // ================================================================== archivos

    /** Valida el escenario y lo escribe en {@code ruta} (se sobrescribe si ya existe). */
    public static void guardar(Path ruta, Escenario escenario) throws ErrorEscenario {
        String error = escenario.validar();
        if (error != null) {
            throw new ErrorEscenario("No se puede guardar: " + error);
        }
        try {
            Files.writeString(ruta, aJson(escenario), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErrorEscenario("No se pudo escribir el archivo \"" + ruta.getFileName() + "\": "
                    + e.getMessage(), e);
        }
    }

    /** Lee y valida un escenario. Cualquier problema se informa con un {@link ErrorEscenario}. */
    public static Escenario cargar(Path ruta) throws ErrorEscenario {
        String texto;
        try {
            texto = Files.readString(ruta, StandardCharsets.UTF_8);
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new ErrorEscenario("El archivo \"" + ruta.getFileName() + "\" no está codificado en UTF-8", e);
        } catch (IOException e) {
            throw new ErrorEscenario("No se pudo leer el archivo \"" + ruta.getFileName() + "\": "
                    + e.getMessage(), e);
        }
        return desdeJson(texto);
    }

    // ================================================================== Escenario -> JSON

    public static String aJson(Escenario e) {
        JsonNodo raiz = JsonNodo.objeto();
        raiz.poner("version", Escenario.VERSION);
        raiz.poner("duracionCicloMs", e.getDuracionCicloMs());
        raiz.poner("latenciaRed", e.getLatenciaRed());
        raiz.poner("quantum", e.getQuantum());

        JsonNodo computadores = JsonNodo.arreglo();
        for (int i = 1; i <= e.getNumComputadores(); i++) {
            computadores.agregar(JsonNodo.objeto()
                    .poner("ram", e.getRam(i))
                    .poner("politica", e.getPolitica(i).name()));
        }
        raiz.poner("computadores", computadores);

        JsonNodo buffers = JsonNodo.arreglo();
        for (BufferEscenario b : e.getBuffers()) {
            buffers.agregar(JsonNodo.objeto()
                    .poner("capacidad", b.capacidad())
                    .poner("computador", b.computador()));
        }
        raiz.poner("buffers", buffers);

        JsonNodo procesos = JsonNodo.arreglo();
        for (EspecificacionProceso p : e.getProcesos()) {
            procesos.agregar(procesoAJson(p));
        }
        raiz.poner("procesos", procesos);
        return raiz.aTexto();
    }

    /**
     * Escribe solo los campos que tienen sentido para el tipo del proceso:
     * CPU bound: instrucciones; I/O bound: + ciclosEntreEs y duracionEs;
     * productor/consumidor: buffer, ciclosPorElemento y elementosRequeridos
     * (sus instrucciones se calculan solas, por eso no se guardan).
     */
    private static JsonNodo procesoAJson(EspecificacionProceso p) {
        JsonNodo o = JsonNodo.objeto();
        o.poner("nombre", p.nombre());
        o.poner("tipo", p.tipo().name());
        boolean esProdCons = p.tipo() == TipoProceso.PRODUCTOR || p.tipo() == TipoProceso.CONSUMIDOR;
        if (!esProdCons) {
            o.poner("instrucciones", p.instrucciones());
        }
        o.poner("memoria", p.memoria());
        o.poner("prioridad", p.prioridad());
        o.poner("deadline", p.deadline());
        if (p.tipo() == TipoProceso.IO_BOUND) {
            o.poner("ciclosEntreEs", p.ciclosEntreEs());
            o.poner("duracionEs", p.duracionEs());
        }
        if (esProdCons) {
            o.poner("buffer", p.idBuffer());
            o.poner("ciclosPorElemento", p.ciclosPorElemento());
            o.poner("elementosRequeridos", p.elementosRequeridos());
        }
        if (p.idComputador() == EspecificacionProceso.AUTOMATICO) {
            o.poner("computador", "auto");
        } else {
            o.poner("computador", p.idComputador());
        }
        return o;
    }

    // ================================================================== JSON -> Escenario

    public static Escenario desdeJson(String texto) throws ErrorEscenario {
        JsonNodo raiz = AnalizadorJson.analizar(texto);
        if (raiz.getTipo() != JsonNodo.Tipo.OBJETO) {
            throw new ErrorEscenario("El archivo debe contener un objeto JSON { ... } con el escenario");
        }
        if (raiz.contiene("version")) {
            int version = entero(raiz, "version", "version");
            if (version != Escenario.VERSION) {
                throw new ErrorEscenario("Versión de escenario no soportada: " + version
                        + " (esta versión de ÁvilaOS lee la " + Escenario.VERSION + ")");
            }
        }
        int ciclo = entero(raiz, "duracionCicloMs", "duracionCicloMs");
        int latencia = entero(raiz, "latenciaRed", "latenciaRed");
        int quantum = entero(raiz, "quantum", "quantum");

        JsonNodo comps = lista(raiz, "computadores", true);
        int n = comps.tamano();
        int[] ram = new int[n];
        PoliticaPlanificacion[] politicas = new PoliticaPlanificacion[n];
        for (int i = 0; i < n; i++) {
            String ruta = "computadores[" + (i + 1) + "]";
            JsonNodo c = objeto(comps.obtener(i), ruta);
            ram[i] = entero(c, "ram", ruta + ".ram");
            politicas[i] = politica(cadena(c, "politica", ruta + ".politica"), ruta + ".politica");
        }

        JsonNodo bufs = lista(raiz, "buffers", false);
        BufferEscenario[] buffers = new BufferEscenario[bufs == null ? 0 : bufs.tamano()];
        for (int i = 0; i < buffers.length; i++) {
            String ruta = "buffers[" + (i + 1) + "]";
            JsonNodo b = objeto(bufs.obtener(i), ruta);
            buffers[i] = new BufferEscenario(entero(b, "capacidad", ruta + ".capacidad"),
                    entero(b, "computador", ruta + ".computador"));
        }

        JsonNodo procs = lista(raiz, "procesos", false);
        EspecificacionProceso[] procesos = new EspecificacionProceso[procs == null ? 0 : procs.tamano()];
        for (int i = 0; i < procesos.length; i++) {
            procesos[i] = procesoDesdeJson(objeto(procs.obtener(i), "procesos[" + (i + 1) + "]"),
                    "procesos[" + (i + 1) + "]");
        }

        Escenario escenario = new Escenario(ciclo, latencia, quantum, ram, politicas, buffers, procesos);
        String error = escenario.validar();
        if (error != null) {
            throw new ErrorEscenario("Escenario inválido: " + error);
        }
        return escenario;
    }

    private static EspecificacionProceso procesoDesdeJson(JsonNodo o, String ruta) throws ErrorEscenario {
        String nombre = cadena(o, "nombre", ruta + ".nombre");
        String textoTipo = cadena(o, "tipo", ruta + ".tipo");
        TipoProceso tipo;
        try {
            tipo = TipoProceso.valueOf(textoTipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ErrorEscenario(ruta + ".tipo: \"" + textoTipo + "\" no es un tipo válido (use "
                    + nombres(TipoProceso.values()) + ")");
        }
        boolean esProdCons = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
        int instrucciones = esProdCons ? 1 : entero(o, "instrucciones", ruta + ".instrucciones");
        int memoria = entero(o, "memoria", ruta + ".memoria");
        int prioridad = entero(o, "prioridad", ruta + ".prioridad");
        int deadline = entero(o, "deadline", ruta + ".deadline");
        int ciclosEs = 0;
        int duracionEs = 0;
        if (tipo == TipoProceso.IO_BOUND) {
            ciclosEs = entero(o, "ciclosEntreEs", ruta + ".ciclosEntreEs");
            duracionEs = entero(o, "duracionEs", ruta + ".duracionEs");
        }
        int buffer = 0;
        int ciclosElemento = 0;
        int elementos = 0;
        if (esProdCons) {
            buffer = entero(o, "buffer", ruta + ".buffer");
            ciclosElemento = entero(o, "ciclosPorElemento", ruta + ".ciclosPorElemento");
            elementos = entero(o, "elementosRequeridos", ruta + ".elementosRequeridos");
        }
        return new EspecificacionProceso(nombre, tipo, instrucciones, memoria, prioridad, deadline,
                ciclosEs, duracionEs, buffer, ciclosElemento, elementos, computadorDeProceso(o, ruta));
    }

    /** "computador" puede ser un número o el texto "auto" (también se acepta "automatico"). */
    private static int computadorDeProceso(JsonNodo o, String ruta) throws ErrorEscenario {
        JsonNodo c = o.get("computador");
        String campo = ruta + ".computador";
        if (c == null) {
            throw new ErrorEscenario("Falta el campo " + campo + " (un número de computador o \"auto\")");
        }
        if (c.getTipo() == JsonNodo.Tipo.CADENA) {
            String t = c.getTexto().trim().toLowerCase();
            if (t.equals("auto") || t.equals("automatico") || t.equals("automático")) {
                return EspecificacionProceso.AUTOMATICO;
            }
            throw new ErrorEscenario(campo + ": \"" + c.getTexto() + "\" no es válido (use un número o \"auto\")");
        }
        return entero(o, "computador", campo);
    }

    // ------------------------------------------------------------------ lectura con mensajes claros

    private static int entero(JsonNodo objeto, String clave, String ruta) throws ErrorEscenario {
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

    private static String cadena(JsonNodo objeto, String clave, String ruta) throws ErrorEscenario {
        JsonNodo n = objeto.get(clave);
        if (n == null) {
            throw new ErrorEscenario("Falta el campo " + ruta);
        }
        if (n.getTipo() != JsonNodo.Tipo.CADENA) {
            throw new ErrorEscenario(ruta + ": debe ser un texto entre comillas y es " + n.describirTipo());
        }
        return n.getTexto();
    }

    /** Devuelve la lista de la clave; si no existe, es un error cuando es obligatoria y null si es opcional. */
    private static JsonNodo lista(JsonNodo objeto, String clave, boolean obligatoria) throws ErrorEscenario {
        JsonNodo n = objeto.get(clave);
        if (n == null) {
            if (obligatoria) {
                throw new ErrorEscenario("Falta el campo \"" + clave + "\"");
            }
            return null;
        }
        if (n.getTipo() != JsonNodo.Tipo.ARREGLO) {
            throw new ErrorEscenario(clave + ": debe ser una lista [ ] y es " + n.describirTipo());
        }
        return n;
    }

    private static JsonNodo objeto(JsonNodo n, String ruta) throws ErrorEscenario {
        if (n.getTipo() != JsonNodo.Tipo.OBJETO) {
            throw new ErrorEscenario(ruta + ": debe ser un objeto { } y es " + n.describirTipo());
        }
        return n;
    }

    private static PoliticaPlanificacion politica(String texto, String ruta) throws ErrorEscenario {
        try {
            return PoliticaPlanificacion.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ErrorEscenario(ruta + ": \"" + texto + "\" no es una política válida (use "
                    + nombres(PoliticaPlanificacion.values()) + ")");
        }
    }

    private static String nombres(Enum<?>[] valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(valores[i].name());
        }
        return sb.toString();
    }
}
