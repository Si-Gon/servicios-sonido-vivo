@echo off
rem Levanta el microservicio en el puerto 8082 contra la base mensajeria de Laragon.
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
cd /d "%~dp0"
if not exist "target\mensajeria-0.0.1-SNAPSHOT.jar" (
  echo No existe el jar. Ejecuta primero compilar.bat
  pause
  exit /b 1
)
"%JAVA_HOME%\bin\java" -jar "target\mensajeria-0.0.1-SNAPSHOT.jar"
pause
