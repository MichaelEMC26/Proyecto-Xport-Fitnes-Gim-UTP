/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.facade;

import com.registro.dao.AlmacenDAO;
import com.registro.dto.AlmacenDTO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Capa de negocio de Almacén: delega en AlmacenDAO.
 *
 * @author Ing
 */
public class AlmacenFACADE {

    private final AlmacenDAO dao = new AlmacenDAO();

    public List<AlmacenDTO> ListaMovimientosDiarioProductosF(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin)
    {
        return dao.ListaMovimientosDiarioProductos(CodEmp, FechaIni, FechaFin);
    }

    public List<AlmacenDTO> ReporteExistenciasProductosF(String CodEmp, String Filtro)
    {
        return dao.ReporteExistenciasProductos(CodEmp, Filtro);
    }

    public AlmacenDTO ListaAcuentaPagoXFechasXTurnoF(String CodEmp, String Filtro)
    {
        return dao.ListaAcuentaPagoXFechasXTurno(CodEmp, Filtro);
    }

    public List<AlmacenDTO> ListaInventarioXRangoFechasF(String CodEmp, LocalDateTime FechaIni, LocalDateTime FechaFin)
    {
        return dao.ListaInventarioXRangoFechas(CodEmp, FechaIni, FechaFin);
    }

    public boolean GeneraMovimientoDiaStockF(AlmacenDTO almacen)
    {
        return dao.GeneraMovimientoDiaStock(almacen);
    }

    public boolean GeneraAperturaInventarioInicialF(AlmacenDTO almacen)
    {
        return dao.GeneraAperturaInventarioInicial(almacen);
    }

    public boolean EliminaInventarioF(AlmacenDTO almacen)
    {
        return dao.EliminaInventario(almacen);
    }
}
