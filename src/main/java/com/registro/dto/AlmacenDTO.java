/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;
import java.time.LocalDateTime;
import java.time.LocalTime;
/**
 *
 * @author Ing
 */
public class AlmacenDTO {
    
    private String idproducto;
    private LocalDateTime FechaMovimiento;
    private int TipoMovimiento;
    private String Proveedor_Cliente;
    private double CantIngreso;
    private double PrecioCompra;
    private double CantSalida;
    private double PrecioVenta;
    private double Saldo;
    private String NroRegistro;
    private String CodEmp;
    private String CodSede;
    private String Hora;
    private String OrdenMov;
    private int CodResp;
    private String Inventario_ID;
    private LocalDateTime FechaInicio;
    private LocalDateTime FechaCierre;
    private int Personal_ID;
    private String Responsable;
    private String Descripcion;
    private double totalItemsBase;
    private double totalItemsAct;
    private double BaseImpon;
    private double IGV;
    private double TotalCosto;
    private double TotalVenta;
    private double TotalMargen;
    private double PromPorcen;
    private String FlagInvAlm;
    
    private double SaldoInicial;
    private double SaldoFinal;
    private String Productos;
    private String UnidadMedida;
    private String Categoria;
    private String MarcaProducto;
    private String Filtro;
    private int Categoria_ID;
    private int MarcaProducto_ID;
    
    private double StockFinal;
    private double PrecioCosto;
    private double ImporteValorizado;
    private String TipoBien;
    private double TotalValorizado;
    private double CantItems;
    private double CantItemsStock;
    private double CantItemsStockMin;
    private double CantItemsSinStock;
    
    public String getidproducto(){return idproducto;}
    public void setidproducto(String idproducto){this.idproducto = idproducto;}
    
    public LocalDateTime getFechaMovimiento(){return FechaMovimiento;}
    public void setFechaMovimiento(LocalDateTime FechaMovimiento){this.FechaMovimiento = FechaMovimiento;}
    
    public int getTipoMovimiento(){return TipoMovimiento;}
    public void setTipoMovimiento(int TipoMovimiento){this.TipoMovimiento = TipoMovimiento;}
    
    public String getProveedor_Cliente(){return Proveedor_Cliente;}
    public void setProveedor_Cliente(String Proveedor_Cliente){this.Proveedor_Cliente = Proveedor_Cliente;}
    
    public double getCantIngreso(){return CantIngreso;}
    public void setCantIngreso(double CantIngreso){this.CantIngreso = CantIngreso;}
    
     public double getPrecioCompra(){return PrecioCompra;}
    public void setPrecioCompra(double PrecioCompra){this.PrecioCompra = PrecioCompra;}
    
     public double getCantSalida(){return CantSalida;}
    public void setCantSalida(double CantSalida){this.CantSalida = CantSalida;}
    
    public double getPrecioVenta(){return PrecioVenta;}
    public void setPrecioVenta(double PrecioVenta){this.PrecioVenta = PrecioVenta;}
    
     public double getSaldo(){return Saldo;}
    public void setSaldo(double Saldo){this.Saldo = Saldo;}
    
    public String getNroRegistro(){return NroRegistro;}
    public void setNroRegistro(String NroRegistro){this.NroRegistro = NroRegistro;}
    
     public String getCodEmp(){return CodEmp;}
    public void setCodEmp(String CodEmp){this.CodEmp = CodEmp;}
    
     public String getCodSede(){return CodSede;}
    public void setCodSede(String CodSede){this.CodSede = CodSede;}
    
     public String getHora(){return Hora;}
    public void setHora(String Hora){this.Hora = Hora;}
    
     public String getOrdenMov(){return OrdenMov;}
    public void setOrdenMov(String OrdenMov){this.OrdenMov = OrdenMov;}
    
    public int getCodResp(){return CodResp;}
    public void setCodResp(int CodResp){this.CodResp = CodResp;}
    
    public String getInventario_ID(){return Inventario_ID;}
    public void setInventario_ID(String Inventario_ID){this.Inventario_ID = Inventario_ID;}
    
    public LocalDateTime getFechaInicio(){return FechaInicio;}
    public void setFechaInicio(LocalDateTime FechaInicio){this.FechaInicio = FechaInicio;}
    
    public LocalDateTime getFechaCierre(){return FechaCierre;}
    public void setFechaCierre(LocalDateTime FechaCierre){this.FechaCierre = FechaCierre;}
    
    public int getPersonal_ID(){return Personal_ID;}
    public void setPersonal_ID(int Personal_ID){this.Personal_ID = Personal_ID;}
    
