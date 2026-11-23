package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.ClienteDTO;
import com.registro.dto.ProductosDTO;
import java.sql.*;
import java.util.ArrayList;

public class ClienteDAO {
    
    /** FechaCumpleaños si la consulta la devuelve (los SP antiguos no la traen). */
    private static java.time.LocalDateTime fechaCumpleanos(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (md.getColumnLabel(i).equalsIgnoreCase("FechaCumpleaños")) {
                Timestamp t = rs.getTimestamp(i);
                return t == null ? null : t.toLocalDateTime();
            }
        }
        return null;
    }

    /**
     * Si la tabla Cliente tiene la columna FechaCumpleaños (script sql/socios_fecha_cumpleanos.sql).
     * Se consulta una sola vez.
     */
    private static volatile Boolean tieneFechaCumpleanos;

    private static boolean tieneFechaCumpleanos(Connection cn) throws SQLException {
        Boolean v = tieneFechaCumpleanos;
        if (v == null) {
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COL_LENGTH('dbo.Cliente', 'FechaCumpleaños')")) {
                v = rs.next() && rs.getObject(1) != null;
            }
            tieneFechaCumpleanos = v;
        }
        return v;
    }

    /**
     * Socios activos para los listados (socios, asistencia, suscripciones, dashboard).
     *
     * Rendimiento: es la misma consulta que sp_lista_clientes_Activos (mismas columnas, mismo filtro y orden)
     * pero SIN la columna HuellaDactilar (varbinary(max)). La web no usa la huella en ningún listado y era
     * lo más pesado de la consulta: se descargaba la huella de todos los socios cada vez.
     * Para editar un socio se usa buscarPorId(), que sí trae la huella (sp_lista_ClienteXcod).
     */
    public ArrayList<ClienteDTO> ListaClientes() {
        ArrayList<ClienteDTO> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion()) {
            boolean conFecha = tieneFechaCumpleanos(cn);
            String sql = "SELECT Cliente_ID, RazonSocial, Direccion, Telefono, Estado, Telefono2, Correo, Sexo, RucDNI, TipoId,"
                    + " Foto, Contacto, TelefonoCont" + (conFecha ? ", FechaCumpleaños" : "")
                    + " FROM dbo.Cliente WHERE Estado = 'Activo' ORDER BY RazonSocial ASC";
            try (Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    ClienteDTO cliente = new ClienteDTO();
                    cliente.setCliente_ID(rs.getString("Cliente_ID"));
                    cliente.setRazonSocial(rs.getString("RazonSocial"));
                    cliente.setDireccion(rs.getString("Direccion"));
                    cliente.setTelefono(rs.getString("Telefono"));
                    cliente.setEstado(rs.getString("Estado"));
                    cliente.setTelefono2(rs.getString("Telefono2"));
                    cliente.setCorreo(rs.getString("Correo"));
                    cliente.setSexo(rs.getString("Sexo"));
                    cliente.setRucDNI(rs.getString("RucDNI"));
                    cliente.setTipoId(rs.getString("TipoId"));
                    cliente.setFoto(rs.getString("Foto"));
                    cliente.setContacto(rs.getString("Contacto"));
                    cliente.setTelefonoCont(rs.getString("TelefonoCont"));
                    if (conFecha) {
                        Timestamp t = rs.getTimestamp("FechaCumpleaños");
                        cliente.setFechaCumpleaños(t == null ? null : t.toLocalDateTime());
                    }
                    lista.add(cliente);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /** Cantidad de socios activos (para el dashboard: antes se descargaba toda la lista solo para contarla). */
    public int ContarClientesActivos() {
        try (Connection cn = ConexionBD.obtenerConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dbo.Cliente WHERE Estado = 'Activo'")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Socio por código, con su huella (sp_lista_ClienteXcod).
     * Antes descargaba la lista completa de socios para buscar uno.
     */
    public ClienteDTO buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return ListaClienteXCod(id.trim());
    }

    public ClienteDTO autogenera_CodCliente() {
        ClienteDTO cliente = null;

        // Igual que SisdataGimnasio (lista_ClientexCodD): el SP no recibe parámetros y
        // devuelve el nuevo código en la primera columna de la primera fila (Rows[0][0]).
        String sql = "{call sp_autogenera_CodClientes}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql);
             ResultSet rs = st.executeQuery()) {

            if (rs.next()) {
                String codigo = rs.getString(1);
                if (codigo != null && !codigo.isBlank()) {
                    cliente = new ClienteDTO();
                    cliente.setCliente_ID(codigo.trim());
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al generar el código del socio:");
            e.printStackTrace();
        }

        return cliente;
    }
    
    public boolean registrarCliente(ClienteDTO cliente) {
        Connection cn = null;
        CallableStatement st = null;
        boolean registroExitoso = false;
        try {
            cn = ConexionBD.obtenerConexion();
            // Corregido a 14 signos '?'
            String sql = "{call sp_Insert_Clientes(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, cliente.getCliente_ID());
            st.setString(2, cliente.getRazonSocial());
            st.setString(3, cliente.getDireccion());
            st.setString(4, cliente.getTelefono());
            st.setString(5, cliente.getEstado());
            st.setString(6, cliente.getTelefono2());
            st.setString(7, cliente.getCorreo());
            st.setString(8, cliente.getSexo());
            st.setString(9, cliente.getRucDNI());
            st.setString(10, cliente.getTipoId());
            st.setString(11, cliente.getFoto());
            st.setString(12, cliente.getContacto());
            st.setString(13, cliente.getTelefonoCont());
            st.setBytes(14, cliente.getHuellaDactilar());
            st.setTimestamp(15, cliente.getFechaCumpleaños() == null ? null : Timestamp.valueOf(cliente.getFechaCumpleaños()));
            
            int filasAfectadas = st.executeUpdate();
            if (filasAfectadas > 0) registroExitoso = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { 
                if (st != null) st.close(); 
                if (cn != null) cn.close(); 
            } catch (Exception e) {}
        }
        return registroExitoso;
    }

    public boolean actualizarCliente(ClienteDTO cliente) {
        Connection cn = null;
        CallableStatement st = null;
        boolean exito = false;
        try {
            cn = ConexionBD.obtenerConexion();
            // Stored procedure para actualizar cliente
            String sql = "{call sp_Actualiza_Cliente(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, cliente.getCliente_ID());
            st.setString(2, cliente.getRazonSocial());
            st.setString(3, cliente.getDireccion());
            st.setString(4, cliente.getTelefono());
            st.setString(5, cliente.getEstado());
            st.setString(6, cliente.getTelefono2());
            st.setString(7, cliente.getCorreo());
            st.setString(8, cliente.getSexo());
            st.setString(9, cliente.getRucDNI());
            st.setString(10, cliente.getTipoId());
            st.setString(11, cliente.getFoto());
            st.setString(12, cliente.getContacto());
            st.setString(13, cliente.getTelefonoCont());
            st.setBytes(14, cliente.getHuellaDactilar());
            st.setTimestamp(15, cliente.getFechaCumpleaños() == null ? null : Timestamp.valueOf(cliente.getFechaCumpleaños()));

            int filas = st.executeUpdate();
            if (filas > 0) exito = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (st != null) st.close();
                if (cn != null) cn.close();
            } catch (Exception e) {}
        }
        return exito;
    }

    public boolean EliminaCliente(ClienteDTO Cliente) {
        Connection cn = null;
        CallableStatement st = null;
        boolean EliminacionCorrecta = false;
        try {
            cn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Elimina_Cliente(?)}";
            st = cn.prepareCall(sql);
            st.setString(1, Cliente.getCliente_ID());
            int filasAfectadas = st.executeUpdate();
            if (filasAfectadas > 0) EliminacionCorrecta = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (st != null) st.close();
                if (cn != null) cn.close();
            } catch (Exception e) {}
        }
        return EliminacionCorrecta;
    }
    
    public ClienteDTO ListaClienteXCod(String Cliente_ID)
    {
      ClienteDTO listaCliente = null;
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_lista_ClienteXcod(?)}";
          
          cs = cn.prepareCall(sql);
          cs.setString(1,Cliente_ID);
          
          rs = cs.executeQuery();

          if(rs.next())
          {
              listaCliente = new ClienteDTO();
              
               listaCliente.setCliente_ID(rs.getString("Cliente_ID"));
               listaCliente.setRazonSocial(rs.getString("RazonSocial"));
               listaCliente.setDireccion(rs.getString("Direccion"));
               listaCliente.setTelefono(rs.getString("Telefono"));
               listaCliente.setEstado(rs.getString("Estado"));
               listaCliente.setTelefono2(rs.getString("Telefono2"));
               listaCliente.setCorreo(rs.getString("Correo"));
               listaCliente.setSexo(rs.getString("Sexo"));
               listaCliente.setRucDNI(rs.getString("RucDNI"));
               listaCliente.setTipoId(rs.getString("TipoId"));               
               listaCliente.setFoto(rs.getString("Foto"));
               listaCliente.setContacto(rs.getString("Contacto"));
               listaCliente.setTelefonoCont(rs.getString("TelefonoCont"));
               listaCliente.setHuellaDactilar(rs.getBytes("HuellaDactilar"));
               listaCliente.setFechaCumpleaños(fechaCumpleanos(rs));
              
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
          if(cs != null)cs.close();
          if(rs != null)rs.close();          
         }        
         catch(SQLException ex)
         {
             ex.printStackTrace();
         }
      }
      
      return listaCliente;
        
    }
    
    public ClienteDTO ListaClienteXRucDni(String RucDNI)
    {
      ClienteDTO listaCliente = null;
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_lista_ClienteXRucDni(?)}";
          
          cs = cn.prepareCall(sql);
          cs.setString(1,RucDNI);
          
          rs = cs.executeQuery();

          if(rs.next())
          {
              listaCliente = new ClienteDTO();
              
               listaCliente.setCliente_ID(rs.getString("Cliente_ID"));
               listaCliente.setRazonSocial(rs.getString("RazonSocial"));
               listaCliente.setDireccion(rs.getString("Direccion"));
               listaCliente.setTelefono(rs.getString("Telefono"));
               listaCliente.setEstado(rs.getString("Estado"));
               listaCliente.setTelefono2(rs.getString("Telefono2"));
               listaCliente.setCorreo(rs.getString("Correo"));
               listaCliente.setSexo(rs.getString("Sexo"));
               listaCliente.setRucDNI(rs.getString("RucDNI"));
               listaCliente.setTipoId(rs.getString("TipoId"));               
               listaCliente.setFoto(rs.getString("Foto"));
               listaCliente.setContacto(rs.getString("Contacto"));
               listaCliente.setTelefonoCont(rs.getString("TelefonoCont"));
               listaCliente.setHuellaDactilar(rs.getBytes("HuellaDactilar"));
               listaCliente.setFechaCumpleaños(fechaCumpleanos(rs));
              
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
          if(cs != null)cs.close();
          if(rs != null)rs.close();          
         }        
         catch(SQLException ex)
         {
             ex.printStackTrace();
         }
      }
      
      return listaCliente;
        
    }
     
    public ArrayList<ClienteDTO> ListaClientesXNombre(String RazonSocial) 
    {
        ArrayList<ClienteDTO> lista = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Busq_ClienteXnombre(?)}";
            st = cn.prepareCall(sql);
            
            st.setString(1, RazonSocial);
            rs = st.executeQuery();
            
            while (rs.next()) {
                ClienteDTO cliente = new ClienteDTO();
                cliente.setCliente_ID(rs.getString("Cliente_ID"));
                cliente.setRazonSocial(rs.getString("RazonSocial"));
                cliente.setDireccion(rs.getString("Direccion"));
                cliente.setTelefono(rs.getString("Telefono"));
                cliente.setEstado(rs.getString("Estado"));
                cliente.setTelefono2(rs.getString("Telefono2"));
                cliente.setCorreo(rs.getString("Correo"));
                cliente.setSexo(rs.getString("Sexo"));
                cliente.setRucDNI(rs.getString("RucDNI"));
                cliente.setTipoId(rs.getString("TipoId"));               
                cliente.setFoto(rs.getString("Foto"));
                cliente.setContacto(rs.getString("Contacto"));
                cliente.setTelefonoCont(rs.getString("TelefonoCont"));
                cliente.setHuellaDactilar(rs.getBytes("HuellaDactilar"));
                cliente.setFechaCumpleaños(fechaCumpleanos(rs));
                lista.add(cliente);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (st != null) st.close();
                if (cn != null) cn.close();
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
        return lista;
    } 
      
    /** sp_MuestraFechaCumpleañosActualSocio: socios que cumplen años en la fecha indicada. */
    public ArrayList<ClienteDTO> MuestraFechaCumpleañosActualSocio(Date FechaActual)
    {
        ArrayList<ClienteDTO> lista = new ArrayList<>();
        String sql = "{call sp_MuestraFechaCumpleañosActualSocio(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setDate(1, FechaActual);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    ClienteDTO c = new ClienteDTO();
                    c.setCliente_ID(rs.getString("Cliente_ID"));
                    c.setRazonSocial(rs.getString("RazonSocial"));
                    c.setTelefono(rs.getString("Telefono"));
                    c.setTelefono2(rs.getString("Telefono2"));
                    c.setCorreo(rs.getString("Correo"));
                    c.setContacto(rs.getString("Contacto"));
                    c.setTelefonoCont(rs.getString("TelefonoCont"));
                    Timestamp fc = rs.getTimestamp("FechaCumpleaños");
                    c.setFechaCumpleaños(fc == null ? null : fc.toLocalDateTime());
                    lista.add(c);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    public ArrayList<ClienteDTO> MuestraFechasProximasCumpleañosSocio(Date FechaIni,Date FechaFin) 
    {
        ArrayList<ClienteDTO> lista = new ArrayList<>();
        Connection cn = null;
        CallableStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionBD.obtenerConexion();
            String sql = "{call sp_MuestraFechasProximasCumpleañosSocio(?,?)}";
            st = cn.prepareCall(sql);
            
            st.setDate(1, FechaIni);
            st.setDate(2, FechaFin);
            rs = st.executeQuery();
            
            while (rs.next()) {
                ClienteDTO cliente = new ClienteDTO();
                cliente.setCliente_ID(rs.getString("Cliente_ID"));
                cliente.setRazonSocial(rs.getString("RazonSocial"));
                cliente.setTelefono(rs.getString("Telefono"));
                cliente.setTelefono2(rs.getString("Telefono2"));
                cliente.setCorreo(rs.getString("Correo"));
                cliente.setContacto(rs.getString("Contacto"));
                cliente.setTelefonoCont(rs.getString("TelefonoCont"));
                Timestamp fc = rs.getTimestamp("FechaCumpleaños");
                cliente.setFechaCumpleaños(fc == null ? null : fc.toLocalDateTime());
                
                lista.add(cliente);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (st != null) st.close();
                if (cn != null) cn.close();
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
        return lista;
    } 
}