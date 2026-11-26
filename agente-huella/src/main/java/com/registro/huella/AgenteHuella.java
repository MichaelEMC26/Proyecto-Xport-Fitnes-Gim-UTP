package com.registro.huella;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agente local del lector de huellas ZK9500.
 *
 * El navegador no puede usar un lector USB directamente, así que este programa corre en la PC de recepción
 * y atiende solo en 127.0.0.1. Las páginas de la aplicación web (socios.xhtml y RegistrarAsistencia.xhtml)
 * lo llaman con fetch():
 *
 *   GET  /estado                          ¿hay lector?, versión de la base cargada
 *   POST /capturar?timeout=&soltar=&imagen=   una lectura (plantilla en Base64 y, si se pide, imagen PNG)
 *   POST /fusionar     cuerpo: 3 plantillas, una por línea      -> plantilla de registro
 *   POST /cargar?version=   cuerpo: "codigo<TAB>plantilla" por línea  -> base 1:N en memoria
 *   POST /identificar?timeout=            espera un dedo y busca al socio en la base cargada
 *   POST /cancelar                        corta la lectura en curso
 *
 * Solo acepta páginas de los orígenes configurados en agente-huella.properties (clave "origenes").
 */
public final class AgenteHuella {

    static final String VERSION = "1.0.0";

    private final LectorZK9500 lector = new LectorZK9500();
    /** Cada lectura nueva incrementa este número; la anterior ve que cambió y se detiene. */
    private final AtomicLong lecturaActual = new AtomicLong();
    private final Set<String> origenes;
    private final boolean cualquierOrigen;
    private final int puerto;

    private AgenteHuella(int puerto, Set<String> origenes) {
        this.puerto = puerto;
        this.origenes = origenes;
        this.cualquierOrigen = origenes.contains("*");
    }

    public static void main(String[] args) throws IOException {
        Properties cfg = cargarConfiguracion();
        int puerto = Integer.parseInt(cfg.getProperty("puerto", "8095").trim());
        Set<String> origenes = new LinkedHashSet<>();
        for (String o : cfg.getProperty("origenes", "http://localhost:8080,http://127.0.0.1:8080").split(",")) {
            String t = o.trim();
            while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
            if (!t.isEmpty()) origenes.add(t.toLowerCase());
        }
        new AgenteHuella(puerto, origenes).iniciar();
    }

    private static Properties cargarConfiguracion() {
        Properties p = new Properties();
        List<Path> rutas = new ArrayList<>();
        String propia = System.getProperty("agente.config");
        if (propia != null) rutas.add(Path.of(propia));
        rutas.add(Path.of("agente-huella.properties"));
        try {
            Path jar = Path.of(AgenteHuella.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            rutas.add(jar.resolveSibling("agente-huella.properties"));
        } catch (Exception ignorada) {
            // sin ruta del .jar: solo la carpeta actual
        }
        for (Path r : rutas) {
            if (Files.isRegularFile(r)) {
                try (InputStream in = Files.newInputStream(r)) {
                    p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                    System.out.println("[agente] Configuración: " + r.toAbsolutePath());
                    return p;
                } catch (IOException ex) {
                    System.err.println("[agente] No se pudo leer " + r + ": " + ex.getMessage());
                }
            }
        }
        System.out.println("[agente] Sin agente-huella.properties: se usan valores por defecto.");
        return p;
    }

    private void iniciar() throws IOException {
        HttpServer srv = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 0);
        srv.createContext("/", this::atender);
        srv.setExecutor(Executors.newFixedThreadPool(4));
        srv.start();
        Runtime.getRuntime().addShutdownHook(new Thread(lector::cerrar));
        System.out.println("[agente] Agente de huella ZK9500 v" + VERSION + " en http://127.0.0.1:" + puerto);
        System.out.println("[agente] Orígenes permitidos: " + origenes);
        System.out.println("[agente] Lector " + (lector.abrir() ? "conectado." : "NO detectado (se reintenta en cada petición)."));
    }