    public String getResponsable(){return Responsable;}
    public void setResponsable(String Responsable){this.Responsable = Responsable;}
    
    public String getDescripcion(){return Descripcion;}
    public void setDescripcion(String Descripcion){this.Descripcion = Descripcion;}
    
    public double gettotalItemsBase(){return totalItemsBase;}
    public void settotalItemsBase(double totalItemsBase){this.totalItemsBase = totalItemsBase;}
    
    public double gettotalItemsAct(){return totalItemsAct;}
    public void settotalItemsAct(double totalItemsAct){this.totalItemsAct = totalItemsAct;}
    
    public double getBaseImpon(){return BaseImpon;}
    public void setBaseImpon(double BaseImpon){this.BaseImpon = BaseImpon;}
    
    public double getIGV(){return IGV;}
    public void setIGV(double IGV){this.IGV = IGV;}
    
    public double getTotalCosto(){return TotalCosto;}
    public void setTotalCosto(double TotalCosto){this.TotalCosto = TotalCosto;}
    
    public double getTotalVenta(){return TotalVenta;}
    public void setTotalVenta(double TotalVenta){this.TotalVenta = TotalVenta;}
    
    public double getTotalMargen(){return TotalMargen;}
    public void setTotalMargen(double TotalMargen){this.TotalMargen = TotalMargen;}
    
    public double getPromPorcen(){return PromPorcen;}
    public void setPromPorcen(double PromPorcen){this.PromPorcen = PromPorcen;}
    
    public String getFlagInvAlm(){return FlagInvAlm;}
    public void setFlagInvAlm(String FlagInvAlm){this.FlagInvAlm = FlagInvAlm;}
    
    public double getSaldoInicial(){return SaldoInicial;}
    public void setSaldoInicial(double SaldoInicial){this.SaldoInicial = SaldoInicial;}
    
    public double getSaldoFinal(){return SaldoFinal;}
    public void setSaldoFinal(double SaldoFinal){this.SaldoFinal = SaldoFinal;}
    
    public String getProductos(){return Productos;}
    public void setProductos(String Productos){this.Productos = Productos;}
    
    public String getUnidadMedida(){return UnidadMedida;}
    public void setUnidadMedida(String UnidadMedida){this.UnidadMedida = UnidadMedida;}
    
    public String getCategoria(){return Categoria;}
    public void setCategoria(String Categoria){this.Categoria = Categoria;}
    
    public String getFiltro(){return Filtro;}
    public void setFiltro(String Filtro){this.Filtro = Filtro;}
    
    public int getCategoria_ID(){return Categoria_ID;}
    public void setCategoria_ID(int Categoria_ID){this.Categoria_ID = Categoria_ID;}
    
    public int getMarcaProducto_ID(){return MarcaProducto_ID;}
    public void setMarcaProducto_ID(int MarcaProducto_ID){this.MarcaProducto_ID = MarcaProducto_ID;}
    
    public String getMarcaProducto(){return MarcaProducto;}
    public void setMarcaProducto(String MarcaProducto){this.MarcaProducto = MarcaProducto;}

    public double getStockFinal(){return StockFinal;}
    public void setStockFinal(double StockFinal){this.StockFinal = StockFinal;}
    
    public double getPrecioCosto(){return PrecioCosto;}
    public void setPrecioCosto(double PrecioCosto){this.PrecioCosto = PrecioCosto;}
    
    public double getImporteValorizado(){return ImporteValorizado;}
    public void setImporteValorizado(double ImporteValorizado){this.ImporteValorizado = ImporteValorizado;}
    
    public String getTipoBien(){return TipoBien;}
    public void setTipoBien(String TipoBien){this.TipoBien = TipoBien;}
    
    public double getTotalValorizado(){return TotalValorizado;}
    public void setTotalValorizado(double TotalValorizado){this.TotalValorizado = TotalValorizado;}
    
    public double getCantItems(){return CantItems;}
    public void setCantItems(double CantItems){this.CantItems = CantItems;}

    public double getCantItemsStock(){return CantItemsStock;}
    public void setCantItemsStock(double CantItemsStock){this.CantItemsStock = CantItemsStock;}
    
    public double getCantItemsStockMin(){return CantItemsStockMin;}
    public void setCantItemsStockMin(double CantItemsStockMin){this.CantItemsStockMin = CantItemsStockMin;}
    
    public double getCantItemsSinStock(){return CantItemsSinStock;}
    public void setCantItemsSinStock(double CantItemsSinStock){this.CantItemsSinStock = CantItemsSinStock;}

}
