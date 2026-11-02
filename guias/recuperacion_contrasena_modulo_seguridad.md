# Recuperación de contraseña – Módulo de Seguridad

## 1. Objetivo

Permitir que un usuario registrado mediante **correo electrónico y contraseña** recupere el acceso a su cuenta cuando olvide su contraseña, estableciendo una nueva y pudiendo iniciar sesión posteriormente.

Este documento se basa en la historia de usuario **HU-SE-09 – Recuperar contraseña** de *Historias de Usuario – Módulo de Seguridad*.

## 2. Requisitos definidos por el módulo

La recuperación debe cumplir lo siguiente:

- Está disponible únicamente para usuarios que se autentican mediante correo electrónico y contraseña.
- El usuario solicita la recuperación indicando el correo asociado a su cuenta.
- El sistema genera un mecanismo temporal de recuperación.
- El mecanismo tiene un periodo limitado de validez.
- Si la solicitud es válida, el usuario puede establecer una nueva contraseña.
- La nueva contraseña debe cumplir las reglas de seguridad establecidas por el sistema.
- Una solicitud utilizada correctamente no puede volver a utilizarse.
- Una solicitud expirada debe ser rechazada.
- Después del cambio, el usuario debe poder iniciar sesión con la nueva contraseña.
- Cuando corresponda, el sistema debe invalidar las sesiones anteriores del usuario.
- Los usuarios registrados únicamente mediante Google, GitHub, Microsoft u otro proveedor OAuth no recuperan su contraseña desde esta plataforma. Deben realizar ese proceso directamente con su proveedor.

## 3. Flujo funcional

### Paso 1. Solicitar recuperación

1. En la pantalla de inicio de sesión, el usuario selecciona **¿Olvidaste tu contraseña?**
2. El frontend muestra un formulario para ingresar el correo asociado a la cuenta.
3. El usuario envía el formulario.
4. Angular envía el correo al endpoint acordado con el backend.

El frontend valida que el campo no esté vacío y que tenga formato de correo válido.

### Paso 2. Procesar la solicitud

1. El backend recibe el correo.
2. Comprueba si la cuenta utiliza autenticación mediante correo y contraseña.
3. Si corresponde, genera un mecanismo temporal de recuperación.
4. El mecanismo debe tener un límite de expiración.
5. El sistema proporciona al usuario la forma de continuar con la recuperación.

**El documento no especifica** si el mecanismo temporal será un enlace enviado por correo, un código u otra alternativa. El equipo debe acordar esa decisión.

Como recomendación de interfaz, conviene mostrar una respuesta neutral que no revele innecesariamente si una dirección está registrada. El texto y el comportamiento exactos deben coordinarse con el backend.

### Paso 3. Validar el mecanismo

Cuando el usuario presenta el mecanismo de recuperación, el sistema comprueba que:

- Corresponde a una solicitud existente.
- No ha expirado.
- No ha sido utilizado anteriormente.

Si no es válido, expiró o ya fue utilizado, el sistema rechaza la operación y el frontend informa que debe solicitar una nueva recuperación.

Si es válido, el frontend permite establecer una nueva contraseña.

### Paso 4. Establecer la nueva contraseña

1. El usuario introduce la nueva contraseña y la confirma.
2. Angular comprueba que ambos valores coincidan y aplica las validaciones de interfaz acordadas.
3. Angular envía la nueva contraseña junto con el mecanismo temporal al backend.
4. El backend vuelve a validar el mecanismo y las reglas de seguridad.
5. Si todo es correcto, actualiza la contraseña.
6. La solicitud queda consumida y no puede volver a utilizarse.
7. Cuando corresponda, el backend invalida las sesiones anteriores del usuario.

La contraseña debe almacenarse de manera segura y nunca en texto plano.

### Paso 5. Confirmar e iniciar sesión

1. El frontend informa que la contraseña fue modificada correctamente.
2. El usuario regresa al inicio de sesión.
3. Puede autenticarse con su correo y la nueva contraseña.
4. Si tiene habilitada la autenticación de dos factores, continúa con el flujo 2FA definido por el módulo.

