# Agente de huella ZK9500 – Xport Fitness Gym

El navegador no puede usar un lector USB directamente. Este pequeño programa corre en la **PC de recepción**
(donde está conectado el ZK9500), atiende solo en `http://127.0.0.1:8095` y la aplicación web lo usa desde:

- **socios.xhtml** → botón *Capturar huella* (3 lecturas del mismo dedo → se graba en `Cliente.HuellaDactilar` al Grabar/Actualizar).
- **RegistrarAsistencia.xhtml** → el socio pone el dedo, se identifica (1:N) y se registra el ingreso como si hubiera digitado su código.

```
Navegador (página web) ──fetch──► Agente 127.0.0.1:8095 ──JNA──► libzkfp.dll ──USB──► ZK9500
        │                                  ▲
        └── /biometria/huellas (servidor) ─┘  huellas de los socios activos (solo con sesión iniciada)
```

## Instalación en la PC de recepción

1. Instale el driver / **ZKFinger SDK** del ZK9500 (deja `libzkfp.dll` en `C:\Windows\System32`). Conecte el lector.
2. Java 17 o superior (64 bits).
3. Copie a una carpeta (p. ej. `C:\AgenteHuella\`):
   - `target\agente-huella.jar` (se genera con `mvn clean package` en esta carpeta)
   - `agente-huella.properties`
   - `iniciar-agente.bat`
4. En `agente-huella.properties`, ponga en `origenes` la dirección con la que se abre el sistema
   (p. ej. `https://www.midominio.com`). Las demás páginas web no pueden usar el lector.
5. Ejecute `iniciar-agente.bat`. Para que arranque solo, cree un acceso directo en `shell:startup`.
6. La primera vez, Chrome/Edge pueden preguntar si la página puede *acceder a otros dispositivos/apps de la red local*: elija **Permitir**.

Prueba rápida: abra `http://127.0.0.1:8095/estado` en el navegador → debe decir `"conectado":true`.

## Notas

- Las huellas que grabó el sistema de escritorio (SisdataGimnasio, control ActiveX ZKFPEngX) pueden tener otro
  formato. Al cargar la asistencia la consola del agente muestra `N correctas, M rechazadas`; si un socio antiguo
  no es reconocido, vuelva a capturar su huella en *Socios*.
- La base de huellas se vuelve a descargar solo cuando cambia (se compara una versión calculada en SQL Server).
- Si cambia el puerto, cámbielo también en `src/main/webapp/js/huella-zk.js` (`HUELLA_AGENTE`).
