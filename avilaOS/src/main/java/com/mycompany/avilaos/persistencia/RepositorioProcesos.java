package com.mycompany.avilaos.persistencia;

import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.ModoUsuario;
import com.mycompany.avilaos.modelo.Permiso;
import com.mycompany.avilaos.modelo.TipoOperacionES;
import com.mycompany.avilaos.modelo.TipoProceso;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import com.mycompany.avilaos.so.planificacion.PoliticaPlanificacion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda y carga el JSON de procesos y configuración (formato del enunciado: globalConfig + computer + processes).
 * Campos opcionales que agrega ÁvilaOS: priority, user, permissions, newName, type (FILE, CPU_BOUND o IO_BOUND),
 * ioEvery, ioDuration, instructions y diskBatchWindow.
 */
public final class RepositorioProcesos {

    public static final int PRIORIDAD_POR_DEFECTO = 5;

    private RepositorioProcesos() {
    }

    public static void guardar(Path ruta, EscenarioProcesos escenario) throws ErrorEscenario {
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

    public static EscenarioProcesos cargar(Path ruta) throws ErrorEscenario {
        return desdeJson(Lector.leerArchivo(ruta));
    }

    public static String aJson(EscenarioProcesos e) {
        JsonNodo raiz = JsonNodo.objeto();
        JsonNodo global = JsonNodo.objeto()
                .poner("cycleDurationMs", e.getDuracionCicloMs())
                .poner("swapSizeBlocks", e.getBloquesSwap())
                .poner("initialDiskPolicy", e.getPoliticaDisco().name());
        if (e.getVentanaAgrupacion() != EscenarioProcesos.VENTANA_NO_DEFINIDA) {
            global.poner("diskBatchWindow", e.getVentanaAgrupacion());
        }
        raiz.poner("globalConfig", global);
        raiz.poner("computer", JsonNodo.objeto()
                .poner("ramCapacity", e.getRam())
                .poner("policy", e.getPoliticaCpu().name())
                .poner("quantum", e.getQuantum()));
        JsonNodo lista = JsonNodo.arreglo();
        for (EspecificacionProceso p : e.getProcesos()) {
            JsonNodo o = JsonNodo.objeto();
            o.poner("name", p.nombre());
            if (p.tipo() != TipoProceso.ES_ARCHIVO) {
                o.poner("type", p.tipo().name());
            }
            o.poner("memory", p.memoria());
            o.poner("deadline", p.deadline());
            o.poner("instructions", p.instrucciones());
            o.poner("priority", p.prioridad());
            if (p.tipo() == TipoProceso.ES_ARCHIVO) {
                o.poner("targetPath", p.ruta());
                o.poner("operation", p.operacion().getCodigoJson());
                o.poner("fileSizeBlocks", p.tamanoBloques());
                if (p.nuevoNombre() != null) {
                    o.poner("newName", p.nuevoNombre());
                }
                o.poner("permissions", p.permiso().getCodigoJson());
                if (p.dueno() != null) {
                    o.poner("owner", p.dueno());
                }
            } else if (p.tipo() == TipoProceso.IO_BOUND) {
                o.poner("ioEvery", p.ciclosEntreEs());
                o.poner("ioDuration", p.duracionEs());
            }
            o.poner("user", p.usuario());
            lista.agregar(o);
        }
        raiz.poner("processes", lista);
        return raiz.aTexto();
    }

    public static EscenarioProcesos desdeJson(String texto) throws ErrorEscenario {
        JsonNodo raiz = AnalizadorJson.analizar(texto);
        if (raiz.getTipo() != JsonNodo.Tipo.OBJETO) {
            throw new ErrorEscenario("El archivo debe contener un objeto JSON { ... } con globalConfig, computer y processes");
        }
        JsonNodo global = Lector.objeto(raiz, "globalConfig", "globalConfig");
        int ciclo = Lector.entero(global, "cycleDurationMs", "globalConfig.cycleDurationMs");
        int swap = Lector.entero(global, "swapSizeBlocks", "globalConfig.swapSizeBlocks");
        String textoDisco = Lector.cadena(global, "initialDiskPolicy", "globalConfig.initialDiskPolicy");
        PoliticaPlanificacionDisco politicaDisco = PoliticaPlanificacionDisco.desdeTexto(textoDisco);
        if (politicaDisco == null) {
            throw new ErrorEscenario("globalConfig.initialDiskPolicy: \"" + textoDisco + "\" no es una política de disco válida (use "
                    + nombres(PoliticaPlanificacionDisco.values()) + ")");
        }
        int ventana = global.contiene("diskBatchWindow")
                ? Lector.entero(global, "diskBatchWindow", "globalConfig.diskBatchWindow")
                : EscenarioProcesos.VENTANA_NO_DEFINIDA;

        JsonNodo comp = Lector.objeto(raiz, "computer", "computer");
        int ram = Lector.entero(comp, "ramCapacity", "computer.ramCapacity");
        String textoCpu = Lector.cadena(comp, "policy", "computer.policy");
        PoliticaPlanificacion politicaCpu = PoliticaPlanificacion.desdeTexto(textoCpu);
        if (politicaCpu == null) {
            throw new ErrorEscenario("computer.policy: \"" + textoCpu + "\" no es una política de CPU válida (use "
                    + nombres(PoliticaPlanificacion.values()) + ")");
        }
        int quantum = comp.contiene("quantum") ? Lector.entero(comp, "quantum", "computer.quantum") : 0;

        JsonNodo lista = Lector.lista(raiz, "processes", "processes");
        EspecificacionProceso[] procesos = new EspecificacionProceso[lista.tamano()];
        for (int i = 0; i < procesos.length; i++) {
            JsonNodo o = lista.obtener(i);
            String donde = "processes[" + (i + 1) + "]";
            if (o.getTipo() != JsonNodo.Tipo.OBJETO) {
                throw new ErrorEscenario(donde + ": debe ser un objeto { } y es " + o.describirTipo());
            }
            procesos[i] = proceso(o, donde);
        }
        EscenarioProcesos escenario = new EscenarioProcesos(ciclo, swap, politicaDisco, ram, politicaCpu, quantum,
                ventana, procesos);
        String error = escenario.validar();
        if (error != null) {
            throw new ErrorEscenario("Procesos inválidos: " + error);
        }
        return escenario;
    }

    private static EspecificacionProceso proceso(JsonNodo o, String donde) throws ErrorEscenario {
        String nombre = Lector.cadena(o, "name", donde + ".name");
        int memoria = Lector.entero(o, "memory", donde + ".memory");
        int deadline = Lector.entero(o, "deadline", donde + ".deadline");
        int instrucciones = Lector.entero(o, "instructions", donde + ".instructions");
        int prioridad = o.contiene("priority") ? Lector.entero(o, "priority", donde + ".priority") : PRIORIDAD_POR_DEFECTO;
        String usuario = o.contiene("user") ? Lector.cadena(o, "user", donde + ".user") : ModoUsuario.NOMBRE_ADMIN;

        TipoProceso tipo = TipoProceso.ES_ARCHIVO;
        if (o.contiene("type")) {
            String t = Lector.cadena(o, "type", donde + ".type").trim().toUpperCase();
            switch (t) {
                case "FILE", "ES_ARCHIVO" -> tipo = TipoProceso.ES_ARCHIVO;
                case "CPU_BOUND" -> tipo = TipoProceso.CPU_BOUND;
                case "IO_BOUND" -> tipo = TipoProceso.IO_BOUND;
                default -> throw new ErrorEscenario(donde + ".type: \"" + t + "\" no es válido (use FILE, CPU_BOUND o IO_BOUND)");
            }
        }
        if (tipo == TipoProceso.CPU_BOUND) {
            return EspecificacionProceso.cpuBound(nombre, instrucciones, memoria, prioridad, deadline);
        }
        if (tipo == TipoProceso.IO_BOUND) {
            return EspecificacionProceso.ioBound(nombre, instrucciones, memoria, prioridad, deadline,
                    Lector.entero(o, "ioEvery", donde + ".ioEvery"), Lector.entero(o, "ioDuration", donde + ".ioDuration"));
        }
        String ruta = Lector.cadena(o, "targetPath", donde + ".targetPath");
        String textoOp = Lector.cadena(o, "operation", donde + ".operation");
        TipoOperacionES operacion = TipoOperacionES.desdeTexto(textoOp);
        if (operacion == null || !operacion.esCrud()) {
            throw new ErrorEscenario(donde + ".operation: \"" + textoOp + "\" no es válida (use CREATE, READ, UPDATE o DELETE)");
        }
        int tamano = 0;
        for (String clave : new String[]{"fileSizeBlocks", "sizeBlocks", "fileSize", "blocks", "size"}) {
            if (o.contiene(clave)) {
                tamano = Lector.entero(o, clave, donde + "." + clave);
                break;
            }
        }
        if (operacion == TipoOperacionES.CREAR && tamano == 0 && !o.contiene("fileSizeBlocks")) {
            throw new ErrorEscenario(donde + ": la operación CREATE necesita fileSizeBlocks (tamaño en bloques; 0 crea un directorio)");
        }
        String nuevoNombre = o.contiene("newName") ? Lector.cadena(o, "newName", donde + ".newName") : null;
        String dueno = o.contiene("owner") ? Lector.cadena(o, "owner", donde + ".owner").trim() : null;
        Permiso permiso = ModoUsuario.NOMBRE_ADMIN.equals(usuario) ? Permiso.PUBLICO : Permiso.PRIVADO;
        if (o.contiene("permissions")) {
            String t = Lector.cadena(o, "permissions", donde + ".permissions");
            permiso = Permiso.desdeTexto(t);
            if (permiso == null) {
                throw new ErrorEscenario(donde + ".permissions: \"" + t + "\" no es válido (use public o private)");
            }
        }
        return EspecificacionProceso.archivo(nombre, instrucciones, memoria, prioridad, deadline, operacion, ruta,
                tamano, nuevoNombre, usuario, permiso, dueno);
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
