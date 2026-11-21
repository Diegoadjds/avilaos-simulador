package com.mycompany.avilaos;

import com.mycompany.avilaos.hardware.Disco;
import com.mycompany.avilaos.modelo.EspecificacionProceso;
import com.mycompany.avilaos.modelo.PCB;
import com.mycompany.avilaos.modelo.SolicitudDisco;
import com.mycompany.avilaos.modelo.TipoOperacionES;
import com.mycompany.avilaos.persistencia.EscenarioArchivos;
import com.mycompany.avilaos.persistencia.EscenarioProcesos;
import com.mycompany.avilaos.persistencia.RepositorioArchivos;
import com.mycompany.avilaos.persistencia.RepositorioProcesos;
import com.mycompany.avilaos.sistema.ConfiguracionSistema;
import com.mycompany.avilaos.sistema.SistemaAvilaOS;
import com.mycompany.avilaos.so.FabricaProcesos;
import com.mycompany.avilaos.so.PlanificadorDisco;
import com.mycompany.avilaos.so.RegistroEventos;
import com.mycompany.avilaos.so.disco.PoliticaPlanificacionDisco;
import java.nio.file.Path;

/**
 * Pruebas de verificación de ÁvilaOS (sin librerías externas). Se ejecutan con:
 *   java -cp <clases> com.mycompany.avilaos.PruebasAvilaOS <carpeta de escenarios>
 * Comprueban los números del anexo del enunciado, los casos de prueba P1-P4 con los JSON de ejemplo y el
 * comportamiento de la suspensión, los permisos y la persistencia.
 */
public class PruebasAvilaOS {

    private static int fallos = 0;
    private static int aciertos = 0;

