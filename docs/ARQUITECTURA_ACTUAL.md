# Arquitectura implementada en el código

## Capas
- Frontend: **Angular 18**, TypeScript, HTML y CSS. Las vistas usan `ApiService` para enviar solicitudes HTTP.
- Backend: **Spring Boot 3.4.5 con Java 21**, controladores REST y servicios de dominio separados.
- Acceso a datos: **Spring JDBC (`JdbcTemplate`)**. No utiliza Spring Data JPA en esta versión: las tablas y consultas están escritas en SQL simple para facilitar el seguimiento y depuración del proyecto académico.
- BD: **PostgreSQL 16**, contenedor Docker persistente en volumen `horizonte_integrador_data`.

## Módulos Java
- `ProjectController` → proyectos y EDT.
- `PlanningService` / `PlanningController` / `WorkdayMath` → relaciones y cálculos de fechas hábiles, calendario y Gantt.
- `ResourceController` / `CostService` → catálogo, asignaciones, costo planificado/real, sobreasignación semanal.
- `MilestoneController` → hitos, criterios, estados y evidencias.
- `KpiController` / `KpiService` / `SimpleFormula` → variables, fórmulas limitadas y seguras, metas, alertas e historial.
- `ChangeController` → solicitudes, evaluaciones, autorización, 10 etapas y línea base.
- `DashboardController` → consultas integradas para ambos tableros.
- `SecurityConfig` → autenticación HTTP Basic y roles ADMIN/USUARIO.
- `SeedData` → carga académica inicial.

## Modelo de datos
- `proyectos` 1 → N `edt_items`; `edt_items.parent_id` representa padre jerárquico.
- `dependencias` referencia dos elementos EDT con tipo FS/SS/FF/SF y desfase.
- `recursos` y `asignaciones` conectan recursos a hojas EDT.
- `hitos` N ↔ N `edt_items` por `hito_actividades`.
- `kpis` 1 → N `mediciones_kpi`.
- `cambios` 1 → N `etapas_cambio` y `linea_base`.

## Integración y alcance
- Angular y Spring Boot corren localmente en `4200` y `8080`; PostgreSQL escucha en el host en `5433`.
- La Gantt, los tableros y los KPI consultan información de las mismas tablas, no copias independientes.
- El ERP y compras de la constructora figuran como entregables dentro de la **EDT del caso**, pero **no están conectados a sistemas corporativos reales**. Es una aplicación educativa de administración del proyecto, no la plataforma operacional empresarial final.
- Para producción faltan autenticación reforzada (por ejemplo, JWT/OIDC), control de secretos, migraciones con Flyway y pruebas de seguridad/carga.

## Diferencias respecto a borradores previos
Algunos documentos de planificación preliminar mencionaban JPA/Hibernate, pero la implementación del código fuente entregado usa `JdbcTemplate`. Para evitar inconsistencias en la presentación, esta ficha refleja el **código realmente preparado**.
