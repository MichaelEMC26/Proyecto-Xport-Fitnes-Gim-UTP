package com.registro.config;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Guarda la foto del socio en una carpeta del servidor y devuelve la ruta que se graba en Cliente.Foto
 * (la misma que lee FotoSocioServlet para mostrarla en Clientes y en Registrar Asistencia).
 *
 * Carpeta: clave gym.fotos.dir (variable GYM_FOTOS_DIR o gimnasio-bd.properties);
 * si no está, {catalina.base}/fotos-socios o {carpeta del usuario}/XportFitnessGym/fotos-socios.
 */
public final class AlmacenFotos {

    public static final long TAMANO_MAXIMO = 2L * 1024 * 1024;   // 2 MB
    /** Cliente.Foto es varchar(100). */
    private static final int LARGO_RUTA = 100;

    private AlmacenFotos() {}

    /** Error de validación que se muestra al usuario tal cual. */
    public static class FotoInvalidaException extends Exception {
        private static final long serialVersionUID = 1L;
        public FotoInvalidaException(String mensaje) { super(mensaje); }
    }

    public static Path carpeta() {
        String dir = ConexionBD.valor("GYM_FOTOS_DIR", "gym.fotos.dir");
        if (dir != null) return Path.of(dir);
        String catalina = System.getProperty("catalina.base");
        if (catalina != null && !catalina.isBlank()) return Path.of(catalina, "fotos-socios");
        return Path.of(System.getProperty("user.home"), "XportFitnessGym", "fotos-socios");
    }

    /**
     * Valida que sea una imagen JPG/PNG/GIF/BMP de hasta 2 MB (por su contenido, no solo por el nombre)
     * y la guarda como {clienteId}.{ext}, reemplazando la foto anterior del socio.
     */
    public static String guardar(Part archivo, String clienteId) throws FotoInvalidaException, IOException {
        if (archivo == null || archivo.getSize() == 0) throw new FotoInvalidaException("Seleccione una imagen.");
        if (archivo.getSize() > TAMANO_MAXIMO) throw new FotoInvalidaException("La foto no debe pesar más de 2 MB.");
        String id = clienteId == null ? "" : clienteId.trim();
        if (!id.matches("[A-Za-z0-9]+")) throw new FotoInvalidaException("El socio no tiene un código válido para guardar la foto.");

        String ext;
        try (InputStream in = archivo.getInputStream()) {
            ext = extensionPorContenido(in.readNBytes(8));
        }
        if (ext == null) throw new FotoInvalidaException("El archivo no es una imagen válida (JPG, PNG, GIF o BMP).");

        Path dir = carpeta().toAbsolutePath().normalize();
        Path destino = dir.resolve(id + "." + ext);
        String ruta = destino.toString();
        if (ruta.length() > LARGO_RUTA) {
            throw new FotoInvalidaException("La carpeta de fotos tiene una ruta muy larga (máx. " + LARGO_RUTA
                    + " caracteres). Configure gym.fotos.dir con una ruta más corta.");
        }

        Files.createDirectories(dir);
        // Quita la foto anterior con otra extensión (p. ej. antes .png y ahora .jpg)
        for (String otra : new String[]{"jpg", "png", "gif", "bmp"}) {
            if (!otra.equals(ext)) Files.deleteIfExists(dir.resolve(id + "." + otra));
        }
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return ruta;
    }

    /** Firma de los primeros bytes del archivo (también la usa AlmacenImagenesProducto). */
    static String extensionPorContenido(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 3 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "gif";
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') return "bmp";
        return null;
    }
}
