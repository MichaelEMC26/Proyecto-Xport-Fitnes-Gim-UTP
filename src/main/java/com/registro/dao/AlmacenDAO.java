/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.AlmacenDTO;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de Almacén (movimientos, existencias e inventarios).
 *
 * @author Ing
 */
public class AlmacenDAO {

    // ------------------------------------------------------------------ utilitarios

    /** Lee una fecha del ResultSet evitando errores con NULL. */
    private static LocalDateTime fecha(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toLocalDateTime();
    }

    /** Convierte LocalDateTime a java.sql.Date (solo la fecha) para los parámetros de los SP. */
    private static Date fecha(LocalDateTime valor) {
        return valor == null ? null : Date.valueOf(valor.toLocalDate());
    }

    /** Lee texto del ResultSet evitando errores con NULL. */
    private static String texto(ResultSet rs, String columna) throws SQLException {
        String s = rs.getString(columna);
        return s == null ? "" : s.trim();
    }

    /** Lee decimales del ResultSet evitando errores con NULL. */
    private static double numero(ResultSet rs, String columna) throws SQLException {
        double m = rs.getDouble(columna);
        return rs.wasNull() ? 0.0 : m;
    }

    // ------------------------------------------------------------------ consultas

    /** sp_ListaMovimientosDiarioProductos: movimientos de productos por venta diaria. */
    public List<AlmacenDTO> ListaMovimientosDiarioProductos(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin) {
        List<AlmacenDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaMovimientosDiarioProductos(?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.setString(1, CodEmp);
            cs.setDate(2, fecha(FechaIni));
            cs.setDate(3, fecha(FechaFin));

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    AlmacenDTO item = new AlmacenDTO();
                    item.setidproducto(texto(rs, "idproducto"));
                    item.setProductos(texto(rs, "Productos"));
                    item.setCantIngreso(numero(rs, "CantIngreso"));
                    item.setCantSalida(numero(rs, "CantSalida"));
                    item.setSaldoFinal(numero(rs, "SaldoFinal"));
                    item.setUnidadMedida(texto(rs, "UnidadMedida"));
                    item.setCategoria(texto(rs, "Categoria"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_ReporteExistenciasProductos: existencias valorizadas de productos. */
    public List<AlmacenDTO> ReporteExistenciasProductos(String CodEmp, String Filtro) {
        List<AlmacenDTO> lista = new ArrayList<>();
        String sql = "{call sp_ReporteExistenciasProductos(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.setString(1, CodEmp);
            cs.setString(2, Filtro);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    AlmacenDTO item = new AlmacenDTO();
                    item.setidproducto(texto(rs, "CodigoProd"));
                    item.setProductos(texto(rs, "NombreProducto"));
                    item.setCategoria_ID(rs.getInt("Categoria_ID"));
                    item.setCategoria(texto(rs, "Categoria"));
                    item.setMarcaProducto_ID(rs.getInt("MarcaProducto_ID"));
                    item.setMarcaProducto(texto(rs, "MarcaProducto"));
                    item.setDescripcion(texto(rs, "Descripcion"));
                    item.setUnidadMedida(texto(rs, "UnidadMedida"));
                    item.setStockFinal(numero(rs, "StockFinal"));
                    item.setPrecioCosto(numero(rs, "PrecioCosto"));
                    item.setImporteValorizado(numero(rs, "ImporteValorizado"));
                    item.setTipoBien(texto(rs, "TipoBien"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_ListaMontosExistenciasProductos: totales del reporte de existencias (una sola fila). */
    public AlmacenDTO ListaAcuentaPagoXFechasXTurno(String CodEmp, String Filtro) {
        String sql = "{call sp_ListaMontosExistenciasProductos(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.setString(1, CodEmp);
            cs.setString(2, Filtro);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    AlmacenDTO item = new AlmacenDTO();
                    item.setTotalValorizado(numero(rs, "TotalValorizado"));
                    item.setCantItems(numero(rs, "CantItems"));
                    item.setCantItemsStock(numero(rs, "CantItemsStock"));
                    item.setCantItemsStockMin(numero(rs, "CantItemsStockMin"));
                    item.setCantItemsSinStock(numero(rs, "CantItemsSinStock"));
                    return item;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** sp_Lista_InventarioXRangoFechas: inventarios registrados entre dos fechas. */
    public List<AlmacenDTO> ListaInventarioXRangoFechas(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin) {
        List<AlmacenDTO> lista = new ArrayList<>();
        String sql = "{call sp_Lista_InventarioXRangoFechas(?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.setString(1, CodEmp);
            cs.setDate(2, fecha(FechaIni));
            cs.setDate(3, fecha(FechaFin));

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    AlmacenDTO item = new AlmacenDTO();
                    item.setInventario_ID(texto(rs, "Inventario_ID"));
                    item.setFechaInicio(fecha(rs, "FechaInicio"));
                    item.setFechaCierre(fecha(rs, "FechaCierre"));
                    item.setPersonal_ID(rs.getInt("Personal_ID"));
                    item.setResponsable(texto(rs, "Responsable"));
                    item.setDescripcion(texto(rs, "Descripcion"));
                    item.settotalItemsBase(numero(rs, "totalItemsBase"));
                    item.settotalItemsAct(numero(rs, "totalItemsAct"));
                    item.setBaseImpon(numero(rs, "BaseImpon"));
                    item.setIGV(numero(rs, "IGV"));
                    item.setTotalCosto(numero(rs, "TotalCosto"));
                    item.setTotalVenta(numero(rs, "TotalVenta"));
                    item.setTotalMargen(numero(rs, "TotalMargen"));
                    item.setPromPorcen(numero(rs, "PromPorcen"));
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    // ------------------------------------------------------------------ procesos


    /** sp_GeneraMovimientoDiaStock: genera el movimiento de stock del día. */
    public boolean GeneraMovimientoDiaStock(AlmacenDTO a) {
        String sql = "{? = call sp_GeneraMovimientoDiaStock(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, a.getCodEmp());
            cs.setTimestamp(3, a.getFechaMovimiento() != null ? Timestamp.valueOf(a.getFechaMovimiento()) : null);
            cs.execute();
            return cs.getInt(1) == 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** sp_GeneraAperturaInventarioInicial: genera la apertura e inventario inicial (FechaInicio..FechaCierre). */
    public boolean GeneraAperturaInventarioInicial(AlmacenDTO a) {
        String sql = "{? = call sp_GeneraAperturaInventarioInicial(?,?,?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, a.getCodEmp());
            cs.setDate(3, fecha(a.getFechaInicio()));
            cs.setDate(4, fecha(a.getFechaCierre()));
            cs.setInt(5, a.getCodResp());
            cs.setString(6, a.getResponsable());
            cs.execute();
            return cs.getInt(1) == 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** sp_EliminaInventario: elimina el inventario, su detalle y sus movimientos de almacén. */
    public boolean EliminaInventario(AlmacenDTO a) {
        String sql = "{? = call sp_EliminaInventario(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, a.getCodEmp());
            cs.setString(3, a.getInventario_ID());
            cs.execute();
            return cs.getInt(1) == 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
