# Guía de integración del frontend con el backend

Este documento resume el contrato observado en la implementación actual de
`ms_security` y el contexto que necesita el equipo frontend para conectar sus
historias de usuario. Los detalles descritos aquí se basan en el código
inspeccionado; los puntos señalados como pendientes deben confirmarse antes de
considerarlos parte del contrato definitivo.

## Resumen

El backend sirve como base para integrar las operaciones de usuarios, roles y
autenticación. A diferencia de lo que podría indicar una guía frontend
anterior, el backend ya tiene operaciones CRUD de usuarios. También cuenta con
endpoints para asignar roles a usuarios y un flujo de inicio de sesión con JWT.

La URL local configurada para el backend es `http://localhost:8181`. Para
consumirlo desde una aplicación Angular en el navegador, hay que habilitar y
verificar CORS para el origen local del frontend.

## Usuarios

El controlador está en
[`UserController.java`](../src/main/java/com/uc/ms_security/controller/UserController.java).
Las respuestas de usuario usan los campos `id`, `name` y `email`; no incluyen
la contraseña.

| Método | Ruta | Request / respuesta |
|---|---|---|
| `GET` | `/api/users` | Devuelve una lista directa de usuarios. No es una respuesta paginada. |
| `GET` | `/api/users/{id}` | Devuelve un usuario. |
| `POST` | `/api/users` | Recibe `name`, `email` y `password`; devuelve el usuario creado con estado `201`. |
| `PUT` | `/api/users/{id}` | Recibe `name`, `email` y `password` opcional; devuelve el usuario actualizado. |
| `DELETE` | `/api/users/{id}` | Elimina el usuario y responde `204` sin cuerpo. |

### DTO y validaciones

- `name`: obligatorio, entre 2 y 100 caracteres.
- `email`: obligatorio y con formato de correo válido. El campo se llama
  `email`, no `mail`.
- `password` al crear: obligatorio, entre 8 y 72 caracteres, debe contener
  mayúscula, minúscula, número y uno de estos caracteres especiales:
  `@$!%*?&`. No se permiten espacios.
- `password` al actualizar: opcional. Si se omite o llega en blanco, la
  contraseña existente se conserva; si se envía, debe cumplir la misma
  política.
- La contraseña se almacena codificada por el backend y no forma parte de las
  respuestas normales de usuario.

Implementación relacionada:

