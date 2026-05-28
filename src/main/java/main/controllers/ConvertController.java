package main.controllers;

import main.services.ConversionResult;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConvertController {

    private final V2XConversionService conversionService;

    public ConvertController(V2XConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping("/api/convert")
    public ResponseEntity<String> convert(
            @RequestBody String body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        ConversionResult result = conversionService.convertFromWebRequest(body, clientIp, "/api/convert", userId);

        return ResponseEntity
            .status(result.getHttpStatusCode())
            .body(result.isSuccess() ? result.getResponseData() : result.getErrorMessage());
    }
}
