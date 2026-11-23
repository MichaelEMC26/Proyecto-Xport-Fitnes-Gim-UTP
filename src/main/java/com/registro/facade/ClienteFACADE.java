package com.registro.facade;

import com.registro.dto.ClienteDTO;
import com.registro.dao.ClienteDAO;
import java.sql.Date;
import java.util.List;

public class ClienteFACADE {
    
    private final ClienteDAO dao = new ClienteDAO();
    
    public List<ClienteDTO> ObtenerListadoCliente() {
        return dao.ListaClientes();
    }
    
    /** Cantidad de socios activos (COUNT, sin descargar la lista). */
    public int ContarClientesActivos() {
        return dao.ContarClientesActivos();
    }

    public ClienteDTO buscarCliente(String id) {
        return dao.buscarPorId(id);
    }

    public ClienteDTO autogenera_CodCliente() {
        return dao.autogenera_CodCliente();
    }

    public boolean registrarCliente(ClienteDTO Cliente) {
        return dao.registrarCliente(Cliente);
    }

    public boolean actualizarCliente(ClienteDTO Cliente) {
        return dao.actualizarCliente(Cliente);
    }
    
    public boolean EliminarCliente(ClienteDTO Cliente) {
        return dao.EliminaCliente(Cliente);
    }
    
    public ClienteDTO ListaClienteXCod(String Cliente_ID)
    {
        return dao.ListaClienteXCod(Cliente_ID);
    }
    public ClienteDTO ListaClienteXRucDni(String RucDNI)
    {
        return dao.ListaClienteXRucDni(RucDNI);
    }
    public List<ClienteDTO> ListaClientesXNombre(String RazonSocial)
    {
        return dao.ListaClientesXNombre(RazonSocial);
    }
    
    public List<ClienteDTO> MuestraFechaCumpleañosActualSocioF(Date FechaActual)
    {
        return dao.MuestraFechaCumpleañosActualSocio(FechaActual);
    }
    public List<ClienteDTO> MuestraFechasProximasCumpleañosSocioF(Date FechaIni,Date FechaFin)
    {
        return dao.MuestraFechasProximasCumpleañosSocio(FechaIni,FechaFin);
    }
}