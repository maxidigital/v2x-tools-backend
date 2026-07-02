# `v2x-tools-backend` — cheat-sheet (LEGACY)

> Quick-ref operativo (se auto-carga). **Arquitectura / historia →
> [`v2x-tools-docs/services/backend/`](../v2x-tools-docs/services/backend/README.md)**. Este archivo: lo operativo.

**Qué es**: **legacy, sin DB, fuera del pipeline V2X.** Tras el split del hub quedó con el cruft no-V2X. Vive
porque el frontend usa el contact form. Spring Boot 3.2 · Java 17.

## Lo único vivo
| Endpoint | Qué |
|---|---|
| `POST /api/contact` | contact form → email (email.admin/mailjet) |
| `POST /api/access-stats` | stats (CSV en disco) |
| `POST /api/monitoring/{stats,test}` | Telegram |

El Caddy del frontend rutea esos 3 paths acá; el resto de `/api/*` va al hub.

## Estructura / deps
- `controllers/`, `monitoring/` (Telegram), `stats/` (CSV). **Sin DB, sin JPA.**
- `libs/` (system scope): `wind.lib` (logging), `wind_commons`, `email.admin`.

## Build / Deploy
`mvn clean package -DskipTests` → push a `main` → Railway. Env: `NOTIFICATION_SERVICE_URL`, credenciales de email, `PORT` (8080).

## Gotchas
- **No adjuntar Postgres** — sin JPA arranca sin DB (un Spring con JPA intentaría conectar al boot).
