package com.registro.report;

import com.registro.config.AlmacenImagenesProducto;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Imagen subida de un producto: /reportes/imagenProducto?f={Producto_ID}_{version}.{ext}
 *
 * Sin consultas a la base de datos: el nombre ya viene de Productos.Imagen y solo se aceptan nombres de
 * archivo de la carpeta de imágenes (AlmacenImagenesProducto.archivo impide salir de ella).
 * Como cada subida genera un nombre nuevo, el contenido de una dirección nunca cambia: el navegador la
 * guarda en caché un año y, si la vuelve a pedir con su ETag, se responde 304 sin leer el archivo.
 * Está bajo /reportes/*, protegido por AutenticacionFilter.
 */
@WebServlet("/reportes/imagenProducto")
public class ImagenProductoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String nombre = req.getParameter("f");
        Path archivo = AlmacenImagenesProducto.archivo(nombre);
        if (archivo == null || !Files.isRegularFile(archivo)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String etag = "\"" + archivo.getFileName() + "\"";
        resp.setHeader("Cache-Control", "private, max-age=31536000, immutable");
        resp.setHeader("ETag", etag);
        if (etag.equals(req.getHeader("If-None-Match"))) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        resp.setContentType(tipo(archivo.getFileName().toString()));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentLengthLong(Files.size(archivo));
        Files.copy(archivo, resp.getOutputStream());
    }

    private static String tipo(String nombre) {
        String n = nombre.toLowerCase(Locale.ROOT);
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
