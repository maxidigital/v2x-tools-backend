//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import main.A;
import main.BadRequest;
import main.ContentTypes;
import main.HttpTools;
import main.services.V2XConversionService;
import main.services.ConversionResult;
import main.utils.HttpResponseUtils;

/**
 * PostRequestHandler - Web interface conversion endpoint
 * 
 * @deprecated Internal endpoint used by web interface. Consider using RESTful API /api/v2x/{from}/{to} for programmatic access.
 * This endpoint will remain for web interface compatibility but is not recommended for API integrations.
 * 
 * @author bott_ma
 */
@Deprecated
public class PostRequestHandler implements HttpHandler {
    
    private final V2XConversionService conversionService;
    
    public PostRequestHandler() {
        this.conversionService = V2XConversionService.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String contentType = ContentTypes.getContentType(exchange);
        String clientIP = HttpResponseUtils.getClientIP(exchange);
        String endpoint = exchange.getRequestURI().getPath();

        A.pt("Processing POST request: " + contentType);
        
        // Validate content type
        if (!(contentType.equals(ContentTypes.CT_TEXT_PLAIN) || contentType.equals(ContentTypes.CT_JSON))) {
            BadRequest badRequest = new BadRequest("Content type %s not supported", contentType);
            badRequest.handle(exchange);
            return;
        }

        try {
            // Read request data
            String textFromRequest = HttpTools.getTextFromRequest(exchange);
            A.pt("Received data from frontend: " + textFromRequest);

            // Use centralized conversion service
            ConversionResult result = conversionService.convertFromWebRequest(textFromRequest, clientIP, endpoint);

            // Handle response
            if (result.isSuccess()) {
                String response = result.getResponseData();
                A.pt("Responding: %s", response.replaceAll("\\n", ""));
                
                HttpResponseUtils.sendTextResponse(exchange, response, result.getHttpStatusCode());
            } else {
                // Maintain backward compatibility with EmptyResponse behavior
                main.EmptyResponse emptyResponse = new main.EmptyResponse(result.getErrorMessage());
                emptyResponse.handle(exchange);
            }
            
        } catch (Exception ex) {
            A.p("Error in PostRequestHandler: " + ex.getMessage());
            ex.printStackTrace();
            main.EmptyResponse emptyResponse = new main.EmptyResponse("Internal server error");
            emptyResponse.handle(exchange);
        }
    }
}
