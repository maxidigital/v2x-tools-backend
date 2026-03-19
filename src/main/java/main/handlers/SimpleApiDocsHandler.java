package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * SimpleApiDocsHandler - Serves OpenAPI documentation without external dependencies
 * 
 * @author v2x.tools
 */
public class SimpleApiDocsHandler implements HttpHandler {
    
    private static final String OPENAPI_JSON = "{\n" +
        "  \"openapi\": \"3.0.0\",\n" +
        "  \"info\": {\n" +
        "    \"title\": \"V2X.tools API (Beta)\",\n" +
        "    \"version\": \"1.8-beta\",\n" +
        "    \"description\": \"🚧 BETA - Vehicle-to-Everything (V2X) message conversion API. Convert between different V2X message formats including UPER, JSON, XML, and more. This is a beta release - please report any issues.\",\n" +
        "    \"contact\": {\n" +
        "      \"name\": \"V2X.tools Support\",\n" +
        "      \"url\": \"https://v2x.tools\",\n" +
        "      \"email\": \"support@v2x.tools\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"servers\": [\n" +
        "    {\n" +
        "      \"url\": \"https://v2x.tools\",\n" +
        "      \"description\": \"Production server\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"url\": \"http://localhost:8080\",\n" +
        "      \"description\": \"Development server\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"paths\": {\n" +
        "    \"/api/v2x/{from}/{to}\": {\n" +
        "      \"post\": {\n" +
        "        \"summary\": \"Convert V2X message between formats\",\n" +
        "        \"description\": \"Convert a V2X message from one format to another. Supports UPER, JSON, XML, and WER formats. Use Content-Type header to specify data representation: text/plain for hex strings, application/octet-stream for binary data.\",\n" +
        "        \"operationId\": \"convertV2X\",\n" +
        "        \"tags\": [\"V2X Conversion\"],\n" +
        "        \"parameters\": [\n" +
        "          {\n" +
        "            \"name\": \"from\",\n" +
        "            \"in\": \"path\",\n" +
        "            \"required\": true,\n" +
        "            \"description\": \"Source format\",\n" +
        "            \"schema\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"enum\": [\"uper\", \"json\", \"xml\", \"wer\"]\n" +
        "            }\n" +
        "          },\n" +
        "          {\n" +
        "            \"name\": \"to\",\n" +
        "            \"in\": \"path\",\n" +
        "            \"required\": true,\n" +
        "            \"description\": \"Target format\",\n" +
        "            \"schema\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"enum\": [\"uper\", \"json\", \"xml\", \"wer\"]\n" +
        "            }\n" +
        "          }\n" +
        "        ],\n" +
        "        \"requestBody\": {\n" +
        "          \"description\": \"V2X message data to convert\",\n" +
        "          \"required\": true,\n" +
        "          \"content\": {\n" +
        "            \"text/plain\": {\n" +
        "              \"schema\": {\n" +
        "                \"type\": \"string\"\n" +
        "              },\n" +
        "              \"example\": \"01142b88000000\",\n" +
        "              \"description\": \"Hexadecimal string representation\"\n" +
        "            },\n" +
        "            \"application/octet-stream\": {\n" +
        "              \"schema\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"format\": \"binary\"\n" +
        "              },\n" +
        "              \"description\": \"Binary data representation\"\n" +
        "            }\n" +
        "          }\n" +
        "        },\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"Successful conversion\",\n" +
        "            \"content\": {\n" +
        "              \"application/json\": {\n" +
        "                \"schema\": {\n" +
        "                  \"type\": \"object\",\n" +
        "                  \"properties\": {\n" +
        "                    \"header\": {\n" +
        "                      \"type\": \"object\"\n" +
        "                    }\n" +
        "                  }\n" +
        "                }\n" +
        "              }\n" +
        "            }\n" +
        "          },\n" +
        "          \"400\": {\n" +
        "            \"description\": \"Bad request - Invalid input or format\",\n" +
        "            \"content\": {\n" +
        "              \"text/plain\": {\n" +
        "                \"schema\": {\n" +
        "                  \"type\": \"string\"\n" +
        "                }\n" +
        "              }\n" +
        "            }\n" +
        "          },\n" +
        "          \"500\": {\n" +
        "            \"description\": \"Internal server error\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"/uper2json\": {\n" +
        "      \"post\": {\n" +
        "        \"summary\": \"Convert UPER to JSON (deprecated)\",\n" +
        "        \"description\": \"⚠️ **DEPRECATED since v1.8** - Use /api/v2x/uper/json instead. This endpoint will be removed in v2.0 (June 2025). Returns deprecation headers: X-API-Deprecated, X-API-Sunset.\",\n" +
        "        \"deprecated\": true,\n" +
        "        \"tags\": [\"Legacy\"],\n" +
        "        \"requestBody\": {\n" +
        "          \"description\": \"UPER encoded hex string\",\n" +
        "          \"required\": true,\n" +
        "          \"content\": {\n" +
        "            \"text/plain\": {\n" +
        "              \"schema\": {\n" +
        "                \"type\": \"string\"\n" +
        "              },\n" +
        "              \"example\": \"020000\"\n" +
        "            }\n" +
        "          }\n" +
        "        },\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"Successful conversion\"\n" +
        "          },\n" +
        "          \"400\": {\n" +
        "            \"description\": \"Error decoding message\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"/\": {\n" +
        "      \"post\": {\n" +
        "        \"summary\": \"Web interface conversion endpoint\",\n" +
        "        \"description\": \"Internal endpoint used by the web interface. Accepts JSON formatted requests.\",\n" +
        "        \"tags\": [\"V2X Conversion\"],\n" +
        "        \"requestBody\": {\n" +
        "          \"description\": \"Conversion request from web interface\",\n" +
        "          \"required\": true,\n" +
        "          \"content\": {\n" +
        "            \"application/json\": {\n" +
        "              \"schema\": {\n" +
        "                \"type\": \"object\",\n" +
        "                \"properties\": {\n" +
        "                  \"textData\": {\n" +
        "                    \"type\": \"string\",\n" +
        "                    \"description\": \"V2X message data\"\n" +
        "                  },\n" +
        "                  \"sendFormat\": {\n" +
        "                    \"type\": \"string\",\n" +
        "                    \"enum\": [\"UPER\", \"JSON\", \"XML\", \"WER\"]\n" +
        "                  },\n" +
        "                  \"receiveFormat\": {\n" +
        "                    \"type\": \"string\",\n" +
        "                    \"enum\": [\"UPER\", \"JSON\", \"XML\", \"WER\"]\n" +
        "                  }\n" +
        "                },\n" +
        "                \"required\": [\"textData\", \"sendFormat\", \"receiveFormat\"]\n" +
        "              }\n" +
        "            }\n" +
        "          }\n" +
        "        },\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"Successful conversion\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"/api/monitoring/stats\": {\n" +
        "      \"post\": {\n" +
        "        \"summary\": \"Get monitoring statistics\",\n" +
        "        \"description\": \"Retrieve current monitoring statistics including uptime, message counts, and system status.\",\n" +
        "        \"tags\": [\"Monitoring\"],\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"Monitoring statistics\",\n" +
        "            \"content\": {\n" +
        "              \"text/plain\": {\n" +
        "                \"schema\": {\n" +
        "                  \"type\": \"string\"\n" +
        "                }\n" +
        "              }\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"tags\": [\n" +
        "    {\n" +
        "      \"name\": \"V2X Conversion\",\n" +
        "      \"description\": \"V2X message format conversion operations\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"Monitoring\",\n" +
        "      \"description\": \"System monitoring and statistics\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"Legacy\",\n" +
        "      \"description\": \"Legacy API endpoints (deprecated)\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";
    
