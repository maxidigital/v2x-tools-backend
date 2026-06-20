# v2x-tools-backend (el **hub**)

Spring Boot 3.2 / Java 17. **Hub público** de la suite V2X: la fachada REST que usan el frontend y los
usuarios de la API. Deployado en Railway — auto-deploya en cada push a `main`.

> **Arquitectura (3 servicios HTTP).** El hub **no hace conversión ni parseo ASN.1**. Orquesta:
> - **`v2x-tools-repo`** — autoridad ASN.1. Digiere el ASN.1 crudo → **árbol digerido** (`GET /api/modules/tree?moduleId=&type=`).
> - **`v2x-tools-engine`** — dueño de los codecs "wind". Codec universal **content-addressed**: `POST /engine/load`
>   (árbol+metadata → `engineId`), y `convert`/`generate` toman el `engineId` por header. No sabe de V2X, users ni repos.
>
> Flujo: el hub resuelve un **ref** → `(moduleId, type, fixups)` → trae el árbol del repo → `engine.load` → `engineId`
> (cacheado `ref→engineId`; ante `engineNotFound`, recarga y reintenta). El hub renderiza el binario (hex `uper:…`).
> El hub **solo** depende de `wind.lib` + `wind_commons` para utils de logging — **nada** de wind de codecs/parser.

## El modelo (universal, sin V2X en el path)

- **alias** — nombre legible de un módulo (su OID). Tabla `module_alias`. `user_id 0` = público/default.
- **saved message** — ref con nombre cuya definición vive en un blob `data` JSON
  (`{moduleAlias, rootType, fixups[], description}`). Tabla `saved_message`. Los **fixups** son **solo de
  generación** (fijan p.ej. el header); convert queda fiel.
- **X-Ref** (cómo se identifica qué definición usar):
  - **messageRef** (sin `:`, p.ej. `cam_v2`) — un saved message, con sus fixups sticky.
  - **typeRef** (`alias:Type`, p.ej. `cam_v2:CAM`) — un tipo ASN.1 crudo del módulo, sin fixups.
  - **auto-detect** — en `convert`, si **falta** `X-Ref` (o es `auto`), el hub lee el header ETSI ITS
    `(protocolVersion, messageId)` del payload y elige el saved message que matchea. El índice se arma de
    los fixups de header de los propios mensajes (sin data extra). UPER/WER = primeros 2 octetos; JSON/XML = se escanea el header.

## Endpoints (`HubController`, `/api`)

- `POST /api/convert` — convierte un payload. Headers `X-From`, `X-To` (UPER/WER/XML/JSON), `X-Ref`
  **opcional** (omitir = auto-detect). `Content-Type: application/octet-stream` para binario crudo de entrada;
  `Accept: application/octet-stream` para recibir UPER/WER como bytes.
- `POST /api/generate` — genera una muestra. `X-Ref` requerido; body `{format, size, minimal}`.
- `GET/POST/DELETE /api/messages` — saved messages (body de create: `{name, moduleAlias, rootType, fixups, description}`).
- `GET/POST/DELETE /api/aliases` — aliases de módulo (headers `X-Alias`, `X-Module-Oid`).
- `GET /api/modules` — módulos del repo + sus aliases. `GET /api/modules/types` (header `X-Module-Oid`).

Todos llevan `X-User-Id` (default `0`). Errores: `200` ok, `400` `{error}` (no se pudo decodificar/codificar),
`404` ref desconocido / auto-detect sin match.

> El acceso público es vía **`v2x.tools/api/*`** — el frontend (Caddy) hace reverse-proxy de `/api/*` a este
> backend (same-origin, sin CORS). La URL de Railway no se expone.

## Estructura (`src/main/java/main/`)

