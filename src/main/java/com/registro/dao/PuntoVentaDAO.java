package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.PuntoVentaDTO;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Capa de datos de Facturación (Punto de Venta).
 *

 *
 * @author Ing
 */
public class PuntoVentaDAO {

    /* =========================================================================================
     *  Tipos de documento (TIPO_DOCUMENTO.Documento_ID, igual que frm_Facturacion)
     * ========================================================================================= */
    public static final String DOC_NOTA_VENTA = "01";
    public static final String DOC_BOLETA = "02";
    public static final String DOC_FACTURA = "03";

    /** Tipo_documento (texto) que esperan los procedimientos de ventas. */
    public static String nombreTipoDocumento(String documentoId) {
        switch (limpio(documentoId)) {
            case DOC_BOLETA: return "BOLETA DE VENTA";
            case DOC_FACTURA: return "FACTURA";
            default: return "NOTA DE VENTA";
        }
    }

    /** Boleta y factura usan serie y número de ConfiguracionSeries (TipoUsaSerie del WinForm). */
    public static boolean usaSerie(String documentoId) {
        String d = limpio(documentoId);
        return DOC_BOLETA.equals(d) || DOC_FACTURA.equals(d);
    }

    /* =========================================================================================
     *  Ayudas de lectura
     * ========================================================================================= */

    private static String limpio(String s) { return s == null ? "" : s.trim(); }

    /** Columna de texto (sin espacios); "" si es NULL o si el procedimiento no la devuelve. */
    private static String texto(ResultSet rs, String columna) throws SQLException {
        try {
            String s = rs.getString(columna);
            return s == null ? "" : s.trim();
        } catch (SQLException ex) {
            if (existeColumna(rs, columna)) throw ex;
            return "";
        }
    }

    private static double numero(ResultSet rs, String columna) throws SQLException {
        try {
            return rs.getDouble(columna);
        } catch (SQLException ex) {
            if (existeColumna(rs, columna)) throw ex;
            return 0;
        }
    }

    private static boolean existeColumna(ResultSet rs, String columna) throws SQLException {
        var md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (columna.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }

    private static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toLocalDateTime().toLocalDate();
    }

    private static Timestamp ts(LocalDate d) { return d == null ? null : Timestamp.valueOf(d.atStartOfDay()); }

    /**
     * Ejecuta el procedimiento y devuelve el primer valor del primer conjunto de resultados.
     * Algunos procedimientos (sp_InsertaNumeracion, sp_InsertaNumeracionComp) hacen un INSERT antes del
     * SELECT y no tienen SET NOCOUNT ON: primero llega un "filas afectadas" y luego el resultado.
     */
    private static String primerValor(CallableStatement cs) throws SQLException {
        boolean hayResultado = cs.execute();
        while (true) {
            if (hayResultado) {
                try (ResultSet rs = cs.getResultSet()) {
                    if (rs.next()) {
                        String v = rs.getString(1);
                        return v == null ? null : v.trim();
                    }
                }
            } else if (cs.getUpdateCount() == -1) {
                return null;                      // no hay más resultados
            }
            hayResultado = cs.getMoreResults();
        }
    }

    /* =========================================================================================
     *  CONSULTAS (cada una con su conexión)
     * ========================================================================================= */

