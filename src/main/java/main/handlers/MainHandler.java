//****************************************************************************//
package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import main.A;
import main.BadRequest;
import main.gets.GetClear;
import main.gets.GetCopyToClipboard;
import main.stats.StatsHandler;
import main.services.V2XConversionService;
import main.services.ConversionResult;
import main.monitoring.TelegramCenter;
import main.utils.HttpResponseUtils;
import main.config.ConfigurationManager;
import main.monitoring.NotificationFilter;

// Define a custom handler for incoming requests
public class MainHandler implements HttpHandler
{

    private final boolean webEnabled;
    private final V2XConversionService conversionService;
    private final TelegramCenter telegramCenter;

    /**
     *
     * @param webEnabled
     */
    public MainHandler(boolean webEnabled) {
        this.webEnabled = webEnabled;
        this.conversionService = V2XConversionService.getInstance();
        this.telegramCenter = TelegramCenter.getInstance();
    }

    private String cleanPath(String path) {
        if (path.startsWith("/v2xtools"))
            path = path.replace("/v2xtools", "");
        return path;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        A.p("");
        A.p("--------------------------------------------------------------------------------------------");
        A.pt("New request: method(%s) remoteAddress(%s) ", exchange.getRequestMethod(), exchange.getRemoteAddress());

        String path = exchange.getRequestURI().getPath();
        path = cleanPath(path);
        A.pt("Path: " + path);

        exchange.setAttribute("request_origin", "website");

        if (exchange.getRequestMethod().equalsIgnoreCase("GET") && webEnabled) {
            // Write the response content

            if (path.equals("/")) {
                path = "/index.html";
                // Send notification for homepage visits
                String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
                String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
                String message = "🌐 New homepage visit\n" +
                               "🌍 IP: " + clientIP + "\n" +
                               "🕒 Time: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n" +
                               "📱 Browser: " + (userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) : "Unknown");
                telegramCenter.sendNotification(message, main.monitoring.NotificationType.STATUS);
            }

            if (path.startsWith("/command")) {
                if (path.equals("/command/copyToClipboard")) {
                    GetCopyToClipboard.exec(exchange);
                } else if (path.equals("/command/clear")) {
                    GetClear.exec(exchange);
                } else if (path.equals("/command/random")) {
                    RandomHandler randomHandler = new RandomHandler();
                    randomHandler.handle(exchange);
                } else {
                    new BadRequest().handle(exchange);
                }            
            } else if (path.startsWith("/api-docs") || path.startsWith("/swagger-ui")) {
                // Handle API documentation requests
                SimpleApiDocsHandler apiDocsHandler = new SimpleApiDocsHandler();
                apiDocsHandler.handle(exchange);
            } else if (path.equals("/sitemap.xml") || path.equals("/robots.txt")) {
                // Handle SEO files
                SitemapHandler sitemapHandler = new SitemapHandler();
                sitemapHandler.handle(exchange);
            } else if (path.startsWith("/")) {
                FileServerHandler fileServer = new FileServerHandler(path);
                fileServer.handle(exchange);
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            if (path.equals("/") || path.equals("/localhost")) {
                PostRequestHandler postRequestHandler = new PostRequestHandler();
                postRequestHandler.handle(exchange);            
            } else if (path.equals("/uper2json") || path.equals("/uper2json/")) {
                UPER2JSONHandler handler = new UPER2JSONHandler();
                handler.handle(exchange);
            } else if (path.startsWith("/api/v2x/")) {
                // New RESTful API endpoints: /api/v2x/{from}/{to}
                handleV2XApiRequest(exchange, path);
            } else if (path.equals("/api/access-stats")) {
                StatsHandler statsHandler = new StatsHandler("counter");
                statsHandler.handle(exchange);
            } else if (path.equals("/api/monitoring/stats")) {
                // New monitoring stats endpoint
                handleMonitoringStats(exchange);
            } else if (path.equals("/api/monitoring/test")) {
                // Test monitoring endpoint
                handleMonitoringTest(exchange);
            }
        } else {
            new BadRequest().handle(exchange);
        }
    }
    
