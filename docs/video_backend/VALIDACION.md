# Registro del material para el video del backend

Preparado y ensayado el **10 de noviembre de 2026**. Base técnica: revisión
`ab2254b` de `improvement/backend-hito2`; el código funcional conserva la
implementación validada del informe del Hito 2.

## Archivos y propósito

| Archivo | Qué hace |
|---|---|
| `GUION_VIDEO_BACKEND_7_MIN.md` | Guion literal, tiempos, acciones, preparación, cobertura A1–A5 y preguntas para ensayar. |
| `SILBOM_Guion_Video_Backend_7min.docx` | Versión editable: cronograma, una página por integrante y material de ensayo aparte. |
| `SILBOM_Demo_7min.postman_collection.json` | Seis peticiones manuales para crear, leer, actualizar, verificar, dar de baja y comprobar el 404. Incluye dos tests por petición. |
| `verificar_persistencia.sql` | Consulta de lectura para contrastar el registro de PostgreSQL antes y después de la baja. Usa variables de psql y contexto local de planta para RLS. |
| `arquitectura_backend.png` | Diagrama del backend construido, reutilizado del informe para la explicación del monolito y sus capas. |
| `VALIDACION.md` | Registro de los cambios documentales y del ensayo técnico. |

El pre-script de la petición 01 genera un identificador con el patrón
`EQ-AAAA-00`, con letras y dígitos aleatorios, y lo conserva como variable de
colección para las siguientes peticiones. Cada POST genera otro identificador:
los IDs de máquinas dadas de baja no se reutilizan. Los scripts se ejecutan
dentro de una función para evitar redeclaraciones entre evaluaciones del
runtime. No se exportan credenciales: las variables de usuario y contraseña
están vacías y deben configurarse localmente.

## Ensayo ejecutado

Se inició temporalmente el JAR en loopback contra PostgreSQL real. Se ejecutó
**esta colección** con Newman, con una identidad supervisora local, y se
comprobó la fila mediante psql con el rol de aplicación. La instancia temporal
se detuvo al terminar. Se usaron datos ficticios de demostración.

| Paso | Método | HTTP observado | Dato comprobado |
|---|---|---|---|
| 01 | POST | 201 | ID generado, 120 horas y MTBF nulo. |
| 02 | GET | 200 | Mismo ID, 120 horas y cero fallas. |
| 03 | PUT | 200 | 150 horas, 2 fallas y MTBF 75.0. |
| 04 | GET | 200 | Actualización recuperada. SQL confirmó 150 horas, 2 fallas, MTBF 75.0 y `activa=true`. |
| 05 | DELETE | 204 | Respuesta sin cuerpo. |
| 06 | GET | 404 | SQL confirmó que la fila permanece con los mismos datos y `activa=false`. |

**Resultado:** 6 peticiones, 12 assertions, 0 fallos. La baja lógica conserva la
fila de demostración. No se alteraron máquinas originales ni se borró historial
LOTO. No se publican logs con cabeceras Authorization o valores de `.env`.

Se revisó el Word renderizado: nueve páginas, cronograma completo, una página
por participante, sin páginas vacías ni desbordamientos de texto. El texto
hablado tiene aproximadamente 584 palabras; los tiempos asignados incluyen
clics, lectura de respuestas y cambios de pantalla.

## Alcance de esta entrega

- Se añaden material de presentación y un enlace desde README. No se cambia
  código de producción, esquema, permisos ni el informe anterior.
- El objetivo del guion es **6:35**, con margen hasta 7:00. No se grabó un video
  ni se midió la duración de las voces del equipo: deben cronometrar el ensayo
  y comprobar el MP4 final.
- Las 49 pruebas del informe se ejecutaron el 9 de noviembre; no se presentan
  como una nueva ejecución en este ensayo.
- A1 exige demostración en vivo y A5 respuesta del integrante sorteado. Un
  video no acredita por sí solo esas dos condiciones de la rúbrica.
- La integración al repositorio oficial `Vicente-PR/SILBOM` sigue pendiente.
  Los materiales se guardan en el repositorio técnico y su rama de mejoras.
- Codex asistió en la redacción y preparación de la colección, contrastando
  las rutas, campos y decisiones con el código y ejecutando el ensayo descrito.
  La comprensión y la grabación de las voces corresponden al equipo.
