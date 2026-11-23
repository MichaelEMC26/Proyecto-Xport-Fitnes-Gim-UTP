package com.registro.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Al detener o volver a desplegar la aplicación en Tomcat, cierra las conexiones del pool
 * (si no, quedan abiertas en SQL Server hasta que el servidor las corte).
 */
@WebListener
public class CierrePoolListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ConexionBD.cerrarPool();
    }
}
