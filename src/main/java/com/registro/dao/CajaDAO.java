package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.CajaDTO;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Apertura y cierre de caja (tabla AperturaCierreCaja).
 * Mismos procedimientos que usan frm_GenerarAperturaCaja y frm_RegistrarCierreCaja de SisdataGimnasio
 * (ver _Procedimiento3.java y Script/ActualizacionGimnasio7_Procedimientos.sql).
 *
 * @author Ing
 */
public class CajaDAO {

    // ---------------------------------------------------------
    // Utilitarios
    // ---------------------------------------------------------

    private static Timestamp ts(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.valueOf(fecha);
    }

    private static LocalDateTime fecha(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toLocalDateTime();
    }

    private static String texto(ResultSet rs, String columna) throws SQLException {
        String s = rs.getString(columna);
        return s == null ? "" : s.trim();
    }

    /** Ejecuta un SP que devuelve una sola columna "Monto" (cast decimal). 0 si no hay filas. */
    private double monto(String sp, Object... parametros) {
        String sql = "{call " + sp + "(" + "?,".repeat(parametros.length).replaceAll(",$", "") + ")}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                Object p = parametros[i];
                if (p instanceof LocalDateTime) cs.setTimestamp(i + 1, ts((LocalDateTime) p));
                else if (p instanceof Integer) cs.setInt(i + 1, (Integer) p);
                else cs.setString(i + 1, p == null ? null : p.toString());
            }
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    // ---------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------

