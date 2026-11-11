@echo off
rem Compila y empaqueta el microservicio. Usa Java 21 y el Maven instalado en tools.
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
cd /d "%~dp0"
call "C:\WorkSpace\tools\apache-maven-3.9.9\bin\mvn.cmd" -B clean package -DskipTests
if errorlevel 1 (
  echo.
  echo FALLO LA COMPILACION: revisa el mensaje de arriba.
) else (
  echo.
  echo Listo: target\mensajeria-0.0.1-SNAPSHOT.jar
)
pause
