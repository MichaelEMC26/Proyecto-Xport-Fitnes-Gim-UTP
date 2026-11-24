package com.registro.facade;

import com.registro.config.ConexionBD;
import com.registro.dao.PuntoVentaDAO;
import com.registro.dto.PuntoVentaDTO;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**

 * @author Ing
 */
public class PuntoVentaFACADE {

    PuntoVentaDAO dao = new PuntoVentaDAO();

    /* ============================ Consultas ============================ */

    /** Formas de pago activas (sp_ListaFormasPagoActivo). */
    public List<PuntoVentaDTO> ListaFormasPagoActivoF() { return dao.ListaFormasPagoActivo(); }

    public List<PuntoVentaDTO> Lista_TiposDocF() { return dao.Lista_TiposDoc(); }

    public List<PuntoVentaDTO> ListaConfiguracionSistemaF() { return dao.ListaConfiguracionSistema(); }

    public List<PuntoVentaDTO> ListaAperturaCierreCajaAbiertoF(String codEmp) { return dao.ListaAperturaCierreCajaAbierto(codEmp); }

    public PuntoVentaDTO ultimaVentaTurnoF(String codEmp, LocalDate fechaTurno, String turno) {
        return dao.ultimaVentaTurno(codEmp, fechaTurno, turno).orElse(null);
    }

    public PuntoVentaDTO autogenera_cod_BoletaVentaF() { return dao.autogenera_cod_BoletaVenta().orElse(null); }

    public PuntoVentaDTO numero_SerieBoletaF() { return dao.numero_SerieBoleta().orElse(null); }

    public PuntoVentaDTO ConfiguracionSeries_SiguienteF(String codEmp, String documentoId) {
        return dao.ConfiguracionSeries_Siguiente(codEmp, documentoId).orElse(null);
    }

    public PuntoVentaDTO lista_ClienteXcodF(String clienteId) { return dao.lista_ClienteXcod(clienteId).orElse(null); }

    public List<PuntoVentaDTO> Lista_ProductosGral_ActivosXNomF(String codEmp, String nombreProducto) {
        return dao.Lista_ProductosGral_ActivosXNom(codEmp, nombreProducto);
    }

    public List<PuntoVentaDTO> Lista_ProductosGral_ActivosXCodBarraF(String codEmp, String codigoBarra) {
        return dao.Lista_ProductosGral_ActivosXCodBarra(codEmp, codigoBarra);
    }

    public PuntoVentaDTO listaTipoProductoXCodigoF(String codEmp, String productoId) {
        return dao.listaTipoProductoXCodigo(codEmp, productoId).orElse(null);
    }

    public boolean ListaOrdenPedidoImpresoXPedF(String pedidoId) { return dao.ListaOrdenPedidoImpresoXPed(pedidoId); }

    public List<PuntoVentaDTO> ListaVentasTurnoF(String codEmp, LocalDate fechaTurno, String turno) {
        return dao.ListaVentasTurno(codEmp, fechaTurno, turno);
    }

    public PuntoVentaDTO ListaVentaXPedidoF(String codEmp, String pedidoId) { return dao.ListaVentaXPedido(codEmp, pedidoId); }

    public List<PuntoVentaDTO> ListaDetalleVentaXPedidoF(String pedidoId) { return dao.ListaDetalleVentaXPedido(pedidoId); }

