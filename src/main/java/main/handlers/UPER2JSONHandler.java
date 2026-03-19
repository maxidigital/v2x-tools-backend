package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import main.A;
import main.ContentTypes;
import main.services.V2XConversionService;
import main.services.ConversionResult;
import main.utils.HttpResponseUtils;

/**
 * UPER2JSONHandler - Legacy UPER to JSON conversion endpoint
 * 
 * @deprecated Since version 1.8. Use /api/v2x/uper/json instead.
 * This endpoint will be removed in version 2.0 (planned for 2025-Q2).
 * 
 * Migration Guide:
 * - Old: POST /uper2json with hex payload in body
 * - New: POST /api/v2x/uper/json with hex payload in body
 * 
 * @author bott_ma
 */
@Deprecated
public class UPER2JSONHandler implements HttpHandler {
    
    private final V2XConversionService conversionService;
    
    public UPER2JSONHandler() {
        this.conversionService = V2XConversionService.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String contentType = ContentTypes.getContentType(exchange);
        String clientIP = HttpResponseUtils.getClientIP(exchange);
        String endpoint = "/uper2json";
        
        A.pt("Requesting UPER2JSON ContentType: " + contentType);

        try {
            // Add deprecation warning to response headers
            exchange.getResponseHeaders().set("X-API-Deprecated", "true");
            exchange.getResponseHeaders().set("X-API-Deprecated-Since", "v1.8");
            exchange.getResponseHeaders().set("X-API-Sunset", "2025-06-01");
            exchange.getResponseHeaders().set("X-API-Migration", "Use POST /api/v2x/uper/json instead");
            
            // Read request body
            byte[] blobData = HttpResponseUtils.readRequestBodyBytes(exchange);
            A.pt("Body length: " + blobData.length);
            
            // Log deprecation usage
            A.p("⚠️ DEPRECATED ENDPOINT USED: /uper2json from %s", clientIP);

            // Use centralized conversion service
            ConversionResult result = conversionService.convertUperToJson(blobData, contentType, clientIP, endpoint);
            
            // Handle response
            if (result.isSuccess()) {
                String response = result.getResponseData();
                
                // Apply JSON compression for successful responses (maintaining compatibility)
                if (result.getHttpStatusCode() == 200) {
                    response = JsonCompressor.compressJson(response);
                }
                
                HttpResponseUtils.sendConversionResponse(exchange, response, result.getHttpStatusCode());
            } else {
                HttpResponseUtils.sendErrorResponse(exchange, result.getErrorMessage(), result.getHttpStatusCode());
            }
            
        } catch (Exception ex) {
            A.p("Error in UPER2JSONHandler: " + ex.getMessage());
            ex.printStackTrace();
            HttpResponseUtils.sendErrorResponse(exchange, "Internal server error", 500);
        }
    }
}
