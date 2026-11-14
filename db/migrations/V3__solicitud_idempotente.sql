-- Aditiva y repetible. Aplicar con db/schema.sql como propietario/migrador.
-- Los clientes anteriores pueden seguir enviando POST sin Idempotency-Key.
ALTER TABLE bloqueos_loto ADD COLUMN IF NOT EXISTS operacion_id VARCHAR(36);
ALTER TABLE bloqueos_loto ADD COLUMN IF NOT EXISTS operacion_hash VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bloqueo_solicitud
    ON bloqueos_loto(planta_id, tecnico_id, operacion_id)
    WHERE operacion_id IS NOT NULL;
