# Proyecto Integrador 2 — Constructora Horizonte

Aplicación de gestión de proyectos desarrollada con **Angular 18**, **Spring Boot 3.4.5 (Java 21)** y **PostgreSQL 16**, utilizando API REST y JDBC.

> **Aviso de pruebas:** Código fuente de integración preparado con pruebas unitarias de calendario y fórmulas. Esta distribución no ha podido compilarse ni probarse de extremo a extremo en este entorno, porque las dependencias de Angular y Maven no estaban disponibles para descarga. Antes de una entrega académica, ejecutar `mvnw.cmd test`, `npm run build` y las verificaciones funcionales descritas abajo en Windows. No presentar como validado lo que aún no se ha comprobado localmente.

## Archivos

```text
ConstructoraHorizonte_ProyectoIntegrador2_FINAL/
  backend/                 API REST y servicios Spring Boot
    pom.xml
    mvnw.cmd               Maven para Windows, sin instalación global
    src/main/java/         Controladores, servicios y datos de ejemplo
    src/main/resources/    schema.sql y application.properties
    src/test/java/         Pruebas unitarias
  frontend/                Pantallas Angular
  docs/                    Matriz de rúbrica, guía de demo y preguntas
  docker-compose.yml      PostgreSQL 16
  iniciar_backend_windows.bat
  iniciar_frontend_windows.bat
  .gitignore
  README.md
```

## PASO 1 — Abrir la carpeta correcta

Extrae el ZIP y abre en VS Code la carpeta que contiene `docker-compose.yml`, `backend` y `frontend`.

## PASO 2 — PostgreSQL en Docker

Abre Docker Desktop y espera hasta que esté iniciado. En una terminal PowerShell de VS Code:

```powershell
docker compose up -d
docker ps
```

Debe aparecer `horizonte-integrador-db` con imagen `postgres:16-alpine` y, luego de unos segundos, estado `healthy`.

Configuración **solo de demostración local**:

| Propiedad | Valor |
|---|---|
| Base de datos | `horizonte_integrador` |
| Usuario | `horizonte_app` |
| Contraseña | `HorizonteDemo2026` |
| Puerto Windows | `5433` |
| Puerto interno Docker | `5432` |


## PASO 3 — Backend Spring Boot

En **otra terminal** ubicada en la carpeta raíz:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Deja abierta la terminal. El primer inicio descargará Maven y las dependencias Java. La API escucha en `http://localhost:8080`. Verás algo similar a `Tomcat started on port 8080` y `Started ProyectoIntegradorApplication` si arrancó correctamente.

## PASO 4 — Frontend Angular

En **una tercera terminal**, desde la raíz:

```powershell
cd frontend
npm install
npm start
```

Espera a que Angular indique que está listo y abre **http://localhost:4200**. No hace falta instalar Angular CLI globalmente.

Alternativa: `iniciar_frontend_windows.bat`.

## Usuarios de demostración

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin@horizonte.cl` | `Admin2026!` |
| Usuario de proyecto | `usuario@horizonte.cl` | `Usuario2026!` |


docker exec horizonte-integrador-db pg_dump -U horizonte_app -d horizonte_integrador > respaldo_horizonte.sql
```

No incluyas `node_modules`, `backend/target` ni contraseñas reales en GitHub.
