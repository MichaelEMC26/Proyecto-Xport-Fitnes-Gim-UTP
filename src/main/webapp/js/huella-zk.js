// Cliente del agente local del lector de huellas ZK9500 (carpeta agente-huella del proyecto).
// El navegador no accede al USB: el agente corre en la PC de recepción en http://127.0.0.1:8095
// y esta librería lo llama con fetch(). Lo usan socios.xhtml (registrar huella) y
// RegistrarAsistencia.xhtml (identificar al socio).
(function (global) {
    "use strict";

    var HUELLA_AGENTE = "http://127.0.0.1:8095";

    function pedir(ruta, opciones, controlador) {
        opciones = opciones || {};
        if (controlador) opciones.signal = controlador.signal;
        return fetch(HUELLA_AGENTE + ruta, opciones).then(function (r) {
            return r.json().catch(function () { return { ok: false, codigo: "RESPUESTA", mensaje: "Respuesta inválida del agente." }; });
        }, function (err) {
            if (err && err.name === "AbortError") return { ok: false, codigo: "CANCELADO", mensaje: "Cancelado." };
            return { ok: false, codigo: "SIN_AGENTE",
                     mensaje: "No se pudo conectar con el agente de huella. Inicie \"iniciar-agente.bat\" en esta PC." };
        });
    }

    // Texto plano: no necesita preflight CORS
    function post(ruta, cuerpo, controlador) {
        return pedir(ruta, { method: "POST", headers: { "Content-Type": "text/plain" }, body: cuerpo || "" }, controlador);
    }

    var HuellaZK = {
        /** { ok, conectado, version, huellas, mensaje } */
        estado: function () { return pedir("/estado"); },

        /** Una lectura. op: { timeout, soltar, imagen, controlador } -> { ok, plantilla, imagen } */
        capturar: function (op) {
            op = op || {};
            return post("/capturar?timeout=" + (op.timeout || 15000) + "&soltar=" + (op.soltar ? 1 : 0)
                        + "&imagen=" + (op.imagen ? 1 : 0), "", op.controlador);
        },

        /** Une 3 lecturas del mismo dedo -> { ok, plantilla } */
        fusionar: function (plantillas) { return post("/fusionar", plantillas.join("\n")); },

        /** Espera un dedo y busca al socio -> { ok, encontrado, codigo, puntaje } */
        identificar: function (op) {
            op = op || {};
            return post("/identificar?timeout=" + (op.timeout || 20000) + "&soltar=" + (op.soltar ? 1 : 0), "", op.controlador);
        },

        cancelar: function () { return post("/cancelar", ""); },

        /**
         * Pasa al agente las huellas de los socios activos (desde /biometria/huellas del servidor),
         * solo si cambiaron desde la última carga. -> { ok, huellas, mensaje }
         */
        sincronizar: function (contextPath) {
            var url = contextPath + "/biometria/huellas";
            return HuellaZK.estado().then(function (est) {
                if (!est.ok) return est;
                if (!est.conectado) return { ok: false, codigo: "SIN_LECTOR", mensaje: est.mensaje };
                return fetch(url + "?version=1", { credentials: "same-origin", cache: "no-store" })
                    .then(function (r) { return r.headers.get("X-Huellas-Version") === null ? null : r.text(); })
                    .then(function (version) {
                        if (version === null) return { ok: false, codigo: "SESION", mensaje: "La sesión expiró. Vuelva a ingresar." };
                        if (version && version === est.version) return { ok: true, huellas: est.huellas };
                        return fetch(url, { credentials: "same-origin", cache: "no-store" })
                            .then(function (r) { return r.text(); })
                            .then(function (lista) { return post("/cargar?version=" + encodeURIComponent(version), lista); })
                            .then(function (res) {
                                if (!res.ok) return res;
                                return { ok: true, huellas: res.cargadas, rechazadas: res.rechazadas };
                            });
                    }, function () {
                        return { ok: false, codigo: "SERVIDOR", mensaje: "No se pudieron obtener las huellas del servidor." };
                    });
            });
        }
    };

    global.HuellaZK = HuellaZK;
}(window));
