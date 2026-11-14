# Diagnóstico inicial y alcance de la corrección móvil

Revisión del 13 de noviembre de 2026. Antes de editar se consultó el remoto:
`hito3_benja` seguía en `29a28b4b7404d81e17921cd88af7cee81abdc318`
(12 de noviembre). El árbol local estaba limpio. La rama predeterminada de
GitHub era `baseline/silbom-original`; esta corrección parte de Hito 3 y propone
integrarse en esa rama, sin modificar la rama predeterminada.

| Archivo en la versión inicial | Hallazgo | Acción |
|---|---|---|
| `src/main/resources/static/app/index.html` | POST y PUT caían a una simulación local tras errores de red. La liberación podía anunciar éxito sin confirmación. Datos demo y roles inferidos offline se mezclaban con la operación real. | Conservar el último estado confirmado, mostrar solicitudes pendientes y requerir confirmación del backend. |
| App móvil y `static/index.html` | Construían HTTP al puerto 8080 salvo un caso especial. | Cliente HTTP compartido con rutas relativas y servidor separado explícito. |
| `BloqueoService`, repositorio y esquema | Locks y transacciones protegían Multi-LOTO y el cierre repetido, pero un reintento de POST era un alta nueva. | Clave idempotente opcional, limitada a técnico/planta, serializada bajo el lock existente y respaldada por índice único. |
| Manifiesto y `app/sw.js` | Íconos SVG en data URL; caché de shell no resolvía correctamente una navegación offline con query. Activación eliminaba cachés ajenas. | PNG locales, caché solo de interfaz, conservación de query y limpieza limitada a SILBOM. |
| App móvil | `?maquina=` ya seleccionaba un equipo, pero faltaban casos inválidos/offline y pruebas que descartaran escrituras. | Validación, resolución solo desde catálogo confirmado y pruebas de navegación sin POST/PUT. |
| `SecurityConfig` | CORS admitía redes completas y el origen `null`. | Orígenes configurables; por defecto loopback y frontend HTTPS existente. Mismo origen no necesita CORS. |
| Workflow | Push automático limitado a otra rama; sin navegador. | Verificación de backend/PostgreSQL y suite de navegador en Hito 3 y la rama de corrección. |
| Copias HTML y APK | Varias copias se editaban por separado. | Fuentes canónicas y script de sincronización; el APK binario anterior no se presenta como validado. |

Se conserva el monolito Spring, el contrato previo de clientes sin cabecera
idempotente, las autorizaciones, las notas, las bajas lógicas y las reglas
Multi-LOTO. La migración V3 es aditiva y requiere aplicarse antes de arrancar
el JAR actualizado. No se cambia la liberación de tickets ajenos ni se agrega
una liberación privilegiada.

La identificación mediante NFC se prepara como un enlace NDEF URI HTTPS
abierto por el sistema operativo; QR usa exactamente el mismo enlace. No se
implementa una lectura Web NFC ni una acción de bloqueo al abrir una etiqueta.

La guía de puesta en marcha, pruebas, límites y criterios de aceptación está
en [PRUEBAS_MOVILES.md](PRUEBAS_MOVILES.md).
