/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.facade;

import com.registro.dao.ProductosDAO;
import com.registro.dto.ProductosDTO;
import com.registro.dto.CategoriaDTO;
import com.registro.dto.MarcasDTO;
/**
 *
 * @author Ing
 */
public class ProductosFACADE {
    
    private final ProductosDAO dao = new ProductosDAO();
    
    public CategoriaDTO autogeneraCategoria() {
    return dao.AutogeneraCategoria();
   } 
    public boolean Mante_Categorias(CategoriaDTO categoria)
    {
        return dao.Mante_Categorias(categoria);
    }    
    public boolean Elimina_Categorias(CategoriaDTO categoria)
    {
        return dao.Elimina_Categorias(categoria);
    }
    
    public MarcasDTO AutogeneraMarca()
    {
        return dao.AutogeneraMarca();
    }
    public boolean Mante_Marcas(MarcasDTO marcas)
    {
        return dao.Mante_Marcas(marcas);
    }
    public boolean Elim_MarcaPro(MarcasDTO marcas)
    {
        return dao.Elim_MarcaPro(marcas);
    }
    
    public ProductosDTO AutogeneraProducto()
    {
        return dao.Autogenera_codprod();
    }
    public boolean Insert_ProductosD(ProductosDTO productos)
    {
        return dao.Insert_ProductosD(productos);
    }
    public boolean Update_Productos(ProductosDTO productos)
    {
        return dao.Update_Productos(productos);
    }
    
    public boolean Elimina_Producto(ProductosDTO productos)
    {
        return dao.Elim_Producto(productos);
    }
}
