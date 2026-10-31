# Funcionamiento de SESSION, JWT_SECRET y cookies HttpOnly
## Módulo de Seguridad – Spring Boot + Angular

Este documento explica cómo debería funcionar la gestión de sesiones según las historias de usuario del **Anexo 1 – Módulo Seguridad**, especialmente HU-SE-08 (Autenticarse y gestionar sesión) y HU-SE-10 (Gestionar autenticación de dos factores).

---

## 1. Conceptos principales

Es importante diferenciar estos cuatro elementos:

- **SESSION:** representa la sesión interna que el backend administra y almacena en la base de datos.
- **JWT:** es el token que permite identificar y validar la sesión en las peticiones posteriores.
- **JWT_SECRET:** es la clave secreta que utiliza el backend para firmar y verificar los JWT.
- **Cookie HttpOnly:** es el mecanismo mediante el cual el backend entrega el token al navegador para que Angular no tenga que almacenarlo ni leerlo directamente.

## 2. ¿Cómo debería funcionar SESSION?

Según HU-SE-08:

- Cada autenticación exitosa debe generar una sesión interna.
- Cada sesión debe tener un identificador o token y una fecha de expiración.
- Debe poder identificarse al usuario asociado.
- Una sesión expirada no puede realizar operaciones protegidas.
- Al cerrar sesión, el token debe dejar de ser válido.

Una estructura lógica de la entidad podría ser:

| Campo | Descripción |
|---|---|
| `id` | Identificador de la sesión. |
| `token` | JWT generado o identificador asociado al token. |
| `expiration` | Fecha y hora de expiración. |
| `code2FA` | Código temporal, si aplica al flujo de autenticación. |
| `user_id` | Usuario propietario de la sesión. |

Un usuario puede tener varias sesiones, por ejemplo, si inicia sesión desde su computador y posteriormente desde otro dispositivo.

> La historia de usuario exige como mínimo un identificador o token, una fecha de expiración y la posibilidad de identificar al usuario. Los campos y la estrategia exacta de almacenamiento son decisiones de implementación.

## 3. ¿Qué función cumple JWT_SECRET?

Un comando de PowerShell como este genera una clave aleatoria criptográficamente segura de 256 bits, la codifica en Base64 y la guarda en una variable de entorno:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$b = New-Object byte[] 32
$rng.GetBytes($b)
$env:JWT_SECRET = [Convert]::ToBase64String($b)
```

Su función es permitir que Spring Boot firme los JWT y posteriormente compruebe que no han sido modificados.

El proceso es:

1. El usuario inicia sesión.
2. El backend valida sus credenciales.
3. El backend genera un JWT.
4. Utiliza `JWT_SECRET` para firmar ese JWT.
5. En peticiones posteriores, verifica la firma utilizando la misma clave.

**JWT_SECRET no es el token del usuario.** Es una clave interna del backend que nunca debe enviarse al frontend ni guardarse en la cookie.

Ejemplo de referencia en `application.properties`:

```properties
jwt.secret=${JWT_SECRET}
```

La propiedad y el nombre exacto deben coincidir con la configuración real del proyecto.

### Consideraciones sobre JWT_SECRET

- No generar una clave nueva en cada arranque de Spring Boot.
- Generar una clave estable para cada entorno y guardarla fuera de Git.
- No subirla a GitHub ni compartirla en archivos versionados.
- Si cambia la clave, los JWT firmados con la clave anterior dejarán de validar.
- Base64 es una codificación, no un cifrado.

## 4. ¿Cómo se genera y guarda el JWT?

Un JWT tiene tres partes:

```text
HEADER.PAYLOAD.SIGNATURE
```

Su contenido puede incluir, por ejemplo:

```json
{
  "sub": "15",
  "sessionId": "982",
  "iat": 1793365200,
  "exp": 1793368800
}
```

| Campo | Función |
|---|---|
| `sub` | Identificador del usuario. |
| `sessionId` | Identificador de la sesión en la base de datos. |
| `iat` | Fecha de creación del token. |
| `exp` | Fecha de expiración del token. |

La firma permite comprobar que el contenido no fue modificado.

**Importante:** un JWT firmado está codificado, pero no cifrado. No incluir contraseñas, secretos ni información sensible en su contenido.

### ¿Qué guardar en SESSION?

Para este proyecto se puede guardar el JWT generado en el campo `token`, junto con la fecha de expiración y el usuario asociado, siguiendo el modelo del documento.

Como mejora de seguridad, se puede almacenar un hash del token o utilizar un identificador `jti` para relacionarlo con la sesión, en lugar de conservar el JWT completo en texto plano. Esta es una decisión de implementación, no un requisito explícito del anexo.

## 5. ¿Cómo funcionan las cookies HttpOnly?

La cookie conecta el backend Spring Boot con Angular. La idea es que Angular no tenga que guardar manualmente el JWT en `localStorage` ni en `sessionStorage`.

### Paso 1. Angular envía las credenciales

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "mail": "usuario@gmail.com",
  "password": "123456"
}
```

