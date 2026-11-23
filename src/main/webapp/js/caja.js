// Comportamiento de AperturaCaja.xhtml y RegistrarCierreCaja.xhtml:
// reloj (timer1), solo números en el monto, Enter = Generar apertura, Esc = Salir, imprimir ticket.
(function () {
    function dos(n) { return (n < 10 ? "0" : "") + n; }
    function textoHora(d) {
        var h = d.getHours(), ampm = h >= 12 ? "PM" : "AM";
        h = h % 12 || 12;
        return dos(h) + ":" + dos(d.getMinutes()) + ":" + dos(d.getSeconds()) + " " + ampm;
    }
    function reloj() {
        var t = textoHora(new Date());
        document.querySelectorAll(".cj-reloj").forEach(function (el) { el.textContent = t; });
    }
    reloj();
    setInterval(reloj, 1000);

    // Monto inicial: solo números y un punto (Componentes.validarNumeros); Enter = Generar apertura
    var monto = document.getElementById("cjMontoInicial");
    if (monto) {
        monto.focus();
        monto.select();
        monto.addEventListener("keypress", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                var btn = document.getElementById("cjGenerar");
                if (btn && !btn.disabled) btn.click();
                return;
            }
            if (e.key.length === 1 && !/[0-9.]/.test(e.key)) e.preventDefault();
            if (e.key === "." && monto.value.indexOf(".") >= 0) e.preventDefault();
        });
    }

    // Esc = Salir (si el ticket está abierto, solo lo cierra)
    document.addEventListener("keyup", function (e) {
        if (e.key !== "Escape") return;
        var cerrarTicket = document.getElementById("cjCerrarTicket");
        if (cerrarTicket) { cerrarTicket.click(); return; }
        var salir = document.getElementById("cjSalir");
        if (salir) salir.click();
    });

    window.cjImprimirTicket = function () { window.print(); };
}());
