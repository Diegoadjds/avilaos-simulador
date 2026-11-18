package com.mycompany.avilaos.so;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.PropietarioBloques;
import com.mycompany.avilaos.modelo.TipoNodo;

/**
 * Un directorio o archivo del sistema de archivos jerárquico (el árbol que muestra el JTree).
 * Un archivo guarda su primer bloque y cuántos bloques ocupa; el resto de la cadena vive en el mapa de bloques.
 * Los hijos solo se tocan con el semáforo del {@link SistemaArchivos}.
 */
public class NodoFS implements PropietarioBloques {

    private volatile String nombre;
    private final TipoNodo tipo;
    private final String dueno;
    private volatile Permiso permiso;
    private final NodoFS padre;
    private final ListaEnlazada<NodoFS> hijos = new ListaEnlazada<>();
    private volatile int bloques;
    private volatile int primerBloque = -1;
    private volatile String creadoPor;
    private final int colorId;

    // Estado de uso (lo cambia el SistemaArchivos con su semáforo tomado)
    private volatile int lecturasEnCurso;
    private volatile boolean escribiendo;
    private volatile boolean enCreacion;

    NodoFS(String nombre, TipoNodo tipo, String dueno, Permiso permiso, NodoFS padre, String creadoPor, int colorId) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.dueno = dueno;
        this.permiso = permiso;
        this.padre = padre;
        this.creadoPor = creadoPor;
        this.colorId = colorId;
    }

    /** Ruta completa, por ejemplo /sistema/boot.bin ("/" para la raíz). */
    public String getRuta() {
        if (padre == null) {
            return "/";
        }
        String rutaPadre = padre.getRuta();
        return (rutaPadre.equals("/") ? "" : rutaPadre) + "/" + nombre;
    }

    public boolean esDirectorio() { return tipo == TipoNodo.DIRECTORIO; }

    public boolean estaOcupado() { return escribiendo || enCreacion || lecturasEnCurso > 0; }

    @Override
    public String getEtiquetaBloques() { return getRuta(); }

    @Override
    public int getColorId() { return colorId; }

    public String getNombre() { return nombre; }
    public TipoNodo getTipo() { return tipo; }
    public String getDueno() { return dueno; }
    public Permiso getPermiso() { return permiso; }
    public NodoFS getPadre() { return padre; }
    public int getBloques() { return bloques; }
    public int getPrimerBloque() { return primerBloque; }
    public String getCreadoPor() { return creadoPor; }
    public int getLecturasEnCurso() { return lecturasEnCurso; }
    public boolean isEscribiendo() { return escribiendo; }
    public boolean isEnCreacion() { return enCreacion; }

    // ---- Solo para el SistemaArchivos ----
    ListaEnlazada<NodoFS> hijosInternos() { return hijos; }
    void setNombre(String nombre) { this.nombre = nombre; }
    void setPermiso(Permiso permiso) { this.permiso = permiso; }
    void setBloques(int bloques, int primerBloque) { this.bloques = bloques; this.primerBloque = primerBloque; }
    void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
    void setLecturasEnCurso(int n) { this.lecturasEnCurso = n; }
    void setEscribiendo(boolean valor) { this.escribiendo = valor; }
    void setEnCreacion(boolean valor) { this.enCreacion = valor; }

    @Override
    public String toString() {
        return padre == null ? "/" : nombre;
    }
}
