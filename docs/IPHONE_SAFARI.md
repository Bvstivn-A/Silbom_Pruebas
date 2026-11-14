# SILBOM en iPhone: Safari, instalación y etiquetas

La entrega móvil principal es la PWA de `/app/`, con el HTML/CSS/JavaScript y
Spring Boot existentes. No requiere instalar una APK. Safari iOS no ofrece
Web NFC: SILBOM no utiliza `NDEFReader` ni exige ese API para identificar equipos.

## Preparar el computador y HTTPS

Seguir [PRUEBAS_MOVILES.md](PRUEBAS_MOVILES.md) para aplicar el esquema, iniciar
Spring y poner Caddy delante con HTTPS. Usar el iPhone y el computador en la
misma Wi-Fi. El backend recibe conexiones desde el proxy local y PostgreSQL
permanece local, sin exponer el puerto 5432 a la red o a internet.

Para la configuración de pruebas, importar **solo** la CA pública de Caddy en
el iPhone: instalar el perfil y luego activar la confianza en Ajustes → General
→ Información → Ajustes de confianza de certificados. No copiar claves privadas
ni `.env`. Si el teléfono administrado no permite instalar la CA, solicitar un
certificado de confianza al administrador. No continuar ignorando errores TLS.
Para las etiquetas definitivas usar un dominio HTTPS estable y un certificado
confiable; una IP temporal de Wi-Fi no sirve como dirección definitiva.

## Instalar desde Safari

1. Abrir **Safari**, no un navegador integrado en una aplicación de mensajería.
2. Visitar `https://<IP_PC>:8443/app/` con la IP del computador. No usar localhost
   en el iPhone. Comprobar que no haya advertencias de certificado.
3. Abrir **Compartir → Añadir a pantalla de inicio**. Según la versión de iOS,
   puede ser necesario desplazarse por las acciones del menú.
4. Mantener **Abrir como app web** activado si aparece, dejar el nombre SILBOM
   y pulsar **Añadir**. No se espera un evento `beforeinstallprompt` en iOS.
5. Abrir SILBOM desde su ícono. Comprobar la vista independiente, los márgenes
   de la cámara/barra de inicio y el teclado al escribir usuario y contraseña.
6. Iniciar sesión con una cuenta individual. La contraseña/cabecera Basic vive
   solo en memoria y viaja mediante HTTPS. Safari y la app instalada pueden
   necesitar sesiones independientes. Recargar o cerrar puede exigir otro login.

`manifest.webmanifest` mantiene `id`, `start_url` y `scope` en `/app/`, con
`display: standalone`. Hay PNG de 192 y 512 px, además de `apple-touch-icon` de
180 px y metadatos Apple. El ícono abre la aplicación; no crea un bloqueo ni
recupera automáticamente una operación pendiente.

## NFC NDEF, QR y selección manual

Escribir **un registro NDEF URI** que contenga solo una URL como esta:

```text
https://silbom.example/app/?maquina=EQ-ENV-01
```

El dominio anterior es ilustrativo: sustituirlo por el origen HTTPS real. No
incluir credenciales, tokens, API keys, Idempotency-Key ni nombres de técnicos.
La etiqueta identifica el equipo y no concede permisos.

En un iPhone compatible con lectura NFC en segundo plano, activar la pantalla,
acercar la parte superior del teléfono a la etiqueta y tocar la notificación
del sistema. Puede ser necesario desbloquear el teléfono. El sistema abre la
URL; no es la página web la que lee la radio NFC. El modelo, versión de iOS y
contexto del dispositivo afectan la lectura. No se garantiza que un enlace NFC
se abra dentro de la PWA instalada: también puede abrir Safari.

El QR debe contener **exactamente la misma URL**. Abrirlo con Cámara o el lector
de códigos de iOS y tocar el enlace. Si NFC no está disponible, usar QR o elegir
el equipo en la lista manual. Ninguna opción requiere una APK o permisos Web NFC.

El enlace selecciona el equipo del catálogo confirmado y muestra su ficha.
El usuario todavía debe iniciar sesión, elegir el tipo de intervención, completar
los datos, declarar el candado físico y pulsar el botón de registro. Nunca se
activa ni libera un bloqueo al abrir la etiqueta. Un ID inexistente o inválido
muestra un error sin seleccionar otro equipo; sin conexión queda pendiente
de identificar hasta actualizar el catálogo.

## Conexión, suspensión y errores

La PWA cachea únicamente sus archivos de interfaz. Las respuestas de API,
credenciales y escrituras LOTO no se guardan en Cache Storage. Que el ícono
abra sin conexión no significa que el backend esté disponible.

Al perder conexión se indica que el estado está sin verificar y se deshabilitan
las acciones. Volver de otra aplicación, recuperar la red o restaurar una vista
desde el historial obliga a consultar de nuevo la API. El evento `online` por
sí mismo no se toma como prueba de que Spring funcione. Esas consultas son GET;
nunca reenvían automáticamente un alta o una liberación.

Si se perdió una respuesta, la solicitud permanece pendiente y solo se reintenta
con la acción explícita **Reintentar misma solicitud**, conservando su clave.
El backend confirma el resultado y conserva Multi-LOTO. Un cierre confirmado
con lectura posterior fallida no implica que el equipo esté libre.

iOS puede descartar una página en segundo plano y no garantiza eventos de cierre.
La solicitud pendiente vive en memoria: si la página se pierde, iniciar sesión
y revisar el historial antes de crear otra intervención. No existe una cola
offline persistente ni se promete recuperación tras el descarte de la página.

## Prueba de aceptación en el iPhone real

- Instalar con Safari y verificar ícono, nombre, apertura independiente, teclado
  y botones en un iPhone con la versión de iOS utilizada por el equipo.
- Abrir la URL por Safari, NFC y QR: comprobar el ID y que no aparezca un ticket
  nuevo hasta completar y enviar el formulario. Probar también selección manual,
  un ID desconocido y uno inválido.
- Usar dos cuentas técnicas para dos candados del mismo equipo. Cerrar el primero
  debe mantener el bloqueo; cerrar el último actualiza el estado desde el servidor.
- Cortar Wi-Fi o detener la API durante un alta y una liberación. No debe aparecer
  una confirmación local. Reconectar y reintentar explícitamente; verificar que no
  se duplican tickets o cierres.
- Cambiar de aplicación, bloquear/desbloquear el iPhone y volver a SILBOM. Debe
  revalidarse el estado sin ejecutar escrituras. Repetir con la API detenida.
- Probar una cuenta sin permiso y el intento de liberar el ticket de otro técnico.
- Tras actualizar, resolver las solicitudes pendientes, cerrar todas las vistas
  de SILBOM y reabrir para activar el service worker nuevo.

Las pruebas automáticas ejecutan los recorridos con Chromium y con WebKit,
este último usando el perfil de iPhone 13. WebKit automatizado no equivale a
un iPhone físico ni verifica la instalación de iOS, su almacén de certificados
o la radio NFC; completar la lista anterior antes de escribir etiquetas definitivas.
