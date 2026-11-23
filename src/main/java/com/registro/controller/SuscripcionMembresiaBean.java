package com.registro.controller;

import com.registro.dto.ClienteDTO;
import com.registro.dto.MembresiaDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.ClienteFACADE;
import com.registro.facade.MembresiaFACADE;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.registro.controller.MembresiaBean.codEmpSesion;
import static com.registro.controller.MembresiaBean.mensaje;
import static com.registro.controller.MembresiaBean.usuarioEnSesion;
import static com.registro.controller.MembresiaBean.vacio;

/**
 * Suscripción de membresía: misma funcionalidad que frm_RegistroMembresia de SisdataGimnasio.
 * Nuevo [F2] · Registrar [F3] · Comprobante [F4] · Ver membresías [F5] · Buscar socio [F6] ·
 * Nuevo socio [F7] · Imprimir [Pausa] · Renovar · Eliminar (anular) · Salir [Esc].
 */
@Named("suscripcionMembresiaBean")
@SessionScoped
public class SuscripcionMembresiaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private transient MembresiaFACADE facade;
    private transient ClienteFACADE clienteFacade;

    private MembresiaFACADE getFacade() {
        if (facade == null) facade = new MembresiaFACADE();
        return facade;
    }

    private ClienteFACADE getClienteFacade() {
        if (clienteFacade == null) clienteFacade = new ClienteFACADE();
        return clienteFacade;
    }

    // Datos del formulario
    private MembresiaDTO suscripcion = nuevaSuscripcionVacia();
    private String duracionTexto = "";
    private String codigoSocio = "";
    private String socioNombre = "";
    private String socioDireccion = "";
    private String socioTelefono = "";
    private boolean aplicarDescuento;
    private List<MembresiaDTO> cronograma = new ArrayList<>();

    // Habilitación de controles y botones (como Enabled en el formulario)
    private boolean controlesActivos;
    private boolean nuevoHabilitado = true;
    private boolean registrarHabilitado;
    private boolean eliminarHabilitado;
    private boolean comprobanteHabilitado;
    private boolean membresiasHabilitado;
    private boolean renovarHabilitado;
    private boolean registrarClienteHabilitado;

    // Selector de planes [F5] (dataGridView1)
    private List<MembresiaDTO> planes = new ArrayList<>();
    private boolean mostrarPlanes;

    // Selector de socios [F6] (frm_Consulta_Clientes); también lo usa Renovar
    private boolean mostrarClientes;
    private boolean selectorParaRenovar;
    private String filtroCliente = "";
    private List<ClienteDTO> clientesEncontrados = new ArrayList<>();

    // Renovar: confirmación cuando el socio tiene una membresía vigente
    private String confirmacionRenovar;
    private ClienteDTO renovarCliente;
    private MembresiaDTO renovarUltima;
    private LocalDate renovarInicio;

    // Imprimir [Pausa]
    private boolean imprimir;

    // ---------------------------------------------------------
    // Carga (frm_RegistroClases_Load); ?nro= abre una suscripción registrada (CondForm = 1)
    // ---------------------------------------------------------

    public void inicializar(String nro) {
        // De regreso de "Nuevo socio [F7]": se conserva lo que había y se carga el socio registrado
        if (volviendoDeNuevoSocio) {
            volviendoDeNuevoSocio = false;
            if (!vacio(socioNuevoId)) {
                ClienteDTO c = getClienteFacade().ListaClienteXCod(socioNuevoId);
                if (c != null) {
                    llenarSocio(c);
                    mensaje(FacesMessage.SEVERITY_INFO, "Socio " + socioNuevoId + " registrado. Revise los datos y presione Registrar [F3].");
                }
            }
            socioNuevoId = null;
            return;
        }
        reiniciarFormulario();
        if (!vacio(nro)) cargarSuscripcion(nro.trim());
    }

    // ---------------------------------------------------------
    // Nuevo socio [F7] (frm_ManteClientes2): abre el registro de socios y regresa con el socio cargado
    // ---------------------------------------------------------

    private boolean volviendoDeNuevoSocio;
    private String socioNuevoId;

    public String irNuevoSocio() {
        if (!registrarClienteHabilitado) return null;
        volviendoDeNuevoSocio = true;
        socioNuevoId = null;
        mostrarPlanes = false;
        cerrarSelectorClientes();
        return "/socios.xhtml?faces-redirect=true&origen=suscripcion";
    }

    /** Lo llama ClienteBean al grabar el socio abierto desde aquí. */
    public void recibirSocioNuevo(String clienteId) {
        volviendoDeNuevoSocio = true;
        socioNuevoId = clienteId;
    }

    private void reiniciarFormulario() {
        limpiar();
        controlesActivos = false;
        nuevoHabilitado = true;
        registrarHabilitado = false;
        eliminarHabilitado = false;
        comprobanteHabilitado = false;
        membresiasHabilitado = false;
        renovarHabilitado = false;
        registrarClienteHabilitado = false;
        mostrarPlanes = false;
        cerrarSelectorClientes();
        cancelarRenovar();
        imprimir = false;
    }

    /** Suscripción ya registrada: se muestra con su cronograma; se puede anular, facturar o imprimir. */
    private void cargarSuscripcion(String nro) {
        MembresiaDTO reg = getFacade().Listasuscripciones_MembresiaXRegistroF(codEmpSesion(), nro);
        if (reg == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "El registro de suscripción no existe.");
            return;
        }
        reg.setCodEmp(codEmpSesion());
        MembresiaDTO plan = vacio(reg.getCodigoM()) ? null : getFacade().ListaMembresiaXCodF(reg.getCodigoM().trim());
        if (plan != null) {
            reg.setNombreMembresia(plan.getNombreMembresia());
            reg.setFrecuencia(plan.getFrecuencia());
            reg.setDiasLimite(plan.getDiasLimite());
            reg.setTarifa(plan.getTarifa());
            duracionTexto = textoDuracion(plan);
        }
        if (vacio(reg.getEstadoSusc())) reg.setEstadoSusc("Pendiente");
        suscripcion = reg;
        aplicarDescuento = reg.getDescuento() > 0;
        if (!vacio(reg.getcliente_id())) {
            ClienteDTO c = getClienteFacade().ListaClienteXCod(reg.getcliente_id().trim());
            if (c != null) llenarSocio(c);
            else codigoSocio = reg.getcliente_id().trim();
        }
        listarCronograma();
        eliminarHabilitado = true;
        comprobanteHabilitado = true;
    }

    // ---------------------------------------------------------
    // Nuevo [F2]
    // ---------------------------------------------------------

    public String nuevo() {
        if (!nuevoHabilitado) return null;
        try {
            iniciarNuevo();
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    private void iniciarNuevo() {
        limpiar();
        activaControles();
        nuevoHabilitado = false;
        membresiasHabilitado = true;
        renovarHabilitado = true;
        registrarClienteHabilitado = true;
        MembresiaDTO generado = getFacade().generasuscripciones_MembresiaF(codEmpSesion());
        suscripcion.setNroSuscripcion_id(generado != null ? generado.getNroSuscripcion_id() : "");
    }

    private void limpiar() {
        suscripcion = nuevaSuscripcionVacia();
        duracionTexto = "";
        aplicarDescuento = false;
        cronograma = new ArrayList<>();
        codigoSocio = "";
        socioNombre = "";
        socioDireccion = "";
        socioTelefono = "";
    }

    private static MembresiaDTO nuevaSuscripcionVacia() {
        MembresiaDTO s = new MembresiaDTO();
        s.setNroSuscripcion_id("");
        s.setCodigoM("");
        s.setNombreMembresia("");
        s.setFrecuencia("");
        s.setObservacionSusc("");
        s.setCondicionSusc("ACTIVA");
        s.setEstadoSusc("Pendiente");   // lblEstado
        s.setFechaInicio(Date.valueOf(LocalDate.now()));
        s.setFechaFin(Date.valueOf(LocalDate.now()));
        return s;
    }

    private void activaControles() {
        controlesActivos = true;
        registrarHabilitado = true;
    }

    // ---------------------------------------------------------
    // Ver membresías [F5]
    // ---------------------------------------------------------

    public String verMembresias() {
        if (!membresiasHabilitado) return null;
        mostrarPlanes = !mostrarPlanes;
        if (mostrarPlanes) {
            try {
                ArrayList<MembresiaDTO> lista = getFacade().ListaMembresiaF();
                planes = lista != null ? lista : new ArrayList<>();
            } catch (RuntimeException ex) {
                mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            }
        }
        return null;
    }

    public String cerrarPlanes() {
        mostrarPlanes = false;
        return null;
    }

    /** Doble clic / Enter en un plan (MostrarInformacionMembresia). */
    public String seleccionarPlan(String codigoM, String nombre) {
        if (cargarMembresia(codigoM, nombre)) mostrarPlanes = false;
        return null;
    }

    /** CargarMembresia: datos del plan, tarifa = subtotal = total y fecha de vencimiento. */
    private boolean cargarMembresia(String codigo, String nombre) {
        try {
            MembresiaDTO plan = getFacade().ListaMembresiaXCodF(codigo == null ? "" : codigo.trim());
            if (plan == null) {
                suscripcion.setDiasLimite(0);
                suscripcion.setTarifa(0);
                mensaje(FacesMessage.SEVERITY_WARN, "No existe la membresia " + codigo);
                return false;
            }
            MembresiaDTO s = suscripcion;
            s.setCodigoM(codigo.trim());
            s.setNombreMembresia(!vacio(nombre) ? nombre.trim() : plan.getNombreMembresia());
            s.setDiasLimite(plan.getDiasLimite());
            duracionTexto = textoDuracion(plan);
            s.setFrecuencia(plan.getFrecuencia());
            s.setTarifa(plan.getTarifa());
            s.setSubTotal(plan.getTarifa());
            s.setTotalSusc(plan.getTarifa());
            if (aplicarDescuento) recalcularTotal();

            // Vencimiento: la primera unidad de duración distinta de cero (días, semanas, meses o años)
            LocalDate inicio = s.getFechaInicio() != null ? s.getFechaInicio().toLocalDate() : LocalDate.now();
            LocalDate fin = LocalDate.now();
            if (plan.getDuracionDias() > 0) fin = inicio.plusDays(plan.getDuracionDias());
            else if (plan.getDuracionSemanas() > 0) fin = inicio.plusDays(plan.getDuracionSemanas() * 7L);
            else if (plan.getDuracionMeses() > 0) fin = inicio.plusMonths(plan.getDuracionMeses());
            else if (plan.getDuracionAnios() > 0) fin = inicio.plusYears(plan.getDuracionAnios());
            s.setFechaFin(Date.valueOf(fin));
            return true;
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return false;
        }
    }

    private static String textoDuracion(MembresiaDTO p) {
        return p.getDuracionDias() + " días, " + p.getDuracionSemanas() + " semanas, "
                + p.getDuracionMeses() + " meses, " + p.getDuracionAnios() + " años";
    }

    // ---------------------------------------------------------
    // Socio: Enter en el código, Buscar [F6], Nuevo socio [F7]
    // ---------------------------------------------------------

    /** Enter en el código del socio (txtcodSocio_KeyPress): busca por código. */
    public String buscarPorCodigo() {
        if (!controlesActivos) return null;
        try {
            String cod = codigoSocio == null ? "" : codigoSocio.trim();
            ClienteDTO c = cod.isEmpty() ? null : getClienteFacade().ListaClienteXCod(cod);
            if (c != null) llenarSocio(c);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    /** Buscar [F6]: abre la consulta de socios. */
    public String buscarSocio() {
        if (!controlesActivos) return null;
        abrirSelectorClientes(false);
        return null;
    }

    private void abrirSelectorClientes(boolean paraRenovar) {
        selectorParaRenovar = paraRenovar;
        mostrarClientes = true;
        mostrarPlanes = false;
        filtroCliente = "";
        sociosSelector = null;       // al abrir el selector se consulta la lista una vez
        filtrarClientes();
    }

    /** Socios activos del selector (se consultan al abrir el selector; filtrar no vuelve a consultar). */
    private transient List<ClienteDTO> sociosSelector;

    /** Filtro de la consulta de socios: por nombre, código o DNI/RUC. */
    public String filtrarClientes() {
        try {
            if (sociosSelector == null) sociosSelector = getClienteFacade().ObtenerListadoCliente();
            List<ClienteDTO> todos = sociosSelector;
            String f = filtroCliente == null ? "" : filtroCliente.trim().toLowerCase(Locale.ROOT);
            List<ClienteDTO> lista = new ArrayList<>();
            if (todos != null) {
                for (ClienteDTO c : todos) {
                    if (f.isEmpty() || contiene(c.getRazonSocial(), f) || contiene(c.getCliente_ID(), f) || contiene(c.getRucDNI(), f)) {
                        lista.add(c);
                    }
                }
            }
            clientesEncontrados = lista;
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    public String cerrarSelectorClientes() {
        mostrarClientes = false;
        selectorParaRenovar = false;
        clientesEncontrados = new ArrayList<>();
        return null;
    }

    /** Socio elegido en la consulta (ElegirCliente). */
    public String seleccionarCliente(String clienteId) {
        ClienteDTO c = vacio(clienteId) ? null : getClienteFacade().ListaClienteXCod(clienteId.trim());
        boolean paraRenovar = selectorParaRenovar;
        cerrarSelectorClientes();
        if (c == null) return null;
        if (paraRenovar) {
            renovar(c);
        } else {
            llenarSocio(c);
        }
        return null;
    }

    private void llenarSocio(ClienteDTO c) {
        String id = c.getCliente_ID() == null ? "" : c.getCliente_ID().trim();
        suscripcion.setcliente_id(id);
        codigoSocio = id;
        socioNombre = c.getRazonSocial();
        socioDireccion = c.getDireccion();
        socioTelefono = c.getTelefono();
    }

    // ---------------------------------------------------------
    // Renovar membresía
    // ---------------------------------------------------------

    /** Renovar: primero se elige el socio. */
    public String renovarMembresia() {
        if (!renovarHabilitado) return null;
        abrirSelectorClientes(true);
        return null;
    }

    private void renovar(ClienteDTO c) {
        try {
            String codcli = c.getCliente_ID() == null ? "" : c.getCliente_ID().trim();
            MembresiaDTO ultima = getFacade().UltimaMembresiaTerminadaF(codEmpSesion(), codcli);
            MembresiaDTO vigente = getFacade().MembresiaVigenteClienteF(codEmpSesion(), codcli);

            LocalDate inicio = LocalDate.now();
            if (vigente != null) {
                LocalDate finVigente = vigente.getFechaFin().toLocalDate();
                // Se pide confirmación (Sí / No) antes de seguir
                renovarCliente = c;
                renovarUltima = ultima != null ? ultima : vigente;
                renovarInicio = finVigente.plusDays(1);
                confirmacionRenovar = (c.getRazonSocial() == null ? "" : c.getRazonSocial().trim())
                        + " tiene una membresia vigente (" + (vigente.getMembresia() == null ? "" : vigente.getMembresia())
                        + ") hasta el " + finVigente.format(DD_MM_YYYY) + ".\n\nDesea renovarla de todas maneras? La nueva empezara el "
                        + renovarInicio.format(DD_MM_YYYY) + ".";
                return;
            }
            completarRenovacion(c, ultima, inicio);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /** "Sí" en la confirmación de renovación. */
    public String confirmarRenovar() {
        if (renovarCliente != null) {
            try {
                completarRenovacion(renovarCliente, renovarUltima, renovarInicio);
            } catch (RuntimeException ex) {
                mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            }
        }
        cancelarRenovar();
        return null;
    }

    /** "No" en la confirmación de renovación. */
    public String cancelarRenovar() {
        confirmacionRenovar = null;
        renovarCliente = null;
        renovarUltima = null;
        renovarInicio = null;
        return null;
    }

    private void completarRenovacion(ClienteDTO c, MembresiaDTO ultima, LocalDate inicio) {
        iniciarNuevo();
        llenarSocio(c);
        suscripcion.setFechaInicio(Date.valueOf(inicio));
        String nombre = c.getRazonSocial() == null ? "" : c.getRazonSocial().trim();
        if (ultima == null) {
            mensaje(FacesMessage.SEVERITY_INFO, nombre + " no tiene membresias terminadas para renovar. Elija la membresia [F5].");
            return;
        }
        if (cargarMembresia(ultima.getCodigoM(), ultima.getMembresia())) {
            suscripcion.setObservacionSusc("Renovacion de la membresia N° "
                    + (ultima.getRegistro() == null ? "" : ultima.getRegistro().trim())
                    + " (vencio el " + ultima.getFechaFin().toLocalDate().format(DD_MM_YYYY) + ")");
        }
    }

    // ---------------------------------------------------------
    // Descuento (casilla "Aplicar descuento" de la plantilla)
    // ---------------------------------------------------------

    public void alCambiarAplicarDescuento() {
        if (!aplicarDescuento) suscripcion.setDescuento(0);
        recalcularTotal();
    }

    public void alCambiarDescuento() {
        recalcularTotal();
    }

    private void recalcularTotal() {
        MembresiaDTO s = suscripcion;
        double descuento = aplicarDescuento ? s.getDescuento() : 0;
        if (descuento < 0) descuento = 0;
        if (descuento > s.getSubTotal()) descuento = s.getSubTotal();
        s.setDescuento(descuento);
        s.setTotalSusc(s.getSubTotal() - descuento);
    }

    // ---------------------------------------------------------
    // Registrar [F3]
    // ---------------------------------------------------------

    public String grabar() {
        if (!registrarHabilitado) return null;
        try {
            MembresiaDTO s = suscripcion;
            if (vacio(codigoSocio)) {
                mensaje(FacesMessage.SEVERITY_WARN, "Elija el socio [F6] o registrelo [F7]");
                return null;
            }
            if (vacio(s.getCodigoM())) {
                mensaje(FacesMessage.SEVERITY_WARN, "Elija la membresia [F5]");
                return null;
            }
            if (s.getFechaFin().before(s.getFechaInicio())) {
                mensaje(FacesMessage.SEVERITY_WARN, "La fecha de vencimiento no puede ser menor que la de inicio");
                return null;
            }

            String codEmp = codEmpSesion();
            if (getFacade().Listasuscripciones_MembresiaXRegistroF(codEmp, s.getNroSuscripcion_id().trim()) != null) {
                mensaje(FacesMessage.SEVERITY_WARN, "No puede Registar el Pago pq ya Se Registro Esta Membresia");
                registrarHabilitado = false;
                nuevoHabilitado = true;
                return null;
            }

            s.setCodEmp(codEmp);
            s.setNroSuscripcion_id(s.getNroSuscripcion_id().trim());
            s.setcliente_id(codigoSocio.trim());
            UsuarioDTO vendedor = usuarioEnSesion();
            s.setPersonal_ID(vendedor != null ? vendedor.getPersonal_ID() : 0);
            s.setDias(s.getDiasLimite());          // DiasClases = txtdias (días límite del plan)
            if (!aplicarDescuento) s.setDescuento(0);
            s.setTotalSusc(s.getSubTotal() - s.getDescuento());

            if (!getFacade().InsertSuscripciones_MembresiaF(s)) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo registrar la suscripción. Revise los datos y la conexión.");
                return null;
            }
            registrarHabilitado = false;
            nuevoHabilitado = true;
            eliminarHabilitado = true;
            comprobanteHabilitado = true;
            listarCronograma();
            mensaje(FacesMessage.SEVERITY_INFO, "Se Genero la Suscripcion de Membresia Correctamente, su asistencia va a ser de "
                    + s.getDiasLimite() + " dias por frecuencia " + (s.getFrecuencia() == null ? "" : s.getFrecuencia())
                    + " solo queda pendiente registrar el Pago");
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    private void listarCronograma() {
        cronograma = new ArrayList<>();
        if (vacio(suscripcion.getNroSuscripcion_id()) || vacio(codigoSocio)) return;
        try {
            cronograma = getFacade().ListaCronogramaSuscripcionF(suscripcion.getNroSuscripcion_id().trim(), codigoSocio.trim());
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_WARN, "No se pudo mostrar el cronograma. " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------
    // Eliminar (anular)
    // ---------------------------------------------------------

    public String eliminar() {
        if (!eliminarHabilitado) return null;
        String nro = suscripcion.getNroSuscripcion_id();
        if (vacio(nro)) {
            mensaje(FacesMessage.SEVERITY_WARN, "Seleccione una suscripción para anular.");
            return null;
        }
        try {
            String respuesta = getFacade().AnularSuscripcionF(codEmpSesion(), nro.trim());
            if ("OK".equals(respuesta)) {
                mensaje(FacesMessage.SEVERITY_INFO, "Suscripción anulada correctamente.");
            } else {
                mensaje(FacesMessage.SEVERITY_WARN, respuesta);
            }
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Ocurrió un error inesperado: " + ex.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------
    // Generar comprobante [F4] e Imprimir [Pausa]
    // ---------------------------------------------------------

    public String generarComprobante() {
        if (!comprobanteHabilitado) return null;
        String nro = suscripcion.getNroSuscripcion_id();
        if (vacio(nro) || vacio(codigoSocio)) {
            mensaje(FacesMessage.SEVERITY_WARN, "No hay una membresía registrada, no se puede generar el Comprobante de Venta");
            return null;
        }
        if (getFacade().Listasuscripciones_MembresiaXRegistroF(codEmpSesion(), nro.trim()) == null) {
            mensaje(FacesMessage.SEVERITY_WARN, "Primero registre la membresía [F3] para generar el Comprobante de Venta");
            return null;
        }
        // frm_Facturacion todavía no existe en la versión web
        mensaje(FacesMessage.SEVERITY_WARN, "La facturación (comprobante de venta) aún no está disponible en la versión web.");
        return null;
    }

    public String imprimir() {
        String nro = suscripcion.getNroSuscripcion_id();
        if (vacio(nro) || vacio(codigoSocio)) {
            mensaje(FacesMessage.SEVERITY_WARN, "No hay una membresía registrada para imprimir.");
            return null;
        }
        if (getFacade().Listasuscripciones_MembresiaXRegistroF(codEmpSesion(), nro.trim()) == null) {
            mensaje(FacesMessage.SEVERITY_WARN, "Primero registre la membresía [F3] para imprimir su cronograma.");
            return null;
        }
        listarCronograma();
        imprimir = true;
        return null;
    }

    /** Una sola vez: la página llama a window.print() y se apaga. */
    public boolean isImprimir() {
        boolean v = imprimir;
        imprimir = false;
        return v;
    }

    // ---------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------

    private static boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private static String fechaTxt(Date d) {
        return d == null ? "" : d.toLocalDate().toString();
    }

    public MembresiaDTO getSuscripcion() { return suscripcion; }
    public String getFechaInicioTxt() { return fechaTxt(suscripcion.getFechaInicio()); }
    public String getFechaFinTxt() { return fechaTxt(suscripcion.getFechaFin()); }
    public String getFechaInicioDMY() { return suscripcion.getFechaInicio() == null ? "" : suscripcion.getFechaInicio().toLocalDate().format(DD_MM_YYYY); }
    public String getFechaFinDMY() { return suscripcion.getFechaFin() == null ? "" : suscripcion.getFechaFin().toLocalDate().format(DD_MM_YYYY); }
    public String getDuracionTexto() { return duracionTexto; }

    public String getCodigoSocio() { return codigoSocio; }
    public void setCodigoSocio(String codigoSocio) { this.codigoSocio = codigoSocio; }
    public String getSocioNombre() { return socioNombre; }
    public String getSocioDireccion() { return socioDireccion; }
    public String getSocioTelefono() { return socioTelefono; }

    public boolean isAplicarDescuento() { return aplicarDescuento; }
    public void setAplicarDescuento(boolean aplicarDescuento) { this.aplicarDescuento = aplicarDescuento; }

    public List<MembresiaDTO> getCronograma() { return cronograma; }

    public boolean isControlesActivos() { return controlesActivos; }
    public boolean isNuevoHabilitado() { return nuevoHabilitado; }
    public boolean isRegistrarHabilitado() { return registrarHabilitado; }
    public boolean isEliminarHabilitado() { return eliminarHabilitado; }
    public boolean isComprobanteHabilitado() { return comprobanteHabilitado; }
    public boolean isMembresiasHabilitado() { return membresiasHabilitado; }
    public boolean isRenovarHabilitado() { return renovarHabilitado; }
    public boolean isRegistrarClienteHabilitado() { return registrarClienteHabilitado; }

    public List<MembresiaDTO> getPlanes() { return planes; }
    public boolean isMostrarPlanes() { return mostrarPlanes; }

    public boolean isMostrarClientes() { return mostrarClientes; }
    public boolean isSelectorParaRenovar() { return selectorParaRenovar; }
    public String getFiltroCliente() { return filtroCliente; }
    public void setFiltroCliente(String filtroCliente) { this.filtroCliente = filtroCliente; }
    public List<ClienteDTO> getClientesEncontrados() { return clientesEncontrados; }

    public String getConfirmacionRenovar() { return confirmacionRenovar; }

    public String getClaseEstado() {
        String e = suscripcion.getEstadoSusc();
        return e != null && e.trim().equalsIgnoreCase("Pendiente") ? "estado-pendiente" : "estado-activa";
    }
}
