package com.registro.controller;

import com.registro.dto.MarcasDTO;
import com.registro.facade.ProductosFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Marcas de productos. Lo usan dos páginas:
 *  - Marcas.xhtml: listado (sp_listaMarcaAct), búsqueda por nombre, editar y eliminar.
 *  - RegistrarMarca.xhtml: formulario para grabar y actualizar (sp_Mante_marcas).
 *  - Nuevo: limpia el formulario y autogenera el código (sp_max_marc).
 *  - Eliminar (botón de cada fila): sp_Elim_MarcaPro.
 */
@Named("marcaBean")
@SessionScoped
public class MarcaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ESTADO_ACTIVO = "Activo";
    private static final int MAX_NOMBRE = 100;

    private MarcasDTO marca;
    private boolean edicion;

    private List<MarcasDTO> listaCompleta = new ArrayList<>();
    private List<MarcasDTO> lista = new ArrayList<>();
    private String textoBusqueda;

    @PostConstruct
    public void init() {
        nuevo();
    }

    /** Al abrir Marcas.xhtml: consulta el listado con el último texto de búsqueda. */
    public void prepararListado() {
        if (FacesContext.getCurrentInstance().isPostback()) return;
        consultar();
    }

    /** Al abrir RegistrarMarca.xhtml: salvo que se venga a editar (?modo=editar), abre una nueva. */
    public void prepararFormulario() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx.isPostback()) return;
        String modo = ctx.getExternalContext().getRequestParameterMap().get("modo");
        if (!"editar".equals(modo) || !edicion) nuevo();
    }

    // ------------------------------------------------------------------ acciones

    public String nuevo() {
        marca = new MarcasDTO();
        marca.setEstadoMarca(ESTADO_ACTIVO);
        marca.setMarca_ID(siguienteId());
        edicion = false;
        return null;
    }

    public String grabar() {
        if (!validar()) return null;
        marca.setNombre(marca.getNombre().trim());
        if (!edicion) {
            marca.setMarca_ID(siguienteId());
            marca.setEstadoMarca(ESTADO_ACTIVO);
        }
        if (new ProductosFACADE().Mante_Marcas(marca)) {
            info((edicion ? "Se actualizó la marca " : "Se registró la marca ") + marca.getNombre() + ".");
            nuevo();
            return "/Marcas.xhtml?faces-redirect=true";
        }
        error("No se pudo guardar la marca.");
        return null;
    }

    /** Botón Editar de Marcas.xhtml: pasa la fila al formulario y abre RegistrarMarca.xhtml. */
    public String editar(MarcasDTO fila) {
        MarcasDTO m = new MarcasDTO();
        m.setMarca_ID(fila.getMarca_ID());
        m.setNombre(fila.getNombre() == null ? "" : fila.getNombre().trim());
        m.setEstadoMarca(fila.getEstadoMarca());
        marca = m;
        edicion = true;
        return "/RegistrarMarca.xhtml?faces-redirect=true&modo=editar";
    }

    public String eliminar(MarcasDTO fila) {
        if (new ProductosFACADE().Elim_MarcaPro(fila)) {
            info("Se eliminó la marca " + (fila.getNombre() == null ? "" : fila.getNombre().trim()) + ".");
            if (edicion && marca.getMarca_ID() == fila.getMarca_ID()) nuevo();
            consultar();
        } else {
            error("No se pudo eliminar la marca (puede tener productos asociados).");
        }
        return null;
    }

    /** Buscar: filtra por nombre el listado consultado a la BD. */
    public String buscar() {
        consultar();
        return null;
    }

    /** Listar: borra el filtro y muestra todas las marcas. */
    public String listar() {
        textoBusqueda = null;
        consultar();
        return null;
    }

    // ------------------------------------------------------------------ internos

    private void consultar() {
        List<MarcasDTO> datos = new ProductosFACADE().ListaMarcas();
        listaCompleta = datos == null ? new ArrayList<>() : datos;
        String t = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase();
        lista = new ArrayList<>();
        for (MarcasDTO m : listaCompleta) {
            String n = m.getNombre() == null ? "" : m.getNombre().trim().toLowerCase();
            if (t.isEmpty() || n.contains(t)) lista.add(m);
        }
    }

    private int siguienteId() {
        MarcasDTO m = new ProductosFACADE().AutogeneraMarca();
        return m == null ? 0 : m.getMarca_ID();
    }

    private boolean validar() {
        String nombre = marca.getNombre() == null ? "" : marca.getNombre().trim();
        if (nombre.isEmpty()) {
            error("Ingrese el nombre de la marca.");
            return false;
        }
        if (nombre.length() > MAX_NOMBRE) {
            error("El nombre de la marca no puede superar los " + MAX_NOMBRE + " caracteres.");
            return false;
        }
        for (MarcasDTO m : new ProductosFACADE().ListaMarcas()) {
            boolean otra = m.getMarca_ID() != marca.getMarca_ID();
            if (otra && m.getNombre() != null && m.getNombre().trim().equalsIgnoreCase(nombre)) {
                error("Ya existe una marca con ese nombre.");
                return false;
            }
        }
        return true;
    }

    /** Los mensajes se conservan aunque la acción redirija a otra página. */
    private static void info(String texto) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, texto, null));
        ctx.getExternalContext().getFlash().setKeepMessages(true);
    }

    private static void error(String texto) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
        ctx.getExternalContext().getFlash().setKeepMessages(true);
    }

    // ------------------------------------------------------------------ getters / setters

    public MarcasDTO getMarca() { return marca; }
    public boolean isEdicion() { return edicion; }
    public List<MarcasDTO> getLista() { return lista; }
    public int getTotal() { return listaCompleta.size(); }
    public int getTotalResultados() { return lista.size(); }
    public String getTextoBusqueda() { return textoBusqueda; }
    public void setTextoBusqueda(String textoBusqueda) { this.textoBusqueda = textoBusqueda; }

    public int getTotalActivas() {
        int n = 0;
        for (MarcasDTO m : listaCompleta) {
            if (m.getEstadoMarca() == null || m.getEstadoMarca().trim().equalsIgnoreCase(ESTADO_ACTIVO)) n++;
        }
        return n;
    }

    public String codigo(MarcasDTO m) {
        return String.format("MAR-%03d", m.getMarca_ID());
    }
}
