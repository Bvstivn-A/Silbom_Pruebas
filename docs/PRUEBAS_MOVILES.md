# SILBOM móvil: PWA, Wi-Fi, HTTPS y etiquetas

La aplicación de técnicos está en `/app/`; el panel de supervisión continúa en
`/`. Ambos se sirven desde Spring Boot. No hace falta otro framework ni un
servidor frontend separado. La interfaz es instalable como PWA cuando el
teléfono confía en su certificado HTTPS.

## Actualizar una instalación

1. Respaldar PostgreSQL y conservar por separado la clave AES existente. Usar
   una base de pruebas y cuentas de prueba para la primera validación.
2. Aplicar como propietario/migrador, con la aplicación detenida:

   ```bash
   psql -h localhost -U silbom_owner -d silbom_db -v ON_ERROR_STOP=1 -f db/schema.sql
   ```

   El esquema incluye V3: dos columnas opcionales y un índice único para
   solicitudes idempotentes. No elimina tickets ni reasigna responsables.
   Puede repetirse; el rol de la API sigue sin ser propietario ni superusuario.
   Con otro rol, agregar `-v app_role=nombre_del_rol`.
3. Mantener `.env` fuera de Git, con DB_USERNAME, DB_PASSWORD, ENCRYPTION_SECRET
   y la cuenta inicial conforme al README. No reemplazar la clave AES de una
   instalación existente por una nueva.
4. Compilar con JDK 21 y Maven 3.9+: `mvn clean package`.

## Configuración propuesta para teléfonos en la misma Wi-Fi

Este ejemplo usa Caddy como proxy HTTPS local. No publica servicios a internet
ni crea un túnel externo. Requiere instalar Caddy desde su distribución oficial
y confiar en su CA de pruebas en cada dispositivo. Es una CA para este entorno
de pruebas, no el certificado definitivo de una planta.

1. Conectar el computador y el teléfono a la misma Wi-Fi. Obtener la IP LAN del
   computador: por ejemplo `192.168.1.50`. En Windows usar `ipconfig`; en Linux
   `ip -brief address`; en macOS consultar los detalles de la conexión Wi-Fi.
   No usar `localhost` en el teléfono: allí identificaría al propio teléfono.
2. Mantener PostgreSQL solo en loopback: `listen_addresses = 'localhost'`,
   reglas `pg_hba.conf` locales para el rol de aplicación y puerto 5432 sin
   acceso desde otras máquinas. Si se usa Docker, publicar como
   `127.0.0.1:5432:5432`. No abrir ni redirigir ese puerto en el router.
3. Iniciar Spring en el perfil que escucha solo en loopback:

   ```bash
   java -jar target/silbom-backend-1.0.0.jar --spring.profiles.active=local-proxy
   ```

   Mantener SERVER_PORT=8080 para que coincida con el upstream del ejemplo.
   Si se cambia, actualizar también `reverse_proxy` en el Caddyfile.
4. En otra terminal, ejecutar desde el repositorio:

   ```bash
   SILBOM_TEST_HOST=192.168.1.50 caddy run --config deploy/Caddyfile.local
   ```

   PowerShell: definir `$env:SILBOM_TEST_HOST = '192.168.1.50'` y ejecutar el
   mismo comando `caddy run`. El proxy recibe HTTPS en 8443 y conecta únicamente
   a Spring en `127.0.0.1:8080`. Spring interpreta las cabeceras del proxy solo
   desde loopback. Permitir en el firewall el puerto TCP 8443 para la red local
   de pruebas; no abrir 8080 ni 5432 a internet.
5. Instalar en el teléfono **solo el certificado público raíz** de la CA local
   de Caddy, `pki/authorities/local/root.crt` en su directorio de datos. En una
   ejecución habitual de Linux como usuario está bajo
   `~/.local/share/caddy/`; macOS usa `~/Library/Application Support/Caddy/` y
   Windows `%APPDATA%\Caddy\`. La ubicación puede variar si corre como servicio.
   No copiar `root.key`, claves de servidor, archivos `.env` ni contraseñas.
   En Android, importar la CA desde los ajustes de certificados de usuario; en
   iOS, instalar el perfil y activar su confianza en Ajustes → General →
   Información → Ajustes de confianza de certificados. Las políticas de un
   teléfono administrado pueden impedirlo; verificar con quien lo administra.
6. Abrir `https://192.168.1.50:8443/app/` en Chrome Android o Safari iOS. El
   navegador debe mostrar una conexión de confianza, sin advertencias. No
   utilizar opciones para ignorar errores TLS. Para el panel, abrir el mismo
   origen con `/` desde el computador.