La recuperación de contraseña no debe conceder por sí sola acceso a funcionalidades protegidas ni considerarse una sesión autenticada.

## 4. Pantallas del frontend

### A. Solicitar recuperación

Debe incluir:

- Título: **Recuperar contraseña**.
- Texto breve que indique que debe ingresar el correo asociado a su cuenta.
- Campo de correo electrónico.
- Botón para enviar la solicitud.
- Enlace para volver al inicio de sesión.
- Indicador de carga.
- Mensajes de resultado o error.

### B. Establecer nueva contraseña

Debe incluir:

- Título: **Crear nueva contraseña**.
- Campo de nueva contraseña.
- Campo para confirmar la contraseña.
- Indicadores de validación según las reglas acordadas con el backend.
- Botón para guardar la nueva contraseña.
- Indicador de carga.
- Mensajes para solicitud inválida, expirada o ya utilizada.

### C. Resultado

La interfaz debe contemplar:

- Confirmación de cambio exitoso.
- Solicitud inválida o expirada.
- Error al procesar la solicitud.
- Opción para regresar al inicio de sesión o solicitar una nueva recuperación, según corresponda.

## 5. Responsabilidades del frontend y backend

| Frontend Angular | Backend Spring Boot |
|---|---|
| Mostrar el formulario de recuperación. | Recibir y procesar la solicitud. |
| Validar el formato del correo. | Determinar si la cuenta usa autenticación tradicional. |
| Enviar la solicitud al endpoint acordado. | Generar el mecanismo temporal. |
| Mostrar el formulario de nueva contraseña. | Validar existencia, vigencia y uso previo del mecanismo. |
| Comprobar que las contraseñas coincidan. | Validar las reglas de seguridad de la contraseña. |
| Enviar el mecanismo y la nueva contraseña. | Actualizar la contraseña de forma segura. |
| Mostrar el resultado. | Marcar la solicitud como utilizada. |
| Permitir volver al inicio de sesión. | Invalidar sesiones anteriores cuando corresponda. |

Las validaciones del frontend no sustituyen las del backend. El backend debe volver a comprobar el mecanismo y las reglas de contraseña.

## 6. Usuarios OAuth

La recuperación interna no aplica a usuarios registrados únicamente mediante Google, GitHub, Microsoft u otro proveedor OAuth.

Por lo tanto:

- El frontend no debe ofrecerles un flujo para cambiar una contraseña propia de la plataforma si no tienen autenticación tradicional.
- Si necesitan recuperar sus credenciales, deben hacerlo directamente con el proveedor correspondiente.
- El backend registra el método de autenticación en `users.auth_provider`. Las cuentas OAuth nuevas se identifican con su proveedor y solo las cuentas `LOCAL_PASSWORD` pueden solicitar recuperación. Las cuentas antiguas se migran como `UNKNOWN` y deben clasificarse explícitamente; no se infiere el proveedor a partir del correo ni del hash de contraseña.

Las cuentas OAuth deben continuar la recuperación con el proveedor de identidad correspondiente.

## 7. Contrato acordado

### Solicitar recuperación

`POST /api/auth/password-recovery/request`

```json
{"email":"usuario@ejemplo.com"}
```

Responde `202 Accepted` con el mismo cuerpo para todo correo válido:

```json
{"message":"Si existe una cuenta compatible, recibirás instrucciones para recuperar la contraseña."}
```

Solo se crea y envía el token a cuentas `LOCAL_PASSWORD`. Correos desconocidos, OAuth o con método aún desconocido reciben la respuesta neutral sin correo.

### Restablecer contraseña

`POST /api/auth/password-recovery/reset`

```json
{"token":"...","newPassword":"ClaveSegura123!"}
```

Un cambio exitoso responde `204 No Content`. Un token inexistente, expirado o utilizado responde `400 Bad Request` con un mensaje genérico. La contraseña debe tener entre 8 y 72 caracteres, incluir mayúscula, minúscula, número y uno de `@$!%*?&`, sin espacios.

### Llamada interna al servicio de correo

