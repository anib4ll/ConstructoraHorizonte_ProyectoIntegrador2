# Estado real de validación del paquete

## Comprobaciones efectuadas en el entorno de generación
- Revisión de estructura del frontend y backend: archivos y módulos presentes.
- Analizador de TypeScript: los archivos `.ts` se analizaron sin errores de sintaxis.
- Compilación aislada con `javac` de `SimpleFormula` y `WorkdayMath`: correcta.
- Ejecución aislada de pruebas de cálculo de fórmula y fecha de término considerando feriados: correcta.
- Recuento estático en los datos de carga: 4 ETAPA, 12 ACTIVIDAD, 20 SUBACTIVIDAD, 4 tipos de precedencia, 6 hitos, 8 KPI proyecto, 12 KPI cambio y 5 solicitudes.
- Documento Word de matriz de correspondencia renderizado y revisado visualmente.

## Pendiente de ejecutar en Windows para concluir la validación
- Compilación del proyecto completo `backend` con `mvnw.cmd test` (necesita descargar dependencias Maven).
- Compilación del frontend con `npm install` + `npm run build` (necesita paquetes Angular).
- Conexión real del backend con PostgreSQL 16 ejecutado en Docker Desktop.
- Prueba de login con ambos roles y permisos de lectura/escritura.
- Prueba de CRUD de proyecto/EDT/recursos/hitos/KPI/cambios con persistencia después de recargar.
- Pruebas de dependencias FS/SS/FF/SF, desfases y ciclos.
- Comprobación de propagación de duración, asignación y CAM-03 en Gantt/costos/KPI/tableros.
- Capturas y registro de resultados de demostración para la evaluación académica.

**Conclusión:** El proyecto contiene código y documentación de todas las áreas solicitadas por la rúbrica, pero no se certifica como compilado y probado integralmente. Es imprescindible ejecutar las verificaciones anteriores antes de presentarlo como terminado y completamente funcional.
