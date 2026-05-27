package main.controllers;

import main.services.Asn1ParsingService;
import main.services.ConversionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asn1")
public class Asn1Controller {

    private final Asn1ParsingService parsingService;

    public Asn1Controller(Asn1ParsingService parsingService) {
        this.parsingService = parsingService;
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parse(
            @RequestBody String body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        ConversionResult result = parsingService.parse(body, clientIp, "/api/asn1/parse");

        return ResponseEntity
                .status(result.getHttpStatusCode())
                .body(result.isSuccess() ? result.getResponseData() : result.getErrorMessage());
    }
}
