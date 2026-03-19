package main.utils;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import main.SecurityHeaders;

/**
 * HttpResponseUtils - Centralized utility for HTTP response handling
 * Eliminates code duplication across handlers for response creation
 * 
 * @author v2x.tools
 */
public class HttpResponseUtils {
    
    /**
     * Send JSON response with proper headers
     */
    public static void sendJsonResponse(HttpExchange exchange, String jsonData, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        SecurityHeaders.addSecurityHeaders(exchange);
        
        byte[] responseBytes = jsonData.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Send text/plain response with proper headers
     */
    public static void sendTextResponse(HttpExchange exchange, String textData, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        SecurityHeaders.addSecurityHeaders(exchange);
        
        byte[] responseBytes = textData.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Send error response with proper headers
     */
    public static void sendErrorResponse(HttpExchange exchange, String errorMessage, int statusCode) throws IOException {
        sendTextResponse(exchange, errorMessage, statusCode);
    }
    
    /**
     * Send success response with conversion result
     * Determines content type based on data format
     */
    public static void sendConversionResponse(HttpExchange exchange, String responseData, int statusCode) throws IOException {
        // Try to determine if response is JSON
        boolean isJson = responseData.trim().startsWith("{") || responseData.trim().startsWith("[");
        
        if (isJson) {
            sendJsonResponse(exchange, responseData, statusCode);
        } else {
            sendTextResponse(exchange, responseData, statusCode);
        }
    }
    
    /**
     * Read request body as string
     */
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        try (java.io.InputStream inputStream = exchange.getRequestBody();
             java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Read request body as byte array
     */
    public static byte[] readRequestBodyBytes(HttpExchange exchange) throws IOException {
        try (java.io.InputStream inputStream = exchange.getRequestBody();
             java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, bytesRead);
            }
            return result.toByteArray();
        }
    }
    
    /**
     * Get client IP address from exchange
     */
    public static String getClientIP(HttpExchange exchange) {
        return exchange.getRemoteAddress().toString();
    }
}