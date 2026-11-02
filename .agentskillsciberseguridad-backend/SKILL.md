---
name: ciberseguridad-backend
description: Auditar, disenar y blindar codigo backend y APIs REST aplicando 20 controles esenciales de ciberseguridad. Usar cuando el usuario pida escribir codigo seguro, revisar vulnerabilidades, proteger endpoints, configurar autenticacion, validar entradas, proteger bases de datos o aplicar buenas practicas de seguridad en proyectos backend como Java Spring Boot, Node.js o PostgreSQL.
---

# Ciberseguridad Backend

Esta guia aplica y verifica 20 controles fundamentales de seguridad en el desarrollo y auditoria de servidores backend, APIs REST y bases de datos relacionales.

## When to Use

- Al crear o generar desde cero un backend en Java Spring Boot, Node.js u otro framework.
- Al auditar codigo existente en busca de vulnerabilidades antes de subirlo a GitHub o presentarlo en una defensa tecnica.
- Cuando el usuario solicite aplicar encapsulamiento estricto, proteccion de credenciales, validacion de datos o cabeceras de seguridad.
- Al preparar listas de verificacion de seguridad (OWASP) o documentacion tecnica de proteccion de datos.

## Steps

### 1. Ocultar API keys y credenciales
Reemplaza cualquier clave, token o contrasena escrita en el codigo fuente por variables de entorno leidas en tiempo de ejecucion (por ejemplo, `${DB_PASSWORD}` en `application.properties`). Crea un archivo `.env.example` con nombres de variables y valores ficticios para documentar la configuracion sin exponer secretos reales.

### 2. Eliminacion de secretos en Git
Configura `.gitignore` antes del primer commit para excluir `.env`, `*.pem`, `*.key` y archivos locales de configuracion. Verifica con `git status` y `git log -S "PASSWORD"` que ninguna credencial haya entrado al historial; si se subio un secreto por error, rota la credencial inmediatamente y limpia el historial.

### 3. Separacion de llaves y roles de Base de Datos
Evita conectar la aplicacion usando el superusuario `postgres` o llaves de administrador total (`service_role`). Crea un usuario de base de datos especifico para la aplicacion con permisos limitados unicamente a las tablas del esquema (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) y nunca expongas credenciales de base de datos al frontend.

### 4. Row-Level Security (RLS)
Activa politicas de seguridad a nivel de fila en PostgreSQL (`ALTER TABLE nombre_tabla ENABLE ROW LEVEL SECURITY;`) o filtra obligatoriamente cada consulta en la capa de repositorio por el identificador de planta, organizacion o usuario propietario para evitar fugas de datos entre clientes.

### 5. Encriptar datos sensibles en reposo
Identifica datos confidenciales que deban recuperarse en su forma original (como tokens externos o documentos sensibles) y cifralos antes de guardarlos usando algoritmos simetricos estandar como `AES-256-GCM` o la extension `pgcrypto` de PostgreSQL.

### 6. Forzar la autenticacion
Configura la seguridad del servidor bajo el principio de denegacion por defecto (*Default Deny*). Exige un token JWT valido o sesion activa en todas las rutas de modificacion (`POST`, `PUT`, `DELETE`), devolviendo `401 Unauthorized` cuando falte o sea invalida la credencial.

### 7. Restringir el acceso a registros (Prevencion de IDOR)
Verifica en la capa de servicio (`Service`) que el usuario autenticado tenga el rol y los permisos necesarios sobre el recurso especifico antes de leerlo, modificarlo o eliminarlo. Si un usuario intenta operar un registro ajeno o no autorizado, rechaza la operacion con `403 Forbidden`.

### 8. Bloquear manipulacion de campos (Mass Assignment y Encapsulamiento)
Declara todos los atributos de las entidades del modelo como `private`. Elimina los *setters* publicos genericos sobre campos criticos (IDs, marcas de tiempo de auditoria, estados de bloqueo o indicadores calculados) y modificalos solo mediante metodos de negocio controlados. Recibe datos del cliente exclusivamente a traves de objetos `DTO` inmutables (como `record` en Java) que contengan solo los campos editables por el usuario.

### 9. Proteger Session Cookies
Si el sistema utiliza cookies para sesiones o tokens de refresco, configuralas siempre con los atributos `HttpOnly=true` (inaccesibles desde JavaScript para mitigar XSS), `Secure=true` (solo por HTTPS) y `SameSite=Strict` o `Lax` (proteccion contra CSRF).

