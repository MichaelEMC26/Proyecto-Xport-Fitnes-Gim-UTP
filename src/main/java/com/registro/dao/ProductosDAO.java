/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.registro.config.ConexionBD;
import com.registro.dto.CategoriaDTO;
import com.registro.dto.MarcasDTO;
import com.registro.dto.ProductosDTO;
import com.registro.dto.UsuarioDTO;

/**
 *
 * @author Ing
 */
public class ProductosDAO {
    
  public ProductosDTO ListaTipoBien()
  {
      ProductosDTO TipoBien = null;
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_ListaTipo_Bien}";
          
          cs = cn.prepareCall(sql);
          rs = cs.executeQuery();

          if(rs.next())
          {
              TipoBien = new ProductosDTO();
              
              TipoBien.setTipoBien_ID(rs.getString("TipoBien"));
              
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
      
      return TipoBien;
  }
  
  public List<CategoriaDTO> ListaCategorias()
  {
      List<CategoriaDTO> listaCat = new ArrayList<>();
      
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_lista_categoriasAct}";
          
          cs = cn.prepareCall(sql);
          rs = cs.executeQuery();
          
          while(rs.next())
          {
              CategoriaDTO categoria = new CategoriaDTO();
              
              categoria.setCategoria_ID(rs.getInt("Categoria_ID"));
              categoria.setNombre(rs.getString("Nombre"));
              categoria.setEstado(rs.getString("Estado"));
              
              listaCat.add(categoria);
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
      
      return listaCat;
    
  }
  
  public List<MarcasDTO> ListaMarcas()
  {
      List<MarcasDTO> listaMarca = new ArrayList<>();
      
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_listaMarcaAct}";
          
          cs = cn.prepareCall(sql);
          rs = cs.executeQuery();
          
          while(rs.next())
          {
              MarcasDTO marca = new MarcasDTO();

              marca.setMarca_ID(rs.getInt("Marca_ID"));
              marca.setNombre(rs.getString("Nombre"));
              marca.setNombre(rs.getString("EstadoMarca"));
              
              listaMarca.add(marca);
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
      
      return listaMarca;
  }
  
  public List<ProductosDTO> ListaProductosGralActivos()
  {
      List<ProductosDTO> listaProductos = new ArrayList<>();
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_Lista_ProductosGral_Activos}";
          
          cs = cn.prepareCall(sql);
          rs = cs.executeQuery();
          
          while(rs.next())
          {
              ProductosDTO productos = new ProductosDTO();
              productos.setProducto_ID(rs.getString("Producto_ID"));
              productos.setNombreProducto(rs.getString("NombreProducto"));
              productos.setEstadoProducto(rs.getString("EstadoProducto"));
              productos.setCategoria_ID(rs.getInt("Categoria_ID"));
              productos.setMarcaProducto_ID(rs.getInt("MarcaProducto_ID"));
              productos.setDescripcionTipo(rs.getString("Descripcion"));
              productos.setUnidadMedida(rs.getString("UnidadMedida"));
              productos.setStockFinal(rs.getDouble("StockFinal"));
              productos.setPrecioCosto(rs.getDouble("PrecioCosto"));
              productos.setPrecioVenta(rs.getDouble("PrecioVenta"));
              productos.setPrecioLimite(rs.getDouble("PrecioLimite"));
              productos.setMargenUtil(rs.getDouble("MargenUtil"));
              productos.setPeso(rs.getDouble("Peso"));
              productos.setImagen(rs.getString("Imagen"));
              productos.setCodigoBarra(rs.getString("CodigoBarra"));
              productos.setCodEmp(rs.getString("CodEmp"));
              productos.setTipoProd(rs.getString("TipoProd"));
              productos.setTipoBien_ID(rs.getString("TipoBien_ID"));

              listaProductos.add(productos);
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
      
      return listaProductos;
  }
  
