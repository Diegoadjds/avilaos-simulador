package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.hardware.MapaBloques;
import com.mycompany.avilaos.modelo.ErrorOperacion;
import com.mycompany.avilaos.modelo.ModoUsuario;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.TipoNodo;
import com.mycompany.avilaos.modelo.TipoOperacionES;
import java.util.concurrent.Semaphore;

/**
 * Sistema de archivos jerárquico sobre el disco. Guarda el árbol de directorios y archivos y, con el
 * {@link MapaBloques}, asigna los bloques de cada archivo de forma encadenada.
 *
 * Importante: NADIE llama a las operaciones de la interfaz directamente. Un proceso de E/S llama a
 * {@link #iniciar(PCB)} (valida, reserva y deja marcado lo que va a cambiar) y, cuando el disco termina
 * la solicitud, el kernel llama a {@link #confirmar(Operacion)}; si el proceso muere antes, a
 * {@link #abortar(Operacion)}. Así el estado visible cambia cuando el disco realmente atiende.
 *
 * Todo el árbol está protegido por un semáforo, porque lo tocan el hilo del computador y la interfaz.
 */
public class SistemaArchivos {

    public static final int MAX_NOMBRE = 40;

    /** Lo que un proceso de E/S pidió, ya validado: el nodo afectado y los bloques que el disco debe recorrer. */
    public record Operacion(TipoOperacionES tipo, NodoFS nodo, NodoFS[] afectados, int[] bloques,
            String nuevoNombre) {
    }

    private final MapaBloques mapa;
    private final NodoFS raiz;
    private final Semaphore mutex = new Semaphore(1);
    private volatile long version;
    private int siguienteColor = 1;

    public SistemaArchivos(MapaBloques mapa) {
        this.mapa = mapa;
        this.raiz = new NodoFS("", TipoNodo.DIRECTORIO, ModoUsuario.NOMBRE_ADMIN, Permiso.PUBLICO, null,
                "sistema", 0);
    }

    // =====================================================================
    //  Carga directa (al leer el JSON del sistema de archivos; no pasa por el disco)
    // =====================================================================

    /** Crea un directorio con su padre ya existente. */
    public NodoFS cargarDirectorio(String ruta, String dueno, Permiso permiso, String creadoPor) throws ErrorOperacion {
        mutex.acquireUninterruptibly();
        try {
            NodoFS padre = padreDe(ruta);
            String nombre = nombreDe(ruta);
            validarNombre(nombre);
            if (buscarHijo(padre, nombre) != null) {
                throw new ErrorOperacion("Ya existe " + ruta);
            }
            NodoFS n = new NodoFS(nombre, TipoNodo.DIRECTORIO, dueno, permiso, padre, creadoPor, siguienteColor++);
            padre.hijosInternos().agregarAlFinal(n);
            version++;
            return n;
        } finally {
            mutex.release();
        }
    }

    /**
     * Crea un archivo ya existente en el disco.
     *
     * @param cadena bloques exactos que ocupa (en orden), o null para encadenar desde {@code primerBloque}
     */
    public NodoFS cargarArchivo(String ruta, int primerBloque, int bloques, int[] cadena, String dueno,
            Permiso permiso, String creadoPor) throws ErrorOperacion {
        mutex.acquireUninterruptibly();
        try {
            NodoFS padre = padreDe(ruta);
            String nombre = nombreDe(ruta);
            validarNombre(nombre);
            if (buscarHijo(padre, nombre) != null) {
                throw new ErrorOperacion("Ya existe " + ruta);
            }
            NodoFS n = new NodoFS(nombre, TipoNodo.ARCHIVO, dueno, permiso, padre, creadoPor, siguienteColor++);
            int[] ocupados;
            if (cadena != null) {
                if (!mapa.reservarExactos(cadena, n)) {
                    throw new ErrorOperacion(ruta + ": alguno de sus bloques indicados no existe, está repetido o ya está ocupado");
                }
                ocupados = cadena;
            } else {
                ocupados = mapa.reservarDesde(primerBloque, bloques, n);
                if (ocupados == null) {
                    throw new ErrorOperacion(ruta + ": el bloque inicial " + primerBloque
                            + " no existe, está ocupado o no hay " + bloques + " bloques libres");
                }
            }
            n.setBloques(ocupados.length, ocupados[0]);
            padre.hijosInternos().agregarAlFinal(n);
            version++;
            return n;
        } finally {
            mutex.release();
        }
    }

