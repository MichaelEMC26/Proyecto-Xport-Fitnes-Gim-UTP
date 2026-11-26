// Registro de la huella del socio (socios.xhtml) con el lector ZK9500.
// Se piden 3 lecturas del mismo dedo, el agente las une en una plantilla y esta queda en el campo
// oculto "huellaCapturada"; ClienteBean la graba en Cliente.HuellaDactilar al presionar Grabar/Actualizar.
(function () {
    "use strict";
    var boton = document.getElementById("btnCapturarHuella");
    var caja = document.getElementById("huellaBox");
    var imagen = document.getElementById("huellaImagen");
    var estado = document.getElementById("huellaEstado");
    var campo = document.getElementById("huellaCapturada");
    if (!boton || !caja || !campo || !window.HuellaZK) return;

    var LECTURAS = 3;
    var controlador = null;     // lectura en curso (el botón la cancela)
    var textoBoton = boton.querySelector("span");
    var teniaHuella = caja.getAttribute("data-estado") === "guardada";

    function mostrar(texto, tipo, estadoCaja) {
        estado.textContent = texto;
        estado.setAttribute("data-tipo", tipo || "");
        if (estadoCaja) caja.setAttribute("data-estado", estadoCaja);
    }

    function terminar(etiqueta) {
        controlador = null;
        textoBoton.textContent = etiqueta;
    }

    function leer(n, plantillas) {
        mostrar("Coloque el dedo en el lector (" + n + " de " + LECTURAS + ")...", "", "leyendo");
        return HuellaZK.capturar({ timeout: 20000, soltar: n > 1, imagen: true, controlador: controlador })
            .then(function (r) {
                if (!r.ok) throw r;
                if (r.imagen) { imagen.src = r.imagen; imagen.hidden = false; }
                plantillas.push(r.plantilla);
                if (n < LECTURAS) {
                    mostrar("Lectura " + n + " correcta. Levante el dedo y vuelva a colocarlo.", "", "leyendo");
                    return leer(n + 1, plantillas);
                }
                return HuellaZK.fusionar(plantillas);
            });
    }

    function capturar() {
        if (controlador) {                 // segundo clic: cancelar
            controlador.abort();
            HuellaZK.cancelar();
            return;
        }
        controlador = new AbortController();
        textoBoton.textContent = "Cancelar";
        HuellaZK.estado().then(function (est) {
            if (!est.ok) throw est;
            if (!est.conectado) throw { mensaje: est.mensaje };
            return leer(1, []);
        }).then(function (r) {
            if (!r.ok) throw r;
            campo.value = r.plantilla;
            mostrar("Huella capturada. Presione Grabar/Actualizar para guardarla.", "ok", "ok");
            terminar("Volver a capturar");
        }).catch(function (err) {
            var cancelado = err && err.codigo === "CANCELADO";
            mostrar(cancelado ? "Captura cancelada." : (err && err.mensaje) || "No se pudo capturar la huella.",
                    cancelado ? "" : "error", campo.value ? "ok" : (teniaHuella ? "guardada" : "error"));
            terminar(campo.value || teniaHuella ? "Volver a capturar" : "Capturar huella");
        });
    }

    boton.addEventListener("click", capturar);

    // Tras un error de validación la página se vuelve a mostrar con la huella ya capturada
    if (campo.value) {
        caja.setAttribute("data-estado", "ok");
        mostrar("Huella capturada. Presione Grabar/Actualizar para guardarla.", "ok");
    }
}());
