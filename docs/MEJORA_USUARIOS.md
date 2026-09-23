# Mejora del módulo de usuarios de SafeWork

## Cambios implementados

1. El alta y la edición usan un formulario específico (`UsuarioFormulario`) para que la petición web no pueda modificar directamente campos internos de la entidad.
2. Se validan usuario, nombre, apellido, correo, rol y estado. Se comprueba que usuario y correo no estén duplicados.
3. La contraseña se cifra con el `PasswordEncoder` existente. En una edición, dejarla vacía conserva el hash actual; nunca se muestra en el formulario.
4. Solo se permite eliminar por `POST`, con la protección CSRF de Spring Security. Se añadieron enlaces para editar y mensajes de resultado.
5. Se impide borrar la propia cuenta, desactivar o quitarse el rol de administrador a sí mismo y dejar el sistema sin un administrador activo.
6. Una cuenta marcada `INACTIVO` no puede iniciar sesión.

## Comprobación realizada

`SeguridadRolesTests` y `UsuarioServiceTests`: 63 pruebas correctas con Maven. Las pruebas usan servicios/repositorios simulados; no sustituyen una prueba manual contra la base de datos real.

## Prueba manual sugerida

1. Iniciar SafeWork y entrar como administrador.
2. Crear un usuario; confirmar que aparece en la lista y puede iniciar sesión.
3. Editarlo sin escribir contraseña; confirmar que la contraseña anterior sigue funcionando.
4. Cambiar su contraseña; confirmar que la anterior deja de funcionar.
5. Marcarlo inactivo y confirmar que ya no puede iniciar sesión.
6. Intentar eliminar la propia cuenta y el último administrador activo; debe mostrarse un error.

## Para explicar en la exposición

El controlador recibe y valida el formulario; el servicio aplica las reglas de negocio y cifra la contraseña; el repositorio consulta y persiste usuarios. Spring Security verifica roles, CSRF y el estado activo de la cuenta. La base de datos mantiene los usuarios y sus contraseñas cifradas.
