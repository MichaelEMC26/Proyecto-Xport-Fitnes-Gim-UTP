package com.registro.huella;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

/**
 * Manejo del lector ZK9500 con libzkfp.dll.
 * Todas las llamadas a la DLL pasan por un mismo candado: el SDK no admite llamadas simultáneas.
 */
final class LectorZK9500 {

    private static final int TAM_PLANTILLA = 2048;
    private static final long PAUSA_MS = 100;

    private final Object candado = new Object();
    private boolean iniciado;
    private Pointer dispositivo;
    private Pointer bd;                 // base 1:N en memoria
    private int ancho, alto;

    /** fid (posición + 1) -> código de socio (Cliente_ID) de la base cargada. */
    private final List<String> codigos = new ArrayList<>();
    private String versionCargada = "";
    /** Por qué no se pudo abrir el lector la última vez (se muestra en la página). */
    private volatile String motivo = "No se detecta el lector de huellas ZK9500. Revise el cable USB.";

    static final class LectorException extends Exception {
        private static final long serialVersionUID = 1L;
        final String codigo;
        LectorException(String codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
    }

    record Captura(byte[] plantilla, byte[] imagenGris, int ancho, int alto) {
        byte[] png() {
            try {
                BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_GRAY);
                img.getRaster().setDataElements(0, 0, ancho, alto, imagenGris);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(img, "png", out);
                return out.toByteArray();
            } catch (IOException | RuntimeException ex) {
                return null;
            }
        }
    }

    record Identificacion(boolean encontrado, String codigo, int puntaje) {}

    // ---------------------------------------------------------
    // Conexión
    // ---------------------------------------------------------

    /** Abre el lector si no está abierto. Devuelve false si no hay lector conectado. */
    boolean abrir() {
        synchronized (candado) {
            if (dispositivo != null) return true;
            try {
                if (!iniciado) {
                    int r = ZKFinger.LIB.ZKFPM_Init();
                    if (r != ZKFinger.OK && r != ZKFinger.YA_INICIADO) {
                        motivo = "No se pudo iniciar el SDK ZKFinger (código " + r + "). Reinstale el driver del ZK9500.";
                        return false;
                    }
                    iniciado = true;
                }
                if (ZKFinger.LIB.ZKFPM_GetDeviceCount() <= 0) {
                    // El SDK solo detecta lectores enchufados al iniciar: se reinicia en el siguiente intento
                    ZKFinger.LIB.ZKFPM_Terminate();
                    iniciado = false;
                    motivo = "No se detecta el lector de huellas ZK9500. Revise el cable USB.";
                    return false;
                }
                Pointer h = ZKFinger.LIB.ZKFPM_OpenDevice(0);
                if (h == null || Pointer.nativeValue(h) == 0) {
                    // Hay lector pero no se puede abrir: casi siempre lo tiene otro programa
                    ZKFinger.LIB.ZKFPM_Terminate();
                    iniciado = false;
                    motivo = "El lector ZK9500 está siendo usado por otro programa (por ejemplo el sistema de"
                            + " escritorio SisdataGimnasio). Ciérrelo y vuelva a intentar.";
                    return false;
                }
                dispositivo = h;
                ancho = parametro(ZKFinger.PARAM_ANCHO);
                alto = parametro(ZKFinger.PARAM_ALTO);
                if (bd == null) {
                    bd = ZKFinger.LIB.ZKFPM_DBInit();
                    versionCargada = "";
                    codigos.clear();
                }
                System.out.println("[agente] Lector ZK9500 abierto (" + ancho + "x" + alto + ")");
                return true;
            } catch (UnsatisfiedLinkError ex) {
                System.err.println("[agente] No se encontró libzkfp.dll. Instale el driver/SDK ZKFinger del ZK9500. " + ex.getMessage());
                motivo = "No se encontró libzkfp.dll. Instale el driver/SDK ZKFinger del ZK9500.";
                return false;
            }
        }
    }

    /** Cierra el lector (se vuelve a abrir solo en la siguiente petición). */
    void cerrar() {
        synchronized (candado) {
            if (dispositivo != null) {
                ZKFinger.LIB.ZKFPM_CloseDevice(dispositivo);
                dispositivo = null;
            }
            if (bd != null) {
                ZKFinger.LIB.ZKFPM_DBFree(bd);
                bd = null;
            }
            codigos.clear();
            versionCargada = "";
            if (iniciado) {
                ZKFinger.LIB.ZKFPM_Terminate();
                iniciado = false;
            }
        }
    }

    boolean conectado() { return abrir(); }

    String motivo() { return motivo; }

    private int parametro(int codigo) {
        byte[] v = new byte[4];
        IntByReference tam = new IntByReference(4);
        if (ZKFinger.LIB.ZKFPM_GetParameters(dispositivo, codigo, v, tam) != ZKFinger.OK) return 0;
        return (v[0] & 0xFF) | (v[1] & 0xFF) << 8 | (v[2] & 0xFF) << 16 | (v[3] & 0xFF) << 24;
    }

    // ---------------------------------------------------------
    // Captura
    // ---------------------------------------------------------

