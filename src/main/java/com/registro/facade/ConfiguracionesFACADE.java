package com.registro.facade;

import com.registro.dao.ConfiguracionesDAO;
import com.registro.dto.ConfiguracionesDTO;
import java.util.List;

/**
 * Tarjetas de pago (ManteTarjetasPago.xhtml). Igual que Ven_FacturaN del WinForm.
 *
 * @author Ing
 */
public class ConfiguracionesFACADE {

    ConfiguracionesDAO dao = new ConfiguracionesDAO();

    public List<ConfiguracionesDTO> listaTarjetasPagoF()
    {
        return dao.listaTarjetasPago();
    }
    public List<ConfiguracionesDTO> ListaTarjetasPagoActivoF()
    {
        return dao.ListaTarjetasPagoActivo();
    }
    public List<ConfiguracionesDTO> ListaTarjetasPagoXCodPagoF(String IDFormaPago)
    {
        return dao.ListaTarjetasPagoXCodPago(IDFormaPago);
    }
    public List<ConfiguracionesDTO> ListaTarjetasPagoXCodF(String IDTarjeta)
    {
        return dao.ListaTarjetasPagoXCod(IDTarjeta);
    }

    public boolean InsertTarjetasPagoF(ConfiguracionesDTO configura)
    {
        return dao.InsertTarjetasPago(configura);
    }
    public boolean UpdateTarjetasPagoF(ConfiguracionesDTO configura)
    {
        return dao.UpdateTarjetasPago(configura);
    }
    public boolean DeleteTarjetasPagoF(ConfiguracionesDTO configura)
    {
        return dao.DeleteTarjetasPago(configura);
    }

    /** Tablas donde ya se usa el código de la tarjeta (IDTarjeta). Vacía = se puede eliminar. */
    public List<String> TablasQueUsanTarjetaF(String IDTarjeta)
    {
        return dao.TablasQueUsanTarjeta(IDTarjeta);
    }
    public ConfiguracionesDTO GeneraCodigoTarjetaF()
    {
        return dao.GeneraCodigoTarjeta();
    }

    /** IDFormaPago de la forma de pago "Tarjeta" (obligatorio en TarjetasPago). */
    public String IDFormaPagoTarjetaF()
    {
        return dao.IDFormaPagoTarjeta();
    }
}
