package com.mycompany.avilaos.sistema;

import com.mycompany.avilaos.persistencia.ErrorEscenario;
import com.mycompany.avilaos.persistencia.EscenarioArchivos;
import com.mycompany.avilaos.persistencia.EscenarioProcesos;

/**
 * Receta para armar una simulación: la configuración base y los dos JSON ya leídos (cualquiera puede faltar todavía).
 * Sirve para construir el sistema y también para REINICIARLO desde el ciclo 0 con exactamente lo mismo.
 */
public final class Montaje {

    private final ConfiguracionSistema configuracion;
    private final EscenarioArchivos archivos;
    private final EscenarioProcesos procesos;
    private final String nombreArchivos;
    private final String nombreProcesos;

    public Montaje(ConfiguracionSistema configuracion, EscenarioArchivos archivos, String nombreArchivos,
            EscenarioProcesos procesos, String nombreProcesos) {
        this.configuracion = configuracion.copia();
        this.archivos = archivos;
        this.procesos = procesos;
        this.nombreArchivos = nombreArchivos;
        this.nombreProcesos = nombreProcesos;
    }

    public ConfiguracionSistema getConfiguracion() { return configuracion.copia(); }
    public EscenarioArchivos getArchivos() { return archivos; }
    public EscenarioProcesos getProcesos() { return procesos; }
    public String getNombreArchivos() { return nombreArchivos; }
    public String getNombreProcesos() { return nombreProcesos; }

    public Montaje conArchivos(EscenarioArchivos nuevos, String nombre) {
        return new Montaje(configuracion, nuevos, nombre, procesos, nombreProcesos);
    }

    public Montaje conProcesos(EscenarioProcesos nuevos, String nombre) {
        return new Montaje(configuracion, archivos, nombreArchivos, nuevos, nombre);
    }

    public Montaje conConfiguracion(ConfiguracionSistema nueva) {
        return new Montaje(nueva, archivos, nombreArchivos, procesos, nombreProcesos);
    }

    /**
     * Construye un sistema nuevo (ciclo 0, sin iniciar). Lo que traen los JSON manda sobre la configuración base.
     *
     * @throws ErrorEscenario si un JSON no encaja con la configuración
     * @throws IllegalArgumentException si la configuración resultante no es válida
     */
    public SistemaAvilaOS construir() throws ErrorEscenario {
        ConfiguracionSistema cfg = configuracion.copia();
        if (archivos != null) {
            archivos.aplicarEn(cfg);
        }
        if (procesos != null) {
            procesos.aplicarEn(cfg);
        }
        SistemaAvilaOS sistema = new SistemaAvilaOS(cfg);
        if (archivos != null) {
            sistema.cargarArchivos(archivos);
        }
        if (procesos != null) {
            sistema.cargarProcesos(procesos);
        }
        return sistema;
    }
}
