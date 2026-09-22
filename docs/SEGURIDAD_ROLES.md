# SafeWork — autorización por roles

Actualización del 22 de septiembre de 2026, basada en el proyecto actual y la matriz del checkpoint.

| Módulo | ADMIN | SUPERVISOR | TRABAJADOR |
| --- | --- | --- | --- |
| Usuarios y áreas | Gestión | Sin acceso | Sin acceso |
| Trabajadores, riesgos, incidentes, inspecciones y acciones | Gestión | Gestión | Listado de consulta |
| Dashboard | Consulta | Consulta | Consulta |

## Cambios

- `SecurityConfig.java`: autoriza los cinco listados GET exactos a los tres roles; las demás rutas de estos módulos quedan reservadas a ADMIN y SUPERVISOR. Usuarios y áreas siguen reservados a ADMIN.
- `PermisosVistaAdvice.java`: aporta los atributos `esAdministrador` y `puedeGestionarSst` a Thymeleaf a partir del usuario autenticado. Estos atributos controlan visibilidad, no sustituyen los filtros de seguridad.
- Los cinco `lista.html`: ocultan el botón nuevo y la columna de edición/eliminación al trabajador.
- `fragments/navbar.html`: muestra Áreas y Usuarios solo al administrador y corrige etiquetas de cierre sobrantes.
- `SeguridadRolesTests.java`: pruebas de filtros, controladores y plantillas reales con servicios simulados.

No se cambiaron login, BCrypt, entidades, credenciales ni datos de MySQL. No se agregaron dependencias.

## Verificación automatizada

Desde la carpeta del proyecto, ejecutar en PowerShell:

```powershell
.\mvnw.cmd -Dtest=SeguridadRolesTests test
```

Resultado obtenido en la copia de trabajo: 56 pruebas, 0 fallos, 0 errores.
Se comprobaron listados y botones con filas de ejemplo, formularios por URL, eliminaciones con CSRF válido, bloqueo del guardado/actualización del trabajador, módulos administrativos, dashboard, redirección del visitante al login, rechazo de POST sin CSRF y logout.

Las pruebas no usan MySQL: simulan servicios y sesiones autenticadas. No acreditan login con cuentas reales, CRUD persistido ni funcionamiento de la base de datos. La prueba existente `SafeworkApplicationTests.contextLoads` no fue ejecutada en esta etapa porque inicia el contexto completo.

## Prueba manual pendiente con cuentas reales

1. Reiniciar SafeWork desde el proyecto actualizado y abrir `http://localhost:8080/login`.
2. Iniciar sesión como ADMIN. Verificar menús Usuarios/Áreas y controles de gestión en los cinco módulos SST.
3. Cerrar sesión e ingresar como SUPERVISOR. Verificar gestión SST y denegación al escribir `/usuarios` o `/areas` directamente.
4. Cerrar sesión e ingresar como TRABAJADOR. Abrir `/trabajadores`, `/riesgos`, `/incidentes`, `/inspecciones`, `/acciones` y `/dashboard`: deben permitir consulta sin botones de gestión.
5. Con TRABAJADOR, escribir `/incidentes/nuevo`, `/acciones/nueva`, `/riesgos/editar/1`, `/trabajadores/nuevo` y `/inspecciones/nueva`: deben devolver acceso denegado (403), incluso si se escribe la URL manualmente.
6. Cerrar sesión e intentar abrir `/dashboard`: debe volver al login.

Los formularios usan `nuevo` para trabajadores, riesgos e incidentes; `nueva` para inspecciones y acciones. Las operaciones POST no se comprueban escribiéndolas en la barra del navegador; los intentos directos con CSRF válido están cubiertos en las pruebas automatizadas.

## Límites y siguiente etapa

La consulta del trabajador muestra el listado completo, conforme a la matriz del checkpoint; no se implementó filtrado por trabajador o por área. No hay pantalla de detalle nueva.
Se conservó el comportamiento existente de las rutas no enumeradas (`authenticated`). La eliminación de usuarios sigue usando GET como en el código recibido; migrarla a POST con CSRF corresponde a la siguiente mejora de Usuarios.
Siguen pendientes edición/estado/validaciones de usuarios, bloqueo de usuarios inactivos y verificación integral del dashboard y las relaciones. No se presentan estas tareas como terminadas.
