package com.registro.report;

import com.registro.facade.ClienteFACADE;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

/**
 * Huellas de los socios activos para el lector ZK9500.
 *
 * La página de asistencia las pasa al agente local de recepción (agente-huella), que hace la
 * identificación 1:N con el SDK ZKFinger. Respuesta en texto: una línea "Cliente_ID<TAB>plantilla Base64".
 *   GET /biometria/huellas            -> lista
 *   GET /biometria/huellas?version=1  -> solo la versión (para no descargar la lista si no cambió)
 * Está bajo /biometria/*, protegido por AutenticacionFilter (solo usuarios con sesión).
 */
@WebServlet("/biometria/huellas")
public class HuellasSociosServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ClienteFACADE facade = new ClienteFACADE();
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        String version = facade.VersionHuellasActivas();
        resp.setHeader("X-Huellas-Version", version);
        PrintWriter out = resp.getWriter();
        if (req.getParameter("version") != null) {
            out.print(version);
            return;
        }
        Base64.Encoder b64 = Base64.getEncoder();
        for (Object[] fila : facade.ListaHuellasActivas()) {
            String id = (String) fila[0];
            if (id.indexOf('\t') >= 0 || id.indexOf('\n') >= 0) continue;
            out.print(id);
            out.print('\t');
            out.print(b64.encodeToString((byte[]) fila[1]));
            out.print('\n');
        }
    }
}
