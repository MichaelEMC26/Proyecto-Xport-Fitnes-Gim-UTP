package com.registro.facade;

import com.registro.config.ConexionBD;
import com.registro.dao.FacturacionDAO;
import com.registro.dto.FacturacionDTO;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Capa de negocio de Facturación (Punto de Venta).
 *
 * Grabar, modificar y anular una venta se hacen en UNA transacción (una sola conexión con
 * autoCommit = false): si cualquier paso falla se hace rollback y no queda nada a medias
 * (cabecera sin detalle, stock descontado sin venta, número de comprobante usado, etc.).
 * Es lo mismo que hace frm_Facturacion con SqlTransaction.
 *
 * @author Ing
 */
public class FacturacionFACADE {

    FacturacionDAO dao = new FacturacionDAO();

    /* ============================ Consultas ============================ */

    /** Formas de pago activas (sp_ListaFormasPagoActivo). */
    public List<FacturacionDTO> ListaFormasPagoActivoF() { return dao.ListaFormasPagoActivo(); }

    public List<FacturacionDTO> Lista_TiposDocF() { return dao.Lista_TiposDoc(); }

    public List<FacturacionDTO> ListaConfiguracionSistemaF() { return dao.ListaConfiguracionSistema(); }

    public List<FacturacionDTO> ListaAperturaCierreCajaAbiertoF(String codEmp) { return dao.ListaAperturaCierreCajaAbierto(codEmp); }

    public FacturacionDTO ultimaVentaTurnoF(String codEmp, LocalDate fechaTurno, String turno) {
        return dao.ultimaVentaTurno(codEmp, fechaTurno, turno).orElse(null);
    }

    public FacturacionDTO autogenera_cod_BoletaVentaF() { return dao.autogenera_cod_BoletaVenta().orElse(null); }

    public FacturacionDTO numero_SerieBoletaF() { return dao.numero_SerieBoleta().orElse(null); }

    public FacturacionDTO ConfiguracionSeries_SiguienteF(String codEmp, String documentoId) {
        return dao.ConfiguracionSeries_Siguiente(codEmp, documentoId).orElse(null);
    }

    public FacturacionDTO lista_ClienteXcodF(String clienteId) { return dao.lista_ClienteXcod(clienteId).orElse(null); }

    public List<FacturacionDTO> Lista_ProductosGral_ActivosXNomF(String codEmp, String nombreProducto) {
        return dao.Lista_ProductosGral_ActivosXNom(codEmp, nombreProducto);
    }

    public List<FacturacionDTO> Lista_ProductosGral_ActivosXCodBarraF(String codEmp, String codigoBarra) {
        return dao.Lista_ProductosGral_ActivosXCodBarra(codEmp, codigoBarra);
    }

    public FacturacionDTO listaTipoProductoXCodigoF(String codEmp, String productoId) {
        return dao.listaTipoProductoXCodigo(codEmp, productoId).orElse(null);
    }

    public boolean ListaOrdenPedidoImpresoXPedF(String pedidoId) { return dao.ListaOrdenPedidoImpresoXPed(pedidoId); }

    public List<FacturacionDTO> ListaVentasTurnoF(String codEmp, LocalDate fechaTurno, String turno) {
        return dao.ListaVentasTurno(codEmp, fechaTurno, turno);
    }

    public FacturacionDTO ListaVentaXPedidoF(String codEmp, String pedidoId) { return dao.ListaVentaXPedido(codEmp, pedidoId); }

    public List<FacturacionDTO> ListaDetalleVentaXPedidoF(String pedidoId) { return dao.ListaDetalleVentaXPedido(pedidoId); }

    public List<FacturacionDTO> Reporte_TicketCierreCajaF(String codEmp, int idAperturaCierre) {
        return dao.Reporte_TicketCierreCaja(codEmp, idAperturaCierre);
    }

    /* ============================ TRANSACCIONES ============================ */

