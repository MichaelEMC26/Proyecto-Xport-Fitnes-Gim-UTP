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
import com.registro.dto.CatalogoDTO;
import com.registro.dto.UsuarioDTO;

/**
 *
 * @author Ing
 */
public class CatalogoDAO {
    
  /**
   * sp_ListaTipo_Bien (SELECT * FROM Tipo_Bien): tipos de bien para el combo de RegistrarProducto.
   * Se lee el código (TipoBien_ID), la descripción y el estado; si la tabla usa otros nombres de
   * columna se toma la primera como código y la segunda como descripción.
   */
  public List<CatalogoDTO> ListaTipoBien()
  {
      List<CatalogoDTO> lista = new ArrayList<>();
      String sql = "{call sp_ListaTipo_Bien}";
      try (Connection cn = ConexionBD.obtenerConexion();
           CallableStatement cs = cn.prepareCall(sql);
           ResultSet rs = cs.executeQuery())
      {
          int columnas = rs.getMetaData().getColumnCount();
          while (rs.next())
          {
              CatalogoDTO tipo = new CatalogoDTO();
              String id = columnaOpcional(rs, "TipoBien_ID");
              if (id == null) id = rs.getString(1);
              String descripcion = columnaOpcional(rs, "Descripcion");
              if (descripcion == null && columnas > 1) descripcion = rs.getString(2);
              tipo.setTipoBien_ID(recortar(id));
              tipo.setDescripcionTipo(descripcion == null ? recortar(id) : descripcion.trim());
              tipo.setEstadoTipo(recortar(columnaOpcional(rs, "Estado")));
              lista.add(tipo);
          }
      }
      catch (SQLException ex)
      {
          ex.printStackTrace();
      }
      return lista;
  }

