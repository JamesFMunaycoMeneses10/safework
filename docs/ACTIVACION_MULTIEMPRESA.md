# Activación multiempresa verificada: 21/10/2026

La actualización ya está aplicada a la base local habitual. No es necesario volver a ejecutar la migración ni habilitar baseline-on-migrate. No se borraron usuarios, claves ni archivos; los datos existentes pertenecen ahora a Empresa principal.

## Para usarla

1. Puedes iniciar SafeWork desde tu IDE como antes. Tus cuentas actuales conservan su rol y acceso a su propia empresa.
2. Para administrar nuevas empresas necesitas una cuenta nueva SUPERADMIN. No existe una contraseña predeterminada ni se convirtió tu cuenta actual.
3. Detén la aplicación y ejecuta en la terminal del proyecto:

```powershell
.\scripts\crear-admin-plataforma.ps1
```

El asistente pide nombre de usuario, nombre, apellido, correo y una contraseña oculta de al menos 12 caracteres. No compartas la contraseña en el chat. Se inicia SafeWork con aprovisionamiento de una sola vez; entra con esa cuenta y comprueba el catálogo. Después pulsa Ctrl+C en esa terminal y vuelve a arrancar normalmente desde el IDE. No vuelvas a ejecutar el asistente si la cuenta ya fue creada.

4. Como SUPERADMIN abre **Empresas cliente**, registra una empresa y una cuenta ADMIN nueva. Luego inicia sesión con esa cuenta: su información SST debe comenzar vacía, sin datos de Empresa principal.
5. Deshabilitar una empresa bloquea sus cuentas y sesiones en su siguiente petición; no elimina sus datos.

## Evidencia y límites

- Suite completa: 231 pruebas correctas. Se repitieron 32 de ellas contra MySQL real en la base de ensayo, también correctas.
- Migración ensayada sobre un respaldo, aplicada con un segundo respaldo y verificada conservando las doce tablas y cinco adjuntos.
- JAR con perfil prod y MySQL: formulario de login válido y salud UP, tanto en ensayo como en la base habitual.
- Docker y GitHub Actions preparados; no ejecutados en contenedores ni en GitHub. Docker no está instalado. No existe despliegue cloud ni pagos en esta etapa.
- La carpeta de respaldos externa al repositorio contiene datos personales; nunca debe publicarse.
- Los documentos antiguos fechados en septiembre describen etapas anteriores. Para la matriz actual de roles y aislamiento, consultar EMPRESAS.md y AISLAMIENTO_EMPRESAS.md.
