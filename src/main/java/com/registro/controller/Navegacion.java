package com.registro.controller;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda desde dónde se abrió cada página para que "Salir" regrese ahí.
 * Los botones del Dashboard abren las páginas con ?desde=dashboard; entonces Salir vuelve al Dashboard.
 * Si la página se abre desde otra (su listado, el menú), se olvida y Salir usa su destino de siempre.
 */
@WebFilter(urlPatterns = {"*.xhtml"})
public class Navegacion implements Filter {

    private static final String ATRIBUTO = "navegacion.origen";
    private static final String DASHBOARD = "/dashboard.xhtml";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession session = req.getSession(false);
        if (session != null && "GET".equalsIgnoreCase(req.getMethod())) {
            String pagina = req.getServletPath();
            if ("dashboard".equals(req.getParameter("desde"))) {
                origenes(session).put(pagina, DASHBOARD);
            } else {
                String anterior = paginaReferer(req);
                // Abierta desde otra página (no un redirect de la misma tras grabar): se olvida el origen
                if (anterior != null && !anterior.equals(pagina)) origenes(session).remove(pagina);
            }
        }
        chain.doFilter(request, response);
    }

    /** Destino de "Salir" para la vista actual: el Dashboard si se abrió desde ahí; si no, porDefecto. */
    public static String salir(String porDefecto) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Object valor = fc.getExternalContext().getSessionMap().get(ATRIBUTO);
        if (valor instanceof Map && fc.getViewRoot() != null) {
            Object origen = ((Map<?, ?>) valor).remove(fc.getViewRoot().getViewId());
            if (origen != null) return origen + "?faces-redirect=true";
        }
        return porDefecto;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> origenes(HttpSession session) {
        Object valor = session.getAttribute(ATRIBUTO);
        if (valor instanceof Map) return (Map<String, String>) valor;
        Map<String, String> mapa = new ConcurrentHashMap<>();
        session.setAttribute(ATRIBUTO, mapa);
        return mapa;
    }

    /** Ruta de la página anterior (Referer) dentro de esta aplicación, p. ej. "/ListaClientes.xhtml". */
    private static String paginaReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer == null) return null;
        try {
            String ruta = URI.create(referer).getPath();
            String ctx = req.getContextPath();
            if (ruta == null || !ruta.startsWith(ctx)) return null;
            return ruta.substring(ctx.length());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
