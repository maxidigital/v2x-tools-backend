package main.controllers;

import main.services.ConversionResult;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2x")
public class RandomController {

    private final V2XConversionService service;

    public RandomController(V2XConversionService service) {
        this.service = service;
    }

    @GetMapping("/generate")
    public ResponseEntity<String> random(
            @RequestParam String mid,
            @RequestParam(defaultValue = "UPER") String format,
            @RequestParam(defaultValue = "false") boolean minimal,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        ConversionResult result = service.generate(userId, mid, format, minimal);
        return ResponseEntity.status(result.getHttpStatusCode())
                .body(result.isSuccess() ? result.getResponseData() : result.getErrorMessage());
    }
}
