package com.registro.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Consulta de DNI (Reniec) y RUC (Sunat) con la API de apisperu.com, igual que frm_ManteClientes2
 * de SisdataGimnasio (ConsultarDni / ConsultarRuc).
 *
 * El token se lee de la configuración (no se guarda en el código):
 *   variable de entorno GYM_APISPERU_TOKEN, o clave gym.apisperu.token en gimnasio-bd.properties.
 */
public final class ServicioConsultaDocumento {

    private static final String URL_DNI = "https://dniruc.apisperu.com/api/v1/dni/";
    private static final String URL_RUC = "https://dniruc.apisperu.com/api/v1/ruc/";
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private ServicioConsultaDocumento() {
    }

    /** Datos devueltos por la consulta (solo los campos que usa el formulario). */
    public static final class Resultado {
        public String nombreCompleto;   // DNI: "ApellidoPaterno ApellidoMaterno Nombres"; RUC: razón social
        public String direccion;        // RUC
        public String estado;           // RUC: ACTIVO, BAJA...
        public String condicion;        // RUC: HABIDO, NO HABIDO...
    }

    public static boolean estaConfigurado() {
        return token() != null;
    }

    /** DNI de 8 dígitos; null si no existe o el servicio no responde. */
    public static Resultado consultarDni(String dni) {
        String json = consultar(URL_DNI, dni);
        if (json == null || !"true".equalsIgnoreCase(campo(json, "success"))) return null;
        String nombres = unir(campo(json, "apellidoPaterno"), campo(json, "apellidoMaterno"), campo(json, "nombres"));
        if (nombres.isEmpty()) return null;
        Resultado r = new Resultado();
        r.nombreCompleto = nombres;
        return r;
    }

    /** RUC de 11 dígitos; null si no existe o el servicio no responde. */
    public static Resultado consultarRuc(String ruc) {
        String json = consultar(URL_RUC, ruc);
        String razon = json == null ? null : campo(json, "razonSocial");
        if (razon == null || razon.isBlank()) return null;
        Resultado r = new Resultado();
        r.nombreCompleto = razon.trim();
        r.direccion = limpio(campo(json, "direccion"));
        r.estado = limpio(campo(json, "estado"));
        r.condicion = limpio(campo(json, "condicion"));
        return r;
    }

    private static String token() {
        return ConexionBD.valor("GYM_APISPERU_TOKEN", "gym.apisperu.token");
    }

    private static String consultar(String base, String numero) {
        String token = token();
        if (token == null || numero == null || !numero.trim().matches("\\d+")) return null;
        try {
            URI uri = URI.create(base + numero.trim() + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = CLIENTE.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return resp.statusCode() == 200 ? resp.body() : null;
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Valor de un campo de primer nivel del JSON ("clave": "texto" | true | false | número | null). */
    static String campo(String json, String clave) {
        String buscado = "\"" + clave + "\"";
        int k = json.indexOf(buscado);
        if (k < 0) return null;
        int i = json.indexOf(':', k + buscado.length());
        if (i < 0) return null;
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        if (json.charAt(i) != '"') {
            int fin = i;
            while (fin < json.length() && ",}] \r\n\t".indexOf(json.charAt(fin)) < 0) fin++;
            String v = json.substring(i, fin);
            return "null".equals(v) ? null : v;
        }
        StringBuilder sb = new StringBuilder();
        for (int j = i + 1; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '"') return sb.toString();
            if (c == '\\' && j + 1 < json.length()) {
                char e = json.charAt(++j);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (j + 4 < json.length()) {
                            sb.append((char) Integer.parseInt(json.substring(j + 1, j + 5), 16));
                            j += 4;
                        }
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String limpio(String v) {
        return v == null || v.isBlank() || v.trim().equals("-") ? "" : v.trim();
    }
}
