package com.registro.controller;

import com.registro.dto.AlmacenDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.AlmacenFACADE;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Almacen.xhtml (solo consulta):
 *  - Inventario del almacén: sp_ReporteExistenciasProductos (AlmacenFACADE.ReporteExistenciasProductosF).
 *    La búsqueda (producto, código, categoría o marca) y el estado se aplican sobre ese listado.
 *  - Tarjetas y barras superiores: sp_ListaMontosExistenciasProductos (AlmacenFACADE.ListaMontosExistenciasProductosF).
 *  - Buscar / Limpiar: Buscar vuelve a consultar la BD con el filtro actual; Limpiar borra texto y estado.
 *  La paginación sigue en el navegador (paginacion.js, data-paginar).
 */
@Named("almacenBean")
@SessionScoped
public class AlmacenBean implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Con 5 unidades o menos el producto se considera en stock bajo (misma regla de la plantilla). */
    private static final double STOCK_BAJO = 5;
    private static final String EST_DISPONIBLE = "disponible";
    private static final String EST_BAJO = "bajo";
    private static final String EST_AGOTADO = "agotado";

    /** Filtro enviado a los SP: vacío = todos los productos. */
    private static final String FILTRO_SP = "";

    private String textoBusqueda;
    /** "" (todos), disponible, bajo o agotado. */
    private String estadoFiltro = "";

    private List<AlmacenDTO> listaCompleta = new ArrayList<>();
    private List<AlmacenDTO> lista = new ArrayList<>();
    private AlmacenDTO montos;
    private double unidades;

    /** Al abrir Almacen.xhtml: consulta existencias y totales con el último filtro usado. */
    public void prepararListado() {
        if (FacesContext.getCurrentInstance().isPostback()) return;
        consultar();
    }

    // ------------------------------------------------------------------ acciones

    public String buscar() {
        consultar();
        return null;
    }

    public String limpiar() {
        textoBusqueda = null;
        estadoFiltro = "";
        consultar();
        return null;
    }

    // ------------------------------------------------------------------ internos

    private void consultar() {
        AlmacenFACADE f = new AlmacenFACADE();
        String emp = codEmp();

        List<AlmacenDTO> datos = f.ReporteExistenciasProductosF(emp, FILTRO_SP);
        listaCompleta = datos == null ? new ArrayList<>() : datos;
        montos = f.ListaMontosExistenciasProductosF(emp, FILTRO_SP);

        unidades = 0;
        for (AlmacenDTO a : listaCompleta) unidades += a.getStockFinal();

        String t = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase();
        String est = estadoFiltro == null ? "" : estadoFiltro;
        lista = new ArrayList<>();
        for (AlmacenDTO a : listaCompleta) {
            if (!est.isEmpty() && !est.equals(estado(a))) continue;
            if (!t.isEmpty() && !contiene(a, t)) continue;
            lista.add(a);
        }
    }

    private static boolean contiene(AlmacenDTO a, String t) {
        String contenido = (nz(a.getProductos()) + " " + nz(a.getidproducto()) + " "
                + nz(a.getCategoria()) + " " + nz(a.getMarcaProducto())).toLowerCase();
        return contenido.contains(t);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** Empresa del usuario que inició sesión; "01" si no tiene (igual que CatalogoBean). */
    private String codEmp() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioAutenticado");
        if (u instanceof UsuarioDTO dto && dto.getCodEmp() != null && !dto.getCodEmp().isBlank()) {
            return dto.getCodEmp().trim();
        }
        return "01";
    }

    // ------------------------------------------------------------------ estado de cada fila

    /** disponible, bajo o agotado según el stock final. */
    public String estado(AlmacenDTO a) {
        double s = a.getStockFinal();
        if (s <= 0) return EST_AGOTADO;
        if (s <= STOCK_BAJO) return EST_BAJO;
        return EST_DISPONIBLE;
    }

    public String etiquetaEstado(AlmacenDTO a) {
        switch (estado(a)) {
            case EST_AGOTADO: return "Agotado";
            case EST_BAJO: return "Stock bajo";
            default: return "Disponible";
        }
    }

    /** Clase extra de la celda de stock: alerta, agotado o vacío. */
    public String claseStock(AlmacenDTO a) {
        switch (estado(a)) {
            case EST_AGOTADO: return "agotado";
            case EST_BAJO: return "alerta";
            default: return "";
        }
    }

    // ------------------------------------------------------------------ tarjetas y barras

    /** Productos registrados (CantItems del SP; si no hay datos, los del listado). */
    public int getTotalProductos() {
        return montos != null ? (int) Math.round(montos.getCantItems()) : listaCompleta.size();
    }

    public double getUnidadesDisponibles() { return unidades; }

    public int getProductosAgotados() {
        return montos != null ? (int) Math.round(montos.getCantItemsSinStock()) : 0;
    }

    public int getProductosStockBajo() {
        return montos != null ? (int) Math.round(montos.getCantItemsStockMin()) : 0;
    }

    public int getProductosDisponibles() {
        return Math.max(getTotalProductos() - getProductosAgotados() - getProductosStockBajo(), 0);
    }

    public int getRequierenReposicion() { return getProductosAgotados() + getProductosStockBajo(); }

    public double getTotalValorizado() { return montos == null ? 0 : montos.getTotalValorizado(); }

    public int getPorcentajeDisponible() { return porcentaje(getProductosDisponibles()); }
    public int getPorcentajeStockBajo() { return porcentaje(getProductosStockBajo()); }
    public int getPorcentajeAgotado() { return porcentaje(getProductosAgotados()); }

    private int porcentaje(int parte) {
        int total = getTotalProductos();
        return total <= 0 ? 0 : Math.min(100, (int) Math.round(parte * 100.0 / total));
    }

    /** "1 producto" / "N productos". */
    public String textoProductos(int n) {
        return n + (n == 1 ? " producto" : " productos");
    }

    // ------------------------------------------------------------------ getters / setters

    public List<AlmacenDTO> getLista() { return lista; }
    public int getTotalResultados() { return lista.size(); }
    public String getTextoBusqueda() { return textoBusqueda; }
    public void setTextoBusqueda(String textoBusqueda) { this.textoBusqueda = textoBusqueda; }
    public String getEstadoFiltro() { return estadoFiltro; }
    public void setEstadoFiltro(String estadoFiltro) { this.estadoFiltro = estadoFiltro; }
}