    // =====================================================================
    //  Consultas
    // =====================================================================

    public NodoFS getRaiz() { return raiz; }

    public NodoFS buscar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            return buscarInterno(ruta);
        } finally {
            mutex.release();
        }
    }

    /** Hijos de un directorio (foto). */
    public NodoFS[] hijos(NodoFS nodo) {
        mutex.acquireUninterruptibly();
        try {
            return nodo.hijosInternos().aArreglo(new NodoFS[0]);
        } finally {
            mutex.release();
        }
    }

    /** Todos los nodos en preorden (directorios antes que su contenido), sin la raíz. */
    public NodoFS[] preorden() {
        mutex.acquireUninterruptibly();
        try {
            ListaEnlazada<NodoFS> salida = new ListaEnlazada<>();
            recorrer(raiz, salida, false);
            return salida.aArreglo(new NodoFS[0]);
        } finally {
            mutex.release();
        }
    }

    /** Solo los archivos, en preorden (para la tabla de asignación). */
    public NodoFS[] archivos() {
        mutex.acquireUninterruptibly();
        try {
            ListaEnlazada<NodoFS> salida = new ListaEnlazada<>();
            recorrer(raiz, salida, true);
            return salida.aArreglo(new NodoFS[0]);
        } finally {
            mutex.release();
        }
    }

    /** Cambia cada vez que el árbol se modifica (la interfaz lo compara para saber si debe redibujar). */
    public long getVersion() { return version; }

    public MapaBloques getMapa() { return mapa; }

    /** Cadena de bloques de un archivo, leída del mapa. */
    public int[] bloquesDe(NodoFS archivo) {
        return archivo.getPrimerBloque() < 0 ? new int[0] : mapa.cadena(archivo.getPrimerBloque());
    }

    // =====================================================================
    //  Permisos
    // =====================================================================

    /**
     * Revisa SIN modificar nada si {@code usuario} puede pedir la operación. Sirve para que la interfaz rechace
     * lo no permitido explicando por qué, antes de crear el proceso.
     *
     * @return el motivo del rechazo, o null si se permite
     */
    public String verificarPermiso(TipoOperacionES operacion, String ruta, String nuevoNombre, String usuario) {
        mutex.acquireUninterruptibly();
        try {
            return permisoInterno(operacion, ruta, nuevoNombre, usuario);
        } finally {
            mutex.release();
        }
    }

    private String permisoInterno(TipoOperacionES operacion, String ruta, String nuevoNombre, String usuario) {
        boolean admin = ModoUsuario.NOMBRE_ADMIN.equals(usuario);
        if (operacion == TipoOperacionES.CREAR) {
            String errorRuta = errorDeRuta(ruta);
            if (errorRuta != null) {
                return errorRuta;
            }
            NodoFS padre = buscarInterno(rutaPadre(ruta));
            if (padre == null) {
                return "No existe el directorio " + rutaPadre(ruta) + " donde se quiere crear " + ruta;
            }
            if (!padre.esDirectorio()) {
                return rutaPadre(ruta) + " es un archivo, no un directorio";
            }
            if (!admin && !usuario.equals(padre.getDueno())) {
                return "Modo usuario: solo puedes crear dentro de tus propios directorios, y "
                        + padre.getRuta() + " pertenece a " + padre.getDueno();
            }
            return null;
        }
        NodoFS nodo = buscarInterno(ruta);
        if (nodo == null || nodo == raiz) {
            return nodo == raiz ? "No se puede operar sobre la raíz del sistema de archivos"
                    : "No existe " + ruta + " en el sistema de archivos";
        }
        if (admin) {
            return null;
        }
        boolean propio = usuario.equals(nodo.getDueno());
        switch (operacion) {
            case LEER:
                if (!propio && nodo.getPermiso() != Permiso.PUBLICO) {
                    return "Modo usuario: " + ruta + " es privado y pertenece a " + nodo.getDueno();
                }
                return null;
            case ACTUALIZAR:
                if (nuevoNombre != null && !nuevoNombre.isBlank()) {
                    return "Modo usuario: solo el administrador puede cambiar el nombre de archivos y directorios";
                }
                if (!propio) {
                    return "Modo usuario: no puedes modificar " + ruta + " porque pertenece a " + nodo.getDueno();
                }
                if (nodo.esDirectorio()) {
                    return "Modo usuario: los directorios solo los modifica el administrador";
                }
                return null;
            case ELIMINAR:
                if (!propio) {
                    return "Modo usuario: no puedes eliminar " + ruta + " porque pertenece a " + nodo.getDueno();
                }
                if (nodo.esDirectorio()) {
                    return "Modo usuario: los directorios solo los elimina el administrador";
                }
                return null;
            default:
                return "Operación no permitida";
        }
    }

    // =====================================================================
    //  Operaciones de los procesos de E/S
    // =====================================================================

    /**
     * Valida la operación del proceso, reserva lo necesario (bloques, marcas de uso) y devuelve lo que el disco
     * debe recorrer. Si algo falla no queda nada reservado.
     */
    public Operacion iniciar(PCB p) throws ErrorOperacion {
        mutex.acquireUninterruptibly();
        try {
            TipoOperacionES op = p.getOperacion();
            String ruta = p.getRutaArchivo();
            String error = permisoInterno(op, ruta, p.getNuevoNombre(), p.getUsuario());
            if (error != null) {
                throw new ErrorOperacion(error);
            }
            String autor = p.getEtiqueta() + " (" + p.getNombre() + ")";
            switch (op) {
                case CREAR:
                    return iniciarCrear(p, ruta, autor);
                case LEER:
                    return iniciarLeer(ruta);
                case ACTUALIZAR:
                    return iniciarActualizar(ruta, p.getNuevoNombre());
                case ELIMINAR:
                    return iniciarEliminar(ruta);
                default:
                    throw new ErrorOperacion("Operación no válida para un proceso de archivos: " + op);
            }
        } finally {
            mutex.release();
        }
    }

    private Operacion iniciarCrear(PCB p, String ruta, String autor) throws ErrorOperacion {
        NodoFS padre = buscarInterno(rutaPadre(ruta));
        String nombre = nombreDe(ruta);
        validarNombre(nombre);
        if (buscarHijo(padre, nombre) != null) {
            throw new ErrorOperacion("Ya existe " + ruta);
        }
        int tamano = p.getTamanoBloques();
        if (tamano == 0) {
            NodoFS dir = new NodoFS(nombre, TipoNodo.DIRECTORIO, p.getUsuario(), p.getPermiso(), padre, autor,
                    siguienteColor++);
            dir.setEnCreacion(true);
            padre.hijosInternos().agregarAlFinal(dir);
            version++;
            return new Operacion(TipoOperacionES.CREAR, dir, new NodoFS[]{dir}, new int[0], null);
        }
        NodoFS archivo = new NodoFS(nombre, TipoNodo.ARCHIVO, p.getUsuario(), p.getPermiso(), padre, autor,
                siguienteColor++);
        int[] bloques = mapa.reservar(tamano, false, archivo);
        if (bloques == null) {
            throw new ErrorOperacion("No hay bloques libres suficientes para crear " + ruta + " (necesita "
                    + tamano + ", libres " + mapa.getLibres(false) + ")");
        }
        archivo.setBloques(bloques.length, bloques[0]);
        archivo.setEnCreacion(true);
        padre.hijosInternos().agregarAlFinal(archivo);
        version++;
        return new Operacion(TipoOperacionES.CREAR, archivo, new NodoFS[]{archivo}, bloques, null);
    }

    private Operacion iniciarLeer(String ruta) throws ErrorOperacion {
        NodoFS nodo = buscarInterno(ruta);
        if (nodo.isEscribiendo() || nodo.isEnCreacion()) {
            throw new ErrorOperacion(ruta + " está siendo modificado por otro proceso");
        }
        nodo.setLecturasEnCurso(nodo.getLecturasEnCurso() + 1);
        version++;
        return new Operacion(TipoOperacionES.LEER, nodo, new NodoFS[]{nodo}, bloquesDe(nodo), null);
    }

    private Operacion iniciarActualizar(String ruta, String nuevoNombre) throws ErrorOperacion {
        NodoFS nodo = buscarInterno(ruta);
        if (nodo.estaOcupado()) {
            throw new ErrorOperacion(ruta + " está siendo usado por otro proceso");
        }
        String nombreNuevo = nuevoNombre == null || nuevoNombre.isBlank() ? null : nuevoNombre.trim();
        if (nombreNuevo != null) {
            validarNombre(nombreNuevo);
            NodoFS hermano = buscarHijo(nodo.getPadre(), nombreNuevo);
            if (hermano != null && hermano != nodo) {
                throw new ErrorOperacion("Ya existe un elemento llamado " + nombreNuevo + " en "
                        + nodo.getPadre().getRuta());
            }
        }
        nodo.setEscribiendo(true);
        version++;
        return new Operacion(TipoOperacionES.ACTUALIZAR, nodo, new NodoFS[]{nodo}, bloquesDe(nodo), nombreNuevo);
    }

    private Operacion iniciarEliminar(String ruta) throws ErrorOperacion {
        NodoFS nodo = buscarInterno(ruta);
        ListaEnlazada<NodoFS> subarbol = new ListaEnlazada<>();
        subarbol.agregarAlFinal(nodo);
        recorrer(nodo, subarbol, false);
        NodoFS[] afectados = subarbol.aArreglo(new NodoFS[0]);
        int total = 0;
        for (NodoFS n : afectados) {
            if (n.estaOcupado()) {
                throw new ErrorOperacion(n.getRuta() + " está siendo usado por otro proceso");
            }
            total += n.getBloques();
        }
        int[] bloques = new int[total];
        int k = 0;
        for (NodoFS n : afectados) {
            if (!n.esDirectorio()) {
                for (int b : bloquesDe(n)) {
                    bloques[k++] = b;
                }
            }
        }
        for (NodoFS n : afectados) {
            n.setEscribiendo(true);
        }
        version++;
        return new Operacion(TipoOperacionES.ELIMINAR, nodo, afectados, bloques, null);
    }

    /** El disco terminó la solicitud: la operación se hace efectiva en el sistema de archivos. */
    public void confirmar(Operacion op) {
        mutex.acquireUninterruptibly();
        try {
            NodoFS nodo = op.nodo();
            switch (op.tipo()) {
                case CREAR -> nodo.setEnCreacion(false);
                case LEER -> nodo.setLecturasEnCurso(Math.max(0, nodo.getLecturasEnCurso() - 1));
                case ACTUALIZAR -> {
                    if (op.nuevoNombre() != null) {
                        nodo.setNombre(op.nuevoNombre());
                    }
                    nodo.setEscribiendo(false);
                }
                case ELIMINAR -> {
                    for (NodoFS n : op.afectados()) {
                        if (!n.esDirectorio()) {
                            mapa.liberarCadena(n.getPrimerBloque());
                        }
                    }
                    nodo.getPadre().hijosInternos().eliminar(nodo);
                }
                default -> {
                }
            }
            version++;
        } finally {
            mutex.release();
        }
    }

    /** El proceso murió antes de que el disco terminara: se deshace lo que se había reservado. */
    public void abortar(Operacion op) {
        mutex.acquireUninterruptibly();
        try {
            NodoFS nodo = op.nodo();
            switch (op.tipo()) {
                case CREAR -> {
                    if (!nodo.esDirectorio()) {
                        mapa.liberarCadena(nodo.getPrimerBloque());
                    }
                    nodo.getPadre().hijosInternos().eliminar(nodo);
                }
                case LEER -> nodo.setLecturasEnCurso(Math.max(0, nodo.getLecturasEnCurso() - 1));
                case ACTUALIZAR -> nodo.setEscribiendo(false);
                case ELIMINAR -> {
                    for (NodoFS n : op.afectados()) {
                        n.setEscribiendo(false);
                    }
                }
                default -> {
                }
            }
            version++;
        } finally {
            mutex.release();
        }
    }

    // =====================================================================
    //  Rutas (auxiliares; se llaman con el semáforo tomado)
    // =====================================================================

    private NodoFS buscarInterno(String ruta) {
        if (ruta == null || !ruta.startsWith("/")) {
            return null;
        }
        NodoFS actual = raiz;
        for (String parte : partes(ruta)) {
            if (!actual.esDirectorio()) {
                return null;
            }
            actual = buscarHijo(actual, parte);
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    private static NodoFS buscarHijo(NodoFS padre, String nombre) {
        ListaEnlazada<NodoFS> hs = padre.hijosInternos();
        for (int i = 0; i < hs.tamano(); i++) {
            if (hs.obtener(i).getNombre().equals(nombre)) {
                return hs.obtener(i);
            }
        }
        return null;
    }

    private NodoFS padreDe(String ruta) throws ErrorOperacion {
        String error = errorDeRuta(ruta);
        if (error != null) {
            throw new ErrorOperacion(error);
        }
        NodoFS padre = buscarInterno(rutaPadre(ruta));
        if (padre == null || !padre.esDirectorio()) {
            throw new ErrorOperacion("No existe el directorio " + rutaPadre(ruta) + " (en " + ruta + ")");
        }
        return padre;
    }

    /** Problema de forma de la ruta de un elemento a crear, o null si está bien. */
    private static String errorDeRuta(String ruta) {
        if (ruta == null || !ruta.startsWith("/") || ruta.length() < 2 || ruta.endsWith("/")) {
            return "Ruta inválida \"" + ruta + "\": debe ser absoluta, por ejemplo /usuarios/notas.txt";
        }
        for (String parte : partes(ruta)) {
            if (parte.equals(".") || parte.equals("..")) {
                return "Ruta inválida \"" + ruta + "\": no se permiten . ni ..";
            }
        }
        return null;
    }

    private static void validarNombre(String nombre) throws ErrorOperacion {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorOperacion("El nombre no puede estar vacío");
        }
        if (nombre.length() > MAX_NOMBRE) {
            throw new ErrorOperacion("El nombre \"" + nombre + "\" supera los " + MAX_NOMBRE + " caracteres");
        }
        if (nombre.indexOf('/') >= 0 || nombre.equals(".") || nombre.equals("..")) {
            throw new ErrorOperacion("El nombre \"" + nombre + "\" no es válido (no puede tener / ni ser . o ..)");
        }
    }

    /** Valida un nombre desde la interfaz; devuelve el error o null. */
    public static String errorDeNombre(String nombre) {
        try {
            validarNombre(nombre);
            return null;
        } catch (ErrorOperacion e) {
            return e.getMessage();
        }
    }

    static String[] partes(String ruta) {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        int inicio = 0;
        for (int i = 0; i <= ruta.length(); i++) {
            if (i == ruta.length() || ruta.charAt(i) == '/') {
                if (i > inicio) {
                    lista.agregarAlFinal(ruta.substring(inicio, i));
                }
                inicio = i + 1;
            }
        }
        return lista.aArreglo(new String[0]);
    }

    /** Ruta del directorio que contiene al elemento ("/" si está en la raíz). */
    public static String rutaPadre(String ruta) {
        int ultimo = ruta.lastIndexOf('/');
        return ultimo <= 0 ? "/" : ruta.substring(0, ultimo);
    }

    /** Último componente de la ruta. */
    public static String nombreDe(String ruta) {
        return ruta.substring(ruta.lastIndexOf('/') + 1);
    }

    private static void recorrer(NodoFS nodo, ListaEnlazada<NodoFS> salida, boolean soloArchivos) {
        ListaEnlazada<NodoFS> hs = nodo.hijosInternos();
        for (int i = 0; i < hs.tamano(); i++) {
            NodoFS h = hs.obtener(i);
            if (!soloArchivos || !h.esDirectorio()) {
                salida.agregarAlFinal(h);
            }
            recorrer(h, salida, soloArchivos);
        }
    }
}
