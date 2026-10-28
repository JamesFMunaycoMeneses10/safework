# Piloto multiempresa de SafeWork

## Roles y alcance

`SUPERADMIN` es el administrador de la plataforma: puede registrar empresas, crear la primera cuenta administradora de cada una y activar o desactivar su acceso. No se le asigna acceso a los módulos SST de los clientes. `ADMIN`, `SUPERVISOR` y `TRABAJADOR` conservan sus responsabilidades, ahora dentro de su propia empresa. Un administrador de una empresa no puede registrar ni listar las demás empresas.

El catálogo está en `/plataforma/empresas`. Las altas y actualizaciones utilizan POST con protección CSRF. No existe eliminación de empresas: deshabilitarlas no borra información. Los códigos son únicos y los nombres de usuario y correos permanecen únicos en toda la plataforma; el login continúa usando usuario y contraseña, sin selector de empresa. No se permite elegir el rol o el ID de empresa del administrador inicial en el formulario.

Los datos preexistentes se conservan en la empresa principal creada por la migración. No se trasladan usuarios existentes entre empresas ni se ascienden administradores de empresa a SUPERADMIN automáticamente. Los pagos, membresías y renovación de planes quedan fuera de esta etapa: esta es una base técnica de SaaS, no un servicio comercial listo para clientes.

## Crear el primer administrador de plataforma

El aprovisionamiento está desactivado normalmente. Solo debe habilitarse una vez, después de aplicar las migraciones y contra la base del piloto, no contra el sistema original sin respaldo.

1. Define en el entorno del proceso de SafeWork:
   - `SAFEWORK_CREAR_ADMIN_PLATAFORMA=true` (mapeada a `safework.plataforma.crear-administrador`).
   - `SAFEWORK_PLATFORM_USERNAME`: nombre nuevo de acceso, entre 3 y 50 caracteres.
   - `SAFEWORK_PLATFORM_PASSWORD`: contraseña nueva, mínimo 12 caracteres y máximo 72 bytes UTF-8.
   - `SAFEWORK_PLATFORM_NOMBRE`, `SAFEWORK_PLATFORM_APELLIDO` y `SAFEWORK_PLATFORM_CORREO`.
2. Inicia una única instancia de SafeWork. Si ya existe un SUPERADMIN o el usuario/correo pertenece a una cuenta existente, el aprovisionamiento falla y no cambia sus privilegios.
3. Retira la opción de aprovisionamiento y todas las variables de la contraseña de arranque, y reinicia con la configuración normal.
4. Ingresa con la nueva cuenta. Debe abrir el catálogo de empresas, no los módulos SST.

No se proporcionan credenciales universales. No escribas contraseñas en Git, archivos de documentación, capturas o comandos compartidos. El hash BCrypt se almacena en MySQL; no se registra la contraseña en logs.

En Compose las variables opcionales están en `.env.example`, desactivadas y sin valores secretos. Completa temporalmente sus valores en `.env` para el primer inicio. Después cambia `SAFEWORK_CREAR_ADMIN_PLATAFORMA=false`, elimina los cinco valores `SAFEWORK_PLATFORM_*` de `.env` y ejecuta `docker compose up -d --force-recreate app` para recrear solamente la aplicación sin esas credenciales; no se borra el volumen de MySQL. No mantengas la contraseña de aprovisionamiento en la configuración permanente del contenedor ni la muestres en capturas. No arranques dos instancias con aprovisionamiento simultáneo.

## Registrar y comprobar otra empresa

1. Entra como SUPERADMIN y abre **Empresas de la plataforma > Nueva empresa**.
2. Completa nombre y código único de la empresa y los datos de una nueva cuenta ADMIN.
3. Conserva la contraseña inicial fuera del sistema y comunícala por un canal seguro. Por seguridad, si el formulario contiene errores debes escribirla otra vez.
4. Cierra sesión e ingresa con el nuevo administrador. Verifica que no vea áreas, trabajadores, registros SST, adjuntos, auditoría ni reportes de la empresa principal.
5. Crea registros propios y prueba sus IDs desde una cuenta de otra empresa: el servidor debe rechazarlos aunque se escriba la URL manualmente.
6. Desde SUPERADMIN, deshabilita esa empresa. Sus usuarios deben quedar bloqueados al iniciar sesión y sus sesiones anteriores deben rechazarse en la siguiente solicitud. Reactivarla permite recuperar el acceso sin eliminar datos.

El alta de la empresa y su administrador inicial se ejecuta en una sola transacción JDBC: si falla la cuenta, se revierte también la empresa. La contraseña se codifica antes de persistir, y los métodos públicos de administración comprueban SUPERADMIN además de las reglas web.

## Límites de operación

- No crear datos reales de clientes hasta verificar migraciones, aislamiento, respaldo/restauración, HTTPS y operación continua.
- Esta etapa no implementa pagos, recuperación de contraseña por correo, cambio obligatorio de contraseña inicial ni autenticación multifactor.
- El administrador de plataforma puede administrar el catálogo, pero no suplantar usuarios de una empresa desde la interfaz.
- Los respaldos de la base completa contienen información de todas las empresas; su custodia corresponde al operador de plataforma y no se exponen como descarga al administrador cliente.
- Las pruebas automáticas y los registros de ejecución deben acompañar la entrega; no declarar que el sistema está en cloud si solo se ejecutó localmente.
