/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.facade;
import com.registro.dao.AsistenciaDAO;
import com.registro.dao.ClienteDAO;
import com.registro.dto.AsistenciaDTO;
import com.registro.dto.ClienteDTO;
import com.registro.dto.ResultadoAsistenciaDTO;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ing
 */
public class AsistenciaFACADE {
    
    private  AsistenciaDAO  dao = new AsistenciaDAO();
    
    public boolean InsertDetRegistroAsistenciasF(AsistenciaDTO asistencia)
    {
        return dao.InsertDetRegistroAsistencias(asistencia);
    }
    
    public List<AsistenciaDTO> ListaDetRegistroAsistenciasXRegistroF(String NroSuscripcion_id, String Cliente_ID)
    {
        return dao.ListaDetRegistroAsistenciasXRegistro(NroSuscripcion_id, Cliente_ID);
    }
    
    public AsistenciaDTO ListaDetRegistroAsistenciasXRegistroXFechaIngF(String NroSuscripcion_id, String Cliente_ID, LocalDate FechaIngreso)
    {
        return dao.ListaDetRegistroAsistenciasXRegistroXFechaIng(NroSuscripcion_id,Cliente_ID,FechaIngreso);
    }
    public boolean InsertaAlertaF(AsistenciaDTO asistencia)
    {
        return dao.InsertaAlerta(asistencia);
    }
    public AsistenciaDTO lista_ClienteXcodF(String Cliente_ID)
    {
        return dao.lista_ClienteXcod(Cliente_ID);
    }
    
    public List<AsistenciaDTO> ListaNroSuscripciones_MembresiaXAlumnoF(String CodEmp, String Cliente_ID)
    {
        return dao.ListaNroSuscripciones_MembresiaXAlumno(CodEmp, Cliente_ID);
    }
    
    public AsistenciaDTO Listasuscripciones_MembresiaXAlumnoXRegistroF(String CodEmp, String Cliente_ID, String NroSuscripcion_id)
    {
        return dao.Listasuscripciones_MembresiaXAlumnoXRegistro(CodEmp,Cliente_ID,NroSuscripcion_id);
    }
    public AsistenciaDTO ListaCantMaximaRegistroAsistenciasXAlumnoF(String CodEmp, String Cliente_ID, String NroSuscripcion_id)
    {
        return dao.ListaCantMaximaRegistroAsistenciasXAlumno(CodEmp,Cliente_ID,NroSuscripcion_id);
    }  
    public List<AsistenciaDTO> lista_Codigoclientes_ActivosF()
    {
        return dao.lista_Codigoclientes_Activos();
    }

    // =====================================================================
    // Registro de ingreso (igual que Ven_FacturaN.RegistrarAsistenciaN de SisdataGimnasio)
    // =====================================================================

    private final ClienteDAO clienteDao = new ClienteDAO();

    /** Turno de la caja abierta o "0" si no hay caja abierta. */
    public String TurnoCajaAbiertaF(String CodEmp)
    {
        return dao.TurnoCajaAbierta(CodEmp);
    }

    /** Deuda pendiente del pedido de la membresía (Total = monto, FechaFin = vencimiento); null si no hay. */
    public AsistenciaDTO DeudaPedidoF(String CodEmp, String Cliente_ID, String Pedido_ID)
    {
        return dao.DeudaPedido(CodEmp, Cliente_ID, Pedido_ID);
    }

