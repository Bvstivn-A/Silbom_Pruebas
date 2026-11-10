# SILBOM · Guion de video y demostración del backend

**Objetivo:** 6:35 de contenido, con 25 segundos de margen hasta 7:00. Cuatro voces: Bastián, Belén, Benjamín y Vicente. Preparado el 10 de noviembre de 2026.

**Condición de la rúbrica:** A1 exige peticiones en vivo y sitúa una grabación en “En desarrollo”. Este material permite grabar una ejecución real, pero el video por sí solo no obtiene el nivel destacado de ejecución en vivo salvo que el docente autorice otro formato. A5 se demuestra al responder la pregunta aleatoria; el video no sustituye esa respuesta.

## 1. Reparto y cronómetro

| Tiempo | Voz | Qué se demuestra |
|---|---|---|
| 0:00–0:20 | Bastián | Presentación del problema |
| 0:20–0:55 | Bastián | 01 · Crear máquina — POST → 201 |
| 0:55–1:35 | Bastián | 02 · Leer máquina guardada — GET → 200 |
| 1:35–2:05 | Belén | 03 · Actualizar máquina — PUT → 200 |
| 2:05–2:25 | Belén | 04 · Leer actualización y comprobar PostgreSQL |
| 2:25–2:50 | Belén | 05 · Dar de baja — DELETE → 204 |
| 2:50–3:10 | Belén | 06 · Verificar baja — GET → 404; SQL activa=false |
| 3:10–3:55 | Benjamín | Código real: recorrido y motivo de la separación |
| 3:55–4:55 | Benjamín | Tecnologías: criterio, alternativa y costo |
| 4:55–6:20 | Vicente | Monolito: postura y condiciones del proyecto |
| 6:20–6:35 | Vicente | Cierre |

El texto hablado contiene aproximadamente 584 palabras. Las franjas incluyen acciones y cambios de pantalla: ensayar a ritmo natural, no leerlo todo de corrido para luego añadir los clics. Terminar el contenido en 6:35; no planificar hasta 7:00 exactos.

## 2. Guion literal y acciones

### 0:00–0:20 · Bastián · Presentación del problema

**Pantalla y acción:** Pantalla del equipo o título sencillo: SILBOM · Backend. Luego mostrar Postman con la API disponible.

**Qué decir:** “Soy Bastián. SILBOM registra intervenciones y bloqueos digitales en maquinaria. Mostraremos el backend funcionando, sus datos persistidos y nuestras decisiones técnicas. El registro digital complementa el candado físico; no verifica por sí solo el aislamiento de energía.”

### 0:20–0:55 · Bastián · 01 · Crear máquina — POST → 201

**Pantalla y acción:** Abrir 01 en la colección corta. Mostrar método, URL y JSON. Pulsar Send y señalar 201 Created, el ID generado y mtbf: null. No abrir Authorization.

**Qué decir:** “Creamos una máquina de demostración con ciento veinte horas y cero fallas. Enviamos un POST con JSON. El servidor valida los datos y responde doscientos uno: recurso creado. Sin fallas observadas, el MTBF aparece como nulo, porque todavía no es calculable.”

### 0:55–1:35 · Bastián · 02 · Leer máquina guardada — GET → 200

**Pantalla y acción:** Ejecutar 02 con el mismo {{maquinaId}}. Señalar 200 OK y los campos de la máquina. Dejar visible el ID para la consulta SQL preparada.

**Qué decir:** “Ahora consultamos el mismo identificador mediante una petición independiente. Recibimos doscientos y los datos enviados. No estamos mostrando una respuesta escrita a mano: Postman consulta el servicio en ejecución. Enseguida contrastaremos el registro directamente con PostgreSQL para comprobar la persistencia.”

### 1:35–2:05 · Belén · 03 · Actualizar máquina — PUT → 200

**Pantalla y acción:** Ejecutar 03: 150.0 horas y 2 fallas. Mostrar el cuerpo enviado, 200 y mtbf: 75.0 en la respuesta.

