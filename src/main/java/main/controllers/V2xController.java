package main.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.services.ConversionResult;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2x")
@Tag(name = "Conversion", description = "Encode, decode and convert V2X messages between formats")
public class V2xController {

    private final V2XConversionService conversionService;

    public V2xController(V2XConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping("/{from}/{to}")
    @Operation(
        summary = "Convert a V2X message",
        description = "Converts a V2X message from one format to another. " +
                      "Supported formats: UPER, WER, JSON, XML. " +
                      "The message type is auto-detected from the first two bytes of the payload."
    )
    public ResponseEntity<String> convert(
            @Parameter(description = "Source format (UPER, WER, JSON, XML)") @PathVariable String from,
            @Parameter(description = "Target format (UPER, WER, JSON, XML)") @PathVariable String to,
            @RequestBody String body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            @Parameter(description = "User ID (0 = anonymous/public)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {

        ConversionResult result = conversionService.convert(
            body.trim(), from, to, clientIp, "/api/v2x/" + from + "/" + to, userId
        );

        return ResponseEntity
            .status(result.getHttpStatusCode())
            .body(result.isSuccess() ? result.getResponseData() : result.getErrorMessage());
    }
}