  public List<ProductosDTO> ListaProductosXNombre(String CodEmp,String NombreProducto)
  {
      List<ProductosDTO> listaProductosXNombre = new ArrayList<>();
      Connection cn = null;
      CallableStatement cs = null;
      ResultSet rs = null;
      
      try
      {
          cn = ConexionBD.obtenerConexion();
          String sql = "{call sp_Lista_ProductosGral_ActivosXNom(?,?)}";
          
          cs = cn.prepareCall(sql);
          cs.setString(1,CodEmp);
          cs.setString(2, NombreProducto);
          
          rs = cs.executeQuery();
          
          while(rs.next())
          {
              ProductosDTO productos = new ProductosDTO();
              productos.setProducto_ID(rs.getString("Producto_ID"));
              productos.setNombreProducto(rs.getString("NombreProducto"));
              productos.setEstadoProducto(rs.getString("EstadoProducto"));
              productos.setCategoria_ID(rs.getInt("Categoria_ID"));
              productos.setMarcaProducto_ID(rs.getInt("MarcaProducto_ID"));
              productos.setDescripcionTipo(rs.getString("Descripcion"));
              productos.setUnidadMedida(rs.getString("UnidadMedida"));
              productos.setStockFinal(rs.getDouble("StockFinal"));
              productos.setPrecioCosto(rs.getDouble("PrecioCosto"));
              productos.setPrecioVenta(rs.getDouble("PrecioVenta"));
              productos.setPrecioLimite(rs.getDouble("PrecioLimite"));
              productos.setMargenUtil(rs.getDouble("MargenUtil"));
              productos.setPeso(rs.getDouble("Peso"));
              productos.setImagen(rs.getString("Imagen"));
              productos.setCodigoBarra(rs.getString("CodigoBarra"));
              productos.setCodEmp(rs.getString("CodEmp"));
              productos.setTipoProd(rs.getString("TipoProd"));
              productos.setTipoBien_ID(rs.getString("TipoBien_ID"));

              listaProductosXNombre.add(productos);
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
      
      return listaProductosXNombre;
  }
  
  /**
     * MANTENIMIENTO DE CATEGORIAS DE PRODUCTOS
     * 
     * */
  public CategoriaDTO AutogeneraCategoria()
  {
        CategoriaDTO autogeneraCategoria = null;
        Connection cnn = null;
        CallableStatement cs = null;
        ResultSet rs = null;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_max_cod_categoria}";
            
            cs = cnn.prepareCall(sql);
            rs = cs.executeQuery();
            
            autogeneraCategoria = new CategoriaDTO();
            
            if(rs.next())
            {
                autogeneraCategoria.setCategoria_ID(rs.getInt("Categoria_ID"));
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
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
                if(rs != null){rs.close();}
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        
        return autogeneraCategoria;
    }
    
    public boolean Mante_Categorias(CategoriaDTO Categorias)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean registroExitoso = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql ="{call sp_Mante_Categorias(?,?,?,?)}";
            cs = cnn.prepareCall(sql);

            cs.setInt(1, Categorias.getCategoria_ID());
            cs.setString(2, Categorias.getNombre());
            cs.setString(3, Categorias.getEstado());
            cs.setString(4, Categorias.getrutaimagen());

            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){ registroExitoso = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}

            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return registroExitoso;
    }
    
    public boolean Elimina_Categorias(CategoriaDTO Categorias)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean categoriaEliminado = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql ="{call sp_Elimina_Categorias(?)}";
            cs = cnn.prepareCall(sql);

