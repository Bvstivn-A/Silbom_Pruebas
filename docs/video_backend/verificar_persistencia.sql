-- SOLO LECTURA: ejecutar antes y después de la baja, en la BD de demostración.
-- psql solicita la contraseña sin incluirla en el comando.
-- Sustituir ID_CREADO y PLANTA_CONFIGURADA por los valores de la demo:
-- psql -h localhost -U silbom_app -d silbom_db \
--   -v maquina_id=ID_CREADO -v planta_id=PLANTA_CONFIGURADA \
--   -f docs/video_backend/verificar_persistencia.sql
-- Este archivo usa variables de psql; no pegarlo sin adaptar en pgAdmin.
BEGIN;
-- RLS requiere la misma planta configurada en el backend.
SELECT set_config('app.current_planta_id', :'planta_id', true);
SELECT id, nombre, horas_operativas, fallas_correctivas, mtbf, activa
FROM maquinas
WHERE id = :'maquina_id' AND planta_id = :'planta_id';
COMMIT;
