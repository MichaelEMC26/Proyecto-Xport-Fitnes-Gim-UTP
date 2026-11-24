package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.ConfiguracionesDTO;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarjetas de pago (tabla TarjetasPago). Mismos procedimientos que Ven_FacturaD del WinForm:
 * sp_ListaTarjetasPago, sp_ListaTarjetasPagoActivo, sp_ListaTarjetasPagoXCodPago, sp_ListaTarjetasPagoXCod,
 * sp_InsertTarjetasPago, sp_UpdateTarjetasPago y sp_DeleteTarjetasPago.
 *
 * @author Ing
 */
public class ConfiguracionesDAO {

    /** Tabla propia: no cuenta como referencia al eliminar. */
    private static final String TABLA_TARJETAS = "TarjetasPago";

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<ConfiguracionesDTO> listaTarjetasPago() {
        return listar("{call sp_ListaTarjetasPago}", null);
    }

    public List<ConfiguracionesDTO> ListaTarjetasPagoActivo() {
        return listar("{call sp_ListaTarjetasPagoActivo}", null);
    }

    /** Tarjetas de una forma de pago (@IDFormaPago varchar(2)). */
    public List<ConfiguracionesDTO> ListaTarjetasPagoXCodPago(String IDFormaPago) {
        return listar("{call sp_ListaTarjetasPagoXCodPago(?)}", IDFormaPago);
    }

    /** Tarjeta por su código (@IDTarjeta varchar(3)); lista vacía si no existe. */
    public List<ConfiguracionesDTO> ListaTarjetasPagoXCod(String IDTarjeta) {
        return listar("{call sp_ListaTarjetasPagoXCod(?)}", IDTarjeta);
    }

    private List<ConfiguracionesDTO> listar(String sql, String parametro) {
        List<ConfiguracionesDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            // El parámetro se asigna ANTES de ejecutar la consulta
            if (parametro != null) cs.setString(1, parametro.trim());
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) lista.add(leer(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar las tarjetas de pago: " + ex.getMessage(), ex);
        }
        return lista;
    }

    /** Una fila de TarjetasPago (select *). IDTarjeta es varchar: se lee como texto. */
    private static ConfiguracionesDTO leer(ResultSet rs) throws SQLException {
        ConfiguracionesDTO item = new ConfiguracionesDTO();
        item.setIDTarjeta(texto(rs, "IDTarjeta"));
        item.setNombreTarjetaPago(texto(rs, "NombreTarjetaPago"));
        item.setEstado(texto(rs, "Estado"));
        item.setPorCom(tieneColumna(rs, "PorCom") ? rs.getDouble("PorCom") : 0);
        item.setIDFormaPago(texto(rs, "IDFormaPago"));
        return item;
    }

    // ------------------------------------------------------------------
    // Grabar / modificar / eliminar (RETURN 0 = correcto, como los SP del WinForm)
    // ------------------------------------------------------------------

    public boolean InsertTarjetasPago(ConfiguracionesDTO t) {
        return grabar("{? = call sp_InsertTarjetasPago(?,?,?,?,?)}", t);
    }

    public boolean UpdateTarjetasPago(ConfiguracionesDTO t) {
        return grabar("{? = call sp_UpdateTarjetasPago(?,?,?,?,?)}", t);
    }

