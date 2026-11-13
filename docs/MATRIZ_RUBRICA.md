# Matriz simple: requisito y módulo que lo resuelve

Caso: Constructora Horizonte. Especificación tomada de la guía y la rúbrica del Proyecto Integrador 2.

| Requerimiento de la evaluación | Módulo de la aplicación | Qué mostrar para verificarlo |
|---|---|---|
| 1. Administración de proyectos (código, responsable, fechas, presupuesto, estado, avance) | **Proyectos** — Angular `projects.component.ts`, API `ProjectController` | Abrir PR-001, editar descripción o presupuesto y recargar |
| 2. EDT jerárquica con 4 etapas, 12 actividades y 20 subactividades | **EDT** — `edt.component.ts`, tabla `edt_items` | Expandir etapas, visualizar niveles y editar una subactividad |
| 3. Duraciones, fechas planificadas/reales, consolidación | **EDT + Planificación** — `PlanningService`, `WorkdayMath` | Cambiar una duración y verificar fechas recalculadas |
| 4. Cuatro dependencias FS, SS, FF y SF; desfases; rechazo de ciclos | **Carta Gantt** — `PlanningController`, `PlanningService` | Revisar relaciones precargadas y probar un ciclo (debe rechazarlo) |
| 5. Gantt interactivo con barras, avance, hitos, alertas, escala | **Carta Gantt** — `planning.component.ts` | Cambiar escala, ver barras y las flechas de precedencia |
| 6. Al menos 5 profesionales y 4 tipos de recursos | **Recursos y costos** — `ResourceController`, `CostService` | Mostrar P01–P05 y el catálogo de licencias/materiales/equipos |
| 7. Asignaciones, costo real/plan, sobrecarga semanal | **Recursos y costos** — `CostService` | Editar horas/cantidad, revisar nuevo costo y alertas |
| 8. Seis hitos con aceptación y evidencia | **Hitos** — `MilestoneController` | Mostrar H01–H06, modificar estado/evidencia |
| 9. Ocho KPI de proyecto/hitos | **Indicadores KPI** — `KpiController`, `KpiService` | Filtrar PROYECTO y revisar KP01–KP08 |
| 10. KPI configurables con fórmula, metas, semáforo e historial | **Indicadores KPI** — `SimpleFormula` y pantalla KPI | Editar meta/fórmula y abrir historial |
| 11. Cinco solicitudes de cambio, aprobación/rechazo y trazabilidad | **Solicitudes de cambio** — `ChangeController` | Mostrar CAM-01…CAM-05; aprobar CAM-03 |
| 12. Diez etapas de seguimiento por cambio | **Solicitudes de cambio** — tabla `etapas_cambio` | Abrir CAM-03 y mostrar etapas 1 a 10 |
| 13. Doce KPI de gestión del cambio | **Indicadores KPI** — KC01–KC12 | Filtrar CAMBIO y revisar cálculo/meta |
| 14. Dashboard del proyecto con filtros, costos, hitos, avance, atrasos | **Dashboard del proyecto** — `DashboardController` | Filtrar etapa, revisar métricas, gráficos y KPI |
| 15. Dashboard de cambios, riesgos, costos, estados y KPI | **Dashboard de cambios** — `DashboardController` | Filtrar por estado y riesgo; revisar gráficos y KPI |
| 16. Persistencia de datos y roles | **PostgreSQL + seguridad** — `schema.sql`, `SecurityConfig` | Editar y refrescar; iniciar sesión con admin y usuario |
| 17. Demostración integrada: cambiar duración | **Demostración integrada → Gantt → Dashboard** | Editar duración; verificar sucesoras y KPI después |
| 18. Demostración integrada: modificar recursos | **Demostración integrada → Recursos → Dashboard** | Cambiar cantidad u horas y comparar costo antes/después |
| 19. Demostración integrada: aprobar solicitud | **Demostración integrada → CAM-03** | Aprobar, revisar línea base y nuevos valores en Gantt/costos/tableros |
| 20. Documentación y pruebas | **docs + tests** | Mostrar README, guía de uso, código fuente y registros de prueba locales |

**Notas:** Es una matriz de correspondencia entre diseño/implementación de código y el requisito. No constituye por sí sola evidencia de que todas las funciones se probaron correctamente. Se requiere ejecutar la aplicación en Windows y registrar resultados reales.