            cs.setInt(1, Categorias.getCategoria_ID());
            
            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){ categoriaEliminado = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}

            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return categoriaEliminado;
    }
    
  /**
     * MANTENIMIENTO DE MARCAS DE PRODUCTOS
     * 
     * */    
    public MarcasDTO AutogeneraMarca()
    {
        MarcasDTO autogeneraMarca = null;
        Connection cnn = null;
        CallableStatement cs = null;
        ResultSet rs = null;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_max_marc}";
            
            cs = cnn.prepareCall(sql);
            rs = cs.executeQuery();
            
            autogeneraMarca = new MarcasDTO();
            
            if(rs.next())
            {
                autogeneraMarca.setMarca_ID(rs.getInt("Marca_ID"));
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
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
                if(rs != null){rs.close();}
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        
        return autogeneraMarca;
    }
    
    public boolean Mante_Marcas(MarcasDTO marcas)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean registroExitoso = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql ="{call sp_Mante_marcas(?,?,?)}";
            cs = cnn.prepareCall(sql);

            cs.setInt(1, marcas.getMarca_ID());
            cs.setString(2, marcas.getNombre());
            cs.setString(3, marcas.getEstadoMarca());

            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){ registroExitoso = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}

            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return registroExitoso;
    }
    public boolean Elim_MarcaPro(MarcasDTO marcas)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean marcaEliminado = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql ="{call sp_Elim_MarcaPro(?)}";
            cs = cnn.prepareCall(sql);

            cs.setInt(1, marcas.getMarca_ID());
            
            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){ marcaEliminado = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}

            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return marcaEliminado;
    }

  /**
     * MANTENIMIENTO DE PRODUCTOS DE COMERCIALIZACION
     * 
     * */ 
    public ProductosDTO Autogenera_codprod()
    {
        ProductosDTO autogenera_codprod = null;
        Connection cnn = null;
        CallableStatement cs = null;
        ResultSet rs = null;
        
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_autogenera_codprod}";
            
            cs = cnn.prepareCall(sql);
            rs = cs.executeQuery();
            
            autogenera_codprod = new ProductosDTO();
            
            if(rs.next())
            {
                autogenera_codprod.setProducto_ID(rs.getString("Producto_ID"));
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
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
                if(rs != null){rs.close();}
            }
            catch(SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        
        return autogenera_codprod;
    }
    
    public boolean Insert_ProductosD(ProductosDTO producto)
    {
        Connection cnn = null;
        CallableStatement cs = null;

        boolean registroExitoso = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Insert_Productos(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            
            cs = cnn.prepareCall(sql);
            
            cs.setString(1,producto.getProducto_ID());
            cs.setString(2, producto.getNombreProducto());
            cs.setString(3, producto.getEstadoProducto()); 
            cs.setInt(4, producto.getCategoria_ID());   
            cs.setInt(5,producto.getMarcaProducto_ID());
            cs.setString(6, producto.getDescripcion());
            cs.setString(7, producto.getUnidadMedida());
            cs.setDouble(8, producto.getStockFinal());   
            cs.setDouble(9,producto.getPrecioCosto());
            cs.setDouble(10, producto.getPrecioVenta());
            cs.setDouble(11, producto.getPrecioLimite());   
            cs.setDouble(12,producto.getMargenUtil());
            cs.setDouble(13, producto.getPeso());
            cs.setString(14, producto.getImagen());   
            cs.setString(15,producto.getCodigoBarra());
            cs.setString(16, producto.getCodEmp());  
            cs.setString(17, producto.getTipoProd());  
            cs.setString(18, producto.getTipoBien_ID());  

            int filasAfectadas = cs.executeUpdate();
            if (filasAfectadas > 0 ) registroExitoso = true;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
            }
            catch(Exception ex){}
        }
        
        return registroExitoso;
    }
    
    public boolean Update_Productos(ProductosDTO producto)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean registroExitoso = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Update_Productos(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1,producto.getProducto_ID());
            cs.setString(2, producto.getNombreProducto());
            cs.setString(3, producto.getEstadoProducto()); 
            cs.setInt(4, producto.getCategoria_ID());   
            cs.setInt(5,producto.getMarcaProducto_ID());
            cs.setString(6, producto.getDescripcion());
            cs.setString(7, producto.getUnidadMedida());
            cs.setDouble(8, producto.getStockFinal());   
            cs.setDouble(9,producto.getPrecioCosto());
            cs.setDouble(10, producto.getPrecioVenta());
            cs.setDouble(11, producto.getPrecioLimite());   
            cs.setDouble(12,producto.getMargenUtil());
            cs.setDouble(13, producto.getPeso());
            cs.setString(14, producto.getImagen());   
            cs.setString(15,producto.getCodigoBarra());
            cs.setString(16, producto.getCodEmp());  
            cs.setString(17, producto.getTipoProd());  
            cs.setString(18, producto.getTipoBien_ID());  
            
            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){registroExitoso = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
            }
            catch(Exception ex){}
        }
        
        return registroExitoso;
    }
    public boolean Elim_Producto(ProductosDTO producto)
    {
        Connection cnn = null;
        CallableStatement cs = null;
        boolean productoEliminado = false;
        try
        {
            cnn = ConexionBD.obtenerConexion();
            String sql = "{call sp_Elim_Producto(?)}";
            cs = cnn.prepareCall(sql);
            
            cs.setString(1,producto.getProducto_ID());

            int filasAfectadas = cs.executeUpdate();
            if(filasAfectadas > 0){productoEliminado = true;}
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if(cnn != null){cnn.close();}
                if(cs != null){cs.close();}
            }
            catch(Exception ex){}
        }
        
        return productoEliminado;
    }
}