**Qué decir:** “Soy Belén. Actualizo las mediciones a ciento cincuenta horas y dos fallas mediante PUT. La respuesta es doscientos y el MTBF cambia a setenta y cinco horas por falla. El cálculo se hace en el backend; las horas son una medición declarada.”

### 2:05–2:25 · Belén · 04 · Leer actualización y comprobar PostgreSQL

**Pantalla y acción:** Ejecutar 04. Cambiar a la terminal SQL ya preparada, usar el ID recibido y ejecutar verificar_persistencia.sql. Mostrar horas_operativas=150.0, fallas_correctivas=2, mtbf=75.0 y activa=true.

**Qué decir:** “Otra consulta recupera los valores actualizados. En PostgreSQL vemos el mismo identificador, ciento cincuenta horas, dos fallas y MTBF setenta y cinco. Así comprobamos que el dato quedó guardado en el motor real.”

### 2:25–2:50 · Belén · 05 · Dar de baja — DELETE → 204

**Pantalla y acción:** Volver a Postman. Ejecutar 05. Señalar 204 No Content y el cuerpo vacío; no esperar un JSON de éxito.

**Qué decir:** “Ejecutamos DELETE sobre esa máquina de prueba. El servidor responde doscientos cuatro, sin cuerpo. Implementamos una baja lógica: retiramos el equipo del catálogo activo y conservamos las referencias históricas. No se permite retirar una máquina con bloqueos activos.”

### 2:50–3:10 · Belén · 06 · Verificar baja — GET → 404; SQL activa=false

**Pantalla y acción:** Ejecutar 06 y señalar 404. Repetir el mismo SELECT en PostgreSQL: la fila permanece y activa es false.

**Qué decir:** “El GET posterior devuelve cuatrocientos cuatro. En la base, la fila permanece con activa en falso. Eso confirma la baja lógica y completa crear, leer, actualizar y borrar del catálogo.”

### 3:10–3:55 · Benjamín · Código real: recorrido y motivo de la separación

**Pantalla y acción:** En el editor, abrir MaquinaController.crear, luego MaquinaService.crearMaquina y MaquinaRepository. Preparar las pestañas antes. Resaltar @Valid, la delegación, @Transactional y save; no recorrer todo el archivo.

**Qué decir:** “Soy Benjamín. En MaquinaController, Valid comprueba el DTO y el controlador delega al servicio. MaquinaService exige supervisor, normaliza los campos y guarda mediante el repositorio dentro de una transacción. El controlador devuelve doscientos uno. Separar estas responsabilidades evita mezclar HTTP con reglas del negocio y consultas. El DTO delimita los campos aceptados y devueltos; la planta se determina en el servidor, no en el JSON del cliente.”

### 3:55–4:55 · Benjamín · Tecnologías: criterio, alternativa y costo

**Pantalla y acción:** Mostrar pom.xml con Java 21 y Spring Boot; después el esquema PostgreSQL. Usar zoom legible. La explicación es la comparación; no leer dependencias completas.

**Qué decir:** “Elegimos Java veintiuno por su modelo de dominio tipado y records para los DTO. Node con TypeScript también era viable; asumimos instalar el JDK y compilar. Spring Boot reúne HTTP, validación, seguridad y transacciones. Frente a Express con bibliotecas separadas, reduce la integración manual, a cambio de más configuración y comprender sus proxies. PostgreSQL ofrece transacciones, claves foráneas y seguridad por filas integrada, que motivó elegirlo frente a MySQL para este diseño. Asumimos instalar el motor, administrar roles y aplicar migraciones. Estas elecciones priorizan integridad y trazabilidad, no una supuesta superioridad universal.”

### 4:55–6:20 · Vicente · Monolito: postura y condiciones del proyecto

**Pantalla y acción:** Mostrar el diagrama de arquitectura del informe o arquitectura_backend.png. Señalar cliente → seguridad → controller → service → repository → PostgreSQL. Mantenerlo visible mientras se justifica la decisión.

