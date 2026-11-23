/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import com.registro.dto.MembresiaDTO;
import com.registro.config.ConexionBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ing
 */

  // ---------------------------------------------------------
  // SUSCRIPCIONES DE MEMBRESIAS
  // ---------------------------------------------------------
public class MembresiaDAO {

    /** Códigos RETURN de los procedimientos de suscripción. */
    public static final int SP_OK = 0;
    public static final int SP_ERROR = -1;
    
    public ArrayList<MembresiaDTO> Consulta_HistorialMembresias()
    {
        ArrayList<MembresiaDTO> listaMembresia = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Consulta_HistorialMembresias(?,?,?,?)}";
            st= cn.prepareCall(sql);
            
            while(rs.next())
            {
                MembresiaDTO membresia = new MembresiaDTO();
                membresia.setRegistro(rs.getString("Registro"));
                membresia.setPedido(rs.getString("Pedido"));
                membresia.setDocumento(rs.getString("Documento"));
                membresia.setCliente(rs.getString("Cliente"));
                membresia.setMembresia(rs.getString("Membresia"));
                membresia.setFechaInicio(rs.getDate("FechaInicio"));
                membresia.setFechaFin(rs.getDate("FechaFin"));
                membresia.setDias(rs.getInt("Dias"));
                membresia.setVigente(rs.getString("Vigente"));
                membresia.setTotal(rs.getDouble("Total"));
                membresia.setCondicion(rs.getString("Condicion"));
                membresia.setVendedor(rs.getString("Vendedor"));
                membresia.setEstado(rs.getString("Estado"));
                
                listaMembresia.add(membresia);
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn != null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
        
        return listaMembresia;
    }
    
    public MembresiaDTO generasuscripciones_Membresia(String CodEmp)
    {
        MembresiaDTO genera = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn = ConexionBD.obtenerConexion();
            String sql = "{call sp_generasuscripciones_Membresia(?)}";            
            st= cn.prepareCall(sql);
            st.setString(1, CodEmp);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                genera= new MembresiaDTO();
                // El SP devuelve una columna sin nombre con el siguiente número
                genera.setNroSuscripcion_id(rs.getString(1));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return genera; 
    }
    
    public MembresiaDTO Listasuscripciones_MembresiaXRegistro(String CodEmp,String NroSuscripcion_id)
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_Listasuscripciones_MembresiaXRegistro(?,?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, CodEmp);
            st.setString(2, NroSuscripcion_id);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                lista = new MembresiaDTO();
                
