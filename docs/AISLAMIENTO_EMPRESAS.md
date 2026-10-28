# Aislamiento de empresas en SafeWork (base SaaS, sin pagos)

## Identidad y alcance

El catálogo `empresas` identifica a cada organización. Los 12 modelos persistentes anteriores heredan de `EmpresaEntidad` y contienen `empresa_id`: usuarios, áreas, trabajadores, riesgos, incidentes, inspecciones, hallazgos, acciones, historial de acciones, auditoría, archivos adjuntos y entregas de EPP.

El usuario no elige ni envía la empresa al guardar un formulario. La autenticación busca una cuenta por su nombre globalmente único y devuelve `EmpresaPrincipal`, que incluye el identificador de usuario y de empresa. `EmpresaContexto` obtiene la empresa de esa identidad validada. Los nombres de usuario y los correos conservan la unicidad global del sistema previo; el DNI del trabajador es único dentro de cada empresa y puede repetirse entre empresas.

`SUPERADMIN` administra el catálogo de la plataforma y la provisión de empresas, no sus registros SST. Para ese rol el contexto devuelve el identificador centinela `-1`, no un acceso global ni la empresa principal. El administrador de empresa conserva el rol `ADMIN` y administra únicamente su organización. Una empresa inactiva bloquea sus cuentas, pero no bloquea al administrador de plataforma.

## Capas de protección

1. Hibernate 7.4 aplica `@TenantId` a las consultas y a las cargas por identificador. La empresa se asigna automáticamente al persistir y su columna no se actualiza mediante edición.
2. Sin identidad de empresa, las consultas usan `-1` y no muestran registros. No existe una empresa principal implícita para visitantes o cuentas desconocidas.
3. `EmpresaIntegridadListener` comprueba, antes de cada alta o actualización, que las relaciones con área, trabajador, usuario, riesgo, incidente, inspección y acción pertenezcan a la misma organización. Usa consultas JDBC parametrizadas con nombres de tabla de las entidades, no valores de la petición.
4. Los servicios también deben resolver por repositorios aislados los identificadores recibidos y comprobar relaciones. Las restricciones compuestas de la migración MySQL agregan una defensa en la base de datos.
5. `EmpresaSesionFilter`, registrado en la cadena de Spring Security, consulta el estado de la cuenta y de la empresa en cada petición autenticada. Si la cuenta fue eliminada, inactivada, cambió de empresa o cambió de rol, invalida la sesión anterior. El estado de la empresa también se comprueba al iniciar sesión.
6. La autorización por rol sigue siendo necesaria: pertenecer a una empresa no permite a un trabajador gestionar usuarios o revisar evidencias como supervisor.

La consulta JDBC global de `UsuarioAccesoRepository` se limita a autenticar y verificar el estado de la sesión. Las operaciones globales de catálogo/provisión se reservan al administrador de plataforma. Cualquier futura consulta SQL nativa sobre datos SST debe incluir explícitamente la empresa: Hibernate no puede filtrar automáticamente SQL nativo.

`EmpresaContexto.ejecutar` permite tareas internas con empresa explícita y restaura el contexto mediante `finally`; no debe exponerse como mecanismo para seleccionar una empresa desde una petición web.

## Pruebas realizadas en esta etapa

`EmpresaAislamientoTests` utiliza Hibernate real y H2 temporal en modo compatible con MySQL, mediante el perfil `test`. Solo escribe en una base cuyo nombre contiene `_test` o termina en `_ci`; crea dos empresas propias y elimina únicamente sus filas al finalizar.

- 12 casos de listados/conteos, uno por modelo persistente.
- 12 casos de acceso por identificador directo que comprueban que otra empresa no es visible.
- Filtros escalares y descargas de archivos, referencias ajenas, rechazo sin empresa, limpieza del contexto, eliminación por ID ajeno, edición por `merge`, referencias diferidas y unicidad de DNI por empresa.
- Total: 32 casos de aislamiento.
- 8 casos unitarios adicionales comprueban la invalidación de sesiones y el alcance de SUPERADMIN.
- Las 15 pruebas de autenticación existentes se adaptaron al repositorio global de acceso y conservaron sus comprobaciones de BCrypt, CSRF y cuentas inactivas.

Comando de verificación independiente:

```powershell
.\mvnw.cmd -B -ntp -Dtest=EmpresaAislamientoTests,EmpresaSesionFilterTests,LoginAutenticacionTests test
```

Resultado local: 55 casos, cero fallos, cero errores. La advertencia de restricción única del caso de DNI duplicado es esperada: la prueba exige que ese intento sea rechazado.

Estas pruebas validan la lógica de aislamiento de Hibernate y de sesión. No acreditan por sí solas que una migración MySQL, una restauración, Docker o un despliegue cloud se hayan ejecutado; esas verificaciones deben documentarse por separado.

### Verificación HTTP integrada

`EmpresaWebAislamientoTests` agrega 21 casos con MockMvc, inicio de sesión BCrypt real, CSRF, filtro de sesión, servicios, repositorios, Hibernate y Thymeleaf reales. Usa otra base H2 temporal (`safework_web_test`), sin mocks de servicios. Mantiene `open-in-view=true`, como la configuración actual de producción del piloto, para conservar la carga diferida de las vistas existentes.

Comprueba diez pantallas de módulos, los conteos del dashboard, archivos e historial por ID, exportaciones CSV y XLSX, entregas propias del trabajador, rechazo de referencias ajenas con 404, intentos de seleccionar otra empresa mediante parámetros, escrituras ajenas sin modificar la fila ni devolver error 500, separación entre administrador de empresa y SUPERADMIN, plantillas de plataforma y protección de sus escrituras mediante rol y CSRF. También verifica que una contraseña rechazada al registrar una empresa no se refleje en el HTML.

Resultado de ejecución independiente: 21 pruebas, cero fallos y cero errores. Este resultado es adicional a las 55 pruebas de la sección anterior y no acredita un despliegue público.

## Límites de esta entrega

No se agregaron pasarela de pagos, planes comerciales, facturación, dominios por cliente ni una promesa de disponibilidad productiva. Esta es la base multiempresa para continuar después con suscripciones. Las migraciones, la copia recuperable y los ensayos de despliegue deben completarse y verificarse antes de aplicar esta versión a los datos de uso diario.
