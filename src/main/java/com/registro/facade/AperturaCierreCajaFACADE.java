package com.registro.facade;

import com.registro.dao.AperturaCierreCajaDAO;
import com.registro.dto.AperturaCierreCajaDTO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fachada de apertura/cierre de caja (frm_GenerarAperturaCaja, frm_RegistrarCierreCaja).
 *
 * @author Ing
 */
public class AperturaCierreCajaFACADE {

    AperturaCierreCajaDAO dao = new AperturaCierreCajaDAO();

    public AperturaCierreCajaDTO ListaAcuentaPagoXFechasXTurnoF(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin, String Turno) {
        return dao.ListaAcuentaPagoXFechasXTurno(CodEmp, FechaIni, FechaFin, Turno);
    }

    public List<AperturaCierreCajaDTO> ListaAperturaCierreCajaAbiertoF(String CodEmp) {
        return dao.ListaAperturaCierreCajaAbierto(CodEmp);
    }

    /** Primera caja abierta de la empresa, o null si no hay ninguna. */
    public AperturaCierreCajaDTO CajaAbiertaF(String CodEmp) {
        List<AperturaCierreCajaDTO> lista = dao.ListaAperturaCierreCajaAbierto(CodEmp);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public boolean InsertAperturaCierreCajaF(AperturaCierreCajaDTO apertura) {
        return dao.InsertAperturaCierreCaja(apertura);
    }

    public AperturaCierreCajaDTO ListaAperturaCierreCajaXFechaTurnoF(String CodEmp, LocalDateTime FechaTurno) {
        return dao.ListaAperturaCierreCajaXFechaTurno(CodEmp, FechaTurno);
    }

    public List<AperturaCierreCajaDTO> ListaCtasXCobrarAmortizadosXRangoFechasXTurnoF(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin, String Turno) {
        return dao.ListaCtasXCobrarAmortizadosXRangoFechasXTurno(CodEmp, FechaIni, FechaFin, Turno);
    }

    public AperturaCierreCajaDTO ListaCobrosDiaXTurnoF(String CodEmp, LocalDateTime FechaTurno, int idAperturaCierre) {
        return dao.ListaCobrosDiaXTurno(CodEmp, FechaTurno, idAperturaCierre);
    }

    public AperturaCierreCajaDTO ListaGastosDiaXTurnoF(String CodEmp, LocalDateTime FechaTurno, int idAperturaCierre) {
        return dao.ListaGastosDiaXTurno(CodEmp, FechaTurno, idAperturaCierre);
    }

    public AperturaCierreCajaDTO ListaTurnoAbiertoF(String CodEmp, LocalDateTime FechaTurno) {
        return dao.ListaTurnoAbierto(CodEmp, FechaTurno);
    }

    public AperturaCierreCajaDTO ListaTurnoDiaF(String CodEmp, LocalDateTime FechaTurno) {
        return dao.ListaTurnoDia(CodEmp, FechaTurno);
    }

    public double MontoVentaDiaXTurnoF(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return dao.MontoVentaDiaXTurno(CodEmp, FechaTurno, Turno);
    }

    public double MontoVentaDiaContadoXTurnoF(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return dao.MontoVentaDiaContadoXTurno(CodEmp, FechaTurno, Turno);
    }

    public double MontoVentaDiaCreditoXTurnoF(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return dao.MontoVentaDiaCreditoXTurno(CodEmp, FechaTurno, Turno);
    }

    public double MontoVentaDiaEfectivoXTurnoF(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return dao.MontoVentaDiaEfectivoXTurno(CodEmp, FechaTurno, Turno);
    }

    public double MontoVentaDiaDepositoXTurnoF(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return dao.MontoVentaDiaDepositoXTurno(CodEmp, FechaTurno, Turno);
    }

    public boolean UpdateAperturaCierreCajaF(AperturaCierreCajaDTO apertura) {
        return dao.UpdateAperturaCierreCaja(apertura);
    }

    public List<AperturaCierreCajaDTO> Reporte_TicketCierreCajaF(String CodEmp, int idAperturaCierre) {
        return dao.Reporte_TicketCierreCaja(CodEmp, idAperturaCierre);
    }
}