  /**
   * StockMinimo y TipoBien_ID de los productos de la empresa (clave: Producto_ID).
   * Completa los listados cuyos SP todavía no devuelven esas columnas.
   */
  public java.util.Map<String, CatalogoDTO> StockMinimoYTipoBien(String codEmp)
  {
      java.util.Map<String, CatalogoDTO> mapa = new java.util.HashMap<>();
      String sql = "SELECT Producto_ID, StockMinimo = ISNULL(StockMinimo, 0), TipoBien_ID FROM Productos WHERE CodEmp = ?";
      try (Connection cn = ConexionBD.obtenerConexion();
           PreparedStatement ps = cn.prepareStatement(sql))
      {
          ps.setString(1, codEmp);
          try (ResultSet rs = ps.executeQuery())
          {
              while (rs.next())
              {
                  CatalogoDTO p = new CatalogoDTO();
                  p.setProducto_ID(recortar(rs.getString("Producto_ID")));
                  p.setStockMinimo(rs.getDouble("StockMinimo"));
                  p.setTipoBien_ID(recortar(rs.getString("TipoBien_ID")));
                  mapa.put(p.getProducto_ID(), p);
              }
          }
      }
      catch (SQLException ex)
      {
          ex.printStackTrace();
      }
      return mapa;
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
              // rutaimagen solo si el SP la devuelve: al editar se reenvía tal cual
              // a sp_Mante_Categorias para no borrar la imagen de la categoría.
              categoria.setrutaimagen(columnaOpcional(rs, "rutaimagen"));
              
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

              // sp_listaMarcaAct devuelve MarcaProducto_ID, Nombre, EstadoMarca
              marca.setMarca_ID(rs.getInt("MarcaProducto_ID"));
              marca.setNombre(rs.getString("Nombre"));
              marca.setEstadoMarca(rs.getString("EstadoMarca"));
              
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
  
  /**
     * MANTENIMIENTO DE CATEGORIAS DE PRODUCTOS
     * 
     * */
  /** sp_max_cod_categoria: siguiente Categoria_ID (la columna no tiene nombre, se lee la primera). */
  public CategoriaDTO AutogeneraCategoria()
  {
        CategoriaDTO autogeneraCategoria = new CategoriaDTO();
        String sql = "{call sp_max_cod_categoria}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql);
             ResultSet rs = cs.executeQuery())
        {
            if(rs.next())
            {
                autogeneraCategoria.setCategoria_ID(rs.getInt(1));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        return autogeneraCategoria;
    }
    
    /**
     * sp_Mante_Categorias: inserta si el Categoria_ID no existe, si existe actualiza Nombre y rutaimagen.
     * El SP usa SET NOCOUNT ON (executeUpdate no informa filas) y captura sus errores con
     * RETURN 0 / RETURN -1, por eso el resultado se toma del valor de retorno.
     */
    public boolean Mante_Categorias(CategoriaDTO Categorias)
    {
        String sql = "{? = call sp_Mante_Categorias(?,?,?,?)}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql))
        {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, Categorias.getCategoria_ID());
            cs.setString(3, Categorias.getNombre());
            cs.setString(4, Categorias.getEstado());
            cs.setString(5, Categorias.getrutaimagen());
            cs.execute();
            return cs.getInt(1) == 0;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * sp_Elimina_Categorias: si la categoría tiene productos la deja INACTIVO, si no la borra.
     * Éxito cuando el SP devuelve RETURN 0.
     */
    public boolean Elimina_Categorias(CategoriaDTO Categorias)
    {
        String sql = "{? = call sp_Elimina_Categorias(?)}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql))
        {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, Categorias.getCategoria_ID());
            cs.execute();
            return cs.getInt(1) == 0;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }
    
  /**
     * MANTENIMIENTO DE MARCAS DE PRODUCTOS
     * 
     * */    
    /** sp_max_marc: siguiente MarcaProducto_ID (la columna no tiene nombre, se lee la primera). */
    public MarcasDTO AutogeneraMarca()
    {
        MarcasDTO autogeneraMarca = new MarcasDTO();
        String sql = "{call sp_max_marc}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql);
             ResultSet rs = cs.executeQuery())
        {
            if(rs.next())
            {
                autogeneraMarca.setMarca_ID(rs.getInt(1));
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
        }
        return autogeneraMarca;
    }
    
    /**
     * sp_Mante_marcas: inserta si el MarcaProducto_ID no existe, si existe actualiza Nombre y EstadoMarca.
     * Igual que en categorías, el éxito se toma del RETURN 0 / -1 del SP.
     */
    public boolean Mante_Marcas(MarcasDTO marcas)
    {
        String sql = "{? = call sp_Mante_marcas(?,?,?)}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql))
        {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, marcas.getMarca_ID());
            cs.setString(3, marcas.getNombre());
            cs.setString(4, marcas.getEstadoMarca());
            cs.execute();
            return cs.getInt(1) == 0;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }
    /**
     * sp_Elim_MarcaPro: si la marca tiene productos la deja INACTIVO, si no la borra.
     * Éxito cuando el SP devuelve RETURN 0.
     */
    public boolean Elim_MarcaPro(MarcasDTO marcas)
    {
        String sql = "{? = call sp_Elim_MarcaPro(?)}";
        try (Connection cnn = ConexionBD.obtenerConexion();
             CallableStatement cs = cnn.prepareCall(sql))
        {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, marcas.getMarca_ID());
            cs.execute();
            return cs.getInt(1) == 0;
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Categoria.rutaimagen de una categoría (null si no tiene o no existe).
     * Se usa al editar cuando sp_lista_categoriasAct no devuelve esa columna, para que
     * sp_Mante_Categorias no borre la imagen al actualizar solo el nombre.
     */
    public String RutaImagenCategoria(int categoriaId)
    {
        String sql = "SELECT rutaimagen FROM Categoria WHERE Categoria_ID = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql))
        {
            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() ? rs.getString(1) : null;
            }
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    /** Valor de una columna que puede no venir en el SELECT del procedimiento (null si no existe). */
    private static String columnaOpcional(ResultSet rs, String columna) throws SQLException
    {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++)
        {
            if (md.getColumnLabel(i).equalsIgnoreCase(columna))
            {
                return rs.getString(i);
            }
        }
        return null;
    }

  /**
     * MANTENIMIENTO DE PRODUCTOS DE COMERCIALIZACION
     * */

    /** sp_Lista_ProductosGral_Activos @CodEmp: productos activos de la empresa. */
    public List<CatalogoDTO> ListaProductosActivos(String codEmp)
    {
        List<CatalogoDTO> lista = new ArrayList<>();
        String sql = "{call sp_Lista_ProductosGral_Activos(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString("CodEmp", codEmp);
            leerListado(cs, lista);
            completarStockMinimo(lista, codEmp);
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_buscaProdxNomTotAct @CodEmp, @descprod: búsqueda por nombre/descripción (frm_BusquedaProd). */
    public List<CatalogoDTO> BuscaProductosXNombre(String codEmp, String texto)
    {
        List<CatalogoDTO> lista = new ArrayList<>();
        String sql = "{call sp_buscaProdxNomTotAct(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString("CodEmp", codEmp);
            cs.setString("descprod", texto);
            leerListado(cs, lista);
            completarStockMinimo(lista, codEmp);
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return lista;
    }

    /** sp_buscaProdxMarcaActivo @CodEmp, @MarcaProducto_ID: productos activos de una marca. */
    public List<CatalogoDTO> BuscaProductosXMarca(String codEmp, int marcaId)
    {
        List<CatalogoDTO> lista = new ArrayList<>();
        String sql = "{call sp_buscaProdxMarcaActivo(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString("CodEmp", codEmp);
            cs.setString("MarcaProducto_ID", String.valueOf(marcaId));
            leerListado(cs, lista);
            completarStockMinimo(lista, codEmp);
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return lista;
    }

    /** Columnas comunes de los listados: las mismas que ordena frm_Productos.OrdenaGrid(). */
    private void leerListado(CallableStatement cs, List<CatalogoDTO> lista) throws SQLException
    {
        try (ResultSet rs = cs.executeQuery())
        {
            while (rs.next())
            {
                CatalogoDTO p = new CatalogoDTO();
                p.setProducto_ID(recortar(rs.getString("Producto_ID")));
                p.setDescripcion(rs.getString("Descripcion"));
                p.setEstadoProducto(recortar(rs.getString("EstadoProducto")));
                p.setCategoria(rs.getString("Categoria"));
                p.setMarca(rs.getString("Marca"));
                p.setNombreProducto(rs.getString("NombreProducto"));
                p.setUnidadMedida(recortar(rs.getString("UnidadMedida")));
                p.setStockFinal(rs.getDouble("StockFinal"));
                p.setPrecioCosto(rs.getDouble("PrecioCosto"));
                p.setPrecioVenta(rs.getDouble("PrecioVenta"));
                p.setPrecioLimite(rs.getDouble("PrecioLimite"));
                p.setMargenUtil(rs.getDouble("MargenUtil"));
                p.setPeso(rs.getDouble("Peso"));
                p.setImagen(rs.getString("Imagen"));
                p.setCodigoBarra(rs.getString("CodigoBarra"));
                p.setTipoProd(recortar(rs.getString("TipoProd")));
                // Columnas nuevas: se leen si el SP ya las devuelve (si no, ver completarStockMinimo)
                String minimo = columnaOpcional(rs, "StockMinimo");
                p.setStockMinimo(minimo == null ? -1 : Double.parseDouble(minimo.trim()));
                p.setTipoBien_ID(recortar(columnaOpcional(rs, "TipoBien_ID")));
                lista.add(p);
            }
        }
    }

    /** Si el SP no devolvió StockMinimo / TipoBien_ID (quedó -1), se completan desde la tabla Productos. */
    private void completarStockMinimo(List<CatalogoDTO> lista, String codEmp)
    {
        boolean falta = false;
        for (CatalogoDTO p : lista)
        {
            if (p.getStockMinimo() < 0) { falta = true; break; }
        }
        if (!falta) return;
        java.util.Map<String, CatalogoDTO> datos = StockMinimoYTipoBien(codEmp);
        for (CatalogoDTO p : lista)
        {
            CatalogoDTO d = datos.get(p.getProducto_ID());
            if (p.getStockMinimo() < 0) p.setStockMinimo(d == null ? 0 : d.getStockMinimo());
            if (p.getTipoBien_ID() == null && d != null) p.setTipoBien_ID(d.getTipoBien_ID());
        }
    }

    private static String recortar(String s)
    {
        return s == null ? null : s.trim();
    }

    /** sp_autogenera_codprod: siguiente código de 6 dígitos (columna NuevoID; se lee la primera columna). */
    public CatalogoDTO Autogenera_codprod()
    {
        CatalogoDTO autogenera = new CatalogoDTO();
        String sql = "{call sp_autogenera_codprod}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql);
             ResultSet rs = cs.executeQuery())
        {
            if (rs.next())
            {
                autogenera.setProducto_ID(recortar(rs.getString(1)));
            }
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return autogenera;
    }

    /**
     * sp_ListaDetalleVentaXProd @Producto_ID: si el producto ya tiene ventas,
     * frm_Productos bloquea el stock y actualiza con sp_Update_Productos2.
     */
    public boolean TieneVentas(String productoId)
    {
        String sql = "{call sp_ListaDetalleVentaXProd(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString("Producto_ID", productoId);
            try (ResultSet rs = cs.executeQuery())
            {
                return rs.next();
            }
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return true;   // ante la duda no se permite cambiar el stock
        }
    }

    /** sp_Insert_Productos. Devuelve true si el procedimiento terminó sin error. */
    public boolean Insert_ProductosD(CatalogoDTO p)
    {
        String sql = "{call sp_Insert_Productos(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            asignarComunes(cs, p, true);
            cs.execute();
            return true;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza el producto.si el stock se puede editar (sin ventas) usa
     * sp_Update_Productos; si ya tiene ventas usa sp_Update_Productos2, que no recibe StockFinal.
     */
    public boolean Update_Productos(CatalogoDTO p, boolean incluyeStock)
    {
        String sql = incluyeStock
                ? "{call sp_Update_Productos(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"
                : "{call sp_Update_Productos2(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            asignarComunes(cs, p, incluyeStock);
            cs.execute();
            return true;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /** Parámetros que envía Alm_Mante_ProductosD (Insert / Update / Update2), más @TipoBien_ID y @StockMinimo. */
    private void asignarComunes(CallableStatement cs, CatalogoDTO p, boolean incluyeStock) throws SQLException
    {
        cs.setString("Producto_ID", p.getProducto_ID());
        cs.setString("NombreProducto", p.getNombreProducto());
        cs.setString("EstadoProducto", p.getEstadoProducto());
        cs.setInt("Categoria_ID", p.getCategoria_ID());
        cs.setString("MarcaProducto_ID", String.valueOf(p.getMarcaProducto_ID()));
        cs.setString("Descripcion", p.getDescripcion());
        cs.setString("UnidadMedida", p.getUnidadMedida());
        if (incluyeStock)
        {
            cs.setDouble("StockFinal", p.getStockFinal());
        }
        cs.setBigDecimal("PrecioCosto", dinero(p.getPrecioCosto()));
        cs.setBigDecimal("PrecioVenta", dinero(p.getPrecioVenta()));
        cs.setBigDecimal("PrecioLimite", dinero(p.getPrecioLimite()));
        cs.setBigDecimal("MargenUtil", dinero(p.getMargenUtil()));
        cs.setDouble("Peso", p.getPeso());
        cs.setString("Imagen", p.getImagen() == null ? "" : p.getImagen());
        cs.setString("CodigoBarra", p.getCodigoBarra() == null ? "" : p.getCodigoBarra());
        cs.setString("CodEmp", p.getCodEmp());
        cs.setString("TipoProd", p.getTipoProd());
        cs.setString("TipoBien_ID", p.getTipoBien_ID());
        cs.setDouble("StockMinimo", p.getStockMinimo());
    }

    private static java.math.BigDecimal dinero(double valor)
    {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * sp_Update_Estado_Producto @Producto_ID, @EstadoProducto, @CodEmp.
     * "Eliminar" en frm_Productos no borra: deja el producto Inactivo.
     */
    public boolean Update_Estado_Producto(String productoId, String estado, String codEmp)
    {
        String sql = "{call sp_Update_Estado_Producto(?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString("Producto_ID", productoId);
            cs.setString("EstadoProducto", estado);
            cs.setString("CodEmp", codEmp);
            cs.execute();
            return true;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /** sp_Elim_Producto: borrado físico (no lo usa frm_Productos; se conserva por compatibilidad). */
    public boolean Elim_Producto(CatalogoDTO producto)
    {
        String sql = "{call sp_Elim_Producto(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql))
        {
            cs.setString(1, producto.getProducto_ID());
            return cs.executeUpdate() > 0;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }
}
