package com.registro.controller;

import com.registro.dto.AperturaCierreCajaDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.AperturaCierreCajaFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registrar Cierre de Caja: igual que frm_RegistrarCierreCaja de SisdataGimnasio.
 * Carga la caja abierta, trae los montos del turno (cada casilla), calcula caja parcial y efectivo
 * actual, y cierra el turno (EstadoCaja = 'C') con sp_UpdateAperturaCierreCaja. Luego muestra el ticket.
 */
@Named("cierreCajaBean")
@ViewScoped
public class CierreCajaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient AperturaCierreCajaFACADE facade;

    private AperturaCierreCajaDTO caja;          // caja abierta (dtot.Rows[0])
    private int codResp;
    private String nombreResp = "";

    private LocalDate fechaTurno;                // dateFechaReg
    private boolean historialCierres;            // checkCierres
    private LocalDate fechaTurno2;               // dateFechaReg2

    // Casillas
    private boolean chkVentaDia, chkContado, chkCredito, chkEfectivo, chkDeposito, chkGastos, chkCobros;

    // Montos
    private double cajaInicial, ventaDia, ventaContado, ventaCredito, ventaEfectivo, ventaDeposito, gastos, cobros;
    private double cajaParcial, efectivoActual;

    private boolean calculado;                   // btnCerrarCaja.Enabled
    private boolean cerrado;
    private int idCierreImpreso = -1;
    private AperturaCierreCajaDTO ticket;

    private AperturaCierreCajaFACADE facade() {
        if (facade == null) facade = new AperturaCierreCajaFACADE();
        return facade;
    }

    private static String codEmp() { return MembresiaBean.codEmpSesion(); }

    /** frm_RegistarCierreCaja_Load -> ListaDetalleTurno */
    @PostConstruct
    public void init() {
        UsuarioDTO u = MembresiaBean.usuarioEnSesion();
        if (u != null) {
            codResp = u.getPersonal_ID();
            nombreResp = AperturaCajaBean.nombreCompleto(u);
        }
        try {
            caja = facade().CajaAbiertaF(codEmp());
            if (caja == null) {
                AperturaCajaBean.mensaje(FacesMessage.SEVERITY_WARN, "No hay una caja abierta. Genere primero la apertura de caja.");
                return;
            }
            fechaTurno = caja.getFechaTurno() != null ? caja.getFechaTurno().toLocalDate() : LocalDate.now();
            fechaTurno2 = fechaTurno;
            cajaInicial = caja.getMontoInicial();
        } catch (RuntimeException ex) {
            AperturaCajaBean.mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /** checkCierres ? dateFechaReg2 : dateFechaReg */
    private LocalDateTime fechaConsulta() {
        LocalDate f = historialCierres && fechaTurno2 != null ? fechaTurno2 : fechaTurno;
        return (f != null ? f : LocalDate.now()).atStartOfDay();
    }

    private String turno() { return caja == null ? "" : caja.getTurno(); }
    private int idApertura() { return caja == null ? 0 : caja.getidAperturaCierre(); }

    // ---------------------------------------------------------
    // Casillas: al marcar trae el monto, al desmarcar 0.00 (checkXxx_CheckedChanged)
    // ---------------------------------------------------------

    public void cambioVentaDia()  { ventaDia      = chkVentaDia  ? facade().MontoVentaDiaXTurnoF(codEmp(), fechaConsulta(), turno()) : 0; invalidar(); }
    public void cambioContado()   { ventaContado  = chkContado   ? facade().MontoVentaDiaContadoXTurnoF(codEmp(), fechaConsulta(), turno()) : 0; invalidar(); }
    public void cambioCredito()   { ventaCredito  = chkCredito   ? facade().MontoVentaDiaCreditoXTurnoF(codEmp(), fechaConsulta(), turno()) : 0; invalidar(); }
    public void cambioEfectivo()  { ventaEfectivo = chkEfectivo  ? facade().MontoVentaDiaEfectivoXTurnoF(codEmp(), fechaConsulta(), turno()) : 0; invalidar(); }
    public void cambioDeposito()  { ventaDeposito = chkDeposito  ? facade().MontoVentaDiaDepositoXTurnoF(codEmp(), fechaConsulta(), turno()) : 0; invalidar(); }
    public void cambioGastos()    { gastos        = chkGastos    ? facade().ListaGastosDiaXTurnoF(codEmp(), fechaConsulta(), idApertura()).getMonto() : 0; invalidar(); }
    public void cambioCobros()    { cobros        = chkCobros    ? facade().ListaCobrosDiaXTurnoF(codEmp(), fechaConsulta(), idApertura()).getMonto() : 0; invalidar(); }

    /** Al cambiar "Historial de cierres" o su fecha: vuelve a traer los montos marcados. */
    public void refrescar() {
        if (caja == null || cerrado) return;
        try {
            cambioVentaDia(); cambioContado(); cambioCredito(); cambioEfectivo();
            cambioDeposito(); cambioGastos(); cambioCobros();
        } catch (RuntimeException ex) {
            AperturaCajaBean.mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /** Si cambian los montos hay que volver a calcular antes de cerrar. */
    private void invalidar() { calculado = false; }

    /** btnactivar_Click */
    public void activarTodos() {
        if (caja == null || cerrado) return;
        chkVentaDia = chkContado = chkCredito = chkEfectivo = chkDeposito = chkGastos = chkCobros = true;
        refrescar();
    }

    /** btndesactivar_Click */
    public void desactivarTodos() {
        if (cerrado) return;
        chkVentaDia = chkContado = chkCredito = chkEfectivo = chkDeposito = chkGastos = chkCobros = false;
        ventaDia = ventaContado = ventaCredito = ventaEfectivo = ventaDeposito = gastos = cobros = 0;
        cajaParcial = efectivoActual = 0;
        calculado = false;
    }

    /** CALCULAR habilitado cuando al menos una casilla está marcada (btncalcular.Enabled). */
    public boolean isPuedeCalcular() {
        return caja != null && !cerrado && (chkVentaDia || chkContado || chkCredito || chkEfectivo || chkDeposito || chkGastos || chkCobros);
    }

    /** btncalcular_Click */
    public void calcular() {
        if (!isPuedeCalcular()) return;
        double result1 = cajaInicial + ventaDia + cobros;
        cajaParcial = result1 - ventaCredito - ventaDeposito - gastos;
        efectivoActual = cajaParcial - cajaInicial;
        calculado = true;
    }

    /** btnCerrarCaja_Click (la confirmación se pide en la página). */
    public void cerrarCaja() {
        if (caja == null || cerrado || !calculado) return;
        try {
            AperturaCierreCajaDTO a = new AperturaCierreCajaDTO();
            a.setidAperturaCierre(caja.getidAperturaCierre());
            a.setFechaTurno(fechaConsulta());
            a.setFechaCaja(LocalDate.now().atStartOfDay());
            a.setCodResp(codResp);
            a.setMontoInicial(cajaInicial);
            a.setVentaDia(ventaDia);
            a.setVentaContado(ventaContado);
            a.setVentaCredito(ventaCredito);
            a.setVentaEfectivo(ventaEfectivo);
            a.setVentaDeposito(ventaDeposito);
            a.setTotalPagos(cobros);
            a.setTotalGastos(gastos);
            a.setTotalIngEfectivo(cajaParcial);
            a.setHoraCierre(LocalDateTime.now().format(AperturaCajaBean.HORA));
            a.setEstadoCaja("C");
            a.setEfectivoActual(efectivoActual);
            a.setTurno(turno());
            a.setCodEmp(codEmp());
            a.setUsuarioMod(codResp);

            if (!facade().UpdateAperturaCierreCajaF(a)) {
                AperturaCajaBean.mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo cerrar la caja.");
                return;
            }
            cerrado = true;
            idCierreImpreso = caja.getidAperturaCierre();
            AperturaCajaBean.mensaje(FacesMessage.SEVERITY_INFO, "Se cerró la Caja y Turno correctamente.");
            reporte();
        } catch (RuntimeException ex) {
            AperturaCajaBean.mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /** btnReporte_Click: ticket de cierre (sp_Reporte_TicketCierreCaja). */
    public void reporte() {
        if (idCierreImpreso < 0) return;
        try {
            List<AperturaCierreCajaDTO> datos = facade().Reporte_TicketCierreCajaF(codEmp(), idCierreImpreso);
            ticket = datos.isEmpty() ? null : datos.get(0);
            if (ticket == null) {
                AperturaCajaBean.mensaje(FacesMessage.SEVERITY_WARN,
                        "La caja se cerró, pero no se pudo obtener el ticket de cierre.");
            }
        } catch (RuntimeException ex) {
            AperturaCajaBean.mensaje(FacesMessage.SEVERITY_WARN,
                    "La caja se cerró, pero no se pudo mostrar el ticket de cierre. " + ex.getMessage());
        }
    }

    public void cerrarTicket() { ticket = null; }

    /** "1" -> "1er", "2" -> "2do", ... (lblTurno) */
    static String nombreTurno(String turno) {
        if (turno == null) return "";
        switch (turno.trim()) {
            case "1": return "1er";
            case "2": return "2do";
            case "3": return "3er";
            case "4": return "4to";
            case "5": return "5to";
            case "6": return "6to";
            case "7": return "7mo";
            case "8": return "8vo";
            case "9": return "9no";
            default: return turno.trim();
        }
    }

    // ---------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------

    public AperturaCierreCajaDTO getCaja() { return caja; }
    public boolean isHayCaja() { return caja != null; }
    public String getTurnoTexto() { return caja == null ? "--" : nombreTurno(caja.getTurno()); }
    public int getIdAperturaCierre() { return idApertura(); }
    public int getCodResp() { return codResp; }
    public String getNombreResp() { return nombreResp; }

    public LocalDate getFechaTurno() { return fechaTurno; }
    public boolean isHistorialCierres() { return historialCierres; }
    public void setHistorialCierres(boolean v) { this.historialCierres = v; }
    public LocalDate getFechaTurno2() { return fechaTurno2; }
    public void setFechaTurno2(LocalDate v) { this.fechaTurno2 = v; }

    public boolean isChkVentaDia() { return chkVentaDia; }
    public void setChkVentaDia(boolean v) { chkVentaDia = v; }
    public boolean isChkContado() { return chkContado; }
    public void setChkContado(boolean v) { chkContado = v; }
    public boolean isChkCredito() { return chkCredito; }
    public void setChkCredito(boolean v) { chkCredito = v; }
    public boolean isChkEfectivo() { return chkEfectivo; }
    public void setChkEfectivo(boolean v) { chkEfectivo = v; }
    public boolean isChkDeposito() { return chkDeposito; }
    public void setChkDeposito(boolean v) { chkDeposito = v; }
    public boolean isChkGastos() { return chkGastos; }
    public void setChkGastos(boolean v) { chkGastos = v; }
    public boolean isChkCobros() { return chkCobros; }
    public void setChkCobros(boolean v) { chkCobros = v; }

    public double getCajaInicial() { return cajaInicial; }
    public double getVentaDia() { return ventaDia; }
    public double getVentaContado() { return ventaContado; }
    public double getVentaCredito() { return ventaCredito; }
    public double getVentaEfectivo() { return ventaEfectivo; }
    public double getVentaDeposito() { return ventaDeposito; }
    public double getGastos() { return gastos; }
    public double getCobros() { return cobros; }
    public double getCajaParcial() { return cajaParcial; }
    public double getEfectivoActual() { return efectivoActual; }

    public boolean isCalculado() { return calculado; }
    public boolean isCerrado() { return cerrado; }
    public boolean isPuedeReporte() { return idCierreImpreso >= 0; }
    public AperturaCierreCajaDTO getTicket() { return ticket; }
    public String getTicketTurno() { return ticket == null ? "" : nombreTurno(ticket.getTurno()); }
}
