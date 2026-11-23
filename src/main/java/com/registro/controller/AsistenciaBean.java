package com.registro.controller;

import com.registro.dto.AsistenciaDTO;
import com.registro.dto.ClienteDTO;
import com.registro.dto.ResultadoAsistenciaDTO;
import com.registro.facade.AsistenciaFACADE;
import com.registro.facade.ClienteFACADE;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Control de asistencia: igual que frm_RegistrarAsistencia de SisdataGimnasio.
 * El socio digita su código o DNI/RUC, o escanea su QR; la regla está en AsistenciaFACADE.RegistrarAsistenciaF.
 * (La lectura de huella digital del formulario de escritorio no aplica en la web.)
 */
@Named("asistenciaBean")
@SessionScoped
public class AsistenciaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient ClienteFACADE clienteFacade;
    private transient AsistenciaFACADE asistenciaFacade;

    /** Socios activos cargados al abrir la ventana (frm_ControlAsistencia_Load). */
    private List<ClienteDTO> clientes;
    private String turno = "0";

    private String codigo;

    // Datos del socio en pantalla
    private String nombreSocio;
    private boolean bienvenido;
    private String fotoClienteId;
    private AsistenciaDTO periodo;
    private List<AsistenciaDTO> historial;
    private boolean ingresoRegistrado;      // el último intento terminó en ingreso correcto
    /**
     * Controles de ingreso bloqueados (casilla Código + botón Ingresar [Enter]).
     * Se pone en true en cuanto se muestra el resultado de un registro (correcto o no)
     * y solo vuelve a false con Nuevo [F2]. Al abrir la página está en false (activos).
     */
    private boolean controlesBloqueados;

    // Avisos
    private boolean mostrarAvisos;
    private int asistenciasPermitidas;
    private int asistenciasUsadas = -1;
    private String deudaMonto;
    private String deudaTexto;

    private ClienteFACADE clientes() {
        if (clienteFacade == null) clienteFacade = new ClienteFACADE();
        return clienteFacade;
    }

    private AsistenciaFACADE asistencias() {
        if (asistenciaFacade == null) asistenciaFacade = new AsistenciaFACADE();
        return asistenciaFacade;
    }

    // ---------------------------------------------------------
    // Carga de la ventana
    // ---------------------------------------------------------

    /** Al abrir la página: pantalla limpia, socios activos y turno de la caja (frm_ControlAsistencia_Load). */
    public void abrir() {
        nuevo();
        cargar();
    }

    /** Socios activos y turno de la caja abierta ("0" si no hay). */
    public void cargar() {
        try {
            List<ClienteDTO> lista = clientes().ObtenerListadoCliente();
            clientes = lista != null ? lista : new ArrayList<>();
            turno = asistencias().TurnoCajaAbiertaF(MembresiaBean.codEmpSesion());
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    // ---------------------------------------------------------
    // Ingresar [Enter]
    // ---------------------------------------------------------

    public String registrar() {
        // Defensa en el servidor: aunque alguien habilite la casilla desde el navegador,
        // no se registra otro ingreso hasta presionar Nuevo.
        if (controlesBloqueados) {
            codigo = null;
            return null;
        }
        try {
            // código digitado, o lo que envía el lector QR (USB o app del celular)
            String codcli = resolverSocio(codigo);
            if (codcli.isEmpty()) {
                codigo = null;
                return null;
            }
            registrarIngreso(codcli);
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
        return null;
    }

    private void registrarIngreso(String codcli) {
        limpiarSocio();   // no dejar en pantalla al socio anterior si este no puede ingresar
        String codEmp = MembresiaBean.codEmpSesion();
        ResultadoAsistenciaDTO res = asistencias().RegistrarAsistenciaF(codEmp, codcli, turno, LocalDateTime.now());

        periodo = res.getDatosSuscripcion();
        if (res.getHistorialAsistencias() != null) historial = res.getHistorialAsistencias();
        if (res.getDatosCliente() != null) mostrarSocio(res.getDatosCliente(), res.isExito());
        if (res.getDatosSuscripcion() != null) mostrarAvisos(res, codEmp, codcli);

        codigo = null;
        controlesBloqueados = true;   // ya hay un resultado en pantalla: solo Nuevo vuelve a activar
        if (!res.isExito()) {
            mensaje("DUPLICADO".equals(res.getTipoMensaje()) ? FacesMessage.SEVERITY_WARN : FacesMessage.SEVERITY_ERROR,
                    res.getMensaje());
            return;
        }
        ingresoRegistrado = true;
    }

    /** Nombre y foto del socio; "Bienvenido(a)" solo si pudo ingresar. */
    private void mostrarSocio(ClienteDTO cli, boolean ingreso) {
        bienvenido = ingreso;
        nombreSocio = cli.getRazonSocial() == null ? "" : cli.getRazonSocial().trim();
        String ruta = cli.getFoto() == null ? "" : cli.getFoto().trim();
        if (!ruta.isEmpty() && new File(ruta).isFile()) {
            fotoClienteId = cli.getCliente_ID() == null ? null : cli.getCliente_ID().trim();
        }
    }

    /** Asistencias (máximo / usadas / quedan) y deuda pendiente del socio. */
    private void mostrarAvisos(ResultadoAsistenciaDTO res, String codEmp, String codcli) {
        mostrarAvisos = true;
        asistenciasPermitidas = res.getAsistenciasPermitidas();
        asistenciasUsadas = res.getAsistenciasUsadas();
        try {
            mostrarDeuda(codEmp, codcli);
        } catch (RuntimeException ex) {
            // el aviso de deuda es informativo: nunca impide registrar
        }
    }

    private void mostrarDeuda(String codEmp, String codcli) {
        String pedido = periodo != null ? periodo.getPedido_ID() : null;
        if (pedido == null || pedido.isEmpty()) return;
        AsistenciaDTO deuda = asistencias().DeudaPedidoF(codEmp, codcli, pedido);
        if (deuda == null || deuda.getFechaFin() == null || deuda.getTotal() <= 0) return;

        LocalDate venc = deuda.getFechaFin().toLocalDate();
        LocalDate hoy = LocalDate.now();
        deudaMonto = String.format(java.util.Locale.US, "%,.2f", deuda.getTotal());
        if (venc.isEqual(hoy)) {
            deudaTexto = "y venció hoy";
        } else if (venc.isBefore(hoy)) {
            deudaTexto = "y venció " + ChronoUnit.DAYS.between(venc, hoy) + " días";
        } else {
            deudaTexto = "y está pendiente de pago";
        }
    }

    // ---------------------------------------------------------
    // Lectura QR / código
    // ---------------------------------------------------------

    /** Código de socio (Cliente_ID) a partir de lo leído o digitado; "" si está vacío. */
    private String resolverSocio(String texto) {
        String t = limpiarLectura(texto);
        if (t.isEmpty()) return "";

        // 1) socios activos cargados al abrir: por código o por DNI/RUC
        String cod = buscarEnClientes(t);
        if (cod != null) return cod;

        // 2) DNI/RUC de un socio registrado después de abrir esta ventana
        if (esNumero(t) && (t.length() == 8 || t.length() == 11)) {
            ClienteDTO c = clientes().ListaClienteXRucDni(t);
            if (c != null && c.getCliente_ID() != null) return c.getCliente_ID().trim();
        }

        // 3) código corto: 12 -> 00000012
        if (esNumero(t) && t.length() < 8) return rellenar(t);
        return t;
    }

    /** Quita caracteres de control, prefijos del QR ("SOCIO:00000012", "...?codigo=12") y espacios. */
    static String limpiarLectura(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : (texto == null ? "" : texto).toCharArray()) {
            if (!Character.isISOControl(c)) sb.append(c);
        }
        String t = sb.toString().trim();
        for (char sep : new char[] { '=', ':', '/', '|', ';' }) {
            int k = t.lastIndexOf(sep);
            if (k >= 0 && k < t.length() - 1) t = t.substring(k + 1);
        }
        return t.trim().replace(" ", "");
    }

    private String buscarEnClientes(String t) {
        if (clientes == null) cargar();
        if (clientes == null) return null;
        String relleno = esNumero(t) && t.length() < 8 ? rellenar(t) : t;
        for (ClienteDTO c : clientes) {
            String cod = c.getCliente_ID() == null ? "" : c.getCliente_ID().trim();
            if (cod.equalsIgnoreCase(t) || cod.equals(relleno)) return cod;
        }
        for (ClienteDTO c : clientes) {
            String doc = c.getRucDNI() == null ? "" : c.getRucDNI().trim();
            if (doc.equals(t)) return c.getCliente_ID() == null ? null : c.getCliente_ID().trim();
        }
        return null;
    }

    private static String rellenar(String t) {
        StringBuilder sb = new StringBuilder(t);
        while (sb.length() < 8) sb.insert(0, '0');
        return sb.toString();
    }

    private static boolean esNumero(String t) {
        if (t.isEmpty()) return false;
        for (char c : t.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    // ---------------------------------------------------------
    // Nuevo [F2]
    // ---------------------------------------------------------

    public String nuevo() {
        codigo = null;
        limpiarSocio();
        ingresoRegistrado = false;
        controlesBloqueados = false;  // Nuevo [F2]: casilla Código e Ingresar activos otra vez
        return null;
    }

    private void limpiarSocio() {
        nombreSocio = null;
        bienvenido = false;
        fotoClienteId = null;
        periodo = null;
        historial = null;
        mostrarAvisos = false;
        asistenciasPermitidas = 0;
        asistenciasUsadas = -1;
        deudaMonto = null;
        deudaTexto = null;
    }

    private static void mensaje(FacesMessage.Severity severidad, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, msg, null));
    }

    // ---------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombreSocio() { return nombreSocio; }
    public boolean isBienvenido() { return bienvenido; }
    public String getFotoClienteId() { return fotoClienteId; }
    public AsistenciaDTO getPeriodo() { return periodo; }
    public List<AsistenciaDTO> getHistorial() { return historial; }
    public boolean isIngresoRegistrado() { return ingresoRegistrado; }
    public boolean isControlesBloqueados() { return controlesBloqueados; }

    public boolean isMostrarAvisos() { return mostrarAvisos; }
    /** Máximo / usadas / quedan, solo si la membresía tiene límite. */
    public boolean isConLimite() { return asistenciasPermitidas > 0; }
    /** Sin límite: solo el total de asistencias (si se conoce). */
    public boolean isSoloTotal() { return asistenciasPermitidas <= 0 && asistenciasUsadas >= 0; }
    public int getAsistenciasPermitidas() { return asistenciasPermitidas; }
    public int getAsistenciasUsadas() { return Math.max(asistenciasUsadas, 0); }
    public int getAsistenciasRestantes() { return Math.max(asistenciasPermitidas - Math.max(asistenciasUsadas, 0), 0); }
    public String getDeudaMonto() { return deudaMonto; }
    public String getDeudaTexto() { return deudaTexto; }

    /** Periodo (o, si el SP no lo trae, el nombre de la membresía). */
    public String getPeriodoTexto() {
        if (periodo == null) return "";
        return periodo.getPeriodo() != null ? periodo.getPeriodo() : (periodo.getMembresia() == null ? "" : periodo.getMembresia());
    }
}
