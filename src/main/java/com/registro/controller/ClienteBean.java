package com.registro.controller;

import com.registro.config.AlmacenFotos;
import com.registro.config.ServicioConsultaDocumento;
import com.registro.dto.ClienteDTO;
import com.registro.facade.ClienteFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.Serializable;
import java.util.ArrayList;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

/** Acciones JSF para listar, registrar, editar y eliminar socios/clientes. */
@Named("clienteBean")
@SessionScoped
public class ClienteBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient ClienteFACADE facade = new ClienteFACADE();

    private ClienteFACADE getFacade() {
        if (facade == null) facade = new ClienteFACADE();
        return facade;
    }
    private ClienteDTO socio;
    private List<ClienteDTO> listaClientes;
    private String modo = "grabar";

    // Paginación del listado (la lista se consulta una vez y se muestra por partes)
    private static final int[] TAMANOS_PAGINA = {10, 25, 50, 100};
    private static final int PAGINAS_VISIBLES = 5;
    private int pagina = 1;
    private int tamPagina = 10;

    // Orden del listado: por nombre (Razón Social) o por código (Cliente_ID), ascendente o descendente
    private String orden = "nombre";
    private String dir = "asc";
    private List<ClienteDTO> listaOrdenada;
    private String claveOrden;

    @PostConstruct
    public void init() {
        prepararNuevo(false);
    }

    public List<ClienteDTO> getListaClientes() {
        if (listaClientes == null) {
            try {
                listaClientes = getFacade().ObtenerListadoCliente();
            } catch (RuntimeException ex) {
                listaClientes = Collections.emptyList();
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo cargar la lista de socios.");
            }
        }
        return listaClientes;
    }

    /** Lista completa ya ordenada según orden/dir (se reordena solo si cambian). */
    private List<ClienteDTO> getListaOrdenada() {
        List<ClienteDTO> base = getListaClientes();
        String clave = orden + "|" + dir + "|" + System.identityHashCode(base);
        if (listaOrdenada == null || !clave.equals(claveOrden)) {
            List<ClienteDTO> copia = new ArrayList<>(base);
            Comparator<ClienteDTO> cmp = "codigo".equals(orden) ? POR_CODIGO : porNombre();
            if ("desc".equals(dir)) cmp = cmp.reversed();
            copia.sort(cmp);
            listaOrdenada = copia;
            claveOrden = clave;
        }
        return listaOrdenada;
    }

    /** Código: numérico si ambos son dígitos (000010 > 000009), si no alfabético. */
    private static final Comparator<ClienteDTO> POR_CODIGO = (a, b) -> {
        String x = texto(a.getCliente_ID()), y = texto(b.getCliente_ID());
        if (x.matches("[0-9]+") && y.matches("[0-9]+")) {
            int c = new java.math.BigInteger(x).compareTo(new java.math.BigInteger(y));
            if (c != 0) return c;
        }
        return x.compareToIgnoreCase(y);
    };

    /** Nombre sin distinguir mayúsculas ni tildes (Álvarez junto a Alvarez). */
    private static Comparator<ClienteDTO> porNombre() {
        Collator col = Collator.getInstance(new Locale("es", "PE"));
        col.setStrength(Collator.PRIMARY);
        return (a, b) -> {
            int c = col.compare(texto(a.getRazonSocial()), texto(b.getRazonSocial()));
            return c != 0 ? c : texto(a.getCliente_ID()).compareTo(texto(b.getCliente_ID()));
        };
    }

    private static String texto(String v) { return v == null ? "" : v.trim(); }

    public String getOrden() { return orden; }
    public void setOrden(String orden) {
        if ("nombre".equals(orden) || "codigo".equals(orden)) {
            if (!orden.equals(this.orden)) pagina = 1;
            this.orden = orden;
        }
    }

    public String getDir() { return dir; }
    public void setDir(String dir) {
        if ("asc".equals(dir) || "desc".equals(dir)) {
            if (!dir.equals(this.dir)) pagina = 1;
            this.dir = dir;
        }
    }

    /** Solo los socios de la página actual. */
    public List<ClienteDTO> getClientesPagina() {
        List<ClienteDTO> todos = getListaOrdenada();
        if (todos.isEmpty()) return todos;
        int desde = (getPagina() - 1) * tamPagina;
        int hasta = Math.min(desde + tamPagina, todos.size());
        return todos.subList(desde, hasta);
    }

    public int getTotalRegistros() { return getListaClientes().size(); }

    public int getTotalPaginas() {
        return Math.max(1, (getTotalRegistros() + tamPagina - 1) / tamPagina);
    }

    /** Página actual, siempre dentro del rango válido. */
    public int getPagina() {
        if (pagina > getTotalPaginas()) pagina = getTotalPaginas();
        if (pagina < 1) pagina = 1;
        return pagina;
    }

    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamPagina() { return tamPagina; }

    public void setTamPagina(int tam) {
        for (int permitido : TAMANOS_PAGINA) {
            if (permitido == tam) {
                if (tam != tamPagina) pagina = 1;   // al cambiar el tamaño se vuelve al inicio
                tamPagina = tam;
                return;
            }
        }
    }

    public List<Integer> getTamanosPagina() {
        List<Integer> tamanos = new ArrayList<>();
        for (int t : TAMANOS_PAGINA) tamanos.add(t);
        return tamanos;
    }

    /** Número del primer registro mostrado (1, 11, 21...). */
    public int getDesde() {
        return getTotalRegistros() == 0 ? 0 : (getPagina() - 1) * tamPagina + 1;
    }

    public int getHasta() { return Math.min(getPagina() * tamPagina, getTotalRegistros()); }

    /** Números de página a mostrar alrededor de la actual (máximo 5). */
    public List<Integer> getPaginasVisibles() {
        int total = getTotalPaginas();
        int inicio = Math.max(1, getPagina() - PAGINAS_VISIBLES / 2);
        int fin = Math.min(total, inicio + PAGINAS_VISIBLES - 1);
        inicio = Math.max(1, fin - PAGINAS_VISIBLES + 1);
        List<Integer> paginas = new ArrayList<>();
        for (int i = inicio; i <= fin; i++) paginas.add(i);
        return paginas;
    }

    public String nuevo() {
        origenSuscripcion = false;
        prepararNuevo(true);
        return "/socios.xhtml?faces-redirect=true";
    }

    // ---------------------------------------------------------
    // Nuevo socio desde la Suscripción de membresía [F7] (frm_ManteClientes2 con OpcReg = true)
    // ---------------------------------------------------------

    @Inject
    private SuscripcionMembresiaBean suscripcionBean;

    /** true si la pantalla se abrió con "Nuevo socio [F7]" de la suscripción: al grabar se regresa allá. */
    private boolean origenSuscripcion;

    /** socios.xhtml?origen=suscripcion: socio nuevo con el código autogenerado. */
    public void nuevoDesdeSuscripcion() {
        origenSuscripcion = true;
        prepararNuevo(true);
    }

    public boolean isOrigenSuscripcion() { return origenSuscripcion; }

    // ---------------------------------------------------------
    // Búsqueda de DNI (Reniec) / RUC (Sunat), como txtRucDni_KeyPress de frm_ManteClientes2
    // ---------------------------------------------------------

    private String datosDocumentoTitulo;     // "Datos de la Reniec" / "Datos de la Sunat"
    private String datosDocumentoDetalle;    // RUC: estado y condición del contribuyente
    private boolean contribuyenteObservado;  // RUC no ACTIVO / no HABIDO (se muestra en rojo)

    public String buscarDocumento() {
        limpiarDatosDocumento();
        ClienteDTO s = getSocio();
        String numero = s.getRucDNI() == null ? "" : s.getRucDNI().trim();
        String tipo = s.getTipoId() == null ? "DNI" : s.getTipoId().trim().toUpperCase();
        if (numero.isEmpty()) {
            mensaje(FacesMessage.SEVERITY_WARN, "Ingrese el número de DNI o RUC a buscar.");
            return null;
        }
        if (!ServicioConsultaDocumento.estaConfigurado()) {
            mensaje(FacesMessage.SEVERITY_WARN, "La búsqueda de DNI/RUC no está configurada (falta gym.apisperu.token). Ingrese los datos manualmente.");
            return null;
        }
        if ("RUC".equals(tipo) && numero.length() < 11) {
            mensaje(FacesMessage.SEVERITY_WARN, "El RUC debe tener 11 dígitos.");
            return null;
        }

        if (!"RUC".equals(tipo) && numero.length() == 8) {
            ServicioConsultaDocumento.Resultado r = ServicioConsultaDocumento.consultarDni(numero);
            if (r == null) {
                mensaje(FacesMessage.SEVERITY_WARN, "Problemas en el Servidor de busqueda de la Reniec o no existe el DNI, puede registrar este numero de todas maneras.");
                return null;
            }
            s.setTipoId("DNI");
            s.setRazonSocial(r.nombreCompleto);
            datosDocumentoTitulo = "Datos de la Reniec";
            return null;
        }

        if (numero.length() == 11) {
            ServicioConsultaDocumento.Resultado r = ServicioConsultaDocumento.consultarRuc(numero);
            if (r == null) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No existe el Ruc o hay problemas con el servidor Sunat, Ingreselo manualmente los datos.");
                return null;
            }
            s.setTipoId("RUC");
            s.setRazonSocial(r.nombreCompleto);
            if (!r.direccion.isEmpty()) s.setDireccion(r.direccion);
            datosDocumentoTitulo = "Datos de la Sunat";
            datosDocumentoDetalle = "Estado: " + (r.estado.isEmpty() ? "-" : r.estado)
                    + " · Condición: " + (r.condicion.isEmpty() ? "-" : r.condicion);
            contribuyenteObservado = !(r.estado.equalsIgnoreCase("ACTIVO") || r.estado.equalsIgnoreCase("HABIDO"))
                    || (!r.condicion.isEmpty() && !r.condicion.equalsIgnoreCase("HABIDO"));
            return null;
        }

        mensaje(FacesMessage.SEVERITY_WARN, "El DNI debe tener 8 dígitos y el RUC 11.");
        return null;
    }

    private void limpiarDatosDocumento() {
        datosDocumentoTitulo = null;
        datosDocumentoDetalle = null;
        contribuyenteObservado = false;
    }

    public String getDatosDocumentoTitulo() { return datosDocumentoTitulo; }
    public String getDatosDocumentoDetalle() { return datosDocumentoDetalle; }
    public boolean isContribuyenteObservado() { return contribuyenteObservado; }

    private void prepararNuevo(boolean resetList) {
        limpiarDatosDocumento();
        archivoFoto = null;
        huellaCapturada = null;
        try {
            socio = getFacade().autogenera_CodCliente();
        } catch (RuntimeException ex) {
            socio = null;
        }
        if (socio == null) socio = new ClienteDTO();
        if (socio.getEstado() == null || socio.getEstado().isBlank()) socio.setEstado("Activo");
        modo = "grabar";
        if (resetList) listaClientes = null;
    }

    public String editar(String id) {
        socio = getFacade().buscarCliente(id);
        if (socio == null) {
            prepararNuevo(false);
            mensaje(FacesMessage.SEVERITY_ERROR, "No se encontró el socio seleccionado.");
            return "/ListaClientes.xhtml?faces-redirect=true";
        }
        modo = "actualizar";
        archivoFoto = null;
        huellaCapturada = null;
        origenSuscripcion = false;
        limpiarDatosDocumento();
        return "/socios.xhtml?faces-redirect=true";
    }

    public String guardar() {
        if (socio == null || socio.getRazonSocial() == null || socio.getRazonSocial().isBlank()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Ingrese el nombre o razón social del socio.");
            return null;
        }
        if (socio.getFechaCumpleaños() != null && socio.getFechaCumpleaños().toLocalDate().isAfter(java.time.LocalDate.now())) {
            mensaje(FacesMessage.SEVERITY_ERROR, "La fecha de cumpleaños no puede ser posterior a hoy.");
            return null;
        }
        if (!aplicarHuellaCapturada()) return null;
        try {
            boolean ok;
            if ("actualizar".equals(modo)) {
                if (!guardarFotoSubida()) return null;
                ok = getFacade().actualizarCliente(socio);
            } else {
                // Igual que GrabarNuevo: no se repite el DNI/RUC
                String doc = socio.getRucDNI() == null ? "" : socio.getRucDNI().trim();
                if (!doc.isEmpty() && getFacade().ListaClienteXRucDni(doc) != null) {
                    mensaje(FacesMessage.SEVERITY_ERROR, "Ya Existe este Ruc o Dni digitar nuevamente o corregir el que existe");
                    return null;
                }
                // Igual que GrabarNuevo: si el código ya fue usado (otro usuario grabó antes), se genera otro
                if (!codigoLibre(socio.getCliente_ID())) {
                    ClienteDTO generado = getFacade().autogenera_CodCliente();
                    if (generado != null) socio.setCliente_ID(generado.getCliente_ID());
                    if (!codigoLibre(socio.getCliente_ID())) {
                        mensaje(FacesMessage.SEVERITY_ERROR, "Ya Existe este Codigo digitar nuevamente o corregir el que existe");
                        return null;
                    }
                }
                if (!guardarFotoSubida()) return null;
                ok = getFacade().registrarCliente(socio);
            }
            if (!ok) {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo guardar el socio. Verifique los datos y la conexión.");
                return null;
            }
            listaClientes = null;

            // Abierto desde la Suscripción [F7]: se regresa allá con el socio cargado, listo para Registrar
            if (origenSuscripcion && !"actualizar".equals(modo)) {
                String id = socio.getCliente_ID() == null ? "" : socio.getCliente_ID().trim();
                origenSuscripcion = false;
                suscripcionBean.recibirSocioNuevo(id);
                prepararNuevo(false);
                return "/membresias.xhtml?faces-redirect=true";
            }

            FacesContext.getCurrentInstance().getExternalContext().getFlash()
                    .put("mensajeSocios", "actualizar".equals(modo)
                            ? "Socio actualizado correctamente." : "Socio registrado correctamente.");
            prepararNuevo(false);
            return "/ListaClientes.xhtml?faces-redirect=true";
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Ocurrió un error al guardar el socio.");
            return null;
        }
    }

    /** Código no vacío y que aún no pertenece a ningún socio. */
    private boolean codigoLibre(String codigo) {
        return codigo != null && !codigo.isBlank() && getFacade().ListaClienteXCod(codigo.trim()) == null;
    }

    public String eliminar(String id) {
        ClienteDTO cliente = new ClienteDTO();
        cliente.setCliente_ID(id);
        try {
            if (getFacade().EliminarCliente(cliente)) {
                listaClientes = null;
                mensaje(FacesMessage.SEVERITY_INFO, "Socio eliminado correctamente.");
            } else {
                mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo eliminar el socio.");
            }
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Ocurrió un error al eliminar el socio.");
        }
        return null;
    }

    public String cancelar() {
        prepararNuevo(false);
        if (origenSuscripcion) {
            origenSuscripcion = false;
            return "/membresias.xhtml?faces-redirect=true";   // la suscripción conserva lo que tenía
        }
        return Navegacion.salir("/ListaClientes.xhtml?faces-redirect=true");
    }

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    /** Fecha de cumpleaños del socio (input date del formulario). */
    public java.time.LocalDate getFechaCumpleanos() {
        return getSocio().getFechaCumpleaños() == null ? null : getSocio().getFechaCumpleaños().toLocalDate();
    }
    public void setFechaCumpleanos(java.time.LocalDate fecha) {
        getSocio().setFechaCumpleaños(fecha == null ? null : fecha.atStartOfDay());
    }

    public ClienteDTO getSocio() {
        if (socio == null) socio = new ClienteDTO();
        return socio;
    }
    public void setSocio(ClienteDTO socio) { this.socio = socio; }

    // ---------------------------------------------------------
    // Huella dactilar (lector ZK9500 por medio del agente local, ver js/huella-zk.js)
    // ---------------------------------------------------------

    /** Plantilla en Base64 que dejó la página al capturar la huella; se graba en Cliente.HuellaDactilar. */
    private String huellaCapturada;

    public String getHuellaCapturada() { return huellaCapturada; }
    public void setHuellaCapturada(String huellaCapturada) { this.huellaCapturada = huellaCapturada; }

    /** El socio ya tiene huella grabada (o capturada en esta pantalla). */
    public boolean isTieneHuella() {
        return (huellaCapturada != null && !huellaCapturada.isBlank())
                || (socio != null && socio.getHuellaDactilar() != null && socio.getHuellaDactilar().length > 0);
    }

    /** Pasa la huella capturada al socio. Sin captura nueva se conserva la grabada (el SP usa ISNULL). */
    private boolean aplicarHuellaCapturada() {
        String b64 = huellaCapturada == null ? "" : huellaCapturada.trim();
        if (b64.isEmpty()) return true;
        try {
            byte[] plantilla = java.util.Base64.getDecoder().decode(b64);
            if (plantilla.length < 64 || plantilla.length > 4096) throw new IllegalArgumentException();
            socio.setHuellaDactilar(plantilla);
            return true;
        } catch (IllegalArgumentException ex) {
            huellaCapturada = null;
            mensaje(FacesMessage.SEVERITY_ERROR, "La huella capturada no es válida. Vuelva a capturarla.");
            return false;
        }
    }

    // ---------------------------------------------------------
    // Foto del socio: botón "Cargar foto" (h:inputFile); se guarda en el servidor al Grabar/Actualizar
    // ---------------------------------------------------------

    /** Archivo elegido en "Cargar foto" (solo vive durante la petición de Grabar). */
    private transient Part archivoFoto;

    public Part getArchivoFoto() { return archivoFoto; }
    public void setArchivoFoto(Part archivoFoto) { this.archivoFoto = archivoFoto; }

    /** Si eligió una imagen, la guarda como {Cliente_ID}.{ext} y deja su ruta en socio.foto. */
    private boolean guardarFotoSubida() {
        Part archivo = archivoFoto;
        archivoFoto = null;
        if (archivo == null || archivo.getSize() == 0) return true;
        try {
            socio.setFoto(AlmacenFotos.guardar(archivo, socio.getCliente_ID()));
            return true;
        } catch (AlmacenFotos.FotoInvalidaException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo guardar la foto en el servidor.");
        }
        return false;
    }

    /** Hay una foto grabada que el servidor puede mostrar (/reportes/fotoSocio lee la ruta de la BD). */
    public boolean isTieneFotoGuardada() {
        if (!"actualizar".equals(modo) || socio == null || socio.getFoto() == null) return false;
        String ruta = socio.getFoto().trim();
        return !ruta.isEmpty() && new java.io.File(ruta).isFile();
    }

    /** Evita que el navegador muestre la foto anterior en caché tras cambiarla. */
    public long getVersionFoto() {
        if (!isTieneFotoGuardada()) return 0;
        return new java.io.File(socio.getFoto().trim()).lastModified();
    }

    private void mensaje(FacesMessage.Severity severity, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, texto, null));
    }
}
