/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.io.Serializable;
/**
 * Datos de Facturación (formas de pago, caja, productos, cabecera/detalle de ventas, créditos).
 *
 * @author Ing
 */
public class FacturacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    
     /********* Formas de Pago ******/
    private String IDFormaPago;
    private String FormasPago;
    private String Estado;
    
    /********* Tipos de Documento ******/
    private String Documento_ID;
    private String DescripcionTipoDoc;
    private String EstadoTipoDoc;
    
    /********* Configuracion de Sistema ******/
    private int idConfiguracion;
    private String FlagStockNegativo;
    private String FlagAlertaS;
    private String FlagAlertaCC;
    private String FlagAlertaCP;
    private String FlagAfectoIgv;
    private String FlagFormaPagoXComElectronico;
    private String FlagActivaCodigoBarra;
    private String FlagEnvioDirecto;
    
    /********* Apertura de Caja ******/
    private int idAperturaCierre;
    private LocalDate FechaTurno;
    private LocalDate FechaCaja;
    private String HoraIni;
    private int CodResp;
    private double MontoInicial;
    private double VentaDia;
    private double VentaContado;
    private double VentaCredito;
    private double VentaEfectivo;
    private double VentaDeposito;
    private double TotalPagos;
    private double TotalGastos;
    private double TotalIngEfectivo;
    private String HoraCierre;
    private String EstadoCaja;
    private double EfectivoActual;
    private String Turno;
    private String CodEmp;
    
    /********* UltimaVentaTurno ******/

    private LocalDateTime UltimaVenta;
    private double HorasBloqueo;
    
    /********* UltimaVentaTurno ******/
    private String NumeracionOP;
    
    /********* autogenera_cod_BoletaVenta ******/
    private String Boleta_id;
    
    /********* autogenera_cod_BoletaVenta ******/
    private String NroSerie;
    
   /********* lista_ClienteXcod ******/
    private String Cliente_ID;
    private String RazonSocial;
    private String Direccion;
    private String Telefono;
    private String EstadoCte;
    private String Telefono2;
    private String Correo;
    private String Sexo;
    private String RucDNI;
    private String TipoId;
    private String Foto;
    private String Contacto;
    private String TelefonoCont;
    private byte[] HuellaDactilar;
    
    /********* Lista_ProductosGral_ActivosXNom ******/
    private String Producto_ID;
    private String Descripcion;
    private String UnidadMedida;
    private double StockFinal;
    private double PrecioCosto;
    private double PrecioVenta;
    private double PrecioLimite;
    private String NombreProducto;
    private String Categoria;      // sp_Lista_ProductosGral_*: devuelve el NOMBRE de la categoría
    private String Marca;          // y el NOMBRE de la marca (no el código)
    private double MargenUtil;
    private double Peso;
    private String Imagen;
    private String CodigoBarra;
    
    /********* listaTipoProductoXCodigo ******/
    private String TipoProd;
    
    /********* Insert_CabeceraVentas ******/
    
    private String Pedido_ID;
    private String Tipo_documento;
    private String Venta_id;
    private String Nro_Doc;
    private LocalDate Fecha_Pedido;
    private String CondPago;
    private String FormaPago;
    private double SubTotal;
    private double IGV;
    private double Total;
    private String EstadoPed;
    private String MontoLetras;
    private int CodPer;
    private String Dia;
    
    private LocalDateTime FechaHora;
    private String TipoDoc;
    private int CodUser;
    
    /********* ConfiguracionSeries_Actualiza ******/
    
    private String NroDocumento;
    
    /********* Insert_Detalle_Ventas ******/
    private String DescripcionProd;
    private double PrecioUnitario;
    private double Cantidad;
    private double Importe;
    private String TipoDocumento;
    
    /********* InsertCtasXCobrar ******/
    private String NroRegistro;
    private LocalDate FechaEmi;
    private double Monto;
    private int NroDias;
    private double Interes;
    private double SaldoCredito;
    private String CondicionCredito;
    private LocalDate FechaCanc;
    private int UsuarioMod;
    private LocalDate FechaVenc;
    private String CondCli;
    
    /********* ListaIDDOCUMENTOXNroPedido ******/
    private String NroGuia;
    private String RucDniCliente;
    
    /********* ListaIDDOCUMENTOXNroPedido ******/
    
    private String NroPedido;
    private String IDDOCUMENTO;
    private String Responsable;
    
    
    private String EmpresaRazon;
    private String EmpresaComercial;
    private String EmpresaRuc;
    private String EmpresaDireccion;
    private String EmpresaTelefono;
    
    /********* DetalleFormasPago (una fila por forma de pago de la venta) ******/
    private double MontoParcial;      // monto pagado con esa forma de pago
    private double PorcInc;           // % de comisión (tarjeta)
    private double MontoInc;          // monto con la comisión (tarjeta) o igual al monto
    private double MontoReal;         // monto real cobrado
    private String IDBanco;           // tarjeta: IDTarjeta de TarjetasPago; demás: "0"
    private String NroOperacion;      // n° de operación / voucher / referencia

    /********* Cabecera_Ventas ******/
    private String Impreso;           // "SI" si el comprobante ya se imprimió

    public FacturacionDTO()
    {
        
    }
            
    public String getIDFormaPago(){return IDFormaPago;}
    public void setIDFormaPago(String IDFormaPago){this.IDFormaPago = IDFormaPago;}
       
    public String getFormasPago(){return FormasPago;}
    public void setFormasPago(String FormasPago){this.FormasPago = FormasPago;}
       
    public String getEstado(){return Estado;}
    public void setEstado(String Estado){this.Estado =Estado;}  
    
    /********* Tipos de Documento ******/
    
    public String getDocumento_ID(){return Documento_ID;}
    public void setDocumento_ID(String Documento_ID){this.Documento_ID = Documento_ID;}
       
    public String getDescripcionTipoDoc(){return DescripcionTipoDoc;}
    public void setDescripcionTipoDoc(String DescripcionTipoDoc){this.DescripcionTipoDoc = DescripcionTipoDoc;}
       
    public String getEstadoTipoDoc(){return EstadoTipoDoc;}
    public void setEstadoTipoDoc(String EstadoTipoDoc){this.EstadoTipoDoc =EstadoTipoDoc;}  
    
      /********* Configuracion de Sistema ******/
    
    public int getidConfiguracion(){return idConfiguracion;}
    public void setidConfiguracion(int idConfiguracion){this.idConfiguracion = idConfiguracion;}
       
    public String getFlagStockNegativo(){return FlagStockNegativo;}
    public void setFlagStockNegativo(String FlagStockNegativo){this.FlagStockNegativo = FlagStockNegativo;}
       
    public String getFlagAlertaS(){return FlagAlertaS;}
    public void setFlagAlertaS(String FlagAlertaS){this.FlagAlertaS =FlagAlertaS;}  
    
     public String getFlagAlertaCC(){return FlagAlertaCC;}
    public void setFlagAlertaCC(String FlagAlertaCC){this.FlagAlertaCC = FlagAlertaCC;}
       
    public String getFlagAlertaCP(){return FlagAlertaCP;}
    public void setFlagAlertaCP(String FlagAlertaCP){this.FlagAlertaCP = FlagAlertaCP;}
       
    public String getFlagAfectoIgv(){return FlagAfectoIgv;}
    public void setFlagAfectoIgv(String FlagAfectoIgv){this.FlagAfectoIgv =FlagAfectoIgv;}  
    
     public String getFlagFormaPagoXComElectronico(){return FlagFormaPagoXComElectronico;}
    public void setFlagFormaPagoXComElectronico(String FlagFormaPagoXComElectronico){this.FlagFormaPagoXComElectronico = FlagFormaPagoXComElectronico;}
       
    public String getFlagActivaCodigoBarra(){return FlagActivaCodigoBarra;}
    public void setFlagActivaCodigoBarra(String FlagActivaCodigoBarra){this.FlagActivaCodigoBarra = FlagActivaCodigoBarra;}
       
    public String getFlagEnvioDirecto(){return FlagEnvioDirecto;}
    public void setFlagEnvioDirecto(String FlagEnvioDirecto){this.FlagEnvioDirecto =FlagEnvioDirecto;}  
    
     /********* Apertura de Caja ******/
    
    public int getidAperturaCierre(){return idAperturaCierre;}
    public void setidAperturaCierre(int idAperturaCierre){this.idAperturaCierre = idAperturaCierre;}
       
    public LocalDate getFechaTurno(){return FechaTurno;}
    public void setFechaTurno(LocalDate FechaTurno){this.FechaTurno = FechaTurno;}
       
    public LocalDate getFechaCaja(){return FechaCaja;}
    public void setFechaCaja(LocalDate FechaCaja){this.FechaCaja = FechaCaja;}
    
    public String getHoraIni(){return HoraIni;}
    public void setHoraIni(String HoraIni){this.HoraIni = HoraIni;}
       
    public int getCodResp(){return CodResp;}
    public void setCodResp(int CodResp){this.CodResp = CodResp;}
       
    public double getMontoInicial(){return MontoInicial;}
    public void setMontoInicial(double MontoInicial){this.MontoInicial = MontoInicial;}  
    
    public double getVentaDia(){return VentaDia;}
    public void setVentaDia(double VentaDia){this.VentaDia = VentaDia;}  
       
    public double getVentaContado(){return VentaContado;}
    public void setVentaContado(double VentaContado){this.VentaContado = VentaContado;}  
       
    public double getVentaCredito(){return VentaCredito;}
    public void setVentaCredito(double VentaCredito){this.VentaCredito = VentaCredito;}  
    
    public double getVentaEfectivo(){return VentaEfectivo;}
    public void setVentaEfectivo(double VentaEfectivo){this.VentaEfectivo = VentaEfectivo;}  
    
    public double getVentaDeposito(){return VentaDeposito;}
    public void setVentaDeposito(double VentaDeposito){this.VentaDeposito = VentaDeposito;}  
       
    public double getTotalPagos(){return TotalPagos;}
    public void setTotalPagos(double TotalPagos){this.TotalPagos = TotalPagos;}  
       
    public double getTotalGastos(){return TotalGastos;}
    public void setTotalGastos(double TotalGastos){this.TotalGastos = TotalGastos;}  
    
    public double getTotalIngEfectivo(){return TotalIngEfectivo;}
    public void setTotalIngEfectivo(double TotalIngEfectivo){this.TotalIngEfectivo = TotalIngEfectivo;}  
    
    public String getHoraCierre(){return HoraCierre;}
    public void setHoraCierre(String HoraCierre){this.HoraCierre = HoraCierre;}  
       
    public String getEstadoCaja(){return EstadoCaja;}
    public void setEstadoCaja(String EstadoCaja){this.EstadoCaja = EstadoCaja;}  
       
    public double getEfectivoActual(){return EfectivoActual;}
    public void setEfectivoActual(double EfectivoActual){this.EfectivoActual = EfectivoActual;}  
    
    public String getTurno(){return Turno;}
    public void setTurno(String Turno){this.Turno = Turno;}  
       
    public String getCodEmp(){return CodEmp;}
    public void setCodEmp(String CodEmp){this.CodEmp = CodEmp;}  
    
     /********* UltimaVentaTurno ******/

    public LocalDateTime getUltimaVenta(){return UltimaVenta;}
    public void setUltimaVenta(LocalDateTime UltimaVenta){this.UltimaVenta = UltimaVenta;}  
       
    public double getHorasBloqueo(){return HorasBloqueo;}
    public void setHorasBloqueo(double HorasBloqueo){this.HorasBloqueo = HorasBloqueo;}  
    
    /********* UltimaVentaTurno ******/

    public String getNumeracionOP(){return NumeracionOP;}
    public void setNumeracionOP(String NumeracionOP){this.NumeracionOP = NumeracionOP;}  
    
    /********* autogenera_cod_BoletaVenta ******/

    public String getBoleta_id(){return Boleta_id;}
    public void setBoleta_id(String Boleta_id){this.Boleta_id = Boleta_id;}  
    
    /********* autogenera_cod_BoletaVenta ******/

    public String getNroSerie(){return NroSerie;}
    public void setNroSerie(String NroSerie){this.NroSerie = NroSerie;}  
    
   /********* lista_ClienteXcod ******/

    public String getCliente_ID(){return Cliente_ID;}
    public void setCliente_ID(String Cliente_ID){this.Cliente_ID = Cliente_ID;} 
    
    public String getRazonSocial(){return RazonSocial;}
    public void setRazonSocial(String RazonSocial){this.RazonSocial = RazonSocial;} 

    public String getDireccion(){return Direccion;}
    public void setDireccion(String Direccion){this.Direccion = Direccion;} 

    public String getTelefono(){return Telefono;}
    public void setTelefono(String Telefono){this.Telefono = Telefono;} 

    public String getEstadoCte(){return EstadoCte;}
    public void setEstadoCte(String EstadoCte){this.EstadoCte = EstadoCte;} 

    public String getTelefono2(){return Telefono2;}
    public void setTelefono2(String Telefono2){this.Telefono2 = Telefono2;}

    public String getCorreo(){return Correo;}
    public void setCorreo(String Correo){this.Correo = Correo;}

    public String getSexo(){return Sexo;}
    public void setSexo(String Sexo){this.Sexo = Sexo;}

    public String getRucDNI(){return RucDNI;}
    public void setRucDNI(String RucDNI){this.RucDNI = RucDNI;}

    public String getTipoId(){return TipoId;}
    public void setTipoId(String TipoId){this.TipoId = TipoId;}

    public String getFoto(){return Foto;}
    public void setFoto(String Foto){this.Foto = Foto;}

    public String getContacto(){return Contacto;}
    public void setContacto(String Contacto){this.Contacto = Contacto;}

    public String getTelefonoCont(){return TelefonoCont;}
    public void setTelefonoCont(String TelefonoCont){this.TelefonoCont = TelefonoCont;}

    public byte[] getHuellaDactilar(){return HuellaDactilar;}
    public void setHuellaDactilar(byte[] HuellaDactilar){this.HuellaDactilar = HuellaDactilar;}
    
    /********* Lista_ProductosGral_ActivosXNom ******/

    public String getProducto_ID(){return Producto_ID;}
    public void setProducto_ID(String Producto_ID){this.Producto_ID = Producto_ID;}

    public String getDescripcion(){return Descripcion;}
    public void setDescripcion(String Descripcion){this.Descripcion = Descripcion;}
    
    public String getUnidadMedida(){return UnidadMedida;}
    public void setUnidadMedida(String UnidadMedida){this.UnidadMedida = UnidadMedida;}

    public double getStockFinal(){return StockFinal;}
    public void setStockFinal(double StockFinal){this.StockFinal = StockFinal;}

    public double getPrecioCosto(){return PrecioCosto;}
    public void setPrecioCosto(double PrecioCosto){this.PrecioCosto = PrecioCosto;}

    public double getPrecioVenta(){return PrecioVenta;}
    public void setPrecioVenta(double PrecioVenta){this.PrecioVenta = PrecioVenta;}

    public double getPrecioLimite(){return PrecioLimite;}
    public void setPrecioLimite(double PrecioLimite){this.PrecioLimite = PrecioLimite;}

    public String getNombreProducto(){return NombreProducto;}
    public void setNombreProducto(String NombreProducto){this.NombreProducto = NombreProducto;}

    public String getCategoria(){return Categoria;}
    public void setCategoria(String Categoria){this.Categoria = Categoria;}
    
    public String getMarca(){return Marca;}
    public void setMarca(String Marca){this.Marca = Marca;}
    
    public double getMargenUtil(){return MargenUtil;}
    public void setMargenUtil(double MargenUtil){this.MargenUtil = MargenUtil;}

    public double getPeso(){return Peso;}
    public void setPeso(double Peso){this.Peso = Peso;}
    
    public String getImagen(){return Imagen;}
    public void setImagen(String Imagen){this.Imagen = Imagen;}

    public String getCodigoBarra(){return CodigoBarra;}
    public void setCodigoBarra(String CodigoBarra){this.CodigoBarra = CodigoBarra;}
    
    /********* listaTipoProductoXCodigo ******/
    public String getTipoProd(){return TipoProd;}
    public void setTipoProd(String TipoProd){this.TipoProd = TipoProd;}

    /********* Insert_CabeceraVentas ******/

    public String getPedido_ID(){return Pedido_ID;}
    public void setPedido_ID(String Pedido_ID){this.Pedido_ID = Pedido_ID;}
    
    public String getTipo_documento(){return Tipo_documento;}
    public void setTipo_documento(String Tipo_documento){this.Tipo_documento = Tipo_documento;}
    
    public String getVenta_id(){return Venta_id;}
    public void setVenta_id(String Venta_id){this.Venta_id = Venta_id;}

    public String getNro_Doc(){return Nro_Doc;}
    public void setNro_Doc(String Nro_Doc){this.Nro_Doc = Nro_Doc;}

    public LocalDate getFecha_Pedido(){return Fecha_Pedido;}
    public void setFecha_Pedido(LocalDate Fecha_Pedido){this.Fecha_Pedido = Fecha_Pedido;}

    public String getCondPago(){return CondPago;}
    public void setCondPago(String CondPago){this.CondPago = CondPago;}

    public String getFormaPago(){return FormaPago;}
    public void setFormaPago(String FormaPago){this.FormaPago = FormaPago;}
    
    public double getSubTotal(){return SubTotal;}
    public void setSubTotal(double SubTotal){this.SubTotal = SubTotal;}
    
    public double getIGV(){return IGV;}
    public void setIGV(double IGV){this.IGV = IGV;}
    
    public double getTotal(){return Total;}
    public void setTotal(double Total){this.Total = Total;}
    
    public String getEstadoPed(){return EstadoPed;}
    public void setEstadoPed(String EstadoPed){this.EstadoPed = EstadoPed;}

    public String getMontoLetras(){return MontoLetras;}
    public void setMontoLetras(String MontoLetras){this.MontoLetras = MontoLetras;}
    
    public int getCodPer(){return CodPer;}
    public void setCodPer(int CodPer){this.CodPer = CodPer;}

    public String getDia(){return Dia;}
    public void setDia(String Dia){this.Dia = Dia;}
    
    public LocalDateTime getFechaHora(){return FechaHora;}
    public void setFechaHora(LocalDateTime FechaHora){this.FechaHora = FechaHora;}
    
    public String getTipoDoc(){return TipoDoc;}
    public void setTipoDoc(String TipoDoc){this.TipoDoc = TipoDoc;}
    
    public int getCodUser(){return CodUser;}
    public void setCodUser(int CodUser){this.CodUser = CodUser;}
    

    /********* ConfiguracionSeries_Actualiza ******/
    
    public String getNroDocumento(){return NroDocumento;}
    public void setNroDocumento(String NroDocumento){this.NroDocumento = NroDocumento;}
    
    /********* Insert_Detalle_Ventas ******/

    public String getDescripcionProd(){return DescripcionProd;}
    public void setDescripcionProd(String DescripcionProd){this.DescripcionProd = DescripcionProd;}
    
    public double getPrecioUnitario(){return PrecioUnitario;}
    public void setPrecioUnitario(double PrecioUnitario){this.PrecioUnitario = PrecioUnitario;}
    
    public double getCantidad(){return Cantidad;}
    public void setCantidad(double Cantidad){this.Cantidad = Cantidad;}
    
    public double getImporte(){return Importe;}
    public void setImporte(double Importe){this.Importe = Importe;}

    /********* InsertCtasXCobrar ******/

    public String getNroRegistro(){return NroRegistro;}
    public void setNroRegistro(String NroRegistro){this.NroRegistro = NroRegistro;}

    public LocalDate getFechaEmi(){return FechaEmi;}
    public void setFechaEmi(LocalDate FechaEmi){this.FechaEmi = FechaEmi;}

    public double getMonto(){return Monto;}
    public void setMonto(double Monto){this.Monto = Monto;}

    public int getNroDias(){return NroDias;}
    public void setNroDias(int NroDias){this.NroDias = NroDias;}

    public double getInteres(){return Interes;}
    public void setInteres(double Interes){this.Interes = Interes;}

    public double getSaldoCredito(){return SaldoCredito;}
    public void setSaldoCredito(double SaldoCredito){this.SaldoCredito = SaldoCredito;}

    public String getCondicionCredito(){return CondicionCredito;}
    public void setCondicionCredito(String CondicionCredito){this.CondicionCredito = CondicionCredito;}

    public LocalDate getFechaCanc(){return FechaCanc;}
    public void setFechaCanc(LocalDate FechaCanc){this.FechaCanc = FechaCanc;}

    public int getUsuarioMod(){return UsuarioMod;}
    public void setUsuarioMod(int UsuarioMod){this.UsuarioMod = UsuarioMod;}
    
    public LocalDate getFechaVenc(){return FechaVenc;}
    public void setFechaVenc(LocalDate FechaVenc){this.FechaVenc = FechaVenc;}
    
    public String getCondCli(){return CondCli;}
    public void setCondCli(String CondCli){this.CondCli = CondCli;}
    
    /********* ListaIDDOCUMENTOXNroPedido ******/

    public String getNroGuia(){return NroGuia;}
    public void setNroGuia(String NroGuia){this.NroGuia = NroGuia;}
    
    public String getTipoDocumento(){return TipoDocumento;}
    public void setTipoDocumento(String TipoDocumento){this.TipoDocumento = TipoDocumento;}
    
    public String getNroPedido(){return NroPedido;}
    public void setNroPedido(String NroPedido){this.NroPedido = NroPedido;}
    
    public String getRucDniCliente(){return RucDniCliente;}
    public void setRucDniCliente(String RucDniCliente){this.RucDniCliente = RucDniCliente;}
    
    public String getIDDOCUMENTO(){return IDDOCUMENTO;}
    public void setIDDOCUMENTO(String IDDOCUMENTO){this.IDDOCUMENTO = IDDOCUMENTO;}
    
    public String getResponsable(){return Responsable;}
    public void setResponsable(String Responsable){this.Responsable = Responsable;}

    public String getEmpresaRazon(){return EmpresaRazon;}
    public void setEmpresaRazon(String EmpresaRazon){this.EmpresaRazon = EmpresaRazon;}

    public String getEmpresaComercial(){return EmpresaComercial;}
    public void setEmpresaComercial(String EmpresaComercial){this.EmpresaComercial = EmpresaComercial;}

    public String getEmpresaRuc(){return EmpresaRuc;}
    public void setEmpresaRuc(String EmpresaRuc){this.EmpresaRuc = EmpresaRuc;}

    public String getEmpresaDireccion(){return EmpresaDireccion;}
    public void setEmpresaDireccion(String EmpresaDireccion){this.EmpresaDireccion = EmpresaDireccion;}
    
    public String getEmpresaTelefono(){return EmpresaTelefono;}
    public void setEmpresaTelefono(String EmpresaTelefono){this.EmpresaTelefono = EmpresaTelefono;}


    public double getMontoParcial(){return MontoParcial;}
    public void setMontoParcial(double MontoParcial){this.MontoParcial = MontoParcial;}

    public double getPorcInc(){return PorcInc;}
    public void setPorcInc(double PorcInc){this.PorcInc = PorcInc;}

    public double getMontoInc(){return MontoInc;}
    public void setMontoInc(double MontoInc){this.MontoInc = MontoInc;}

    public double getMontoReal(){return MontoReal;}
    public void setMontoReal(double MontoReal){this.MontoReal = MontoReal;}

    public String getIDBanco(){return IDBanco;}
    public void setIDBanco(String IDBanco){this.IDBanco = IDBanco;}

    public String getNroOperacion(){return NroOperacion;}
    public void setNroOperacion(String NroOperacion){this.NroOperacion = NroOperacion;}

    public String getImpreso(){return Impreso;}
    public void setImpreso(String Impreso){this.Impreso = Impreso;}
}
