/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.registro.controller;

import com.registro.config.AlmacenImagenesProducto;
import com.registro.dto.CategoriaDTO;
import com.registro.facade.CatalogoFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Categorías de productos. Lo usan dos páginas:
 *  - Categorias.xhtml: listado (sp_lista_categoriasAct), búsqueda por nombre, editar y eliminar.
 *  - RegistrarCategoria.xhtml: formulario para grabar y actualizar (sp_Mante_Categorias).
 *  - Nuevo: limpia el formulario y autogenera el código (sp_max_cod_categoria).
 *  - Eliminar (botón de cada fila): sp_Elimina_Categorias.
 *  - Imagen (Categoria.rutaimagen, varchar(150)): igual que en Productos, se sube un archivo (se guarda en
 *    la carpeta de AlmacenImagenesProducto como CAT{id}_{version}.{ext} y se graba su nombre) o se pega
 *    una URL externa (se graba la URL). Las subidas se muestran con ImagenProductoServlet.
 */
@Named("categoriaBean")
@SessionScoped
public class CategoriaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ESTADO_ACTIVO = "Activo";
    private static final String CRITERIO_NOMBRE = "NOMBRE";
    private static final String CRITERIO_CODIGO = "CODIGO";
    private static final int MAX_NOMBRE = 50;   // @Nombre varchar(50) en sp_Mante_Categorias

    private CategoriaDTO categoria;
    private boolean edicion;

    /** Todas las categorías devueltas por la BD y las que se muestran tras filtrar. */
    private List<CategoriaDTO> listaCompleta = new ArrayList<>();
    private List<CategoriaDTO> lista = new ArrayList<>();
    private String textoBusqueda;
    /** Condición del filtro (chips de la plantilla): NOMBRE o CODIGO. */
    private String criterioBusqueda = CRITERIO_NOMBRE;

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
        limpiarImagen();
        return null;
    }

    public String grabar() {
        if (!validar()) return null;
        categoria.setNombre(categoria.getNombre().trim());
        if (!edicion) {
            categoria.setCategoria_ID(siguienteId());
            categoria.setEstado(ESTADO_ACTIVO);
        }
        // La imagen se resuelve con el código ya definitivo (el archivo se nombra con él)
        String subida = resolverImagen();
        if (subida == null) return null;
        boolean ok = new CatalogoFACADE().Mante_Categorias(categoria);
        if (ok) {
            imagenGrabada();
            info((edicion ? "Se actualizó la categoría " : "Se registró la categoría ") + categoria.getNombre() + ".");
            nuevo();
            return "/Categorias.xhtml?faces-redirect=true";
        }
        AlmacenImagenesProducto.borrar(subida);   // no se grabó: no queda el archivo suelto
        error("No se pudo guardar la categoría.");
        return null;
    }

    /** Botón Editar de Categorias.xhtml: pasa la fila al formulario y abre RegistrarCategoria.xhtml. */
    public String editar(CategoriaDTO fila) {
        CategoriaDTO c = new CategoriaDTO();
        c.setCategoria_ID(fila.getCategoria_ID());
        c.setNombre(fila.getNombre() == null ? "" : fila.getNombre().trim());
        c.setEstado(fila.getEstado());
        // Si el SP del listado no trae rutaimagen, se lee de la tabla para no perder la imagen al actualizar
        c.setrutaimagen(fila.getrutaimagen() != null ? fila.getrutaimagen()
                : new CatalogoFACADE().RutaImagenCategoria(fila.getCategoria_ID()));
        limpiarImagen();
        categoria = c;
        edicion = true;
        return "/RegistrarCategoria.xhtml?faces-redirect=true&modo=editar";
    }

    public String eliminar(CategoriaDTO fila) {
        if (new CatalogoFACADE().Elimina_Categorias(fila)) {
            info("Se eliminó la categoría " + (fila.getNombre() == null ? "" : fila.getNombre().trim())
                    + ". Si tenía productos asociados, quedó Inactiva.");
            if (edicion && categoria.getCategoria_ID() == fila.getCategoria_ID()) nuevo();
            consultar();
        } else {
            error("No se pudo eliminar la categoría.");
        }
        return null;
    }

    /** Buscar: filtra por nombre o código el listado ya consultado a la BD. */
    public String buscar() {
        consultar();
        return null;
    }

    /** Listar: borra el filtro y muestra todas las categorías. */
    public String listar() {
        textoBusqueda = null;
        criterioBusqueda = CRITERIO_NOMBRE;
        consultar();
        return null;
    }

    // ------------------------------------------------------------------ internos

    private void consultar() {
        List<CategoriaDTO> datos = new CatalogoFACADE().ListaCategorias();
        listaCompleta = datos == null ? new ArrayList<>() : datos;
        String t = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase();
        boolean porCodigo = CRITERIO_CODIGO.equals(criterioBusqueda);
        lista = new ArrayList<>();
        for (CategoriaDTO c : listaCompleta) {
            if (t.isEmpty() || coincide(c, t, porCodigo)) lista.add(c);
        }
    }

    /** Código: el número debe empezar con lo escrito (12 encuentra 12, 120...). Nombre: contiene el texto. */
    private static boolean coincide(CategoriaDTO c, String t, boolean porCodigo) {
        if (porCodigo) return String.valueOf(c.getCategoria_ID()).startsWith(t);
        String n = c.getNombre() == null ? "" : c.getNombre().trim().toLowerCase();
        return n.contains(t);
    }

    private int siguienteId() {
        CategoriaDTO c = new CatalogoFACADE().autogeneraCategoria();
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
        for (CategoriaDTO c : new CatalogoFACADE().ListaCategorias()) {
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

    // ------------------------------------------------------------------ imagen de la categoría

    /** Archivo elegido en "Subir imagen" (solo vive durante la petición de Grabar). */
    private transient Part archivoImagen;
    /** URL externa pegada en el formulario. */
    private String urlImagen = "";
    /** "Quitar imagen": la categoría queda sin imagen. */
    private boolean quitarImagen;

    private void limpiarImagen() {
        archivoImagen = null;
        urlImagen = "";
        quitarImagen = false;
    }

    /** Prefijo de los archivos de esta categoría en la carpeta de imágenes (no choca con los productos). */
    private String prefijoArchivo() {
        return "CAT" + categoria.getCategoria_ID();
    }

    /**
     * Deja en categoria.rutaimagen lo que se va a grabar, en este orden:
     *  1. archivo subido, 2. URL externa, 3. "Quitar imagen" (vacío), 4. si no se tocó nada, la que tenía.
     * Devuelve el nombre del archivo recién subido ("" si no se subió) o null si hubo un error.
     */
    private String resolverImagen() {
        Part archivo = archivoImagen;
        archivoImagen = null;
        try {
            if (archivo != null && archivo.getSize() > 0) {
                String nombre = AlmacenImagenesProducto.guardar(archivo, prefijoArchivo());
                categoria.setrutaimagen(nombre);
                urlImagen = "";
                return nombre;
            }
            if (urlImagen != null && !urlImagen.isBlank()) {
                categoria.setrutaimagen(AlmacenImagenesProducto.validarUrl(urlImagen));
            } else if (quitarImagen) {
                categoria.setrutaimagen("");
            }
            return "";
        } catch (AlmacenImagenesProducto.ImagenInvalidaException ex) {
            error(ex.getMessage());
        } catch (IOException ex) {
            error("No se pudo guardar la imagen en el servidor: " + ex.getMessage());
        }
        return null;
    }

    /** Categoría grabada: se borran sus imágenes subidas anteriores (solo queda la actual, si es un archivo). */
    private void imagenGrabada() {
        String imagen = categoria.getrutaimagen() == null ? "" : categoria.getrutaimagen().trim();
        AlmacenImagenesProducto.borrarAnteriores(prefijoArchivo(),
                AlmacenImagenesProducto.esNombreArchivo(imagen) ? imagen : "");
    }

    /** Dirección para la vista previa de la imagen guardada ("" si no tiene). */
    public String getUrlImagenActual() {
        return AlmacenImagenesProducto.url(
                FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath(),
                categoria.getrutaimagen());
    }

    public Part getArchivoImagen() { return archivoImagen; }
    public void setArchivoImagen(Part archivoImagen) { this.archivoImagen = archivoImagen; }
    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen == null ? "" : urlImagen.trim(); }
    public boolean isQuitarImagen() { return quitarImagen; }
    public void setQuitarImagen(boolean quitarImagen) { this.quitarImagen = quitarImagen; }

    // ------------------------------------------------------------------ getters / setters

    public CategoriaDTO getCategoria() { return categoria; }
    public boolean isEdicion() { return edicion; }
    public List<CategoriaDTO> getLista() { return lista; }
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
        for (CategoriaDTO c : listaCompleta) {
            if (c.getEstado() == null || c.getEstado().trim().equalsIgnoreCase(ESTADO_ACTIVO)) n++;
        }
        return n;
    }

    public String codigo(CategoriaDTO c) {
        return String.valueOf(c.getCategoria_ID());   // código numérico, tal como está en la BD
    }
}