    private static final String SWAGGER_UI_HTML = 
        "<!DOCTYPE html>\n" +
        "<html lang=\"en\">\n" +
        "<head>\n" +
        "    <meta charset=\"UTF-8\">\n" +
        "    <title>V2X.tools API Documentation (Beta)</title>\n" +
        "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css\">\n" +
        "    <link rel=\"icon\" type=\"image/png\" href=\"/favicon.ico\">\n" +
        "    <style>\n" +
        "        html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }\n" +
        "        *, *:before, *:after { box-sizing: inherit; }\n" +
        "        body { margin: 0; background: #fafafa; }\n" +
        "        .topbar { display: none !important; }\n" +
        "        .swagger-ui .info { margin: 20px 0; }\n" +
        "        .swagger-ui .info .title { color: #3b4151; }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "    <div id=\"swagger-ui\"></div>\n" +
        "    <script src=\"https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>\n" +
        "    <script src=\"https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-standalone-preset.js\"></script>\n" +
        "    <script>\n" +
        "        window.onload = function() {\n" +
        "            const ui = SwaggerUIBundle({\n" +
        "                url: \"/api-docs\",\n" +
        "                dom_id: '#swagger-ui',\n" +
        "                deepLinking: true,\n" +
        "                presets: [\n" +
        "                    SwaggerUIBundle.presets.apis,\n" +
        "                    SwaggerUIStandalonePreset\n" +
        "                ],\n" +
        "                plugins: [\n" +
        "                    SwaggerUIBundle.plugins.DownloadUrl\n" +
        "                ],\n" +
        "                layout: \"StandaloneLayout\",\n" +
        "                validatorUrl: null,\n" +
        "                tryItOutEnabled: true,\n" +
        "                supportedSubmitMethods: ['get', 'post', 'put', 'delete', 'patch'],\n" +
        "                onComplete: function() {\n" +
        "                    console.log('Swagger UI loaded');\n" +
        "                }\n" +
        "            });\n" +
        "            window.ui = ui;\n" +
        "        }\n" +
        "    </script>\n" +
        "</body>\n" +
        "</html>";
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        if (!method.equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        
        try {
            if (path.equals("/api-docs") || path.equals("/api-docs.json")) {
                handleApiDocsJson(exchange);
            } else if (path.equals("/swagger-ui") || path.equals("/swagger-ui/")) {
                handleSwaggerUI(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage(), "text/plain");
        }
    }
    
    private void handleApiDocsJson(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        sendResponse(exchange, 200, OPENAPI_JSON, "application/json");
    }
    
    private void handleSwaggerUI(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, SWAGGER_UI_HTML, "text/html");
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}