# v2x-tools-backend

Spring Boot 3.2 / Java 17. Backend REST para encoding/decoding de mensajes V2X (UPER).
Deployado en Railway — auto-deploya en cada push a `main`.

## Dependencias

### Maven Central (resuelven automáticamente)
`spring-boot-starter-web`, `telegrambots`, `angus-mail`, `mailjet-client`

### JARs internos (`libs/` — commiteados al repo)
Los artefactos DLR y de underwater no están en Maven Central ni en ningún registry
público. Se commitean directamente en `libs/` con `scope=system` en el `pom.xml`.
**Esto es intencional** — evita gestión de credenciales en Railway y cualquier otro
entorno de build.

| Archivo | Proyecto fuente |
|---|---|
| `wind.connector.jar` | `v2x-framework/tools/wind.connector` |
| `wind.lib-4.2.jar` | `v2x-framework/commons/wind.lib` |
| `wind_asn1_parser-2.0.jar` | `v2x-tools-wind/wind_asn1_parser` |
| `wind_parser-2.0.jar` | `v2x-tools-wind/wind_parser` |
| `wind_generic-2.0.jar` | `v2x-tools-wind/wind_generic` |
| `wind_generator-2.1.jar` | `v2x-tools-wind/wind_generator` |
| `wind_commons-2.0.jar` | `v2x-tools-wind/wind_commons` |
| `utils.xmladmin2-1.3.2.jar` | `v2x-framework/tools/utils.xmladmin2` |
| `email.admin-1.0.1.jar` | `underwater/admin/email.admin` |

### Cómo actualizar un JAR

```bash
# 1. Rebuild el proyecto fuente (ejemplo: wind.lib)
cd v2x-framework/commons/wind.lib && mvn package

# 2. Si es wind.connector (fat jar que embebe wind.lib):
cd v2x-framework/tools/wind.connector && mvn package
cp target/wind.connector.jar v2x-tools-backend/libs/

# 3. Commit y push → Railway redeploya automáticamente
git add libs/wind.connector.jar
git commit -m "chore: update wind.connector to vX.Y"
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
├── controllers/    REST endpoints (V2x, Asn1, Stats, Contact, etc.)
├── services/       Lógica de negocio (V2XConversionService, WindEngineService)
├── repo/           RepoClient — HTTP client para v2x-tools-repo
├── monitoring/     Notificaciones Telegram
├── stats/          CSV de estadísticas de uso
└── utils/          Utilidades
```

## Endpoints principales

- `POST /api/convert` — encode/decode/convert mensajes V2X
- `GET  /command/random` — genera payload random/minimal/maximal
- `GET  /api/capabilities` — descripción de capacidades (para MCP)
- `POST /api/support/report` — reporte de soporte

## Notas

- `wind.connector` es un fat jar que embebe `wind.lib`, `portshub`,
  `commons.translators` y `commons.modules.serializers`.
  `includeSystemScope=true` en el spring-boot-maven-plugin asegura que
  los system-scope JARs se incluyan en el fat jar final.
- `RepoClient` en `main/repo/` implementa la interfaz `Asn1Repo` de
  `wind_parser`. Hay una copia idéntica en `v2x-tools-repo-client`
  (artefacto separado para uso de `wind_generator`).
