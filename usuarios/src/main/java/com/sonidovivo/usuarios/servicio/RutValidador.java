package com.sonidovivo.usuarios.servicio;

// RUT chileno: cuerpo y dígito verificador (módulo 11). Se guarda limpio, sin puntos.
public final class RutValidador {

    private RutValidador() {
    }

    /** Deja el RUT como 12345678-5: sin puntos, sin espacios y con la K en mayuscula. */
    public static String normalizar(String rut) {
        if (rut == null) {
            return "";
        }

        String limpio = rut.replaceAll("[^0-9kK]", "").toUpperCase();

        if (limpio.length() < 2) {
            return limpio;
        }

        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    public static boolean esValido(String rut) {
        String normalizado = normalizar(rut);

        if (!normalizado.matches("\\d{1,8}-[0-9K]")) {
            return false;
        }

        String cuerpo = normalizado.substring(0, normalizado.indexOf('-'));
        char digito = normalizado.charAt(normalizado.length() - 1);

        return calcularDigito(cuerpo) == digito;
    }

    private static char calcularDigito(String cuerpo) {
        int suma = 0;
        int factor = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }

        int resto = 11 - (suma % 11);

        if (resto == 11) {
            return '0';
        }

        if (resto == 10) {
            return 'K';
        }

        return Character.forDigit(resto, 10);
    }
}