- [`CreateUserDTO.java`](../src/main/java/com/uc/ms_security/dto/user/CreateUserDTO.java)
- [`UpdateUserDTO.java`](../src/main/java/com/uc/ms_security/dto/user/UpdateUserDTO.java)
- [`BaseUserDTO.java`](../src/main/java/com/uc/ms_security/dto/user/BaseUserDTO.java)
- [`UserResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/user/UserResponseDTO.java)
- [`UserService.java`](../src/main/java/com/uc/ms_security/service/UserService.java)

Los correos duplicados producen un conflicto (`409`). Un ID inexistente produce
`404`. La estructura de los errores de negocio e invalidación se implementa en
[`GlobalExceptionHandler.java`](../src/main/java/com/uc/ms_security/exception/GlobalExceptionHandler.java);
el frontend debe manejar también los errores de autenticación y autorización
emitidos por Spring Security.

## Roles y asignaciones usuario-rol

El CRUD de roles está disponible en `/api/roles`:

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/api/roles` | Listar roles. |
| `GET` | `/api/roles/{id}` | Consultar un rol. |
| `POST` | `/api/roles` | Crear un rol. |
| `PUT` | `/api/roles/{id}` | Actualizar un rol. |
| `DELETE` | `/api/roles/{id}` | Eliminar un rol. |

La relación usuario-rol se administra mediante un recurso separado, no como
parte del DTO básico de usuario:

| Método | Ruta | Operación |
|---|---|---|
| `POST` | `/api/user-roles` | Asignar; recibe `{ "userId": number, "roleId": number }`. |
| `GET` | `/api/user-roles` | Listar asignaciones. |
| `GET` | `/api/user-roles/users/{userId}` | Listar roles asignados a un usuario. |
| `GET` | `/api/user-roles/roles/{roleId}` | Listar usuarios asignados a un rol. |
| `DELETE` | `/api/user-roles/{userId}/{roleId}` | Retirar una asignación. |

La asignación duplicada genera un conflicto (`409`) y los IDs no encontrados
producen `404`.

Implementación relacionada:

- [`RoleController.java`](../src/main/java/com/uc/ms_security/controller/RoleController.java)
- [`UserRoleController.java`](../src/main/java/com/uc/ms_security/controller/UserRoleController.java)
- [`AssignRoleRequestDTO.java`](../src/main/java/com/uc/ms_security/dto/userrole/AssignRoleRequestDTO.java)
- [`UserRoleResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/userrole/UserRoleResponseDTO.java)
- [`UserRolesResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/user/UserRolesResponseDTO.java)

Antes de exponer acciones de asignación en la interfaz, hay que confirmar qué
perfiles de usuario pueden realizarlas. Aunque existen operaciones para leer y
eliminar roles, las reglas de integridad y el comportamiento esperado al borrar
roles con asignaciones deben probarse con la API.

## Inicio de sesión y protección de rutas

El backend implementa:

- `POST /api/auth/login`, con request `{ "email": "...", "password": "..." }`.
- Respuesta con `accessToken`, `tokenType` (`Bearer`) y `expiresAt`.
- Token JWT con duración de 15 minutos.
- El endpoint de login es público; el resto de rutas requiere un token Bearer
  válido en el encabezado `Authorization`.

Implementación relacionada:

- [`AuthController.java`](../src/main/java/com/uc/ms_security/controller/AuthController.java)
- [`LoginRequestDTO.java`](../src/main/java/com/uc/ms_security/dto/auth/LoginRequestDTO.java)
- [`AuthResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/auth/AuthResponseDTO.java)
- [`SecurityConfig.java`](../src/main/java/com/uc/ms_security/config/SecurityConfig.java)
- [`JwtService.java`](../src/main/java/com/uc/ms_security/service/JwtService.java)
- [`JwtConfig.java`](../src/main/java/com/uc/ms_security/config/JwtConfig.java)

No se encontraron rutas para renovar o cerrar sesión ni un flujo 2FA en la
implementación inspeccionada. La configuración protege las rutas exigiendo
autenticación, pero no se observaron reglas de autorización por rol o permiso.
Por tanto, si las historias requieren diferentes permisos administrativos, el
backend debe implementarlos: ocultar botones en el frontend no protege la API.

No almacenar, registrar ni mostrar contraseñas o tokens innecesariamente. En
entornos distintos del desarrollo local, usar HTTPS. La autorización y la
revocación de tokens deben acordarse con el equipo backend antes de definir el
comportamiento de la sesión en el frontend.

## Otros recursos disponibles

Hay endpoints para perfil bajo `/api/users/{userId}/profile` y para sesiones
de aplicación bajo `/api/users/{userId}/sessions`. Estos recursos no deben
confundirse con la sesión JWT de autenticación: no se encontraron endpoints de
renovación o cierre de sesión JWT.

Los endpoints adicionales de detalle de usuario incluyen:

- `GET /api/users/{id}/detail`
- `GET /api/users/{id}/detail-with-sessions`
- `GET /api/users/{id}/detail-with-roles`

Antes de mostrar datos de sesión en una pantalla administrativa, revisar el
DTO correspondiente y confirmar que no se expongan credenciales, tokens u
otros datos sensibles.

## Errores, configuración y documentación

- Puerto local: `8181`, configurado en
  [`application.properties`](../src/main/resources/application.properties).
- No se encontró configuración explícita de CORS en el código inspeccionado.
  Configurar en el backend los orígenes reales del frontend.
- No se encontró una dependencia de Swagger/OpenAPI en el `pom.xml`. La
  documentación de este archivo no sustituye una especificación OpenAPI
  publicada.
- Los errores de validación responden `400`; recursos inexistentes, `404`;
  conflictos de negocio, `409`; y errores internos, `500`. Probar en ejecución
  el cuerpo exacto de errores `401` y `403` de Spring Security.
- Verificar localmente que el servicio pueda conectarse a la base de datos y
  que el esquema requerido esté disponible. No compartir credenciales de
  configuración ni incluir secretos en el frontend.

## Archivos Angular necesarios para completar la integración

Para implementar y verificar el trabajo frontend, compartir o revisar estos
archivos:

1. `package.json`, `angular.json` y `tsconfig*.json`, para identificar las
   versiones y los comandos de build y pruebas.
2. `src/app/app.routes.ts`, `src/app/app.config.ts` y los archivos de entorno
   que definan `apiUrl`.
3. `src/app/models/user.model.ts` y `src/app/services/user.service.ts`.
4. La página de usuarios completa (TypeScript, plantilla HTML y estilos).
5. La pantalla de login y los servicios, interceptores o guards de autenticación
   que ya existan.
6. La página CRUD de roles, el modelo y el servicio de roles, como referencia
   para tabla, formularios, confirmación y notificaciones.
7. Los componentes compartidos usados por las páginas: tabla, formulario
   dinámico, diálogo de confirmación y manejo de mensajes.
8. La página de perfiles, porque también consume usuarios, y los archivos de
   menú/sidebar si se necesita agregar navegación.
9. Las pruebas de usuarios, autenticación y componentes compartidos, si existen.

## Secuencia recomendada

1. Iniciar el backend y la base de datos; comprobar el login y las rutas
   disponibles con una herramienta HTTP.
2. Verificar el cuerpo de respuesta y los errores para alta, consulta,
   actualización, eliminación y asignación de roles.
3. Habilitar CORS para el origen local del frontend y comprobar la llamada desde
   el navegador.
4. Apuntar el entorno Angular a `http://localhost:8181`.
5. Implementar el servicio de usuarios con las rutas y DTO confirmados; separar
   los DTO de escritura del modelo de lectura para no incluir contraseña en
   listados o detalles.
6. Conectar la página CRUD reutilizando los componentes y patrones existentes.
7. Implementar el login y el envío de `Authorization: Bearer <accessToken>`
   según el flujo acordado; resolver expiración del token de manera explícita.
8. Ejecutar build, pruebas automatizadas y pruebas manuales de carga, errores,
   validación y permisos.

## Pendientes de confirmar con el equipo backend

- Habilitación de CORS y orígenes permitidos.
- Reglas de autorización por rol/permisos para cada operación.
- Formato exacto de errores `401` y `403` y tratamiento del token vencido.
- Si se necesita renovación, revocación o cierre de sesión para JWT.
- Comportamiento al eliminar usuarios o roles que tengan relaciones.
- DTO y datos permitidos en los endpoints de detalle de sesión.
- Publicación de Swagger/OpenAPI y procedimiento de ejecución en cada entorno.
