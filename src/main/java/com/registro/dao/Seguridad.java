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
    
    public static String encriptarContraseña(String contraseña)
    {
        if(contraseña == null)
        {
            return "";
        }
        
        String normalizada =  contraseña.trim().toUpperCase(Locale.ROOT);
        
        if(normalizada.isEmpty())
        {
            return "";
        }
        
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            byte[] hash = digest.digest(normalizada.getBytes(StandardCharsets.UTF_16LE));
            
            return Base64.getEncoder().encodeToString(hash);
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Error a inicializar SHA-256",e);
        }
    }
}