    // ---------------------------------------------------------
    // Peticiones
    // ---------------------------------------------------------

    private void atender(HttpExchange ex) throws IOException {
        try {
            // Solo nombres locales en Host (evita que otra página use DNS rebinding para llegar aquí)
            String host = ex.getRequestHeaders().getFirst("Host");
            if (host == null || !(host.startsWith("127.0.0.1") || host.startsWith("localhost") || host.startsWith("[::1]"))) {
                responder(ex, 403, error("PROHIBIDO", "Host no permitido."));
                return;
            }
            String origen = ex.getRequestHeaders().getFirst("Origin");
            if (origen != null && !origenPermitido(origen)) {
                System.err.println("[agente] Origen rechazado: " + origen
                        + " (agréguelo a 'origenes' en agente-huella.properties)");
                // Con la cabecera CORS la página puede leer el motivo (si no, solo vería "no se pudo conectar").
                // No da acceso: la respuesta es solo este error.
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", origen);
                ex.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
                responder(ex, 403, error("ORIGEN", "Esta página (" + origen + ") no está autorizada para usar el lector."
                        + " Agréguela en 'origenes' de agente-huella.properties y reinicie el agente."));
                return;
            }
            if (origen != null) {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", origen);
                ex.getResponseHeaders().set("Vary", "Origin");
            }
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                // Chrome/Edge: permiso para que una página pública llame a la PC local
                ex.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
                ex.getResponseHeaders().set("Access-Control-Max-Age", "600");
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String ruta = ex.getRequestURI().getPath();
            Map<String, String> q = parametros(ex.getRequestURI());
            boolean post = "POST".equals(ex.getRequestMethod());
            switch (ruta) {
                case "/estado" -> estado(ex);
                case "/capturar" -> { if (post) capturar(ex, q); else noPermitido(ex); }
                case "/fusionar" -> { if (post) fusionar(ex); else noPermitido(ex); }
                case "/cargar" -> { if (post) cargar(ex, q); else noPermitido(ex); }
                case "/identificar" -> { if (post) identificar(ex, q); else noPermitido(ex); }
                case "/cancelar" -> {
                    lecturaActual.incrementAndGet();
                    responder(ex, 200, "{\"ok\":true}");
                }
                default -> responder(ex, 404, error("NO_EXISTE", "Ruta no encontrada."));
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            responder(ex, 500, error("ERROR", "Error interno del agente: " + e.getMessage()));
        } finally {
            ex.close();
        }
    }

    private boolean origenPermitido(String origen) {
        if (cualquierOrigen) return true;
        String o = origen.trim().toLowerCase();
        while (o.endsWith("/")) o = o.substring(0, o.length() - 1);
        return origenes.contains(o);
    }

    private void estado(HttpExchange ex) throws IOException {
        boolean conectado = lector.conectado();
        responder(ex, 200, "{\"ok\":true,\"agente\":\"" + VERSION + "\",\"conectado\":" + conectado
                + ",\"version\":" + json(lector.versionCargada())
                + ",\"huellas\":" + lector.cantidadCargada()
                + ",\"mensaje\":" + json(conectado ? "Lector ZK9500 listo." : lector.motivo()) + "}");
    }

    private void capturar(HttpExchange ex, Map<String, String> q) throws IOException {
        long mia = lecturaActual.incrementAndGet();
        try {
            LectorZK9500.Captura c = lector.capturar(tiempo(q, 15000), "1".equals(q.get("soltar")),
                    () -> lecturaActual.get() != mia);
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"plantilla\":\"")
                    .append(Base64.getEncoder().encodeToString(c.plantilla())).append('"');
            if ("1".equals(q.get("imagen"))) {
                byte[] png = c.png();
                if (png != null) {
                    sb.append(",\"imagen\":\"data:image/png;base64,")
                      .append(Base64.getEncoder().encodeToString(png)).append('"');
                }
            }
            responder(ex, 200, sb.append('}').toString());
        } catch (LectorZK9500.LectorException e) {
            responder(ex, 200, error(e.codigo, e.getMessage()));
        }
    }

