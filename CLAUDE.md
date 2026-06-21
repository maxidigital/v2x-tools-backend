# v2x-tools-backend (LEGACY — sin DB)

Spring Boot 3.2 / Java 17. **Servicio legacy, NO es parte del pipeline V2X.** Tras el detach del hub
(jun 2026) quedó solo con el **cruft no-V2X** del backend viejo: formulario de contacto, stats de acceso y
monitoring/Telegram. **No usa base de datos.** Deployado en Railway (auto-deploya al push a `main`).

> ⚠️ **El hub V2X se mudó a `v2x-tools-hub`** (servicio/repo propio). Toda la orquestación repo+engine,
> aliases, saved messages, convert/generate y la DB (`module_alias`/`saved_message`) ahora viven ahí —
> **no toques nada de eso acá** (ya no existe). Ver `v2x-tools-hub/CLAUDE.md` y `docs/SYSTEM.md`.

## Qué sirve (lo único vivo)

| Endpoint | Qué hace |
|---|---|
| `POST /api/contact` | Formulario de contacto del sitio → manda email (email.admin / mailjet / angus-mail). |
| `POST /api/access-stats` | Registra stats de acceso (CSV en disco, `StatsHandler`). |
| `POST /api/monitoring/stats`, `POST /api/monitoring/test` | Notificaciones de monitoreo vía Telegram. |

Sigue vivo porque el **frontend usa el formulario de contacto**. El Caddy del frontend rutea
`/api/contact*`, `/api/access-stats*`, `/api/monitoring*` a este servicio (por red interna); el resto de
`/api/*` va al hub.

## Estructura (`src/main/java/main/`)

```
V2xToolsApplication.java   @SpringBootApplication
controllers/   ContactController, StatsController, MonitoringController
monitoring/    Telegram (TelegramCenter, NotificationFilter/Type)
stats/         StatsHandler (CSV de uso)
config/        CorsConfig, OpenApiConfig, ConfigurationManager, AppLifecycle
forwarder/, handlers/, gets/, A.java, ContentTypes.java — utils
```
> Paquetes vacíos sobrantes (`loader/`, `services/`) — leftovers locales, git no los trackea.
> Lo que se fue al hub: `hub/`, `engine/`, `repo/` (borrados) + deps `spring-data-jpa`/`postgresql`/`jaxb-api`.

## Dependencias

**Maven Central:** `spring-boot-starter-web`, `springdoc-openapi-starter-webmvc-ui`, `telegrambots`,
`angus-mail`, `mailjet-client`. (Sin JPA/Postgres — el servicio no tiene DB.)

**JARs internos** (`libs/`, scope=system, commiteados): `wind.lib-4.2.jar` (logging `A`),
`wind_commons-2.0.jar` (`StatsHandler`), `utils.xmladmin2-1.3.2.jar` (transitiva),
`email.admin-1.0.1.jar` (email de contacto). `includeSystemScope=true` los mete al fat jar.

## Build / Deploy

```bash
mvn clean package -DskipTests   # local, sin credenciales
```
Railway auto-deploya al push a `main`. **No requiere Postgres** (no adjuntar/linkear ninguno — un servicio
Spring con JPA intentaría conectar al boot, pero ya le sacamos JPA, así que arranca sin DB).

## Configuración (env Railway)
- `NOTIFICATION_SERVICE_URL` — hub de notificaciones. `monitoring.*` — rate limit Telegram.
- Credenciales de email (mailjet/SMTP) según el setup de `email.admin`.
- `PORT` — puerto (Tomcat bindea 8080).

## Follow-up
Se le puede seguir adelgazando (o incluso jubilar moviendo el contact form a `apps/contact.form` y
stats/monitoring a `notification.service`), pero por ahora queda vivo sirviendo esos 3 endpoints.
