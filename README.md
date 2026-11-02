# SILBOM - Sistema Inteligente de Lectura y Bloqueo Operativo de Maquinaria
### Backend REST API - Planta San Joaquín (Hito 2)

---

## 👥 Equipo de Trabajo
* **Vicente** - GitHub: [@Vicente-PR](https://github.com/Vicente-PR) (Coordinador)
* **Bastián** - GitHub: [@Bvstivn-A](https://github.com/Bvstivn-A) (Responsable Documento)
* **Belén** - GitHub: [@lenbbee](https://github.com/lenbbee) (Responsable Prototipo)
* **Benjamín** - GitHub: [@BenjaHK](https://github.com/BenjaHK) (Responsable Repositorio)

---

## 📖 1. Contexto del Proyecto y Dominio LOTO

SILBOM digitaliza la auditoría del protocolo de seguridad industrial **LOTO (Lockout / Tagout)** mediante etiquetas pasivas NFC/QR adheridas a las máquinas de la **Planta San Joaquín**, garantizando el estricto cumplimiento del **Decreto Supremo 594** y **DS 44** de la legislación chilena.

### Problemas Críticos Industriales que Resuelve
1. **Control en "Energía Cero":** Funciona cuando la maquinaria está totalmente desenergizada (apagada), auditando el estado operativo mediante el smartphone del técnico de mantenimiento.
2. **Bloqueo Grupal Concurrente (Multi-LOTO):** Si múltiples técnicos (`bloqueosActivos`) intervienen un equipo, la máquina **DEBE** permanecer en estado `bloqueada = true` hasta que el último técnico retire su candado (`bloqueosActivos == 0`).
3. **Cálculo Dinámico de MTBF (Mean Time Between Failures):**
   $$\text{MTBF} = \text{RedondearAUnDecimal}\left(\frac{\text{horasOperativas}}{\text{fallasCorrectivas}}\right)$$
   *(Si $\text{fallasCorrectivas} = 0$, $\text{MTBF} = \text{horasOperativas}$)*.
   * Intervención `CORRECTIVA` (avería no programada): Suma +1 a `fallasCorrectivas` y penaliza el MTBF.
   * Intervención `PREVENTIVA` (mantención programada): **NO** altera las fallas ni penaliza el MTBF.
4. **Cumplimiento Legal DS 594:** El backend rechaza (`400 Bad Request`) cualquier bloqueo si el usuario no declara haber instalado el candado físico mecánico (`confirmacionCandadoDs594 = true`).

---

## 🏗️ 2. Arquitectura de Software (Monolito en 3 Capas)

* **Lenguaje:** Java 21 (LTS)
* **Framework:** Spring Boot 3.3.5 (Maven)
* **Motor de Base de Datos:** PostgreSQL 17 / 16 (Persistencia permanente en disco)
* **Paquete Base:** `cl.silbom.backend`

```
cl.silbom.backend
├── controller   # REST Controllers (@Valid, ResponseEntity, Códigos HTTP exactos, CERO lógica)
├── service      # Lógica de Negocio (Multi-LOTO, MTBF, validación legal, @Transactional)
├── repository   # Spring Data JPA con consultas parametrizadas (Anti-SQLi)
├── model        # Entidades JPA (Encapsulamiento estricto, métodos de dominio)
├── dto          # Objetos de Transferencia Inmutables (Java Records)
├── security     # Filtros de Ciberseguridad (Rate Limit, Headers, API Key, AES-256-GCM)
├── exception    # GlobalExceptionHandler (@RestControllerAdvice sin exponer stack traces)
└── config       # DataInitializer con seed inicial de Planta San Joaquín
```

---

## 🛡️ 3. Los 20 Controles de Ciberseguridad Backend Implementados

| # | Control de Seguridad | Implementación en SILBOM | Ubicación en el Código |
|---|---|---|---|
| **1** | **Ocultar credenciales** | Variables de entorno con defaults seguros | `src/main/resources/application.properties` |
| **2** | **Eliminación de secretos en Git** | `.gitignore` estricto y plantilla `.env.example` | `.gitignore`, `.env.example` |
| **3** | **Separación de roles DB** | Rol `silbom_app` con permisos mínimos (sin superusuario) | `db/schema.sql` |
| **4** | **Row-Level Security (RLS)** | Políticas PostgreSQL por `planta_id` | `db/schema.sql`, `MaquinaRepository.java` |
| **5** | **Encriptar datos en reposo** | AES-256-GCM con Vector de Inicialización (IV) dinámico | `CryptoUtils.java` |
| **6** | **Forzar autenticación** | `ApiKeyAuthFilter` en rutas de mutación (`POST, PUT, DELETE`) | `ApiKeyAuthFilter.java` |
| **7** | **Restringir acceso (Anti-IDOR)** | Validación de pertenencia por `planta_id` en Service | `MaquinaService.java`, `BloqueoService.java` |
| **8** | **Encapsulamiento y Anti-Mass Assignment** | Atributos `private`, DTOs `record`, sin `@Setter` públicos | `Maquina.java`, `BloqueoLoto.java` |
| **9** | **Proteger Session Cookies** | `HttpOnly=true`, `Secure=true`, `SameSite=Strict` | `application.properties` |
| **10** | **Hashear contraseñas** | `BCryptPasswordEncoder` con factor de costo 12 | `SecurityConfig.java` |
| **11** | **Rate Limiting** | Ventana de 60 seg en memoria (máx. 60 peticiones/minuto) | `RateLimitFilter.java` |
| **12** | **Protección contra bots** | Validación de User-Agent y campos trampa honeypot | `RateLimitFilter.java`, `*CreateDTO.java` |
| **13** | **Parametrizar queries** | JPA y consultas `@Query` con `:param` (Anti-SQLi) | `MaquinaRepository.java`, `BloqueoLotoRepository.java` |
| **14** | **Validar inputs en la frontera** | `@NotBlank`, `@Size`, `@Pattern`, `@AssertTrue` en DTOs | Paquete `cl.silbom.backend.dto` |
| **15** | **Sanitizar contenido** | Limpieza regex de etiquetas `<script>` y HTML malicioso | `InputSanitizer.java` |
| **16** | **Restringir archivos subidos** | `spring.servlet.multipart.enabled=false` | `application.properties` |
| **17** | **Devolver solo datos necesarios** | DTOs de salida y `server.error.include-stacktrace=never` | `GlobalExceptionHandler.java` |
| **18** | **Security Headers y CORS** | Cabeceras HSTS, CSP, X-Frame-Options y orígenes cerrados | `SecurityHeadersFilter.java`, `SecurityConfig.java` |
| **19** | **Forzar HTTPS** | `Strict-Transport-Security: max-age=31536000; includeSubDomains` | `SecurityHeadersFilter.java` |
| **20** | **Escanear dependencias** | Dependencias oficiales y comando de auditoría | `pom.xml`, Sección 5 de esta guía |

---

## 🚀 4. Guía de Puesta en Marcha (Paso a Paso)

### 4.1 Requisitos Previos
* **Java Development Kit (JDK):** Versión 21 (o 17+).
* **Apache Maven:** Versión 3.9+ (ya configurado en el sistema o incluido en PATH).
* **PostgreSQL:** Versión 15, 16 o 17 corriendo en el puerto `5432`.

### 4.2 Configuración de la Base de Datos PostgreSQL
1. Abre tu terminal de PostgreSQL (`psql`) o interfaz gráfica (pgAdmin / DBeaver) como superusuario `postgres`:
```sql
CREATE DATABASE silbom_db;
CREATE USER silbom_app WITH ENCRYPTED PASSWORD 'CAMBIAR_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE silbom_db TO silbom_app;
```
2. Ejecuta el script DDL y los datos iniciales de Planta San Joaquín:
```bash
psql -U postgres -d silbom_db -f db/schema.sql
```

### 4.3 Configurar Variables de Entorno
Crea tu archivo `.env` en la raíz copiando la plantilla:
```bash
cp .env.example .env
```
*(Si usas los valores por defecto locales de `application.properties`, no es obligatorio editar nada para desarrollo)*.

### 4.4 Compilar y Ejecutar Pruebas Automatizadas
Para verificar la compilación y ejecutar los tests unitarios y de integración:
```bash
mvn clean test
```

### 4.5 Iniciar el Servidor Backend
Ejecuta la aplicación Spring Boot:
```bash
mvn spring-boot:run
```
O empaqueta el JAR ejecutable:
```bash
mvn clean package
java -jar target/silbom-backend-1.0.0.jar
```
El servidor quedará escuchando en `http://localhost:8080`.

---

## 🔍 5. Auditoría de Dependencias (Control 20)

Para auditar y verificar que no existan vulnerabilidades conocidas (CVEs) en las librerías del proyecto:
```bash
# Verificar actualizaciones disponibles
mvn versions:display-dependency-updates

# Escaneo de vulnerabilidades con el plugin oficial de OWASP (opcional)
mvn org.owasp:dependency-check-maven:check
```

---

## 🧪 6. Demostración en Vivo en Postman (7 Minutos)

Importa el archivo [`docs/SILBOM_Postman_Collection.json`](docs/SILBOM_Postman_Collection.json) en Postman.

### Flujo Demostrativo Recomendado:
1. **Petición 1.1:** `GET /api/v1/maquinas` -> Muestra las 6 máquinas base de Planta San Joaquín con sus horas, fallas y MTBF.
2. **Petición 1.5:** `POST /api/v1/maquinas` (Sin API Key) -> Demuestra el **Control 6 (401 Unauthorized)**.
3. **Petición 2.2:** `POST /api/v1/bloqueos` (con `confirmacionCandadoDs594: false`) -> Demuestra el rechazo **400 Bad Request** por incumplimiento del **DS 594**.
4. **Petición 2.3:** `POST /api/v1/bloqueos` (Técnico 1, `CORRECTIVA`) -> La máquina `EQ-ENV-01` pasa a `bloqueada = true`, suma +1 falla y su MTBF se recalcula automáticamente de 233.8 a 175.4.
5. **Petición 2.4:** `POST /api/v1/bloqueos` (Técnico 2, `PREVENTIVA`) -> **Multi-LOTO:** El contador `bloqueosActivos` sube a 2. Al ser preventiva, el MTBF no se penaliza.
6. **Petición 2.5:** `PUT /api/v1/bloqueos/4/liberar` -> El Técnico 1 libera su ticket. **La máquina SIGUE bloqueada** (`bloqueosActivos = 1`) para proteger la vida del Técnico 2.
7. **Petición 2.6:** `PUT /api/v1/bloqueos/5/liberar` -> El Técnico 2 libera su ticket. Al llegar `bloqueosActivos` a 0, la máquina finalmente se declara **desbloqueada** (`bloqueada = false`).
8. **Petición 2.7:** `DELETE /api/v1/bloqueos/X` (Bloqueo ACTIVO) -> Demuestra que la seguridad industrial prohíbe eliminar bloqueos en curso (**409 Conflict**).

---

## 🎓 7. Preguntas de Defensa Técnica (Interrogación Individual)

### ¿Por qué las entidades JPA no tienen setters públicos ni Lombok `@Data`?
> **Respuesta:** Para aplicar el **Control 8 (Anti-Mass Assignment y Encapsulamiento Estricto)**. Si existieran setters como `setBloqueada(false)` o `setMtbf(10.0)`, un desarrollador o una deserialización mal configurada podrían desbloquear una máquina con operarios adentro o falsificar métricas de confiabilidad. Los cambios de estado ocurren exclusivamente mediante métodos de dominio con validación interna (`registrarEntradaBloqueo`, `registrarSalidaBloqueo`).

### ¿Cómo opera el protocolo Multi-LOTO en caso de intervenciones concurrentes?
> **Respuesta:** Se modela a través de la variable `bloqueosActivos` en la entidad `Maquina`. Cada bloqueo exitoso incrementa el contador en +1 y activa `bloqueada = true`. Cuando un técnico concluye su labor, se descuenta 1. La máquina **solo y únicamente** pasa a `bloqueada = false` cuando `bloqueosActivos == 0`. Si 3 técnicos intervienen y 2 salen, la máquina debe permanecer energéticamente bloqueada para evitar mutilaciones o muertes de trabajadores.

### ¿Cuál es la diferencia entre una intervención CORRECTIVA y una PREVENTIVA en el MTBF?
> **Respuesta:** La fórmula de confiabilidad industrial es $\text{MTBF} = \frac{\text{horasOperativas}}{\text{fallasCorrectivas}}$. Una intervención `PREVENTIVA` es una actividad programada de rutina (lubricación, calibración) y no representa una interrupción por falla, por lo que **no penaliza el indicador**. Una intervención `CORRECTIVA` ocurre por rotura o avería no prevista, por lo que suma +1 al contador de fallas y reduce el tiempo medio entre fallas.

### ¿Cómo se valida legalmente el Decreto Supremo 594 en el Backend?
> **Respuesta:** En la capa web, el DTO `BloqueoCreateDTO` incluye el atributo `@NotNull @AssertTrue Boolean confirmacionCandadoDs594`. Si el cliente HTTP intenta registrar un bloqueo enviando `false` o `null`, el validador de Jakarta detiene el flujo antes de ingresar a la lógica de negocio y responde `400 Bad Request`. Además, en la base de datos se implementa una restricción `CHECK (confirmacion_candado_ds594 = TRUE)`.

### ¿Cómo se evitan ataques de IDOR (Insecure Direct Object References)?
> **Respuesta:** Aplicando el **Control 7** y **Control 4 (Row-Level Security)**. En el servicio Java, cada consulta busca por `(id, plantaId)`. De este modo, un usuario de una planta no puede leer ni manipular registros de otra planta simplemente adivinando el ID numérico del recurso.
