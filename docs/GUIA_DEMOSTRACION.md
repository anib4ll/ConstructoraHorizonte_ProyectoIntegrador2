# Guía para la demostración del Proyecto Integrador 2

## Preparación
1. Abre Docker Desktop; `docker compose up -d`.
2. Abre backend: `cd backend` y `./mvnw.cmd spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`).
3. Abre frontend: `cd frontend`, `npm install`, `npm start`.
4. Entra a `http://localhost:4200` con admin@horizonte.cl / Admin2026!.
5. Selecciona PR-001 Constructora Horizonte.

## Estado inicial
- Proyectos: 1 caso de prueba, presupuesto 260 millones CLP.
- EDT: 4 etapas, 12 actividades, 20 subactividades.
- Carta Gantt: 4 dependencias precargadas FS/SS/FF/SF.
- Recursos: P01–P05 + tipos, licencias, materiales y equipos.
- Hitos: H01–H06.
- KPI: KP01–KP08 y KC01–KC12.
- Cambios: CAM-01–CAM-05.

## Escenario 1 — Duración y dependencias
1. Abre Carta Gantt; localiza las subactividades 1.1.1.1 y 1.1.1.2, relacionadas por FS.
2. Anota fecha de inicio/término de 1.1.1.2.
3. Cambia duración de 1.1.1.1 de 6 a 10 días hábiles y guarda.
4. Comprueba que la sucesora se desplaza y las barras se actualizan.
5. Revisa el Dashboard del proyecto y KPI de atraso.
6. Opcional: intenta una dependencia inversa que cree ciclo; la API debe rechazarla.

## Escenario 2 — Recursos y costos
1. Abre Recursos y costos; anota costo planificado del proyecto.
2. Edita una asignación de profesional y aumenta la cantidad u horas semanales.
3. Guarda y revisa el costo actualizado.
4. Revisa si aparece una alerta por sobreasignación.
5. Abre Dashboard de proyecto y KPI de costos / utilización.

## Escenario 3 — Aprobación de cambio y trazabilidad
1. Abre Solicitudes de cambio y selecciona CAM-03 (Firma digital en recepción).
2. Antes de aprobar, revisa duración y cantidad de recurso propuestas.
3. Aprueba la solicitud. Se modifica la subactividad de recepción y su asignación asociada.
4. Revisa la tabla de línea base, que debe mostrar **antes → después**.
5. Regresa a Gantt, Recursos y costos, KPI y los dos dashboards. Compara valores.
6. No vuelvas a aprobar el mismo cambio; el backend lo impide.

## Rol de usuario
Cierra sesión e ingresa como `usuario@horizonte.cl` / `Usuario2026!`. Debe poder consultar módulos y registrar porcentaje de avance o evidencia de hitos, pero no aprobar solicitudes ni editar la configuración administrativa.

## Si hay problemas
- Docker: `docker ps`, buscar `horizonte-integrador-db` y estado healthy.
- API: navegador `http://localhost:8080/api/proyectos` con credenciales.
- Angular: terminal `npm start`, inspeccionar el error que aparezca.
- Puerto de BD: 5433 en Windows, 5432 dentro del contenedor.
- No eliminar el volumen PostgreSQL para «probar de nuevo» a menos que quieras borrar los datos.

## Limitación de validación
Este guion especifica **comprobaciones que deben hacerse**, no afirma que se han ejecutado. Marca como aprobada cada prueba únicamente después de realizarla en tu PC.
