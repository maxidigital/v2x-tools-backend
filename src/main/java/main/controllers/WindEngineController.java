package main.controllers;

import main.services.WindEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/engine")
public class WindEngineController {

    private final WindEngineService engineService;

    public WindEngineController(WindEngineService engineService) {
        this.engineService = engineService;
    }

    /** Loads the given aliases into the user's engine. Body: ["cam_v2", "denm_v2"] */
    @PostMapping("/{userId}/load")
    public ResponseEntity<?> load(
            @PathVariable Long userId,
            @RequestBody List<String> aliases) {

        List<String> registered = engineService.load(userId, aliases);
        return ResponseEntity.ok(Map.of(
                "userId",     userId,
                "requested",  aliases.size(),
                "registered", registered.size(),
                "aliases",    registered));
    }

    /** Returns the aliases loaded in the engine for the given userId. */
    @GetMapping("/{userId}/aliases")
    public ResponseEntity<?> aliases(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId",   userId,
                "aliases",  engineService.getAliases(userId)));
    }

    /** Evicts the engine for the given userId. */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> evict(@PathVariable Long userId) {
        engineService.evict(userId);
        return ResponseEntity.ok().build();
    }
}
