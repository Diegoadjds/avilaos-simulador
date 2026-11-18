package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.ModoUsuario;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.TipoNodo;
import com.mycompany.avilaos.so.NodoFS;
import com.mycompany.avilaos.so.SistemaArchivos;
import com.mycompany.avilaos.sistema.ConfiguracionSistema;

/**
 * El contenido del JSON del sistema de archivos: configuración del disco (tamaño, cabezal inicial) y el árbol de
 * directorios y archivos con sus bloques, dueños y permisos. Son solo datos validados; quien los aplica es el sistema.
 */
public final class EscenarioArchivos {

    /** Un directorio o archivo. {@code cadena} (opcional) guarda los bloques exactos; si es null se encadena desde primerBloque. */
    public record Entrada(String ruta, TipoNodo tipo, int primerBloque, int bloques, int[] cadena, String dueno,
            Permiso permiso, String creadoPor) {
    }

    public static final int VELOCIDAD_NO_DEFINIDA = 0;

    private final int totalBloques;
    private final int cabezalInicial;
    private final int velocidadCabezal;
    private final Entrada[] entradas;

    public EscenarioArchivos(int totalBloques, int cabezalInicial, int velocidadCabezal, Entrada[] entradas) {
        this.totalBloques = totalBloques;
        this.cabezalInicial = cabezalInicial;
        this.velocidadCabezal = velocidadCabezal;
        this.entradas = entradas.clone();
    }

    public int getTotalBloques() { return totalBloques; }
    public int getCabezalInicial() { return cabezalInicial; }
    /** Velocidad del cabezal si el JSON la trae, o {@link #VELOCIDAD_NO_DEFINIDA}. */
    public int getVelocidadCabezal() { return velocidadCabezal; }
    public Entrada[] getEntradas() { return entradas.clone(); }

    /** Copia al ConfiguracionSistema lo que este archivo define del disco. */
    public void aplicarEn(ConfiguracionSistema cfg) {
        cfg.setBloquesDisco(totalBloques);
        cfg.setCabezalInicial(cabezalInicial);
        if (velocidadCabezal != VELOCIDAD_NO_DEFINIDA) {
            cfg.setVelocidadCabezal(velocidadCabezal);
        }
    }

    /** Devuelve el primer problema del contenido, o null si es válido. */
    public String validar() {
        if (totalBloques < ConfiguracionSistema.BLOQUES_MIN || totalBloques > ConfiguracionSistema.BLOQUES_MAX) {
            return "diskConfig.totalBlocks debe estar entre " + ConfiguracionSistema.BLOQUES_MIN + " y "
                    + ConfiguracionSistema.BLOQUES_MAX + " (el archivo dice " + totalBloques + ")";
        }
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques) {
            return "diskConfig.initialHead debe estar entre 0 y " + (totalBloques - 1) + " (el archivo dice "
                    + cabezalInicial + ")";
        }
        if (velocidadCabezal != VELOCIDAD_NO_DEFINIDA
                && (velocidadCabezal < 1 || velocidadCabezal > ConfiguracionSistema.VELOCIDAD_MAX)) {
            return "diskConfig.headSpeed debe estar entre 1 y " + ConfiguracionSistema.VELOCIDAD_MAX;
        }
        for (int i = 0; i < entradas.length; i++) {
            Entrada e = entradas[i];
            String donde = "fileSystem[" + (i + 1) + "] (" + e.ruta() + "): ";
            if (e.ruta() == null || !e.ruta().startsWith("/") || e.ruta().length() < 2 || e.ruta().endsWith("/")) {
                return donde + "la ruta debe ser absoluta, por ejemplo /sistema/boot.bin";
            }
            String errorNombre = SistemaArchivos.errorDeNombre(SistemaArchivos.nombreDe(e.ruta()));
            if (errorNombre != null) {
                return donde + errorNombre;
            }
            if (e.dueno() == null || e.dueno().isBlank()) {
                return donde + "falta el dueño (owner)";
            }
            if (e.tipo() == TipoNodo.ARCHIVO) {
                if (e.bloques() < 1 || e.bloques() > totalBloques) {
                    return donde + "blocks debe estar entre 1 y " + totalBloques;
                }
                if (e.cadena() == null && (e.primerBloque() < 0 || e.primerBloque() >= totalBloques)) {
                    return donde + "startBlock debe estar entre 0 y " + (totalBloques - 1);
                }
                if (e.cadena() != null && e.cadena().length != e.bloques()) {
                    return donde + "la lista chain debe tener exactamente " + e.bloques() + " bloques";
                }
            }
            for (int j = 0; j < i; j++) {
                if (entradas[j].ruta().equals(e.ruta())) {
                    return donde + "la ruta está repetida";
                }
            }
        }
        return null;
    }

    /** Foto del sistema de archivos actual (estructura, bloques exactos, dueños y permisos) y de la posición del cabezal. */
    public static EscenarioArchivos capturar(SistemaArchivos fs, int totalBloques, int cabezal, int velocidad) {
        NodoFS[] nodos = fs.preorden();
        Entrada[] entradas = new Entrada[nodos.length];
        for (int i = 0; i < nodos.length; i++) {
            NodoFS n = nodos[i];
            if (n.esDirectorio()) {
                entradas[i] = new Entrada(n.getRuta(), TipoNodo.DIRECTORIO, -1, 0, null, n.getDueno(), n.getPermiso(),
                        n.getCreadoPor());
            } else {
                entradas[i] = new Entrada(n.getRuta(), TipoNodo.ARCHIVO, n.getPrimerBloque(), n.getBloques(),
                        fs.bloquesDe(n), n.getDueno(), n.getPermiso(), n.getCreadoPor());
            }
        }
        return new EscenarioArchivos(totalBloques, cabezal, velocidad, entradas);
    }

    /** Dueño por defecto de lo que no lo declara. */
    public static String duenoPorDefecto() {
        return ModoUsuario.NOMBRE_ADMIN;
    }
}
