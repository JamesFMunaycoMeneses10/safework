# Cierre de SafeWork — implementación del 28/10/2026

## 1. Capacitaciones

- ADMIN/SUPERVISOR: menú Capacitaciones, programación, edición de actividades programadas, participantes, asistencia, finalización y cancelación.
- TRABAJADOR: Mis capacitaciones, únicamente asignaciones de su ficha y su asistencia. No consulta el padrón de otros participantes.
- Área obligatoria de realización; participantes de cualquier área de la misma empresa. Máximo 500 por actividad.
- Asistencia a partir del inicio; finalización después del horario y con toda la asistencia registrada. Una actividad realizada/cancelada no se edita.
- No se elimina el historial al cancelar. Auditoría de creación, edición, asistencia, finalización y cancelación.
- Migración V3 crea las dos tablas con restricciones de empresa. No modifica V1 ni V2.

Para demostrarlo: supervisor programa una actividad, elige área y trabajadores; al realizarla marca Asistió/Faltó, guarda y finaliza. El trabajador vinculado consulta su historial. Puede registrarse una actividad ya realizada con su fecha verdadera para documentar formación pasada.

## 2. Dashboard

Una acción Cerrada por aprobación se cuenta como completada, al igual que los registros antiguos Completada. Ambas aparecen como terminadas y no vencidas. Si no hay registros de riesgo ni incidente, y tampoco alertas de acciones, se muestra SIN DATOS SUFICIENTES. Los indicadores no certifican que una empresa esté libre de peligro.

## 3. GitHub Actions

La anotación pública de la ejecución 40712940581 identifica: `Unrecognized named-value: 'runner'` en el env del job, línea 43. Se reemplazó ese contexto por `github.workspace`. actionlint confirma que el original falla y que el workflow modificado valida. Las pruebas locales y el ensayo de imagen complementan esa comprobación; se necesita una nueva ejecución en GitHub después de subir los cambios. No declarar un resultado remoto verde sin verlo.

## 4. Respaldos Docker

Desde PowerShell en la carpeta del proyecto:

```powershell
.\scripts\backup-docker.ps1
.\scripts\restore-docker-isolated.ps1 -BackupFile 'RUTA_COMPLETA_DEL_RESPALDO.sql'
.\scripts\install-docker-backup-task.ps1 -RunAt '20:00'
```

La copia usa el MySQL del contenedor, conserva binarios mediante `--hex-blob`, calcula SHA256 y restringe permisos del directorio. La restauración de comprobación crea un MySQL temporal sin red ni puertos de host y lo elimina al acabar; no reemplaza la base de 8081. La tarea diaria funciona con sesión Windows abierta, Docker encendido y alimentación eléctrica según la configuración actual de la tarea. No equivale a continuidad 24/7. Conservar además una copia fuera de la laptop, protegida. Un volumen persistente no sustituye el respaldo.

Nunca ejecutar `docker compose down -v` para resolver un acceso o actualizar la app. Una migración aplicada se corrige mediante otra migración; volver a una imagen anterior no revierte automáticamente cambios de esquema.

## 5. Acceso y recuperación

- Regla única de contraseña nueva: mínimo 12 caracteres Unicode y máximo 72 bytes UTF-8. Las contraseñas existentes no cambian y siguen permitiendo login.
- Cinco fallos por combinación origen/cuenta en una ventana de 15 minutos bloquean temporalmente esa combinación. Treinta por origen limitan el cambio masivo de nombres. Se devuelve 429 y Retry-After.
- El contador es de una instancia y se pierde al reiniciar; para varias réplicas debe trasladarse a un almacén compartido o al proxy de acceso.
- Cambiar/restablecer contraseña incrementa una versión de acceso e invalida las sesiones anteriores en la siguiente solicitud.
- Recuperación: usuario activo, empresa activa, correo ya registrado, token aleatorio de 32 bytes, solo SHA256 en BD, duración 20 minutos, uso único y CSRF. No recupera SUPERADMIN desde la web.
- No muestra si una cuenta existe. La clave nueva no se envía por correo. Nunca registrar el enlace/token en logs.

El envío real permanece deshabilitado hasta configurar `SAFEWORK_RECUPERACION_ENABLED=true`, `SAFEWORK_PUBLIC_URL=https://...`, `SAFEWORK_MAIL_FROM` y las variables `SAFEWORK_SMTP_*` del ejemplo. SMTP usa STARTTLS obligatorio y límites de tiempo. No enviar estas claves al chat ni guardarlas en Git. Las pruebas de correo son simuladas, no envíos a personas.

## 6. Publicación y monitoreo

Se añadió `compose.cloud.yaml` con Caddy, HTTPS, límites iniciales de memoria/CPU y rotación de logs. Es una configuración preparada, no una publicación realizada.

Se necesita una VM Linux con Docker/Compose, un dominio o subdominio con DNS apuntando a esa VM, acceso por SSH y puertos 80/443. Como punto inicial de ensayo puede evaluarse una máquina con 2 CPU y 4 GB de RAM, midiendo después la carga. Esto no es una garantía de capacidad ni una contratación de proveedor gratuito.

