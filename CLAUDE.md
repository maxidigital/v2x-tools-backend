# V2X.tools - Proyecto de Herramientas V2X

## Descripción General
V2X.tools es una aplicación web para decodificar y codificar mensajes V2X (Vehicle-to-Everything) usando UPER (Unaligned Packed Encoding Rules). Es una herramienta diseñada para ingenieros automotrices e investigadores que trabajan con comunicación vehicular, autos conectados y sistemas de tráfico inteligente.

## Tecnologías Principales
- **Backend**: Java 8 con servidor HTTP embebido (com.sun.net.httpserver)
- **Frontend**: HTML5, JavaScript vanilla, Tailwind CSS
- **Build**: Maven para Java, npm para assets frontend
- **Dependencias clave**:
  - wind.connector (v6.0) - Biblioteca V2X del DLR
  - telegrambots (v5.1.0) - Para notificaciones
  - better-sqlite3 - Base de datos local
  - Tailwind CSS v4.1.4 - Estilos

## Estructura del Proyecto

### Backend (Java)
- **MainWeb.java**: Punto de entrada principal, configura el servidor HTTP
- **handlers/**: Manejo de rutas HTTP
  - MainHandler: Maneja rutas web principales
  - UPER2JSONHandler: Convierte UPER a JSON
  - ContactFormHandler: Procesa formularios de contacto
  - SimpleApiDocsHandler: Documentación API
  - SitemapHandler: Genera sitemap.xml
- **services/**: Lógica de negocio
  - V2XConversionService: Servicio principal de conversión
- **monitoring/**: Sistema de notificaciones Telegram
- **stats/**: Recopilación de estadísticas de uso

### Frontend (web/)
- **index.html**: Interfaz principal con formularios de conversión
- **v2xConverter.js**: Lógica JavaScript del cliente
- **styles.css**: Estilos personalizados + Tailwind
- **doc/**: Documentación API interactiva

## API Endpoints

### Nuevos endpoints REST (recomendados):
- `POST /api/v2x/uper/json` - UPER a JSON
- `POST /api/v2x/uper/xml` - UPER a XML  
- `POST /api/v2x/json/uper` - JSON a UPER
- `POST /api/v2x/xml/json` - XML a JSON

### Endpoints deprecados:
- `/uper2json` - Será eliminado en v2.0 (Junio 2025)

## Configuración

### Variables de entorno y argumentos:
- `--port`: Puerto del servidor (default: 8080)
- `--web-enabled`: Habilita interfaz web
- `--forwarding-port`: Puerto de reenvío
- `--log`: Habilita logging
- `--debug`: Modo debug

### Archivos de configuración:
- `config.properties`: Configuración general
- `nginx-v2x.conf` / `nginx-v2x-ssl.conf`: Configuración nginx

## Scripts de Deployment
- `deploy.sh`: Script principal de despliegue
- `restart.sh`: Reinicia el servicio
- `localhost-run.sh`: Ejecuta localmente
- `status.sh`: Verifica estado del servicio
- `logs.sh`: Ver logs

## Desarrollo

### Compilar backend:
```bash
mvn clean package
```

### Compilar CSS:
```bash
npm run build:css
```

### Desarrollo con hot-reload CSS:
```bash
npm run watch:css
```

## Notas Importantes
- Versión actual: 1.8
- Puerto por defecto: 8080
- Requiere Java 8+
- Los endpoints legacy están marcados como deprecados
- Incluye Google Analytics (G-EGM7LYNG67)
- Copyright: German Aerospace Center (DLR)

## Monitoreo
- Sistema de notificaciones via Telegram
- Recopilación de estadísticas en archivos CSV
- Logs en carpeta `log/`