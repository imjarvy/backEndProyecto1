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

La URL local configurada para el backend es `http://localhost:8181`. CORS está
configurado para `http://localhost:4200` y permite credenciales.

## Actualización: autenticación JWT mediante cookie

Esta sección describe el contrato actual y **reemplaza cualquier indicación
anterior de guardar el JWT en memoria o enviarlo como Bearer**. El frontend
Angular no está en este workspace; por tanto, sus servicios e interceptores aún
deben adaptarse.

### Rutas de autenticación

| Método | Ruta | Funcionamiento |
|---|---|---|
| `GET` | `/api/auth/csrf` | Devuelve un token CSRF y establece la cookie legible `XSRF-TOKEN`. Llamar primero. |
| `POST` | `/api/auth/login` | Recibe `{ "email": "...", "password": "..." }`; enviar cookie CSRF y encabezado `X-XSRF-TOKEN`. Responde `204` y establece `AUTH_TOKEN` HttpOnly; no devuelve el JWT en el cuerpo. |
| `GET` | `/api/auth/session` | Valida la cookie de autenticación y devuelve los datos seguros `{ "id": number, "email": string }`. |
| `POST` | `/api/auth/logout` | Enviar CSRF; elimina la cookie `AUTH_TOKEN` y responde `204`. |

Las peticiones del navegador deben incluir credenciales (`withCredentials:
true`). `AUTH_TOKEN` usa `HttpOnly`, `Path=/api` y `SameSite=Lax`; `XSRF-TOKEN`
no es HttpOnly para que Angular pueda copiar su valor al encabezado
`X-XSRF-TOKEN` en operaciones mutables. `Secure` se configura con
`COOKIE_SECURE` (por defecto `false` localmente; usar `true` con HTTPS).

CORS permite actualmente `http://localhost:4200` con credenciales. Los JWT
duran 15 minutos. Un token ausente, inválido o vencido produce `401`; la falta
de permiso dinámico o un token CSRF ausente/inválido produce `403`. El logout
borra la cookie del navegador, pero no revoca un JWT copiado hasta que expire.

### Archivos backend tocados

- [`AuthController.java`](../src/main/java/com/uc/ms_security/controller/AuthController.java):
  login con cookie, endpoint CSRF, sesión actual y logout.
- [`SecurityConfig.java`](../src/main/java/com/uc/ms_security/config/SecurityConfig.java):
  activa CSRF y CORS con credenciales; registra las rutas públicas y la
  autenticación dinámica.
- [`CookieBearerTokenResolver.java`](../src/main/java/com/uc/ms_security/authorization/CookieBearerTokenResolver.java):
  extrae `AUTH_TOKEN` para que Spring Resource Server valide el JWT.
- [`SpaCsrfTokenRepository.java`](../src/main/java/com/uc/ms_security/config/SpaCsrfTokenRepository.java):
  configura la cookie CSRF y su ámbito.
- [`CurrentUserResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/auth/CurrentUserResponseDTO.java)
  y [`CsrfTokenResponseDTO.java`](../src/main/java/com/uc/ms_security/dto/auth/CsrfTokenResponseDTO.java):
  respuestas de sesión y CSRF.
- [`application.properties`](../src/main/resources/application.properties):
  propiedades `JWT_COOKIE_NAME` y `COOKIE_SECURE`.
- [`UserController.java`](../src/main/java/com/uc/ms_security/controller/UserController.java):
  elimina el CORS local; ahora se aplica globalmente.
- [`AuthSecurityTests.java`](../src/test/java/com/uc/ms_security/controller/AuthSecurityTests.java):
  verifica cookie, CSRF, sesión, logout y CORS.

`JwtService.java` y `JwtConfig.java` continúan generando y validando/firmando el
JWT, respectivamente; el cambio consiste en entregarlo mediante cookie y
resolverlo desde ella en las siguientes peticiones.

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
- El JWT se entrega en la cookie HttpOnly `AUTH_TOKEN`; consultar la sección
  «Actualización: autenticación JWT mediante cookie» para el contrato vigente.
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

Existe `POST /api/auth/logout`, que elimina la cookie del navegador; no hay
renovación ni revocación inmediata del JWT. Las rutas protegidas usan
autorización dinámica por método HTTP y URL asociada a permisos del usuario.
Ocultar botones en el frontend no sustituye la protección del backend.

No almacenar, registrar ni mostrar contraseñas o tokens innecesariamente. En
entornos distintos del desarrollo local, usar HTTPS. La autorización y la
revocación de tokens deben acordarse con el equipo backend antes de definir el
comportamiento de la sesión en el frontend.

## Otros recursos disponibles

Hay endpoints para perfil bajo `/api/users/{userId}/profile` y para sesiones
de aplicación bajo `/api/users/{userId}/sessions`. Estos recursos no deben
confundirse con la sesión JWT de autenticación, consultable en
`GET /api/auth/session`.

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
- CORS permite actualmente `http://localhost:4200` con credenciales; actualizar
  el origen permitido en `SecurityConfig` para otros entornos.
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
3. Confirmar CORS para el origen del frontend y probar las llamadas con
   credenciales desde el navegador.
4. Apuntar el entorno Angular a `http://localhost:8181`.
5. Implementar el servicio de usuarios con las rutas y DTO confirmados; separar
   los DTO de escritura del modelo de lectura para no incluir contraseña en
   listados o detalles.
6. Conectar la página CRUD reutilizando los componentes y patrones existentes.
7. Adaptar Angular al flujo de cookie: solicitar `/api/auth/csrf`, usar
   `withCredentials: true`, enviar el CSRF en `X-XSRF-TOKEN` para operaciones
   mutables y consultar la sesión en `/api/auth/session`. No esperar el JWT en
   el cuerpo del login ni enviarlo en `Authorization`.
8. Ejecutar build, pruebas automatizadas y pruebas manuales de carga, errores,
   validación y permisos.

## Pendientes para completar la integración

- Actualizar y probar el servicio/interceptor Angular para cookies, CORS con
  credenciales y CSRF; el frontend no está incluido en este workspace.
- Configurar `COOKIE_SECURE=true` y los orígenes correctos en entornos HTTPS.
- Si se necesita renovación o revocación inmediata del JWT, implementarla en
  backend; el logout actual solo borra la cookie.
- Probar el tratamiento de `401` y `403` en la interfaz.
- Comportamiento al eliminar usuarios o roles que tengan relaciones.
- DTO y datos permitidos en los endpoints de detalle de sesión.
- Publicación de Swagger/OpenAPI y procedimiento de ejecución en cada entorno.
