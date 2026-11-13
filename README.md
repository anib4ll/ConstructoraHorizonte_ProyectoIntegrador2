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

## Requisitos de tu PC

1. **Java 21** (`java -version`).
2. **Node.js LTS** y npm (`node -v`, `npm -v`).
3. **Docker Desktop** abierto y con motor Linux funcionando (`docker ps`).
4. **VS Code** con extensiones recomendadas: Extension Pack for Java, Spring Boot Extension Pack y Angular Language Service.
5. Internet la primera vez para descargar Maven y paquetes npm.

## PASO 1 — Abrir la carpeta correcta

Extrae el ZIP y abre en VS Code la carpeta que contiene `docker-compose.yml`, `backend` y `frontend`. Los comandos siguientes se ejecutan desde **esa carpeta raíz**, no desde `backend`.

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

Se usa **5433** en Windows para no interferir con el contenedor anterior que usaba 5432. No necesitas crear las tablas manualmente: Spring Boot ejecuta `schema.sql` y carga datos del caso al iniciar por primera vez.

Para comprobar que PostgreSQL acepta el usuario/contraseña, ejecuta:

```powershell
docker exec -e PGPASSWORD=HorizonteDemo2026 horizonte-integrador-db psql -h 127.0.0.1 -U horizonte_app -d horizonte_integrador -c "SELECT current_user;"
```

El resultado esperado contiene `horizonte_app`.

**Importante:** `docker compose down` detiene y elimina contenedor/red pero conserva los datos. **NO ejecutes `docker compose down -v`** salvo que quieras borrar deliberadamente toda la información de la base de datos de este proyecto.

## PASO 3 — Backend Spring Boot

En **otra terminal** ubicada en la carpeta raíz:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Deja abierta la terminal. El primer inicio descargará Maven y las dependencias Java. La API escucha en `http://localhost:8080`. Verás algo similar a `Tomcat started on port 8080` y `Started ProyectoIntegradorApplication` si arrancó correctamente.

Alternativa: doble clic en `iniciar_backend_windows.bat` desde la carpeta raíz.

Si aparece error de conexión, comprueba Docker y el puerto 5433 antes de borrar volúmenes. Si hay un servicio anterior ocupando 5433, puedes cambiar **ambos lados**: `docker-compose.yml` (puerto del host) y `backend/src/main/resources/application.properties` (JDBC URL). No cambies solo uno.

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

La autenticación usa HTTP Basic para **uso local educativo**. No está endurecida para producción ni se recomienda publicarla en internet. Admin puede editar/autorizar; usuario puede consultar y registrar avances/evidencia.

## Datos precargados

Se carga el caso Constructora Horizonte **solo si la tabla de proyectos está vacía**: 4 etapas, 12 actividades, 20 subactividades, 4 precedencias FS/SS/FF/SF, 5 profesionales y otros recursos, 6 hitos, 8 KPI del proyecto, 12 KPI del cambio y 5 solicitudes.

Las tarifas, porcentajes, fechas internas y responsables de ejemplo son **datos académicos ficticios**, no cifras oficiales suministradas por la empresa. El presupuesto máximo del caso es $260.000.000 CLP y el piloto se plantea para tres obras. La plataforma no reemplaza el ERP corporativo.

## Módulos

- Proyectos / EDT (crear, editar, ver; borrar componentes EDT sin hijos ni referencias).
- Planificación: duraciones, dependencias FS/SS/FF/SF, desfases positivos/negativos, feriados, detección de ciclos y Carta Gantt.
- Recursos: profesionales, tipos, materiales, equipamiento, licencias; asignación y costos plan/real; alertas por sobrecarga semanal.
- Hitos: fecha comprometida/real, aceptación y evidencia.
- KPI: fórmulas configurables, metas, semáforos, historial; 8 + 12 precargados.
- Solicitudes de cambio: creación, evaluación, aprobación/rechazo, diez etapas, cierre, adopción/efectividad y línea base antes/después.
- Dos dashboards con filtros y módulo de demostración integrada.

## Verificación sugerida antes de entregar

Desde la raíz, con Docker ejecutándose y sin cerrar el backend:

```powershell
cd backend
.\mvnw.cmd test
```

En otra terminal:

```powershell
cd frontend
npm run build
```

Luego realiza los tres escenarios de `docs/GUIA_DEMOSTRACION.md`. También puedes consultar los datos de la API desde el navegador en `http://localhost:8080/api/proyectos`, ingresando las credenciales de demostración cuando lo pida el navegador.

## Reiniciar, cerrar y respaldar

Para detener Angular y Spring Boot usa `Ctrl+C` en sus terminales. Para detener PostgreSQL conservando los datos:

```powershell
docker compose down
```

Para respaldo (mientras esté funcionando PostgreSQL):

```powershell
docker exec horizonte-integrador-db pg_dump -U horizonte_app -d horizonte_integrador > respaldo_horizonte.sql
```

No incluyas `node_modules`, `backend/target` ni contraseñas reales en GitHub.
