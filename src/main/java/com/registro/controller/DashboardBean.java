package com.registro.controller;

import com.registro.dto.AperturaCierreCajaDTO;
import com.registro.dto.ClienteDTO;
import com.registro.dto.MembresiaDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.AperturaCierreCajaFACADE;
import com.registro.facade.ClienteFACADE;
import com.registro.facade.MembresiaFACADE;
import com.registro.facade.UsuarioFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Datos del Dashboard.
 * - Registros del sistema del día y Ventas del día: solo áreas Administración y Sistemas.
 * - Clientes activos, clientes por vencer y cumpleaños del día: cualquier usuario.
 */
@Named("dashboardBean")
@RequestScoped
public class DashboardBean implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Suscripciones activas que terminan dentro de estos días cuentan como "por vencer". */
    static final int DIAS_POR_VENCER = 7;

    private Map<String, Integer> resumenPersonal = Collections.emptyMap();
    private boolean areaAdministrativa;
    private List<UsuarioDTO> registrosDia = Collections.emptyList();
    private double ventasDia;
    private int clientesActivos;
    private int clientesPorVencer;
    private List<ClienteDTO> cumpleanos = Collections.emptyList();

    @PostConstruct
    public void init() {
        UsuarioDTO u = MembresiaBean.usuarioEnSesion();
        areaAdministrativa = u != null && esAreaAdministracionOSistemas(u.getArea());
        String codEmp = MembresiaBean.codEmpSesion();
        LocalDate hoy = LocalDate.now();

        try { resumenPersonal = new UsuarioFACADE().resumenPersonal(); }
        catch (RuntimeException ex) { resumenPersonal = Collections.emptyMap(); }

        if (areaAdministrativa) {
            try { registrosDia = new UsuarioFACADE().ListaAuditoria_SistemaDiaF(hoy.atStartOfDay()); }
            catch (RuntimeException ex) { registrosDia = Collections.emptyList(); }
            try { ventasDia = ventasDelDia(codEmp, hoy.atStartOfDay()); }
            catch (RuntimeException ex) { ventasDia = 0; }
        }

        try { clientesActivos = new ClienteFACADE().ContarClientesActivos(); }   // COUNT(*): no descarga la lista
        catch (RuntimeException ex) { clientesActivos = 0; }

        try { clientesPorVencer = porVencer(codEmp, hoy); }
        catch (RuntimeException ex) { clientesPorVencer = 0; }

        try { cumpleanos = new ClienteFACADE().MuestraFechaCumpleañosActualSocioF(java.sql.Date.valueOf(hoy)); }
        catch (RuntimeException ex) { cumpleanos = Collections.emptyList(); }
    }

    /** Área Administración o Sistemas (sin importar mayúsculas, tildes ni espacios). */
    static boolean esAreaAdministracionOSistemas(String area) {
        if (area == null) return false;
        String a = area.trim().toUpperCase(Locale.ROOT);
        return a.contains("ADMINISTRA") || a.contains("SISTEMA");
    }

    /** Suma la venta de todos los turnos del día (sp_lista_Monto_VentaDiaXTurno, turnos 1..último). */
    private static double ventasDelDia(String codEmp, LocalDateTime fecha) {
        AperturaCierreCajaFACADE caja = new AperturaCierreCajaFACADE();
        AperturaCierreCajaDTO siguiente = caja.ListaTurnoDiaF(codEmp, fecha);   // MAX(Turno)+1
        int ultimo;
        try { ultimo = siguiente == null ? 0 : Integer.parseInt(siguiente.getTurno().trim()) - 1; }
        catch (NumberFormatException ex) { ultimo = 0; }
        double total = 0;
        for (int t = 1; t <= ultimo; t++) total += caja.MontoVentaDiaXTurnoF(codEmp, fecha, String.valueOf(t));
        return total;
    }

    /** Socios con suscripción ACTIVA que termina entre hoy y los próximos DIAS_POR_VENCER días. */
    private static int porVencer(String codEmp, LocalDate hoy) {
        LocalDate limite = hoy.plusDays(DIAS_POR_VENCER);
        Set<String> socios = new HashSet<>();
        for (MembresiaDTO s : new MembresiaFACADE().Listasuscripciones_MembresiaF(codEmp, 1)) {
            if (s.getFechaFin() == null || !"ACTIVA".equalsIgnoreCase(s.getCondicion())) continue;
            LocalDate fin = s.getFechaFin().toLocalDate();
            if (!fin.isBefore(hoy) && !fin.isAfter(limite) && s.getcliente_id() != null) socios.add(s.getcliente_id().trim());
        }
        return socios.size();
    }

    public Map<String, Integer> getResumenPersonal() { return resumenPersonal; }
    public int getPersonalActivo() { return resumenPersonal.getOrDefault("activos", 0); }
    public int getPersonalConAcceso() { return resumenPersonal.getOrDefault("conAcceso", 0); }

    public boolean isAreaAdministrativa() { return areaAdministrativa; }
    public List<UsuarioDTO> getRegistrosDia() { return registrosDia; }
    public double getVentasDia() { return ventasDia; }
    public int getClientesActivos() { return clientesActivos; }
    public int getClientesPorVencer() { return clientesPorVencer; }
    public int getDiasPorVencer() { return DIAS_POR_VENCER; }
    public List<ClienteDTO> getCumpleanos() { return cumpleanos; }
}
