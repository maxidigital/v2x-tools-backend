package main.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.engine.EngineService;
import main.loader.MessageLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2x/messages")
@Tag(name = "Messages", description = "Manage which message types are loaded for a user")
public class WindEngineController {

    private final EngineService engineService;
    private final MessageLoader loader;

    public WindEngineController(EngineService engineService, MessageLoader loader) {
        this.engineService = engineService;
        this.loader = loader;
    }

    @PostMapping("/load")
    @Operation(
        summary = "Load message types",
        description = "Loads the given message aliases into the user's engine. " +
                      "Example body: [\"cam_v2\", \"denm_v2\"]. " +
                      "Public modules use userId=0 and are loaded at startup."
    )
    public ResponseEntity<?> load(
            @Parameter(description = "User ID (0 = anonymous/public)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestBody List<String> aliases) {

        MessageLoader.LoadResult result = loader.load(userId, aliases);
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

    @GetMapping
    @Operation(summary = "List loaded message types", description = "Returns the aliases currently loaded in the user's engine.")
    public ResponseEntity<?> aliases(
            @Parameter(description = "User ID (0 = anonymous/public)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId",  userId,
                "aliases", engineService.getAliases(userId)));
    }

    @DeleteMapping
    @Operation(summary = "Reset engine", description = "Evicts the user's engine from the cache. Next conversion will reload from scratch.")
    public ResponseEntity<?> evict(
            @Parameter(description = "User ID (0 = anonymous/public)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        engineService.evict(userId);
        return ResponseEntity.ok().build();
    }
}