    /**
     * Valida y registra el ingreso del socio. Toda la regla está aquí; el bean solo muestra el resultado.
     * 1 cliente, 2 suscripción (no Pendiente) y fecha fin, 3 vencida (inserta alerta), 4 ya registró hoy,
     * 5 límite de asistencias, 6 grabar y refrescar.
     */
    public ResultadoAsistenciaDTO RegistrarAsistenciaF(String CodEmp, String Cliente_ID, String Turno, LocalDateTime ahora)
    {
        ResultadoAsistenciaDTO resultado = new ResultadoAsistenciaDTO();
        resultado.setAsistenciasUsadas(-1);
        LocalDate hoy = ahora.toLocalDate();

        // 1. Cliente
        ClienteDTO cliente = clienteDao.ListaClienteXCod(Cliente_ID);
        if (cliente == null) {
            resultado.setTipoMensaje("NO_EXISTE");
            resultado.setMensaje("No existe el socio con código " + Cliente_ID + ".");
            return resultado;
        }
        resultado.setDatosCliente(cliente);

        // 2. Suscripción: sp_ListaNroSuscripciones_MembresiaXAlumno (Estado <> 'Anulada', Condicion = 'ACTIVA').
        //    Se usa la primera que NO esté en estado "Pendiente"; si todas lo están, no puede ingresar.
        List<AsistenciaDTO> nros = dao.ListaNroSuscripciones_MembresiaXAlumno(CodEmp, Cliente_ID);
        if (nros == null || nros.isEmpty()) return sinMembresia(resultado);

        AsistenciaDTO elegida = null;
        for (AsistenciaDTO n : nros) {
            if (!esPendiente(n)) { elegida = n; break; }
        }
        boolean pendiente = elegida == null;
        if (pendiente) elegida = nros.get(0);   // para mostrar en pantalla el periodo de la suscripción pendiente

        String nro = elegida.getNroSuscripcion_id() == null ? "" : elegida.getNroSuscripcion_id().trim();
        AsistenciaDTO sus = dao.Listasuscripciones_MembresiaXAlumnoXRegistro(CodEmp, Cliente_ID, nro);
        if (sus == null) return sinMembresia(resultado);
        resultado.setDatosSuscripcion(sus);
        if (sus.getNroSuscripcion_id() != null && !sus.getNroSuscripcion_id().isEmpty()) nro = sus.getNroSuscripcion_id().trim();

        if (pendiente) {
            resultado.setTipoMensaje("PENDIENTE");
            resultado.setMensaje("La membresía N° " + nro + " está en estado Pendiente. "
                    + "No puede registrar su asistencia hasta cancelar el pago de la suscripción.");
            return resultado;
        }

        if (sus.getFechaFin() == null) {
            resultado.setTipoMensaje("SIN_FECHA");
            resultado.setMensaje("La membresía N° " + nro + " no tiene fecha de fin. Revise el registro de la membresía.");
            return resultado;
        }
        LocalDate fechaFin = sus.getFechaFin().toLocalDate();

        // Historial actual, para mostrarlo sin importar el resultado
        resultado.setHistorialAsistencias(historial(nro, Cliente_ID));

        // 3. Membresía vencida
        if (hoy.isAfter(fechaFin)) {
            AsistenciaDTO alerta = new AsistenciaDTO();
            alerta.setidAlerta(1);
            alerta.setNombre("Alerta");
            dao.InsertaAlerta(alerta);

            resultado.setTipoMensaje("VENCIDO");
            resultado.setMensaje("Su membresía venció el " + fechaFin.format(DD_MM_YYYY) + ". No puede ingresar hasta renovarla.");
            return resultado;
        }

        // 4. Ya registró hoy (antes que el límite, para dar el mensaje correcto)
        if (dao.ListaDetRegistroAsistenciasXRegistroXFechaIng(nro, Cliente_ID, hoy) != null) {
            leerLimite(CodEmp, Cliente_ID, nro, resultado);
            resultado.setTipoMensaje("DUPLICADO");
            resultado.setMensaje("Ya registró su asistencia hoy, no puede repetir.");
            return resultado;
        }

        // 5. Límite de asistencias de la membresía (0 o vacío = sin límite)
        leerLimite(CodEmp, Cliente_ID, nro, resultado);
        if (resultado.getAsistenciasPermitidas() > 0
                && resultado.getAsistenciasUsadas() >= resultado.getAsistenciasPermitidas()) {
            resultado.setTipoMensaje("LIMITE_EXCEDIDO");
            resultado.setMensaje("Ya completó las " + resultado.getAsistenciasPermitidas()
                    + " asistencias permitidas de su membresía.");
            return resultado;
        }

        // 6. Guardar y refrescar
        AsistenciaDTO a = new AsistenciaDTO();
        a.setNroSuscripcion_id(nro);
        a.setCliente_ID(Cliente_ID);
        a.setFechaHora(Timestamp.valueOf(ahora));
        a.setFechaFin(hoy.atStartOfDay());   // @FechaFin = FechaIngreso
        a.setTurno(Turno == null || Turno.trim().isEmpty() ? "M" : Turno.trim());

        if (!dao.InsertDetRegistroAsistencias(a)) {
            resultado.setTipoMensaje("ERROR");
            resultado.setMensaje("No se pudo registrar la asistencia. Intente nuevamente.");
            return resultado;
        }
        dao.GrabarHoraAsistencia(nro, Cliente_ID, Timestamp.valueOf(ahora));

        resultado.setHistorialAsistencias(historial(nro, Cliente_ID));
        leerLimite(CodEmp, Cliente_ID, nro, resultado);
        resultado.setExito(true);
        resultado.setTipoMensaje("OK");
        resultado.setMensaje("Asistencia registrada correctamente.");
        return resultado;
    }

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Estado de la suscripción = "Pendiente" (sin importar mayúsculas ni espacios). */
    private static boolean esPendiente(AsistenciaDTO suscripcion)
    {
        String estado = suscripcion.getEstado() == null ? "" : suscripcion.getEstado().trim();
        return estado.equalsIgnoreCase("Pendiente");
    }

