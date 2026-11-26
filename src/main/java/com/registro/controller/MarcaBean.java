/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.registro.controller;

import com.registro.dto.MarcasDTO;
import com.registro.facade.CatalogoFACADE;
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
    private static final String CRITERIO_NOMBRE = "NOMBRE";
    private static final String CRITERIO_CODIGO = "CODIGO";
    private static final int MAX_NOMBRE = 50;  // @Nombre varchar(50) en sp_Mante_marcas

    private MarcasDTO marca;
    private boolean edicion;

    private List<MarcasDTO> listaCompleta = new ArrayList<>();
    private List<MarcasDTO> lista = new ArrayList<>();
    private String textoBusqueda;
    /** Condición del filtro (chips de la plantilla): NOMBRE o CODIGO. */
    private String criterioBusqueda = CRITERIO_NOMBRE;

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
        if (new CatalogoFACADE().Mante_Marcas(marca)) {
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
        if (new CatalogoFACADE().Elim_MarcaPro(fila)) {
            info("Se eliminó la marca " + (fila.getNombre() == null ? "" : fila.getNombre().trim())
                    + ". Si tenía productos asociados, quedó Inactiva.");
            if (edicion && marca.getMarca_ID() == fila.getMarca_ID()) nuevo();
            consultar();
        } else {
            error("No se pudo eliminar la marca.");
        }
        return null;
    }

    /** Buscar: filtra por nombre o código el listado consultado a la BD. */
    public String buscar() {
        consultar();
        return null;
    }

    /** Listar: borra el filtro y muestra todas las marcas. */
    public String listar() {
        textoBusqueda = null;
        criterioBusqueda = CRITERIO_NOMBRE;
        consultar();
        return null;
    }

    // ------------------------------------------------------------------ internos

    private void consultar() {
        List<MarcasDTO> datos = new CatalogoFACADE().ListaMarcas();
        listaCompleta = datos == null ? new ArrayList<>() : datos;
        String t = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase();
        boolean porCodigo = CRITERIO_CODIGO.equals(criterioBusqueda);
        lista = new ArrayList<>();
        for (MarcasDTO m : listaCompleta) {
            if (t.isEmpty() || coincide(m, t, porCodigo)) lista.add(m);
        }
    }

    /** Código: el número debe empezar con lo escrito (12 encuentra 12, 120...). Nombre: contiene el texto. */
    private static boolean coincide(MarcasDTO m, String t, boolean porCodigo) {
        if (porCodigo) return String.valueOf(m.getMarca_ID()).startsWith(t);
        String n = m.getNombre() == null ? "" : m.getNombre().trim().toLowerCase();
        return n.contains(t);
    }

    private int siguienteId() {
        MarcasDTO m = new CatalogoFACADE().AutogeneraMarca();
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
        for (MarcasDTO m : new CatalogoFACADE().ListaMarcas()) {
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
    public String getCriterioBusqueda() { return criterioBusqueda; }
    public void setCriterioBusqueda(String criterioBusqueda) {
        this.criterioBusqueda = CRITERIO_CODIGO.equals(criterioBusqueda) ? CRITERIO_CODIGO : CRITERIO_NOMBRE;
    }

    public int getTotalActivas() {
        int n = 0;
        for (MarcasDTO m : listaCompleta) {
            if (m.getEstadoMarca() == null || m.getEstadoMarca().trim().equalsIgnoreCase(ESTADO_ACTIVO)) n++;
        }
        return n;
    }

    public String codigo(MarcasDTO m) {
        return String.valueOf(m.getMarca_ID());   // código numérico, tal como está en la BD
    }
}