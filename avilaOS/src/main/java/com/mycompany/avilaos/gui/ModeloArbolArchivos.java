package com.mycompany.avilaos.gui;

import com.mycompany.avilaos.estructuras.ListaEnlazada;
import com.mycompany.avilaos.so.NodoFS;
import com.mycompany.avilaos.so.SistemaArchivos;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Modelo del JTree: expone directamente el árbol del sistema de archivos (sin copiarlo a otra estructura). Los hijos
 * se piden al {@link SistemaArchivos}, que los entrega bajo su semáforo.
 */
public class ModeloArbolArchivos implements TreeModel {

    private final SistemaArchivos archivos;
    private final ListaEnlazada<TreeModelListener> oyentes = new ListaEnlazada<>();

    public ModeloArbolArchivos(SistemaArchivos archivos) {
        this.archivos = archivos;
    }

    @Override
    public Object getRoot() { return archivos.getRaiz(); }

    @Override
    public Object getChild(Object padre, int indice) {
        NodoFS[] hijos = archivos.hijos((NodoFS) padre);
        return indice >= 0 && indice < hijos.length ? hijos[indice] : null;
    }

    @Override
    public int getChildCount(Object padre) { return archivos.hijos((NodoFS) padre).length; }

    @Override
    public boolean isLeaf(Object nodo) { return !((NodoFS) nodo).esDirectorio(); }

    @Override
    public void valueForPathChanged(TreePath ruta, Object nuevoValor) {
        // El árbol no se edita desde la interfaz: los cambios los hacen los procesos de E/S.
    }

    @Override
    public int getIndexOfChild(Object padre, Object hijo) {
        NodoFS[] hijos = archivos.hijos((NodoFS) padre);
        for (int i = 0; i < hijos.length; i++) {
            if (hijos[i] == hijo) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener oyente) { oyentes.agregarAlFinal(oyente); }

    @Override
    public void removeTreeModelListener(TreeModelListener oyente) { oyentes.eliminar(oyente); }

    /** Avisa que la estructura cambió (la interfaz lo llama cuando la versión del sistema de archivos cambia). */
    public void recargar() {
        TreeModelEvent e = new TreeModelEvent(this, new Object[]{getRoot()});
        for (TreeModelListener o : oyentes.aArreglo(new TreeModelListener[0])) {
            o.treeStructureChanged(e);
        }
    }
}
