package main.controllers;

import main.services.ConversionResult;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2x")
public class V2xController {

    private final V2XConversionService conversionService;

    public V2xController(V2XConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping("/{from}/{to}")
    public ResponseEntity<String> convert(
            @PathVariable String from,
            @PathVariable String to,
            @RequestBody String body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        ConversionResult result = conversionService.convert(
            body.trim(), from, to, clientIp, "/api/v2x/" + from + "/" + to
        );

        return ResponseEntity
            .status(result.getHttpStatusCode())
            .body(result.isSuccess() ? result.getResponseData() : result.getErrorMessage());
    }
}
