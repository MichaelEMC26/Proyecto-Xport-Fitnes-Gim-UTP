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
        document.getElementById("raPeriodo").innerHTML = '<tr class="ra-fila-vacia"><td colspan="8">Sin datos</td></tr>';
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
