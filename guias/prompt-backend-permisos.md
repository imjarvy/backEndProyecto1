# Prompt: agregar los permisos de las rutas de jugadores, habilidades y anotaciones al microservicio de seguridad

## 1. Contexto y objetivo

Estás dentro del espacio de trabajo del backend. Hay dos proyectos relacionados:

- **Backend_Negocio** (NestJS, TypeORM, MySQL): contiene los módulos `teams`, `sports`, `matches` y `players`. Las rutas cuelgan de `/api`.
- **ms_security** (Java, Spring): microservicio de seguridad. Cada petición de Backend_Negocio se autoriza ahí, por método, URL y entidad. Los permisos se siembran en `DatabaseSeeder`.

Las entidades nuevas (jugadores, habilidades, las asignaciones entre jugadores, habilidades y equipos, y las anotaciones de partido) ya están implementadas y ya están en inglés. **No renombres nada.**

El problema: `DatabaseSeeder` no tiene ninguna ruta de esas entidades. Mientras no se agreguen, esas rutas responden 403 aunque el usuario sea ADMIN. El objetivo es agregar sus permisos al seeder, con las rutas exactas que existen hoy en el código.

## 2. Reglas para gastar pocos tokens

- No leas todo el proyecto. Lista directorios (solo nombres) y lee únicamente lo necesario.
- Los únicos archivos que modificas son `DatabaseSeeder` (en ms_security) y, solo si es indispensable y con aprobación, lo que la verificación revele.
- Un archivo leído una vez no se vuelve a leer. No pegues código completo en la conversación: reporta con listas cortas.
- Sin comentarios decorativos, sin bloques de comentarios innecesarios y sin emojis. Respeta el formato y el estilo del seeder.

## 3. Fase 0: analiza antes de cambiar nada

1. **Rutas reales de las entidades nuevas.** En Backend_Negocio, lee los controllers del módulo `players` y el controller de las anotaciones de partido (en `matches`). Lista cada ruta con: método HTTP, ruta completa (prefijo global + prefijo del controller + ruta del método) y la entidad sobre la que actúa.
2. **Cómo se aplica la seguridad.** Lee `SecurityGuard` y `main.ts` de Backend_Negocio (prefijo global, qué método y URL envía al microservicio). Luego, en ms_security, lee la lógica que valida un permiso: cómo compara método y URL, cómo trata los parámetros de ruta y los parámetros de consulta. Confirma que cada parámetro de ruta se escribe `{id}` en el permiso, como en las entradas existentes (por ejemplo `/api/user-roles/{id}/{id}`).
3. **`DatabaseSeeder`.** Lee `ADMIN_PERMISSIONS` y el flujo de `seedDatabase`. Identifica cómo se elige el campo `entity` en las entradas de rutas anidadas (por ejemplo `/api/matches/{id}/result` usa `Match` y `/api/users/{id}/profile` usa `Profile`) y qué rutas de las nuevas entidades ya estuvieran sembradas, para no duplicarlas.
4. **Reporte corto.** Entrega una tabla con cada ruta encontrada, la línea de `PermissionSeed` que propones y la entidad elegida con una frase de justificación. Incluye las diferencias frente a la tabla de referencia de la sección 4. Pregunta solo lo que no puedas deducir del código; en particular, si el rol CONSULTOR debe recibir los permisos de lectura (`GET`) de estas entidades (por defecto no). Espera la confirmación del usuario antes de editar el seeder.

## 4. Tabla de referencia (solo para contrastar)

Estas son las rutas que el diseño esperaba. **La fuente de verdad son los controllers**: si algo no coincide, usa lo que encuentres en el código y repórtalo como diferencia.

| Entidad | Método y ruta |
| --- | --- |
| Player | `POST /api/players`, `GET /api/players`, `GET /api/players/{id}`, `GET /api/players/{id}/profile`, `PATCH /api/players/{id}`, `DELETE /api/players/{id}` |
| Skill | `POST /api/skills`, `GET /api/skills`, `GET /api/skills/{id}`, `PATCH /api/skills/{id}`, `DELETE /api/skills/{id}` |
| PlayerSkill | `POST /api/players/{id}/skills`, `GET /api/players/{id}/skills`, `PATCH /api/players/{id}/skills/{id}`, `DELETE /api/players/{id}/skills/{id}`, `GET /api/skills/{id}/players` |
| TeamPlayer | `POST /api/teams/{id}/players`, `PATCH /api/teams/{id}/players/{id}/withdrawal`, `GET /api/teams/{id}/players`, `GET /api/players/{id}/teams` |
| ScoringEvent | `POST /api/matches/{id}/scoring-events`, `GET /api/matches/{id}/scoring-events`, `PATCH /api/scoring-events/{id}`, `DELETE /api/scoring-events/{id}` |

Si los nombres reales de clases o recursos difieren de esta tabla (por ejemplo otro nombre para la anotación), usa los reales tanto en la URL como en el campo `entity`.

## 5. Cómo escribir las entradas

Reglas de formato de cada `PermissionSeed(método, url, entidad)`:

- Método en mayúsculas, como lo envía el guard (`GET`, `POST`, `PATCH`, `DELETE`).
- URL con el prefijo `/api`, recursos exactamente como están en el controller y cada parámetro de ruta escrito `{id}`, aunque en NestJS tenga otro nombre.
- Los parámetros de consulta (`?...`) no forman parte de la URL del permiso, salvo que la lógica de validación de ms_security demuestre lo contrario.
- `entity` en PascalCase singular, igual que el nombre de la clase de la entidad (`Team`, `TeamSport`, `Match`).

Ejemplos del formato esperado, con la entidad tomada de la tabla de referencia:

```java
new PermissionSeed("POST", "/api/players", "Player"),
new PermissionSeed("GET", "/api/players/{id}/profile", "Player"),
new PermissionSeed(
        "PATCH",
        "/api/teams/{id}/players/{id}/withdrawal",
        "TeamPlayer"
),
```

Colocación y estilo:

- Agrégalas al arreglo `ADMIN_PERMISSIONS`, agrupadas por entidad, después de las de `Match` y antes del cierre del arreglo.
- Parte las líneas largas con el mismo estilo de las existentes (la de `/api/tournaments/{id}/development` es el modelo).
- No cambies la lógica del seeder. Es idempotente: usa `findOrCreatePermission` y `assignPermission`, así que basta reiniciar `ms_security` para que las entradas nuevas se creen y se asignen al rol ADMIN.
- No agregues permisos al rol CONSULTOR salvo que el usuario lo confirme.
- Si encuentras filas de permisos antiguas en la base con rutas distintas a las reales, repórtalas; no las borres.

## 6. Verificación

- `ms_security` compila y arranca sin errores.
- Después de reiniciar, existe una fila de permiso por cada ruta nueva y está asignada al rol ADMIN.
- Con un usuario ADMIN, cada ruta nueva responde distinto de 403: llega al controller y devuelve 200, 201, 204, 400, 404 o 409.
- Con un usuario sin permisos, esas mismas rutas responden 403.
- No hay entradas duplicadas en `ADMIN_PERMISSIONS` ni se modificó ninguna de las existentes.

## 7. Entrega

Al terminar, reporta:

- Cantidad de entradas agregadas y el archivo modificado.
- Resultado de la verificación.
- La tabla final de rutas reales (método, ruta y entidad). Esa tabla se usará para actualizar el prompt del front end con las rutas exactas.
- Cualquier diferencia con este documento que haya quedado pendiente.
