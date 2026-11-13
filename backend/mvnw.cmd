@echo off
setlocal EnableExtensions

rem Wrapper simple para usar Maven sin instalarlo globalmente en Windows.
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME_LOCAL=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME_LOCAL%\bin\mvn.cmd"
set "MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

if exist "%MAVEN_CMD%" goto run_maven

echo Maven %MAVEN_VERSION% no esta instalado para este proyecto.
echo Se descargara automaticamente una sola vez...

where powershell >nul 2>nul
if errorlevel 1 (
    echo ERROR: PowerShell no esta disponible.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%';" ^
  "$tmp=Join-Path $env:TEMP 'horizonte-maven-wrapper';" ^
  "if(Test-Path $tmp){Remove-Item $tmp -Recurse -Force};" ^
  "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath $tmp -Force;" ^
  "$src=Join-Path $tmp 'apache-maven-%MAVEN_VERSION%';" ^
  "$dst='%MAVEN_HOME_LOCAL%';" ^
  "if(Test-Path $dst){Remove-Item $dst -Recurse -Force};" ^
  "New-Item -ItemType Directory -Path (Split-Path $dst) -Force | Out-Null;" ^
  "Move-Item $src $dst;" ^
  "Remove-Item $tmp -Recurse -Force;" ^
  "Remove-Item '%MAVEN_ZIP%' -Force -ErrorAction SilentlyContinue"

if errorlevel 1 (
    echo ERROR: No se pudo descargar Maven.
    echo Revisa tu conexion a Internet e intentalo nuevamente.
    exit /b 1
)

:run_maven
call "%MAVEN_CMD%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
