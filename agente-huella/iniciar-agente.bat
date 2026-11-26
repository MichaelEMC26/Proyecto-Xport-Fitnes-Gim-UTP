@echo off
rem Inicia el agente del lector de huellas ZK9500 (dejar abierta esta ventana o ponerlo en Inicio de Windows).
cd /d "%~dp0"
title Agente de huella ZK9500
chcp 65001 >nul
set JAR=agente-huella.jar
if not exist "%JAR%" set JAR=target\agente-huella.jar
if not exist "%JAR%" (
    echo No se encontro agente-huella.jar. Compile con "mvn clean package" en esta carpeta.
    pause
    exit /b 1
)
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%JAR%"
pause
