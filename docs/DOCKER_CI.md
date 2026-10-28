# SafeWork: Docker local y pruebas automáticas

## Alcance y límites

Esta preparación corresponde al piloto multiempresa, no a un servicio SaaS comercial publicado. No activa pagos, no publica imágenes en un registro y no modifica la base MySQL que ya existe en Windows. Ejecutar el workflow en GitHub requiere que estos archivos estén en el repositorio remoto; escribir el archivo no demuestra que GitHub Actions ya haya ejecutado un pipeline.

El equipo debe tener Docker Desktop con Docker Compose, backend WSL 2 y virtualización habilitados. Su instalación o aceptación de licencias no se hace automáticamente. Java 21 y Maven se ejecutan dentro de la fase de construcción de la imagen; el equipo anfitrión no necesita instalarlos para usar Compose.

## Archivos

| Archivo | Función |
| --- | --- |
| `Dockerfile` | Construye el JAR con Java 21 y lo ejecuta en una imagen JRE como usuario sin privilegios (UID 10001). |
| `compose.yaml` | Inicia la aplicación y un MySQL 8.0 independiente, con volumen propio. |
| `.env.example` | Plantilla sin contraseñas reales para la configuración local. |
| `application-prod.properties` | Exige conexión y credenciales externas, valida el esquema, desactiva SQL detallado y reinicio de desarrollo. |
| `.github/workflows/ci.yml` | Compila, ejecuta toda la suite aislada (incluida integración H2), guarda resultados y JAR y arranca la imagen contra MySQL desechable. |

## Arranque local aislado

1. Conserva primero el respaldo y la versión estable. No importes datos personales en una instancia pública de prueba.
2. Copia `.env.example` a `.env` y abre el archivo en tu editor.
3. Establece dos contraseñas diferentes y largas en `SAFEWORK_MYSQL_PASSWORD` y `SAFEWORK_MYSQL_ROOT_PASSWORD`. Una cadena aleatoria de al menos 32 caracteres hexadecimales evita problemas de interpretación de símbolos por `.env`. No uses la contraseña de tu MySQL habitual.
4. Mantén `.env` fuera del control de versiones. No compartas capturas de su contenido, de `docker inspect` ni de `docker compose config`, que pueden mostrar credenciales.
5. Desde la raíz del repositorio, valida primero solo la sintaxis:

   ```powershell
   docker compose config --quiet
   ```

6. Construye e inicia el piloto:

   ```powershell
   docker compose up --build -d
   docker compose ps
   docker compose logs --tail 100 app
   ```

7. Abre `http://localhost:8081/login`. Este puerto no interfiere con el SafeWork actual en `localhost:8080`.

La base interna se llama `safework_piloto`. Flyway aplica las migraciones versionadas y después Hibernate comprueba el esquema con `validate`. La creación inicial de una cuenta administrativa debe seguir el procedimiento de aprovisionamiento de `EMPRESAS.md`: las variables opcionales están desactivadas y vacías en la plantilla. No se incluye una contraseña universal ni se copian automáticamente las cuentas del sistema existente. Retira las credenciales de aprovisionamiento y recrea el contenedor de aplicación después del primer inicio correcto.

Si el puerto 8081 está ocupado, configura `SAFEWORK_HTTP_PORT` en `.env`. MySQL no publica ningún puerto al equipo anfitrión: la aplicación lo accede por el nombre `mysql` en una red privada de Compose. El SQL de la URL desactiva TLS únicamente en esa red local aislada; una conexión a un proveedor externo debe usar sus requisitos de TLS y certificados, no copiar esta URL sin revisar.

## Conservación de los datos

El volumen `mysql_data` del proyecto Compose `safework-piloto` conserva los datos al detener o reemplazar contenedores. Para detenerlos sin borrar datos:

```powershell
docker compose stop
```

Para volver a iniciarlos:

```powershell
docker compose up -d
```

No elimines el volumen desde Docker Desktop ni utilices la opción de eliminación de volúmenes al desmontar Compose: eso eliminaría la base del piloto. No se proporciona un comando destructivo como parte del arranque normal.