    /**
     * Espera a que pongan el dedo y devuelve la lectura.
     * @param esperarSoltar primero espera a que el lector quede libre (evita leer otra vez el mismo toque)
     */
    Captura capturar(long tiempoMaxMs, boolean esperarSoltar, BooleanSupplier cancelado) throws LectorException {
        if (!abrir()) throw new LectorException("SIN_LECTOR", motivo);
        long fin = System.currentTimeMillis() + tiempoMaxMs;
        boolean libre = !esperarSoltar;
        while (System.currentTimeMillis() < fin) {
            if (cancelado.getAsBoolean()) throw new LectorException("CANCELADO", "Lectura cancelada.");
            Captura c = leerUnaVez();
            if (c == null) {
                libre = true;
            } else if (libre) {
                return c;
            }
            try {
                Thread.sleep(PAUSA_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new LectorException("CANCELADO", "Lectura cancelada.");
            }
        }
        throw new LectorException("TIEMPO", "No se detectó ninguna huella.");
    }

    /** Un intento de lectura: null si no hay dedo. */
    private Captura leerUnaVez() throws LectorException {
        synchronized (candado) {
            if (dispositivo == null && !abrir()) {
                throw new LectorException("SIN_LECTOR", "Se desconectó el lector de huellas.");
            }
            byte[] imagen = new byte[Math.max(ancho * alto, 1)];
            byte[] plantilla = new byte[TAM_PLANTILLA];
            IntByReference tam = new IntByReference(TAM_PLANTILLA);
            int r = ZKFinger.LIB.ZKFPM_AcquireFingerprint(dispositivo, imagen, imagen.length, plantilla, tam);
            if (r == ZKFinger.OK) {
                return new Captura(Arrays.copyOf(plantilla, tam.getValue()), imagen, ancho, alto);
            }
            if (r == ZKFinger.CAPTURA) return null;       // sin dedo
            if (r == -9) return null;                     // imagen de mala calidad: que vuelva a intentar
            // Otro error (lector desconectado, etc.): se cierra para reabrir en el próximo intento
            System.err.println("[agente] Error de lectura " + r + "; se reinicia el lector.");
            cerrar();
            throw new LectorException("SIN_LECTOR", "Se perdió la conexión con el lector (código " + r + ").");
        }
    }

    // ---------------------------------------------------------
    // Registro (3 lecturas -> 1 plantilla)
    // ---------------------------------------------------------

    byte[] fusionar(byte[] p1, byte[] p2, byte[] p3) throws LectorException {
        synchronized (candado) {
            if (!abrir()) throw new LectorException("SIN_LECTOR", motivo);
            if (ZKFinger.LIB.ZKFPM_DBMatch(bd, p1, p1.length, p2, p2.length) <= 0
                    || ZKFinger.LIB.ZKFPM_DBMatch(bd, p1, p1.length, p3, p3.length) <= 0) {
                throw new LectorException("DISTINTAS", "Las 3 lecturas no son del mismo dedo. Vuelva a intentarlo.");
            }
            byte[] reg = new byte[TAM_PLANTILLA];
            IntByReference tam = new IntByReference(TAM_PLANTILLA);
            int r = ZKFinger.LIB.ZKFPM_DBMerge(bd, p1, p2, p3, reg, tam);
            if (r != ZKFinger.OK) throw new LectorException("FUSION", "No se pudo generar la huella (código " + r + ").");
            return Arrays.copyOf(reg, tam.getValue());
        }
    }

    // ---------------------------------------------------------
    // Identificación 1:N
    // ---------------------------------------------------------

    /** Reemplaza la base en memoria. Devuelve {cargadas, rechazadas}. */
    int[] cargar(List<String> codigosSocios, List<byte[]> plantillas, String version) throws LectorException {
        synchronized (candado) {
            if (!abrir()) throw new LectorException("SIN_LECTOR", motivo);
            ZKFinger.LIB.ZKFPM_DBClear(bd);
            codigos.clear();
            int cargadas = 0, rechazadas = 0;
            for (int i = 0; i < plantillas.size(); i++) {
                byte[] p = plantillas.get(i);
                codigos.add(codigosSocios.get(i));
                int fid = codigos.size();              // fid = posición + 1
                if (p != null && p.length > 0 && ZKFinger.LIB.ZKFPM_DBAdd(bd, fid, p, p.length) == ZKFinger.OK) {
                    cargadas++;
                } else {
                    rechazadas++;
                }
            }
            versionCargada = version == null ? "" : version;
            return new int[] { cargadas, rechazadas };
        }
    }

    Identificacion identificar(byte[] plantilla) {
        synchronized (candado) {
            if (bd == null || codigos.isEmpty()) return new Identificacion(false, null, 0);
            IntByReference fid = new IntByReference();
            IntByReference puntaje = new IntByReference();
            int r = ZKFinger.LIB.ZKFPM_DBIdentify(bd, plantilla, plantilla.length, fid, puntaje);
            int i = fid.getValue() - 1;
            if (r == ZKFinger.OK && i >= 0 && i < codigos.size()) {
                return new Identificacion(true, codigos.get(i), puntaje.getValue());
            }
            return new Identificacion(false, null, 0);
        }
    }

    String versionCargada() { synchronized (candado) { return versionCargada; } }
    int cantidadCargada() { synchronized (candado) { return codigos.size(); } }
}
