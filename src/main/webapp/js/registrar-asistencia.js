// Comportamiento de RegistrarAsistencia.xhtml (igual que frm_RegistrarAsistencia de SisdataGimnasio).
// El registro lo hace AsistenciaBean; aquí: reloj, lectura QR, estado del botón Ingresar y atajos.
(function () {
    var form = document.getElementById("formAsistencia");
    var codigo = document.getElementById("raCodigo");
    var btnIngresar = document.getElementById("raIngresar");
    var btnNuevo = document.getElementById("raNuevo");
    var btnSalir = document.getElementById("raSalir");
    var fecha = document.getElementById("raFecha");
    var hora = document.getElementById("raHora");
    if (!codigo || !form) return;

    function dos(n) { return (n < 10 ? "0" : "") + n; }
    function textoFecha(d) { return dos(d.getDate()) + "/" + dos(d.getMonth() + 1) + "/" + d.getFullYear(); }
    function textoHora(d) {
        var h = d.getHours(), ampm = h >= 12 ? "PM" : "AM";
        h = h % 12 || 12;
        return dos(h) + ":" + dos(d.getMinutes()) + ":" + dos(d.getSeconds()) + " " + ampm;
    }

    // Reloj (timer1 del formulario de escritorio)
    function reloj() {
        var d = new Date();
        var dias = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];
        fecha.textContent = dias[d.getDay()] + ", " + textoFecha(d);
        hora.textContent = textoHora(d);
    }
    reloj();
    setInterval(reloj, 1000);

    // ---- Bloqueo de los controles de ingreso ----
    // AsistenciaBean.controlesBloqueados llega en data-bloqueado. Si es "true" (ya se mostró el
    // resultado de un registro) la casilla Código y el botón Ingresar [Enter] quedan desactivados;
    // solo Nuevo [F2] los vuelve a activar. Al abrir la página llega "false": activos.
    var ayuda = document.getElementById("raAyuda");

    function bloquearControles(bloquear) {
        codigo.disabled = bloquear;
        btnIngresar.disabled = bloquear;
        if (ayuda) ayuda.setAttribute("data-bloqueado", bloquear ? "true" : "false");
        if (bloquear) codigo.blur(); else codigo.focus();
    }

    function bloqueado() { return codigo.disabled; }

    bloquearControles(btnIngresar.getAttribute("data-bloqueado") === "true");

    function ingresar() {
        if (bloqueado()) return;   // Enter / Ingresar no hacen nada hasta presionar Nuevo
        if (!codigo.value.trim()) { codigo.value = ""; codigo.focus(); return; }
        document.getElementById("lnkIngresar").click();
    }

    // Nuevo [F2]: limpia en pantalla al instante y avisa al bean
    function limpiarPantalla() {
        codigo.value = "";
        document.querySelectorAll(".ra-dato-socio").forEach(function (el) { el.hidden = true; el.style.display = "none"; });
        var avisos = document.getElementById("raAvisos");
        if (avisos && !avisos.querySelector(".ra-aviso-vacio")) {
            var p = document.createElement("p");
            p.className = "ra-aviso-vacio";
            p.textContent = "Registre un ingreso para ver los avisos del socio.";
            avisos.appendChild(p);
        }
        var foto = document.getElementById("raFoto");
        if (foto && !foto.querySelector("i")) foto.insertAdjacentHTML("afterbegin", '<i class="fa-solid fa-user"></i>');
        document.getElementById("raPeriodo").innerHTML = '<tr class="ra-fila-vacia"><td colspan="7">Sin datos</td></tr>';
        document.getElementById("raHistorial").innerHTML = '<tr class="ra-fila-vacia"><td colspan="3">Sin asistencias</td></tr>';
        bloquearControles(false);   // solo aquí (Nuevo) se vuelven a activar
    }

    function nuevo() {
        limpiarPantalla();
        document.getElementById("lnkNuevo").click();
    }

    btnIngresar.addEventListener("click", ingresar);
    btnNuevo.addEventListener("click", nuevo);

    // Enter en el código = Ingresar (también con el botón deshabilitado, como txtcodigo_KeyPress)
    codigo.addEventListener("keydown", function (e) {
        if (e.key === "Enter") { e.preventDefault(); ingresar(); }
    });

    if (!bloqueado()) codigo.focus();

    // ---- Lector de huellas ZK9500 (agente local, ver js/huella-zk.js) ----
    // Con los controles activos espera un dedo; si reconoce al socio pone su código y hace Ingresar.
    // Con un resultado en pantalla no lee: igual que la casilla Código, se reactiva con Nuevo [F2].
    var cajaHuella = document.getElementById("raHuella");
    var textoHuella = document.getElementById("raHuellaEstado");
    var reintento = null;

    function estadoHuella(estado, texto) {
        if (!cajaHuella) return;
        cajaHuella.setAttribute("data-estado", estado);
        textoHuella.textContent = texto;
    }

    function reintentar(fn, ms) {
        clearTimeout(reintento);
        reintento = setTimeout(fn, ms);
    }

    function esperarHuella(soltar) {
        if (bloqueado()) {
            estadoHuella("pausa", "Presione Nuevo [F2] para leer otra huella.");
            return;
        }
        HuellaZK.identificar({ timeout: 20000, soltar: soltar }).then(function (r) {
            if (bloqueado()) return;
            if (r.ok && r.encontrado) {
                estadoHuella("ok", "Huella reconocida.");
                codigo.value = r.codigo;
                ingresar();
            } else if (r.ok) {
                estadoHuella("error", "Huella no registrada. Intente otra vez o digite su código.");
                reintentar(function () { esperarHuella(true); }, 300);
            } else if (r.codigo === "TIEMPO") {
                estadoHuella("listo", "Coloque su dedo en el lector.");
                esperarHuella(false);
            } else if (r.codigo !== "CANCELADO") {
                estadoHuella("sin", r.mensaje);
                reintentar(iniciarHuella, 5000);
            }
        });
    }

    function iniciarHuella() {
        if (!cajaHuella || !window.HuellaZK) return;
        HuellaZK.sincronizar(cajaHuella.getAttribute("data-contexto") || "").then(function (r) {
            if (!r.ok) {
                estadoHuella("sin", r.mensaje);
                if (r.codigo !== "SESION") reintentar(iniciarHuella, 5000);
                return;
            }
            if (!r.huellas) {
                estadoHuella("sin", "Ningún socio activo tiene huella registrada.");
                reintentar(iniciarHuella, 30000);
                return;
            }
            estadoHuella("listo", "Coloque su dedo en el lector.");
            esperarHuella(true);
        });
    }

    iniciarHuella();

    document.addEventListener("keydown", function (e) {
        if (e.key === "F2") { e.preventDefault(); nuevo(); return; }
        if (e.key === "Escape") { e.preventDefault(); if (btnSalir) window.location.href = btnSalir.href; return; }

        // Con los controles bloqueados no se acepta nada (ni Enter ni lo que escriba el lector QR)
        if (bloqueado()) {
            if (e.key === "Enter") e.preventDefault();
            return;
        }

        // Si el lector QR escribe con el cursor en otro lugar, el texto va a la casilla del código
        var t = e.target;
        if (t === codigo || e.ctrlKey || e.altKey || e.metaKey || e.key.length !== 1) return;
        if (t && (t.tagName === "INPUT" || t.tagName === "TEXTAREA" || t.tagName === "SELECT")) return;
        codigo.focus();
    });
}());
