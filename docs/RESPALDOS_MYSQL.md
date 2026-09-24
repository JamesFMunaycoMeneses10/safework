# Respaldos y recuperación de SafeWork

Estos scripts respaldan la base MySQL `safework_db` en Windows. Guardan los archivos fuera del repositorio, generan una suma SHA-256, conservan 30 días y permiten restaurar primero en una base separada para validar la copia.

## Requisitos y credenciales

- Windows PowerShell 5.1 o posterior.
- Cliente MySQL instalado y `mysql`, `mysqldump` y `mysql_config_editor` disponibles en `PATH`.
- El usuario MySQL debe tener permisos para leer los datos y rutinas de SafeWork. Para restaurar también necesita permisos para crear, eliminar e importar bases.

Configura una sola vez un perfil de credenciales de MySQL para la cuenta de Windows que ejecutará el respaldo. La herramienta solicita la contraseña de manera interactiva:

```powershell
mysql_config_editor set --login-path=safework-backup --host=localhost --user=TU_USUARIO --password
mysql --login-path=safework-backup --execute="SELECT 1"
```

Si MySQL está en otro servidor o puerto, usa esos datos en `--host` y `--port`. El perfil se almacena en la cuenta de Windows y la contraseña no se escribe en los scripts ni en la tarea programada.

## Crear una copia manual

Desde la carpeta del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup-mysql.ps1
```

Por defecto, los `.sql`, sus archivos `.sha256` y el registro diario se guardan en `%LOCALAPPDATA%\SafeWork\Backups\MySQL`. La carpeta nueva restringe permisos al usuario actual y a SYSTEM. Para guardarlos en otra unidad, por ejemplo un disco cifrado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup-mysql.ps1 -OutputDirectory "E:\SafeWorkBackups\MySQL"
```

El script publica una copia solo después de que `mysqldump` termina sin errores y produce un archivo no vacío. Calcula SHA-256 y elimina únicamente copias propias de `safework_db` con más de 30 días. Puedes cambiar la retención con `-RetentionDays 60`.

## Programar el respaldo diario

Una vez configurado el perfil MySQL, registra una tarea diaria a las 2:00 a. m.:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-backup-task.ps1 -RunAt "02:00"
```

La tarea se ejecuta como el usuario actual cuando inicia sesión en Windows; si el equipo estaba apagado a la hora programada, Windows intentará ejecutarla al volver a estar disponible. Para revisar el resultado, abre el Programador de tareas y revisa el historial de **SafeWork MySQL Backup** o el registro en la carpeta de respaldos. Para reemplazar o quitar la tarea se puede administrar desde el Programador de tareas.

## Validar una copia sin reemplazar la base actual

La restauración por defecto usa un nombre nuevo y rechaza bases existentes. Primero elige una copia reciente y restáurala en una base temporal:

```powershell
$copia = Get-ChildItem "$env:LOCALAPPDATA\SafeWork\Backups\MySQL\safework_db_*.sql" |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restore-mysql.ps1 `
  -BackupFile $copia -TargetDatabase safework_restore_prueba
```

El script comprueba SHA-256 si existe el archivo asociado, importa el SQL y confirma que se hayan creado tablas. Después, conecta una instancia de pruebas de SafeWork a `safework_restore_prueba` y valida inicio de sesión, datos y descarga de evidencias. Esta comprobación es necesaria: que el archivo exista y tenga checksum no demuestra por sí solo que la restauración de la aplicación funciona.

La copia restaurada queda en la base temporal. Cuando termines de validarla, puedes conservarla para pruebas o eliminarla de forma explícita desde MySQL Workbench. No elimines la base de producción al limpiar las pruebas.

## Recuperar la base de producción

Detén SafeWork para evitar escrituras durante la recuperación. Revisa el nombre y fecha de la copia, y luego ejecuta el reemplazo indicando dos veces el nombre exacto de la base:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restore-mysql.ps1 `
  -BackupFile "C:\ruta\safework_db_20260924_020000.sql" `
  -TargetDatabase safework_db `
  -ReplaceDatabase `
  -ConfirmDatabaseName safework_db
```

El script exige además escribir `RESTAURAR safework_db` en la consola. Antes de eliminar la base actual, crea una copia de seguridad adicional en `pre-restore`. Si la importación de la copia elegida falla, intenta recuperar automáticamente la base desde esa copia previa. Conserva ambas copias hasta validar SafeWork y los adjuntos.

Los respaldos contienen datos personales y evidencia laboral. Mantén las copias en una unidad cifrada, limita quién puede acceder a ellas y guarda al menos una copia en otro dispositivo o ubicación. La tarea diaria por sí sola no protege frente a la pérdida física del equipo.
