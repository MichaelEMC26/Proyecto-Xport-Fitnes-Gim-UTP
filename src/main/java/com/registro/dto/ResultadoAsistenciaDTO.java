package com.registro.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de registrar el ingreso de un socio.
 * Equivale a Ven_FacturaE.ResultadoAsistenciaE de SisdataGimnasio.
 */
public class ResultadoAsistenciaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Tipos de mensaje: OK, NO_EXISTE, SIN_FECHA, VENCIDO, DUPLICADO, LIMITE_EXCEDIDO. */
    private boolean exito;
    private String tipoMensaje;
    private String mensaje;
    private ClienteDTO datosCliente;
    private AsistenciaDTO datosSuscripcion;
    private List<AsistenciaDTO> historialAsistencias;
    private int asistenciasPermitidas;      // 0 = sin límite
    private int asistenciasUsadas = -1;     // -1 = no se sabe

    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }

    public String getTipoMensaje() { return tipoMensaje; }
    public void setTipoMensaje(String tipoMensaje) { this.tipoMensaje = tipoMensaje; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public ClienteDTO getDatosCliente() { return datosCliente; }
    public void setDatosCliente(ClienteDTO datosCliente) { this.datosCliente = datosCliente; }

    public AsistenciaDTO getDatosSuscripcion() { return datosSuscripcion; }
    public void setDatosSuscripcion(AsistenciaDTO datosSuscripcion) { this.datosSuscripcion = datosSuscripcion; }

    public List<AsistenciaDTO> getHistorialAsistencias() { return historialAsistencias; }
    public void setHistorialAsistencias(List<AsistenciaDTO> historialAsistencias) {
        this.historialAsistencias = historialAsistencias != null ? historialAsistencias : new ArrayList<>();
    }

    public int getAsistenciasPermitidas() { return asistenciasPermitidas; }
    public void setAsistenciasPermitidas(int asistenciasPermitidas) { this.asistenciasPermitidas = asistenciasPermitidas; }

    public int getAsistenciasUsadas() { return asistenciasUsadas; }
    public void setAsistenciasUsadas(int asistenciasUsadas) { this.asistenciasUsadas = asistenciasUsadas; }
}
