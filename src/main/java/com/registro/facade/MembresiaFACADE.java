/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.facade;

import java.util.List;
import java.util.ArrayList;
import com.registro.dto.MembresiaDTO;
import com.registro.dao.MembresiaDAO;
/**
 *
 * @author Ing
 */
public class MembresiaFACADE {
    
    private MembresiaDAO dao = new MembresiaDAO();
    
    public ArrayList<MembresiaDTO> Consulta_HistorialMembresiasF()
    {
        return dao.Consulta_HistorialMembresias();
    }
    
    public MembresiaDTO generasuscripciones_MembresiaF(String CodEmp)
    {
        return dao.generasuscripciones_Membresia(CodEmp);
    }
    public MembresiaDTO Listasuscripciones_MembresiaXRegistroF(String CodEmp,String NroSuscripcion_id)
    {
        return dao.Listasuscripciones_MembresiaXRegistro(CodEmp,NroSuscripcion_id);
    }
    
    public boolean InsertSuscripciones_MembresiaF(MembresiaDTO membresias)
    {
        return dao.InsertSuscripciones_Membresia(membresias);
    }
    public boolean Eliminasuscripciones_MembresiaF(MembresiaDTO membresias)
    {
        return dao.Eliminasuscripciones_Membresia(membresias);
    }
    
    public MembresiaDTO ListaDetSuscripcionPeriodoClasesF(String NroSuscripcion_id, String Cliente_ID)
    {
        return dao.ListaDetSuscripcionPeriodoClases(NroSuscripcion_id,Cliente_ID);
    }
    public ArrayList<MembresiaDTO> ListaFrecuenciaF()
    {
        return dao.ListaFrecuencia();
    }
    public ArrayList<MembresiaDTO> ListaMembresiaF()
    {
        return dao.ListaMembresia();
    }
    public MembresiaDTO ListaMembresiaXCodF(String CodigoM)
    {
        return dao.ListaMembresiaXCod(CodigoM);
    }
    public String autogeneraCodMembresiaF()
    {
        return dao.autogeneraCodMembresia();
    }
    public MembresiaDTO Listasuscripciones_MembresiaXCodMembresiaF(String CodEmp, String CodigoM)
    {
        return dao.Listasuscripciones_MembresiaXCodMembresia(CodEmp,CodigoM);
    }
    
    public boolean InsertMembresiaF(MembresiaDTO membresia)
    {
        return dao.InsertMembresia(membresia);
    }
    public boolean UpdateMembresiaF(MembresiaDTO membresia)
    {
        return dao.UpdateMembresia(membresia);
    }
    public boolean DeleteMembresiaF(MembresiaDTO membresia)
    {
        return dao.DeleteMembresia(membresia);
    }
    
    public boolean InsertDiasMembresiaF(MembresiaDTO membresia)
    {
        return dao.InsertDiasMembresia(membresia);
    }
    public boolean DeleteDiasMembresiaF(MembresiaDTO membresia)
    {
        return dao.DeleteDiasMembresia(membresia);
    }

    // =====================================================================
    // Igual que Ven_FacturaN de SisdataGimnasio (frm_RegistroMembresia)
    // =====================================================================

    /** Cronograma (IDSecuencia, Rutina, Asistio) de la suscripción. */
    public ArrayList<MembresiaDTO> ListaCronogramaSuscripcionF(String NroSuscripcion_id, String Cliente_ID)
    {
        return dao.ListaCronogramaSuscripcion(NroSuscripcion_id, Cliente_ID);
    }

    /**
     * Anula la suscripción (Eliminasuscripciones_MembresiaN): no se puede si su estado es "Efectiva".
     * Devuelve "OK" o el mensaje a mostrar.
     */
    public String AnularSuscripcionF(String CodEmp, String NroSuscripcion_id)
    {
        MembresiaDTO reg = dao.Listasuscripciones_MembresiaXRegistro(CodEmp, NroSuscripcion_id);
        if (reg == null) {
            return "El registro de suscripción no existe.";
        }
        String estado = reg.getEstadoSusc() == null ? "" : reg.getEstadoSusc().trim();
        if (estado.equalsIgnoreCase("Efectiva")) {
            return "No se puede anular la suscripción porque su condición actual es " + estado;
        }
        MembresiaDTO eliminar = new MembresiaDTO();
        eliminar.setCodEmp(CodEmp);
        eliminar.setNroSuscripcion_id(NroSuscripcion_id);
        return dao.Eliminasuscripciones_Membresia(eliminar)
                ? "OK"
                : "Ocurrió un error en la base de datos al intentar anular la suscripción.";
    }

    /** Última membresía terminada del cliente (FechaFin más reciente); null si no tiene. */
    public MembresiaDTO UltimaMembresiaTerminadaF(String CodEmp, String Cliente_ID)
    {
        return membresiaDelCliente(CodEmp, Cliente_ID, true);
    }

    /**
     * sp_Listasuscripciones_Membresia: Opc 1 = activas (y terminadas con deuda), Opc 2 = terminadas.
     * Consulta de suscripciones (frm_ConsultaMembresias).
     */
    public ArrayList<MembresiaDTO> Listasuscripciones_MembresiaF(String CodEmp, int Opc)
    {
        return dao.Listasuscripciones_Membresia(CodEmp, Opc);
    }

    /** Membresía vigente (no terminada) del cliente, la de FechaFin más reciente; null si no tiene. */
    public MembresiaDTO MembresiaVigenteClienteF(String CodEmp, String Cliente_ID)
    {
        return membresiaDelCliente(CodEmp, Cliente_ID, false);
    }

    /** Terminada = Condicion TERMINADA o FechaFin anterior a hoy. Usa sp_Listasuscripciones_Membresia (Opc 1 y 2). */
    private MembresiaDTO membresiaDelCliente(String CodEmp, String Cliente_ID, boolean terminada)
    {
        String codcli = Cliente_ID == null ? "" : Cliente_ID.trim();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        MembresiaDTO mejor = null;
        for (int opc : new int[] { 1, 2 }) {
            for (MembresiaDTO r : dao.Listasuscripciones_Membresia(CodEmp, opc)) {
                if (!codcli.equals(r.getCliente() == null ? "" : r.getCliente().trim())) continue;
                if (r.getFechaFin() == null) continue;
                String cond = r.getCondicion() == null ? "" : r.getCondicion().trim().toUpperCase();
                boolean esTerminada = cond.startsWith("TERMIN") || r.getFechaFin().toLocalDate().isBefore(hoy);
                if (esTerminada != terminada) continue;
                if (mejor == null || r.getFechaFin().after(mejor.getFechaFin())) mejor = r;
            }
        }
        return mejor;
    }
}
