package com.registro.report;

import com.registro.dao.ClienteDAO;
import com.registro.dto.ClienteDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Foto del socio para el control de asistencia (frm_RegistrarAsistencia muestra la foto guardada en Clientes.Foto).
 * Solo sirve la ruta registrada en la BD para ese Cliente_ID y solo si es una imagen.
 * Está bajo /reportes/*, protegido por AutenticacionFilter.
 */
@WebServlet("/reportes/fotoSocio")
public class FotoSocioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        if (id == null || id.isBlank()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ClienteDTO cliente = new ClienteDAO().ListaClienteXCod(id.trim());
        String ruta = cliente != null && cliente.getFoto() != null ? cliente.getFoto().trim() : "";
        if (ruta.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path archivo = Path.of(ruta);
        String tipo = tipoImagen(archivo);
        if (tipo == null || !Files.isRegularFile(archivo)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType(tipo);
        resp.setHeader("Cache-Control", "private, max-age=300");
        resp.setContentLengthLong(Files.size(archivo));
        Files.copy(archivo, resp.getOutputStream());
    }

    private static String tipoImagen(Path archivo) {
        String n = archivo.getFileName() == null ? "" : archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".bmp")) return "image/bmp";
        return null;
    }
}
