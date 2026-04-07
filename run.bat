@echo off
REM Script para rodar AutoClicker com JAVA_HOME configurado
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo [INFO] JAVA_HOME: %JAVA_HOME%
echo [INFO] Java Version:
java -version

echo.
echo [INFO] Iniciando compilacao e execucao...
echo.

call mvnw.cmd javafx:run

pause

