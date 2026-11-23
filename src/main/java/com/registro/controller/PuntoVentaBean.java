package com.registro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registro.dao.FacturacionDAO;
import com.registro.dto.AperturaCierreCajaDTO;
import com.registro.dto.FacturacionDTO;
import com.registro.dto.UsuarioDTO;
import com.registro.facade.AperturaCierreCajaFACADE;
import com.registro.facade.FacturacionFACADE;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador del Punto de Venta (PuntoVenta.xhtml): grabar, modificar y eliminar ventas.
 *
 * La página (JavaScript) arma la venta en JSON y la envía por ajax a {@link #grabar()}; el resultado vuelve
 * en {@link #getResultado()} (también JSON). La transacción la hace FacturacionFACADE:
 *  - grabar   -> RegistrarVentaF  (venta nueva)        |  pedidoId vacío
 *  - grabar   -> ModificarVentaF  (venta en edición)   |  pedidoId con el N° de pedido
 *  - eliminar -> AnularVentaF
 * Los datos de la caja (FechaTurno, Turno, CodResp) salen del turno abierto, y CodEmp / CodUser del usuario en sesión.
 */
@Named("puntoVentaBean")
@ViewScoped
public class PuntoVentaBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double IGV = 0.18;          // el total del punto de venta incluye IGV (igual que la ventana de pago)
    private static final String CLIENTE_GENERAL = "01";   // cliente por defecto del WinForm ("CLIENTE")
    private static final int LARGO_CLIENTE = 8;            // Cliente_ID char(8)
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private transient FacturacionFACADE facade;

    private String codEmp;
    private int codPer;
    private int codUser;
    private LocalDate fechaTurno;
    private String turno;
    private String errorCaja;
    private String errorProductos = "";
    private List<FacturacionDTO> tiposDoc;

    /** JSON que envía la página (venta, o {pedidoId} para cargar / eliminar). */
    private String datos;
    /** JSON con la respuesta para la página. */
    private String resultado = "{}";

    private FacturacionFACADE facade() {
        if (facade == null) facade = new FacturacionFACADE();
        return facade;
    }

    @PostConstruct
    public void init() {
        UsuarioDTO u = usuario();
        codEmp = u == null || vacio(u.getCodEmp()) ? MembresiaBean.COD_EMP_DEFECTO : u.getCodEmp().trim();
        codUser = u == null ? 0 : (u.getId_UserSist() > 0 ? u.getId_UserSist() : u.getPersonal_ID());
        try {
            AperturaCierreCajaDTO caja = new AperturaCierreCajaFACADE().ListaTurnoAbiertoF(codEmp, LocalDate.now().atStartOfDay());
            if (caja == null || caja.getFechaTurno() == null) {
                errorCaja = "No hay un turno de caja abierto. Abra la caja antes de vender.";
            } else {
                fechaTurno = caja.getFechaTurno().toLocalDate();
                turno = vacio(caja.getTurno()) ? "1" : caja.getTurno().trim();
                codPer = u != null && u.getPersonal_ID() > 0 ? u.getPersonal_ID() : caja.getCodResp();
            }
        } catch (RuntimeException ex) {
            errorCaja = "No se pudo leer el turno de caja: " + ex.getMessage();
        }
    }

    /* ======================================================================================
     *  Datos para la página
     * ====================================================================================== */

    /** Productos activos de la empresa (JSON) para la grilla del Punto de Venta. */
    public String getProductosJson() {
        try {
            return JSON.writeValueAsString(productos());
        } catch (Exception ex) {
            return "[]";
        }
    }

    /** Mensaje si no se pudieron leer los productos ("" si todo bien). Va después de productosJson en la página. */
    public String getErrorProductos() { return errorProductos; }

    /** Tipos de documento activos (Nota de venta, Boleta, Factura). Se consultan una vez por pantalla. */
    public List<FacturacionDTO> getTiposDoc() {
        if (tiposDoc == null) tiposDoc = cargarTiposDoc();
        return tiposDoc;
    }

    private List<FacturacionDTO> cargarTiposDoc() {
        try {
            List<FacturacionDTO> lista = facade().Lista_TiposDocF();
            if (!lista.isEmpty()) return lista;
        } catch (RuntimeException ex) {
            System.err.println("Lista_TiposDocF: " + ex.getMessage());
        }
        List<FacturacionDTO> fijos = new ArrayList<>();
        fijos.add(tipo(FacturacionDAO.DOC_NOTA_VENTA, "NOTA DE VENTA"));
        fijos.add(tipo(FacturacionDAO.DOC_BOLETA, "BOLETA DE VENTA"));
        fijos.add(tipo(FacturacionDAO.DOC_FACTURA, "FACTURA"));
        return fijos;
    }

    private static FacturacionDTO tipo(String id, String descripcion) {
        FacturacionDTO t = new FacturacionDTO();
        t.setDocumento_ID(id);
        t.setDescripcion(descripcion);
        return t;
    }

    public String getErrorCaja() { return errorCaja; }
    public String getTurnoTexto() {
        return fechaTurno == null ? "" : "Turno " + turno + " · " + fechaTurno.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /* ======================================================================================
     *  Acciones (ajax)
     * ====================================================================================== */

    /** Graba la venta nueva, o modifica la venta en edición si trae pedidoId. */
    public void grabar() {
        try {
            exigirCaja();
            JsonNode v = JSON.readTree(datos == null ? "{}" : datos);
            String pedidoId = texto(v, "pedidoId");
            boolean modificar = !pedidoId.isEmpty();

            // Venta original (al modificar): sus cantidades vuelven al stock disponible
            Map<String, Double> reservado = new HashMap<>();
            if (modificar) {
                for (FacturacionDTO d : facade().ListaDetalleVentaXPedidoF(pedidoId)) {
                    reservado.merge(limpio(d.getProducto_ID()), d.getCantidad(), Double::sum);
                }
            }

            FacturacionDTO cab = armarCabecera(v);
            List<FacturacionDTO> detalles = armarDetalle(v, reservado);
            List<FacturacionDTO> pagos = armarPagos(v, cab);

            FacturacionDTO hecho;
            if (modificar) {
                cab.setPedido_ID(pedidoId);
                hecho = facade().ModificarVentaF(cab, detalles, pagos);
            } else {
                hecho = facade().RegistrarVentaF(cab, detalles, pagos);
            }

            Map<String, Object> r = ok((modificar ? "Se hicieron los cambios de la venta N° " : "Se grabó la venta N° ")
                    + hecho.getPedido_ID() + " (" + documento(hecho) + ").");
            r.put("pedido", hecho.getPedido_ID());
            r.put("documento", documento(hecho));
            r.put("modificada", modificar);
            r.put("productos", productos());
            responder(r);
        } catch (Exception ex) {
            responder(error(ex));
        }
    }

    /** Carga una venta para modificarla (cabecera y productos). */
    public void cargarVenta() {
        try {
            exigirCaja();
            String pedidoId = texto(JSON.readTree(datos == null ? "{}" : datos), "pedidoId");
            FacturacionDTO cab = facade().ListaVentaXPedidoF(codEmp, pedidoId);
            if (cab == null) throw new IllegalArgumentException("La venta N° " + pedidoId + " no existe.");
            if (!FacturacionDAO.DOC_NOTA_VENTA.equals(cab.getTipoDoc()) && "SI".equalsIgnoreCase(cab.getImpreso())) {
                throw new IllegalArgumentException("El comprobante ya se imprimió; no se puede modificar.");
            }
            Map<String, Object> venta = new LinkedHashMap<>();
            venta.put("pedido", cab.getPedido_ID());
            venta.put("documento", documento(cab));
            venta.put("tipoDoc", cab.getTipoDoc());
            venta.put("clienteId", CLIENTE_GENERAL.equals(cab.getCliente_ID()) ? "" : cab.getCliente_ID());
            venta.put("clienteNombre", "CLIENTE".equalsIgnoreCase(cab.getRazonSocial()) ? "" : cab.getRazonSocial());
            venta.put("total", cab.getTotal());
            List<Map<String, Object>> items = new ArrayList<>();
            for (FacturacionDTO d : facade().ListaDetalleVentaXPedidoF(pedidoId)) {
                Map<String, Object> it = new LinkedHashMap<>();
                it.put("productoId", d.getProducto_ID());
                it.put("descripcion", d.getDescripcion());
                it.put("precio", d.getPrecioUnitario());
                it.put("costo", d.getPrecioCosto());
                it.put("cantidad", d.getCantidad());
                items.add(it);
            }
            venta.put("items", items);
            Map<String, Object> r = ok("Venta N° " + cab.getPedido_ID() + " cargada para modificar.");
            r.put("venta", venta);
            responder(r);
        } catch (Exception ex) {
            responder(error(ex));
        }
    }

    /** Elimina (anula) la venta: devuelve el stock y borra la venta. */
    public void eliminar() {
        try {
            exigirCaja();
            String pedidoId = texto(JSON.readTree(datos == null ? "{}" : datos), "pedidoId");
            if (pedidoId.isEmpty()) throw new IllegalArgumentException("Indique la venta que desea eliminar.");
            FacturacionDTO v = facade().AnularVentaF(codEmp, pedidoId);
            Map<String, Object> r = ok("Se eliminó la venta N° " + pedidoId + " (" + documento(v) + ").");
            r.put("pedido", pedidoId);
            r.put("productos", productos());
            responder(r);
        } catch (Exception ex) {
            responder(error(ex));
        }
    }

    /** Ventas del turno abierto (historial). */
    public void listarVentas() {
        try {
            exigirCaja();
            List<Map<String, Object>> ventas = new ArrayList<>();
            for (FacturacionDTO c : facade().ListaVentasTurnoF(codEmp, fechaTurno, turno)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("pedido", c.getPedido_ID());
                m.put("documento", documento(c));
                m.put("tipo", c.getTipo_documento());
                m.put("hora", c.getFechaHora() == null ? "" : c.getFechaHora().format(HORA));
                m.put("cliente", c.getRazonSocial());
                m.put("condPago", c.getCondPago());
                m.put("formaPago", c.getFormaPago());
                m.put("total", c.getTotal());
                m.put("impreso", !FacturacionDAO.DOC_NOTA_VENTA.equals(c.getTipoDoc()) && "SI".equalsIgnoreCase(c.getImpreso()));
                ventas.add(m);
            }
            Map<String, Object> r = ok("");
            r.put("ventas", ventas);
            r.put("turno", getTurnoTexto());
            responder(r);
        } catch (Exception ex) {
            responder(error(ex));
        }
    }

    /* ======================================================================================
     *  Armado de la venta
     * ====================================================================================== */

    private FacturacionDTO armarCabecera(JsonNode v) {
        String tipoDoc = texto(v, "tipoDoc");
        if (!tipoDoc.equals(FacturacionDAO.DOC_NOTA_VENTA) && !tipoDoc.equals(FacturacionDAO.DOC_BOLETA)
                && !tipoDoc.equals(FacturacionDAO.DOC_FACTURA)) {
            tipoDoc = FacturacionDAO.DOC_NOTA_VENTA;
        }
        double total = redondear(v.path("total").asDouble(0));
        if (total <= 0) throw new IllegalArgumentException("El total de la venta no es válido.");

        JsonNode cli = v.path("cliente");
        String clienteId = texto(cli, "documento");
        String nombre = texto(cli, "nombre");
        if (clienteId.length() > LARGO_CLIENTE) {
            throw new IllegalArgumentException("El DNI / código del cliente admite como máximo " + LARGO_CLIENTE
                    + " caracteres (Cliente_ID es char(8)).");
        }
        if (FacturacionDAO.DOC_FACTURA.equals(tipoDoc) && clienteId.isEmpty()) {
            throw new IllegalArgumentException("Para emitir FACTURA indique el documento del cliente (pestaña Cliente).");
        }

        FacturacionDTO c = new FacturacionDTO();
        c.setTipoDoc(tipoDoc);
        c.setCliente_ID(clienteId.isEmpty() ? CLIENTE_GENERAL : clienteId);
        c.setRazonSocial(nombre.isEmpty() ? "CLIENTE" : recortar(nombre.toUpperCase(Locale.ROOT), 100));
        c.setDireccion(recortar(texto(cli, "direccion"), 100));
        c.setTelefono(recortar(texto(cli, "telefono"), 20));
        c.setCorreo(recortar(texto(cli, "email"), 100));
        c.setFecha_Pedido(LocalDate.now());
        c.setFechaHora(LocalDateTime.now());
        c.setTotal(total);
        double sub = redondear(total / (1 + IGV));
        c.setSubTotal(sub);
        c.setIGV(redondear(total - sub));
        c.setMontoLetras(NumeroLetras.convertir(total));
        c.setEstadoPed("Efectiva");                   // Estado de la venta (igual que el WinForm)
        c.setCodEmp(codEmp);
        c.setCodPer(codPer);
        c.setCodUser(codUser);
        c.setDia("A");
        c.setFechaTurno(fechaTurno);
        c.setTurno(turno);
        return c;
    }

    private List<FacturacionDTO> armarDetalle(JsonNode v, Map<String, Double> reservado) {
        Map<String, FacturacionDTO> catalogo = new HashMap<>();
        for (FacturacionDTO p : facade().Lista_ProductosGral_ActivosXNomF(codEmp, "")) {
            catalogo.put(limpio(p.getProducto_ID()), p);
        }
        // Un producto por fila (sp_Insert_Detalle_Ventas actualiza si el producto se repite)
        Map<String, FacturacionDTO> filas = new LinkedHashMap<>();
        for (JsonNode it : v.path("items")) {
            String id = texto(it, "productoId");
            double cant = it.path("cantidad").asDouble(0);
            double precio = it.path("precio").asDouble(-1);
            if (id.isEmpty() || cant <= 0) continue;
            FacturacionDTO p = catalogo.get(id);
            FacturacionDTO d = filas.get(id);
            if (d == null) {
                d = new FacturacionDTO();
                d.setProducto_ID(id);
                d.setDescripcion(recortar(p != null ? p.getDescripcion() : texto(it, "descripcion"), 100));
                d.setPrecioCosto(p != null ? p.getPrecioCosto() : it.path("costo").asDouble(0));
                d.setPrecioUnitario(precio >= 0 ? precio : (p != null ? p.getPrecioVenta() : 0));
                filas.put(id, d);
            }
            d.setCantidad(d.getCantidad() + cant);
            d.setImporte(redondear(d.getPrecioUnitario() * d.getCantidad()));
        }
        if (filas.isEmpty()) throw new IllegalArgumentException("No ha ingresado productos; imposible grabar la venta.");

        // Stock: lo que hay más lo que la venta original ya tenía (al modificar)
        for (FacturacionDTO d : filas.values()) {
            FacturacionDTO p = catalogo.get(d.getProducto_ID());
            double disponible = (p != null ? p.getStockFinal() : 0) + reservado.getOrDefault(d.getProducto_ID(), 0d);
            if (p == null && !reservado.containsKey(d.getProducto_ID())) {
                throw new IllegalArgumentException("El producto " + d.getDescripcion() + " ya no está activo.");
            }
            if (d.getCantidad() > disponible + 0.0001) {
                throw new IllegalArgumentException("Stock insuficiente para " + d.getDescripcion()
                        + ": disponible " + formato(disponible) + ".");
            }
        }
        return new ArrayList<>(filas.values());
    }

    /** Formas de pago de la ventana; también deja en la cabecera CondPago, FormaPago, IDFormaPago y el crédito. */
    private List<FacturacionDTO> armarPagos(JsonNode v, FacturacionDTO cab) {
        JsonNode pago = v.path("pago");
        double total = cab.getTotal();
        double vuelto = Math.max(0, pago.path("vuelto").asDouble(0));

        List<FacturacionDTO> pagos = new ArrayList<>();
        double pagado = 0;
        double credito = 0;
        boolean soloEfectivo = true;
        FacturacionDTO mayor = null;
        for (JsonNode d : pago.path("detalle")) {
            double monto = redondear(d.path("monto").asDouble(0));
            if (monto <= 0) continue;
            String tipo = texto(d, "tipo");
            String idForma = texto(d, "idFormaPago");
            if (idForma.isEmpty()) {
                throw new IllegalArgumentException("Las formas de pago no se cargaron de la base de datos; no se puede grabar la venta.");
            }
            // El vuelto sale del efectivo: en DetalleFormasPago va lo que realmente queda en caja
            if ("efectivo".equals(tipo) && vuelto > 0) {
                double resta = Math.min(vuelto, monto);
                monto = redondear(monto - resta);
                vuelto = redondear(vuelto - resta);
                if (monto <= 0) continue;
            }
            FacturacionDTO p = new FacturacionDTO();
            p.setIDFormaPago(idForma);
            p.setFormaPago(recortar(texto(d, "metodo"), 30));
            p.setMontoParcial(monto);
            p.setMontoReal(monto);
            p.setNroOperacion(recortar(texto(d, "info"), 30));
            if ("tarjeta".equals(tipo)) {
                double porCom = d.path("porCom").asDouble(0);
                double com = redondear(d.path("comision").asDouble(0));
                p.setPorcInc(porCom);
                p.setMontoInc(redondear(monto + com));   // monto con la comisión de la tarjeta
                p.setIDBanco(texto(d, "idTarjeta"));      // código de TarjetasPago (como el WinForm)
            } else {
                p.setPorcInc(0);
                p.setMontoInc(monto);
                p.setIDBanco("0");
            }
            if ("credito".equals(tipo)) credito += monto;
            if (!"efectivo".equals(tipo)) soloEfectivo = false;
            if (mayor == null || monto > mayor.getMontoParcial()) mayor = p;
            pagado += monto;
            pagos.add(p);
        }
        if (pagos.isEmpty()) throw new IllegalArgumentException("No se registró la forma de pago; la venta no se grabó.");
        if (redondear(pagado) < total - 0.009) {
            throw new IllegalArgumentException("Falta pagar S/ " + formato(total - pagado) + ".");
        }

        cab.setIDFormaPago(mayor.getIDFormaPago());
        cab.setFormaPago(soloEfectivo ? "EFECTIVO" : "TARJETA O DEPOSITO");   // mismo criterio del WinForm

        if (credito > 0) {
            String clienteId = cab.getCliente_ID();
            if (CLIENTE_GENERAL.equals(clienteId)) {
                throw new IllegalArgumentException("Para vender al crédito indique el DNI del cliente (pestaña Cliente).");
            }
            LocalDate venc;
            try {
                venc = LocalDate.parse(texto(pago, "fechaVencimiento"));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Indique la fecha de vencimiento del crédito.");
            }
            if (venc.isBefore(cab.getFecha_Pedido())) {
                throw new IllegalArgumentException("La fecha de vencimiento no puede ser menor a la fecha de emisión.");
            }
            cab.setCondPago("CREDITO");
            cab.setCondCli("CREDITO");
            cab.setFechaVenc(venc);
            cab.setNroDias((int) ChronoUnit.DAYS.between(cab.getFecha_Pedido(), venc));
            cab.setMonto(redondear(credito));
            cab.setSaldoCredito(redondear(credito));
            cab.setCondicionCredito("ADEUDA");
            cab.setInteres(0);
        } else {
            cab.setCondPago("CONTADO");
            cab.setCondCli("CONTADO");
        }
        return pagos;
    }

    /* ======================================================================================
     *  Ayudas
     * ====================================================================================== */

    private List<Map<String, Object>> productos() {
        List<Map<String, Object>> lista = new ArrayList<>();
        errorProductos = "";
        try {
            for (FacturacionDTO p : facade().Lista_ProductosGral_ActivosXNomF(codEmp, "")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", limpio(p.getProducto_ID()));
                m.put("nombre", vacio(p.getDescripcion()) ? limpio(p.getNombreProducto()) : p.getDescripcion());
                m.put("codigo", limpio(p.getCodigoBarra()));
                m.put("precio", p.getPrecioVenta());
                m.put("costo", p.getPrecioCosto());
                m.put("stock", p.getStockFinal());
                m.put("categoria", limpio(p.getCategoria()));
                m.put("marca", limpio(p.getMarca()));
                m.put("unidad", limpio(p.getUnidadMedida()));
                lista.add(m);
            }
        } catch (RuntimeException ex) {
            System.err.println("Lista_ProductosGral_ActivosXNomF: " + ex.getMessage());
            errorProductos = "No se pudieron cargar los productos: " + ex.getMessage();
        }
        return lista;
    }

    private void exigirCaja() {
        if (errorCaja != null) throw new IllegalStateException(errorCaja);
    }

    private static String documento(FacturacionDTO c) {
        String serie = limpio(c.getNroSerie());
        String nro = limpio(c.getNro_Doc());
        String tipo = limpio(c.getTipo_documento());
        if (FacturacionDAO.DOC_NOTA_VENTA.equals(limpio(c.getTipoDoc())) || tipo.equals("NOTA DE VENTA")) {
            return "Nota de venta " + limpio(c.getPedido_ID());
        }
        return (tipo.isEmpty() ? "Comprobante" : capitalizar(tipo)) + " " + serie + (nro.isEmpty() ? "" : "-" + nro);
    }

    private static String capitalizar(String s) {
        String t = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private Map<String, Object> ok(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("mensaje", mensaje);
        return r;
    }

    private Map<String, Object> error(Exception ex) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", false);
        String msg = ex.getMessage();
        r.put("mensaje", msg == null ? "Ocurrió un error inesperado." : msg);
        if (!(ex instanceof IllegalArgumentException)) ex.printStackTrace();
        return r;
    }

    private void responder(Map<String, Object> r) {
        try {
            resultado = JSON.writeValueAsString(r);
        } catch (Exception ex) {
            resultado = "{\"ok\":false,\"mensaje\":\"No se pudo preparar la respuesta.\"}";
        }
    }

    private static UsuarioDTO usuario() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioAutenticado");
        return u instanceof UsuarioDTO ? (UsuarioDTO) u : null;
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private static String recortar(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static double redondear(double n) { return Math.round(n * 100.0) / 100.0; }
    private static String formato(double n) { return String.format(Locale.US, "%.2f", n); }
    private static String limpio(String s) { return s == null ? "" : s.trim(); }
    private static boolean vacio(String s) { return s == null || s.trim().isEmpty(); }

    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }
    public String getResultado() { return resultado; }
}
