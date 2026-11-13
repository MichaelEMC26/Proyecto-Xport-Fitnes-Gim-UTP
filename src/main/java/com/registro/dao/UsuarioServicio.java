/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dao;

import com.registro.dao.Seguridad;
import com.registro.dto.UsuarioDTO;
/**
 *
 * @author Ing
 */
public class UsuarioServicio {
    
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    
    public int registrarNuevoUsuario(UsuarioDTO usuario)
    {
        String contraseñaplana = usuario.getContraseña();
        
        String Contraseñaencriptada = Seguridad.encriptarContraseña(contraseñaplana);
        
        usuario.setContraseña(Contraseñaencriptada);
        
        return usuarioDAO.grabarPersonal(usuario);
    }
}