### Paso 2. Spring Boot valida las credenciales

El backend:

1. Busca al usuario por correo.
2. Comprueba su contraseña utilizando BCrypt.
3. Verifica que el usuario esté habilitado.
4. Comprueba si tiene 2FA habilitado.

Si tiene 2FA, todavía no debe entregar una sesión completamente autenticada. Primero debe validar el segundo factor.

### Paso 3. Spring Boot genera la sesión

Cuando la autenticación esté completa:

- Crea el registro `SESSION` en la base de datos.
- Genera el JWT.
- Establece su fecha de expiración.
- Relaciona la sesión con el usuario.

### Paso 4. El backend entrega una cookie

Ejemplo ilustrativo de respuesta HTTP:

```http
HTTP/1.1 200 OK
Set-Cookie: accessToken=eyJhbGciOiJIUzI1NiJ9...; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=3600
```

| Atributo | Función |
|---|---|
| `HttpOnly` | JavaScript no puede leer directamente la cookie. |
| `Secure` | Solo se envía mediante HTTPS. |
| `SameSite` | Controla cuándo se envía la cookie entre sitios. |
| `Path=/` | Permite enviarla a las rutas correspondientes del servidor. |
| `Max-Age=3600` | Establece una duración de una hora. |

Los valores son ilustrativos. La duración de la cookie debe ser coherente con la expiración del JWT y de la sesión.

Para desarrollo local con HTTP puede ser necesaria una configuración diferente de `Secure` y `SameSite`. No se debe trasladar esa configuración de desarrollo a producción sin revisar el despliegue.

### Paso 5. Angular continúa trabajando normalmente

Angular no necesita recibir el JWT en el cuerpo de la respuesta. El navegador administra la cookie y la envía automáticamente en las siguientes peticiones que correspondan.

Ejemplo conceptual:

```http
GET /api/users
Cookie: accessToken=eyJhbGciOiJIUzI1NiJ9...
```

## 6. ¿Cómo se validan las siguientes peticiones?

Cuando el usuario realiza una petición protegida, el flujo recomendado es:

1. Angular realiza una petición, por ejemplo `GET /api/users`.
2. El navegador adjunta la cookie correspondiente.
3. Spring Security extrae el JWT de la cookie.
4. El backend valida la firma y la expiración del JWT.
5. El backend comprueba que la sesión asociada exista y siga activa en la base de datos.
6. El backend comprueba los roles y permisos del usuario.
7. Si todo es válido, permite ejecutar la operación; de lo contrario, rechaza la petición.

Hay tres validaciones distintas:

1. **JWT:** ¿El token es auténtico y no está expirado?
2. **SESSION:** ¿La sesión todavía existe y está activa en la base de datos?
3. **Permisos:** ¿El usuario tiene autorización para realizar esta operación?

No basta con validar la firma del JWT. Si el usuario cerró sesión, el backend debe reconocer que esa sesión ya no es válida, aunque el JWT todavía no haya expirado.

JWT por sí solo no proporciona revocación inmediata; la comprobación de la sesión permite implementar esa invalidación.

## 7. ¿Cómo debería funcionar el cierre de sesión?

Según HU-SE-08, cerrar sesión debe invalidar el token asociado.

Flujo recomendado:

1. Angular envía `POST /api/auth/logout`.
2. El navegador adjunta automáticamente la cookie.
3. Spring Boot identifica el JWT y su sesión.
4. El backend invalida la sesión en MySQL.
5. El backend elimina la cookie mediante `Set-Cookie`, estableciendo su expiración en el pasado o `Max-Age=0`.
6. El usuario ya no puede utilizar esa sesión.

Ejemplo de respuesta:

```http
HTTP/1.1 200 OK
Set-Cookie: accessToken=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0
```

**Eliminar la cookie no es suficiente.** También se debe invalidar la sesión en la base de datos. De lo contrario, alguien que conserve una copia del JWT podría seguir utilizándolo si todavía es válido.

## 8. ¿Cómo encaja el 2FA en SESSION?

HU-SE-10 establece que una sesión no debe quedar completamente autenticada hasta que el segundo factor haya sido validado.

Se pueden distinguir dos estados:

### Estado 1. Autenticación pendiente

- Correo y contraseña validados.
- Se genera un proceso temporal de autenticación.
- Se genera o asocia un código 2FA.
- El usuario todavía no puede acceder a recursos protegidos.

### Estado 2. Sesión autenticada

- Código 2FA validado.
- Se crea la sesión definitiva.
- Se genera el JWT.
- Se entrega la cookie HttpOnly.
- El usuario puede acceder a sus recursos autorizados.

**Recomendación:** no utilizar el mismo JWT de acceso definitivo para representar el proceso pendiente de 2FA. Se puede implementar un mecanismo temporal independiente.

