# ms_security

Backend REST para gestionar usuarios, autenticación JWT mediante cookie y
sesiones asociadas a usuarios. La aplicación usa Spring Boot, Spring Security,
JPA y MySQL.

## Configuración

Configura estas variables antes de iniciar la aplicación:

| Variable | Obligatoria | Descripción |
|---|---:|---|
| `JWT_SECRET` | Sí | Clave aleatoria para firmar JWT, con al menos 32 bytes UTF-8. No uses una clave de ejemplo ni la guardes en el repositorio. |
| `COOKIE_SECURE` | No | `true` por defecto. Mantén este valor en producción; para desarrollo local sobre HTTP puedes establecer `false`. |
| `JWT_COOKIE_NAME` | No | Nombre de la cookie JWT; por defecto `AUTH_TOKEN`. |
| `SEED_ADMIN_PASSWORD` | No | Contraseña inicial del usuario administrador semilla. |

La conexión a MySQL se configura mediante `spring.datasource.*` en
`src/main/resources/application.properties`. CORS permite actualmente
`http://localhost:4200` y credenciales.
Al iniciar, la aplicación migra automáticamente la columna antigua
`permissions.model` a `permissions.entity` y elimina el índice único antiguo
de `permissions(url, method)` si detecta ese esquema.

## Flujo de autenticación normal

### 1. Obtener el token CSRF

Antes de enviar operaciones `POST`, `PUT`, `PATCH` o `DELETE`, el cliente debe
solicitar un token CSRF:

```http
GET /api/auth/csrf
```

La respuesta contiene un token y el servidor establece la cookie `XSRF-TOKEN`.
El frontend debe conservar las cookies y enviar el token recibido en el
encabezado `X-XSRF-TOKEN` para las operaciones que modifican datos.

### 2. Iniciar sesión

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <csrf-token>
Cookie: XSRF-TOKEN=<csrf-cookie>

{
  "email": "persona@example.com",
  "password": "contraseña"
}
```

Las credenciales se validan en `AuthService`. Si son correctas:

1. `JwtService` crea un JWT de 15 minutos. El `sub` identifica al usuario, el
   claim `email` contiene el correo y el `jti` identifica esta instancia del
   token.
2. `SessionService` registra el `jti`, la expiración y el usuario en la tabla
   `sessions`.
3. `AuthController` devuelve `204 No Content` y establece el JWT en una cookie
   `HttpOnly`, `SameSite=Lax`, con ruta `/api` y atributo `Secure` según
   `COOKIE_SECURE`.

El JWT no se devuelve en el cuerpo JSON ni debe guardarse en `localStorage` ni
añadirse manualmente a `Authorization: Bearer`. El navegador lo envía como
cookie.

### 3. Mantener y comprobar la sesión

El frontend debe configurar sus peticiones HTTP con `withCredentials: true`
(o la opción equivalente del cliente HTTP) tanto para obtener como para enviar
cookies en solicitudes al backend.

Al cargar o recargar la aplicación puede consultar:

```http
GET /api/auth/session
```

- `200 OK`: devuelve la identidad básica, por ejemplo
  `{"id": 12, "email": "persona@example.com"}`.
- `401 Unauthorized`: no hay autenticación válida.

El token se obtiene de la cookie mediante `CookieBearerTokenResolver`. Además
de validar firma, emisor y expiración del JWT, `JwtConfig` comprueba que exista
una sesión cuyo `jti`, usuario y expiración coincidan. Así, quitar la sesión
del registro invalida el token inmediatamente, aunque su JWT todavía no haya
expirado.

### 4. Cerrar sesión

```http
POST /api/auth/logout
X-XSRF-TOKEN: <csrf-token>
Cookie: AUTH_TOKEN=<jwt>; XSRF-TOKEN=<csrf-cookie>
```

El backend elimina de `sessions` el registro correspondiente al `jti` del JWT
autenticado y expira la cookie. Si no se envía un JWT válido, el endpoint puede
limitarse a expirar la cookie del navegador; no hay una sesión autenticada que
revocar en ese caso.

## Usuarios

Las operaciones de usuarios están bajo `/api/users` y sujetas al sistema de
permisos dinámicos configurado en Spring Security:

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/users` | Crea un usuario; la contraseña se almacena codificada con BCrypt. |
| `GET` | `/api/users` | Lista usuarios autorizados. |
| `GET` | `/api/users/{id}` | Consulta un usuario. |
| `PUT` | `/api/users/{id}` | Actualiza datos básicos y, si se envía, contraseña. |
| `DELETE` | `/api/users/{id}` | Elimina el usuario. |
| `GET` | `/api/users/{id}/detail` | Consulta usuario y perfil. |
| `GET` | `/api/users/{id}/detail-with-sessions` | Consulta usuario y metadatos de sesiones propias. |
| `GET` | `/api/users/{id}/detail-with-roles` | Consulta usuario y roles. |

La entidad `User` tiene una relación uno-a-muchos con `Session`. La ruta de
detalle con sesiones compara el identificador de ruta con el usuario autenticado;
no permite consultar las sesiones de otra identidad.

## Sesiones

El registro `Session` representa la vigencia de un JWT de autenticación: guarda
el `jti` (en la columna existente `token`), la expiración y la referencia al
usuario. No guarda el JWT bearer completo ni un código 2FA en el modelo actual.
La respuesta `SessionResponseDTO` solo expone `id` y `expiration`.