    /** Formas de pago activas (sp_ListaFormasPagoActivo): IDFormaPago, FormasPago, Estado. */
    public List<PuntoVentaDTO> ListaFormasPagoActivo() {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_ListaFormasPagoActivo}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                PuntoVentaDTO item = new PuntoVentaDTO();
                item.setIDFormaPago(texto(rs, "IDFormaPago"));
                item.setFormasPago(texto(rs, "FormasPago"));
                item.setEstado(texto(rs, "Estado"));
                lista.add(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron cargar las formas de pago: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Tipos de documento activos (sp_Lista_TiposDoc): 01 Nota de venta, 02 Boleta, 03 Factura. */
    public List<PuntoVentaDTO> Lista_TiposDoc() {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_Lista_TiposDoc}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                PuntoVentaDTO item = new PuntoVentaDTO();
                item.setDocumento_ID(texto(rs, "Documento_ID"));
                item.setDescripcion(texto(rs, "Descripcion"));
                item.setDescripcionTipoDoc(item.getDescripcion());
                item.setEstado(texto(rs, "Estado"));
                lista.add(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron cargar los tipos de documento: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /**
     * Configuración del sistema (sp_ListaConfiguracionSistema).
     */
    public List<PuntoVentaDTO> ListaConfiguracionSistema() {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_ListaConfiguracionSistema}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                PuntoVentaDTO item = new PuntoVentaDTO();
                item.setidConfiguracion((int) numero(rs, "idConfiguracion"));
                item.setFlagStockNegativo(texto(rs, "FlagStockNegativo"));
                item.setFlagAlertaS(texto(rs, "FlagAlertaS"));
                item.setFlagAlertaCC(texto(rs, "FlagAlertaCC"));
                item.setFlagAlertaCP(texto(rs, "FlagAlertaCP"));
                item.setFlagAfectoIgv(texto(rs, "FlagAfectoIgv"));
                item.setFlagFormaPagoXComElectronico(texto(rs, "FlagFormaPagoXComElectronico"));
                item.setFlagActivaCodigoBarra(texto(rs, "FlagActivaCodigoBarra"));
                item.setFlagEnvioDirecto(texto(rs, "FlagEnvioDirecto"));
                lista.add(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer la configuración del sistema: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Cajas abiertas de la empresa (sp_ListaAperturaCierreCajaAbierto @CodEmp). */
    public List<PuntoVentaDTO> ListaAperturaCierreCajaAbierto(String codEmp) {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_ListaAperturaCierreCajaAbierto(?)}")) {
            cs.setString(1, codEmp);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    PuntoVentaDTO item = new PuntoVentaDTO();
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
            throw new IllegalStateException("No se pudo leer la caja abierta: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Última venta del turno y horas de bloqueo (sp_UltimaVentaTurno). */
    public Optional<PuntoVentaDTO> ultimaVentaTurno(String codEmp, LocalDate fechaTurno, String turno) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_UltimaVentaTurno(?, ?, ?)}")) {
            cs.setString(1, codEmp);
            cs.setTimestamp(2, ts(fechaTurno));
            cs.setString(3, turno);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    PuntoVentaDTO f = new PuntoVentaDTO();
                    Timestamp ultima = rs.getTimestamp("UltimaVenta");
                    f.setUltimaVenta(ultima != null ? ultima.toLocalDateTime() : null);
                    f.setHorasBloqueo(rs.getDouble("HorasBloqueo"));
                    return Optional.of(f);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Error al obtener la última venta del turno: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /** Siguiente código de boleta (sp_autogenera_cod_BoletaVenta: columna sin nombre). */
    public Optional<PuntoVentaDTO> autogenera_cod_BoletaVenta() {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_autogenera_cod_BoletaVenta}")) {
            String v = primerValor(cs);
            if (v == null) return Optional.empty();
            PuntoVentaDTO dato = new PuntoVentaDTO();
            dato.setBoleta_id(v);
            return Optional.of(dato);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo generar el código de boleta: " + ex.getMessage(), ex);
        }
    }

    /** Serie de boleta (sp_numero_SerieBoleta: columna sin nombre, '001' si no hay). */
    public Optional<PuntoVentaDTO> numero_SerieBoleta() {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_numero_SerieBoleta}")) {
            String v = primerValor(cs);
            if (v == null) return Optional.empty();
            PuntoVentaDTO dato = new PuntoVentaDTO();
            dato.setNroSerie(v);
            return Optional.of(dato);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer la serie de boleta: " + ex.getMessage(), ex);
        }
    }

    /** Serie y siguiente número del tipo de documento (sp_ConfiguracionSeries_Siguiente). */
    public Optional<PuntoVentaDTO> ConfiguracionSeries_Siguiente(String codEmp, String documentoId) {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            return ConfiguracionSeries_Siguiente(cn, codEmp, documentoId);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer la serie del documento: " + ex.getMessage(), ex);
        }
    }

    /** Cliente por código (sp_lista_ClienteXcod @cliente_id char(8)). */
    public Optional<PuntoVentaDTO> lista_ClienteXcod(String clienteId) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_lista_ClienteXcod(?)}")) {
            cs.setString(1, clienteId);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    PuntoVentaDTO f = new PuntoVentaDTO();
                    f.setCliente_ID(texto(rs, "Cliente_ID"));
                    f.setRazonSocial(texto(rs, "RazonSocial"));
                    f.setDireccion(texto(rs, "Direccion"));
                    f.setTelefono(texto(rs, "Telefono"));
                    f.setEstado(texto(rs, "Estado"));
                    f.setTelefono2(texto(rs, "Telefono2"));
                    f.setCorreo(texto(rs, "Correo"));
                    f.setSexo(texto(rs, "Sexo"));
                    f.setRucDNI(texto(rs, "RucDNI"));
                    f.setTipoId(texto(rs, "TipoId"));
                    f.setFoto(texto(rs, "Foto"));
                    f.setContacto(texto(rs, "Contacto"));
                    f.setTelefonoCont(texto(rs, "TelefonoCont"));
                    return Optional.of(f);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo buscar el cliente: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /** Productos activos por nombre (sp_Lista_ProductosGral_ActivosXNom). "" = todos. */
    public List<PuntoVentaDTO> Lista_ProductosGral_ActivosXNom(String codEmp, String nombreProducto) {
        return listarProductos("{call sp_Lista_ProductosGral_ActivosXNom(?,?)}", codEmp, nombreProducto);
    }

    /** Productos activos por código de barras (sp_Lista_ProductosGral_ActivosXCodBarra). */
    public List<PuntoVentaDTO> Lista_ProductosGral_ActivosXCodBarra(String codEmp, String codigoBarra) {
        return listarProductos("{call sp_Lista_ProductosGral_ActivosXCodBarra(?,?)}", codEmp, codigoBarra);
    }

    private List<PuntoVentaDTO> listarProductos(String sql, String codEmp, String filtro) {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, codEmp);                    // los parámetros van ANTES de ejecutar
            cs.setString(2, filtro == null ? "" : filtro);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    PuntoVentaDTO item = new PuntoVentaDTO();
                    item.setProducto_ID(texto(rs, "Producto_ID"));
                    item.setDescripcion(texto(rs, "Descripcion"));
                    item.setUnidadMedida(texto(rs, "UnidadMedida"));
                    item.setStockFinal(rs.getDouble("StockFinal"));
                    item.setPrecioCosto(rs.getDouble("PrecioCosto"));
                    item.setPrecioVenta(rs.getDouble("PrecioVenta"));
                    item.setPrecioLimite(rs.getDouble("PrecioLimite"));
                    // XNom devuelve NombreProducto; XCodBarra lo devuelve como NombreBusqueda
                    String nombre = texto(rs, "NombreProducto");
                    item.setNombreProducto(nombre.isEmpty() ? texto(rs, "NombreBusqueda") : nombre);
                    item.setCategoria(texto(rs, "Categoria"));   // nombre de la categoría
                    item.setMarca(texto(rs, "Marca"));           // nombre de la marca
                    item.setMargenUtil(rs.getDouble("MargenUtil"));
                    item.setPeso(rs.getDouble("Peso"));
                    item.setImagen(texto(rs, "Imagen"));
                    item.setCodigoBarra(texto(rs, "CodigoBarra"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron cargar los productos: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Tipo de producto (sp_listaTipoProductoXCodigo). */
    public Optional<PuntoVentaDTO> listaTipoProductoXCodigo(String codEmp, String productoId) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_listaTipoProductoXCodigo(?, ?)}")) {
            cs.setString(1, codEmp);
            cs.setString(2, productoId);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    PuntoVentaDTO f = new PuntoVentaDTO();
                    f.setTipoProd(texto(rs, "TipoProd"));
                    return Optional.of(f);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el tipo de producto: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /** true si el comprobante ya se imprimió (sp_ListaOrdenPedidoImpresoXPed; no aplica a notas de venta). */
    public boolean ListaOrdenPedidoImpresoXPed(String pedidoId) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_ListaOrdenPedidoImpresoXPed(?)}")) {
            cs.setString(1, pedidoId);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo revisar si la venta fue impresa: " + ex.getMessage(), ex);
        }
    }

    /**
     * Ventas de un turno (para el historial del Punto de Venta: modificar / eliminar).
     * Consulta directa a Cabecera_Ventas (no hay procedimiento para esto).
     */
    public List<PuntoVentaDTO> ListaVentasTurno(String codEmp, LocalDate fechaTurno, String turno) {
        String sql = "SELECT Pedido_ID, Tipo_documento, Venta_id, NroSerie, Nro_Documento, Cliente_ID, RazonSocial,"
                + " Fecha_Pedido, CondPago, FormaPago, SubTotal, IGV, Total, Estado, Impreso, FechaHora, FechaTurno, Turno, TipoDoc"
                + " FROM dbo.Cabecera_Ventas"
                + " WHERE CodEmp = ? AND FechaTurno >= ? AND FechaTurno < ? AND LTRIM(RTRIM(Turno)) = ?"
                + " ORDER BY FechaHora DESC, Pedido_ID DESC";
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codEmp);
            ps.setTimestamp(2, ts(fechaTurno));
            ps.setTimestamp(3, ts(fechaTurno.plusDays(1)));
            ps.setString(4, limpio(turno));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(leerCabecera(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron listar las ventas del turno: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Cabecera de una venta (null si no existe). */
    public PuntoVentaDTO ListaVentaXPedido(String codEmp, String pedidoId) {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            return ListaVentaXPedido(cn, codEmp, pedidoId);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer la venta: " + ex.getMessage(), ex);
        }
    }

    /** Detalle de una venta (Detalle_Ventas). */
    public List<PuntoVentaDTO> ListaDetalleVentaXPedido(String pedidoId) {
        String sql = "SELECT Producto_ID, Descripcion, PrecioCosto, PrecioUnitario, Cantidad, Importe"
                + " FROM dbo.Detalle_Ventas WHERE Pedido_ID = ? ORDER BY Descripcion";
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PuntoVentaDTO d = new PuntoVentaDTO();
                    d.setProducto_ID(texto(rs, "Producto_ID"));
                    d.setDescripcion(texto(rs, "Descripcion"));
                    d.setPrecioCosto(rs.getDouble("PrecioCosto"));
                    d.setPrecioUnitario(rs.getDouble("PrecioUnitario"));
                    d.setCantidad(rs.getDouble("Cantidad"));
                    d.setImporte(rs.getDouble("Importe"));
                    lista.add(d);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el detalle de la venta: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Ticket de cierre de caja (sp_Reporte_TicketCierreCaja). Antes tenía el mismo nombre que la búsqueda por código de barras. */
    public List<PuntoVentaDTO> Reporte_TicketCierreCaja(String codEmp, int idAperturaCierre) {
        List<PuntoVentaDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_Reporte_TicketCierreCaja(?,?)}")) {
            cs.setString(1, codEmp);
            cs.setInt(2, idAperturaCierre);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    PuntoVentaDTO item = new PuntoVentaDTO();
                    item.setEmpresaRazon(texto(rs, "EmpresaRazon"));
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
                    item.setResponsable(texto(rs, "Responsable"));
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
            throw new IllegalStateException("No se pudo generar el ticket de cierre: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /* =========================================================================================
     *  TRANSACCIÓN: grabar, modificar y anular ventas (todos reciben la conexión de la FACADE)
     * ========================================================================================= */

    /** Siguiente Pedido_ID (sp_InsertaNumeracion: lo reserva en NumeracionOP). */
    public String InsertaNumeracion(Connection cn) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_InsertaNumeracion}")) {
            String v = primerValor(cs);
            if (v == null || v.isEmpty()) throw new SQLException("sp_InsertaNumeracion no devolvió el número de pedido.");
            return v;
        }
    }

    /** Siguiente Venta_id del tipo de documento (sp_InsertaNumeracionComp @TipDoc: 01, 02, 03). */
    public String InsertaNumeracionComp(Connection cn, String documentoId) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_InsertaNumeracionComp(?)}")) {
            cs.setString(1, documentoId);
            String v = primerValor(cs);
            if (v == null || v.isEmpty()) throw new SQLException("sp_InsertaNumeracionComp no devolvió el número del comprobante.");
            return v;
        }
    }

    /** Serie de boleta para la nota de venta (sp_numero_SerieBoleta, dentro de la transacción). */
    public String numero_SerieBoleta(Connection cn) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_numero_SerieBoleta}")) {
            String v = primerValor(cs);
            return v == null || v.isEmpty() ? "001" : v;
        }
    }

    /** Serie y siguiente número (sp_ConfiguracionSeries_Siguiente), leídos al grabar por si otra caja usó el número. */
    public Optional<PuntoVentaDTO> ConfiguracionSeries_Siguiente(Connection cn, String codEmp, String documentoId) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_ConfiguracionSeries_Siguiente(?, ?)}")) {
            cs.setString(1, codEmp);
            cs.setString(2, documentoId);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    PuntoVentaDTO f = new PuntoVentaDTO();
                    f.setNroSerie(texto(rs, "NroSerie"));
                    f.setNroDocumento(texto(rs, "NroDocumento"));
                    return Optional.of(f);
                }
            }
        }
        return Optional.empty();
    }

    /** Guarda el último número usado de la serie (sp_ConfiguracionSeries_Actualiza). */
    public void ConfiguracionSeries_Actualiza(Connection cn, String codEmp, String documentoId,
                                              String nroSerie, String nroDocumento) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_ConfiguracionSeries_Actualiza(?, ?, ?, ?)}")) {
            cs.setString(1, codEmp);
            cs.setString(2, documentoId);
            cs.setString(3, nroSerie);
            cs.setString(4, nroDocumento);
            cs.execute();
        }
    }

    /** Crea el cliente solo si no existe (RegistraClienteSiNoExiste del WinForm; no toca un cliente existente). */
    public void RegistraClienteSiNoExiste(Connection cn, PuntoVentaDTO c) throws SQLException {
        String id = limpio(c.getCliente_ID());
        if (id.isEmpty()) return;
        try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM dbo.Cliente WHERE Cliente_ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        // sp_Insert_Clientes: 14 parámetros (el 15.º, @FechaCumpleaños, tiene valor por defecto)
        try (CallableStatement cs = cn.prepareCall("{call sp_Insert_Clientes(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setString(1, id);
            cs.setString(2, limpio(c.getRazonSocial()).isEmpty() ? "CLIENTE" : limpio(c.getRazonSocial()));
            cs.setString(3, limpio(c.getDireccion()));
            cs.setString(4, limpio(c.getTelefono()));
            cs.setString(5, "Activo");
            cs.setString(6, limpio(c.getTelefono()));
            cs.setString(7, limpio(c.getCorreo()));
            cs.setString(8, "MASCULINO");
            cs.setString(9, id);
            cs.setString(10, id.length() >= 11 ? "RUC" : "DNI");
            cs.setString(11, "");
            cs.setString(12, "");
            cs.setString(13, "");
            cs.setNull(14, Types.VARBINARY);
            cs.execute();
        }
    }

    /** Cabecera de la venta (sp_Insert_CabeceraVentas: también Nota_Venta / Boleta / Factura y DetalleFormasPago). */
    public void Insert_CabeceraVentas(Connection cn, PuntoVentaDTO c) throws SQLException {
        try (CallableStatement cs = cn.prepareCall(
                "{call sp_Insert_CabeceraVentas(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setString(1, c.getPedido_ID());
            cs.setString(2, c.getTipo_documento());
            cs.setString(3, c.getVenta_id());
            cs.setString(4, c.getNroSerie());
            cs.setString(5, c.getNro_Doc());
            cs.setString(6, c.getCliente_ID());
            cs.setString(7, c.getRazonSocial());
            cs.setTimestamp(8, ts(c.getFecha_Pedido()));
            cs.setString(9, c.getCondPago());
            cs.setString(10, c.getFormaPago());
            cs.setDouble(11, c.getSubTotal());
            cs.setDouble(12, c.getIGV());
            cs.setDouble(13, c.getTotal());
            cs.setString(14, c.getEstadoPed());
            cs.setString(15, c.getCodEmp());
            cs.setString(16, c.getMontoLetras());
            cs.setInt(17, c.getCodPer());
            cs.setString(18, c.getDia());
            cs.setTimestamp(19, ts(c.getFechaTurno()));
            cs.setTimestamp(20, c.getFechaHora() != null ? Timestamp.valueOf(c.getFechaHora()) : null);
            cs.setString(21, c.getTurno());
            cs.setString(22, c.getTipoDoc());
            cs.setString(23, c.getIDFormaPago());
            cs.setInt(24, c.getCodUser());
            cs.execute();
        }
    }

    /** Detalle de la venta, un producto por llamada (sp_Insert_Detalle_Ventas: también descuenta el stock). */
    public void Insert_Detalle_Ventas(Connection cn, PuntoVentaDTO c, List<PuntoVentaDTO> detalles) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_Insert_Detalle_Ventas(?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            for (PuntoVentaDTO d : detalles) {
                cs.setString(1, c.getPedido_ID());
                cs.setString(2, c.getVenta_id());
                cs.setString(3, d.getProducto_ID());
                cs.setString(4, d.getDescripcion());
                cs.setDouble(5, d.getPrecioCosto());
                cs.setDouble(6, d.getPrecioUnitario());
                cs.setDouble(7, d.getCantidad());
                cs.setDouble(8, d.getImporte());
                cs.setString(9, c.getTipo_documento());
                cs.setString(10, c.getCodEmp());
                cs.setTimestamp(11, ts(c.getFechaTurno()));
                cs.setString(12, c.getTurno());
                cs.execute();
            }
        }
    }

    /** Comprobante electrónico (sp_InsertCont_CabCompVentas): solo boleta y factura. */
    public void InsertCont_CabCompVentas(Connection cn, PuntoVentaDTO c) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_InsertCont_CabCompVentas(?, ?, ?, ?)}")) {
            cs.setString(1, c.getCodEmp());
            cs.setString(2, c.getPedido_ID());
            cs.setTimestamp(3, ts(c.getFecha_Pedido()));
            cs.setString(4, c.getCliente_ID());
            cs.execute();
        }
    }

    /**
     * Cuenta por cobrar de la venta al crédito (sp_InsertCtasXCobrar).
     * CondCli = "CREDITO" crea o actualiza la cuenta; NroRegistro = Pedido_ID (igual que el WinForm).
     */
    public void InsertCtasXCobrar(Connection cn, PuntoVentaDTO c) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_InsertCtasXCobrar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setString(1, c.getCliente_ID());          // @Cliente_ID
            cs.setString(2, c.getPedido_ID());           // @NroRegistro
            cs.setTimestamp(3, ts(c.getFecha_Pedido()));  // @FechaEmi
            cs.setString(4, c.getTipo_documento());      // @TipoDocumento
            cs.setString(5, c.getNro_Doc());             // @NroDocumento
            cs.setDouble(6, c.getMonto());               // @Monto
            cs.setInt(7, c.getNroDias());                // @NroDias
            cs.setDouble(8, c.getInteres());             // @Interes
            cs.setInt(9, c.getCodPer());                 // @Personal_ID
            cs.setDouble(10, c.getSaldoCredito());       // @SaldoCredito
            cs.setString(11, c.getCondicionCredito());   // @CondicionCredito
            cs.setTimestamp(12, ts(c.getFecha_Pedido())); // @FechaCanc (igual que el WinForm)
            cs.setString(13, c.getCodEmp());             // @CodEmp
            cs.setInt(14, c.getCodPer());                // @UsuarioMod
            cs.setTimestamp(15, ts(c.getFechaVenc()));    // @FechaVenc
            cs.setString(16, c.getCondCli());            // @CondCli
            cs.execute();
        }
    }

    /**
     * Formas de pago de la venta: reemplaza las filas de DetalleFormasPago del pedido.
     * sp_Insert_CabeceraVentas solo graba una fila "Efectivo" por el total cuando no hay datos en
     * TempDetalleFormasPago; aquí se graban todas las formas de pago elegidas en la ventana.
     */
    public void ReemplazaDetalleFormasPago(Connection cn, PuntoVentaDTO c, List<PuntoVentaDTO> pagos) throws SQLException {
        try (PreparedStatement del = cn.prepareStatement(
                "DELETE FROM dbo.DetalleFormasPago WHERE Pedido_ID = ? AND CodEmp = ?")) {
            del.setString(1, c.getPedido_ID());
            del.setString(2, c.getCodEmp());
            del.executeUpdate();
        }
        try (PreparedStatement ins = cn.prepareStatement(
                "INSERT INTO dbo.DetalleFormasPago ([IDFormaPago],[Pedido_ID],[FormaPago],[MontoParcial],[PorcInc],"
              + "[MontoInc],[MontoReal],[IDBanco],[NroOperacion],[CodEmp]) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            for (PuntoVentaDTO p : pagos) {
                ins.setString(1, p.getIDFormaPago());
                ins.setString(2, c.getPedido_ID());
                ins.setString(3, p.getFormaPago());
                ins.setDouble(4, p.getMontoParcial());
                ins.setDouble(5, p.getPorcInc());
                ins.setDouble(6, p.getMontoInc());
                ins.setDouble(7, p.getMontoReal());
                ins.setString(8, limpio(p.getIDBanco()).isEmpty() ? "0" : p.getIDBanco());
                ins.setString(9, limpio(p.getNroOperacion()));
                ins.setString(10, c.getCodEmp());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    /** Cabecera de una venta dentro de la transacción (null si no existe). */
    public PuntoVentaDTO ListaVentaXPedido(Connection cn, String codEmp, String pedidoId) throws SQLException {
        String sql = "SELECT Pedido_ID, Tipo_documento, Venta_id, NroSerie, Nro_Documento, Cliente_ID, RazonSocial,"
                + " Fecha_Pedido, CondPago, FormaPago, SubTotal, IGV, Total, Estado, Impreso, FechaHora, FechaTurno, Turno, TipoDoc"
                + " FROM dbo.Cabecera_Ventas WHERE CodEmp = ? AND Pedido_ID = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codEmp);
            ps.setString(2, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leerCabecera(rs) : null;
            }
        }
    }

    private static PuntoVentaDTO leerCabecera(ResultSet rs) throws SQLException {
        PuntoVentaDTO v = new PuntoVentaDTO();
        v.setPedido_ID(texto(rs, "Pedido_ID"));
        v.setTipo_documento(texto(rs, "Tipo_documento"));
        v.setVenta_id(texto(rs, "Venta_id"));
        v.setNroSerie(texto(rs, "NroSerie"));
        v.setNro_Doc(texto(rs, "Nro_Documento"));
        v.setCliente_ID(texto(rs, "Cliente_ID"));
        v.setRazonSocial(texto(rs, "RazonSocial"));
        v.setFecha_Pedido(fecha(rs, "Fecha_Pedido"));
        v.setCondPago(texto(rs, "CondPago"));
        v.setFormaPago(texto(rs, "FormaPago"));
        v.setSubTotal(rs.getDouble("SubTotal"));
        v.setIGV(rs.getDouble("IGV"));
        v.setTotal(rs.getDouble("Total"));
        v.setEstadoPed(texto(rs, "Estado"));
        v.setImpreso(texto(rs, "Impreso"));
        Timestamp fh = rs.getTimestamp("FechaHora");
        v.setFechaHora(fh == null ? null : fh.toLocalDateTime());
        v.setFechaTurno(fecha(rs, "FechaTurno"));
        v.setTurno(texto(rs, "Turno"));
        v.setTipoDoc(texto(rs, "TipoDoc"));
        return v;
    }

    /** Modifica la cabecera (sp_Update_CabeceraVentas: también Nota_Venta / Boleta / Factura). */
    public void Update_CabeceraVentas(Connection cn, PuntoVentaDTO c) throws SQLException {
        try (CallableStatement cs = cn.prepareCall("{call sp_Update_CabeceraVentas(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setString(1, c.getPedido_ID());
            cs.setString(2, c.getTipo_documento());
            cs.setString(3, c.getVenta_id());
            cs.setString(4, c.getNroSerie());
            cs.setString(5, c.getNro_Doc());
            cs.setString(6, c.getCliente_ID());
            cs.setString(7, c.getRazonSocial());
            cs.setTimestamp(8, ts(c.getFecha_Pedido()));
            cs.setString(9, c.getCondPago());
            cs.setString(10, c.getFormaPago());
            cs.setDouble(11, c.getSubTotal());
            cs.setDouble(12, c.getIGV());
            cs.setDouble(13, c.getTotal());
            cs.setString(14, c.getEstadoPed());
            cs.setString(15, c.getCodEmp());
            cs.setString(16, c.getMontoLetras());
            cs.execute();
        }
        // sp_Update_CabeceraVentas declara @NroSerie char(3): una serie de 4 caracteres (B001, F001) queda
        // cortada ("B00"). Se vuelve a poner la serie original en las tablas que el procedimiento tocó.
        String serie = limpio(c.getNroSerie());
        if (serie.length() > 3) {
            ejecutar(cn, "UPDATE dbo.Cabecera_Ventas SET NroSerie = ? WHERE Pedido_ID = ? AND CodEmp = ?",
                    serie, c.getPedido_ID(), c.getCodEmp());
            if (DOC_BOLETA.equals(limpio(c.getTipoDoc()))) {
                ejecutar(cn, "UPDATE dbo.Boleta SET NroSerie = ? WHERE Boleta_ID = ? AND Pedido_ID = ?",
                        serie, c.getVenta_id(), c.getPedido_ID());
            } else if (DOC_FACTURA.equals(limpio(c.getTipoDoc()))) {
                ejecutar(cn, "UPDATE dbo.Factura SET NroSerie = ? WHERE Factura_ID = ? AND Pedido_ID = ?",
                        serie, c.getVenta_id(), c.getPedido_ID());
            }
        }
    }

    /**
     * Al modificar: devuelve al stock lo vendido y borra el detalle anterior (Detalle_Ventas y el detalle
     * del comprobante), para volver a grabarlo con sp_Insert_Detalle_Ventas.
     * Si no se hace, sp_Insert_Detalle_Ventas actualiza la fila existente y vuelve a descontar todo el stock.
     */
    public void RevierteDetalleVenta(Connection cn, PuntoVentaDTO c) throws SQLException {
        ejecutar(cn, "UPDATE P SET P.StockFinal = P.StockFinal + D.Cantidad"
                + " FROM dbo.Productos P INNER JOIN dbo.Detalle_Ventas D ON D.Producto_ID = P.Producto_ID"
                + " WHERE D.Pedido_ID = ?", c.getPedido_ID());
        ejecutar(cn, "DELETE FROM dbo.Detalle_Ventas WHERE Pedido_ID = ?", c.getPedido_ID());
        switch (limpio(c.getTipoDoc())) {
            case DOC_BOLETA:
                ejecutar(cn, "DELETE FROM dbo.Detalle_Boleta WHERE Boleta_ID = ?", c.getVenta_id());
                break;
            case DOC_FACTURA:
                ejecutar(cn, "DELETE FROM dbo.Detalle_Factura WHERE Factura_ID = ?", c.getVenta_id());
                break;
            default:
                ejecutar(cn, "DELETE FROM dbo.Detalle_Nota_Venta WHERE Nota_Venta_ID = ?", c.getVenta_id());
        }
    }

    /** Saldo pendiente de la cuenta por cobrar de la venta (null si la venta no tiene crédito). */
    public double[] MontoYSaldoCredito(Connection cn, String codEmp, String pedidoId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT Monto, SaldoCredito FROM dbo.CtasXCobrar WHERE NroRegistro = ? AND CodEmp = ?")) {
            ps.setString(1, pedidoId);
            ps.setString(2, codEmp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new double[] { rs.getDouble(1), rs.getDouble(2) } : null;
            }
        }
    }

    /** Quita la cuenta por cobrar de la venta (al pasar de crédito a contado o al eliminar la venta). */
    public void EliminaCtasXCobrar(Connection cn, String codEmp, String pedidoId) throws SQLException {
        ejecutar(cn, "DELETE FROM dbo.CtasXCobrar WHERE NroRegistro = ? AND CodEmp = ?", pedidoId, codEmp);
    }

    /**
     * Elimina la venta (sp_EliminaVenta): devuelve el stock y borra la cabecera, el detalle y el comprobante.
     *

     */
    public void EliminaVenta(Connection cn, PuntoVentaDTO c) throws SQLException {
        String tipoSp;
        switch (limpio(c.getTipoDoc())) {
            case DOC_BOLETA: tipoSp = "01"; break;
            case DOC_FACTURA: tipoSp = "02"; break;
            default: tipoSp = "03";             // nota de venta
        }
        try (CallableStatement cs = cn.prepareCall("{call sp_EliminaVenta(?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, c.getPedido_ID());
            cs.setString(2, c.getVenta_id());
            cs.setString(3, tipoSp);
            cs.setString(4, c.getCodEmp());
            cs.setTimestamp(5, ts(c.getFechaTurno()));
            cs.setString(6, c.getTurno());
            cs.execute();
        }
        // sp_EliminaVenta no borra las formas de pago ni el comprobante electrónico de la venta
        ejecutar(cn, "DELETE FROM dbo.DetalleFormasPago WHERE Pedido_ID = ? AND CodEmp = ?", c.getPedido_ID(), c.getCodEmp());
        ejecutar(cn, "DELETE FROM dbo.Cont_CabCompVentas WHERE Pedido_ID = ? AND CodEmp = ?", c.getPedido_ID(), c.getCodEmp());
    }

    /* ======================================================================================
     *  Membresías cobradas en el Punto de Venta (Generar comprobante [F4] de frm_RegistroMembresia)
     * ====================================================================================== */

    public static final String MARCA_SUSCRIPCION = "SUSC. ";

    /** Descripción de la línea de la membresía (máx. 100, como Detalle_Ventas.Descripcion). */
    public static String descripcionSuscripcion(String nombrePlan, String nroSuscripcion) {
        String sufijo = " (" + MARCA_SUSCRIPCION + limpio(nroSuscripcion) + ")";
        String nombre = "MEMBRESIA " + limpio(nombrePlan);
        int max = 100 - sufijo.length();
        return (nombre.length() > max ? nombre.substring(0, max) : nombre) + sufijo;
    }

    /** true si la línea del detalle es una membresía (lleva la marca de la suscripción). */
    public static boolean esDetalleSuscripcion(PuntoVentaDTO d) {
        return d != null && d.getDescripcion() != null && d.getDescripcion().contains("(" + MARCA_SUSCRIPCION);
    }

    /** N° de suscripción que lleva la línea en su descripción ("... (SUSC. 00000012)"), o "" si no es membresía. */
    public static String nroSuscripcion(PuntoVentaDTO d) {
        if (!esDetalleSuscripcion(d)) return "";
        String t = d.getDescripcion();
        int ini = t.lastIndexOf("(" + MARCA_SUSCRIPCION) + MARCA_SUSCRIPCION.length() + 1;
        int fin = t.indexOf(')', ini);
        return fin < 0 ? "" : t.substring(ini, fin).trim();
    }

    /**

     */
    public String[] EstadoSuscripcionBloqueo(Connection cn, String nroSuscripcion) throws SQLException {
        // Estado, Condicion, cliente_id y cuántas suscripciones ACTIVA / Pendiente del MISMO plan tiene ese
        // cliente (sp_Insert_Detalle_Ventas marca la más antigua de ese plan: debe ser esta).
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT ISNULL(RTRIM(s.Estado), ''), ISNULL(RTRIM(s.Condicion), ''), RTRIM(s.cliente_id),"
                + " (SELECT COUNT(*) FROM dbo.suscripciones_Membresia o"
                + "   WHERE o.cliente_id = s.cliente_id AND o.CodigoM = s.CodigoM"
                + "     AND o.Condicion = 'ACTIVA' AND o.Estado = 'Pendiente')"
                + " FROM dbo.suscripciones_Membresia s WITH (UPDLOCK, ROWLOCK)"
                + " WHERE s.NroSuscripcion_id = ?")) {
            ps.setString(1, nroSuscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new String[] { rs.getString(1), rs.getString(2), rs.getString(3), String.valueOf(rs.getInt(4)) }
                        : null;
            }
        }
    }

    /**
     * Al eliminar la venta o antes de rehacer su detalle al modificarla: las suscripciones cobradas en ella
     * (las que figuran en su Detalle_Ventas) vuelven de Efectiva a Pendiente. Una sola sentencia filtrada
     * por Pedido_ID; la suscripción se ubica por su número dentro de la descripción de la línea.
     */
    public int RevierteSuscripcionesVenta(Connection cn, String pedidoId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE s SET s.Estado = 'Pendiente'"
                + " FROM dbo.suscripciones_Membresia s"
                + " INNER JOIN dbo.Detalle_Ventas d"
                + "   ON d.Descripcion LIKE '%(" + MARCA_SUSCRIPCION + "' + RTRIM(s.NroSuscripcion_id) + ')%'"
                + " WHERE d.Pedido_ID = ? AND RTRIM(s.Estado) = 'Efectiva'")) {
            ps.setString(1, pedidoId);
            return ps.executeUpdate();
        }
    }

    private static void ejecutar(Connection cn, String sql, String... parametros) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setString(i + 1, parametros[i]);
            ps.executeUpdate();
        }
    }
}