    private boolean grabar(String sql, ConfiguracionesDTO t) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, t.getIDTarjeta());
            cs.setString(3, t.getNombreTarjetaPago());
            cs.setString(4, t.getEstado());
            cs.setFloat(5, (float) t.getPorCom());          // @PorCom real
            if (t.getIDFormaPago() == null || t.getIDFormaPago().isBlank()) cs.setNull(6, Types.VARCHAR);
            else cs.setString(6, t.getIDFormaPago().trim());
            cs.execute();
            return cs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo grabar la tarjeta de pago: " + ex.getMessage(), ex);
        }
    }

    public boolean DeleteTarjetasPago(ConfiguracionesDTO t) {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{? = call sp_DeleteTarjetasPago(?)}")) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, t.getIDTarjeta());
            cs.execute();
            return cs.getInt(1) == 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo eliminar la tarjeta de pago: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * IDFormaPago de la forma de pago "Tarjeta" (TarjetasPago.IDFormaPago no admite NULL).

     */
    public String IDFormaPagoTarjeta() {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            try (CallableStatement cs = cn.prepareCall("{call sp_ListaFormasPago}");
                 ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String nombre = rs.getString(2);
                    if (id != null && !id.isBlank() && nombre != null
                            && nombre.toUpperCase(java.util.Locale.ROOT).contains("TARJETA")) {
                        return id.trim();
                    }
                }
            } catch (SQLException ex) {
                System.err.println("sp_ListaFormasPago: " + ex.getMessage());   // se intenta el paso 2
            }
            try (java.sql.PreparedStatement ps = cn.prepareStatement(
                    "SELECT TOP 1 LTRIM(RTRIM(IDFormaPago)) FROM TarjetasPago"
                  + " WHERE IDFormaPago IS NOT NULL AND LTRIM(RTRIM(IDFormaPago)) <> ''");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo obtener la forma de pago Tarjeta: " + ex.getMessage(), ex);
        }
    }

    /**
     * Siguiente código de tarjeta (sp_GeneraCodigoTarjeta: MAX(IDTarjeta) + 1 con 2 dígitos, ej. "03").
     * El procedimiento devuelve una sola columna sin nombre; se lee por posición.
     */
    public ConfiguracionesDTO GeneraCodigoTarjeta() {
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall("{call sp_GeneraCodigoTarjeta}");
             ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                ConfiguracionesDTO GeneraCodigo = new ConfiguracionesDTO();
                String cod = rs.getString(1);
                GeneraCodigo.setIDTarjeta(cod == null ? null : cod.trim());
                return GeneraCodigo;
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo generar el código de la tarjeta: " + ex.getMessage(), ex);
        }
    }
    // ------------------------------------------------------------------
    // Validación para eliminar: el código no debe estar en otra tabla
    // ------------------------------------------------------------------

    /**
     * Tablas (distintas de TarjetasPago) que tienen la columna IDTarjeta y al menos un registro con este código.

     */
    public List<String> TablasQueUsanTarjeta(String IDTarjeta) {
        List<String> usadas = new ArrayList<>();
        String buscarTablas =
                "SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME"
              + " FROM INFORMATION_SCHEMA.COLUMNS c"
              + " JOIN INFORMATION_SCHEMA.TABLES t ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME"
              + " WHERE t.TABLE_TYPE = 'BASE TABLE' AND c.COLUMN_NAME = 'IDTarjeta' AND c.TABLE_NAME <> ?";

        try (Connection cn = ConexionBD.obtenerConexion()) {
            List<String[]> tablas = new ArrayList<>();
            try (PreparedStatement ps = cn.prepareStatement(buscarTablas)) {
                ps.setString(1, TABLA_TARJETAS);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) tablas.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3) });
                }
            }
            for (String[] tb : tablas) {
                // Nombres tomados del catálogo de la base (no del usuario), entre corchetes
                String sql = "SELECT TOP 1 1 FROM " + corchetes(tb[0]) + "." + corchetes(tb[1])
                           + " WHERE LTRIM(RTRIM(CAST(" + corchetes(tb[2]) + " AS varchar(20)))) = ?";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setString(1, IDTarjeta == null ? "" : IDTarjeta.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) usadas.add(tb[1]);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo verificar si la tarjeta está en uso: " + ex.getMessage(), ex);
        }
        return usadas;
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private static String corchetes(String nombre) {
        return "[" + nombre.replace("]", "]]") + "]";
    }

    private static boolean tieneColumna(ResultSet rs, String columna) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (md.getColumnLabel(i).equalsIgnoreCase(columna)) return true;
        }
        return false;
    }

    private static String texto(ResultSet rs, String columna) throws SQLException {
        if (!tieneColumna(rs, columna)) return "";
        String s = rs.getString(columna);
        return s == null ? "" : s.trim();
    }
}