Las rutas de administración de sesión están bajo
`/api/users/{userId}/sessions`:

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/users/{userId}/sessions` | Crea un registro de sesión. |
| `GET` | `/api/users/{userId}/sessions` | Lista metadatos de sesiones. |
| `GET` | `/api/users/{userId}/sessions/{sessionId}` | Consulta una sesión. |
| `PUT` | `/api/users/{userId}/sessions/{sessionId}` | Actualiza identificador/expiración. |
| `DELETE` | `/api/users/{userId}/sessions/{sessionId}` | Elimina el registro y revoca el JWT asociado. |

El DTO de escritura espera:

```json
{
  "tokenId": "identificador-jti",
  "expiration": "2026-10-29T18:30:00"
}
```

La expiración debe estar en el futuro. Todas las rutas de sesión verifican que
`userId` coincida con el usuario autenticado y también requieren el permiso de
ruta correspondiente. Los registros que crea automáticamente el login son los
que vinculan las sesiones con JWT emitidos por el backend.

## Archivos principales de autenticación y sesiones

| Archivo | Responsabilidad |
|---|---|
| `src/main/java/com/uc/ms_security/controller/AuthController.java` | Endpoints de login, CSRF, estado de sesión y logout; emisión y expiración de cookie. |
| `src/main/java/com/uc/ms_security/service/AuthService.java` | Verifica credenciales y registra la sesión después de un login válido. |
| `src/main/java/com/uc/ms_security/service/JwtService.java` | Emite JWT con `sub`, `email`, expiración y `jti`. |
| `src/main/java/com/uc/ms_security/dto/auth/AuthResponseDTO.java` | Respuesta interna de login con token, tipo, `jti` y expiración; el controlador solo envía el token en cookie. |
| `src/main/java/com/uc/ms_security/config/JwtConfig.java` | Configura firma y validación de JWT, incluida la comprobación de sesión activa. |
| `src/main/java/com/uc/ms_security/authorization/CookieBearerTokenResolver.java` | Resuelve el JWT desde la cookie de autenticación. |
| `src/main/java/com/uc/ms_security/config/SecurityConfig.java` | Configura autorización, CSRF, CORS y estado stateless de Spring Security. |
| `src/main/java/com/uc/ms_security/entity/User.java` | Usuario y relación uno-a-muchos con sesiones. |
| `src/main/java/com/uc/ms_security/entity/Session.java` | Persistencia de `jti`, expiración y usuario propietario. |
| `src/main/java/com/uc/ms_security/repository/SessionRepository.java` | Consultas y revocación de registros de sesión. |
| `src/main/java/com/uc/ms_security/service/SessionService.java` | Creación en login, revocación y operaciones de sesión con verificación de propietario. |
| `src/main/java/com/uc/ms_security/controller/SessionController.java` | Endpoints CRUD de sesiones. |
| `src/main/java/com/uc/ms_security/dto/session/SessionRequestDTO.java` | Contrato de escritura de sesión (`tokenId`, `expiration`). |
| `src/main/java/com/uc/ms_security/dto/session/SessionResponseDTO.java` | Contrato de respuesta sin bearer token ni código 2FA. |
| `src/main/java/com/uc/ms_security/mapper/SessionMapper.java` | Mapeo entre entidad y DTO de sesión. |
| `src/main/java/com/uc/ms_security/controller/UserController.java` | Endpoints de usuario y consulta de sesiones del usuario autenticado. |
| `src/main/java/com/uc/ms_security/service/UserService.java` | Lógica de usuarios y comprobación de identidad para detalle con sesiones. |
| `src/main/java/com/uc/ms_security/exception/GlobalExceptionHandler.java` | Respuestas de error, incluido `403 Forbidden` por acceso denegado. |
| `src/main/resources/application.properties` | Configuración de JWT, cookie y conexión. |
| `src/test/java/com/uc/ms_security/controller/AuthSecurityTests.java` | Pruebas web de cookie, autenticación, sesión activa y logout. |
| `src/test/java/com/uc/ms_security/service/AuthServiceTests.java` | Pruebas de login y registro de la sesión. |
| `src/test/java/com/uc/ms_security/service/SessionServiceTests.java` | Pruebas del registro/revocación y de aislamiento entre usuarios. |
| `src/test/java/com/uc/ms_security/service/UserServiceSessionAuthorizationTests.java` | Prueba del control de identidad en el detalle de usuario con sesiones. |

## Datos de sesiones existentes y pruebas

La configuración de JPA usa `ddl-auto=update`; no elimina automáticamente
columnas o datos antiguos. Las filas históricas creadas antes de almacenar el
`jti` pueden contener el JWT anterior en la columna `token` y no se consideran
sesiones activas por la nueva validación. La columna histórica `code_2fa` puede
seguir en la base de datos. No se borran esos datos automáticamente; planifica
una migración y rotación de tokens por separado si deben retirarse.

Ejecuta la suite de pruebas con:

```powershell
.\mvnw.cmd test
```

Las pruebas incluyen validación de JWT con sesión activa/inactiva, revocación al
cerrar sesión, registro del `jti`, y rechazo de acceso de un usuario a sesiones
de otra identidad.