En el servidor, crear un `.env` privado nuevo con contraseñas distintas. Definir `SAFEWORK_DOMAIN`, `SAFEWORK_TLS_EMAIL` y una etiqueta `SAFEWORK_RELEASE` para la versión que se va a liberar. No reutilizar el directorio/volumen de pruebas como producción por accidente.

```bash
docker compose -f compose.yaml -f compose.cloud.yaml config --quiet
docker compose -f compose.yaml -f compose.cloud.yaml up -d --build
```

El proxy publica solo 80/443; Tomcat y MySQL quedan internos. Caddy obtiene certificados cuando DNS y conectividad son correctos. Verificar HTTPS, cookies Secure, roles, datos por empresa, adjuntos, backup y restauración antes de declarar el servicio publicado. Los límites iniciales se ajustan con medición.

`scripts/check-health.ps1` consulta solo `/actuator/health`, mide latencia y guarda JSON sin credenciales. Un resultado DOWN o lento produce código de salida 1 para conectarlo a un monitor/tarea. No hay central de alertas ni SLA comercial contratado. En un servidor Linux habrá que configurar allí la ejecución de backups/monitoreo y la copia externa; las tareas Windows no se trasladan automáticamente.

## 7. Membresía y Mercado Pago en integración

Se añadió una página de Membresía exclusiva de ADMIN, catálogo de plan, solicitud de pago, historial y vigencia. El plan inicial PEN 49 por 30 días es una tarifa ficticia académica, no una oferta comercial.

- Se utiliza Checkout Pro con Preferences API (`/checkout/preferences`) y consulta autenticada de pagos (`/v1/payments/{id}`). La API Orders es otra alternativa del proveedor, no se mezclan sus notificaciones con este adaptador.
- La cuenta de vendedor debe confirmarse por API como `test_user`, con el ID configurado. No se decide si una clave es de prueba solo por su prefijo.
- Se usa `init_point` de la respuesta; no se reemplaza por `sandbox_init_point`.
- El servidor obtiene el precio del catálogo; no acepta importe/empresa desde el navegador. La solicitud repetida usa la misma referencia.
- El retorno success/failure/pending solo muestra información. No activa membresías.
- El webhook verifica HMAC-SHA256, timestamp reciente y request-id; después consulta el pago en API y valida referencia, importe, moneda, vendedor y `live_mode=false`.
- El evento se procesa una sola vez. Un pago rechazado no activa vigencia y un reembolso la recalcula. Solo el callback tiene excepción de CSRF, el resto de los formularios continúa protegido.
- La membresía es de renovación manual en este piloto, no un débito automático. No se bloquea el acceso SST por impago en esta fase.
- No se almacenan tarjetas ni CVV. No existe un modo de cobro real en esta versión.

Para conectarlo, crear una aplicación en Mercado Pago Developers Perú y obtener una cuenta vendedora de prueba y su comprador de prueba. Configurar de forma privada:

```text
SAFEWORK_PAGOS_ENABLED=true
SAFEWORK_MP_TEST_ACCESS_TOKEN=valor_privado_del_vendedor_de_prueba
SAFEWORK_MP_TEST_SELLER_ID=id_del_vendedor_de_prueba
SAFEWORK_MP_WEBHOOK_SECRET=secreto_privado_de_firma
SAFEWORK_PUBLIC_URL=https://direccion_publica
```

El webhook es `https://direccion_publica/webhooks/mercadopago`. Probar con comprador/tarjetas de prueba según la documentación del proveedor. El piloto local sigue con pagos deshabilitados hasta tener esas variables y HTTPS. Las pruebas automatizadas usan un proveedor simulado y peticiones HTTP interceptadas: no acreditan todavía una transacción real en el entorno de Mercado Pago.

Fuentes oficiales consultadas:
- https://www.mercadopago.com.pe/developers/es/reference/online-payments/checkout-pro-preferences/create-preference/post
- https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/test-accounts
- https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/integration-test/test-purchases
- https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/payment-notifications
- https://github.com/mercadopago/openapi/blob/main/schemas/webhooks.yaml
- https://developers.mercadolibre.com.ar/servicios-consulta-usuarios
- https://caddyserver.com/docs/automatic-https
- https://github.com/rhysd/actionlint

## Pendientes que dependen de servicios externos

1. Elegir alojamiento y dominio, configurar DNS y publicar; verificarlo desde otra red.
2. Configurar SMTP y comprobar recepción de un correo solicitado por el usuario.
3. Configurar vendedor/comprador de prueba de Mercado Pago y completar el circuito externo con webhook.
4. Subir los cambios para ejecutar de nuevo GitHub Actions y conservar el resultado.

Después se decide el alcance comercial: renovación automática, cancelación de suscripción, facturación, límites por plan, atención de contracargos, MFA y operación con varias réplicas. No son funciones terminadas por tener ahora una pantalla de membresía.

Si Windows bloquea la ejecución directa de scripts, puedes usar `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup-docker.ps1`. Ese ajuste se limita a ese proceso y no cambia la política global.
