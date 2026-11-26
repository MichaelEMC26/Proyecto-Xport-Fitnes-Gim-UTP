/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
/**
 *
 * @author Ing
 */
public class Seguridad {

    /**
     * Encriptado de contraseñas IGUAL al del proyecto WinForms SistemaGimnasio
     * (Negocios.Seguridad.EncriptarClave): SHA-256 de los bytes UTF-8 de la clave tal cual
     * (sin recortar espacios ni pasar a mayúsculas), en Base64 (44 caracteres).
     * Así una misma cuenta entra con la misma clave en los dos sistemas.
     */
    public static String encriptarContraseña(String contraseña)
    {
        if (contraseña == null || contraseña.isEmpty())
        {
            return "";
        }
        return sha256Base64(contraseña.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encriptado que usaba antes este proyecto web (clave recortada, en mayúsculas y en UTF-16LE).
     * Solo se usa en el login para reconocer las claves grabadas desde la web con la versión
     * anterior; al entrar, la clave se vuelve a grabar con encriptarContraseña().
     */
    public static String encriptarContraseñaWebAnterior(String contraseña)
    {
        if (contraseña == null)
        {
            return "";
        }
        String normalizada = contraseña.trim().toUpperCase(Locale.ROOT);
        if (normalizada.isEmpty())
        {
            return "";
        }
        return sha256Base64(normalizada.getBytes(StandardCharsets.UTF_16LE));
    }

    private static String sha256Base64(byte[] datos)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(datos));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Error al inicializar SHA-256", e);
        }
    }
}
