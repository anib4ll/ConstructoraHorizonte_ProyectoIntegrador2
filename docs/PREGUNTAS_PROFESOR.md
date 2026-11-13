# Preguntas y respuestas breves para la defensa

**¿Por qué no microservicios?** Porque los módulos de este caso comparten transacciones e información. Un monolito modular reduce el costo de integración y es suficiente para el alcance académico.

**¿Por qué PostgreSQL?** Es una base relacional persistente que permite integridad entre proyectos, actividades, recursos, hitos, indicadores y cambios.

**¿Para qué Angular?** Construir una interfaz de componentes con formularios, Gantt y tableros dinámicos que consultan una API REST.

**¿Qué hace Spring Boot?** Expone la API, valida datos y ejecuta reglas de negocio. El frontend no calcula la planificación ni los costos oficiales.

**¿Cuál es la diferencia entre EDT y Gantt?** La EDT organiza entregables y trabajo por jerarquía. El Gantt representa las fechas, duraciones y precedencias de ese trabajo.

**¿Por qué una tabla `edt_items`?** Las etapas, actividades y subactividades comparten campos. `tipo` y `parent_id` representan la jerarquía sin repetir tablas.

**¿Cómo evita duplicar costos?** Los costos se asignan a las subactividades y se suman hacia actividades/etapas, sin sumar otra vez los totales de los niveles padres.

**¿Qué es FS, SS, FF y SF?** FS: la sucesora comienza tras terminar la predecesora; SS: no puede comenzar antes del inicio de la predecesora; FF: su fin depende del fin de la predecesora; SF: su fin depende del inicio de la predecesora.

**¿Qué es una dependencia circular?** A depende de B y B (directa o indirectamente) depende de A. No se puede calcular un orden coherente y el backend rechaza la relación.

**¿Cómo se calculan costos?** Profesional = horas por semana × semanas × tarifa × cantidad. Material/equipo/licencia = cantidad × costo unitario.

**¿Cómo se configura un KPI?** Código, nombre, ámbito, fórmula, meta, sentido y umbral; el sistema evalúa variables registradas y asigna estado semáforo.

**¿Cómo se registra un cambio aprobado?** Se guardan los valores anteriores y nuevos en la línea base y se actualizan las duraciones/asignaciones, seguido del recálculo de planificación. Es trazabilidad antes/después.

**¿El sistema está listo para producción?** No. Es una implementación educativa; utiliza usuarios de demostración y autenticación básica en entorno local. Para producción hacen falta endurecimiento de seguridad, integración real con ERP y pruebas de carga.