**Qué decir:** “Soy Vicente. Elegimos un monolito organizado por capas. Somos cuatro integrantes y desarrollamos un prototipo académico; la carga prevista son consultas y escrituras esporádicas de mantenimiento con los participantes del piloto, no tráfico industrial medido que justifique servicios independientes. Un proceso de backend y una base simplifican el despliegue y el costo de operación. Además, crear un ticket, actualizar el contador de máquina y registrar el evento deben confirmarse juntos en una transacción. Separarlos en microservicios agregaría llamadas de red, coordinación de fallos y observabilidad distribuida. Aceptamos que los módulos se despliegan juntos, que una caída del proceso afecta al servicio y que no podemos escalar cada parte por separado. Revisaríamos esta decisión si las mediciones mostraran necesidades distintas de escala o disponibilidad, o equipos autónomos. Las pruebas concurrentes verifican consistencia; no certifican capacidad de carga industrial.”

### 6:20–6:35 · Vicente · Cierre

**Pantalla y acción:** Mostrar una vista final del recorrido CRUD o el repositorio técnico en improvement/backend-hito2. Detener la grabación a más tardar en 6:45, sin créditos largos.

**Qué decir:** “Mostramos el CRUD, la persistencia y el recorrido del código, con tecnologías justificadas y sus costos. La integración del backend al repositorio oficial del equipo sigue pendiente.”

## 3. Peticiones y evidencia SQL

Importar [la colección corta](SILBOM_Demo_7min.postman_collection.json). Ejecutar cada petición con Send; no usar un resumen del Runner como único material de demostración. Mostrar método, ruta, cuerpo cuando corresponda, estado y respuesta real.

| Orden | Petición | Resultado que debe verse |
|---|---|---|
| 01 | POST /api/v1/maquinas | 201; ID nuevo; 120 horas; 0 fallas; mtbf=null. |
| 02 | GET /api/v1/maquinas/{{maquinaId}} | 200; mismo ID y datos del alta. |
| 03 | PUT /api/v1/maquinas/{{maquinaId}} | 200; 150 horas; 2 fallas; mtbf=75.0. |
| 04 | GET /api/v1/maquinas/{{maquinaId}} | 200; actualización recuperada; contrastar con SQL. |
| 05 | DELETE /api/v1/maquinas/{{maquinaId}} | 204; sin cuerpo. |
| 06 | GET /api/v1/maquinas/{{maquinaId}} | 404; SQL conserva la fila con activa=false. |

El PUT envía nombre, area, icono, horasOperativas y fallasCorrectivas; no es PATCH. Las respuestas 75 y 75.0 representan el mismo valor numérico. Las assertions comprueban códigos y datos, pero no sustituyen ver las respuestas.

Ejecutar [verificar_persistencia.sql](verificar_persistencia.sql) con psql después de 04 y de 06. Es una consulta de lectura; no borra ni altera máquinas. Sustituir los marcadores antes de grabar:

```bash
psql -h localhost -U silbom_app -d silbom_db \
  -v maquina_id=ID_CREADO -v planta_id=PLANTA_CONFIGURADA \
  -f docs/video_backend/verificar_persistencia.sql
```

RLS requiere la planta correcta mediante app.current_planta_id dentro de la transacción. No usar el propietario o superusuario como configuración normal de la API. El rol de aplicación puede consultar la fila inactiva cuando la planta coincide. Mantener el contexto y la contraseña fuera de la captura si contienen datos privados.

## 4. Preparación y grabación