### 10. Hashear contrasenas
Nunca almacenes contrasenas en texto plano ni con cifrado reversible. Utiliza `BCryptPasswordEncoder` (con factor de costo minimo de 10 o 12) o `Argon2id` para transformar cada contrasena en un hash irreversible con sal aleatoria antes de persistirla.

### 11. Rate Limiting (Limite de peticiones)
Aplica un limite de velocidad por direccion IP o por usuario (por ejemplo, mediante un filtro con `Bucket4j` o un interceptor de peticiones) para bloquear ataques de fuerza bruta y denegacion de servicio, respondiendo con el codigo `429 Too Many Requests` al superar el umbral.

### 12. Proteccion contra bots
Protege los formularios publicos y rutas de inicio de sesion contra automatizacion maliciosa integrando validacion de tokens CAPTCHA (Cloudflare Turnstile o reCAPTCHA), campos *honeypot* invisibles y bloqueo temporal tras intentos fallidos consecutivos.

### 13. Parametrizar queries (Prevencion de SQL Injection)
Ejecuta todas las consultas a la base de datos mediante metodos derivados de Spring Data JPA, consultas `@Query` con parametros enlazados (`:parametro`) o `PreparedStatement`. Nunca concatenes texto enviado por el usuario directamente dentro de cadenas SQL.

### 14. Validar inputs en la frontera
Aplica validacion estricta en los DTOs de entrada usando anotaciones de `jakarta.validation` (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, `@AssertTrue`) junto con `@Valid` en el controlador. Si la entrada no cumple el formato esperado, detiene el flujo antes de llegar a la capa de negocio y devuelve `400 Bad Request`.

### 15. Sanitizar contenido
Limpia y escapa cualquier texto libre ingresado por el usuario (como motivos de falla u observaciones) eliminando etiquetas HTML o scripts (`<script>`, eventos inline) y caracteres de control antes de almacenarlo o devolverlo, previniendo ataques de Cross-Site Scripting (XSS) e inyeccion en logs.

### 16. Restringir archivos subidos
Si el sistema permite subir archivos o imagenes, valida el tipo real leyendo la firma binaria (*magic bytes*) y no solo la extension, limita el tamano maximo permitido en la configuracion del servidor, renombra el archivo con un `UUID` generado internamente y almacenalo fuera de la carpeta publica ejecutable.

### 17. Devolver solo los datos necesarios
Usa DTOs de salida para construir las respuestas JSON con el principio de minimizacion de datos. Nunca devuelvas entidades JPA crudas con relaciones internas, hashes de contrasenas ni trazas de error del servidor (*stack traces*). Implementa un `@RestControllerAdvice` global que capture excepciones y devuelva mensajes limpios con su codigo HTTP exacto.

### 18. Security Headers y CORS estricto
Configura cabeceras de seguridad HTTP en las respuestas del servidor:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Content-Security-Policy: default-src 'self'`
- `Referrer-Policy: strict-origin-when-cross-origin`
- En `@CrossOrigin` o `CorsConfiguration`, especifica la lista exacta de dominios permitidos (por ejemplo, `http://localhost:3000` o `http://localhost:5173`) y nunca utilices el comodin abierto `*` en produccion.

### 19. Forzar HTTPS
Configura el entorno productivo para redirigir todo el trafico HTTP hacia HTTPS (TLS 1.2 o 1.3) e incluye la cabecera `Strict-Transport-Security: max-age=31536000; includeSubDomains` (HSTS) para impedir ataques de degradacion de protocolo o intercepcion en redes locales.

### 20. Escanear dependencias
Audita periodicamente las librerias del archivo `pom.xml` o `package.json` para detectar vulnerabilidades conocidas (CVEs) utilizando herramientas como OWASP Dependency-Check en Maven, GitHub Dependabot o `mvn versions:display-dependency-updates`, manteniendo actualizado el framework base.

## Gotchas

- **Poner variables en `private` pero generar `@Data` o `@Setter` en toda la clase:** Esto anula el encapsulamiento y expone campos de auditoria a modificaciones accidentales o maliciosas. Crea solo *getters* y metodos de dominio con validacion interna.
- **Devolver `200 OK` con un mensaje de error adentro:** Rompe el estandar REST y oculta fallos de seguridad a los sistemas de monitoreo. Usa siempre `400`, `401`, `403`, `404`, `409` o `429` segun corresponda.
- **Confiar en la validacion del frontend:** Un atacante puede enviar peticiones directo desde Postman o `curl` saltandose el navegador. Toda regla (como el check legal del candado fisico DS 594) debe validarse nuevamente en el backend.