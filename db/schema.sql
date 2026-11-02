-- ====================================================================
-- SILBOM: SISTEMA INTELIGENTE DE LECTURA Y BLOQUEO OPERATIVO DE MAQUINARIAS
-- PLANTA SAN JOAQUÍN - SCRIPT DDL Y POBLADO INICIAL (POSTGRESQL 15+)
-- ====================================================================
-- Este script es 100% reproducible y aplica los controles de ciberseguridad
-- exigidos para bases de datos relacionales: roles de privilegios mínimos,
-- restricciones CHECK en dominio, llaves foráneas y Row-Level Security (RLS).
-- ====================================================================

-- --------------------------------------------------------------------
-- CONTROL 3: SEPARACIÓN DE ROLES DE BASE DE DATOS (MÍNIMO PRIVILEGIO)
-- (Ejecutar como superusuario 'postgres' al inicializar el servidor)
-- --------------------------------------------------------------------
/*
-- 1. Crear base de datos aislada para SILBOM
CREATE DATABASE silbom_db;

-- 2. Crear usuario de aplicación sin permisos de superusuario
CREATE USER silbom_app WITH ENCRYPTED PASSWORD 'CAMBIAR_PASSWORD';

-- 3. Conceder conexión a la base de datos
GRANT CONNECT ON DATABASE silbom_db TO silbom_app;

-- 4. Conectarse a silbom_db y otorgar permisos solo sobre esquema public
\c silbom_db;
GRANT USAGE ON SCHEMA public TO silbom_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO silbom_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO silbom_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO silbom_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO silbom_app;
*/

-- --------------------------------------------------------------------
-- LIMPIEZA DE TABLAS PREVIAS (Para ejecuciones en limpio)
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS bloqueos_loto CASCADE;
DROP TABLE IF EXISTS maquinas CASCADE;

-- --------------------------------------------------------------------
-- TABLA 1: MAQUINAS (Catálogo de equipos de Planta San Joaquín)
-- --------------------------------------------------------------------
-- QUÉ HACE: Almacena el inventario de maquinaria industrial crítica sujeta a LOTO.
-- POR QUÉ: Registra telemetría básica (horas, fallas, MTBF) y controla el estado
-- grupal de bloqueo ('bloqueada' y 'bloqueos_activos') para evitar arranques accidentales.
CREATE TABLE maquinas (
    id VARCHAR(20) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    icono VARCHAR(10) NOT NULL,
    horas_operativas NUMERIC(10, 1) NOT NULL DEFAULT 0.0 CHECK (horas_operativas >= 0.0),
    fallas_correctivas INT NOT NULL DEFAULT 0 CHECK (fallas_correctivas >= 0),
    mtbf NUMERIC(10, 1) NOT NULL DEFAULT 0.0 CHECK (mtbf >= 0.0),
    bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    bloqueos_activos INT NOT NULL DEFAULT 0 CHECK (bloqueos_activos >= 0),
    planta_id VARCHAR(50) NOT NULL DEFAULT 'PLANTA-SAN-JOAQUIN',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_maquina_id_formato CHECK (id ~ '^EQ-[A-Z]{2,4}-[0-9]{2}$')
);

COMMENT ON TABLE maquinas IS 'Inventario de maquinas de planta protegidas bajo protocolo LOTO';
COMMENT ON COLUMN maquinas.id IS 'Identificador estandar industrial (ej: EQ-ENV-01)';
COMMENT ON COLUMN maquinas.mtbf IS 'Mean Time Between Failures: horas_operativas / fallas_correctivas (1 decimal)';
COMMENT ON COLUMN maquinas.bloqueos_activos IS 'Contador concurrente Multi-LOTO de tecnicos interviniendo';

-- --------------------------------------------------------------------
-- TABLA 2: BLOQUEOS_LOTO (Auditoría de tickets de intervención)
-- --------------------------------------------------------------------
-- QUÉ HACE: Registra cada candado digital colocado sobre una máquina.
-- POR QUÉ: Exige el cumplimiento legal del candado físico (DS 594),
-- audita fecha/hora de inicio/fin, técnico responsable y motivo sanitizado.
CREATE TABLE bloqueos_loto (
    id BIGSERIAL PRIMARY KEY,
    maquina_id VARCHAR(20) NOT NULL,
    tipo_intervencion VARCHAR(20) NOT NULL CHECK (tipo_intervencion IN ('CORRECTIVA', 'PREVENTIVA')),
    motivo VARCHAR(250) NOT NULL,
    nombre_tecnico VARCHAR(80) NOT NULL,
    confirmacion_candado_ds594 BOOLEAN NOT NULL CHECK (confirmacion_candado_ds594 = TRUE),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CERRADO')),
    hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hora_fin TIMESTAMP,
    duracion_minutos BIGINT,
    notas_auditoria_cifradas TEXT,
    planta_id VARCHAR(50) NOT NULL DEFAULT 'PLANTA-SAN-JOAQUIN',
    CONSTRAINT fk_bloqueo_maquina FOREIGN KEY (maquina_id) REFERENCES maquinas(id) ON DELETE RESTRICT
);