1. Preparar una base de demostración PostgreSQL y una cuenta supervisora. Levantar el backend antes de grabar; no gastar los siete minutos instalando o compilando.
2. Importar SILBOM_Demo_7min.postman_collection.json. Fijar baseUrl con /api/v1 y credenciales propias en valores locales o Vault. No mostrar Authorization, el entorno de secretos ni .env en pantalla.
3. Ejecutar 01–06 manualmente y en orden durante el ensayo. El POST genera un ID nuevo cada vez; no reutilizar un ID dado de baja ni crear una variable de entorno maquinaId que oculte la variable de colección.
4. Preparar la terminal con psql y verificar_persistencia.sql. Usar el ID creado y la PLANTA_ID del backend. Introducir la contraseña fuera de la captura o mediante el administrador de credenciales local; nunca escribirla como argumento visible.
5. Ensayar el cambio de Postman a SQL después de 04 y después de 06. Usar el mismo ID. Antes del DELETE debe verse activa=true; después, false. No cambiar permisos ni desactivar RLS para que aparezcan filas.
6. Dejar abiertas las pestañas de código y el diagrama del informe. Aumentar el zoom hasta leer URL, JSON, código HTTP y SQL en una grabación de 1080p.
7. Grabar pantalla y voces reales del equipo. Identificar a cada integrante al iniciar su turno. Una sola captura continua de las peticiones ayuda a mostrar la secuencia; no sustituir las respuestas por capturas preparadas.
8. Ensayar con cronómetro a 6:35. El cambio de pestañas y los clics cuentan dentro del tiempo. Si superan 6:45, acortar comentarios repetidos; conservar CRUD, SQL, costos, arquitectura y código.
9. Exportar el video a MP4, 1920×1080 y audio claro. Revisar su duración real: como máximo 7:00. Evitar introducciones musicales y créditos que consuman el margen.
10. No llamar "microservicios" a paquetes ni afirmar frontend, QR/NFC o predicción implementados. El repositorio técnico es Bvstivn-A/Silbom_Pruebas, rama improvement/backend-hito2; la integración al repositorio oficial de Vicente sigue pendiente.

Si falla algo: 401 → revisar cuenta activa y Basic; 403 → revisar rol supervisor; 409 al crear → repetir 01 con un ID nuevo; 429 → esperar la ventana de cuotas. Si SQL no muestra la fila, revisar base, ID y planta. No editar la grabación para aparentar respuestas que no ocurrieron.

## 5. Ensayo de la pregunta aleatoria (A5)

Fuera del video: sortear una persona y una pregunta. Dar 30–45 segundos para señalar el método real, explicar qué hace y justificar por qué está diseñado así. Repetir con todos; no limitar a cada uno a su parte narrada. La asistencia de IA está declarada en el informe: no inventar autoría individual ni memorizar una explicación sin comprender el código.

### ¿Por qué el controlador delega al servicio?

El controlador traduce HTTP y activa @Valid. El servicio aplica permisos, reglas y transacciones. Así la regla no depende de una ruta concreta.

**Abrir:** MaquinaController.crear; MaquinaService.crearMaquina.

### ¿Qué hace @Transactional y qué no resuelve por sí sola?

Confirma o revierte el conjunto de cambios. Para impedir incrementos perdidos y dobles cierres concurrentes también se usan locks y validaciones de estado.

**Abrir:** MaquinaService; BloqueoService.liberarBloqueo.

### ¿Por qué DELETE no borra la fila?

Es una baja lógica: activa=false. El catálogo filtra recursos activos, mientras las referencias históricas se conservan. No equivale a eliminar físicamente los datos.

**Abrir:** MaquinaService.eliminarMaquina; Maquina.desactivar; MaquinaRepository.

### ¿Por qué MTBF es null sin fallas?

No puede dividirse entre cero ni inventarse un promedio. Con 150 horas y 2 fallas da 75.0. No predice exactamente una falla futura.

**Abrir:** Maquina.recalcularMtbf.

### ¿Por qué usar DTO si existe la entidad?

Define un contrato explícito, valida entradas y evita exponer o aceptar campos internos indiscriminadamente. Ser un record no vuelve inmutables todos los objetos que pudiera contener.

**Abrir:** MaquinaCreateDTO; MaquinaResponseDTO.

