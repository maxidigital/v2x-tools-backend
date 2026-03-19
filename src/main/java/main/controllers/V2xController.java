package main.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import main.services.V2XConversionService;
import main.services.ConversionResult;

@RestController
@RequestMapping("/api/v2x")
public class V2xController {

    private final V2XConversionService conversionService = V2XConversionService.getInstance();

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