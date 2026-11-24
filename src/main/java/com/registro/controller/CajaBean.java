package com.registro.controller;

import com.registro.dto.CajaDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.CajaFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generar Apertura de Caja: igual que frm_GenerarAperturaCaja de SisdataGimnasio.
 * Inserta el turno con EstadoCaja = 'A', monto inicial y los demás montos en 0.
 */
@Named("cajaBean")
@ViewScoped
public class CajaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);

    private transient CajaFACADE facade;

    private LocalDate fechaTurno = LocalDate.now();
    private String concepto = "APERTURA DE CAJA";
    private String montoInicial = "0.00";
    private int codResp;
    private String nombreResp = "";

    /** Caja ya abierta (no se puede abrir otra hasta cerrarla). */
    private CajaDTO cajaAbierta;
    private boolean aperturaGenerada;
    private String turnoGenerado;

    private CajaFACADE facade() {
        if (facade == null) facade = new CajaFACADE();
        return facade;
    }

    /**
     * Proceso que pidió la caja (frm_GenerarAperturaCaja.AlAperturar), llega desde CajaAbiertaFilter
     * como ?destino=/RegistrarAsistencia.xhtml. Solo se aceptan las páginas del filtro.
     */
    private String destino;

    @PostConstruct
    public void init() {
        destino = destinoValido(FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("destino"));
        if (destino != null) {
            mensaje(FacesMessage.SEVERITY_WARN, "Para ingresar a " + nombreProceso(destino)
                    + " primero debe generar la apertura de caja.");
        }
        UsuarioDTO u = MembresiaBean.usuarioEnSesion();
        if (u != null) {
            codResp = u.getPersonal_ID();
            nombreResp = nombreCompleto(u);
        }
        try {
            cajaAbierta = facade().CajaAbiertaF(MembresiaBean.codEmpSesion());
            if (cajaAbierta != null) {
                mensaje(FacesMessage.SEVERITY_WARN, "Ya existe una caja abierta (turno " + CierreCajaBean.nombreTurno(cajaAbierta.getTurno())
                        + "). Ciérrela antes de generar una nueva apertura.");
            }
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /** btngenerarApertura_Click */
    public String generar() {
        if (cajaAbierta != null || aperturaGenerada) return null;
        Double monto = validarMonto();
        if (monto == null) return null;
        if (fechaTurno == null) {
            mensaje(FacesMessage.SEVERITY_WARN, "Seleccione la fecha de turno.");
            return null;
        }
        try {
            String codEmp = MembresiaBean.codEmpSesion();
            LocalDateTime fecha = fechaTurno.atStartOfDay();

            CajaDTO turno = facade().ListaTurnoDiaF(codEmp, fecha);
            String nroTurno = turno != null && turno.getTurno() != null && !turno.getTurno().isEmpty() ? turno.getTurno() : "1";

            CajaDTO a = new CajaDTO();
            a.setFechaTurno(fecha);
            a.setFechaCaja(fecha);
            a.setHoraIni(LocalDateTime.now().format(HORA));
            a.setCodResp(codResp);
            a.setMontoInicial(monto);
            a.setHoraCierre("");
            a.setEstadoCaja("A");
            a.setTurno(nroTurno);
            a.setCodEmp(codEmp);
            a.setUsuarioMod(codResp);

            if (!facade().InsertAperturaCierreCajaF(a)) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo generar la apertura de caja.");
                return null;
            }
            aperturaGenerada = true;
            turnoGenerado = nroTurno;
            mensaje(FacesMessage.SEVERITY_INFO, "Se generó la Apertura de Caja correctamente (turno "
                    + CierreCajaBean.nombreTurno(nroTurno) + ").");
            if (destino != null) {
                // Abre el proceso que pidió la caja; el mensaje viaja en el flash
                FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
                return destino + (destino.contains("?") ? "&" : "?") + "faces-redirect=true";
            }
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    /** ValidarMontoInicial: obligatorio, numérico y no negativo. */
    private Double validarMonto() {
        String texto = montoInicial == null ? "" : montoInicial.trim().replace(",", "");
        String error = null;
        Double monto = null;
        if (texto.isEmpty()) error = "Ingrese el monto inicial de la caja.";
        else {
            try { monto = Double.parseDouble(texto); }
            catch (NumberFormatException ex) { error = "El monto inicial debe ser un número válido."; }
            if (monto != null && monto < 0) error = "El monto inicial no puede ser negativo.";
        }
        if (error == null) return monto;
        mensaje(FacesMessage.SEVERITY_WARN, error);
        return null;
    }

    /** Acepta solo /Pagina.xhtml (con su query) de CajaAbiertaFilter.PAGINAS_CON_CAJA. */
    private static String destinoValido(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        int q = v.indexOf('?');
        String pagina = q >= 0 ? v.substring(0, q) : v;
        return CajaAbiertaFilter.PAGINAS_CON_CAJA.contains(pagina) ? v : null;
    }

    private static String nombreProceso(String destino) {
        if (destino.startsWith("/RegistrarAsistencia")) return "Registrar Asistencia";
        if (destino.startsWith("/Ventas")) return "Ventas";
        if (destino.startsWith("/Registro-plan-membresia")) return "Registro de Plan de Membresía";
        if (destino.startsWith("/membresias")) return "Suscribir / Renovar Membresía";
        if (destino.startsWith("/PuntoVenta")) return "Punto de Venta";
        return "este proceso";
    }

    static String nombreCompleto(UsuarioDTO u) {
        StringBuilder sb = new StringBuilder();
        for (String p : new String[]{u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno()}) {
            if (p != null && !p.trim().isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(p.trim());
        }
        return sb.length() > 0 ? sb.toString() : (u.getUsuario() == null ? "" : u.getUsuario().trim());
    }

    static void mensaje(FacesMessage.Severity severidad, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, msg, null));
    }

    // ---------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------

    public LocalDate getFechaTurno() { return fechaTurno; }
    public void setFechaTurno(LocalDate fechaTurno) { this.fechaTurno = fechaTurno; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public String getMontoInicial() { return montoInicial; }
    public void setMontoInicial(String montoInicial) { this.montoInicial = montoInicial; }
    public int getCodResp() { return codResp; }
    public String getNombreResp() { return nombreResp; }
    public CajaDTO getCajaAbierta() { return cajaAbierta; }
    public boolean isAperturaGenerada() { return aperturaGenerada; }
    public String getTurnoGenerado() { return turnoGenerado; }
    public boolean isBloqueado() { return cajaAbierta != null || aperturaGenerada; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destinoValido(destino); }
    public String getNombreDestino() { return destino == null ? "" : nombreProceso(destino); }
}
