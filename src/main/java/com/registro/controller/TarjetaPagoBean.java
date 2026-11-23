package com.registro.controller;

import com.registro.dto.ConfiguracionesDTO;
import com.registro.facade.ConfiguracionesFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mantenimiento de Tarjetas de Pago (ManteTarjetasPago.xhtml).
 * Misma lógica que frm_ManteTarjetasPago de SisdataGimnasio:
 *  - Nuevo [F2]: limpia y genera el código con GeneraCodigoTarjetaF (campo de solo lectura).
 *  - Seleccionar una fila (doble clic en el WinForm): carga los datos con el código bloqueado.
 *  - Modificar [F4]: habilita la edición de la tarjeta seleccionada (sin cambiar el código).
 *  - Grabar [F3]: si el código está habilitado inserta (valida código en blanco y repetido); si no, actualiza.
 *  - Eliminar [F6]: valida que exista y, además, que su código (IDTarjeta) no tenga registros en otra tabla.
 */
@Named("tarjetaPagoBean")
@ViewScoped
public class TarjetaPagoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Estados de la pantalla (Desacer / Desacer2 del WinForm). */
    public static final String MODO_CONSULTA = "consulta";
    public static final String MODO_NUEVO = "nuevo";
    public static final String MODO_MODIFICAR = "modificar";

    private static final int LARGO_CODIGO = 3;          // @IDTarjeta varchar(3)
    private static final int LARGO_DESCRIPCION = 100;   // @NombreTarjetaPago varchar(100)

    private transient ConfiguracionesFACADE facade;

    private List<ConfiguracionesDTO> lista = new ArrayList<>();
    private String modo = MODO_CONSULTA;

    // Campos del formulario (txtcodigo, txtdescripcion, numIncremento, cboestado)
    private String codigo;
    private String descripcion;
    private Double porCom = 0d;
    private String estado = "ACTIVO";
    private String idFormaPago;        // no se muestra (igual que el WinForm); se conserva al modificar
    private String seleccionado;       // código de la fila elegida en la grilla

    private ConfiguracionesFACADE facade() {
        if (facade == null) facade = new ConfiguracionesFACADE();
        return facade;
    }

    @PostConstruct
    public void init() {
        listar();
    }

    // ------------------------------------------------------------------
    // Listar (frm_ManteBancos_Load -> Listar)
    // ------------------------------------------------------------------

    private void listar() {
        try {
            lista = facade().listaTarjetasPagoF();
        } catch (RuntimeException ex) {
            lista = new ArrayList<>();
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Botones
    // ------------------------------------------------------------------

    /** Nuevo [F2]: limpia y genera el código automáticamente (GeneraCodigoTarjetaF); el código es de solo lectura. */
    public String nuevo() {
        limpiar();
        try {
            codigo = generarCodigo();
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return null;
        }
        if (codigo.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo generar el código de la tarjeta.");
            return null;
        }
        modo = MODO_NUEVO;
        return null;
    }

    /** Código siguiente de sp_GeneraCodigoTarjeta ("" si no devolvió nada). */
    private String generarCodigo() {
        ConfiguracionesDTO g = facade().GeneraCodigoTarjetaF();
        return g == null ? "" : limpio(g.getIDTarjeta());
    }

    /** Clic en una fila de la grilla (ListaTextos): carga los datos con el código bloqueado. */
    public String seleccionar(String id) {
        ConfiguracionesDTO t = buscar(id);
        if (t == null) {
            mensaje(FacesMessage.SEVERITY_WARN, "La tarjeta seleccionada ya no existe. Se actualizó la lista.");
            listar();
            return null;
        }
        cargar(t);
        modo = MODO_CONSULTA;
        return null;
    }

    /** Modificar [F4]: habilita la edición de la tarjeta seleccionada. */
    public String modificar() {
        if (vacio(seleccionado)) {
            mensaje(FacesMessage.SEVERITY_WARN, "Seleccione en la lista la tarjeta que desea modificar.");
            return null;
        }
        modo = MODO_MODIFICAR;
        return null;
    }

    /** Grabar [F3]: con el código habilitado graba uno nuevo; si no, actualiza (btngrabar_Click). */
    public String grabar() {
        if (MODO_NUEVO.equals(modo)) graba();
        else if (MODO_MODIFICAR.equals(modo)) actualiza();
        else mensaje(FacesMessage.SEVERITY_WARN, "Presione Nuevo para registrar o Modificar para editar una tarjeta.");
        return null;
    }

    /** Cancela la edición y vuelve al estado inicial. */
    public String cancelar() {
        limpiar();
        modo = MODO_CONSULTA;
        return null;
    }

    private void graba() {
        String cod = limpio(codigo);
        if (cod.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se puede dejar código en blanco, verifique.");
            return;
        }
        if (!cod.matches("[0-9]+")) {   // validarNumerosEnteros
            mensaje(FacesMessage.SEVERITY_ERROR, "El código solo debe tener números.");
            return;
        }
        if (cod.length() > LARGO_CODIGO) {
            mensaje(FacesMessage.SEVERITY_ERROR, "El código debe tener como máximo " + LARGO_CODIGO + " dígitos.");
            return;
        }
        if (!validarDatos()) return;
        try {
            if (!facade().ListaTarjetasPagoXCodF(cod).isEmpty()) {
                // Otro usuario grabó ese código mientras tanto: se genera el siguiente
                codigo = generarCodigo();
                mensaje(FacesMessage.SEVERITY_WARN, "El código " + cod + " ya fue registrado. Se generó el código "
                        + codigo + "; presione Grabar nuevamente.");
                return;
            }
            if (!asegurarFormaPago()) return;
            if (!facade().InsertTarjetasPagoF(armar(cod))) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo grabar la tarjeta de pago.");
                return;
            }
            mensaje(FacesMessage.SEVERITY_INFO, "Se grabaron los datos correctamente.");
            terminar(cod);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    private void actualiza() {
        String cod = limpio(seleccionado);
        if (cod.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Código en blanco, no se puede hacer los cambios, verifique.");
            return;
        }
        if (!validarDatos()) return;
        try {
            if (!asegurarFormaPago()) return;
            if (!facade().UpdateTarjetasPagoF(armar(cod))) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudieron hacer los cambios.");
                return;
            }
            mensaje(FacesMessage.SEVERITY_INFO, "Se hicieron los cambios correctamente.");
            terminar(cod);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /**
     * Eliminar [F6]. Además de la validación del WinForm (que el código exista), no deja eliminar si el
     * código de la tarjeta (IDTarjeta) tiene registros en otra tabla con ese mismo campo.
     */
    public String eliminar() {
        String cod = limpio(seleccionado);
        if (cod.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Seleccione en la lista la tarjeta que desea eliminar.");
            return null;
        }
        try {
            if (facade().ListaTarjetasPagoXCodF(cod).isEmpty()) {
                mensaje(FacesMessage.SEVERITY_ERROR, "Código no existe, no se puede eliminar los datos, verifique.");
                listar();
                limpiar();
                return null;
            }
            List<String> usadas = facade().TablasQueUsanTarjetaF(cod);
            if (!usadas.isEmpty()) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se puede eliminar la tarjeta " + cod
                        + " porque tiene registros en: " + String.join(", ", usadas)
                        + ". Puede dejarla en estado INACTIVO.");
                return null;
            }
            if (!facade().DeleteTarjetasPagoF(armarCodigo(cod))) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo eliminar la tarjeta de pago.");
                return null;
            }
            mensaje(FacesMessage.SEVERITY_INFO, "Se eliminaron los datos correctamente.");
            listar();
            limpiar();
            modo = MODO_CONSULTA;
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private boolean validarDatos() {
        String desc = limpio(descripcion);
        if (desc.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Ingrese la descripción de la tarjeta.");
            return false;
        }
        if (desc.length() > LARGO_DESCRIPCION) {
            mensaje(FacesMessage.SEVERITY_ERROR, "La descripción debe tener como máximo " + LARGO_DESCRIPCION + " caracteres.");
            return false;
        }
        double pc = porCom == null ? 0 : porCom;
        if (pc < 0 || pc > 100) {   // NumericUpDown: 0 a 100, 2 decimales
            mensaje(FacesMessage.SEVERITY_ERROR, "El porcentaje debe estar entre 0 y 100.");
            return false;
        }
        if (!"ACTIVO".equals(estado) && !"INACTIVO".equals(estado)) estado = "ACTIVO";
        return true;
    }

    /**
     * TarjetasPago.IDFormaPago no admite NULL: si la tarjeta no lo tiene (nueva, o antigua sin dato),
     * se usa el ID de la forma de pago "Tarjeta". Al modificar se conserva el que ya tenía.
     */
    private boolean asegurarFormaPago() {
        if (!vacio(idFormaPago)) return true;
        idFormaPago = facade().IDFormaPagoTarjetaF();
        if (vacio(idFormaPago)) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se encontró la forma de pago \"Tarjeta\" en la lista de formas de pago "
                    + "(sp_ListaFormasPago). Regístrela antes de grabar tarjetas.");
            return false;
        }
        return true;
    }

    private ConfiguracionesDTO armar(String cod) {
        ConfiguracionesDTO t = new ConfiguracionesDTO();
        t.setIDTarjeta(cod);
        t.setNombreTarjetaPago(limpio(descripcion));
        t.setEstado(estado);
        t.setPorCom(Math.round((porCom == null ? 0 : porCom) * 100.0) / 100.0);
        t.setIDFormaPago(idFormaPago);
        return t;
    }

    private static ConfiguracionesDTO armarCodigo(String cod) {
        ConfiguracionesDTO t = new ConfiguracionesDTO();
        t.setIDTarjeta(cod);
        return t;
    }

    /** Tras grabar: recarga la lista y deja seleccionada la tarjeta grabada. */
    private void terminar(String cod) {
        listar();
        ConfiguracionesDTO t = buscar(cod);
        if (t != null) cargar(t); else limpiar();
        modo = MODO_CONSULTA;
    }

    private ConfiguracionesDTO buscar(String id) {
        String cod = limpio(id);
        for (ConfiguracionesDTO t : lista) {
            if (cod.equals(limpio(t.getIDTarjeta()))) return t;
        }
        return null;
    }

    private void cargar(ConfiguracionesDTO t) {
        seleccionado = limpio(t.getIDTarjeta());
        codigo = seleccionado;
        descripcion = t.getNombreTarjetaPago();
        porCom = t.getPorCom();
        estado = vacio(t.getEstado()) ? "ACTIVO" : t.getEstado().trim().toUpperCase(Locale.ROOT);
        idFormaPago = t.getIDFormaPago();
    }

    private void limpiar() {
        seleccionado = null;
        codigo = null;
        descripcion = null;
        porCom = 0d;
        estado = "ACTIVO";
        idFormaPago = null;
    }

    private static boolean vacio(String s) { return s == null || s.trim().isEmpty(); }
    private static String limpio(String s) { return s == null ? "" : s.trim(); }

    private static void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    // ------------------------------------------------------------------
    // Getters / setters para la vista
    // ------------------------------------------------------------------

    public List<ConfiguracionesDTO> getLista() { return lista; }
    public int getTotal() { return lista.size(); }
    public String getModo() { return modo; }
    public boolean isEditando() { return !MODO_CONSULTA.equals(modo); }
    public boolean isCodigoHabilitado() { return MODO_NUEVO.equals(modo); }
    public boolean isHaySeleccion() { return !vacio(seleccionado); }
    public String getSeleccionado() { return seleccionado; }

    public boolean esSeleccionada(ConfiguracionesDTO t) {
        return t != null && !vacio(seleccionado) && seleccionado.equals(limpio(t.getIDTarjeta()));
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getPorCom() { return porCom; }
    public void setPorCom(Double porCom) { this.porCom = porCom; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
