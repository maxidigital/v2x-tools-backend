package main.controllers;

import main.handlers.JsonCompressor;
import main.services.ConversionResult;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Since v1.8. Use POST /api/v2x/uper/json instead.
 */
@RestController
public class LegacyController {

    private final V2XConversionService conversionService;

    public LegacyController(V2XConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping("/uper2json")
    public ResponseEntity<String> uperToJson(
            @RequestBody byte[] body,
            @RequestHeader(value = "Content-Type", defaultValue = "text/plain") String contentType,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        ConversionResult result = conversionService.convertUperToJson(body, contentType, clientIp, "/uper2json");

        if (result.isSuccess()) {
            String compressed = JsonCompressor.compressJson(result.getResponseData());
            return ResponseEntity.ok()
                    .header("X-API-Deprecated", "true")
                    .header("X-API-Deprecated-Since", "v1.8")
                    .header("X-API-Sunset", "2025-06-01")
                    .header("X-API-Migration", "Use POST /api/v2x/uper/json instead")
                    .body(compressed);
        } else {
            return ResponseEntity.status(result.getHttpStatusCode())
                    .header("X-API-Deprecated", "true")
                    .body(result.getErrorMessage());
        }
    }
}