                lista.setNroSuscripcion_id(rs.getString("NroSuscripcion_id"));
                lista.setcliente_id(rs.getString("cliente_id"));
                lista.setCodigoM(rs.getString("CodigoM"));
                lista.setFechaInicio(rs.getDate("FechaInicio"));
                lista.setFechaFin(rs.getDate("FechaFin"));
                lista.setPersonal_ID(rs.getInt("Personal_ID"));
                lista.setSubTotal(rs.getDouble("SubTotal"));
                lista.setDescuento(rs.getDouble("Descuento"));
                lista.setTotalSusc(rs.getDouble("Total"));
                lista.setEstadoSusc(rs.getString("Estado"));
                lista.setCondicionSusc(rs.getString("Condicion"));
                lista.setObservacionSusc(rs.getString("Observacion"));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return  lista; 
    }
    
    public boolean InsertSuscripciones_Membresia(MembresiaDTO membresias)
    {
        Connection cn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cn = ConexionBD.obtenerConexion();
            // El SP inserta o actualiza (según NroSuscripcion_id) y genera el detalle de clases.
            // Usa SET NOCOUNT ON y devuelve RETURN 0 (éxito) / -1 (error).
            String sql = "{? = call sp_InsertSuscripciones_Membresia(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            cs = cn.prepareCall(sql);

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2,membresias.getNroSuscripcion_id());
            cs.setString(3,membresias.getcliente_id());
            cs.setString(4,membresias.getCodigoM());
            cs.setDate(5,membresias.getFechaInicio());
            cs.setDate(6,membresias.getFechaFin());
            cs.setInt(7,membresias.getPersonal_ID());
            cs.setDouble(8,membresias.getSubTotal());
            cs.setDouble(9,membresias.getDescuento());
            cs.setDouble(10,membresias.getTotalSusc());
            cs.setInt(11,membresias.getDias());
            cs.setString(12,membresias.getEstadoSusc());
            cs.setString(13,membresias.getCondicionSusc());
            cs.setString(14,membresias.getObservacionSusc());
            cs.setString(15,membresias.getCodEmp());

            cs.execute();
            ResultadoExitoso = cs.getInt(1) == SP_OK;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn != null){cn.close();}
                if(cs != null){cs.close();}
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    
    public boolean Eliminasuscripciones_Membresia(MembresiaDTO membresias)
    {
        Connection cn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cn = ConexionBD.obtenerConexion();
            // Elimina el detalle de clases y la suscripción en una transacción.
            // Usa SET NOCOUNT ON y devuelve RETURN 0 (éxito) / -1 (error).
            String sql = "{? = call sp_Eliminasuscripciones_Membresia(?,?)}";
            cs = cn.prepareCall(sql);

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2,membresias.getCodEmp());
            cs.setString(3,membresias.getNroSuscripcion_id());

            cs.execute();
            ResultadoExitoso = cs.getInt(1) == SP_OK;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn != null){cn.close();}
                if(cs != null){cs.close();}
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    
    /**
     * Cronograma completo de la suscripción (sp_ListaDetSuscripcionPeriodoClases):
     * IDSecuencia, Rutina y Asistio, leídos por nombre (frm_RegistroMembresia.listarCronograma).
     */
    public ArrayList<MembresiaDTO> ListaCronogramaSuscripcion(String NroSuscripcion_id, String Cliente_ID)
    {
        ArrayList<MembresiaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaDetSuscripcionPeriodoClases(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {
            st.setString(1, NroSuscripcion_id);
            st.setString(2, Cliente_ID);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    MembresiaDTO det = new MembresiaDTO();
                    String sec = texto(rs, "IDSecuencia");
                    try {
                        det.setIDSecuencia(sec == null ? 0 : (int) Double.parseDouble(sec));
                    } catch (NumberFormatException ex) {
                        det.setIDSecuencia(0);
                    }
                    det.setRutina(texto(rs, "Rutina"));
                    det.setAsistio(siNo(texto(rs, "Asistio")));
                    lista.add(det);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /**
     * sp_Listasuscripciones_Membresia(@CodEmp, @Opc): suscripciones de la empresa.
     * Devuelve registro = NroSuscripcion_id, cliente = CodSocio, codigoM, membresia = Periodo,
     * fechaFin y condicion (se usa para Renovar membresía).
     */
    public ArrayList<MembresiaDTO> Listasuscripciones_Membresia(String CodEmp, int Opc)
    {
        ArrayList<MembresiaDTO> lista = new ArrayList<>();
        String sql = "{call sp_Listasuscripciones_Membresia(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {
            st.setString(1, CodEmp);
            st.setInt(2, Opc);
            try (ResultSet rs = st.executeQuery()) {
                if (!tieneColumna(rs, "CodSocio") || !tieneColumna(rs, "FechaFin")) return lista;
                while (rs.next()) {
                    MembresiaDTO s = new MembresiaDTO();
                    s.setRegistro(texto(rs, "NroSuscripcion_id"));
                    s.setCliente(texto(rs, "CodSocio"));
                    s.setCodigoM(texto(rs, "CodigoM"));
                    s.setMembresia(texto(rs, "Periodo"));
                    s.setCondicion(texto(rs, "Condicion"));
                    s.setFechaFin(fechaDe(rs, "FechaFin"));
                    // Resto de columnas (consulta de suscripciones, como frm_ConsultaMembresias)
                    s.setNroSuscripcion_id(s.getRegistro());
                    s.setcliente_id(s.getCliente());
                    s.setSocio(texto(rs, "SocioCliente"));
                    s.setNombreMembresia(s.getMembresia());
                    s.setDuracionDias(entero(rs, "Duracion"));
                    s.setFrecuencia(texto(rs, "Frecuencia"));
                    s.setFechaInicio(fechaDe(rs, "FechaInicio"));
                    s.setPersonal_ID(entero(rs, "Personal_ID"));
                    s.setSubTotal(decimal(rs, "SubTotal"));
                    s.setDescuento(decimal(rs, "Descuento"));
                    s.setTotal(decimal(rs, "Total"));
                    s.setDias(entero(rs, "DiasClases"));
                    s.setEstadoSusc(texto(rs, "Estado"));
                    s.setObservacionSusc(texto(rs, "Observacion"));
                    lista.add(s);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    private static boolean tieneColumna(ResultSet rs, String columna) throws SQLException
    {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (md.getColumnLabel(i).equalsIgnoreCase(columna)) return true;
        }
        return false;
    }

    private static Date fechaDe(ResultSet rs, String columna) throws SQLException
    {
        if (!tieneColumna(rs, columna)) return null;
        try {
            Timestamp f = rs.getTimestamp(columna);
            return f == null ? null : new Date(f.getTime());
        } catch (SQLException ex) {
            return null;
        }
    }

    private static int entero(ResultSet rs, String columna) throws SQLException
    {
        return (int) decimal(rs, columna);
    }

    private static double decimal(ResultSet rs, String columna) throws SQLException
    {
        String v = texto(rs, columna);
        try {
            return v == null || v.isEmpty() ? 0 : Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    private static String texto(ResultSet rs, String columna) throws SQLException
    {
        if (!tieneColumna(rs, columna)) return null;
        String v = rs.getString(columna);
        return v == null ? null : v.trim();
    }

    /** Asistio puede venir como bit (1/0, true/false) o texto. */
    private static String siNo(String v)
    {
        if (v == null) return "";
        if (v.equals("1") || v.equalsIgnoreCase("true")) return "Si";
        if (v.equals("0") || v.equalsIgnoreCase("false")) return "No";
        return v;
    }

    public MembresiaDTO ListaDetSuscripcionPeriodoClases(String NroSuscripcion_id, String Cliente_ID)
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_ListaDetSuscripcionPeriodoClases(?,?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, NroSuscripcion_id);
            st.setString(2, Cliente_ID);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                lista = new MembresiaDTO();
                
                lista.setNroSuscripcion_id(rs.getString("NroSuscripcion_id"));
                lista.setcliente_id(rs.getString("cliente_id"));
                lista.setRutina(rs.getString("Rutina"));
                lista.setIDSecuencia(rs.getInt("IDSecuencia"));
                lista.setAsistio(rs.getString("Asistio"));

            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return  lista; 
    }
    
  // ---------------------------------------------------------
  // PLANES DE MEMBRESIAS
  // ---------------------------------------------------------
    
    public ArrayList<MembresiaDTO> ListaFrecuencia()
    {
        ArrayList<MembresiaDTO> lista = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_ListaFrecuencia()}";
            st = cn.prepareCall(sql);
            
            rs = st.executeQuery();
            
            while(rs.next())
            {
                MembresiaDTO frecuencia = new MembresiaDTO();

                frecuencia.setIDFrecuencia(rs.getInt("IDFrecuencia"));
                frecuencia.setFrecuencia(rs.getString("Frecuencia"));
                frecuencia.setEstado(rs.getString("Estado"));

                lista.add(frecuencia);
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return  lista; 
    }
    public ArrayList<MembresiaDTO> ListaMembresia()
    {
        ArrayList<MembresiaDTO> lista = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;

        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_ListaMembresia()}";
            st = cn.prepareCall(sql);

            rs = st.executeQuery();

            while(rs.next())
            {
                MembresiaDTO plan = new MembresiaDTO();

                plan.setCodigoM(rs.getString("CodigoM"));
                plan.setNombreMembresia(rs.getString("NombreMembresia"));
                plan.setDuracionDias(rs.getInt("Duracion_Dias"));
                plan.setDuracionSemanas(rs.getInt("Duracion_Semanas"));
                plan.setDuracionMeses(rs.getInt("Duracion_Meses"));
                plan.setDuracionAnios(rs.getInt("Duracion_Anios"));
                plan.setDiasLimite(rs.getInt("DiasLimite"));
                plan.setTarifa(rs.getDouble("Tarifa"));
                plan.setIDFrecuencia(rs.getInt("IDFrecuencia"));
                plan.setFrecuencia(rs.getString("Frecuencia"));
                plan.setEstado(rs.getString("Estado"));

                lista.add(plan);
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return  lista; 
    }
    
    public MembresiaDTO ListaMembresiaXCod(String CodigoM)
    {
        MembresiaDTO plan = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;

        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_ListaMembresiaXCod(?)}";
            st = cn.prepareCall(sql);
            st.setString(1, CodigoM);

            rs = st.executeQuery();

            if(rs.next())
            {
                plan = new MembresiaDTO();

                plan.setCodigoM(rs.getString("CodigoM"));
                plan.setNombreMembresia(rs.getString("NombreMembresia"));
                plan.setDuracionDias(rs.getInt("Duracion_Dias"));
                plan.setDuracionSemanas(rs.getInt("Duracion_Semanas"));
                plan.setDuracionMeses(rs.getInt("Duracion_Meses"));
                plan.setDuracionAnios(rs.getInt("Duracion_Anios"));
                plan.setDiasLimite(rs.getInt("DiasLimite"));
                plan.setTarifa(rs.getDouble("Tarifa"));
                plan.setIDFrecuencia(rs.getInt("IDFrecuencia"));
                plan.setFrecuencia(rs.getString("Frecuencia"));
                plan.setEstado(rs.getString("Estado"));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
        }

        return plan;
    }

    /** Siguiente código de plan (sp_autogenera_codMembresia), p. ej. "0005". */
    public String autogeneraCodMembresia()
    {
        String codigo = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;

        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_autogenera_codMembresia}";
            st = cn.prepareCall(sql);

            rs = st.executeQuery();

            if(rs.next())
            {
                // El SP devuelve una columna sin nombre
                codigo = rs.getString(1);
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
        }

        return codigo;
    }

    public MembresiaDTO Listasuscripciones_MembresiaXCodMembresia(String CodEmp, String CodigoM)
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{call sp_Listasuscripciones_MembresiaXCodMembresia(?,?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, CodEmp);
            st.setString(2, CodigoM);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                lista = new MembresiaDTO();
                
                lista.setNroSuscripcion_id(rs.getString("NroSuscripcion_id"));
                lista.setcliente_id(rs.getString("cliente_id"));
                lista.setCodigoM(rs.getString("CodigoM"));
                lista.setFechaInicio(rs.getDate("FechaInicio"));
                lista.setFechaFin(rs.getDate("FechaFin"));
                lista.setPersonal_ID(rs.getInt("Personal_ID"));
                lista.setSubTotal(rs.getDouble("SubTotal"));
                lista.setDescuento(rs.getDouble("Descuento"));
                lista.setTotal(rs.getInt("Total"));
                lista.setEstado(rs.getString("Estado"));
                lista.setCondicion(rs.getString("Condicion"));
                lista.setObservacionSusc(rs.getString("Observacion"));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cn!=null)cn.close();
                if(st != null)st.close();
                if(rs != null)rs.close();
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
           
        }
        
        return  lista; 
    }
    
    public boolean InsertMembresia(MembresiaDTO membresia)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_InsertMembresia(?,?,?,?,?,?,?,?,?,?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1, membresia.getCodigoM());
            cs.setString(2, membresia.getNombreMembresia());
            cs.setInt(3, membresia.getDuracionDias());
            cs.setInt(4, membresia.getDuracionSemanas());
            cs.setInt(5, membresia.getDuracionMeses());
            cs.setInt(6, membresia.getDuracionAnios());
            cs.setInt(7, membresia.getDiasLimite());
            cs.setDouble(8, membresia.getTarifa());
            cs.setString(9, membresia.getEstado());
            cs.setInt(10, membresia.getIDFrecuencia());
            
            int FilasAfectadas = cs.executeUpdate();
            if(FilasAfectadas > 0) ResultadoExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null)cnn.close();
                if(cs != null)cs.close();
                
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    public boolean UpdateMembresia(MembresiaDTO membresia)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_UpdateMembresia(?,?,?,?,?,?,?,?,?,?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1, membresia.getCodigoM());
            cs.setString(2, membresia.getNombreMembresia());
            cs.setInt(3, membresia.getDuracionDias());
            cs.setInt(4, membresia.getDuracionSemanas());
            cs.setInt(5, membresia.getDuracionMeses());
            cs.setInt(6, membresia.getDuracionAnios());
            cs.setInt(7, membresia.getDiasLimite());
            cs.setDouble(8, membresia.getTarifa());
            cs.setString(9, membresia.getEstado());
            cs.setInt(10, membresia.getIDFrecuencia());
            
            int FilasAfectadas = cs.executeUpdate();
            if(FilasAfectadas > 0) ResultadoExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null)cnn.close();
                if(cs != null)cs.close();
                
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    public boolean DeleteMembresia(MembresiaDTO membresia)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_DeleteMembresia(?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1, membresia.getCodigoM());
            
            int FilasAfectadas = cs.executeUpdate();
            if(FilasAfectadas > 0) ResultadoExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null)cnn.close();
                if(cs != null)cs.close();
                
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    public boolean InsertDiasMembresia(MembresiaDTO membresia)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_InsertDiasMembresia(?,?,?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1,membresia.getCodigoM());
            cs.setString(2,membresia.getCodDia());
            cs.setString(3,membresia.getDia());
            
            int FilasAfectadas = cs.executeUpdate();
            if(FilasAfectadas > 0) ResultadoExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null)cnn.close();
                if(cs != null)cs.close();
                
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
    public boolean DeleteDiasMembresia(MembresiaDTO membresia)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        
        boolean ResultadoExitoso = false;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_DeleteDiasMembresia(?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1,membresia.getCodigoM());
            
            int FilasAfectadas = cs.executeUpdate();
            if(FilasAfectadas > 0) ResultadoExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null)cnn.close();
                if(cs != null)cs.close();                
            }
            catch(Exception ex)
            {
                
            }
        }
        
        return ResultadoExitoso;
    }
}
