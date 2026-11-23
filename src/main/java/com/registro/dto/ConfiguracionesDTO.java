package com.registro.dto;

import java.io.Serializable;

/**
 * Tarjetas de pago (tabla TarjetasPago) - ManteTarjetasPago.xhtml / frm_ManteTarjetasPago.
 *
 * Columnas: IDTarjeta varchar(3), NombreTarjetaPago varchar(100), Estado varchar(10) (ACTIVO / INACTIVO),
 * PorCom real (% de comisión o incremento) e IDFormaPago varchar(2) (forma de pago a la que pertenece).
 *
 * @author Ing
 */
public class ConfiguracionesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String IDTarjeta;
    private String NombreTarjetaPago;
    private String Estado;
    private double PorCom;
    private String IDFormaPago;

    public ConfiguracionesDTO()
    {
    }

    public String getIDTarjeta(){return IDTarjeta;}
    public void setIDTarjeta(String IDTarjeta){this.IDTarjeta = IDTarjeta;}

    public String getNombreTarjetaPago(){return NombreTarjetaPago;}
    public void setNombreTarjetaPago(String NombreTarjetaPago){this.NombreTarjetaPago = NombreTarjetaPago;}

    public String getEstado(){return Estado;}
    public void setEstado(String Estado){this.Estado = Estado;}

    public double getPorCom(){return PorCom;}
    public void setPorCom(double PorCom){this.PorCom = PorCom;}

    public String getIDFormaPago(){return IDFormaPago;}
    public void setIDFormaPago(String IDFormaPago){this.IDFormaPago = IDFormaPago;}
}