7. Iniciar sesión con una cuenta individual de prueba. En Android: menú →
   Instalar aplicación. En iOS: Compartir → Añadir a pantalla de inicio. La
   oferta automática depende del navegador; el diálogo también explica la
   instalación manual. Abrir desde el ícono y comprobar el modo independiente.

HTTP en loopback sigue disponible para desarrollo en el computador. Un origen
HTTP de una IP LAN no permite enviar credenciales desde los clientes actuales
ni instalar una PWA completa. HTTPS debe estar listo para las pruebas móviles.

Si falla la conexión, comprobar primero que la URL use la IP del computador,
que el proxy esté activo, la confianza de la CA, el firewall y que la Wi-Fi no
tenga aislamiento entre clientes. Una interfaz cacheada no demuestra que la
API esté disponible.

## Autenticación y CORS

- Se mantiene HTTP Basic individual y BCrypt. Usuario/contraseña no se
  almacenan en localStorage, sessionStorage ni Cache Storage; el encabezado de
  sesión permanece en memoria. Cerrar la app o recargar puede exigir login.
- La API mantiene sus permisos: supervisor para administración, propietario
  para liberar, notas según identidad. Un técnico no puede liberar un ticket
  ajeno aunque el botón esté visible; decide el servidor.
- Si PWA y API comparten origen HTTPS, no se configura CORS para el teléfono.
  La IP del teléfono no es un origen a agregar a una lista.
- Si se sirve un frontend separado, declarar su origen HTTPS exacto en
  `SILBOM_CORS_ORIGINS`, separado por comas si hay varios. El selector de
  servidor acepta solo orígenes sin usuario, contraseña ni parámetros. Cambiar
  de servidor borra la sesión antes de consultar el nuevo catálogo.
- No se toma un servidor de `?api=`: una etiqueta no puede redirigir una
  credencial a otro backend. No se permiten redirecciones HTTP de la API.
- El origen `null` de archivos HTML/WebView no está autorizado por defecto.
  El APK binario anterior es legado y no forma parte de la validación PWA.
  No ampliar CORS con `null` ni comodines de redes para hacer funcionar la PWA.

## Desconexiones, reintentos y Multi-LOTO

La aplicación conserva el último estado confirmado y no simula altas ni cierres.
Una solicitud cuyo resultado no pudo leerse queda pendiente en la vista, con
el botón **Reintentar misma solicitud**. No se envían escrituras en segundo
plano ni al volver la conexión. Una pulsación explícita es necesaria.

El alta usa `Idempotency-Key` (UUID aleatorio). El backend conserva la clave
y la huella del contenido, limitadas a técnico/planta. Repetir esa solicitud
devuelve el ticket ya registrado sin incrementar contadores ni eventos. Una
clave reutilizada con otro contenido recibe 409. Los clientes que no envían
la cabecera mantienen su contrato anterior, sin protección de reintento de alta.

Para una liberación con respuesta perdida, un reintento puede recibir 409.
La app consulta entonces el ticket: solo un estado CERRADO confirmado por la
API permite anunciar el cierre. Aunque el ticket esté cerrado, una lectura
fallida de la máquina no permite anunciar que el equipo quedó libre. Multi-LOTO
continúa mostrando los otros candados hasta el cierre del último.

La solicitud pendiente y su contenido permanecen **en memoria**, no en una
cola duradera. No cerrar ni recargar mientras se resuelve. Si el navegador o
el sistema la descarta, iniciar sesión y revisar el historial y el equipo antes
de iniciar otra intervención: no se promete recuperación de solicitudes tras
perder la página. La clave idempotente solo identifica un reintento que conserve
la misma clave. Esta limitación evita guardar notas confidenciales en el teléfono.

La referencia de falla queda vacía para que el técnico identifique una nueva
avería o introduzca la referencia compartida por los otros participantes de
esa avería. No reutilizar una referencia para averías distintas.

## Identificación con NFC NDEF y QR

En pruebas, el enlace es:

```text
https://192.168.1.50:8443/app/?maquina=EQ-ENV-01
```

Para una etiqueta NFC usar **un registro NDEF URI** con esa URL (tipo URI,
`TNF_WELL_KNOWN` / `RTD_URI`), escrito con una herramienta de etiquetas del
teléfono o del proveedor. La lectura del sistema operativo abre el enlace en
el navegador; no requiere que la PWA use Web NFC. El QR debe codificar
exactamente la misma URL y se abre con la cámara/lector del sistema. La selección
manual de máquinas permanece disponible.

El enlace contiene exclusivamente el origen HTTPS estable, `/app/` y el ID
de máquina. No incluir usuarios, contraseñas, tokens, API keys, el Idempotency-Key
de una operación ni nombres de técnicos. La etiqueta identifica una máquina;
no autentica al portador. Su apertura hace lecturas, nunca POST ni PUT.

