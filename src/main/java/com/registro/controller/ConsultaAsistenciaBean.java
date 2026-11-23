package com.registro.controller;

import com.registro.dto.AsistenciaDTO;
import com.registro.dto.MembresiaDTO;
import com.registro.facade.AsistenciaFACADE;
import com.registro.facade.MembresiaFACADE;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.registro.controller.MembresiaBean.codEmpSesion;
import static com.registro.controller.MembresiaBean.mensaje;
import static com.registro.controller.MembresiaBean.vacio;

/**
 * Consulta de asistencia por día (asistencia.xhtml), como frm_ConsultaAsistenciaXDia de SisdataGimnasio:
 * usa AsistenciaFACADE.ListaDetRegistroAsistenciasXFechaXTurnoF con la fecha y el turno de caja elegidos
 * ("Todos" = cada turno del día). Las faltas son los socios con membresía activa ese día que no ingresaron.
 */
@Named("consultaAsistenciaBean")
@ViewScoped
public class ConsultaAsistenciaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "PE"));

    private transient AsistenciaFACADE facade;

    private String fecha = LocalDate.now().toString();   // yyyy-MM-dd (input type="date")
    private String turno = "";                           // "" = todos los turnos del día
    private String filtro = "";

    private List<AsistenciaDTO> turnos = new ArrayList<>();
    private List<AsistenciaDTO> lista = new ArrayList<>();
    private int alDia;
    private int conDeuda;
    private int faltas;

    private AsistenciaFACADE getFacade() {
        if (facade == null) facade = new AsistenciaFACADE();
        return facade;
    }

    /** Al abrir la página: asistencias de hoy, todos los turnos. */
    public void abrir() {
        if (FacesContext.getCurrentInstance().isPostback()) return;
        cargar();
    }

    public String buscar() {
        cargar();
        return null;
    }

    /** Al cambiar la fecha se recargan los turnos de caja de ese día. */
    public void alCambiarFecha() {
        turno = "";
        cargar();
    }

    private void cargar() {
        lista = new ArrayList<>();
        alDia = conDeuda = faltas = 0;

        LocalDate dia = getFechaDia();
        String codEmp = codEmpSesion();

        try {
            turnos = getFacade().ListaTurnosXFechaF(codEmp, dia);
        } catch (RuntimeException ex) {
            turnos = new ArrayList<>();
        }
        if (!vacio(turno) && turnos.stream().noneMatch(t -> turno.equalsIgnoreCase(t.getTurno()))) turno = "";

        // Turnos a consultar: el elegido, o todos los de la caja del día más los que usa el registro web
        // cuando no hay caja abierta ("0") o el turno llega vacío ("M").
        Set<String> consultar = new LinkedHashSet<>();
        if (!vacio(turno)) {
            consultar.add(turno.trim());
        } else {
            for (AsistenciaDTO t : turnos) consultar.add(t.getTurno().trim());
            consultar.add("0");
            consultar.add("M");
        }

        // El SP repite al socio por cada cuenta por cobrar: una fila por socio, día y turno ("ADEUDA" manda)
        Map<String, AsistenciaDTO> unicos = new LinkedHashMap<>();
        try {
            for (String t : consultar) {
                for (AsistenciaDTO a : getFacade().ListaDetRegistroAsistenciasXFechaXTurnoF(codEmp, dia.atStartOfDay(), t)) {
                    String clave = limpio(a.getCliente_ID()) + "|" + limpio(a.getDia()) + "|" + t;
                    AsistenciaDTO previo = unicos.get(clave);
                    if (previo == null || (!adeuda(previo) && adeuda(a))) unicos.put(clave, a);
                }
            }
        } catch (RuntimeException ex) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo consultar la asistencia: " + ex.getMessage());
            return;
        }

        Set<String> asistieron = new HashSet<>();
        String texto = filtro == null ? "" : filtro.trim().toUpperCase(Locale.ROOT);
        for (AsistenciaDTO a : unicos.values()) {
            asistieron.add(limpio(a.getCliente_ID()));
            if (!texto.isEmpty() && !contiene(a.getCliente_ID(), texto) && !contiene(a.getRazonSocial(), texto)) continue;
            lista.add(a);
            if (adeuda(a)) conDeuda++; else alDia++;
        }
        lista.sort(Comparator.comparing(AsistenciaDTO::getHoraIngreso, Comparator.nullsLast(Comparator.naturalOrder())));

        faltas = contarFaltas(codEmp, dia, asistieron);
    }

    /** Socios con una membresía activa ese día (inicio ≤ día ≤ fin) que no registraron ingreso. */
    private int contarFaltas(String codEmp, LocalDate dia, Set<String> asistieron) {
        Set<String> activos = new HashSet<>();
        try {
            for (MembresiaDTO s : new MembresiaFACADE().Listasuscripciones_MembresiaF(codEmp, 1)) {
                if (s.getFechaInicio() == null || s.getFechaFin() == null) continue;
                String estado = s.getEstadoSusc() == null ? "" : s.getEstadoSusc().trim().toUpperCase(Locale.ROOT);
                if (estado.startsWith("ANULAD")) continue;
                if (s.getFechaInicio().toLocalDate().isAfter(dia) || s.getFechaFin().toLocalDate().isBefore(dia)) continue;
                activos.add(limpio(s.getCliente()));
            }
        } catch (RuntimeException ex) {
            return 0;
        }
        activos.removeAll(asistieron);
        activos.remove("");
        return activos.size();
    }

    // ---------------------------------------------------------------
    // Ayudas para la vista
    // ---------------------------------------------------------------

    public String hora(LocalDateTime h) {
        return h == null ? "--:--" : h.format(HORA);
    }

    public boolean adeuda(AsistenciaDTO a) {
        return a != null && "ADEUDA".equalsIgnoreCase(limpio(a.getCondicionCredito()));
    }

    /** Iniciales del socio para el avatar (dos letras). */
    public String iniciales(String nombre) {
        if (vacio(nombre)) return "?";
        String[] p = nombre.trim().split("\\s+");
        String i = p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "");
        return i.toUpperCase(Locale.ROOT);
    }

    /** Nombre del turno de caja (NomTurno) o el código. */
    public String nombreTurno(String t) {
        if (vacio(t)) return "-";
        for (AsistenciaDTO x : turnos) if (t.trim().equalsIgnoreCase(x.getTurno())) return x.getNombre();
        return t.trim();
    }

    private static String limpio(String s) { return s == null ? "" : s.trim(); }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toUpperCase(Locale.ROOT).contains(texto);
    }

    private LocalDate getFechaDia() {
        try {
            return vacio(fecha) ? LocalDate.now() : LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException ex) {
            fecha = LocalDate.now().toString();
            return LocalDate.now();
        }
    }

    // ---------------------------------------------------------------
    // Getters / setters
    // ---------------------------------------------------------------

    public String getFechaTexto() { return getFechaDia().format(FECHA_LARGA); }
    public List<AsistenciaDTO> getTurnos() { return turnos; }
    public List<AsistenciaDTO> getLista() { return lista; }
    public int getTotal() { return lista.size(); }
    public int getAlDia() { return alDia; }
    public int getConDeuda() { return conDeuda; }
    public int getFaltas() { return faltas; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public String getFiltro() { return filtro; }
    public void setFiltro(String filtro) { this.filtro = filtro; }
}