    private void fusionar(HttpExchange ex) throws IOException {
        List<byte[]> p = new ArrayList<>();
        for (String linea : leerCuerpo(ex).split("\\r?\\n")) {
            if (!linea.isBlank()) p.add(base64(linea));
        }
        if (p.size() != 3 || p.contains(null)) {
            responder(ex, 400, error("DATOS", "Se necesitan 3 lecturas."));
            return;
        }
        try {
            byte[] reg = lector.fusionar(p.get(0), p.get(1), p.get(2));
            responder(ex, 200, "{\"ok\":true,\"plantilla\":\"" + Base64.getEncoder().encodeToString(reg) + "\"}");
        } catch (LectorZK9500.LectorException e) {
            responder(ex, 200, error(e.codigo, e.getMessage()));
        }
    }

    private void cargar(HttpExchange ex, Map<String, String> q) throws IOException {
        List<String> codigos = new ArrayList<>();
        List<byte[]> plantillas = new ArrayList<>();
        for (String linea : leerCuerpo(ex).split("\\r?\\n")) {
            int tab = linea.indexOf('\t');
            if (tab <= 0) continue;
            codigos.add(linea.substring(0, tab).trim());
            plantillas.add(base64(linea.substring(tab + 1)));
        }
        try {
            int[] r = lector.cargar(codigos, plantillas, q.getOrDefault("version", ""));
            System.out.println("[agente] Base de huellas cargada: " + r[0] + " correctas, " + r[1] + " rechazadas.");
            responder(ex, 200, "{\"ok\":true,\"cargadas\":" + r[0] + ",\"rechazadas\":" + r[1] + "}");
        } catch (LectorZK9500.LectorException e) {
            responder(ex, 200, error(e.codigo, e.getMessage()));
        }
    }

    private void identificar(HttpExchange ex, Map<String, String> q) throws IOException {
        long mia = lecturaActual.incrementAndGet();
        try {
            LectorZK9500.Captura c = lector.capturar(tiempo(q, 20000), "1".equals(q.get("soltar")),
                    () -> lecturaActual.get() != mia);
            LectorZK9500.Identificacion id = lector.identificar(c.plantilla());
            if (id.encontrado()) {
                System.out.println("[agente] Huella identificada: socio " + id.codigo() + " (puntaje " + id.puntaje() + ")");
                responder(ex, 200, "{\"ok\":true,\"encontrado\":true,\"codigo\":" + json(id.codigo())
                        + ",\"puntaje\":" + id.puntaje() + "}");
            } else {
                responder(ex, 200, "{\"ok\":true,\"encontrado\":false,\"mensaje\":\"Huella no registrada.\"}");
            }
        } catch (LectorZK9500.LectorException e) {
            responder(ex, 200, error(e.codigo, e.getMessage()));
        }
    }

    // ---------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------

    private static void noPermitido(HttpExchange ex) throws IOException {
        responder(ex, 405, error("METODO", "Use POST."));
    }

    private static long tiempo(Map<String, String> q, long porDefecto) {
        try {
            long t = Long.parseLong(q.getOrDefault("timeout", String.valueOf(porDefecto)));
            return Math.max(1000, Math.min(t, 60000));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static byte[] base64(String s) {
        try {
            return Base64.getDecoder().decode(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, String> parametros(URI uri) {
        Map<String, String> m = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null) return m;
        for (String par : raw.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i);
            String v = i < 0 ? "" : par.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    private static String leerCuerpo(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] b = in.readNBytes(32 * 1024 * 1024);
            return new String(b, StandardCharsets.UTF_8);
        }
    }

    private static String error(String codigo, String mensaje) {
        return "{\"ok\":false,\"codigo\":" + json(codigo) + ",\"mensaje\":" + json(mensaje) + "}";
    }

    private static String json(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    private static void responder(HttpExchange ex, int estado, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(estado, b.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(b);
        }
    }
}
