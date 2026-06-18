package main.hub;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import main.engine.EngineResult;
import main.repo.DefinitionNotFoundException;
import main.repo.RepoClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The hub's public REST API (universal; no V2X in the path). Scalar params travel in headers (so OIDs
 * with spaces never hit the URL); structured creates go in the JSON body. MCP wraps this later.
 */
@RestController
@RequestMapping("/api")
public class HubController {

    private final ResolutionService resolution;
    private final AliasService aliases;
    private final SavedMessageService messages;
    private final RepoClient repo;

    public HubController(ResolutionService resolution, AliasService aliases,
                         SavedMessageService messages, RepoClient repo) {
        this.resolution = resolution;
        this.aliases = aliases;
        this.messages = messages;
        this.repo = repo;
    }

    // ── operations ────────────────────────────────────────────────────────────

    @PostMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestHeader("X-Ref") String ref,
            @RequestHeader("X-From") String from,
            @RequestHeader("X-To") String to,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestBody(required = false) byte[] payload) {
        try {
            boolean binaryIn = contentType != null && contentType.toLowerCase().contains("octet-stream");
            EngineResult r = resolution.convert(userId, ref, payload == null ? new byte[0] : payload, binaryIn, from, to);
            return render(r, wantsBinary(accept));
        } catch (IllegalArgumentException | DefinitionNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(
            @RequestHeader("X-Ref") String ref,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestBody(required = false) String optionsJson) {
        try {
            EngineResult r = resolution.generate(userId, ref, optionsJson);
            return render(r, wantsBinary(accept));
        } catch (IllegalArgumentException | DefinitionNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    // ── saved messages ──────────────────────────────────────────────────────────

    @PostMapping("/messages")
    public ResponseEntity<?> createMessage(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            messages.create(userId, body);
            return ResponseEntity.ok(Map.of("status", "created", "name", String.valueOf(body.get("name"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> listMessages(@RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return ResponseEntity.ok(messages.list(userId));
    }

    @DeleteMapping("/messages")
    public ResponseEntity<?> deleteMessage(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader("X-Message-Name") String name) {
        return messages.delete(userId, name) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // ── aliases ─────────────────────────────────────────────────────────────────

    @PostMapping("/aliases")
    public ResponseEntity<?> createAlias(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader("X-Alias") String alias,
            @RequestHeader("X-Module-Oid") String oid) {
        try {
            aliases.create(userId, alias, oid);
            return ResponseEntity.ok(Map.of("status", "created", "alias", alias));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/aliases")
    public ResponseEntity<?> listAliases(@RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return ResponseEntity.ok(aliases.list(userId));
    }

    @DeleteMapping("/aliases")
    public ResponseEntity<?> deleteAlias(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader("X-Alias") String alias) {
        return aliases.delete(userId, alias) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // ── browse ──────────────────────────────────────────────────────────────────

    /** Modules with their oid + aliases (the hub joins the repo's list with its alias table). */
    @GetMapping("/modules")
    public ResponseEntity<?> modules(@RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        // aliases by moduleId (default + user's)
        Map<String, List<String>> byModule = new LinkedHashMap<>();
        for (Map<String, Object> a : aliases.list(userId))
            byModule.computeIfAbsent(String.valueOf(a.get("moduleId")), k -> new ArrayList<>())
                    .add(String.valueOf(a.get("alias")));
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode m : repo.modules()) {
            String moduleId = text(m, "moduleId");
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("oid", text(m, "oid"));
            e.put("moduleName", text(m, "moduleName"));
            e.put("moduleId", moduleId);
            e.put("aliases", byModule.getOrDefault(moduleId, List.of()));
            out.add(e);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/modules/types")
    public ResponseEntity<?> moduleTypes(@RequestHeader("X-Module-Oid") String oid) {
        try {
            return ResponseEntity.ok(repo.types(aliases.moduleIdForOid(oid)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private ResponseEntity<?> render(EngineResult r, boolean binaryOut) {
        if (r != null && r.isOk()) {
            String data = r.data;
            if (binaryOut && data != null && (data.startsWith("uper:") || data.startsWith("wer:"))) {
                byte[] bytes = a.tools.Tools.hexStringToBytes(data.substring(data.indexOf(':') + 1));
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes);
            }
            return ResponseEntity.ok(data);
        }
        if (r != null && "decodeError".equals(r.status))
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(r.error)));
        if (r != null && r.isEngineNotFound())
            return ResponseEntity.status(404).body(Map.of("error", "definition not available"));
        return ResponseEntity.status(500).body(Map.of("error", "engine error"));
    }

    private static boolean wantsBinary(String accept) {
        return accept != null && accept.toLowerCase().contains("octet-stream");
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null ? "" : v.asText();
    }
}