### ¿Cómo se demuestra que el dato quedó persistido?

Con una petición GET independiente y un SELECT al mismo PostgreSQL y a la misma planta. H2 en memoria o una captura no prueban ese recorrido real.

**Abrir:** Colección corta: 02 y 04; verificar_persistencia.sql.

### ¿Por qué los locks siguen Técnico → Máquina → Ticket?

Un orden común reduce interbloqueos. Se lee primero solo el ID de máquina para evitar conservar un ticket obsoleto durante la espera. Se valida dueño y estado antes de descontar.

**Abrir:** BloqueoService.liberarBloqueo.

### ¿Cómo evita duplicar fallas si intervienen dos técnicos?

Una referencia por máquina y planta agrupa los tickets del mismo incidente. Solo una referencia nueva incrementa el conteo correctivo.

**Abrir:** BloqueoService.crearBloqueo; FallaRepository.

### ¿Qué diferencia hay entre Basic, BCrypt y RLS?

Basic transporta la identidad y requiere TLS fuera de loopback; BCrypt comprueba la contraseña mediante un hash; RLS filtra filas según el contexto de planta. Son controles distintos.

**Abrir:** SecurityConfig; PlantaTransactionAspect; migración V2.

### ¿Por qué monolito y cuándo cambiarían la decisión?

Cuatro integrantes, alcance académico, un proceso y necesidad transaccional. Considerar distribución ante mediciones de escala/disponibilidad independiente o equipos autónomos; no por moda.

**Abrir:** Arquitectura del informe y paquetes del backend.

## 6. Cobertura de la rúbrica

| Criterio | Evidencia preparada | Condición para el nivel destacado |
|---|---|---|
| A1 · 15 | 01–06, estados y SQL antes/después. | Ejecutar en vivo; una grabación no equivale al destacado descrito. |
| A2 · 12 | 3:55–4:55: Java/Spring/PostgreSQL, alternativas y costo asumido. | Explicar criterios aplicados, no solo nombrar herramientas. |
| A3 · 8 | 4:55–6:20: monolito, carga, cuatro personas, operación y despliegue. | Argumentar con el contexto real y reconocer los costos. |
| A4 · 5 | Cuatro voces, orden y objetivo 6:35. | Ensayar y verificar duración final ≤7:00. |
| A5 · 10 | Recorrido de código 3:10–3:55 y preguntas de ensayo. | El integrante sorteado explica qué hace el código y por qué; el video no acredita esta respuesta. |

## 7. Fuentes y registro del material

- Rúbrica A1–A5 suministrada por el equipo en esta conversación.
- Backend revisado: revisión ab2254b (sin cambios funcionales frente a 1473951), rama improvement/backend-hito2.
- [Informe editable del Hito 2](../SILBOM_Informe_Hito2.docx), README y código fuente.
- [Diagrama para mostrar en pantalla](arquitectura_backend.png).
- [MaquinaController.crear](../../src/main/java/cl/silbom/backend/controller/MaquinaController.java); [MaquinaService](../../src/main/java/cl/silbom/backend/service/MaquinaService.java); [MaquinaRepository](../../src/main/java/cl/silbom/backend/repository/MaquinaRepository.java).
- [Reglas de máquina](../../src/main/java/cl/silbom/backend/model/Maquina.java); [BloqueoService](../../src/main/java/cl/silbom/backend/service/BloqueoService.java); [contexto RLS](../../src/main/java/cl/silbom/backend/config/PlantaTransactionAspect.java).
- Asistencia de IA: Codex redactó el guion y preparó la colección; las rutas, campos y decisiones se contrastaron con el código. El equipo debe ensayar, comprender y grabar sus propias voces.
- Esta entrega prepara material de grabación; no contiene un video ni evidencia de la defensa oral. No cambia código de producción, esquema, permisos ni el informe anterior.

Ver [VALIDACION.md](VALIDACION.md) para el ensayo técnico de la colección y SQL.
