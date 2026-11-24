/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;
/**
 *
 * @author Ing
 */
public class CajaDTO {
    
 LocalDateTime FechaTurno; 
 LocalDateTime FechaCaja; 
 String HoraIni; 
 int CodResp; 
 double MontoInicial;
 double VentaDia;  
 double VentaContado; 
 double VentaCredito;  
 double VentaEfectivo; 
 double VentaDeposito;
 double TotalPagos; 
 double TotalGastos;
 double TotalIngEfectivo; 
 String HoraCierre;
 String EstadoCaja; 
 double EfectivoActual; 
 String Turno;
 String CodEmp; 
 int UsuarioMod;
 String AcuentaPago;
 
 String Nombres;
 String NomTurno;
 int idAperturaCierre;
 double Monto;
 
String Cobro_ID;
String Cliente_ID;
String RazonSocial;
String NroRegistro;
LocalDateTime FechaEmi;
String TipoDocumento;
String NroDocumento;
                
int NroDias;
double SaldoCredito;
double CondicionCredito; 
LocalDateTime FechaVenc;

/** Ticket de cierre: quien cerró la caja (UsuarioMod). Nombres = responsable. */
String Cajero;

public String getCajero(){return Cajero;}
public void setCajero(String Cajero){this.Cajero = Cajero;}


String EmpresaComercial;
String EmpresaRuc;
String EmpresaDireccion;
String EmpresaTelefono;
                
 public CajaDTO()
 {
     
 }
 
 public LocalDateTime getFechaTurno(){return FechaTurno;}
 public void setFechaTurno(LocalDateTime FechaTurno){this.FechaTurno = FechaTurno;}
 
 public LocalDateTime getFechaCaja(){return FechaCaja;}
 public void setFechaCaja(LocalDateTime FechaCaja){this.FechaCaja = FechaCaja;}
 
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
 
 public int getUsuarioMod(){return UsuarioMod;}
 public void setUsuarioMod(int UsuarioMod){this.UsuarioMod = UsuarioMod;}
 
 public String getAcuentaPago(){return AcuentaPago;}
 public void setAcuentaPago(String AcuentaPago){this.AcuentaPago = AcuentaPago;}
 
 public String getNomTurno(){return NomTurno;}
 public void setNomTurno(String NomTurno){this.NomTurno = NomTurno;}

 public String getNombres(){return Nombres;}
 public void setNombres(String Nombres){this.Nombres = Nombres;}
 
 public int getidAperturaCierre(){return idAperturaCierre;}
 public void setidAperturaCierre(int idAperturaCierre){this.idAperturaCierre = idAperturaCierre;}
 
 public double getMonto(){return Monto;}
 public void setMonto(double Monto){this.Monto = Monto;}
 
 public String getCobro_ID(){return Cobro_ID;}
 public void setCobro_ID(String Cobro_ID){this.Cobro_ID = Cobro_ID;}
 
 public String getCliente_ID(){return Cliente_ID;}
 public void setCliente_ID(String Cliente_ID){this.Cliente_ID = Cliente_ID;}
 
 public String getRazonSocial(){return RazonSocial;}
 public void setRazonSocial(String RazonSocial){this.RazonSocial = RazonSocial;}
 
 public String getNroRegistro(){return NroRegistro;}
 public void setNroRegistro(String NroRegistro){this.NroRegistro = NroRegistro;}
 
 public LocalDateTime getFechaEmi(){return FechaEmi;}
 public void setFechaEmi(LocalDateTime FechaEmi){this.FechaEmi = FechaEmi;}

 public String getTipoDocumento(){return TipoDocumento;}
 public void setTipoDocumento(String TipoDocumento){this.TipoDocumento = TipoDocumento;}
 
 public String getNroDocumento(){return NroDocumento;}
 public void setNroDocumento(String NroDocumento){this.NroDocumento = NroDocumento;}
 
public int getNroDias(){return NroDias;}
 public void setNroDias(int NroDias){this.NroDias = NroDias;}
 
public double getSaldoCredito(){return SaldoCredito;}
public void setSaldoCredito(double SaldoCredito){this.SaldoCredito = SaldoCredito;}
 
public double getCondicionCredito(){return CondicionCredito;}
public void setCondicionCredito(double CondicionCredito){this.CondicionCredito = CondicionCredito;}

public LocalDateTime getFechaVenc(){return FechaVenc;}
public void setFechaVenc(LocalDateTime FechaVenc){this.FechaVenc = FechaVenc;}

public String getEmpresaComercial(){return EmpresaComercial;}
public void setEmpresaComercial(String EmpresaComercial){this.EmpresaComercial = EmpresaComercial;}
 
public String getEmpresaRuc(){return EmpresaRuc;}
public void setEmpresaRuc(String EmpresaRuc){this.EmpresaRuc = EmpresaRuc;}
 
public String getEmpresaDireccion(){return EmpresaDireccion;}
public void setEmpresaDireccion(String EmpresaDireccion){this.EmpresaDireccion = EmpresaDireccion;}
 
public String getEmpresaTelefono(){return EmpresaTelefono;}
public void setEmpresaTelefono(String EmpresaTelefono){this.EmpresaTelefono = EmpresaTelefono;}

}
