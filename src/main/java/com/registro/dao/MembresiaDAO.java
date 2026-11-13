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
    
    public ArrayList<MembresiaDTO> Consulta_HistorialMembresias()
    {
        ArrayList<MembresiaDTO> listaMembresia = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn = ConexionBD.obtenerConexion();
            String sql = "{sp_Consulta_HistorialMembresias(?,?,?,?)}";
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
            String sql = "{sp_generasuscripciones_Membresia(?)}";            
            st= cn.prepareCall(sql);
            st.setString(1, CodEmp);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                genera= new MembresiaDTO();
                genera.setCodigoM(rs.getString("NroSuscripcion_id"));
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
            String sql = "{sp_Listasuscripciones_MembresiaXRegistro(?,?)}";
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
            String sql = "{sp_InsertSuscripciones_Membresia(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            cs = cn.prepareCall(sql);
            
            cs.setString(1,membresias.getNroSuscripcion_id());
            cs.setString(2,membresias.getCliente());
            cs.setString(3,membresias.getCodigoM());
            cs.setDate(4,membresias.getFechaInicio());
            cs.setDate(1,membresias.getFechaFin());
            cs.setInt(6,membresias.getPersonal_ID());
            cs.setDouble(7,membresias.getSubTotal());
            cs.setDouble(8,membresias.getDescuento());
            cs.setDouble(9,membresias.getTotal());
            cs.setInt(10,membresias.getDias());
            cs.setString(11,membresias.getEstadoSusc());
            cs.setString(12,membresias.getCondicionSusc());
            cs.setString(13,membresias.getObservacionSusc());
            cs.setString(14,membresias.getCodEmp());
            
            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0) ResultadoExitoso = true;
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
            String sql = "{sp_Eliminasuscripciones_Membresia(?,?)}";
            cs = cn.prepareCall(sql);
            
            cs.setString(1,membresias.getCodEmp());
            cs.setString(2,membresias.getNroSuscripcion_id());
            
            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0) ResultadoExitoso = true;
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
    
    public MembresiaDTO ListaDetSuscripcionPeriodoClases(String NroSuscripcion_id, String Cliente_ID)
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{sp_ListaDetSuscripcionPeriodoClases(?,?)}";
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
    
    public MembresiaDTO ListaFrecuencia()
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{sp_ListaFrecuencia()}";
            st = cn.prepareCall(sql);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                lista = new MembresiaDTO();
                
                lista.setIDFrecuencia(rs.getInt("IDFrecuencia"));
                lista.setFrecuencia(rs.getString("Frecuencia"));
                lista.setEstado(rs.getString("Estado"));

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
    public MembresiaDTO ListaMembresia()
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{sp_ListaMembresia()}";
            st = cn.prepareCall(sql);
            
            rs = st.executeQuery();
            
            if(rs.next())
            {
                lista = new MembresiaDTO();
                
                lista.setCodigoM(rs.getString("CodigoM"));
                lista.setNombreMembresia(rs.getString("NombreMembresia"));
                lista.setDuracionDias(rs.getInt("Duracion_Dias"));
                lista.setDuracionSemanas(rs.getInt("Duracion_Semanas"));
                lista.setDuracionMeses(rs.getInt("Duracion_Meses"));
                lista.setDuracionAnios(rs.getInt("Duracion_Anios"));
                lista.setDiasLimite(rs.getInt("DiasLimite"));
                lista.setTarifa(rs.getDouble("Tarifa"));
                lista.setIDFrecuencia(rs.getInt("IDFrecuencia"));
                lista.setFrecuencia(rs.getString("Frecuencia"));
                lista.setEstado(rs.getString("Estado"));
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
    
    public MembresiaDTO Listasuscripciones_MembresiaXCodMembresia(String CodEmp, String CodigoM)
    {
        MembresiaDTO lista = null;
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        
        try
        {
            cn= ConexionBD.obtenerConexion();
            String sql = "{sp_Listasuscripciones_MembresiaXCodMembresia(?,?)}";
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
            String sql = "{sp_InsertMembresia(?,?,?,?,?,?,?,?,?,?)}";
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
            String sql = "{sp_UpdateMembresia(?,?,?,?,?,?,?,?,?,?)}";
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
            String sql = "{sp_DeleteMembresia(?)}";
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
            String sql = "{sp_InsertDiasMembresia(?,?,?)}";
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
            String sql = "{sp_DeleteDiasMembresia(?)}";
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
