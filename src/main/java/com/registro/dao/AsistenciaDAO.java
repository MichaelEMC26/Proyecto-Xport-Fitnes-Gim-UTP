/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import com.registro.config.ConexionBD;
import com.registro.dto.AsistenciaDTO;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 *
 * @author Ing
 */
public class AsistenciaDAO {

    /** Formato que devuelve CONVERT(CHAR(10), fecha, 101) en SQL Server: MM/dd/yyyy */
    private static final DateTimeFormatter FORMATO_FECHA_101 = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    /** Formato que devuelve CONVERT(CHAR(10), fecha, 108) en SQL Server: HH:mm:ss */
    private static final DateTimeFormatter FORMATO_HORA_108 = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * InsertDetRegistroAsistencias
     * El procedimiento usa SET NOCOUNT ON y devuelve RETURN 0 si todo salió bien,
     * por eso se lee el código de retorno en lugar de las filas afectadas.
     */
    public boolean InsertDetRegistroAsistencias(AsistenciaDTO asistencia) {
        boolean ResultadoExitoso = false;
        String sql = "{? = call sp_InsertDetRegistroAsistencias(?,?,?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, asistencia.getNroSuscripcion_id());
            cs.setString(3, asistencia.getCliente_ID());

            if (asistencia.getFechaHora() != null) {
                cs.setTimestamp(4, asistencia.getFechaHora());
            } else {
                cs.setNull(4, Types.TIMESTAMP);
            }

            if (asistencia.getFechaFin() != null) {
                cs.setTimestamp(5, Timestamp.valueOf(asistencia.getFechaFin()));
            } else {
                cs.setNull(5, Types.TIMESTAMP);
            }

            cs.setString(6, asistencia.getTurno());

            cs.execute();
            ResultadoExitoso = cs.getInt(1) == 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return ResultadoExitoso;
    }

    public List<AsistenciaDTO> ListaDetRegistroAsistenciasXRegistro(String NroSuscripcion_id, String Cliente_ID) {
        List<AsistenciaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaDetRegistroAsistenciasXRegistro(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, NroSuscripcion_id);
            st.setString(2, Cliente_ID);

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    AsistenciaDTO det = new AsistenciaDTO();
                    String dia = texto(rs, "Dia");
                    det.setDia(dia);
                    try {
                        det.setIDSecuencia(dia == null ? 0 : Integer.parseInt(dia));
                    } catch (NumberFormatException ex) {
                        det.setIDSecuencia(0); 
                    }

                    LocalDateTime fechaCol = leerFechaHora(rs, "FechaIngreso");
                    LocalDateTime horaCol = leerFechaHora(rs, "HoraIngreso");

                    LocalDate fecha = fechaCol != null ? fechaCol.toLocalDate()
                            : (horaCol != null && horaCol.toLocalDate().getYear() > 1900 ? horaCol.toLocalDate() : null);
                    det.setFechaIngreso(fecha);

                    // Hora: la de HoraIngreso (texto HH:mm:ss viene con fecha 1900-01-01: se le pone la fecha real)
                    if (horaCol != null && horaCol.toLocalTime().toSecondOfDay() != 0) {
                        LocalDateTime fechaHora = fecha != null ? fecha.atTime(horaCol.toLocalTime()) : horaCol;
                        det.setHoraIngreso(fechaHora);
                        det.setFechaHora(Timestamp.valueOf(fechaHora));
                    }
                    lista.add(det);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lista;
    }