    /** sp_ListaAcuentaPagoXFechasXTurno: monto cobrado a cuenta entre fechas y por turno. */
    public CajaDTO ListaAcuentaPagoXFechasXTurno(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin, String Turno) {
        String sql = "{call sp_ListaAcuentaPagoXFechasXTurno(?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, ts(FechaIni));
            cs.setTimestamp(3, ts(FechaFin));
            cs.setString(4, Turno);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setAcuentaPago(texto(rs, "AcuentaPago"));
                    return item;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** sp_ListaAperturaCierreCajaAbierto: caja(s) con EstadoCaja = 'A'. */
    public List<CajaDTO> ListaAperturaCierreCajaAbierto(String CodEmp) {
        List<CajaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaAperturaCierreCajaAbierto(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setidAperturaCierre(rs.getInt("idAperturaCierre"));
                    item.setFechaTurno(fecha(rs, "FechaTurno"));
                    item.setFechaCaja(fecha(rs, "FechaCaja"));
                    item.setHoraIni(texto(rs, "HoraIni"));
                    item.setCodResp(rs.getInt("CodResp"));
                    item.setMontoInicial(rs.getDouble("MontoInicial"));
                    item.setVentaDia(rs.getDouble("VentaDia"));
                    item.setVentaContado(rs.getDouble("VentaContado"));
                    item.setVentaCredito(rs.getDouble("VentaCredito"));
                    item.setVentaEfectivo(rs.getDouble("VentaEfectivo"));
                    item.setVentaDeposito(rs.getDouble("VentaDeposito"));
                    item.setTotalPagos(rs.getDouble("TotalPagos"));
                    item.setTotalGastos(rs.getDouble("TotalGastos"));
                    item.setTotalIngEfectivo(rs.getDouble("TotalIngEfectivo"));
                    item.setHoraCierre(texto(rs, "HoraCierre"));
                    item.setEstadoCaja(texto(rs, "EstadoCaja"));
                    item.setEfectivoActual(rs.getDouble("EfectivoActual"));
                    item.setTurno(texto(rs, "Turno"));
                    item.setCodEmp(texto(rs, "CodEmp"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_ListaAperturaCierreCajaXFechaTurno: responsables y turnos de una fecha. */
    public CajaDTO ListaAperturaCierreCajaXFechaTurno(String CodEmp, LocalDateTime FechaTurno) {
        String sql = "{call sp_ListaAperturaCierreCajaXFechaTurno(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, ts(FechaTurno));
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setCodResp(rs.getInt("CodResp"));
                    item.setNombres(texto(rs, "Nombres"));
                    item.setTurno(texto(rs, "Turno"));
                    item.setNomTurno(texto(rs, "NomTurno"));
                    return item;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** sp_ListaCtasXCobrarAmortizadosXRangoFechasXTurno. */
    public List<CajaDTO> ListaCtasXCobrarAmortizadosXRangoFechasXTurno(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin, String Turno) {
        List<CajaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaCtasXCobrarAmortizadosXRangoFechasXTurno(?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, ts(FechaIni));
            cs.setTimestamp(3, ts(FechaFin));
            cs.setString(4, Turno);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setCobro_ID(texto(rs, "Cobro_ID"));
                    item.setCliente_ID(texto(rs, "Cliente_ID"));
                    item.setRazonSocial(texto(rs, "RazonSocial"));
                    item.setNroRegistro(texto(rs, "NroRegistro"));
                    item.setFechaEmi(fecha(rs, "FechaEmi"));
                    item.setTipoDocumento(texto(rs, "TipoDocumento"));
                    item.setNroDocumento(texto(rs, "NroDocumento"));
                    item.setMonto(rs.getDouble("Monto"));
                    item.setNroDias(rs.getInt("NroDias"));
                    item.setCodResp(rs.getInt("Personal_ID"));
                    item.setSaldoCredito(rs.getDouble("SaldoCredito"));
                    item.setCondicionCredito(rs.getDouble("CondicionCredito"));
                    item.setFechaVenc(fecha(rs, "FechaVenc"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_ListaCobrosDiaXTurno(@FechaTurno, @idAperturaCierre, @CodEmp): total cobranza del turno. */
    public CajaDTO ListaCobrosDiaXTurno(String CodEmp, LocalDateTime FechaTurno, int idAperturaCierre) {
        CajaDTO item = new CajaDTO();
        item.setMonto(monto("sp_ListaCobrosDiaXTurno", FechaTurno, idAperturaCierre, CodEmp));
        return item;
    }

    /** sp_ListaGastosDiaXTurno(@FechaTurno, @idAperturaCierre, @CodEmp): total gastos del turno. */
    public CajaDTO ListaGastosDiaXTurno(String CodEmp, LocalDateTime FechaTurno, int idAperturaCierre) {
        CajaDTO item = new CajaDTO();
        item.setMonto(monto("sp_ListaGastosDiaXTurno", FechaTurno, idAperturaCierre, CodEmp));
        return item;
    }

    /** sp_ListaTurnoAbierto: turno abierto hasta la fecha indicada. */
    public CajaDTO ListaTurnoAbierto(String CodEmp, LocalDateTime FechaTurno) {
        String sql = "{call sp_ListaTurnoAbierto(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, ts(FechaTurno));
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setidAperturaCierre(rs.getInt("idAperturaCierre"));
                    item.setFechaTurno(fecha(rs, "FechaTurno"));
                    item.setFechaCaja(fecha(rs, "FechaCaja"));
                    item.setHoraIni(texto(rs, "HoraIni"));
                    item.setCodResp(rs.getInt("CodResp"));
                    item.setNombres(texto(rs, "Nombres"));
                    item.setMontoInicial(rs.getDouble("MontoInicial"));
                    item.setTotalPagos(rs.getDouble("TotalPagos"));
                    item.setTotalGastos(rs.getDouble("TotalGastos"));
                    item.setTotalIngEfectivo(rs.getDouble("TotalIngEfectivo"));
                    item.setHoraCierre(texto(rs, "HoraCierre"));
                    item.setEstadoCaja(texto(rs, "EstadoCaja"));
                    item.setEfectivoActual(rs.getDouble("EfectivoActual"));
                    item.setTurno(texto(rs, "Turno"));
                    return item;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** sp_ListaTurnoDia: siguiente número de turno del día (MAX(Turno)+1, o '1'). */
    public CajaDTO ListaTurnoDia(String CodEmp, LocalDateTime FechaTurno) {
        String sql = "{call sp_ListaTurnoDia(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, ts(FechaTurno));
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setTurno(texto(rs, "Turno"));
                    return item;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ---------------------------------------------------------
    // Montos de venta del turno (frm_RegistrarCierreCaja)
    // ---------------------------------------------------------

    /** sp_lista_Monto_VentaDiaXTurno: total venta del turno. */
    public double MontoVentaDiaXTurno(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return monto("sp_lista_Monto_VentaDiaXTurno", CodEmp, FechaTurno, Turno);
    }

    /** sp_lista_Monto_VentaDiaContadoXTurno: ventas al contado del turno. */
    public double MontoVentaDiaContadoXTurno(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return monto("sp_lista_Monto_VentaDiaContadoXTurno", CodEmp, FechaTurno, Turno);
    }

    /** sp_lista_Monto_VentaDiaCreditoXTurno: ventas al crédito del turno. */
    public double MontoVentaDiaCreditoXTurno(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return monto("sp_lista_Monto_VentaDiaCreditoXTurno", CodEmp, FechaTurno, Turno);
    }

    /** sp_lista_Monto_VentaDiaEfectivoXTurno: ventas en efectivo del turno. */
    public double MontoVentaDiaEfectivoXTurno(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return monto("sp_lista_Monto_VentaDiaEfectivoXTurno", CodEmp, FechaTurno, Turno);
    }

    /** sp_lista_Monto_VentaDiaDepositoXTurno: ventas con tarjeta o depósito del turno. */
    public double MontoVentaDiaDepositoXTurno(String CodEmp, LocalDateTime FechaTurno, String Turno) {
        return monto("sp_lista_Monto_VentaDiaDepositoXTurno", CodEmp, FechaTurno, Turno);
    }

    // ---------------------------------------------------------
    // Grabar
    // ---------------------------------------------------------

    /** sp_InsertAperturaCierreCaja (19 parámetros). El SP devuelve SELECT 0 AS RESULT. */
    public boolean InsertAperturaCierreCaja(CajaDTO a) {
        String sql = "{call sp_InsertAperturaCierreCaja(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setTimestamp(1, ts(a.getFechaTurno()));
            cs.setTimestamp(2, ts(a.getFechaCaja()));
            cs.setString(3, a.getHoraIni());
            cs.setInt(4, a.getCodResp());
            cs.setDouble(5, a.getMontoInicial());
            cs.setDouble(6, a.getVentaDia());
            cs.setDouble(7, a.getVentaContado());
            cs.setDouble(8, a.getVentaCredito());
            cs.setDouble(9, a.getVentaEfectivo());
            cs.setDouble(10, a.getVentaDeposito());
            cs.setDouble(11, a.getTotalPagos());
            cs.setDouble(12, a.getTotalGastos());
            cs.setDouble(13, a.getTotalIngEfectivo());
            cs.setString(14, a.getHoraCierre());
            cs.setString(15, a.getEstadoCaja());
            cs.setDouble(16, a.getEfectivoActual());
            cs.setString(17, a.getTurno());
            cs.setString(18, a.getCodEmp());
            cs.setInt(19, a.getUsuarioMod());
            cs.execute();   // SET NOCOUNT ON + SELECT: executeUpdate devolvería -1
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * sp_UpdateAperturaCierreCaja (19 parámetros, sin HoraIni): cierra el turno y marca Dia='C'
     * en ventas, compras y gastos. El SP hace RETURN 0 / -1.
     */
    public boolean UpdateAperturaCierreCaja(CajaDTO a) {
        String sql = "{? = call sp_UpdateAperturaCierreCaja(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.registerOutParameter(1, java.sql.Types.INTEGER);
            cs.setInt(2, a.getidAperturaCierre());
            cs.setTimestamp(3, ts(a.getFechaTurno()));
            cs.setTimestamp(4, ts(a.getFechaCaja()));
            cs.setInt(5, a.getCodResp());
            cs.setDouble(6, a.getMontoInicial());
            cs.setDouble(7, a.getVentaDia());
            cs.setDouble(8, a.getVentaContado());
            cs.setDouble(9, a.getVentaCredito());
            cs.setDouble(10, a.getVentaEfectivo());
            cs.setDouble(11, a.getVentaDeposito());
            cs.setDouble(12, a.getTotalPagos());
            cs.setDouble(13, a.getTotalGastos());
            cs.setDouble(14, a.getTotalIngEfectivo());
            cs.setString(15, a.getHoraCierre());
            cs.setString(16, a.getEstadoCaja());
            cs.setDouble(17, a.getEfectivoActual());
            cs.setString(18, a.getTurno());
            cs.setString(19, a.getCodEmp());
            cs.setInt(20, a.getUsuarioMod());
            cs.execute();
            return cs.getInt(1) == 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ---------------------------------------------------------
    // Reporte
    // ---------------------------------------------------------

    /** sp_Reporte_TicketCierreCaja: datos del ticket de cierre. */
    public List<CajaDTO> Reporte_TicketCierreCaja(String CodEmp, int idAperturaCierre) {
        List<CajaDTO> lista = new ArrayList<>();
        String sql = "{call sp_Reporte_TicketCierreCaja(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setInt(2, idAperturaCierre);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    CajaDTO item = new CajaDTO();
                    item.setRazonSocial(texto(rs, "EmpresaRazon"));
                    item.setEmpresaComercial(texto(rs, "EmpresaComercial"));
                    item.setEmpresaRuc(texto(rs, "EmpresaRuc"));
                    item.setEmpresaDireccion(texto(rs, "EmpresaDireccion"));
                    item.setEmpresaTelefono(texto(rs, "EmpresaTelefono"));
                    item.setidAperturaCierre(rs.getInt("idAperturaCierre"));
                    item.setTurno(texto(rs, "Turno"));
                    item.setFechaTurno(fecha(rs, "FechaTurno"));
                    item.setFechaCaja(fecha(rs, "FechaCaja"));
                    item.setHoraIni(texto(rs, "HoraIni"));
                    item.setHoraCierre(texto(rs, "HoraCierre"));
                    item.setEstadoCaja(texto(rs, "EstadoCaja"));
                    item.setNombres(texto(rs, "Responsable"));
                    item.setCajero(texto(rs, "Cajero"));
                    item.setMontoInicial(rs.getDouble("MontoInicial"));
                    item.setVentaDia(rs.getDouble("VentaDia"));
                    item.setVentaContado(rs.getDouble("VentaContado"));
                    item.setVentaCredito(rs.getDouble("VentaCredito"));
                    item.setVentaEfectivo(rs.getDouble("VentaEfectivo"));
                    item.setVentaDeposito(rs.getDouble("VentaDeposito"));
                    item.setTotalPagos(rs.getDouble("TotalPagos"));
                    item.setTotalGastos(rs.getDouble("TotalGastos"));
                    item.setTotalIngEfectivo(rs.getDouble("TotalIngEfectivo"));
                    item.setEfectivoActual(rs.getDouble("EfectivoActual"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }
}