Spring llama a FastAPI mediante `POST /internal/email/password-recovery`, con el header obligatorio `X-Service-Token: <secreto compartido>` y el cuerpo:

```json
{
  "to":"usuario@ejemplo.com",
  "recoveryUrl":"http://localhost:4200/authentication/reset-password?token=..."
}
```

FastAPI debe validar el secreto con comparación de tiempo constante antes de enviar el correo. Este repositorio solo implementa la llamada saliente; la ruta, validación y envío en FastAPI quedan pendientes porque su código está en otro microservicio. El envío usa Gmail API y FastAPI no almacena usuarios ni tokens.

No se deben asumir rutas como `/forgot-password` o `/reset-password`, ni estructuras JSON, hasta que el equipo las confirme.

## 8. Criterios de aceptación para las pruebas

- [ ] Un usuario tradicional puede solicitar recuperar su contraseña indicando su correo.
- [ ] El sistema genera un mecanismo temporal.
- [ ] El mecanismo tiene un periodo limitado de validez.
- [ ] Una solicitud válida permite establecer una nueva contraseña.
- [ ] Una solicitud expirada es rechazada.
- [ ] Una solicitud ya utilizada no puede reutilizarse.
- [ ] La nueva contraseña cumple las reglas establecidas.
- [ ] Después del cambio, el usuario puede iniciar sesión con la nueva contraseña.
- [ ] Las sesiones anteriores se invalidan cuando corresponda.
- [ ] Un usuario exclusivamente OAuth no puede recuperar una contraseña desde la plataforma.
- [ ] Los errores se muestran de forma comprensible.
- [ ] La recuperación no concede acceso a recursos protegidos.

## 9. Decisiones de implementación

- El token se genera con un CSPRNG, se almacena solo como SHA-256 y vence a los 30 minutos.
- Cada solicitud local nueva invalida las anteriores; el token solo puede utilizarse una vez.
- Si falla el envío, Spring elimina el token recién creado y mantiene la respuesta pública neutral.
- Al restablecer la contraseña se reutiliza el `PasswordEncoder` BCrypt existente y se eliminan las sesiones activas mediante el mecanismo de sesiones existente.
- La migración SQL está en `src/main/resources/db/manual/V1__password_recovery_tokens.sql` y debe ejecutarse una sola vez en `security_ms_db_martes`, antes de desplegar la aplicación y antes de clasificar cuentas.
- La migración deja todas las filas presentes en ese momento con `auth_provider='UNKNOWN'`. Una persona operadora debe clasificar los IDs verificados como cuentas locales (`LOCAL_PASSWORD`) o proveedor (`GOOGLE`, `MICROSOFT`, `GITHUB`) antes de habilitarles recuperación. No volver a ejecutar la migración después de clasificarlas.
- Ejemplo de clasificación manual, reemplazando los IDs por los verificados en la base:

```sql
UPDATE users SET auth_provider = 'LOCAL_PASSWORD' WHERE id IN (...);
UPDATE users SET auth_provider = 'GOOGLE' WHERE id IN (...);
```
- Variables de entorno Spring: `NOTIFICATION_BASE_URL`, `NOTIFICATION_SERVICE_TOKEN` y `PASSWORD_RECOVERY_RESET_URL`. No guardar el secreto en el repositorio.
- FastAPI debe configurar el mismo secreto compartido y aplicarlo únicamente a la ruta interna de recuperación. El 2FA existente queda fuera de este flujo.

## Resumen del flujo

**Inicio de sesión → ¿Olvidaste tu contraseña? → Ingresar correo → Solicitar recuperación → Recibir mecanismo temporal → Validar mecanismo → Establecer nueva contraseña → Confirmación → Iniciar sesión con la nueva contraseña.**

La recuperación solo aplica a cuentas de autenticación tradicional. Las cuentas exclusivamente OAuth deben gestionar sus credenciales con su proveedor.

## Fuente

*Historias de Usuario – Módulo de Seguridad*, HU-SE-09 – Recuperar contraseña. Los pasos de interfaz organizan la implementación; los detalles no especificados en los criterios de aceptación quedan identificados como decisiones pendientes del equipo.
