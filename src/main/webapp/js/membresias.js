/*
 * Suscripción de membresía (igual que frm_RegistroMembresia de SisdataGimnasio).
 * SuscripcionMembresiaBean tiene la lógica; aquí: estado Enabled de los botones, clics, atajos,
 * navegación con flechas en los selectores, confirmación de Renovar e impresión del cronograma.
 * Se usa delegación de eventos porque las acciones AJAX re-renderizan el formulario.
 */
document.addEventListener("DOMContentLoaded", function () {
    const $ = function (id) { return document.getElementById(id); };

    function clic(id) { const el = $(id); if (el) el.click(); }

    /** Botón del diseño habilitado (data-habilitado="false" = Enabled false en el formulario). */
    function habilitado(id) { const b = $(id); return !!b && !b.disabled; }

    function aplicarEstados() {
        document.querySelectorAll("button[data-habilitado]").forEach(function (b) {
            b.disabled = b.dataset.habilitado !== "true";
        });
    }

    function irDashboard() {
        const enlace = document.querySelector('.sidebar a[href*="dashboard"]');
        if (enlace) window.location.href = enlace.href;
        else window.history.back();
    }

    // ---------- Selectores de planes [F5] y socios [F6]: flechas ↑/↓, Enter o doble clic, botón Agregar ----------

    function filasDe(tabla) {
        return Array.prototype.filter.call(tabla.querySelectorAll("tbody tr"), function (tr) {
            return tr.querySelector("a.link-fila");
        });
    }

    /** Resalta la fila (como la fila seleccionada del DataGridView) y le da el foco. */
    function marcarFila(fila) {
        if (!fila) return;
        const tabla = fila.closest("table");
        tabla.querySelectorAll("tbody tr.seleccionado").forEach(function (tr) { tr.classList.remove("seleccionado"); });
        fila.classList.add("seleccionado");
        fila.focus();
        fila.scrollIntoView({ block: "nearest" });
    }

    function elegirFila(fila) {
        const a = fila && fila.querySelector("a.link-fila");
        if (a) a.click();
    }

    /** Mueve la selección +1 / -1 dentro de la tabla. */
    function moverSeleccion(tabla, paso) {
        const filas = filasDe(tabla);
        if (!filas.length) return;
        let i = filas.indexOf(tabla.querySelector("tbody tr.seleccionado"));
        i = i < 0 ? (paso > 0 ? 0 : filas.length - 1) : Math.min(Math.max(i + paso, 0), filas.length - 1);
        marcarFila(filas[i]);
    }

    /** Al abrir un selector: foco en la fila elegida (o la primera); en socios, en el filtro. */
    function enfocarSelector() {
        const filtro = $("filtroCliente");
        if (filtro) { filtro.focus(); return; }
        const tabla = document.querySelector("#selectorPlanes .tabla-planes");
        if (!tabla) return;
        const filas = filasDe(tabla);
        marcarFila(tabla.querySelector("tbody tr.seleccionado") || filas[0]);
    }

    // ---------- Renovar e Imprimir ----------

    /** Renovar: si el socio tiene una membresía vigente, se pregunta Sí / No. */
    function preguntarRenovacion() {
        const c = $("confirmacionRenovar");
        const texto = c ? c.dataset.mensaje : "";
        if (!texto) return;
        c.dataset.mensaje = "";
        clic(confirm(texto) ? "lnkConfirmarRenovar" : "lnkCancelarRenovar");
    }

    /** Imprimir [Pausa]: el bean validó y cargó el cronograma. */
    function imprimirSiCorresponde() {
        const p = $("impresionCronograma");
        if (p && p.dataset.imprimir === "true") {
            p.dataset.imprimir = "false";
            setTimeout(function () { window.print(); }, 100);
        }
    }

    function alCargar() {
        aplicarEstados();
        preguntarRenovacion();
        imprimirSiCorresponde();
    }

    // Tras cada AJAX (descuento) se vuelve a aplicar el estado de los botones
    const ajax = window.faces ? window.faces.ajax : (window.jsf ? window.jsf.ajax : null);
    if (ajax) ajax.addOnEvent(function (data) { if (data.status === "success") alCargar(); });

    // ---------- Clics ----------

    document.addEventListener("click", function (e) {
        const t = e.target;

        // Clic en una fila de los selectores: la resalta (el botón Agregar o el doble clic la eligen)
        const fila = t.closest(".tabla-planes tbody tr");
        if (fila && !t.closest("a")) {
            if (fila.querySelector("a.link-fila")) marcarFila(fila);
            return;
        }

        const boton = t.closest("button");
        if (!boton || boton.disabled) return;

        // Botones del diseño original que ejecutan una acción del controlador (enlace JSF oculto)
        if (boton.dataset.accion) {
            if (boton.dataset.confirmar && !confirm(boton.dataset.confirmar)) return;
            clic(boton.dataset.accion);
            return;
        }

        if (boton.id === "btnSalir") irDashboard();
    });

    // Doble clic en una fila = elegirla (dataGridView1_DoubleClick)
    document.addEventListener("dblclick", function (e) {
        const fila = e.target.closest(".tabla-planes tbody tr");
        if (fila && !e.target.closest("a")) elegirFila(fila);
    });

    // ---------- Teclado ----------

    document.addEventListener("keydown", function (e) {
        const t = e.target;
        const fila = t && t.closest ? t.closest(".tabla-planes tbody tr") : null;

        // Flechas dentro de un selector (o desde el filtro de socios hacia la tabla)
        if (e.key === "ArrowDown" || e.key === "ArrowUp") {
            let tabla = fila ? fila.closest("table") : null;
            if (!tabla && t && t.id === "filtroCliente") tabla = document.querySelector("#selectorClientes .tabla-planes");
            if (!tabla && !(t && /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName))) {
                tabla = document.querySelector("#selectorPlanes .tabla-planes, #selectorClientes .tabla-planes");
            }
            if (tabla) {
                e.preventDefault();
                if (e.key === "ArrowUp" && fila && fila === filasDe(tabla)[0] && $("filtroCliente") && tabla.closest("#selectorClientes")) {
                    $("filtroCliente").focus();   // desde la primera fila se vuelve al filtro
                    return;
                }
                moverSeleccion(tabla, e.key === "ArrowDown" ? 1 : -1);
                return;
            }
        }

        if (e.key === "Enter" && t) {
            // Enter en una fila de los selectores: la elige (dataGridView1_KeyDown)
            if (fila) { e.preventDefault(); elegirFila(fila); return; }
            // Enter en el código del socio: lo busca por código
            if (t.id === "codigo") { e.preventDefault(); clic("lnkBuscarCodigo"); return; }
            // Enter en el filtro de socios: busca
            if (t.id === "filtroCliente") { e.preventDefault(); clic("lnkFiltrarClientes"); return; }
        }

        // ---------- Atajos de teclado (solo si el botón está habilitado, como en el formulario) ----------
        const acciones = {
            "F2": function () { if (habilitado("btnNuevo")) clic("btnNuevo"); },
            "F3": function () { if (habilitado("btnRegistrar")) clic("btnRegistrar"); },
            "F4": function () { if (habilitado("btnComprobante")) clic("btnComprobante"); },
            "F5": function () { if (habilitado("btnVerMembresias")) clic("btnVerMembresias"); },
            "F6": function () { if (habilitado("btnBuscar")) clic("btnBuscar"); },
            "F7": function () { if (habilitado("btnNuevoSocio")) clic("btnNuevoSocio"); },
            "Pause": function () { clic("btnImprimir"); },
            "Escape": function () {
                if ($("selectorPlanes")) clic("lnkCerrarPlanes");
                else if ($("selectorClientes")) clic("lnkCerrarClientes");
                else irDashboard();
            }
        };
        const accion = acciones[e.key];
        if (accion) {
            e.preventDefault();
            accion();
        }
    });

    alCargar();
    enfocarSelector();
});
