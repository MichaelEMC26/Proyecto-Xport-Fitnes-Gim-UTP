package com.registro.controller;

import com.registro.dto.CategoriaDTO;
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
 * Categorías de productos. Lo usan dos páginas:
 *  - Categorias.xhtml: listado (sp_lista_categoriasAct), búsqueda por nombre, editar y eliminar.
 *  - RegistrarCategoria.xhtml: formulario para grabar y actualizar (sp_Mante_Categorias).
 *  - Nuevo: limpia el formulario y autogenera el código (sp_max_cod_categoria).
 *  - Eliminar (botón de cada fila): sp_Elimina_Categorias.
 */
@Named("categoriaBean")
@SessionScoped
public class CategoriaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ESTADO_ACTIVO = "Activo";
    private static final int MAX_NOMBRE = 60;

    private CategoriaDTO categoria;
    private boolean edicion;

    /** Todas las categorías devueltas por la BD y las que se muestran tras filtrar. */
    private List<CategoriaDTO> listaCompleta = new ArrayList<>();
    private List<CategoriaDTO> lista = new ArrayList<>();
    private String textoBusqueda;

    @PostConstruct
    public void init() {
        nuevo();
    }

    /** Al abrir Categorias.xhtml: consulta el listado con el último texto de búsqueda. */
    public void prepararListado() {
        if (FacesContext.getCurrentInstance().isPostback()) return;
        consultar();
    }

    /** Al abrir RegistrarCategoria.xhtml: salvo que se venga a editar (?modo=editar), abre una nueva. */
    public void prepararFormulario() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx.isPostback()) return;
        String modo = ctx.getExternalContext().getRequestParameterMap().get("modo");
        if (!"editar".equals(modo) || !edicion) nuevo();
    }

    // ------------------------------------------------------------------ acciones

    public String nuevo() {
        categoria = new CategoriaDTO();
        categoria.setEstado(ESTADO_ACTIVO);
        categoria.setCategoria_ID(siguienteId());
        edicion = false;
        return null;
    }

    public String grabar() {
        if (!validar()) return null;
        categoria.setNombre(categoria.getNombre().trim());
        boolean ok;
        if (edicion) {
            ok = new ProductosFACADE().Mante_Categorias(categoria);
        } else {
            categoria.setCategoria_ID(siguienteId());
            categoria.setEstado(ESTADO_ACTIVO);
            ok = new ProductosFACADE().Mante_Categorias(categoria);
        }
        if (ok) {
            info((edicion ? "Se actualizó la categoría " : "Se registró la categoría ") + categoria.getNombre() + ".");
            nuevo();
            return "/Categorias.xhtml?faces-redirect=true";
        }
        error("No se pudo guardar la categoría.");
        return null;
    }

    /** Botón Editar de Categorias.xhtml: pasa la fila al formulario y abre RegistrarCategoria.xhtml. */
    public String editar(CategoriaDTO fila) {
        CategoriaDTO c = new CategoriaDTO();
        c.setCategoria_ID(fila.getCategoria_ID());
        c.setNombre(fila.getNombre() == null ? "" : fila.getNombre().trim());
        c.setEstado(fila.getEstado());
        c.setrutaimagen(fila.getrutaimagen());
        categoria = c;
        edicion = true;
        return "/RegistrarCategoria.xhtml?faces-redirect=true&modo=editar";
    }

    public String eliminar(CategoriaDTO fila) {
        if (new ProductosFACADE().Elimina_Categorias(fila)) {
            info("Se eliminó la categoría " + (fila.getNombre() == null ? "" : fila.getNombre().trim()) + ".");
            if (edicion && categoria.getCategoria_ID() == fila.getCategoria_ID()) nuevo();
            consultar();
        } else {
            error("No se pudo eliminar la categoría (puede tener productos asociados).");
        }
        return null;
    }

    /** Buscar: filtra por nombre el listado ya consultado a la BD. */
    public String buscar() {
        consultar();
        return null;
    }

    /** Listar: borra el filtro y muestra todas las categorías. */
    public String listar() {
        textoBusqueda = null;
        consultar();
        return null;
    }

    // ------------------------------------------------------------------ internos

    private void consultar() {
        List<CategoriaDTO> datos = new ProductosFACADE().ListaCategorias();
        listaCompleta = datos == null ? new ArrayList<>() : datos;
        String t = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase();
        lista = new ArrayList<>();
        for (CategoriaDTO c : listaCompleta) {
            String n = c.getNombre() == null ? "" : c.getNombre().trim().toLowerCase();
            if (t.isEmpty() || n.contains(t)) lista.add(c);
        }
    }

    private int siguienteId() {
        CategoriaDTO c = new ProductosFACADE().autogeneraCategoria();
        return c == null ? 0 : c.getCategoria_ID();
    }

    private boolean validar() {
        String nombre = categoria.getNombre() == null ? "" : categoria.getNombre().trim();
        if (nombre.isEmpty()) {
            error("Ingrese el nombre de la categoría.");
            return false;
        }
        if (nombre.length() > MAX_NOMBRE) {
            error("El nombre de la categoría no puede superar los " + MAX_NOMBRE + " caracteres.");
            return false;
        }
        for (CategoriaDTO c : new ProductosFACADE().ListaCategorias()) {
            boolean otra = c.getCategoria_ID() != categoria.getCategoria_ID();
            if (otra && c.getNombre() != null && c.getNombre().trim().equalsIgnoreCase(nombre)) {
                error("Ya existe una categoría con ese nombre.");
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

    public CategoriaDTO getCategoria() { return categoria; }
    public boolean isEdicion() { return edicion; }
    public List<CategoriaDTO> getLista() { return lista; }
    public int getTotal() { return listaCompleta.size(); }
    public int getTotalResultados() { return lista.size(); }
    public String getTextoBusqueda() { return textoBusqueda; }
    public void setTextoBusqueda(String textoBusqueda) { this.textoBusqueda = textoBusqueda; }

    public int getTotalActivas() {
        int n = 0;
        for (CategoriaDTO c : listaCompleta) {
            if (c.getEstado() == null || c.getEstado().trim().equalsIgnoreCase(ESTADO_ACTIVO)) n++;
        }
        return n;
    }

    public String codigo(CategoriaDTO c) {
        return String.format("CAT-%03d", c.getCategoria_ID());
    }
}