## 9. ¿Cómo funciona cuando el usuario ingresa mediante OAuth?

El documento contempla Google, GitHub y Microsoft. El proceso cambia únicamente en la validación inicial:

| Credenciales propias | OAuth |
|---|---|
| Correo y contraseña. | Google, GitHub o Microsoft. |
| BCrypt valida la contraseña. | El proveedor valida la identidad. |
| Se comprueba 2FA si está habilitado. | No requiere el 2FA interno del proyecto, según el anexo. |
| Se crea SESSION. | Se crea SESSION. |
| Se genera JWT. | Se genera JWT. |
| Se entrega cookie. | Se entrega cookie. |

Independientemente del método de autenticación, el resultado final debe ser el mismo: un usuario interno con una sesión interna.

Esto sigue el flujo general descrito en el anexo.

## 10. Configuraciones necesarias en Angular y Spring Boot

### Angular

Las peticiones deben permitir el envío de cookies:

```typescript
this.http.post(
  'http://localhost:8181/api/auth/login',
  credentials,
  { withCredentials: true }
);
```

También se debe configurar el envío de credenciales en las peticiones posteriores, por ejemplo, mediante un interceptor HTTP.

### Spring Boot

Se deben configurar:

- CORS permitiendo el origen exacto de Angular.
- `allowCredentials(true)`.
- Un filtro que extraiga el JWT desde la cookie.
- Validación de firma y expiración del JWT.
- Validación de la sesión en la base de datos.
- Autorización mediante roles y permisos.
- Protección CSRF para las peticiones que modifican información.

**Importante:** utilizar cookies HttpOnly no elimina el riesgo CSRF. Se debe implementar una estrategia de protección CSRF compatible con Angular.

## 11. Arquitectura general del módulo de seguridad

```text
                    ANGULAR FRONTEND
             Login / Peticiones HTTP / Logout
                           |
                           | Cookie HttpOnly
                           v
                 SPRING BOOT BACKEND
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     AuthController    JWT Service    Security Filter
          |                |                |
          |          JWT_SECRET             |
          |                |                |
          +----------------+----------------+
                           |
                           v
                   Authorization
                  Roles y permisos
                           |
                           v
                     MYSQL DATABASE
          +----------------+----------------+
          |                |                |
          v                v                v
         USER           SESSION            ROLE
                                             |
                                             v
                                         PERMISSION
```

### Responsabilidades principales

| Componente | Responsabilidad |
|---|---|
| `AuthController` | Login, OAuth y logout. |
| `JwtService` | Crear y validar tokens. |
| `Security Filter` | Leer la cookie y autenticar peticiones. |
| `Authorization` | Validar roles y permisos. |
| `JWT_SECRET` | Firmar y verificar JWT. |
| `USER` | Almacenar usuarios. |
| `SESSION` | Administrar las sesiones activas. |
| `ROLE` y `PERMISSION` | Administrar la autorización. |

## 12. Orden recomendado de implementación

| Orden | Implementación |
|---:|---|
| 1 | Configurar `JWT_SECRET` como variable de entorno estable. |
| 2 | Implementar generación y validación JWT. |
| 3 | Implementar creación de SESSION en MySQL. |
| 4 | Implementar login con correo y contraseña. |
| 5 | Entregar JWT mediante cookie HttpOnly. |
| 6 | Implementar filtro de autenticación que lea la cookie. |
| 7 | Validar SESSION en cada petición protegida. |
| 8 | Implementar logout e invalidación de sesión. |
| 9 | Integrar roles y permisos. |
| 10 | Implementar 2FA y OAuth. |

## 13. Archivos del backend que conviene revisar

Para comprobar que la implementación cumple las historias HU-SE-08 y HU-SE-10, conviene revisar:

1. `Session.java`: entidad y relación con USER.
2. `SessionRepository.java`: consultas y persistencia.
3. `JwtService.java`: generación, firma y validación JWT.
4. `JwtConfig.java`: configuración de `JWT_SECRET`.
5. `AuthService.java`: login, autenticación y creación de sesiones.
6. `SecurityConfig.java`: filtros, cookies, CORS y CSRF.
7. `AuthController.java`: endpoints de login y logout.

---

## Conclusión

La idea central es que **SESSION sea la fuente de control de las sesiones activas**, mientras que JWT permita transportar la identidad y los datos necesarios para reconocer la sesión en cada petición.

`JWT_SECRET` sirve para firmar y verificar los JWT. La cookie HttpOnly permite que el navegador transporte el token sin exponerlo directamente al JavaScript de Angular.

Finalmente, Spring Boot debe comprobar tres cosas antes de permitir una operación protegida:

1. Que el JWT sea válido.
2. Que la sesión siga activa y no haya expirado.
3. Que el usuario tenga los permisos necesarios.

La configuración de JWT_SECRET debe ser estable por entorno, mantenerse fuera de Git y no regenerarse en cada arranque.
