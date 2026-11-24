package com.registro.controller;

import com.registro.dto.PuntoVentaDTO;
import com.registro.facade.PuntoVentaFACADE;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Formas de pago de la ventana Formas de Pago del Punto de Venta (PuntoVenta.xhtml).
 * Lee las formas de pago ACTIVAS con PuntoVentaFACADE.ListaFormasPagoActivoF (sp_ListaFormasPagoActivo)
 * y a cada una le asigna el panel que usa la ventana (tipo), según su IDFormaPago o su nombre:
 * efectivo, virtual, tarjeta, deposito, nota, credito (IDFormaPago "06" o "Crédito/Deuda") u otro.
 * Si la consulta falla o no devuelve filas, se muestran las 5 formas de pago fijas de antes.
 */
@Named("formasPagoVentaBean")
@RequestScoped
public class FormasPagoVentaBean {

    /** IDFormaPago de Crédito/Deuda en la tabla de formas de pago. */
    public static final String ID_CREDITO = "06";

    private List<Item> activas;
    private String error;
    private boolean desdeTabla;

    public List<Item> getActivas() {
        if (activas == null) cargar();
        return activas;
    }

    /** Mensaje si no se pudo leer la tabla (null si todo bien). */
    public String getError() {
        getActivas();
        return error;
    }

    /** true si las formas de pago vienen de la tabla; false si se usan las fijas. */
    public boolean isDesdeTabla() {
        getActivas();
        return desdeTabla;
    }

    private void cargar() {
        activas = new ArrayList<>();
        List<PuntoVentaDTO> filas;
        try {
            filas = new PuntoVentaFACADE().ListaFormasPagoActivoF();
        } catch (RuntimeException ex) {
            System.err.println("ListaFormasPagoActivoF: " + ex.getMessage());
            filas = new ArrayList<>();
            error = "No se pudieron cargar las formas de pago de la base de datos; se muestran las formas de pago por defecto.";
        }

        Set<String> usadas = new HashSet<>();
        for (PuntoVentaDTO f : filas) {
            String nombre = f.getFormasPago() == null ? "" : f.getFormasPago().trim();
            String id = f.getIDFormaPago() == null ? "" : f.getIDFormaPago().trim();
            if (nombre.isEmpty() && id.isEmpty()) continue;
            if (nombre.isEmpty()) nombre = "Forma de pago " + id;

            String tipo = tipo(id, nombre);
            // Cada panel de la ventana es para una sola forma de pago; si se repite, va al panel genérico
            String clave = tipo;
            if ("otro".equals(tipo) || !usadas.add(tipo)) {
                tipo = "otro";
                clave = "fp" + (id.isEmpty() ? String.valueOf(activas.size()) : id.replaceAll("[^A-Za-z0-9]", ""));
            }
            activas.add(new Item(clave, tipo, id, nombre));
        }

        desdeTabla = !activas.isEmpty();
        if (!desdeTabla) {
            if (error == null && filas.isEmpty()) {
                error = "No hay formas de pago activas en la base de datos; se muestran las formas de pago por defecto.";
            }
            activas.add(new Item("efectivo", "efectivo", "", "Efectivo"));
            activas.add(new Item("virtual", "virtual", "", "Pago Virtual"));
            activas.add(new Item("tarjeta", "tarjeta", "", "Tarjeta"));
            activas.add(new Item("deposito", "deposito", "", "Deposito/Transferencia"));
            activas.add(new Item("nota", "nota", "", "Nota de Credito"));
        }
    }

    /** Panel de la ventana que corresponde a la forma de pago. */
    static String tipo(String id, String nombre) {
        String n = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        if (ID_CREDITO.equals(id)) return "credito";                             // IDFormaPago "06"
        if (n.contains("NOTA")) return "nota";                                   // Nota de Crédito
        if (n.contains("TARJETA")) return "tarjeta";                             // incluye "Tarjeta de crédito"
        if (n.contains("CREDITO") || n.contains("DEUDA")) return "credito";
        if (n.contains("EFECTIVO")) return "efectivo";
        if (n.contains("DEPOSITO") || n.contains("TRANSFER")) return "deposito";
        if (n.contains("VIRTUAL") || n.contains("YAPE") || n.contains("PLIN") || n.contains("QR")
                || n.contains("BILLETERA")) return "virtual";
        return "otro";
    }

    /** Forma de pago para la vista. */
    public static class Item {
        private final String clave;   // data-m de la ventana (único)
        private final String tipo;    // panel: efectivo, virtual, tarjeta, deposito, nota, credito, otro
        private final String id;      // IDFormaPago ("" si es una de las fijas)
        private final String nombre;  // FormasPago

        Item(String clave, String tipo, String id, String nombre) {
            this.clave = clave;
            this.tipo = tipo;
            this.id = id;
            this.nombre = nombre;
        }

        public String getClave() { return clave; }
        public String getTipo() { return tipo; }
        public String getId() { return id; }
        public String getNombre() { return nombre; }
    }
}