    /**
     * Handle new RESTful V2X API requests: /api/v2x/{from}/{to}
     */
    private void handleV2XApiRequest(HttpExchange exchange, String path) throws IOException {
        String clientIP = HttpResponseUtils.getClientIP(exchange);
        
        try {
            // Parse path parameters: /api/v2x/{from}/{to}
            String[] pathParts = path.split("/");
            if (pathParts.length != 5) {
                HttpResponseUtils.sendErrorResponse(exchange, "Invalid API path format. Expected: /api/v2x/{from}/{to}", 400);
                return;
            }
            
            String fromFormat = pathParts[3];  // e.g., "uper"  
            String toFormat = pathParts[4];    // e.g., "json"
            
            // Read request body
            String inputData = HttpResponseUtils.readRequestBody(exchange);
            if (inputData == null || inputData.trim().isEmpty()) {
                HttpResponseUtils.sendErrorResponse(exchange, "Request body is empty", 400);
                return;
            }
            
            A.p("New V2X API request: %s -> %s from %s", fromFormat, toFormat, clientIP);
            
            // Use conversion service
            ConversionResult result = conversionService.convert(
                inputData.trim(), 
                fromFormat, 
                toFormat, 
                clientIP, 
                path
            );
            
            // Send response
            if (result.isSuccess()) {
                HttpResponseUtils.sendJsonResponse(exchange, result.getResponseData(), result.getHttpStatusCode());
            } else {
                HttpResponseUtils.sendErrorResponse(exchange, result.getErrorMessage(), result.getHttpStatusCode());
            }
            
        } catch (Exception e) {
            A.p("Error handling V2X API request: " + e.getMessage());
            telegramCenter.notifyError(e.getMessage(), path, clientIP);
            HttpResponseUtils.sendErrorResponse(exchange, "Internal server error", 500);
        }
    }
    
    /**
     * Handle monitoring stats request
     */
    private void handleMonitoringStats(HttpExchange exchange) throws IOException {
        try {
            ConfigurationManager config = ConfigurationManager.getInstance();
            String stats = "📊 V2X.tools Monitoring Dashboard\n\n" +
                          "🔧 Configuration Status:\n" +
                          "- Telegram Enabled: " + config.isTelegramEnabled() + "\n" +
                          "- Bot Token: " + (config.getTelegramBotToken().isEmpty() ? "NOT SET" : "SET") + "\n" +
                          "- Chat ID: " + config.getTelegramChatId() + "\n\n" +
                          conversionService.getStats() + "\n\n" +
                          telegramCenter.getStats();
            
            HttpResponseUtils.sendTextResponse(exchange, stats, 200);
            
        } catch (Exception e) {
            HttpResponseUtils.sendErrorResponse(exchange, "Error retrieving stats", 500);
        }
    }
    
    /**
     * Handle monitoring test request
     */
    private void handleMonitoringTest(HttpExchange exchange) throws IOException {
        String clientIP = HttpResponseUtils.getClientIP(exchange);
        
        try {
            ConfigurationManager config = ConfigurationManager.getInstance();
            
            // Send test notification
            telegramCenter.sendNotification(
                "🧪 <b>Test Notification</b>\n" +
                "📍 From: <code>" + clientIP + "</code>\n" +
                "🔧 Endpoint: /api/monitoring/test\n" +
                "✅ Monitoring system is working!",
                main.monitoring.NotificationType.STATUS
            );
            
            String response = "Test notification sent!\n\n" +
                            "Configuration:\n" +
                            "- Telegram Enabled: " + config.isTelegramEnabled() + "\n" +
                            "- Bot Token: " + (config.getTelegramBotToken().isEmpty() ? "NOT SET" : "SET") + "\n" +
                            "- Chat ID: " + config.getTelegramChatId() + "\n\n" +
                            "Check your Telegram for the test message.";
            
            HttpResponseUtils.sendTextResponse(exchange, response, 200);
            
        } catch (Exception e) {
            A.p("Error in monitoring test: " + e.getMessage());
            HttpResponseUtils.sendErrorResponse(exchange, "Error sending test notification: " + e.getMessage(), 500);
        }
    }

}
