/*
 * Ventana FORMA DE PAGO del Punto de Venta (plantilla formas_de_pago.html / FormasPago.java).
 *
 * Uso:
 *   FormasPago.abrir(totalConIgv, function (pago) { ... });
 *
 * "pago" (al presionar Aceptar):
 *   { total, opGravada, igv, comision, pagado, vuelto,
 *     condPago: "Credito" | "Contado",                         // "Credito" si se usó Crédito/Deuda
 *     fechaVencimiento: "2026-12-22" o null,                   // FechaVencimiento del crédito
 *     pagos:   { efectivo: 50, virtual: 10, ... },              // monto por forma de pago (clave del menú)
 *     detalle: [ { metodo: "Pago Virtual", clave, tipo, idFormaPago, monto: 10, info: "Yape · Op. 0012" }, ... ],
 *     tarjeta: { idTarjeta, nombre, porCom } o null,           // tarjeta de la tabla TarjetasPago
 *     resumen: "Efectivo S/ 50.00 + Pago Virtual (Yape) S/ 10.00" }
 *
 * Las formas de pago del menú vienen de la tabla (solo ACTIVAS: FormasPagoVentaBean ->
 * FacturacionFACADE.ListaFormasPagoActivoF). Cada botón trae:
 *   data-m (clave única), data-tipo (panel: efectivo, virtual, tarjeta, deposito, nota, credito, otro),
 *   data-id (IDFormaPago) y data-nombre.
 * Crédito/Deuda (IDFormaPago "06"): pide la fecha de vencimiento y envía CondPago = "Credito".
 * Opción Tarjeta: tarjetas de la tabla TarjetasPago; comisión = PorCom de la tarjeta elegida.
 * Flechas ↑ / ↓: forma de pago anterior / siguiente. Cada forma muestra su imagen arriba.
 */