```
hub/            EL HUB. HubController + servicios:
                  AliasService, SavedMessageService, ResolutionService (el reactor ref→engineId),
                  MessageIdentifier (auto-detect), SchemaFixup (limpieza de schema al boot).
  entity/         ModuleAlias, SavedMessage (JPA).
  repo/           ModuleAliasRepository, SavedMessageRepository (Spring Data).
engine/         EngineClient + EngineResult — HTTP client tipado al engine (engineId API).
repo/           RepoClient + DefinitionNotFoundException — HTTP client al repo (/tree, /modules, /types).
controllers/    Cruft NO-V2X (legacy del backend viejo): ContactController (/api/contact),
                StatsController (/api/access-stats), MonitoringController (/api/monitoring).
monitoring/     Telegram (TelegramCenter, NotificationFilter/Type).
stats/          CSV de estadísticas de uso (StatsHandler).
config/         CORS, OpenApi, lifecycle, ConfigurationManager.
forwarder/, handlers/, A.java, ContentTypes.java — utils.
```

> Hay paquetes **vacíos** sobrantes del refactor (`services/`, `loader/`, `gets/`, `telegram/`) — git no los
> trackea, son leftovers locales. El viejo `V2XConversionService`/`MessageLoader`/`/api/v2x/*` ya **no existe**.
> El cruft de `controllers/` + `monitoring/` + `stats/` es lo que motiva el **detach** del hub a su propio
> servicio (opción A, pendiente).

## DB (Postgres)

El hub persiste `module_alias` + `saved_message`. En Railway: adjuntar un plugin Postgres (provee `PG*`).
`ddl-auto=update` **no dropea columnas** → `hub/SchemaFixup.java` (ApplicationRunner + JdbcTemplate) dropea
columnas huérfanas al boot (`IF EXISTS`, idempotente).

## Dependencias

### Maven Central
`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `jaxb-api`, `telegrambots`,
`angus-mail`, `mailjet-client`, `springdoc-openapi-starter-webmvc-ui`.

> ⚠️ **`javax.xml.bind:jaxb-api`** es necesario: Hibernate 6.3 (JSON format-mapper) tropieza con
> `jackson-module-jaxb-annotations` (javax.xml.bind) al arrancar → `NoClassDefFoundError` → 502. El build
> local con `-DskipTests` no lo detecta (es runtime).

### JARs internos (`libs/`, scope=system, commiteados)
| Archivo | Fuente | Uso |
|---|---|---|
| `wind.lib-4.2.jar` | `v2x-framework/commons/wind.lib` | utils de logging (`A`) |
| `wind_commons-2.0.jar` | `v2x-tools-wind/wind_commons` | `StatsHandler` |
| `utils.xmladmin2-1.3.2.jar` | `v2x-framework/tools/utils.xmladmin2` | transitiva |
| `email.admin-1.0.1.jar` | `underwater/admin/email.admin` | email de soporte |

`includeSystemScope=true` en el spring-boot-maven-plugin mete los system-scope al fat jar.

## Build / Deploy

```bash
mvn clean package -DskipTests   # local, sin credenciales
```
Railway auto-deploya en cada push a `main`. **El build de Railway compila los tests** (no los corre) — un
test que no compila rompe el deploy.

> ⚠️ **Cambios de contrato engine↔hub**: deployar **ambos juntos** (ventana de contrato mixto mientras buildea).

## Configuración (`application.properties` / env Railway)

- `WIND_REPO_URL` — URL del repo. `RepoClient`/`EngineClient` prependen `http`/`https` si la URL no trae scheme.
- `WIND_ENGINE_URL` — URL del engine. En Railway, red privada IPv6-only: usar `http://<svc>.railway.internal:<port>`.
- `PG*` — Postgres del hub. `NOTIFICATION_SERVICE_URL` — hub de notificaciones. `monitoring.*` — rate limit Telegram.

## Notas

- El hub **no toca tipos wind ni hace pre-checks** — reacciona al `EngineResult` tipado (`ok`/`engineNotFound`/`decodeError`).
- **EngineClient siempre habla JSON con el engine** (`Accept: application/json`); el binario lo renderiza el hub.
  (Histórico: mandar `Accept: text/plain` daba 406 → "No content to map".)
- `auth` y `detach` del hub a su propio servicio están pendientes (el `userId` ya separa scope; hoy todo es `0`).