    public List<PuntoVentaDTO> Reporte_TicketCierreCajaF(String codEmp, int idAperturaCierre) {
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
    public PuntoVentaDTO RegistrarVentaF(PuntoVentaDTO cab, List<PuntoVentaDTO> detalles, List<PuntoVentaDTO> pagos) {
        validar(detalles, pagos);
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            String tipoDoc = cab.getTipoDoc();
            cab.setTipo_documento(PuntoVentaDAO.nombreTipoDocumento(tipoDoc));

            // 0. Membresías: la suscripción debe estar Pendiente ANTES de grabar (se bloquea su fila hasta el commit)
            verificarSuscripciones(cn, cab, detalles);

            // 1. Números: pedido y comprobante (se reservan dentro de la transacción)
            cab.setPedido_ID(dao.InsertaNumeracion(cn));
            cab.setVenta_id(dao.InsertaNumeracionComp(cn, tipoDoc));

            // 2. Serie y número: boleta y factura desde ConfiguracionSeries (leídos de nuevo al grabar)
            boolean usaSerie = PuntoVentaDAO.usaSerie(tipoDoc);
            if (usaSerie) {
                PuntoVentaDTO serie = dao.ConfiguracionSeries_Siguiente(cn, cab.getCodEmp(), tipoDoc)
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

            // 4b. Membresías: sp_Insert_Detalle_Ventas pasa a Efectiva la suscripción cuando la línea vendida es
            //     su plan (sql/membresia_pago_en_detalle.sql). El programa no la actualiza, así no se hace dos veces.

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
    public PuntoVentaDTO ModificarVentaF(PuntoVentaDTO cab, List<PuntoVentaDTO> detalles, List<PuntoVentaDTO> pagos) {
        validar(detalles, pagos);
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            PuntoVentaDTO orig = ventaEditable(cn, cab.getCodEmp(), cab.getPedido_ID(), "modificar");

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

            // Detalle: devuelve el stock del detalle anterior, lo borra y graba el nuevo.
            // La suscripción cobrada en esta venta sigue Efectiva: solo sp_Insert_CabeceraVentas la marca
            // (sp_Update_CabeceraVentas no), así que aquí no se toca su estado.
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
    public PuntoVentaDTO AnularVentaF(String codEmp, String pedidoId) {
        Connection cn = null;
        try {
            cn = ConexionBD.obtenerConexion();
            cn.setAutoCommit(false);

            PuntoVentaDTO orig = ventaEditable(cn, codEmp, pedidoId, "eliminar");
            orig.setCodEmp(codEmp);

            // Antes de borrar el detalle: la membresía cobrada en esta venta vuelve a Pendiente de pago
            dao.RevierteSuscripcionesVenta(cn, pedidoId);
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
    private PuntoVentaDTO ventaEditable(Connection cn, String codEmp, String pedidoId, String accion) throws SQLException {
        PuntoVentaDTO orig = dao.ListaVentaXPedido(cn, codEmp, pedidoId);
        if (orig == null) {
            throw new IllegalStateException("La venta N° " + pedidoId + " no existe.");
        }
        boolean esNota = PuntoVentaDAO.DOC_NOTA_VENTA.equals(orig.getTipoDoc() == null ? "" : orig.getTipoDoc().trim());
        if (!esNota && "SI".equalsIgnoreCase(orig.getImpreso())) {
            throw new IllegalStateException("El comprobante ya se imprimió; no se puede " + accion + ".");
        }
        double[] credito = dao.MontoYSaldoCredito(cn, codEmp, pedidoId);
        if (credito != null && credito[1] < credito[0] - 0.005) {
            throw new IllegalStateException("La venta tiene pagos a cuenta de su crédito; no se puede " + accion + ".");
        }
        return orig;
    }

    /**
     * Antes de grabar: la suscripción debe cumplir lo mismo que pide sp_Insert_Detalle_Ventas para pasarla a
     * Efectiva; si no, la venta se grabaría y la suscripción quedaría Pendiente sin aviso:
     *  - existir, ser del cliente de la venta, Condicion = 'ACTIVA' y Estado = 'Pendiente';
     *  - ser la ÚNICA ACTIVA / Pendiente de ese plan para el socio (el procedimiento marca la más antigua).
     * Se lee con bloqueo de fila: dos cajas no cobran la misma suscripción (la segunda espera y la ve Efectiva).
     */
    private void verificarSuscripciones(Connection cn, PuntoVentaDTO cab, List<PuntoVentaDTO> detalles) throws SQLException {
        for (PuntoVentaDTO d : detalles) {
            String nro = PuntoVentaDAO.nroSuscripcion(d);
            if (nro.isEmpty()) continue;
            String[] s = dao.EstadoSuscripcionBloqueo(cn, nro);
            if (s == null) {
                throw new IllegalArgumentException("La suscripción N° " + nro + " no existe.");
            }
            String estado = s[0], condicion = s[1], cliente = s[2];
            int pendientes = Integer.parseInt(s[3]);
            if (!estado.equalsIgnoreCase("Pendiente")) {
                throw new IllegalArgumentException("La suscripción N° " + nro + " no está pendiente de pago (estado actual: "
                        + (estado.isEmpty() ? "sin estado" : estado) + ").");
            }
            if (!condicion.equalsIgnoreCase("ACTIVA")) {
                throw new IllegalArgumentException("La suscripción N° " + nro + " no está ACTIVA (condición: "
                        + (condicion.isEmpty() ? "sin condición" : condicion) + ").");
            }
            String clienteVenta = cab.getCliente_ID() == null ? "" : cab.getCliente_ID().trim();
            if (!cliente.equals(clienteVenta)) {
                throw new IllegalArgumentException("La suscripción N° " + nro + " es del socio " + cliente
                        + " y la venta es para " + clienteVenta + ".");
            }
            if (pendientes > 1) {
                throw new IllegalArgumentException("El socio " + cliente + " tiene " + pendientes
                        + " suscripciones pendientes de pago del mismo plan. Anule las que no correspondan y deje solo una para cobrarla.");
            }
        }
    }

    private static boolean esCredito(PuntoVentaDTO cab) {
        return "CREDITO".equalsIgnoreCase(cab.getCondCli() == null ? "" : cab.getCondCli().trim()) && cab.getMonto() > 0;
    }

    private static void validar(List<PuntoVentaDTO> detalles, List<PuntoVentaDTO> pagos) {
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