Si el ID es inválido o no está en el catálogo de la planta, se informa el error
sin seleccionar otro equipo. Si falta red, se conserva el ID pendiente hasta
que el usuario actualice el catálogo. Identificar no acepta automáticamente la
declaración de candado ni pulsa el botón de bloqueo.

No escribir todavía etiquetas definitivas con una IP que pueda cambiar. Primero
acordar un nombre DNS estable y un certificado confiable, validar el inventario
y probar NFC y QR con los modelos de teléfonos reales. Luego preparar el lote
de etiquetas y decidir si se protegen contra reescritura conforme al procedimiento
de planta. El registro digital no verifica físicamente energía cero.

## Pruebas automatizadas

```bash
mvn test
# PostgreSQL real, usando únicamente las credenciales de una base efímera:
bash scripts/test-postgres.sh
python -m venv .venv
.venv/bin/python -m pip install -r tests/browser/requirements.txt
.venv/bin/python -m playwright install chromium
.venv/bin/python -m unittest discover -s tests/browser -v
python scripts/sync-web-assets.py --check
```

En Windows usar `.venv\Scripts\python`. En Linux pueden requerirse las
dependencias de Chromium (`python -m playwright install --with-deps chromium`).
Para utilizar un Chromium del sistema, definir CHROMIUM_PATH con su ruta.
Las pruebas de navegador usan una API HTTP controlada y no acceden a una base
real. La suite Java/PostgreSQL cubre persistencia, permisos, concurrencia y
reintentos simultáneos. El workflow ejecuta ambas suites en PR y en pushes de
las ramas de Hito 3/corrección.

Resultados de esta revisión: 44 pruebas generales Java, 8 de integración con
PostgreSQL 17 y 18 de navegador aprobadas, sin fallos ni omisiones. El esquema
se aplicó dos veces sobre una base efímera. También se recorrió la interfaz
contra Spring Boot y PostgreSQL reales con dos cuentas técnicas: una respuesta
de alta perdida y reintentada produjo un único ticket; dos candados se cerraron
sucesivamente respetando Multi-LOTO, sin errores JavaScript observados. El
Caddyfile se procesó con `caddy adapt`; esto valida su configuración, no la
confianza de certificados en teléfonos físicos.

Se cubren desconexión de alta/liberación, respuesta perdida, 500 posterior al
commit, timeout, doble pulsación, 401/403/409, JSON inválido/incompleto, fallo de
actualización tras un cierre confirmado, segundo candado Multi-LOTO, enlaces
válidos/inválidos, puertos no predeterminados, pantallas de 320–768 px y caché
offline de la interfaz sin API ni credenciales.

## Comprobación manual antes de una prueba en terreno

- En el teléfono, instalar la PWA con HTTPS confiable y comprobar inicio de
  sesión con una cuenta técnica, además de una supervisora por separado.
- Abrir el enlace de una máquina y comprobar nombre/ID sin crear ningún ticket.
- Abrir el mismo enlace por QR y, después, mediante una etiqueta NDEF de prueba.
- Registrar dos candados con dos usuarios y verificar su aparición en el panel.
  Liberar uno: el equipo debe seguir bloqueado. Liberar el último: comprobar
  el estado confirmado por el servidor.
- Activar modo avión durante un alta y durante una liberación. La pantalla
  debe informar que no hay confirmación. Reconectar y reintentar explícitamente;
  verificar un solo ticket/evento de alta y un solo cierre.
- Comprobar que una cuenta no pueda liberar el ticket ajeno ni leer sus notas.
- Cerrar/reabrir la PWA, girar el teléfono, abrir el teclado y comprobar botones,
  formularios y mensajes. Tras una actualización, cerrar todas las vistas de
  SILBOM y reabrir para activar el nuevo service worker sin perder una operación.

La revisión automatizada no sustituye estas pruebas de dispositivos físicos,
la confianza de certificados del teléfono ni la lectura de una etiqueta real.

## Fuentes y artefactos

Editar `src/main/resources/static/app/index.html`, `app/api-client.js` y
`src/main/resources/static/index.html`. Ejecutar `python scripts/sync-web-assets.py`
para generar las copias HTML de compatibilidad, documentación y Android. La CI
comprueba que coincidan. Descargar HTML desde la app obtiene las fuentes
estáticas, sin exportar el DOM con datos de sesión o notas.

El APK ya versionado pertenece a la versión anterior. Se conserva como artefacto
legado; no se ha recompilado, firmado para release ni probado en un dispositivo
en este cambio. Las fuentes de sus assets sí se sincronizan para una futura
construcción. La entrega inmediata que se valida es la web/PWA.