    /**
     * Graba una venta nueva (Inserta_Venta del WinForm).
     *
     * @param cab      cabecera: TipoDoc, cliente, totales, CondPago, FormaPago, IDFormaPago, CodEmp, CodPer,
     *                 CodUser, FechaTurno, Turno y, si es al crédito, Monto / NroDias / FechaVenc.
     * @param detalles productos (Producto_ID, Descripcion, PrecioCosto, PrecioUnitario, Cantidad, Importe).
     * @param pagos    formas de pago (IDFormaPago, FormaPago, MontoParcial, PorcInc, MontoInc, MontoReal, IDBanco, NroOperacion).
     * @return la misma cabecera con Pedido_ID, Venta_id, NroSerie y Nro_Doc con los que se grabó.
     */
    public FacturacionDTO RegistrarVentaF(FacturacionDTO cab, List<FacturacionDTO> detalles, List<FacturacionDTO> pagos) {
        validar(detalles, pagos);
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            String tipoDoc = cab.getTipoDoc();
            cab.setTipo_documento(FacturacionDAO.nombreTipoDocumento(tipoDoc));

            // 1. Números: pedido y comprobante (se reservan dentro de la transacción)
            cab.setPedido_ID(dao.InsertaNumeracion(cn));
            cab.setVenta_id(dao.InsertaNumeracionComp(cn, tipoDoc));

            // 2. Serie y número: boleta y factura desde ConfiguracionSeries (leídos de nuevo al grabar)
            boolean usaSerie = FacturacionDAO.usaSerie(tipoDoc);
            if (usaSerie) {
                FacturacionDTO serie = dao.ConfiguracionSeries_Siguiente(cn, cab.getCodEmp(), tipoDoc)
                        .orElseThrow(() -> new IllegalStateException(
                                "No hay serie configurada en ConfiguracionSeries para " + cab.getTipo_documento() + "."));
                cab.setNroSerie(serie.getNroSerie());
                cab.setNro_Doc(serie.getNroDocumento());
            } else {
                cab.setNroSerie(dao.numero_SerieBoleta(cn));
                cab.setNro_Doc(cab.getVenta_id());     // la nota de venta usa el Pedido_ID (lo pone el procedimiento)
            }

            // 3. Cliente (solo si no existe), cabecera y correlativo de la serie
            dao.RegistraClienteSiNoExiste(cn, cab);
            dao.Insert_CabeceraVentas(cn, cab);
            if (usaSerie) {
                dao.ConfiguracionSeries_Actualiza(cn, cab.getCodEmp(), tipoDoc, cab.getNroSerie(), cab.getNro_Doc());
            }

            // 4. Detalle (descuenta stock) y formas de pago
            dao.Insert_Detalle_Ventas(cn, cab, detalles);
            dao.ReemplazaDetalleFormasPago(cn, cab, pagos);

            // 5. Comprobante electrónico (boleta y factura) y cuenta por cobrar (crédito)
            if (usaSerie) dao.InsertCont_CabCompVentas(cn, cab);
            if (esCredito(cab)) dao.InsertCtasXCobrar(cn, cab);

            cn.commit();
            return cab;
        } catch (Exception ex) {
            deshacer(cn, ex);
            throw new IllegalStateException("No se pudo grabar la venta: " + mensaje(ex), ex);
        } finally {
            cerrar(cn);
        }
    }

    /**
     * Modifica una venta (Update_Venta del WinForm): cliente, productos, totales, formas de pago y crédito.
     * El tipo de documento, la serie, el número y la fecha no cambian.
     */
    public FacturacionDTO ModificarVentaF(FacturacionDTO cab, List<FacturacionDTO> detalles, List<FacturacionDTO> pagos) {
        validar(detalles, pagos);
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            FacturacionDTO orig = ventaEditable(cn, cab.getCodEmp(), cab.getPedido_ID(), "modificar");

            // Se conservan los datos del comprobante original
            cab.setTipo_documento(orig.getTipo_documento());
            cab.setTipoDoc(orig.getTipoDoc());
            cab.setVenta_id(orig.getVenta_id());
            cab.setNroSerie(orig.getNroSerie());
            cab.setNro_Doc(orig.getNro_Doc());
            cab.setFecha_Pedido(orig.getFecha_Pedido());
            cab.setFechaTurno(orig.getFechaTurno());
            cab.setTurno(orig.getTurno());

            dao.RegistraClienteSiNoExiste(cn, cab);
            dao.Update_CabeceraVentas(cn, cab);

            // Detalle: devuelve el stock del detalle anterior, lo borra y graba el nuevo
            dao.RevierteDetalleVenta(cn, cab);
            dao.Insert_Detalle_Ventas(cn, cab, detalles);
            dao.ReemplazaDetalleFormasPago(cn, cab, pagos);

            // Crédito: crea / actualiza la cuenta por cobrar, o la quita si ahora es al contado
            if (esCredito(cab)) dao.InsertCtasXCobrar(cn, cab);
            else dao.EliminaCtasXCobrar(cn, cab.getCodEmp(), cab.getPedido_ID());

            cn.commit();
            return cab;
        } catch (Exception ex) {
            deshacer(cn, ex);
            throw new IllegalStateException("No se pudo modificar la venta: " + mensaje(ex), ex);
        } finally {
            cerrar(cn);
        }
    }

    /**
     * Anula (elimina) una venta (Eliminar del WinForm, sp_EliminaVenta): devuelve el stock y borra la venta,
     * su comprobante, sus formas de pago y su cuenta por cobrar.
     */
    public FacturacionDTO AnularVentaF(String codEmp, String pedidoId) {
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            FacturacionDTO orig = ventaEditable(cn, codEmp, pedidoId, "eliminar");
            orig.setCodEmp(codEmp);

            dao.EliminaVenta(cn, orig);
            dao.EliminaCtasXCobrar(cn, codEmp, pedidoId);

            cn.commit();
            return orig;
        } catch (Exception ex) {
            deshacer(cn, ex);
            throw new IllegalStateException("No se pudo eliminar la venta: " + mensaje(ex), ex);
        } finally {
            cerrar(cn);
        }
    }

    /* ============================ Ayudas ============================ */

    /** Venta que se puede modificar o eliminar: existe, no está impresa y su crédito no tiene pagos. */
    private FacturacionDTO ventaEditable(Connection cn, String codEmp, String pedidoId, String accion) throws SQLException {
        FacturacionDTO orig = dao.ListaVentaXPedido(cn, codEmp, pedidoId);
        if (orig == null) {
            throw new IllegalStateException("La venta N° " + pedidoId + " no existe.");
        }
        boolean esNota = FacturacionDAO.DOC_NOTA_VENTA.equals(orig.getTipoDoc() == null ? "" : orig.getTipoDoc().trim());
        if (!esNota && "SI".equalsIgnoreCase(orig.getImpreso())) {
            throw new IllegalStateException("El comprobante ya se imprimió; no se puede " + accion + ".");
        }
        double[] credito = dao.MontoYSaldoCredito(cn, codEmp, pedidoId);
        if (credito != null && credito[1] < credito[0] - 0.005) {
            throw new IllegalStateException("La venta tiene pagos a cuenta de su crédito; no se puede " + accion + ".");
        }
        return orig;
    }

    private static boolean esCredito(FacturacionDTO cab) {
        return "CREDITO".equalsIgnoreCase(cab.getCondCli() == null ? "" : cab.getCondCli().trim()) && cab.getMonto() > 0;
    }

    private static void validar(List<FacturacionDTO> detalles, List<FacturacionDTO> pagos) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("No ha ingresado el detalle; imposible grabar la venta.");
        }
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException("No se registró la forma de pago; la venta no se grabó.");
        }
    }

    /** Rollback sin tapar el error original (si el procedimiento ya revirtió, el rollback falla). */
    private static void deshacer(Connection cn, Exception ex) {
        if (cn == null) return;
        try {
            cn.rollback();
        } catch (SQLException rollbackEx) {
            ex.addSuppressed(rollbackEx);
        }
    }

    private static void cerrar(Connection cn) {
        if (cn == null) return;
        try {
            cn.setAutoCommit(true);
        } catch (SQLException ignorada) {
            // la conexión se cierra igual
        }
        try {
            cn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static String mensaje(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