    /**
     * ListaDetRegistroAsistenciasXRegistroXFechaIng
     */
    public AsistenciaDTO ListaDetRegistroAsistenciasXRegistroXFechaIng(String NroSuscripcion_id, String Cliente_ID, LocalDate FechaIngreso) {
        AsistenciaDTO ListaDet = null;
        String sql = "{call sp_ListaDetRegistroAsistenciasXRegistroXFechaIng(?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, NroSuscripcion_id);
            st.setString(2, Cliente_ID);
            if (FechaIngreso != null) {
                st.setDate(3, Date.valueOf(FechaIngreso));
            } else {
                st.setNull(3, Types.DATE);
            }

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    ListaDet = new AsistenciaDTO();
                    ListaDet.setNroSuscripcion_id(rs.getString("NroSuscripcion_id"));
                    ListaDet.setCliente_ID(rs.getString("Cliente_ID"));
                    ListaDet.setIDSecuencia(rs.getInt("IDSecuencia"));
                    ListaDet.setHoraIngreso(aLocalDateTime(rs.getTimestamp("HoraIngreso")));
                    ListaDet.setFechaIngreso(aLocalDate(rs.getDate("FechaIngreso")));
                    ListaDet.setTurno(rs.getString("Turno"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return ListaDet;
    }

    /**
     * InsertaAlerta

     */
    public boolean InsertaAlerta(AsistenciaDTO asistencia) {
        boolean ResultadoExitoso = false;
        String sql = "{? = call sp_InsertaAlerta(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, asistencia.getidAlerta());
            cs.setString(3, asistencia.getNombre());

            cs.execute();
            ResultadoExitoso = cs.getInt(1) == 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return ResultadoExitoso;
    }

    /**
     * lista_ClienteXcod
     */
    public AsistenciaDTO lista_ClienteXcod(String Cliente_ID) {
        AsistenciaDTO lista_Cliente = null;
        String sql = "{call sp_lista_ClienteXcod(?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, Cliente_ID);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    lista_Cliente = new AsistenciaDTO();
                    lista_Cliente.setCliente_ID(rs.getString("Cliente_ID"));
                    lista_Cliente.setRazonSocial(rs.getString("RazonSocial"));
                    lista_Cliente.setDireccion(rs.getString("Direccion"));
                    lista_Cliente.setTelefono(rs.getString("Telefono"));
                    lista_Cliente.setEstado(rs.getString("Estado"));
                    lista_Cliente.setTelefono2(rs.getString("Telefono2"));
                    lista_Cliente.setCorreo(rs.getString("Correo"));
                    lista_Cliente.setSexo(rs.getString("Sexo"));
                    lista_Cliente.setRucDNI(rs.getString("RucDNI"));
                    lista_Cliente.setTipoId(rs.getString("TipoId"));
                    lista_Cliente.setFoto(rs.getString("Foto"));
                    lista_Cliente.setContacto(rs.getString("Contacto"));
                    lista_Cliente.setTelefonoCont(rs.getString("TelefonoCont"));
                    lista_Cliente.setHuellaDactilar(rs.getBytes("HuellaDactilar"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lista_Cliente;
    }

    /**
     * ListaNroSuscripciones_MembresiaXAlumno
     * Un alumno puede tener varias suscripciones activas, por eso devuelve lista.
     */
    public List<AsistenciaDTO> ListaNroSuscripciones_MembresiaXAlumno(String CodEmp, String Cliente_ID) {
        List<AsistenciaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaNroSuscripciones_MembresiaXAlumno(?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, CodEmp);
            st.setString(2, Cliente_ID);

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    AsistenciaDTO item = new AsistenciaDTO();
                    item.setNroSuscripcion_id(rs.getString("NroSuscripcion_id"));
                    item.setEstado(rs.getString("Estado"));
                    item.setCondicion(rs.getString("Condicion"));
                    item.setObservacion(rs.getString("Observacion"));
                    item.setCliente_ID(Cliente_ID);
                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lista;
    }

    /**
     * Listasuscripciones_MembresiaXAlumnoXRegistro
     */
    public AsistenciaDTO Listasuscripciones_MembresiaXAlumnoXRegistro(String CodEmp, String Cliente_ID, String NroSuscripcion_id) {
        AsistenciaDTO listar = null;
        String sql = "{call sp_Listasuscripciones_MembresiaXAlumnoXRegistro(?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, CodEmp);
            st.setString(2, Cliente_ID);
            st.setString(3, NroSuscripcion_id);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    // Las columnas se leen por nombre y son opcionales (el SP cambia entre versiones)
                    listar = new AsistenciaDTO();
                    String nro = texto(rs, "NroSuscripcion_id");
                    listar.setNroSuscripcion_id(nro != null ? nro : texto(rs, "NroRegistro"));
                    listar.setCodSocio(texto(rs, "CodSocio"));
                    listar.setSocioCliente(texto(rs, "SocioCliente"));
                    listar.setCodigoM(texto(rs, "CodigoM"));
                    listar.setMembresia(texto(rs, "Membresia"));
                    listar.setDuracion(entero(rs, "Duracion"));
                    listar.setFrecuencia(texto(rs, "Frecuencia"));
                    Timestamp ini = fecha(rs, "FechaInicio");
                    listar.setFechaInicio(ini != null ? ini.toLocalDateTime().toLocalDate() : null);
                    listar.setFechaFin(aLocalDateTime(fecha(rs, "FechaFin")));
                    listar.setEstado(texto(rs, "Estado"));
                    listar.setCondicion(texto(rs, "Condicion"));
                    listar.setObservacion(texto(rs, "Observacion"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return listar;
    }

    /**
     * ListaCantMaximaRegistroAsistenciasXAlumno
     */
    public AsistenciaDTO ListaCantMaximaRegistroAsistenciasXAlumno(String CodEmp, String Cliente_ID, String NroSuscripcion_id) {
        AsistenciaDTO listar = null;
        String sql = "{call sp_ListaCantMaximaRegistroAsistenciasXAlumno(?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {

            st.setString(1, CodEmp);
            st.setString(2, Cliente_ID);
            st.setString(3, NroSuscripcion_id);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    // Dias = máximo permitido (0 = sin límite); CANT = usadas (-1 = no viene)
                    listar = new AsistenciaDTO();
                    listar.setDia(texto(rs, "Dias"));
                    listar.setCANT(tieneColumna(rs, "CANT") ? rs.getInt("CANT") : -1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return listar;
    }

    /**
     * lista_Codigoclientes_Activos

     */
    public List<AsistenciaDTO> lista_Codigoclientes_Activos() {
        List<AsistenciaDTO> lista = new ArrayList<>();
        String sql = "{call sp_lista_Codigoclientes_Activos}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                AsistenciaDTO item = new AsistenciaDTO();
                item.setCliente_ID(rs.getString("Cliente_ID"));
                lista.add(item);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lista;
    }

    /**
     * ListaAperturaCierreCajaAbierto: turno de la caja abierta (columna 18), o "0" si no hay caja abierta.
     */
    public String TurnoCajaAbierta(String CodEmp) {
        String sql = "{call sp_ListaAperturaCierreCajaAbierto(?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {
            st.setString(1, CodEmp);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next() && rs.getMetaData().getColumnCount() >= 18) {
                    String turno = rs.getString(18);
                    return turno == null ? "0" : turno.trim();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "0";
    }

    /**

     */
    public void GrabarHoraAsistencia(String NroSuscripcion_id, String Cliente_ID, Timestamp FechaHora) {
        String sql = "{call sp_AsistenciaHora_Grabar(?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, NroSuscripcion_id == null ? "" : NroSuscripcion_id.trim());
            cs.setString(2, Cliente_ID == null ? "" : Cliente_ID.trim());
            cs.setTimestamp(3, FechaHora);
            cs.execute();
        } catch (SQLException ex) {
            // sin sql/asistencia_hora.sql: la asistencia queda grabada, solo sin hora
            System.err.println("[AsistenciaDAO] sp_AsistenciaHora_Grabar: " + ex.getMessage()
                    + " (ejecute sql/asistencia_hora.sql)");
        }
    }

    /** sp_AsistenciaHora_Lista: hora de ingreso por fecha (un ingreso por día). Vacío si el SP no existe. */
    public Map<LocalDate, LocalDateTime> ListaHoraAsistencia(String NroSuscripcion_id, String Cliente_ID) {
        Map<LocalDate, LocalDateTime> horas = new HashMap<>();
        String sql = "{call sp_AsistenciaHora_Lista(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {
            st.setString(1, NroSuscripcion_id == null ? "" : NroSuscripcion_id.trim());
            st.setString(2, Cliente_ID == null ? "" : Cliente_ID.trim());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Timestamp dia = fecha(rs, "FechaIngreso");
                    Timestamp hora = fecha(rs, "HoraIngreso");
                    if (dia != null && hora != null) {
                        horas.put(dia.toLocalDateTime().toLocalDate(), hora.toLocalDateTime());
                    }
                }
            }
        } catch (SQLException ex) {
            // sin sql/asistencia_hora.sql: la lista sale sin hora
            System.err.println("[AsistenciaDAO] sp_AsistenciaHora_Lista: " + ex.getMessage()
                    + " (ejecute sql/asistencia_hora.sql)");
        }
        return horas;
    }

    /**
     * ListaCtasXCobrarXClienteXPedido: deuda pendiente del pedido de la membresía.

     */
    public AsistenciaDTO DeudaPedido(String CodEmp, String Cliente_ID, String Pedido_ID) {
        String sql = "{call sp_ListaCtasXCobrarXClienteXPedido(?,?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement st = cn.prepareCall(sql)) {
            st.setString(1, CodEmp);
            st.setString(2, Cliente_ID);
            st.setString(3, Pedido_ID);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next() || rs.getMetaData().getColumnCount() < 18) return null;
                LocalDateTime venc = parsearFechaLibre(rs.getString(18));
                double monto;
                try {
                    monto = Double.parseDouble(rs.getString(13).trim());
                } catch (RuntimeException ex) {
                    return null;
                }
                if (venc == null || monto <= 0) return null;
                AsistenciaDTO deuda = new AsistenciaDTO();
                deuda.setTotal(monto);
                deuda.setFechaFin(venc);
                return deuda;
            }
        } catch (SQLException ex) {
            // el aviso de deuda es informativo: nunca impide registrar
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Lectura de columnas opcionales
    // ---------------------------------------------------------------------

    private static boolean tieneColumna(ResultSet rs, String columna) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (md.getColumnLabel(i).equalsIgnoreCase(columna)) return true;
        }
        return false;
    }

