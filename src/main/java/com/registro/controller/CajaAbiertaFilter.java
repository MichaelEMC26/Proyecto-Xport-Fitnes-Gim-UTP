package com.registro.controller;

import com.registro.dto.UsuarioDTO;
import com.registro.facade.AperturaCierreCajaFACADE;
import com.registro.facade.MembresiaFACADE;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

/**
 * Pide apertura de caja antes de entrar a los procesos que la necesitan, igual que
 * frm_Menu.AbrirConCaja de SisdataGimnasio: si no hay turno abierto (sp_ListaTurnoAbierto con la
 * fecha de hoy) se va a la Apertura de Caja y, al aperturar, se continúa al proceso pedido.
 * Solo se revisa al abrir la página (GET), no en los envíos del formulario.
 * Registrar Asistencia además exige que exista al menos una suscripción de membresía registrada;
 * si no hay ninguna, se vuelve al Dashboard con un aviso (no se llega a pedir la caja).
 */
@WebFilter(urlPatterns = {"/RegistrarAsistencia.xhtml", "/Ventas.xhtml", "/Registro-plan-membresia.xhtml", "/membresias.xhtml", "/PuntoVenta.xhtml"})
public class CajaAbiertaFilter implements Filter {

    /** Páginas que requieren caja abierta (mismas que urlPatterns). AperturaCajaBean solo regresa a estas. */
    static final Set<String> PAGINAS_CON_CAJA = Set.of(
            "/RegistrarAsistencia.xhtml", "/Ventas.xhtml", "/Registro-plan-membresia.xhtml", "/membresias.xhtml", "/PuntoVenta.xhtml");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        Object usuario = session == null ? null : session.getAttribute("usuarioAutenticado");
        // Sin sesión: AutenticacionFilter se encarga. Postbacks/ajax: la caja ya se revisó al abrir.
        if (!(usuario instanceof UsuarioDTO) || !"GET".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String codEmp = codEmp((UsuarioDTO) usuario);

        // Registrar Asistencia: sin suscripciones de membresía registradas no hay a quién controlar
        if (PAGINA_ASISTENCIA.equals(req.getServletPath()) && !haySuscripciones(codEmp)) {
            res.sendRedirect(req.getContextPath() + "/dashboard.xhtml?aviso=sin-suscripciones");
            return;
        }

        if (hayTurnoAbierto(codEmp)) {
            chain.doFilter(request, response);
            return;
        }

        String destino = req.getServletPath() + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
        res.sendRedirect(req.getContextPath() + "/AperturaCaja.xhtml?destino="
                + URLEncoder.encode(destino, StandardCharsets.UTF_8));
    }

    static final String PAGINA_ASISTENCIA = "/RegistrarAsistencia.xhtml";

    /**
     * Hay al menos una suscripción de membresía registrada (no anulada):
     * sp_Listasuscripciones_Membresia con Opc 1 devuelve las ACTIVAS y TERMINADAS.
     */
    /** Empresas que ya tienen suscripciones (una vez que hay, no vuelve a quedar en cero). */
    private static final Set<String> CON_SUSCRIPCIONES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static boolean haySuscripciones(String codEmp) {
        // Rendimiento: cuando ya hay suscripciones no se vuelve a consultar (antes se descargaba la lista
        // completa de suscripciones en cada ingreso a Registrar Asistencia solo para saber si estaba vacía).
        if (CON_SUSCRIPCIONES.contains(codEmp)) return true;
        try {
            boolean hay = !new MembresiaFACADE().Listasuscripciones_MembresiaF(codEmp, 1).isEmpty();
            if (hay) CON_SUSCRIPCIONES.add(codEmp);
            return hay;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return true;   // si falla la consulta no se bloquea el sistema
        }
    }

    /** HayTurnoAbierto(HoyMedianoche): sp_ListaTurnoAbierto devuelve filas. */
    static boolean hayTurnoAbierto(String codEmp) {
        try {
            return new AperturaCierreCajaFACADE().ListaTurnoAbiertoF(codEmp, LocalDate.now().atStartOfDay()) != null;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return true;   // si falla la consulta no se bloquea el sistema
        }
    }

    private static String codEmp(UsuarioDTO u) {
        String cod = u.getCodEmp();
        return cod == null || cod.trim().isEmpty() ? MembresiaBean.COD_EMP_DEFECTO : cod.trim();
    }
}
