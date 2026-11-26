package com.registro.config;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Imagen del producto (columna Productos.Imagen, varchar(150)). Admite dos formas:
 *
 *  - URL externa: Imagen guarda la dirección completa (http:// o https://) y el navegador la carga de ahí.
 *  - Imagen subida: el archivo se guarda en una carpeta del servidor y Imagen guarda solo su NOMBRE
 *    ({Producto_ID}_{version}.{ext}). ImagenProductoServlet la entrega en /reportes/imagenProducto?f=NOMBRE.
 *
 * El nombre cambia en cada subida (la versión es la hora en base 36), así la dirección de cada imagen nunca
 * cambia de contenido: el navegador la guarda en caché un año y no vuelve a pedirla, y el listado no tiene
 * que revisar el disco para armar la dirección.
 *
 * Carpeta: clave gym.productos.dir (variable GYM_PRODUCTOS_DIR o gimnasio-bd.properties);
 * si no está, {catalina.base}/fotos-productos o {carpeta del usuario}/XportFitnessGym/fotos-productos.
 */
public final class AlmacenImagenesProducto {

    public static final long TAMANO_MAXIMO = 2L * 1024 * 1024;   // 2 MB
    /** Productos.Imagen es varchar(150). */
    public static final int LARGO_MAXIMO = 150;
    /** Dirección del servlet que entrega las imágenes subidas. */
    public static final String RUTA_SERVLET = "/reportes/imagenProducto";

    /** Nombre de un archivo guardado aquí: solo letras, números, "_" y "-", con extensión de imagen. */
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,100}\\.(jpg|png|gif|bmp)");

    private AlmacenImagenesProducto() {}

    /** Error de validación que se muestra al usuario tal cual. */
    public static class ImagenInvalidaException extends Exception {
        private static final long serialVersionUID = 1L;
        public ImagenInvalidaException(String mensaje) { super(mensaje); }
    }

    public static Path carpeta() {
        String dir = ConexionBD.valor("GYM_PRODUCTOS_DIR", "gym.productos.dir");
        if (dir != null) return Path.of(dir);
        String catalina = System.getProperty("catalina.base");
        if (catalina != null && !catalina.isBlank()) return Path.of(catalina, "fotos-productos");
        return Path.of(System.getProperty("user.home"), "XportFitnessGym", "fotos-productos");
    }

    // ------------------------------------------------------------------ tipos de valor

    public static boolean esUrlExterna(String imagen) {
        String t = imagen == null ? "" : imagen.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("http://") || t.startsWith("https://");
    }

    /** true si es el nombre de un archivo guardado en la carpeta (no una ruta ni una URL). */
    public static boolean esNombreArchivo(String imagen) {
        return imagen != null && NOMBRE_VALIDO.matcher(imagen.trim()).matches();
    }

    /**
     * Valida una URL externa: http/https, con servidor, sin espacios ni comillas y de hasta 150 caracteres.
     * Devuelve la URL limpia.
     */
    public static String validarUrl(String url) throws ImagenInvalidaException {
        String t = url == null ? "" : url.trim();
        if (t.length() > LARGO_MAXIMO) {
            throw new ImagenInvalidaException("La URL de la imagen admite como máximo " + LARGO_MAXIMO + " caracteres.");
        }
        if (!esUrlExterna(t) || t.matches(".*[\\s\"'<>].*")) {
            throw new ImagenInvalidaException("La URL de la imagen debe empezar con http:// o https:// y no llevar espacios.");
        }
        try {
            URI u = URI.create(t);
            if (u.getHost() == null || u.getHost().isBlank()) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new ImagenInvalidaException("La URL de la imagen no es válida.");
        }
        return t;
    }

    /**
     * Dirección para mostrar la imagen en la página ("" si no tiene o el valor no es válido).
     * No toca el disco ni la base de datos.
     */
    public static String url(String contextPath, String imagen) {
        String t = imagen == null ? "" : imagen.trim();
        if (esUrlExterna(t)) return t;
        if (esNombreArchivo(t)) {
            return (contextPath == null ? "" : contextPath) + RUTA_SERVLET + "?f=" + URLEncoder.encode(t, StandardCharsets.UTF_8);
        }
        return "";   // vacío, o un valor antiguo (ruta del WinForm) que el servidor no puede entregar
    }

    /** Archivo de la carpeta para ese nombre, o null si el nombre no es válido (evita salir de la carpeta). */
    public static Path archivo(String nombre) {
        if (!esNombreArchivo(nombre)) return null;
        Path dir = carpeta().toAbsolutePath().normalize();
        Path f = dir.resolve(nombre.trim()).normalize();
        return f.getParent() != null && f.getParent().equals(dir) ? f : null;
    }

    // ------------------------------------------------------------------ guardar y limpiar

    /**
     * Valida que sea una imagen JPG/PNG/GIF/BMP de hasta 2 MB (por su contenido, no solo por el nombre)
     * y la guarda como {productoId}_{version}.{ext}. Devuelve el nombre que se graba en Productos.Imagen.
     * La imagen anterior se borra después, con {@link #borrarAnteriores}, solo si el producto se grabó.
     */
    public static String guardar(Part archivo, String productoId) throws ImagenInvalidaException, IOException {
        if (archivo == null || archivo.getSize() == 0) throw new ImagenInvalidaException("Seleccione una imagen.");
        if (archivo.getSize() > TAMANO_MAXIMO) throw new ImagenInvalidaException("La imagen no debe pesar más de 2 MB.");
        String id = productoId == null ? "" : productoId.trim();
        if (!id.matches("[A-Za-z0-9]{1,20}")) {
            throw new ImagenInvalidaException("El producto no tiene un código válido para guardar la imagen.");
        }
        String ext;
        try (InputStream in = archivo.getInputStream()) {
            ext = AlmacenFotos.extensionPorContenido(in.readNBytes(8));
        }
        if (ext == null) throw new ImagenInvalidaException("El archivo no es una imagen válida (JPG, PNG, GIF o BMP).");

        String nombre = id + "_" + Long.toString(System.currentTimeMillis(), 36) + "." + ext;
        Path dir = carpeta().toAbsolutePath().normalize();
        Files.createDirectories(dir);
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        }
        return nombre;
    }

    /** Borra las imágenes de ese producto salvo la que queda en uso ("" = ninguna). Los errores no detienen nada. */
    public static void borrarAnteriores(String productoId, String enUso) {
        String id = productoId == null ? "" : productoId.trim();
        if (!id.matches("[A-Za-z0-9]{1,20}")) return;
        Path dir = carpeta().toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, id + "_*.{jpg,png,gif,bmp}")) {
            for (Path f : ds) {
                if (!f.getFileName().toString().equals(enUso)) Files.deleteIfExists(f);
            }
        } catch (IOException ex) {
            System.err.println("No se pudieron borrar imágenes anteriores del producto " + id + ": " + ex.getMessage());
        }
    }

    /** Borra un archivo recién subido cuando el producto no se pudo grabar. */
    public static void borrar(String nombre) {
        Path f = archivo(nombre);
        if (f == null) return;
        try {
            Files.deleteIfExists(f);
        } catch (IOException ex) {
            System.err.println("No se pudo borrar la imagen " + nombre + ": " + ex.getMessage());
        }
    }
}