(function () {
    "use strict";

    const IGV = 0.18;                 // 18 %: el total del punto de venta ya incluye el IGV
    const DIAS_CREDITO = 30;          // vencimiento propuesto para el crédito
    // Imagen (images/formas-pago/{tipo}.svg) y ayuda de cada tipo de forma de pago
    const AYUDAS = {
        efectivo: "Pago en billetes y monedas",
        virtual: "Yape, Plin, Agora o PagoEfectivo",
        tarjeta: "Débito o crédito con POS",
        deposito: "Depósito o transferencia bancaria",
        nota: "Descuento con una nota de crédito",
        credito: "Venta al crédito con fecha de vencimiento",
        otro: "Otra forma de pago"
    };

    const $ = function (id) { return document.getElementById(id); };
    const redondear = function (n) { return Math.round((n + Number.EPSILON) * 100) / 100; };
    const fmt = function (n) { return redondear(n).toFixed(2); };

    // Se llenan con los botones del menú (formas de pago activas de la tabla)
    let METODOS = [];                 // claves en el orden del menú
    const NOMBRES = {};               // clave -> nombre (FormasPago)
    const TIPOS = {};                 // clave -> tipo de panel
    const IDS = {};                   // clave -> IDFormaPago

    let total = 0;
    let metodo = "";
    let alAceptar = null;
    const pagos = {};

    function claveDeTipo(tipo) {
        return METODOS.find(function (m) { return TIPOS[m] === tipo; }) || null;
    }
    function inputMonto(m) {
        if (TIPOS[m] === "otro") return $("fpMontoOtro");    // panel genérico compartido
        return document.querySelector('#fpOverlay .fp-monto[data-m="' + TIPOS[m] + '"]');
    }
    function radio(nombre) {
        const r = document.querySelector('#fpOverlay input[name="' + nombre + '"]:checked');
        return r ? r.value : "";
    }
    function texto(id) { const el = $(id); return el ? el.value.trim() : ""; }

    /** Fecha local yyyy-mm-dd (dias después de hoy). */
    function fechaISO(dias) {
        const d = new Date();
        d.setDate(d.getDate() + (dias || 0));
        return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
    }
    function fechaPeru(iso) {
        const p = (iso || "").split("-");
        return p.length === 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }

    /** Monto pagado con las formas de un tipo (efectivo, tarjeta, credito...). */
    function montoTipo(tipo) {
        const m = claveDeTipo(tipo);
        return m ? pagos[m] : 0;
    }

    /** Tarjeta elegida (radio de la tabla TarjetasPago) o null si no hay ninguna. */
    function tarjetaElegida() {
        const r = document.querySelector('#fpOverlay input[name="fpTarjeta"]:checked');
        if (!r) return null;
        const porc = parseFloat(r.dataset.porcom);
        return { idTarjeta: r.value, nombre: r.dataset.nombre || r.value, porCom: isFinite(porc) && porc > 0 ? porc : 0 };
    }

    /** Comisión de la tarjeta = monto pagado con tarjeta × PorCom %. */
    function comision() {
        const t = tarjetaElegida();
        return t ? redondear(montoTipo("tarjeta") * t.porCom / 100) : 0;
    }

    /** Muestra el % de comisión de la tarjeta elegida (oculto si no tiene). */
    function mostrarComisionTarjeta() {
        const caja = $("fpComisionInfo");
        if (!caja) return;
        const t = tarjetaElegida();
        if (!t || t.porCom <= 0) { caja.hidden = true; return; }
        const mt = montoTipo("tarjeta");
        $("fpComisionTexto").textContent = "La tarjeta " + t.nombre + " tiene una comisión de "
            + fmt(t.porCom) + " %" + (mt > 0 ? " (S/ " + fmt(comision()) + ")" : "") + ".";
        caja.hidden = false;
    }

    /** Texto del crédito: monto y fecha de vencimiento. */
    function mostrarCredito() {
        const t = $("fpCreditoTexto");
        if (!t) return;
        const mc = montoTipo("credito");
        const f = texto("fpFechaVenc");
        t.textContent = mc > 0
            ? "S/ " + fmt(mc) + " a cobrar hasta el " + (f ? fechaPeru(f) : "—") + "."
            : "Ingrese el monto al crédito y la fecha de vencimiento.";
    }

    /** Imagen alusiva de la forma de pago en la parte superior. */
    function mostrarImagen(m) {
        const img = $("fpImagen");
        if (!img) return;
        const tipo = TIPOS[m] || "otro";
        const caja = $("fpIlustracion");
        caja.dataset.m = tipo;
        caja.classList.remove("fp-cambio");
        void caja.offsetWidth;            // reinicia la animación
        caja.classList.add("fp-cambio");
        img.src = img.dataset.base + tipo + ".svg";
        img.alt = NOMBRES[m];
        $("fpImagenTitulo").textContent = NOMBRES[m];
        $("fpImagenAyuda").textContent = AYUDAS[tipo];
    }

    function pagado() {
        return METODOS.reduce(function (s, m) { return s + pagos[m]; }, 0);
    }

    function calcular() {
        const com = comision();
        const pag = pagado();
        const dif = redondear(pag - total);
        const esTarjeta = TIPOS[metodo] === "tarjeta";

        $("fpTotal").textContent = fmt(total);
        $("fpPagado").value = fmt(pag);
        $("fpMontoSel").textContent = fmt(pagos[metodo] || 0);
        $("fpComision").textContent = fmt(esTarjeta ? com : 0);
        $("fpImporte").textContent = fmt(esTarjeta ? pagos[metodo] + com : (pagos[metodo] || 0));
        $("fpSaldo").value = fmt(dif);
        $("fpSaldo").classList.toggle("falta", dif < 0);
        $("fpAceptar").disabled = dif < -0.005 || total <= 0;
        $("fpMsg").textContent = dif < -0.005 ? "Falta pagar S/ " + fmt(-dif) : "";

        // monto de cada forma de pago al lado del botón del menú
        METODOS.forEach(function (m) {
            const s = document.querySelector('#fpMenu button[data-m="' + m + '"] .fp-menu-monto');
            if (s) s.textContent = pagos[m] > 0 ? "S/ " + fmt(pagos[m]) : "";
        });
        mostrarComisionTarjeta();
        mostrarCredito();
    }

    function elegir(m) {
        if (METODOS.indexOf(m) < 0) return;
        metodo = m;
        const tipo = TIPOS[m];
        document.querySelectorAll("#fpMenu button").forEach(function (b) {
            b.classList.toggle("activo", b.dataset.m === m);
            if (b.dataset.m === m) b.scrollIntoView({ block: "nearest", behavior: "smooth" });
        });
        mostrarImagen(m);
        if (tipo === "otro") {               // el panel genérico pasa a ser de esta forma de pago
            const inp = $("fpMontoOtro");
            inp.dataset.m = m;
            inp.value = fmt(pagos[m]);
        }
        document.querySelectorAll("#fpOverlay .fp-panel").forEach(function (p) {
            p.classList.toggle("activo", p.id === "fp-" + tipo);
        });
        calcular();
        const inp = inputMonto(m);
        if (inp) inp.focus();
    }

    /** Datos de cada forma de pago usada (medio, banco, n° de operación...). */
    function detalle() {
        const info = {
            efectivo: function () {
                const r = parseFloat(texto("fpRecibido")) || 0;
                return r > 0 ? "Recibido S/ " + fmt(r) : "";
            },
            virtual: function () {
                const op = texto("fpOpVirtual");
                return radio("fpVirtual") + (op ? " · Op. " + op : "");
            },
            tarjeta: function () {
                const v = texto("fpVoucher");
                const t = tarjetaElegida();
                return (t ? t.nombre : "") + " " + radio("fpModo")
                    + (t && t.porCom > 0 ? " · Com. " + fmt(t.porCom) + "%" : "") + (v ? " · Voucher " + v : "");
            },
            deposito: function () {
                const op = texto("fpOpDeposito");
                return radio("fpDep") + " " + radio("fpBanco") + (op ? " · Op. " + op : "");
            },
            nota: function () {
                const n = texto("fpNotaCredito");
                return n ? "NC " + n : "";
            },
            credito: function () {
                return "Vence " + fechaPeru(texto("fpFechaVenc"));
            },
            otro: function () {
                const op = texto("fpOpOtro");
                return op ? "Ref. " + op : "";
            }
        };
        return METODOS.filter(function (m) { return pagos[m] > 0; }).map(function (m) {
            const tipo = TIPOS[m];
            const d = {
                metodo: NOMBRES[m], clave: m, tipo: tipo, idFormaPago: IDS[m] || null,
                monto: redondear(pagos[m]), info: info[tipo]().trim()
            };
            if (tipo === "tarjeta") {
                const t = tarjetaElegida();
                d.idTarjeta = t ? t.idTarjeta : null;   // IDTarjeta de la tabla TarjetasPago
                d.porCom = t ? t.porCom : 0;
                d.comision = comision();
            }
            if (tipo === "credito") d.fechaVencimiento = texto("fpFechaVenc");
            return d;
        });
    }

    function abrir(totalVenta, callback) {
        total = redondear(Number(totalVenta) || 0);
        alAceptar = callback;
        if (total <= 0) { alert("Agrega productos a la venta."); return; }
        if (!METODOS.length) { alert("No hay formas de pago activas."); return; }

        METODOS.forEach(function (m) { pagos[m] = 0; });
        document.querySelectorAll("#fpOverlay .fp-monto").forEach(function (i) { i.value = "0.00"; });
        document.querySelectorAll("#fpOverlay .fp-extra input:not([type=radio])").forEach(function (i) { i.value = ""; });

        // Crédito: vencimiento propuesto a 30 días, no antes de hoy
        const fv = $("fpFechaVenc");
        if (fv) { fv.min = fechaISO(0); fv.value = fechaISO(DIAS_CREDITO); }

        // Como en la plantilla: se propone pagar todo en efectivo (o con la primera forma de pago)
        const inicial = claveDeTipo("efectivo") || METODOS[0];
        pagos[inicial] = total;

        const base = total / (1 + IGV);
        $("fpDesglose").textContent = "Op. gravada " + fmt(base) + "  |  IGV 18% " + fmt(total - base);

        $("fpOverlay").classList.add("abierto");
        elegir(inicial);
        const inp = inputMonto(inicial);
        inp.value = fmt(total);
        inp.select();
    }

    function cerrar() {
        $("fpOverlay").classList.remove("abierto");
        $("fpCalc").classList.remove("abierto");
    }

    function aceptar() {
        if ($("fpAceptar").disabled) return;
        if (montoTipo("tarjeta") > 0 && !tarjetaElegida()) {
            elegir(claveDeTipo("tarjeta"));   // (elegir recalcula y limpia el mensaje: va después)
            $("fpMsg").textContent = "Elija una tarjeta registrada en Configuraciones → Tarjetas de Pago.";
            return;
        }
        const credito = montoTipo("credito") > 0;
        const fv = texto("fpFechaVenc");
        if (credito && (!fv || fv < fechaISO(0))) {
            elegir(claveDeTipo("credito"));
            $("fpMsg").textContent = "Ingrese la fecha de vencimiento del crédito (no puede ser anterior a hoy).";
            $("fpFechaVenc").focus();
            return;
        }
        const pag = redondear(pagado());
        const base = redondear(total / (1 + IGV));
        const det = detalle();
        const resultado = {
            total: total,
            opGravada: base,
            igv: redondear(total - base),
            comision: comision(),
            tarjeta: montoTipo("tarjeta") > 0 ? tarjetaElegida() : null,
            condPago: credito ? "Credito" : "Contado",
            fechaVencimiento: credito ? fv : null,
            pagado: pag,
            vuelto: Math.max(0, redondear(pag - total)),
            pagos: Object.assign({}, pagos),
            detalle: det,
            resumen: det.map(function (d) {
                return d.metodo + (d.info && d.tipo !== "efectivo" ? " (" + d.info + ")" : "") + " S/ " + fmt(d.monto);
            }).join(" + ")
        };
        cerrar();
        if (typeof alAceptar === "function") alAceptar(resultado);
    }

    // ---------- Calculadora ----------
    let expr = "";
    function calcTecla(t) {
        if (t === "C") expr = "";
        else if (t === "⌫") expr = expr.slice(0, -1);
        else if (t === "=") {
            const limpia = expr.replace(/[^0-9+\-*/.]/g, "");
            try {
                const v = limpia ? Function('"use strict";return (' + limpia + ")")() : 0;
                expr = isFinite(v) ? String(redondear(v)) : "";
            } catch (e) { expr = ""; }
        } else if (t === "Usar") {    // envía el resultado al monto de la forma de pago activa
            const v = Math.max(0, parseFloat(expr) || 0);
            inputMonto(metodo).value = fmt(v);
            pagos[metodo] = v;
            calcular();
            $("fpCalc").classList.remove("abierto");
            return;
        } else expr += t;
        $("fpCalcPantalla").value = expr || "0";
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!$("fpOverlay")) return;

        // Formas de pago del menú (de la tabla)
        document.querySelectorAll("#fpMenu button[data-m]").forEach(function (b) {
            const m = b.dataset.m;
            METODOS.push(m);
            NOMBRES[m] = b.dataset.nombre || b.textContent.trim();
            TIPOS[m] = AYUDAS[b.dataset.tipo] ? b.dataset.tipo : "otro";
            IDS[m] = b.dataset.id || "";
            pagos[m] = 0;
        });
        metodo = METODOS[0] || "";

        document.querySelectorAll("#fpOverlay .fp-monto").forEach(function (inp) {
            inp.addEventListener("input", function () {
                // data-m del input = tipo de panel (o la clave, en el panel genérico)
                const m = inp.id === "fpMontoOtro" ? inp.dataset.m : claveDeTipo(inp.dataset.m);
                if (!m) return;
                pagos[m] = Math.max(0, parseFloat(inp.value) || 0);
                calcular();
            });
            inp.addEventListener("focus", function () { inp.select(); });
        });

        $("fpMenu").addEventListener("click", function (e) {
            const b = e.target.closest("button");
            if (b) elegir(b.dataset.m);
        });

        // Al cambiar de tarjeta se recalcula la comisión con su PorCom
        document.querySelectorAll('#fpOverlay input[name="fpTarjeta"]').forEach(function (r) {
            r.addEventListener("change", calcular);
        });
        // Fecha de vencimiento del crédito
        if ($("fpFechaVenc")) $("fpFechaVenc").addEventListener("change", calcular);

        // Efectivo recibido del cliente → monto en efectivo (calcula el vuelto)
        $("fpRecibido").addEventListener("input", function () {
            const r = parseFloat($("fpRecibido").value) || 0;
            const m = claveDeTipo("efectivo");
            if (r > 0 && m) {
                pagos[m] = r;
                inputMonto(m).value = fmt(r);
                calcular();
            }
        });

        $("fpAceptar").addEventListener("click", aceptar);
        $("fpCancelar").addEventListener("click", cerrar);
        $("fpCerrarX").addEventListener("click", cerrar);

        const teclas = ["7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+", "C", "⌫", "Usar"];
        teclas.forEach(function (t) {
            const b = document.createElement("button");
            b.type = "button";
            b.textContent = t;
            if ("/*-+=".indexOf(t) >= 0) b.className = "op";
            $("fpCalcTeclas").appendChild(b);
        });
        $("fpCalcTeclas").addEventListener("click", function (e) {
            if (e.target.tagName === "BUTTON") calcTecla(e.target.textContent);
        });
        $("fpBtnCalc").addEventListener("click", function () { $("fpCalc").classList.toggle("abierto"); });
        $("fpCalcCerrar").addEventListener("click", function () { $("fpCalc").classList.remove("abierto"); });

        // Esc: cierra la calculadora o la ventana; Enter en un monto: Aceptar;
        // flechas arriba / abajo: forma de pago anterior / siguiente
        // (la fecha de vencimiento usa sus propias flechas)
        document.addEventListener("keydown", function (e) {
            if (!$("fpOverlay").classList.contains("abierto")) return;
            if ((e.key === "ArrowDown" || e.key === "ArrowUp") && !$("fpCalc").classList.contains("abierto")
                    && e.target.id !== "fpFechaVenc") {
                e.preventDefault();
                const i = METODOS.indexOf(metodo);
                const j = e.key === "ArrowDown" ? Math.min(METODOS.length - 1, i + 1) : Math.max(0, i - 1);
                if (j !== i) elegir(METODOS[j]);
                return;
            }
            if (e.key === "Escape") {
                e.preventDefault();
                if ($("fpCalc").classList.contains("abierto")) $("fpCalc").classList.remove("abierto");
                else cerrar();
            } else if (e.key === "Enter" && e.target.classList && (e.target.classList.contains("fp-monto") || e.target.id === "fpRecibido")) {
                e.preventDefault();
                aceptar();
            }
        });
    });

    window.FormasPago = { abrir: abrir, cerrar: cerrar };
})();