    private static ResultadoAsistenciaDTO sinMembresia(ResultadoAsistenciaDTO r)
    {
        r.setTipoMensaje("NO_EXISTE");
        r.setMensaje("El socio no tiene una membresía registrada.");
        return r;
    }

    /** Historial con la hora de ingreso completada desde AsistenciaHora (CompletarHorasAsistencia). */
    private List<AsistenciaDTO> historial(String nro, String Cliente_ID)
    {
        List<AsistenciaDTO> lista = dao.ListaDetRegistroAsistenciasXRegistro(nro, Cliente_ID);
        Map<LocalDate, LocalDateTime> horas = null;
        for (AsistenciaDTO det : lista) {
            if (det.getHoraIngreso() != null && det.getHoraIngreso().toLocalTime().toSecondOfDay() != 0) continue;
            if (det.getFechaIngreso() == null) continue;
            if (horas == null) horas = dao.ListaHoraAsistencia(nro, Cliente_ID);
            LocalDateTime hora = horas.get(det.getFechaIngreso());
            if (hora != null) det.setHoraIngreso(hora);
        }
        return lista;
    }

    /** Máximo (columna Dias) y usadas (columna CANT); si no vienen, usadas = filas del historial. */
    private void leerLimite(String CodEmp, String Cliente_ID, String nro, ResultadoAsistenciaDTO r)
    {
        AsistenciaDTO dt = dao.ListaCantMaximaRegistroAsistenciasXAlumno(CodEmp, Cliente_ID, nro);
        int permitidas = 0;
        int usadas = -1;
        if (dt != null) {
            try {
                permitidas = dt.getDia() == null ? 0 : (int) Double.parseDouble(dt.getDia().trim());
            } catch (NumberFormatException ex) {
                permitidas = 0;
            }
            usadas = dt.getCANT();
        }
        if (usadas < 0 && r.getHistorialAsistencias() != null) usadas = r.getHistorialAsistencias().size();
        r.setAsistenciasPermitidas(permitidas);
        r.setAsistenciasUsadas(usadas);
    }
    
    public List<AsistenciaDTO> ListaDetRegistroAsistenciasXFechaXTurnoF(String CodEmp,LocalDateTime FechaIngreso,String Turno)
    {
        return dao.ListaDetRegistroAsistenciasXFechaXTurno(CodEmp,FechaIngreso,Turno);
    }

    /** Turnos de caja del día (combo de turnos de frm_ConsultaAsistenciaXDia): Turno y Nombre (NomTurno). */
    public List<AsistenciaDTO> ListaTurnosXFechaF(String CodEmp, LocalDate Fecha)
    {
        return dao.ListaTurnosXFecha(CodEmp, Fecha);
    }
}
