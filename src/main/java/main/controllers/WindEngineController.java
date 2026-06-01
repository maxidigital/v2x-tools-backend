package main.controllers;

import main.services.WindEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2x/messages")
public class WindEngineController {

    private final WindEngineService engineService;

    public WindEngineController(WindEngineService engineService) {
        this.engineService = engineService;
    }

    /** Loads the given aliases into the user's engine. Body: ["cam_v2", "denm_v2"] */
    @PostMapping("/load")
    public ResponseEntity<?> load(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestBody List<String> aliases) {

        WindEngineService.LoadResult result = engineService.load(userId, aliases);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("userId",     userId);
        body.put("requested",  aliases.size());
        body.put("registered", result.registered().size());
        body.put("skipped",    result.skipped().size());
        body.put("aliases",    result.registered());
        if (!result.errors().isEmpty())
            body.put("errors", result.errors());
        return ResponseEntity.ok(body);
    }

    /** Returns the aliases loaded in the engine for the given user. */
    @GetMapping
    public ResponseEntity<?> aliases(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId",  userId,
                "aliases", engineService.getAliases(userId)));
    }

    /** Evicts the engine for the given user. */
    @DeleteMapping
    public ResponseEntity<?> evict(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        engineService.evict(userId);
        return ResponseEntity.ok().build();
    }
}
