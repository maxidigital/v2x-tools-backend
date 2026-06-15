# v2x-tools-backend

Spring Boot 3.2 / Java 17. Backend REST para encoding/decoding de mensajes V2X (UPER).
Deployado en Railway — auto-deploya en cada push a `main`.

> **Arquitectura (3 servicios)**: el backend **ya no hace conversión V2X**. La conversión
> vive en `v2x-tools-engine` (servicio aparte, dueño de todo el "wind" de codecs). El backend
> le habla **solo por HTTP** vía `EngineClient` (`main/engine/`), reacciona al `EngineResult`
> tipado y, ante `notFound`, usa `MessageLoader` (`main/loader/`) para traer la definición del
> repo y cargarla en el engine. Las definiciones ASN.1 las digiere `v2x-tools-repo`.
> Por eso el backend ya **no** depende de los jars wind de parseo/codecs — solo le quedan
> `wind.lib` + `wind_commons` para utils de logging (`A`, `StatsHandler`).

> **Reactor único (convert + generate)**: ambos endpoints pasan por `V2XConversionService` y
> siguen el mismo flujo — piden al engine, leen el `EngineResult` tipado (`ok` / `notFound` /
> `decodeError`) y, ante `notFound`, resuelven la definición del repo (`ensureLoaded`) y reintentan.
> El **lazy-load del repo está reservado al usuario público `0`**; otros usuarios gestionan su estado
> a mano (`/messages/load`) y reciben `notFound` sin auto-carga. El `userId` viene del header
> `X-User-Id` (default `0`). Generate (`RandomController`) ya **no** es un pasamanos: usa el reactor.

## Dependencias

### Maven Central (resuelven automáticamente)
`spring-boot-starter-web`, `telegrambots`, `angus-mail`, `mailjet-client`,
`springdoc-openapi-starter-webmvc-ui`

### JARs internos (`libs/` — commiteados al repo)
Los artefactos DLR y de underwater no están en Maven Central ni en ningún registry
público. Se commitean directamente en `libs/` con `scope=system` en el `pom.xml`.
**Esto es intencional** — evita gestión de credenciales en Railway y cualquier otro
entorno de build.

| Archivo | Proyecto fuente | Uso |
|---|---|---|
| `wind.lib-4.2.jar` | `v2x-framework/commons/wind.lib` | utils de logging (`A`) |
| `wind_commons-2.0.jar` | `v2x-tools-wind/wind_commons` | `StatsHandler` |
| `utils.xmladmin2-1.3.2.jar` | `v2x-framework/tools/utils.xmladmin2` | transitiva |
| `email.admin-1.0.1.jar` | `underwater/admin/email.admin` | email de soporte |

### Cómo actualizar un JAR

```bash
# 1. Rebuild el proyecto fuente (ejemplo: wind.lib)
cd v2x-framework/commons/wind.lib && mvn package
cp target/wind.lib-4.2.jar v2x-tools-backend/libs/

# 2. Commit y push → Railway redeploya automáticamente
git add libs/wind.lib-4.2.jar
git commit -m "chore: update wind.lib to vX.Y"
git push
```

## Build

```bash
# Build local (no requiere credenciales)
mvn clean package -DskipTests

# Con tests
mvn clean package
```

No se necesita `-s settings.xml` para buildear localmente.
`settings.xml` existe solo si en algún momento se necesita resolver
algo de GitHub Packages (no es el caso actualmente).

## Deploy

Railway auto-deploya en cada push a `main`. No hay pasos manuales.

## Estructura

```
src/main/java/main/
├── config/         Spring config (CORS, lifecycle)
├── controllers/    REST endpoints (V2x, Stats, Contact, etc.)
├── services/       Lógica de negocio (V2XConversionService — reactor sin wind)
├── engine/         EngineClient + EngineResult — HTTP client al v2x-tools-engine
├── loader/         MessageLoader — trae definiciones del repo y las carga en el engine
├── repo/           RepoClient — HTTP client para v2x-tools-repo
├── monitoring/     Notificaciones Telegram
├── stats/          CSV de estadísticas de uso
└── utils/          Utilidades
```

## Endpoints principales

- `POST /api/v2x/{from}/{to}` — convierte un mensaje entre UPER/WER/XML/JSON (delega en el engine).
  Auto-detecta el messageId del payload; lazy-load del repo solo para `userId=0`.
- `GET  /api/v2x/generate` — genera un payload de muestra (`?mid=&format=&minimal=`). Mismo reactor
  que convert: lazy-load del repo solo para `userId=0`.
- `POST /api/v2x/messages/load` — carga explícita por alias (cualquier usuario; no es el lazy-load).
- `GET/DELETE /api/v2x/messages` — lista / limpia los mensajes cargados del usuario en el engine.
- `GET  /api/capabilities` — descripción de capacidades (para MCP)
- `POST /api/support/report` — reporte de soporte

Todos llevan el header `X-User-Id` (default `0`). El parseo ASN.1 (`POST /api/asn1/parse`) se movió
a **`v2x-tools-repo`** (es la autoridad ASN.1).

## Configuración

- `wind.engine.url` (`application.properties`) — URL del engine. Local: `http://localhost:8090`.
  En Railway: `WIND_ENGINE_URL=http://v2x-tools-engine.railway.internal:8090`.

## Notas

- El backend **no importa wind de codecs/parser**. La conversión la hace `v2x-tools-engine`
  (HTTP). `V2XConversionService` solo reacciona al `EngineResult` tipado (`ok` / `notFound`
  / `decodeError`) — nunca toca tipos wind ni hace pre-checks. Convert y generate comparten ese reactor.
- **Lazy-load solo para `userId=0`**: ante `notFound`, el backend va al repo y reintenta únicamente
  para el usuario público (0). Para otros usuarios devuelve `notFound` (404) sin tocar el repo — ellos
  cargan con `/api/v2x/messages/load`.
- `includeSystemScope=true` en el spring-boot-maven-plugin asegura que los system-scope JARs
  (`libs/`) se incluyan en el fat jar final.
- `RepoClient` en `main/repo/` es un cliente HTTP del repo. (La antigua implementación de la
  interfaz `Asn1Repo` de `wind_parser` ya no vive acá — el repo es la autoridad ASN.1.)
