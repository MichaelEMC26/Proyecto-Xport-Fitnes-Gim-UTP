package com.registro.controller;

/**
 * Importe en letras para la venta (MontoLetras, igual que ConvierteLetras del WinForm).
 * Ejemplo: 1250.5 -> "MIL DOSCIENTOS CINCUENTA CON 50/100 SOLES".
 */
final class NumeroLetras {

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ",
        "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE",
        "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE",
        "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS",
        "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private NumeroLetras() { }

    static String convertir(double importe) {
        long centimos = Math.round(Math.abs(importe) * 100);
        long entero = centimos / 100;
        int dec = (int) (centimos % 100);
        String letras = entero == 0 ? "CERO" : enLetras(entero);
        return (letras + " CON " + (dec < 10 ? "0" : "") + dec + "/100 SOLES").trim();
    }

    private static String enLetras(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            long resto = n % 1_000_000;
            String m = millones == 1 ? "UN MILLON" : apocope(enLetras(millones)) + " MILLONES";
            return resto == 0 ? m : m + " " + enLetras(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String m = miles == 1 ? "MIL" : apocope(hasta999((int) miles)) + " MIL";
            return resto == 0 ? m : m + " " + hasta999((int) resto);
        }
        return hasta999((int) n);
    }

    private static String hasta999(int n) {
        if (n == 100) return "CIEN";
        int c = n / 100;
        int r = n % 100;
        String texto = CENTENAS[c];
        if (r > 0) texto = (texto.isEmpty() ? "" : texto + " ") + hasta99(r);
        return texto;
    }

    private static String hasta99(int n) {
        if (n < 30) return UNIDADES[n];
        int d = n / 10;
        int u = n % 10;
        return DECENAS[d] + (u > 0 ? " Y " + UNIDADES[u] : "");
    }

    /** "UNO" delante de MIL / MILLONES se dice "UN" (VEINTIUNO -> VEINTIUN). */
    private static String apocope(String s) {
        return s.endsWith("UNO") ? s.substring(0, s.length() - 1) : s;
    }
}
