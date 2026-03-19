package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SitemapHandler - Serves sitemap.xml and robots.txt for SEO
 * 
 * @author v2x.tools
 */
public class SitemapHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        if (!method.equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        
        try {
            if (path.equals("/sitemap.xml")) {
                handleSitemap(exchange);
            } else if (path.equals("/robots.txt")) {
                handleRobots(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage(), "text/plain");
        }
    }
    
    private void handleSitemap(HttpExchange exchange) throws IOException {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        String sitemap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                "    <url>\n" +
                "        <loc>https://v2x.tools/</loc>\n" +
                "        <lastmod>" + currentDate + "</lastmod>\n" +
                "        <changefreq>weekly</changefreq>\n" +
                "        <priority>1.0</priority>\n" +
                "    </url>\n" +
                "    <url>\n" +
                "        <loc>https://v2x.tools/swagger-ui/</loc>\n" +
                "        <lastmod>" + currentDate + "</lastmod>\n" +
                "        <changefreq>monthly</changefreq>\n" +
                "        <priority>0.8</priority>\n" +
                "    </url>\n" +
                "    <url>\n" +
                "        <loc>https://v2x.tools/api-docs</loc>\n" +
                "        <lastmod>" + currentDate + "</lastmod>\n" +
                "        <changefreq>monthly</changefreq>\n" +
                "        <priority>0.7</priority>\n" +
                "    </url>\n" +
                "</urlset>";
        
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponse(exchange, 200, sitemap, "application/xml");
    }
    
    private void handleRobots(HttpExchange exchange) throws IOException {
        String robots = "# robots.txt for v2x.tools\n" +
                "# Allow search engines to index the site\n\n" +
                "User-agent: *\n" +
                "Allow: /\n\n" +
                "# Sitemap location\n" +
                "Sitemap: https://v2x.tools/sitemap.xml\n\n" +
                "# Block AI training bots to save bandwidth\n" +
                "User-agent: GPTBot\n" +
                "Disallow: /\n\n" +
                "User-agent: CCBot\n" +
                "Disallow: /\n\n" +
                "User-agent: anthropic-ai\n" +
                "Disallow: /\n\n" +
                "User-agent: Claude-Web\n" +
                "Disallow: /\n\n" +
                "# Block aggressive SEO crawlers\n" +
                "User-agent: Semrush\n" +
                "Disallow: /\n\n" +
                "User-agent: AhrefsBot\n" +
                "Disallow: /\n\n" +
                "User-agent: MJ12bot\n" +
                "Disallow: /\n";
        
        sendResponse(exchange, 200, robots, "text/plain");
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