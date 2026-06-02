package main.controllers;

import main.engine.EngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2x")
public class RandomController {

    private final EngineClient engine;

    public RandomController(EngineClient engine) {
        this.engine = engine;
    }

    @GetMapping("/generate")
    public ResponseEntity<String> random(
            @RequestParam String mid,
            @RequestParam(defaultValue = "UPER") String format,
            @RequestParam(defaultValue = "false") boolean minimal,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return ResponseEntity.ok(engine.generate(userId, mid, format, minimal));
    }
}
