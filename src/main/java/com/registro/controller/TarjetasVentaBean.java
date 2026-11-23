package com.registro.controller;

import com.registro.dto.ConfiguracionesDTO;
import com.registro.facade.ConfiguracionesFACADE;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarjetas para la ventana Formas de Pago del Punto de Venta (opción Tarjeta).
 * Lee la tabla TarjetasPago con ConfiguracionesFACADE.ListaTarjetasPagoActivoF (sp_ListaTarjetasPagoActivo):
 * solo las tarjetas en estado ACTIVO, con su porcentaje de comisión (PorCom).
 */
@Named("tarjetasVentaBean")
@RequestScoped
public class TarjetasVentaBean {

    private List<ConfiguracionesDTO> activas;
    private String error;

    public List<ConfiguracionesDTO> getActivas() {
        if (activas == null) {
            try {
                activas = new ConfiguracionesFACADE().ListaTarjetasPagoActivoF();
            } catch (RuntimeException ex) {
                activas = new ArrayList<>();
                error = "No se pudieron cargar las tarjetas de pago.";
            }
        }
        return activas;
    }

    /** Mensaje si falló la consulta (null si todo bien). */
    public String getError() {
        getActivas();
        return error;
    }
}
