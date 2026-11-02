# Modelo de Datos Relacional - SILBOM (Planta San Joaquín)

Este documento describe la estructura de la base de datos relacional para el **Sistema Inteligente de Lectura y Bloqueo Operativo de Maquinarias (SILBOM)**, diseñado para la auditoría de procedimientos LOTO (Lockout / Tagout) bajo normativa chilena (Decreto Supremo 594 y DS 44).

---

## 1. Diagrama Entidad-Relación (Mermaid)

```mermaid
erDiagram
    MAQUINAS ||--o{ BLOQUEOS_LOTO : "tiene"
    
    MAQUINAS {
        varchar(20) id PK "Identificador industrial EQ-XXX-00"
        varchar(100) nombre "Nombre operativo del equipo"
        varchar(100) area "Área física en Planta San Joaquín"
        varchar(10) icono "Emoji o ícono identificador"
        numeric horas_operativas "Horas acumuladas de operación"
        int fallas_correctivas "Cantidad de fallas no programadas"
        numeric mtbf "MTBF calculado = horas / fallas"
        boolean bloqueada "Estado actual del equipo (Multi-LOTO)"
        int bloqueos_activos "Cantidad de técnicos interviniendo"
        varchar(50) planta_id "Aislamiento de planta (RLS)"
        timestamp fecha_creacion "Auditoría de alta"
        timestamp fecha_modificacion "Auditoría de última edición"
    }

    BLOQUEOS_LOTO {
        bigserial id PK "Identificador único autoincremental"
        varchar(20) maquina_id FK "Referencia a MAQUINAS.id"
        varchar(20) tipo_intervencion "CORRECTIVA o PREVENTIVA"
        varchar(250) motivo "Razón de la intervención sanitizada"
        varchar(80) nombre_tecnico "Nombre del operador o mantenedor"
        boolean confirmacion_candado_ds594 "Check legal obligatorio candado físico"
        varchar(20) estado "ACTIVO o CERRADO"
        timestamp hora_inicio "Timestamp de colocación del bloqueo"
        timestamp hora_fin "Timestamp de retiro de bloqueo"
        bigint duracion_minutos "Tiempo total de intervención"
        text notas_auditoria_cifradas "Notas confidenciales AES-256-GCM"
        varchar(50) planta_id "Aislamiento de planta (RLS)"
    }
```

---

## 2. Descripción de Entidades y Restricciones de Ciberseguridad

### Entidad: `MAQUINAS`
* **Propósito:** Catálogo maestro de equipos intervenibles.
* **Clave Primaria (`PK`):** `id` con restricción de expresión regular `CHECK (id ~ '^EQ-[A-Z]{2,4}-[0-9]{2}$')` (Control 14: Validación estricta en base de datos).
* **Multi-LOTO:** Los campos `bloqueada` y `bloqueos_activos` se manipulan conjuntamente de manera atómica (`@Transactional`). La máquina permanece en `bloqueada = true` mientras `bloqueos_activos > 0`.
* **Cálculo MTBF:** Se mantiene sincronizado mediante la regla: si `fallas_correctivas == 0`, `mtbf = horas_operativas`; en caso contrario, `mtbf = horas_operativas / fallas_correctivas` redondeado a 1 decimal.

### Entidad: `BLOQUEOS_LOTO`
* **Propósito:** Registro inmutable de eventos de desenergización y mantención.
* **Clave Foránea (`FK`):** `maquina_id` referencia a `maquinas(id)` con `ON DELETE RESTRICT` (Control de Integridad Referencial: una máquina no puede eliminarse mientras tenga registros asociados o bloqueos activos).
* **Cumplimiento DS 594:** Restricción `CHECK (confirmacion_candado_ds594 = TRUE)`. Ningún registro puede crearse si el operario no declara explícitamente haber puesto su candado mecánico físico.
* **Control 5 (Cifrado en reposo):** El campo `notas_auditoria_cifradas` almacena texto cifrado con el algoritmo estándar simétrico `AES-256-GCM`.
* **Control 4 (Row-Level Security):** Ambas tablas poseen activado `ROW LEVEL SECURITY` y filtran por `planta_id = 'PLANTA-SAN-JOAQUIN'`.