    private static String texto(ResultSet rs, String columna) throws SQLException {
        if (!tieneColumna(rs, columna)) return null;
        String v = rs.getString(columna);
        return v == null ? null : v.trim();
    }

    private static int entero(ResultSet rs, String columna) throws SQLException {
        String v = texto(rs, columna);
        try {
            return v == null || v.isEmpty() ? 0 : (int) Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Timestamp fecha(ResultSet rs, String columna) throws SQLException {
        if (!tieneColumna(rs, columna)) return null;
        try {
            return rs.getTimestamp(columna);
        } catch (SQLException ex) {
            LocalDateTime f = parsearFechaLibre(rs.getString(columna));
            return f == null ? null : Timestamp.valueOf(f);
        }
    }

    /** Fecha en texto: yyyy-MM-dd[ HH:mm:ss], dd/MM/yyyy o MM/dd/yyyy (CONVERT 101). */
    private static LocalDateTime parsearFechaLibre(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        try {
            return Timestamp.valueOf(t.length() == 10 ? t + " 00:00:00" : t).toLocalDateTime();
        } catch (IllegalArgumentException ex) {
            // otros formatos
        }
        String soloFecha = t.length() >= 10 ? t.substring(0, 10) : t;
        for (String patron : new String[] { "dd/MM/yyyy", "MM/dd/yyyy" }) {
            try {
                return LocalDate.parse(soloFecha, DateTimeFormatter.ofPattern(patron)).atStartOfDay();
            } catch (DateTimeParseException ex) {
                // siguiente formato
            }
        }
        return null;
    }

    /**
     * sp_ListaDetRegistroAsistenciasXFechaXTurno(@CodEmp, @FechaIngreso, @Turno): ingresos del día y turno

     */
    public List<AsistenciaDTO> ListaDetRegistroAsistenciasXFechaXTurno(String CodEmp, LocalDateTime FechaIngreso, String Turno)
    {
        List<AsistenciaDTO> lista = new ArrayList<>();
        String sql = "{call (?,?,?)}";

        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {

            cs.setString(1, CodEmp);
            // FechaIngreso se guarda a las 00:00 (ver RegistrarAsistenciaF): se compara con el inicio del día
            cs.setTimestamp(2, Timestamp.valueOf(FechaIngreso.toLocalDate().atStartOfDay()));
            cs.setString(3, Turno == null ? "" : Turno.trim());

            try (ResultSet rs = cs.executeQuery()) {
                int columnas = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    AsistenciaDTO item = new AsistenciaDTO();

                    item.setDia(texto(rs, "Dia"));
                    item.setCliente_ID(texto(rs, "Cliente_ID"));
                    item.setRazonSocial(texto(rs, "RazonSocial"));
                    item.setCondicionCredito(texto(rs, "CondicionCredito"));
                    item.setTurno(Turno == null ? "" : Turno.trim());

                    Timestamp dia = fecha(rs, "FechaIngreso");
                    LocalDate fechaIng = dia != null ? dia.toLocalDateTime().toLocalDate() : FechaIngreso.toLocalDate();
                    item.setFechaIngreso(fechaIng);

                    LocalTime hora = columnas >= 4 ? horaDe(rs.getString("HoraIngreso")) : null;
                    item.setHoraIngreso(hora == null ? null : fechaIng.atTime(hora));

                    lista.add(item);
                }
            }
        } catch (SQLException ex) {
            System.err.println("sp_ListaDetRegistroAsistenciasXFechaXTurno: " + ex.getMessage());
        }

        return lista;
    }

    /**
     * sp_ListaAperturaCierreCajaXFechaTurno(@CodEmp, @FechaTurno): turnos de caja del día (combo cboturnos de
     * frm_ConsultaAsistenciaXDia). Devuelve Turno en Turno y NomTurno en Nombre.
     */
    public List<AsistenciaDTO> ListaTurnosXFecha(String CodEmp, LocalDate Fecha)
    {
        List<AsistenciaDTO> lista = new ArrayList<>();
        String sql = "{call sp_ListaAperturaCierreCajaXFechaTurno(?,?)}";
        try (Connection cn = ConexionBD.obtenerConexion();
             CallableStatement cs = cn.prepareCall(sql)) {
            cs.setString(1, CodEmp);
            cs.setTimestamp(2, Timestamp.valueOf(Fecha.atStartOfDay()));
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    String turno = texto(rs, "Turno");
                    if (turno == null || turno.isEmpty()) continue;
                    AsistenciaDTO t = new AsistenciaDTO();
                    t.setTurno(turno);
                    String nombre = texto(rs, "NomTurno");
                    t.setNombre(nombre == null || nombre.isEmpty() ? "Turno " + turno : nombre);
                    if (lista.stream().noneMatch(x -> x.getTurno().equalsIgnoreCase(turno))) lista.add(t);
                }
            }
        } catch (SQLException ex) {
            System.err.println("sp_ListaAperturaCierreCajaXFechaTurno: " + ex.getMessage());
        }
        return lista;
    }

    /** Hora en texto "HH:mm[:ss[.nnnnnnn]]" (CAST(... AS time)) o fecha completa; null si no se puede leer. */
    private static LocalTime horaDe(String texto)
    {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        int espacio = t.lastIndexOf(' ');
        if (espacio > 0 && t.indexOf(':') > espacio) t = t.substring(espacio + 1);
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
    // ---------------------------------------------------------------------
    // Utilidades de conversión de fechas
    // ---------------------------------------------------------------------

    private static LocalDate aLocalDate(Date fecha) {
        return fecha != null ? fecha.toLocalDate() : null;
    }

    private static LocalDateTime aLocalDateTime(Timestamp fechaHora) {
        return fechaHora != null ? fechaHora.toLocalDateTime() : null;
    }

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),   // CONVERT 101
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),   // CONVERT 103
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),   // CONVERT 23 / ISO
            DateTimeFormatter.ofPattern("yyyyMMdd"),     // CONVERT 112
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy") };

    private static final DateTimeFormatter[] FORMATOS_HORA = {
            DateTimeFormatter.ofPattern("H:mm:ss"),       // CONVERT 108
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("h:mm:ss a", java.util.Locale.US),
            DateTimeFormatter.ofPattern("h:mma", java.util.Locale.US) };

    /**
     * Lee una columna de fecha u hora sin importar cómo la entregue el procedimiento:
     * datetime/date/time (objeto JDBC), o texto "yyyy-MM-dd HH:mm:ss[.fff]", "MM/dd/yyyy", "dd/MM/yyyy",
     * "HH:mm:ss" (solo hora: se devuelve con fecha 1900-01-01, como SQL Server). Valor binario
     * (rowversion), vacío o no reconocido: null. Si la columna no existe: null.
     */
    private static LocalDateTime leerFechaHora(ResultSet rs, String columna) throws SQLException {
        if (!tieneColumna(rs, columna)) return null;
        Object v = rs.getObject(columna);
        if (v == null || v instanceof byte[]) return null;
        if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        if (v instanceof Date) return ((Date) v).toLocalDate().atStartOfDay();
        if (v instanceof LocalDate) return ((LocalDate) v).atStartOfDay();
        if (v instanceof java.sql.Time) return LocalDate.of(1900, 1, 1).atTime(((java.sql.Time) v).toLocalTime());
        if (v instanceof LocalTime) return LocalDate.of(1900, 1, 1).atTime((LocalTime) v);
        if (v instanceof java.util.Date) return new Timestamp(((java.util.Date) v).getTime()).toLocalDateTime();
        return parsearFechaHoraTexto(v.toString());
    }

    private static LocalDateTime parsearFechaHoraTexto(String texto) {
        if (texto == null) return null;
        String t = texto.trim().replace('T', ' ');
        if (t.isEmpty()) return null;

        try {
            return Timestamp.valueOf(t.length() == 16 ? t + ":00" : t).toLocalDateTime();
        } catch (IllegalArgumentException ex) {
            // otro formato
        }

        String[] partes = t.split(" +", 2);
        LocalDate fecha = null;
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try { fecha = LocalDate.parse(partes[0], f); break; } catch (DateTimeParseException ex) { /* siguiente */ }
        }
        String textoHora = fecha == null ? t : (partes.length > 1 ? partes[1] : null);
        LocalTime hora = null;
        if (textoHora != null) {
            String h = textoHora.trim().toUpperCase(java.util.Locale.US)
                    .replace("A. M.", "AM").replace("P. M.", "PM").replace("A.M.", "AM").replace("P.M.", "PM");
            if (h.indexOf('.') > 0) h = h.substring(0, h.indexOf('.'));   // quita milisegundos
            for (DateTimeFormatter f : FORMATOS_HORA) {
                try { hora = LocalTime.parse(h, f); break; } catch (DateTimeParseException ex) { /* siguiente */ }
            }
        }
        if (fecha == null && hora == null) return null;
        return (fecha != null ? fecha : LocalDate.of(1900, 1, 1)).atTime(hora != null ? hora : LocalTime.MIDNIGHT);
    }

    private static LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), FORMATO_FECHA_101);
        } catch (DateTimeParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static LocalTime parsearHora(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(texto.trim(), FORMATO_HORA_108);
        } catch (DateTimeParseException ex) {
            // HoraIngreso de tipo "timestamp" (binario): la hora se completa con sp_AsistenciaHora_Lista
            return null;
        }
    }
}