    static void verificar(boolean condicion, String descripcion) {
        if (condicion) {
            aciertos++;
            System.out.println("  OK   " + descripcion);
        } else {
            fallos++;
            System.out.println("  FALLA " + descripcion);
        }
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Path.of(args.length > 0 ? args[0] : "escenarios");
        System.out.println("== Anexo del enunciado (disco aislado, velocidad 1) ==");
        anexo();
        System.out.println("== Casos P1-P4 con los JSON de ejemplo ==");
        casosEnunciado(carpeta);
        System.out.println("== Persistencia ==");
        persistencia(carpeta);
        System.out.println("== CRUD por procesos de E/S ==");
        crud(carpeta);
        System.out.println("== Modo usuario ==");
        modoUsuario(carpeta);
        System.out.println("== Directorios de un usuario y JSON tal como lo pide el enunciado ==");
        directorioDeUsuario(carpeta);
        jsonSinCamposOpcionales(carpeta);
        System.out.println("== Suspensión de procesos (mediano plazo) ==");
        suspension(carpeta);
        System.out.println("== Deadline con operaciones pendientes ==");
        deadline(carpeta);
        System.out.println();
        System.out.println(aciertos + " verificaciones correctas, " + fallos + " fallidas");
        System.exit(fallos == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ anexo

    private static void anexo() {
        int[] cola = {82, 17, 43, 140, 24, 16, 190};
        Object[][] esperado = {
            {PoliticaPlanificacionDisco.FIFO, "82,17,43,140,24,16,190", 518L},
            {PoliticaPlanificacionDisco.SSTF, "43,24,17,16,82,140,190", 208L},
            {PoliticaPlanificacionDisco.SCAN, "82,140,190,43,24,17,16", 332L},
            {PoliticaPlanificacionDisco.C_SCAN, "82,140,190,16,17,24,43", 192L},
        };
        for (Object[] fila : esperado) {
            PoliticaPlanificacionDisco pol = (PoliticaPlanificacionDisco) fila[0];
            Resultado r = correrDisco(pol, 50, cola, 1);
            verificar(r.orden.equals(fila[1]), pol + ": orden " + r.orden);
            verificar(r.desplazamiento == (Long) fila[2], pol + ": desplazamiento " + r.desplazamiento + " (esperado " + fila[2] + ")");
        }
    }

    private record Resultado(String orden, long desplazamiento) {
    }

    private static Resultado correrDisco(PoliticaPlanificacionDisco pol, int cabezal, int[] cola, int velocidad) {
        RegistroEventos registro = new RegistroEventos();
        registro.setImprimirEnConsola(false);
        Disco disco = new Disco(200, 100, cabezal, velocidad);
        PlanificadorDisco plan = new PlanificadorDisco(pol, 200, 300, registro);
        plan.setVentanaAgrupacion(0);
        disco.setControlador(plan);
        for (int b : cola) {
            PCB p = FabricaProcesos.crear(EspecificacionProceso.cpuBound("x" + b, 5, 1, 1, 100));
            plan.encolar(new SolicitudDisco(p, TipoOperacionES.LEER, new int[]{b}, null), 0);
        }
        StringBuilder orden = new StringBuilder();
        int atendidas = 0;
        for (long c = 1; c < 100_000 && atendidas < cola.length; c++) {
            disco.alCiclo(c);
            for (SolicitudDisco s : plan.sacarCompletadas(c + 1)) {
                orden.append(orden.length() == 0 ? "" : ",").append(s.getBloqueObjetivo());
                atendidas++;
            }
        }
        return new Resultado(orden.toString(), disco.getDesplazamientoTotal());
    }

    // ------------------------------------------------------------------ casos P1-P4

    private static void casosEnunciado(Path carpeta) throws Exception {
        String[][] casos = {
            {"fifo", "95,180,34,119,11,123,62,64"},
            {"sstf", "62,64,34,11,95,119,123,180"},
            {"scan", "62,64,95,119,123,180,34,11"},
            {"cscan", "62,64,95,119,123,180,11,34"},
        };
        for (String[] caso : casos) {
            SistemaAvilaOS sistema = construir(carpeta.resolve("fs-enunciado.json"),
                    carpeta.resolve("procesos-enunciado-" + caso[0] + ".json"));
            sistema.getRegistro().setImprimirEnConsola(false);
            sistema.iniciarPausado();
            for (int i = 0; i < 700 && !terminaron(sistema); i++) {
                sistema.avanzarUnCiclo();
            }
            String orden = ordenAtencion(sistema);
            verificar(orden.equals(caso[1]), "caso " + caso[0] + ": orden " + orden);
            verificar(terminaron(sistema), "caso " + caso[0] + ": todos los procesos terminaron (ciclo "
                    + sistema.getReloj().getCicloActual() + ")");
            sistema.detener();
        }
    }

    private static SistemaAvilaOS construir(Path fs, Path procesos) throws Exception {
        EscenarioArchivos ea = RepositorioArchivos.cargar(fs);
        EscenarioProcesos ep = RepositorioProcesos.cargar(procesos);
        ConfiguracionSistema cfg = new ConfiguracionSistema();
        ea.aplicarEn(cfg);
        ep.aplicarEn(cfg);
        SistemaAvilaOS s = new SistemaAvilaOS(cfg);
        s.cargarArchivos(ea);
        s.cargarProcesos(ep);
        return s;
    }

    private static boolean terminaron(SistemaAvilaOS s) {
        return s.getComputador().getKernel().getTerminados().length == s.getComputador().getKernel().getTodosLosProcesos().length
                && s.getComputador().getKernel().getTerminados().length > 0;
    }

    private static String ordenAtencion(SistemaAvilaOS s) {
        StringBuilder sb = new StringBuilder();
        for (String linea : s.getRegistro().getLineas()) {
            int i = linea.indexOf("El disco atiende la solicitud de ");
            if (i >= 0) {
                int j = linea.indexOf("en el bloque ", i) + "en el bloque ".length();
                int k = linea.indexOf(' ', j);
                sb.append(sb.length() == 0 ? "" : ",").append(linea, j, k);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ persistencia

    private static void persistencia(Path carpeta) throws Exception {
        EscenarioArchivos original = RepositorioArchivos.cargar(carpeta.resolve("fs-enunciado.json"));
        String json = RepositorioArchivos.aJson(original);
        EscenarioArchivos ida = RepositorioArchivos.desdeJson(json);
        verificar(ida.getEntradas().length == original.getEntradas().length, "FS: ida y vuelta conserva las entradas");
        verificar(RepositorioArchivos.aJson(ida).equals(json), "FS: guardar -> cargar -> guardar es idéntico");
        boolean rechazo = false;
        try {
            RepositorioArchivos.desdeJson("{\"diskConfig\":{\"totalBlocks\":50,\"initialHead\":0},\"fileSystem\":[]}");
        } catch (Exception e) {
            rechazo = true;
        }
        verificar(rechazo, "FS: rechaza un disco de 50 bloques (mínimo 100)");
    }

    // ------------------------------------------------------------------ helpers

    private static SistemaAvilaOS sistemaVacio(Path carpeta, String jsonProcesos) throws Exception {
        EscenarioArchivos ea = RepositorioArchivos.cargar(carpeta.resolve("fs-enunciado.json"));
        EscenarioProcesos ep = RepositorioProcesos.desdeJson(jsonProcesos);
        ConfiguracionSistema cfg = new ConfiguracionSistema();
        ea.aplicarEn(cfg);
        ep.aplicarEn(cfg);
        SistemaAvilaOS s = new SistemaAvilaOS(cfg);
        s.getRegistro().setImprimirEnConsola(false);
        s.cargarArchivos(ea);
        s.cargarProcesos(ep);
        return s;
    }

    private static final String SIN_PROCESOS = "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,"
            + "\"initialDiskPolicy\":\"SSTF\"},\"computer\":{\"ramCapacity\":1024,\"policy\":\"FCFS\",\"quantum\":0},"
            + "\"processes\":[]}";

    private static void correr(SistemaAvilaOS s, int ciclos) {
        if (!s.getReloj().isCorriendo()) {
            s.iniciarPausado();
        }
        for (int i = 0; i < ciclos; i++) {
            s.avanzarUnCiclo();
        }
    }

    private static boolean todosTerminados(SistemaAvilaOS s) {
        PCB[] todos = s.getComputador().getKernel().getTodosLosProcesos();
        return todos.length > 0 && s.getComputador().getKernel().getTerminados().length == todos.length;
    }

    private static void crud(Path carpeta) throws Exception {
        SistemaAvilaOS s = sistemaVacio(carpeta, SIN_PROCESOS);
        var fs = s.getArchivos();
        var mapa = s.getDisco().getMapa();
        int libresAntes = mapa.getLibres(false);
        s.crearProcesoArchivo("crear-a", TipoOperacionES.CREAR, "/sistema/nuevo.dat", 3, null,
                com.mycompany.avilaos.modelo.Permiso.PUBLICO, 16, 5, 1000, 3);
        s.crearProcesoArchivo("crear-dir", TipoOperacionES.CREAR, "/sistema/logs", 0, null,
                com.mycompany.avilaos.modelo.Permiso.PUBLICO, 16, 5, 1000, 3);
        correr(s, 120);
        var nuevo = fs.buscar("/sistema/nuevo.dat");
        verificar(nuevo != null && nuevo.getBloques() == 3, "CREATE: aparece /sistema/nuevo.dat con 3 bloques");
        verificar(nuevo != null && !nuevo.isEnCreacion(), "CREATE: queda confirmado cuando el disco termina");
        verificar(mapa.getLibres(false) == libresAntes - 3, "CREATE: ocupa 3 bloques del disco");
        verificar(nuevo != null && fs.bloquesDe(nuevo).length == 3, "CREATE: los bloques están encadenados (3)");
        var dir = fs.buscar("/sistema/logs");
        verificar(dir != null && dir.esDirectorio(), "CREATE con 0 bloques crea un directorio");
        s.crearProcesoArchivo("renombrar", TipoOperacionES.ACTUALIZAR, "/sistema/nuevo.dat", 0, "viejo.dat",
                com.mycompany.avilaos.modelo.Permiso.PUBLICO, 16, 5, 1000, 3);
        s.crearProcesoArchivo("grande", TipoOperacionES.CREAR, "/sistema/enorme.bin", 400, null,
                com.mycompany.avilaos.modelo.Permiso.PUBLICO, 16, 5, 1000, 3);
        correr(s, 120);
        verificar(fs.buscar("/sistema/viejo.dat") != null && fs.buscar("/sistema/nuevo.dat") == null,
                "UPDATE: cambia el nombre del archivo");
        verificar(fs.buscar("/sistema/enorme.bin") == null, "CREATE sin bloques libres suficientes se rechaza");
        boolean huboError = false;
        for (PCB p : s.getComputador().getKernel().getTerminados()) {
            if (p.getNombre().equals("grande") && p.getErrorES() != null) {
                huboError = true;
            }
        }
        verificar(huboError, "el proceso rechazado termina con error de E/S y la simulación sigue");
        int libresAntesBorrar = mapa.getLibres(false);
        s.crearProcesoArchivo("borrar-dir", TipoOperacionES.ELIMINAR, "/usuarios", 0, null,
                com.mycompany.avilaos.modelo.Permiso.PUBLICO, 16, 5, 1000, 3);
        correr(s, 200);
        verificar(fs.buscar("/usuarios") == null && fs.buscar("/usuarios/notas.txt") == null,
                "DELETE de un directorio elimina también sus archivos");
        verificar(mapa.getLibres(false) == libresAntesBorrar + 4, "DELETE libera los 4 bloques de los archivos de /usuarios");
        verificar(todosTerminados(s), "todos los procesos de E/S terminaron");
        s.detener();
    }

    private static void modoUsuario(Path carpeta) throws Exception {
        SistemaAvilaOS s = sistemaVacio(carpeta, SIN_PROCESOS);
        s.getSesion().entrarComoUsuario("user1");
        verificar(intenta(s, TipoOperacionES.LEER, "/usuarios/notas.txt", null), "user1 lee su archivo privado");
        verificar(intenta(s, TipoOperacionES.LEER, "/sistema/boot.bin", null), "user1 lee un archivo público");
        verificar(intenta(s, TipoOperacionES.LEER, "/usuarios/data.csv", null), "user1 lee un archivo público de user2");
        verificar(!intenta(s, TipoOperacionES.ELIMINAR, "/usuarios/data.csv", null), "user1 NO elimina el archivo de user2");
        verificar(!intenta(s, TipoOperacionES.ACTUALIZAR, "/sistema/boot.bin", null), "user1 NO modifica archivos del sistema");
        verificar(!intenta(s, TipoOperacionES.ACTUALIZAR, "/usuarios/notas.txt", "otro.txt"), "user1 NO cambia nombres");
        verificar(!intenta(s, TipoOperacionES.CREAR, "/usuarios/mio.txt", null), "user1 NO crea en un directorio ajeno");
        verificar(intenta(s, TipoOperacionES.ELIMINAR, "/usuarios/img1.png", null), "user1 elimina su propio archivo");
        s.getSesion().entrarComoUsuario("user2");
        verificar(!intenta(s, TipoOperacionES.LEER, "/usuarios/notas.txt", null), "user2 NO lee el archivo privado de user1");
        boolean bloqueado = false;
        try {
            s.cambiarPoliticaDisco(PoliticaPlanificacionDisco.SCAN);
        } catch (Exception e) {
            bloqueado = true;
        }
        verificar(bloqueado, "en modo usuario no se cambia la política de disco");
        s.getSesion().entrarComoAdministrador();
        verificar(intenta(s, TipoOperacionES.ACTUALIZAR, "/usuarios/notas.txt", "otro.txt"), "el administrador sí cambia nombres");
        s.detener();
    }

    private static boolean intenta(SistemaAvilaOS s, TipoOperacionES op, String ruta, String nuevoNombre) {
        try {
            s.crearProcesoArchivo("p-" + op, op, ruta, 1, nuevoNombre, com.mycompany.avilaos.modelo.Permiso.PRIVADO, 16, 5, 1000, 3);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void directorioDeUsuario(Path carpeta) throws Exception {
        SistemaAvilaOS s = sistemaVacio(carpeta, SIN_PROCESOS);
        boolean ok = false;
        try {
            s.crearProcesoArchivo("dir", TipoOperacionES.CREAR, "/usuarios/user1", 0, null,
                    com.mycompany.avilaos.modelo.Permiso.PRIVADO, "user1", 8, 5, 1000, 3);
            ok = true;
        } catch (Exception e) {
            ok = false;
        }
        verificar(ok, "el administrador crea un directorio con dueño user1");
        correr(s, 40);
        verificar(s.getArchivos().buscar("/usuarios/user1") != null
                && "user1".equals(s.getArchivos().buscar("/usuarios/user1").getDueno()), "el directorio quedó a nombre de user1");
        s.getSesion().entrarComoUsuario("user1");
        verificar(intenta(s, TipoOperacionES.CREAR, "/usuarios/user1/informe.txt", null), "user1 crea un archivo en su directorio");
        s.getSesion().entrarComoUsuario("user2");
        verificar(!intenta(s, TipoOperacionES.CREAR, "/usuarios/user1/otro.txt", null), "user2 NO crea en el directorio de user1");
        s.getSesion().entrarComoUsuario("user1");
        boolean ajeno = true;
        try {
            s.crearProcesoArchivo("dir2", TipoOperacionES.CREAR, "/usuarios/user1/sub", 0, null,
                    com.mycompany.avilaos.modelo.Permiso.PRIVADO, "user2", 8, 5, 1000, 3);
        } catch (Exception e) {
            ajeno = false;
        }
        verificar(!ajeno, "user1 NO crea elementos a nombre de otro usuario");
        s.detener();
    }

    /** El JSON del PDF no trae fileSizeBlocks, priority, user ni newName: tienen que ser opcionales. */
    private static void jsonSinCamposOpcionales(Path carpeta) throws Exception {
        String json = "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,\"initialDiskPolicy\":\"FIFO\"},"
                + "\"computer\":{\"ramCapacity\":1024,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":["
                + "{\"name\":\"Proc_95\",\"memory\":16,\"deadline\":500,\"instructions\":2,\"targetPath\":\"/sistema/config.sys\",\"operation\":\"READ\"},"
                + "{\"name\":\"Proc_34\",\"memory\":16,\"deadline\":500,\"instructions\":2,\"targetPath\":\"/usuarios/notas.txt\",\"operation\":\"UPDATE\"},"
                + "{\"name\":\"Proc_119\",\"memory\":16,\"deadline\":500,\"instructions\":2,\"targetPath\":\"/usuarios/img1.png\",\"operation\":\"DELETE\"}]}";
        SistemaAvilaOS s = sistemaVacio(carpeta, json);
        correr(s, 80);
        verificar(todosTerminados(s), "los procesos del formato del enunciado (sin campos opcionales) terminan");
        s.detener();
    }

    private static void suspension(Path carpeta) throws Exception {
        String json = "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,\"initialDiskPolicy\":\"SSTF\"},"
                + "\"computer\":{\"ramCapacity\":48,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":["
                + "{\"name\":\"io1\",\"type\":\"IO_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":6,\"priority\":2,\"ioEvery\":1,\"ioDuration\":60},"
                + "{\"name\":\"io2\",\"type\":\"IO_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":6,\"priority\":9,\"ioEvery\":1,\"ioDuration\":60},"
                + "{\"name\":\"io3\",\"type\":\"IO_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":6,\"priority\":4,\"ioEvery\":1,\"ioDuration\":60},"
                + "{\"name\":\"nuevo\",\"type\":\"CPU_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":8,\"priority\":1}]}";
        SistemaAvilaOS s = sistemaVacio(carpeta, json);
        correr(s, 600);
        var m = s.getMetricasDisco();
        verificar(m.getSuspensiones() >= 1, "se suspendió al menos un proceso (" + m.getSuspensiones() + ")");
        verificar(m.getReactivaciones() >= 1, "se reactivó al menos un proceso (" + m.getReactivaciones() + ")");
        verificar(m.getSuspensiones() == m.getReactivaciones(), "cada suspendido volvió a la RAM");
        boolean victimaCorrecta = false;
        for (String l : s.getRegistro().getLineas()) {
            if (l.contains("Se suspende al proceso P1 para admitir a P4")) {
                victimaCorrecta = true;
            }
        }
        verificar(victimaCorrecta, "el planificador a mediano plazo suspende a un bloqueado (el único en ese ciclo: io1) para admitir a 'nuevo'");
        verificar(todosTerminados(s), "todos los procesos terminaron");
        verificar(s.getDisco().getMapa().getLibres(true) == 100, "el área de swap quedó totalmente libre");
        verificar(s.getComputador().getRam().getUsada() == 0, "la RAM quedó libre");
        boolean sinDeadline = true;
        for (PCB p : s.getComputador().getKernel().getTerminados()) {
            sinDeadline &= !p.isTerminoPorDeadline();
        }
        verificar(sinDeadline, "ninguno murió por deadline");
        s.detener();
    }

    private static void deadline(Path carpeta) throws Exception {
        String json = "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,\"initialDiskPolicy\":\"FIFO\"},"
                + "\"computer\":{\"ramCapacity\":32,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":["
                + "{\"name\":\"io1\",\"type\":\"IO_BOUND\",\"memory\":16,\"deadline\":40,\"instructions\":6,\"priority\":9,\"ioEvery\":1,\"ioDuration\":500},"
                + "{\"name\":\"io2\",\"type\":\"IO_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":6,\"priority\":8,\"ioEvery\":1,\"ioDuration\":500},"
                + "{\"name\":\"nuevo\",\"type\":\"CPU_BOUND\",\"memory\":16,\"deadline\":3000,\"instructions\":4,\"priority\":1},"
                + "{\"name\":\"lector\",\"memory\":8,\"deadline\":12,\"instructions\":2,\"targetPath\":\"/sistema/boot.bin\",\"operation\":\"READ\",\"fileSizeBlocks\":0}"
                + "]}";
        SistemaAvilaOS s = sistemaVacio(carpeta, json);
        correr(s, 120);
        var mapa = s.getDisco().getMapa();
        boolean algunoVencio = false;
        for (PCB p : s.getComputador().getKernel().getTerminados()) {
            algunoVencio |= p.isTerminoPorDeadline();
        }
        verificar(algunoVencio, "algún proceso murió por deadline");
        var nodo = s.getArchivos().buscar("/sistema/boot.bin");
        verificar(nodo != null && !nodo.estaOcupado(), "un READ cancelado por deadline no deja el archivo marcado como ocupado");
        verificar(s.getDisco().getMapa().getLibres(true) + swapOcupado(s) == 100, "los bloques de swap son consistentes");
        s.detener();
    }

    private static int swapOcupado(SistemaAvilaOS s) {
        int n = 0;
        for (PCB p : s.getComputador().getKernel().getBloqueadosSuspendidos()) {
            n += p.getMemoria();
        }
        for (PCB p : s.getComputador().getKernel().getListosSuspendidos()) {
            n += p.getMemoria();
        }
        for (PCB p : s.getComputador().getKernel().getBloqueados()) {
            if (p.isSuspendiendo()) {
                n += p.getMemoria();
            }
        }
        return n;
    }
}
