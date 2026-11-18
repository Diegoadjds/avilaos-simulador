package com.mycompany.avilaos.hardware;

import com.mycompany.avilaos.modelo.PropietarioBloques;
import java.util.concurrent.Semaphore;

/**
 * Mapa de bloques del disco con asignación ENCADENADA: cada bloque ocupado guarda cuál es el
 * siguiente bloque de su archivo (o -1 si es el último). El disco tiene dos zonas:
 * el área de datos (bloques 0 .. datos-1, para los archivos) y, a continuación, el área de
 * intercambio o swap (bloques datos .. datos+swap-1, para los procesos suspendidos).
 *
 * Todo el acceso pasa por un semáforo, porque lo usan el hilo del computador, el del disco y
 * la interfaz a la vez.
 */
public class MapaBloques {

    public static final int LIBRE = -2;
    public static final int FIN_DE_CADENA = -1;

    private final int bloquesDatos;
    private final int bloquesSwap;
    private final int[] siguiente;
    private final PropietarioBloques[] duenos;
    private final Semaphore mutex = new Semaphore(1);
    /** Cambia con cada modificación; la interfaz lo compara para saber si debe redibujar. */
    private volatile long version;

    public MapaBloques(int bloquesDatos, int bloquesSwap) {
        this.bloquesDatos = bloquesDatos;
        this.bloquesSwap = bloquesSwap;
        this.siguiente = new int[bloquesDatos + bloquesSwap];
        this.duenos = new PropietarioBloques[bloquesDatos + bloquesSwap];
        for (int i = 0; i < siguiente.length; i++) {
            siguiente[i] = LIBRE;
        }
    }

    /**
     * Reserva {@code cantidad} bloques libres (los de menor número) de una zona y los encadena.
     *
     * @param swap true para el área de intercambio, false para el área de datos
     * @return los bloques en el orden de la cadena, o null si no hay suficientes libres
     */
    public int[] reservar(int cantidad, boolean swap, PropietarioBloques dueno) {
        mutex.acquireUninterruptibly();
        try {
            int desde = swap ? bloquesDatos : 0;
            int hasta = swap ? bloquesDatos + bloquesSwap : bloquesDatos;
            return encadenar(cantidad, desde, desde, hasta, dueno);
        } finally {
            mutex.release();
        }
    }

    /**
     * Reserva una cadena que empieza exactamente en {@code inicio} (para cargar un disco desde el JSON);
     * el resto de los bloques son los libres siguientes (dando la vuelta al área de datos si hace falta).
     *
     * @return la cadena, o null si el bloque inicial no existe, no es del área de datos o está ocupado
     */
    public int[] reservarDesde(int inicio, int cantidad, PropietarioBloques dueno) {
        mutex.acquireUninterruptibly();
        try {
            if (inicio < 0 || inicio >= bloquesDatos || siguiente[inicio] != LIBRE) {
                return null;
            }
            return encadenar(cantidad, inicio, 0, bloquesDatos, dueno);
        } finally {
            mutex.release();
        }
    }

    /**
     * Reserva exactamente los bloques indicados, en ese orden (para restaurar un disco guardado).
     *
     * @return false si alguno no existe, está ocupado o se repite
     */
    public boolean reservarExactos(int[] bloques, PropietarioBloques dueno) {
        mutex.acquireUninterruptibly();
        try {
            for (int i = 0; i < bloques.length; i++) {
                int b = bloques[i];
                if (b < 0 || b >= bloquesDatos || siguiente[b] != LIBRE) {
                    return false;
                }
                for (int j = 0; j < i; j++) {
                    if (bloques[j] == b) {
                        return false;
                    }
                }
            }
            for (int i = 0; i < bloques.length; i++) {
                siguiente[bloques[i]] = i + 1 < bloques.length ? bloques[i + 1] : FIN_DE_CADENA;
                duenos[bloques[i]] = dueno;
            }
            version++;
            return true;
        } finally {
            mutex.release();
        }
    }

