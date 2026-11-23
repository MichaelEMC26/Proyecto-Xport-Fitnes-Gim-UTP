package com.registro.controller;

import com.registro.dto.MembresiaDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.MembresiaFACADE;
import com.registro.facade.UsuarioFACADE;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.registro.controller.MembresiaBean.codEmpSesion;
import static com.registro.controller.MembresiaBean.mensaje;
import static com.registro.controller.MembresiaBean.vacio;

/**
 * Consulta de suscripciones de membresía (ListaSuscripcionesMembresia.xhtml), como frm_ConsultaMembresias
 * de SisdataGimnasio: usa MembresiaFACADE.Listasuscripciones_MembresiaF (Opc 1 = activas, 2 = terminadas).
 * Filtros: rango de fechas de inicio y texto (N° registro, código / nombre del socio o membresía).
 */
@Named("consultaSuscripcionesBean")
@ViewScoped
public class ConsultaSuscripcionesBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private transient MembresiaFACADE facade;

    private String opcion = "1";       // 1 = activas, 2 = terminadas (rdactivas / rdterminadas)
    private String desde = "";         // yyyy-MM-dd (input type="date")
    private String hasta = "";
    private String filtro = "";

    private List<MembresiaDTO> lista = new ArrayList<>();
    private int vigentes;
    private double totalCobrado;
    private double montoPendiente;      // suma del Total de las suscripciones en estado "Pendiente"
    private int cantidadPendientes;
    private Map<Integer, String> vendedores;

    // Rendimiento: la lista de la BD se guarda por opción (1 activas / 2 terminadas) y Buscar / Limpiar
    // solo filtran en memoria. Se vuelve a consultar al abrir la página, al cambiar la opción o al anular.
    private List<MembresiaDTO> datosBD;
    private boolean datosActivas;

    private MembresiaFACADE getFacade() {
        if (facade == null) facade = new MembresiaFACADE();
        return facade;
    }

    /** Al abrir la página (?socio= filtra por el socio). Solo la primera vez, no en los postback. */
    public void abrir() {
        if (FacesContext.getCurrentInstance().isPostback()) return;
        String socio = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("socio");
        if (!vacio(socio)) filtro = socio.trim();
        datosBD = null;
        cargar();
    }

    public String buscar() {
        cargar();
        return null;
    }

    public String limpiar() {
        opcion = "1";
        desde = "";
        hasta = "";
        filtro = "";
        cargar();
        return null;
    }

    /** Anula la suscripción (no se permite si ya está "Efectiva", igual que en la Suscripción). */
    public String anular(String nro) {
        if (vacio(nro)) return null;
        try {
            String r = getFacade().AnularSuscripcionF(codEmpSesion(), nro.trim());
            if ("OK".equals(r)) mensaje(FacesMessage.SEVERITY_INFO, "Se anuló la suscripción " + nro.trim() + ".");
            else mensaje(FacesMessage.SEVERITY_WARN, r);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo anular la suscripción: " + ex.getMessage());
        }
        datosBD = null;          // cambió en la BD: se vuelve a consultar
        cargar();
        return null;
    }

    private void cargar() {
        lista = new ArrayList<>();
        vigentes = 0;
        totalCobrado = 0;
        montoPendiente = 0;
        cantidadPendientes = 0;

        LocalDate d = fecha(desde);
        LocalDate h = fecha(hasta);
        if (d != null && h != null && d.isAfter(h)) {
            mensaje(FacesMessage.SEVERITY_WARN, "La fecha Desde no puede ser mayor que la fecha Hasta.");
            return;
        }

        boolean activas = !"2".equals(opcion);
        String texto = filtro == null ? "" : filtro.trim().toUpperCase(Locale.ROOT);

        List<MembresiaDTO> datos;
        if (datosBD != null && datosActivas == activas) {
            datos = datosBD;
        } else {
            try {
                datos = getFacade().Listasuscripciones_MembresiaF(codEmpSesion(), activas ? 1 : 2);
            } catch (RuntimeException ex) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo consultar las suscripciones: " + ex.getMessage());
                return;
            }
            datosBD = datos;
            datosActivas = activas;
        }

        for (MembresiaDTO s : datos) {
            // Activas: igual que el RowFilter del formulario (Condicion NOT LIKE 'TERMIN*')
            if (activas && terminada(s)) continue;

            LocalDate inicio = s.getFechaInicio() == null ? null : s.getFechaInicio().toLocalDate();
            if (d != null && (inicio == null || inicio.isBefore(d))) continue;
            if (h != null && (inicio == null || inicio.isAfter(h))) continue;

            if (!texto.isEmpty() && !contiene(s.getRegistro(), texto) && !contiene(s.getCliente(), texto)
                    && !contiene(s.getSocio(), texto) && !contiene(s.getMembresia(), texto)) continue;

            s.setVigente(esVigente(s) ? "SI" : "NO");
            s.setVendedor(nombreVendedor(s.getPersonal_ID()));
            lista.add(s);

            if (esVigente(s)) vigentes++;
            if (!anulada(s)) totalCobrado += s.getTotal();
            if (pendiente(s)) {
                montoPendiente += s.getTotal();
                cantidadPendientes++;
            }
        }
    }

    // ---------------------------------------------------------------
    // Ayudas para la vista
    // ---------------------------------------------------------------

    public String fechaTexto(Date f) {
        return f == null ? "" : f.toLocalDate().format(DD_MM_YYYY);
    }

    public String soles(double v) {
        return String.format(Locale.US, "S/ %,.2f", v);
    }

    public boolean esVigente(MembresiaDTO s) {
        return !terminada(s) && !anulada(s);
    }

    public boolean terminada(MembresiaDTO s) {
        String cond = s.getCondicion() == null ? "" : s.getCondicion().trim().toUpperCase(Locale.ROOT);
        return cond.startsWith("TERMIN")
                || (s.getFechaFin() != null && s.getFechaFin().toLocalDate().isBefore(LocalDate.now()));
    }

    /** Estado "Pendiente": la suscripción está pendiente de pago (se muestra en rojo). */
    public boolean pendiente(MembresiaDTO s) {
        String e = s.getEstadoSusc() == null ? "" : s.getEstadoSusc().trim();
        return e.toUpperCase(Locale.ROOT).startsWith("PENDIENTE");
    }

    private static boolean anulada(MembresiaDTO s) {
        String e = s.getEstadoSusc() == null ? "" : s.getEstadoSusc().trim();
        return e.toUpperCase(Locale.ROOT).startsWith("ANULAD");
    }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toUpperCase(Locale.ROOT).contains(texto);
    }

    private static LocalDate fecha(String iso) {
        if (vacio(iso)) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** Personal_ID → nombre del vendedor (sp_lista_personal); el código si no se encuentra. */
    private String nombreVendedor(int id) {
        if (id <= 0) return "";
        if (vendedores == null) {
            vendedores = new HashMap<>();
            try {
                for (UsuarioDTO u : new UsuarioFACADE().ListaPersonal()) {
                    String nombre = u.getNombreCompleto();
                    if (!vacio(nombre)) vendedores.put(u.getPersonal_ID(), nombre.trim());
                }
            } catch (RuntimeException ex) {
                // sin lista de personal: se muestra el código
            }
        }
        return vendedores.getOrDefault(id, String.valueOf(id));
    }

    // ---------------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------------

    public List<MembresiaDTO> getLista() { return lista; }
    public int getCantidad() { return lista.size(); }
    public int getVigentes() { return vigentes; }
    public String getTotalCobradoTexto() { return soles(totalCobrado); }
    public String getMontoPendienteTexto() { return soles(montoPendiente); }
    public int getCantidadPendientes() { return cantidadPendientes; }
    public String getPeriodoTexto() {
        String t = "2".equals(opcion) ? "Membresías terminadas" : "Membresías activas";
        LocalDate d = fecha(desde), h = fecha(hasta);
        if (d != null || h != null) {
            t += " · inicio " + (d != null ? "del " + d.format(DD_MM_YYYY) : "") + (h != null ? " al " + h.format(DD_MM_YYYY) : "");
        }
        return t;
    }

    public String getOpcion() { return opcion; }
    public void setOpcion(String opcion) { this.opcion = opcion; }
    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }
    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }
    public String getFiltro() { return filtro; }
    public void setFiltro(String filtro) { this.filtro = filtro; }
}
