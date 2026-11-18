package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.modelo.ModoUsuario;

/**
 * Quién está usando ÁvilaOS ahora: el modo (administrador o usuario) y, en modo usuario, el nombre del usuario.
 * Concentra las reglas de qué puede hacer cada modo; la interfaz solo consulta estas respuestas.
 */
public class SesionUsuario {

    private volatile ModoUsuario modo = ModoUsuario.ADMINISTRADOR;
    private volatile String usuario = ModoUsuario.NOMBRE_ADMIN;

    public ModoUsuario getModo() { return modo; }

    /** Nombre con el que se crean los procesos: "admin" en modo administrador. */
    public String getUsuario() { return usuario; }

    public boolean esAdministrador() { return modo == ModoUsuario.ADMINISTRADOR; }

    public void entrarComoAdministrador() {
        this.modo = ModoUsuario.ADMINISTRADOR;
        this.usuario = ModoUsuario.NOMBRE_ADMIN;
    }

    /** @throws IllegalArgumentException si el nombre está vacío, es muy largo o es el del administrador */
    public void entrarComoUsuario(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (n.length() > 20 || n.indexOf('/') >= 0) {
            throw new IllegalArgumentException("El nombre de usuario admite máximo 20 caracteres y no puede tener /");
        }
        if (n.equalsIgnoreCase(ModoUsuario.NOMBRE_ADMIN)) {
            throw new IllegalArgumentException("\"" + ModoUsuario.NOMBRE_ADMIN + "\" es el administrador: use el modo Administrador");
        }
        this.modo = ModoUsuario.USUARIO;
        this.usuario = n;
    }

    /** Solo el administrador gestiona procesos generales, cambia políticas, ve las métricas y guarda/carga archivos JSON. */
    public String motivoSiNoPuedeAdministrar(String accion) {
        return esAdministrador() ? null : "Modo usuario: solo el administrador puede " + accion;
    }
}