    /** Busca libres a partir de {@code primero} dentro de [desde, hasta), dando la vuelta; null si no alcanzan. */
    private int[] encadenar(int cantidad, int primero, int desde, int hasta, PropietarioBloques dueno) {
        if (cantidad < 1) {
            return null;
        }
        int[] elegidos = new int[cantidad];
        int n = 0;
        int rango = hasta - desde;
        for (int k = 0; k < rango && n < cantidad; k++) {
            int b = desde + ((primero - desde + k) % rango);
            if (siguiente[b] == LIBRE) {
                elegidos[n++] = b;
            }
        }
        if (n < cantidad) {
            return null;
        }
        for (int i = 0; i < cantidad; i++) {
            siguiente[elegidos[i]] = i + 1 < cantidad ? elegidos[i + 1] : FIN_DE_CADENA;
            duenos[elegidos[i]] = dueno;
        }
        version++;
        return elegidos;
    }

    /** Recorre la cadena desde su primer bloque y devuelve todos sus bloques en orden. */
    public int[] cadena(int primero) {
        mutex.acquireUninterruptibly();
        try {
            return recorrer(primero);
        } finally {
            mutex.release();
        }
    }

    private int[] recorrer(int primero) {
        int largo = 0;
        for (int b = primero; b >= 0 && b < siguiente.length && siguiente[b] != LIBRE && largo <= siguiente.length;
                b = siguiente[b]) {
            largo++;
            if (siguiente[b] == FIN_DE_CADENA) {
                break;
            }
        }
        int[] bloques = new int[largo];
        int b = primero;
        for (int i = 0; i < largo; i++) {
            bloques[i] = b;
            b = siguiente[b];
        }
        return bloques;
    }

    /** Libera todos los bloques de la cadena que empieza en {@code primero}. Devuelve cuántos liberó. */
    public int liberarCadena(int primero) {
        mutex.acquireUninterruptibly();
        try {
            int[] bloques = recorrer(primero);
            for (int b : bloques) {
                siguiente[b] = LIBRE;
                duenos[b] = null;
            }
            if (bloques.length > 0) {
                version++;
            }
            return bloques.length;
        } finally {
            mutex.release();
        }
    }

    public boolean estaLibre(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            return siguiente[bloque] == LIBRE;
        } finally {
            mutex.release();
        }
    }

    public int getSiguiente(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            return siguiente[bloque];
        } finally {
            mutex.release();
        }
    }

    public PropietarioBloques getDueno(int bloque) {
        mutex.acquireUninterruptibly();
        try {
            return duenos[bloque];
        } finally {
            mutex.release();
        }
    }

    /** Libres en el área de datos o en el área de intercambio. */
    public int getLibres(boolean swap) {
        mutex.acquireUninterruptibly();
        try {
            int desde = swap ? bloquesDatos : 0;
            int hasta = swap ? bloquesDatos + bloquesSwap : bloquesDatos;
            int libres = 0;
            for (int b = desde; b < hasta; b++) {
                if (siguiente[b] == LIBRE) {
                    libres++;
                }
            }
            return libres;
        } finally {
            mutex.release();
        }
    }

    /** Foto de quién ocupa cada bloque (null = libre), tomada de una vez para dibujarla sin interferir. */
    public PropietarioBloques[] fotoDuenos() {
        mutex.acquireUninterruptibly();
        try {
            return duenos.clone();
        } finally {
            mutex.release();
        }
    }

    /** Foto del bloque siguiente de cada bloque (LIBRE, FIN_DE_CADENA o el número del siguiente). */
    public int[] fotoSiguientes() {
        mutex.acquireUninterruptibly();
        try {
            return siguiente.clone();
        } finally {
            mutex.release();
        }
    }

    public int getBloquesDatos() { return bloquesDatos; }

    public int getBloquesSwap() { return bloquesSwap; }

    public int getTotal() { return bloquesDatos + bloquesSwap; }

    public long getVersion() { return version; }

    public boolean esSwap(int bloque) { return bloque >= bloquesDatos; }
}
