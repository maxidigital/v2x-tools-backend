package main.controllers;

import main.services.V2XConversionService;
import main.services.ConversionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConvertController {

    private final V2XConversionService conversionService = V2XConversionService.getInstance();

    //@PostMapping("/")
    @PostMapping("/api/convert")
    public ResponseEntity<String> convert(
            @RequestBody String body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        ConversionResult result = conversionService.convertFromWebRequest(body, clientIp, "/");

        if (result.isSuccess()) {
            return ResponseEntity.status(result.getHttpStatusCode()).body(result.getResponseData());
        } else {
            return ResponseEntity.status(result.getHttpStatusCode()).body(result.getErrorMessage());
        }
    }
}