Un volumen no es un respaldo. La guía `RESPALDOS_MYSQL.md` describe el mecanismo existente para el MySQL de Windows; no debe asumirse que esos scripts respaldan automáticamente esta nueva base interna de Compose. Antes de usar el piloto para datos importantes, configura y prueba un procedimiento de exportación y restauración específico para `safework_piloto`, guardando copias fuera del volumen y del equipo. La copia estable original debe mantenerse separada.

Cambiar las contraseñas de `.env` no cambia las credenciales de un volumen MySQL ya inicializado. Una rotación requiere modificar la contraseña dentro de MySQL y actualizar la configuración de la aplicación de forma coordinada, no borrar el volumen para resolver el inicio de sesión.

## Qué ejecuta GitHub Actions

El workflow se activa con un push a `main` o `master`, con un Pull Request o manualmente desde la pestaña Actions. Usa permisos de lectura sobre el repositorio y no recibe credenciales del MySQL real.

1. Obtiene el código e instala Java 21 con caché de Maven.
2. Inicia MySQL 8.0 con la base `safework_ci` y credenciales exclusivas del trabajo efímero, para la posterior prueba de arranque de la imagen.
3. Prepara una carpeta vacía para que no se importen adjuntos personales del equipo.
4. Ejecuta `./mvnw --batch-mode --no-transfer-progress clean verify` **sin omitir pruebas**. Las pruebas de integración marcadas con el perfil `test` usan H2 en memoria, su esquema temporal y Flyway desactivado. No se imponen variables `SPRING_DATASOURCE_URL` ni `SPRING_JPA_HIBERNATE_DDL_AUTO` que sobrescriban ese perfil.
5. Publica los resultados de Surefire incluso cuando haya fallos. Solo conserva el JAR y construye la imagen si las pruebas finalizaron correctamente.
6. Comprueba la construcción de la imagen y la sintaxis de Compose, sin publicar la imagen ni desplegarla en cloud.
7. Inicia la imagen construida con 512 MiB de memoria contra el MySQL efímero y comprueba que la página de login responde con formulario y token CSRF. Detiene ese contenedor al finalizar. Esta prueba de arranque no sustituye las pruebas funcionales ni acredita una capacidad para un número concreto de usuarios.

La construcción del Dockerfile por sí sola ejecuta `package -DskipTests`; esta etapa empaqueta, no valida la suite. En CI la construcción ocurre después de `clean verify`, que sí ejecuta todas las pruebas, incluidas las integraciones aisladas H2. Luego el perfil `prod` de la imagen usa MySQL 8.0, aplica Flyway y valida el esquema antes de comprobar `/login`. No se debe usar un build manual exitoso como evidencia de que las pruebas pasaron.

H2 no demuestra por sí solo compatibilidad completa con MySQL. La migración y conservación de datos de una base existente se verifican aparte contra una copia MySQL mediante el procedimiento de migración del piloto; una ejecución documentada de esa comprobación no debe confundirse con la prueba de arranque sobre la base MySQL vacía del workflow.

Los artefactos de CI tienen una retención de 7 días. Las claves visibles `ci-only-...` son credenciales de un servicio descartable dentro del ejecutor, no secretos válidos de desarrollo ni producción. No se usan claves de pagos, tokens cloud ni datos del MySQL local.

La protección de la rama para exigir el resultado `verify` antes de fusionar debe activarse en la configuración del repositorio de GitHub. El archivo YAML no puede imponer por sí solo esa política.

## Comprobaciones antes de exponerlo a Internet

- Probar login, los tres roles y el aislamiento de datos entre empresas, incluyendo URLs directas y descargas.
- Verificar que detener y volver a iniciar no pierde registros ni archivos.
- Configurar HTTPS real y establecer `SAFEWORK_HTTPS=true` en el entorno público. No actives esa opción con HTTP local: el navegador no enviaría la cookie de sesión segura.
- Mantener MySQL sin acceso público, usar permisos mínimos y revisar TLS de la conexión externa.
- Gestionar secretos y respaldos fuera del repositorio; comprobar restauración en otra base.
- Validar consumo de memoria y tiempos de respuesta con la carga prevista.
- Revisar logs sin divulgar contraseñas o información laboral.

Este perfil solo expone la selección de Actuator `health`; no modifica la autorización de Spring Security ni hace públicos detalles de salud o métricas. Mientras no se hayan verificado Docker y GitHub Actions en sus respectivos entornos, deben figurar como preparados y pendientes de ejecución, no como despliegue cloud completado.
