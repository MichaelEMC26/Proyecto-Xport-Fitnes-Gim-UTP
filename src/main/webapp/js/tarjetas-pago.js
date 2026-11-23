// Comportamiento de ManteTarjetasPago.xhtml (igual que frm_ManteTarjetasPago de SisdataGimnasio):
// atajos F2/F3/F4/F6/Esc, Enter para pasar de campo y grabar, código solo números, clic en la fila,
// y confirmación antes de eliminar.
(function () {
    function $(id) { return document.getElementById(id); }

    // Un h:commandLink deshabilitado se dibuja como <span>: solo se hace clic si es un enlace
    function pulsar(id) {
        var el = $(id);
        if (el && el.tagName === "A") el.click();
    }

    var codigo = $("tpCodigo");
    var descripcion = $("tpDescripcion");
    var porCom = $("tpPorCom");
    var estado = $("tpEstado");

    // Foco inicial: descripción (el código es de solo lectura, lo genera sp_GeneraCodigoTarjeta)
    if (descripcion && !descripcion.disabled) { descripcion.focus(); descripcion.select(); }

    // Código de solo lectura: Enter o clic en él pasan a la Descripción
    if (codigo) {
        codigo.addEventListener("keydown", function (e) {
            if (e.key === "Enter") { e.preventDefault(); if (descripcion) descripcion.focus(); }
        });
    }
    // Descripción: Enter -> Monto (%)
    if (descripcion) {
        descripcion.addEventListener("keypress", function (e) {
            if (e.key === "Enter") { e.preventDefault(); if (porCom) { porCom.focus(); porCom.select(); } }
        });
    }
    // Monto (%): Enter -> Estado
    if (porCom) {
        porCom.addEventListener("keypress", function (e) {
            if (e.key === "Enter") { e.preventDefault(); if (estado) estado.focus(); }
        });
    }
    // Estado: Enter -> Grabar (cboestado_KeyPress)
    if (estado) {
        estado.addEventListener("keydown", function (e) {
            if (e.key === "Enter") { e.preventDefault(); pulsar("tpGrabar"); }
        });
    }

    // Clic en cualquier parte de la fila = seleccionar (doble clic del dataGridView)
    document.querySelectorAll(".tp-fila").forEach(function (fila) {
        fila.addEventListener("click", function (e) {
            if (e.target.closest("a")) return;
            var link = fila.querySelector("a.tp-link-fila");
            if (link) link.click();
        });
    });

    // Confirmación antes de eliminar
    window.tpConfirmarEliminar = function () {
        var fila = document.querySelector(".tp-fila-activa");
        var nombre = fila ? fila.children[1].textContent.trim() : "";
        return window.confirm("¿Desea eliminar la tarjeta de pago" + (nombre ? " \"" + nombre + "\"" : "") + "?");
    };

    // Atajos del formulario de escritorio
    document.addEventListener("keydown", function (e) {
        switch (e.key) {
            case "F2": e.preventDefault(); pulsar("tpNuevo"); break;
            case "F3": e.preventDefault(); pulsar("tpGrabar"); break;
            case "F4": e.preventDefault(); pulsar("tpModificar"); break;
            case "F6": e.preventDefault(); pulsar("tpEliminar"); break;
            case "Escape":
                e.preventDefault();
                if ($("tpCancelar")) pulsar("tpCancelar"); else pulsar("tpSalir");
                break;
        }
    });
}());
