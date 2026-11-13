-- Esquema relacional de la aplicación. Idempotente: permite reiniciar sin borrar datos.
CREATE TABLE IF NOT EXISTS proyectos (
 id BIGSERIAL PRIMARY KEY, codigo VARCHAR(30) UNIQUE NOT NULL, nombre VARCHAR(220) NOT NULL,
 descripcion TEXT NOT NULL DEFAULT '', responsable VARCHAR(120) NOT NULL DEFAULT '',
 inicio DATE NOT NULL, fin DATE NOT NULL, presupuesto NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK(presupuesto>=0),
 estado VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADO', avance NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(avance BETWEEN 0 AND 100)
);
CREATE TABLE IF NOT EXISTS edt_items (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id),
 parent_id BIGINT REFERENCES edt_items(id), codigo VARCHAR(32) NOT NULL,
 nombre VARCHAR(220) NOT NULL, descripcion TEXT NOT NULL DEFAULT '',
 tipo VARCHAR(25) NOT NULL CHECK(tipo IN ('ETAPA','ACTIVIDAD','SUBACTIVIDAD')),
 responsable VARCHAR(120) NOT NULL DEFAULT '', inicio_base DATE NOT NULL, inicio DATE NOT NULL, duracion INT NOT NULL DEFAULT 5 CHECK (duracion>=1),
 fin DATE NOT NULL, inicio_real DATE, fin_real DATE,
 progreso NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (progreso BETWEEN 0 AND 100), estado VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE',
 UNIQUE(proyecto_id,codigo)
);
CREATE TABLE IF NOT EXISTS dias_no_laborales (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id), fecha DATE NOT NULL,
 descripcion VARCHAR(180), UNIQUE(proyecto_id,fecha)
);
CREATE TABLE IF NOT EXISTS dependencias (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id),
 predecesora_id BIGINT NOT NULL REFERENCES edt_items(id), sucesora_id BIGINT NOT NULL REFERENCES edt_items(id),
 tipo VARCHAR(2) NOT NULL CHECK(tipo IN ('FS','SS','FF','SF')),
 desfase INT NOT NULL DEFAULT 0, CHECK(predecesora_id<>sucesora_id), UNIQUE(predecesora_id,sucesora_id)
);
CREATE TABLE IF NOT EXISTS recursos (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id), codigo VARCHAR(30) NOT NULL,
 nombre VARCHAR(160) NOT NULL, tipo VARCHAR(30) NOT NULL,
 especialidad VARCHAR(130) NOT NULL DEFAULT '', tarifa_hora NUMERIC(14,2) NOT NULL DEFAULT 0,
 horas_disponibles NUMERIC(10,2) NOT NULL DEFAULT 40, costo_unidad NUMERIC(14,2) NOT NULL DEFAULT 0,
 cantidad_disponible NUMERIC(12,2) NOT NULL DEFAULT 1, unidad VARCHAR(30) NOT NULL DEFAULT 'unidad',
 UNIQUE(proyecto_id,codigo)
);
CREATE TABLE IF NOT EXISTS asignaciones (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id),
 item_id BIGINT NOT NULL REFERENCES edt_items(id), recurso_id BIGINT NOT NULL REFERENCES recursos(id),
 cantidad NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK(cantidad>=0), cantidad_real NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(cantidad_real>=0),
 horas_semana NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(horas_semana>=0), horas_semana_real NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(horas_semana_real>=0),
 semanas NUMERIC(10,2) NOT NULL DEFAULT 1 CHECK(semanas>=0), semanas_reales NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(semanas_reales>=0),
 UNIQUE(item_id,recurso_id)
);
CREATE TABLE IF NOT EXISTS hitos (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id), codigo VARCHAR(30) NOT NULL,
 descripcion VARCHAR(220) NOT NULL, responsable VARCHAR(120) NOT NULL DEFAULT '', fecha_comprometida DATE NOT NULL,
 fecha_real DATE, estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE', criterio TEXT NOT NULL DEFAULT '',
 evidencia TEXT NOT NULL DEFAULT '', UNIQUE(proyecto_id,codigo)
);
CREATE TABLE IF NOT EXISTS hito_actividades (
 hito_id BIGINT NOT NULL REFERENCES hitos(id) ON DELETE CASCADE,
 item_id BIGINT NOT NULL REFERENCES edt_items(id), PRIMARY KEY(hito_id,item_id)
);
CREATE TABLE IF NOT EXISTS kpis (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id), codigo VARCHAR(30) NOT NULL,
 nombre VARCHAR(200) NOT NULL, objetivo TEXT NOT NULL DEFAULT '', ambito VARCHAR(30) NOT NULL,
 formula VARCHAR(300) NOT NULL, variables TEXT NOT NULL DEFAULT '', unidad VARCHAR(30) NOT NULL DEFAULT '%',
 sentido VARCHAR(15) NOT NULL DEFAULT 'MAYOR', meta NUMERIC(14,3) NOT NULL DEFAULT 90,
 umbral NUMERIC(14,3) NOT NULL DEFAULT 75, periodicidad VARCHAR(30) NOT NULL DEFAULT 'Semanal',
 fuente VARCHAR(150) NOT NULL DEFAULT 'Sistema', responsable VARCHAR(120) NOT NULL DEFAULT 'Jefe de Proyecto',
 valor NUMERIC(16,3), semaforo VARCHAR(20) NOT NULL DEFAULT 'SIN_DATOS', activo BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(proyecto_id,codigo)
);
CREATE TABLE IF NOT EXISTS mediciones_kpi (
 id BIGSERIAL PRIMARY KEY, kpi_id BIGINT NOT NULL REFERENCES kpis(id), fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 valor NUMERIC(16,3) NOT NULL, semaforo VARCHAR(20) NOT NULL, meta NUMERIC(14,3) NOT NULL
);
CREATE TABLE IF NOT EXISTS cambios (
 id BIGSERIAL PRIMARY KEY, proyecto_id BIGINT NOT NULL REFERENCES proyectos(id), codigo VARCHAR(30) NOT NULL,
 solicitante VARCHAR(120) NOT NULL, fecha_solicitud DATE NOT NULL DEFAULT CURRENT_DATE,
 descripcion TEXT NOT NULL, justificacion TEXT NOT NULL DEFAULT '', tipo VARCHAR(30) NOT NULL,
 prioridad VARCHAR(15) NOT NULL DEFAULT 'MEDIA', riesgo VARCHAR(15) NOT NULL DEFAULT 'MEDIO',
 impacto TEXT NOT NULL DEFAULT '', responsable VARCHAR(120) NOT NULL DEFAULT '',
 estado VARCHAR(25) NOT NULL DEFAULT 'ABIERTO', fecha_comprometida DATE, fecha_real DATE,
 costo_estimado NUMERIC(16,2) NOT NULL DEFAULT 0, costo_real NUMERIC(16,2) NOT NULL DEFAULT 0,
 resultado TEXT NOT NULL DEFAULT '', item_id BIGINT REFERENCES edt_items(id),
 nueva_duracion INT, asignacion_id BIGINT REFERENCES asignaciones(id), nueva_cantidad NUMERIC(12,2),
 efectividad NUMERIC(5,2), adopcion NUMERIC(5,2),
 UNIQUE(proyecto_id,codigo)
);
CREATE TABLE IF NOT EXISTS etapas_cambio (
 id BIGSERIAL PRIMARY KEY, cambio_id BIGINT NOT NULL REFERENCES cambios(id) ON DELETE CASCADE,
 orden INT NOT NULL CHECK(orden BETWEEN 1 AND 10), nombre VARCHAR(100) NOT NULL, responsable VARCHAR(120) NOT NULL DEFAULT '',
 completada BOOLEAN NOT NULL DEFAULT FALSE, fecha DATE, evidencia TEXT NOT NULL DEFAULT '', UNIQUE(cambio_id,orden)
);
CREATE TABLE IF NOT EXISTS linea_base (
 id BIGSERIAL PRIMARY KEY, cambio_id BIGINT NOT NULL REFERENCES cambios(id),
 item_id BIGINT REFERENCES edt_items(id), campo VARCHAR(70) NOT NULL,
 antes VARCHAR(200) NOT NULL, despues VARCHAR(200) NOT NULL, fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_edt_proyecto ON edt_items(proyecto_id);
CREATE INDEX IF NOT EXISTS idx_kpi_mediciones ON mediciones_kpi(kpi_id,fecha);