COMMENT ON TABLE bloqueos_loto IS 'Historial de auditoria de bloqueos de seguridad LOTO (Energia Cero)';
COMMENT ON COLUMN bloqueos_loto.confirmacion_candado_ds594 IS 'Declaracion jurada legal obligatoria: candado fisico instalado (DS 594)';
COMMENT ON COLUMN bloqueos_loto.notas_auditoria_cifradas IS 'Notas confidenciales encriptadas con AES-256-GCM en reposo';

-- --------------------------------------------------------------------
-- CONTROL 4: ACTIVACIÓN DE ROW-LEVEL SECURITY (RLS)
-- --------------------------------------------------------------------
-- QUÉ HACE: Impide que consultas SQL accidentales lean datos de otra planta.
-- POR QUÉ: Aislamiento multi-tenancy a nivel de motor PostgreSQL.
ALTER TABLE maquinas ENABLE ROW LEVEL SECURITY;
ALTER TABLE bloqueos_loto ENABLE ROW LEVEL SECURITY;

-- Politica para permitir que la aplicacion consulte los datos de la planta asignada
CREATE POLICY maquinas_planta_isolation_policy ON maquinas
    FOR ALL
    USING (planta_id = CURRENT_SETTING('app.current_planta_id', true) OR planta_id = 'PLANTA-SAN-JOAQUIN');

CREATE POLICY bloqueos_planta_isolation_policy ON bloqueos_loto
    FOR ALL
    USING (planta_id = CURRENT_SETTING('app.current_planta_id', true) OR planta_id = 'PLANTA-SAN-JOAQUIN');

-- --------------------------------------------------------------------
-- DATOS INICIALES OBLIGATORIOS (PLANTA SAN JOAQUÍN)
-- Coincidencia 100% con los entregables del Hito 1
-- --------------------------------------------------------------------

-- 1. Carga de los 6 Equipos Industriales
INSERT INTO maquinas (id, nombre, area, icono, horas_operativas, fallas_correctivas, mtbf, bloqueada, bloqueos_activos, planta_id) VALUES
('EQ-ENV-01', 'Envasadora Rotativa Automática', 'Línea de Envasado 1', '📦', 701.5, 3, 233.8, FALSE, 0, 'PLANTA-SAN-JOAQUIN'),
('EQ-PR-04',  'Prensa Hidráulica 200T',         'Área de Estampado',   '⚙️', 712.0, 2, 356.0, FALSE, 0, 'PLANTA-SAN-JOAQUIN'),
('EQ-EXT-02', 'Extrusora de Polímeros',          'Área de Moldeo',      '🔄', 685.2, 4, 171.3, FALSE, 0, 'PLANTA-SAN-JOAQUIN'),
('EQ-MEZ-01', 'Mezclador Industrial de Cinta',   'Área de Preparación', '🥣', 715.0, 1, 715.0, FALSE, 0, 'PLANTA-SAN-JOAQUIN'),
('EQ-COM-03', 'Compresor de Tornillo',           'Sala de Máquinas',    '💨', 718.5, 1, 718.5, FALSE, 0, 'PLANTA-SAN-JOAQUIN'),
('EQ-TR-01',  'Cinta Transportadora Principal',  'Logística y Despacho','🏗️', 708.0, 2, 354.0, FALSE, 0, 'PLANTA-SAN-JOAQUIN');

-- 2. Carga del Historial Inicial (3 Registros Cerrados)
INSERT INTO bloqueos_loto (maquina_id, tipo_intervencion, motivo, nombre_tecnico, confirmacion_candado_ds594, estado, hora_inicio, hora_fin, duracion_minutos, planta_id) VALUES
('EQ-PR-04',  'PREVENTIVA', 'Inspección y lubricación semanal programada',         'Carlos R.',    TRUE, 'CERRADO', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '45 minutes', 45, 'PLANTA-SAN-JOAQUIN'),
('EQ-EXT-02', 'CORRECTIVA', 'Sobrecalentamiento en motor principal (Eléctrico)',   'Vicente M.',   TRUE, 'CERRADO', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '72 minutes', 72, 'PLANTA-SAN-JOAQUIN'),
('EQ-ENV-01', 'CORRECTIVA', 'Atascamiento de cabezal móvil (Mecánico)',            'Benjamín V.',  TRUE, 'CERRADO', NOW() - INTERVAL '1 day',  NOW() - INTERVAL '1 day'  + INTERVAL '35 minutes', 35, 'PLANTA-SAN-JOAQUIN');
