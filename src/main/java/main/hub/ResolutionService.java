package main.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import main.engine.EngineClient;
import main.engine.EngineResult;
import main.hub.entity.SavedMessage;
import main.repo.RepoClient;
import org.springframework.stereotype.Service;

/**
 * The hub's reactor: turns a reference ({@code X-Ref}) into an engineId and operates on it.
 * typeRef ({@code alias:typeName}, has ':') = raw type, no fixups. messageRef (no ':') = a saved message
 * with its fixups. Caches {@code ref→engineId}; on {@code engineNotFound} it reloads (tree → metadata →
 * engine.load) and retries — the engine is the source of truth for what's actually loaded.
 */
@Service
public class ResolutionService {

    private final RepoClient repo;
    private final EngineClient engine;
    private final AliasService aliases;
    private final SavedMessageService messages;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> refToEngineId = new ConcurrentHashMap<>();

    public ResolutionService(RepoClient repo, EngineClient engine, AliasService aliases, SavedMessageService messages) {
        this.repo = repo;
        this.engine = engine;
        this.aliases = aliases;
        this.messages = messages;
    }

    private record Resolved(String moduleId, String type, String fixupsJson, String description) {}

    private Resolved resolve(Long userId, String ref) {
        if (ref == null || ref.isBlank())
            throw new IllegalArgumentException("X-Ref is required");
        int c = ref.indexOf(':');
        if (c >= 0) { // typeRef: alias:typeName
            String left = ref.substring(0, c);
            String type = ref.substring(c + 1);
            if (type.isBlank()) throw new IllegalArgumentException("typeRef must be alias:typeName");
            String moduleId = aliases.resolveModuleId(userId, left);
            return new Resolved(moduleId, type, "[]", moduleId + ":" + type);
        }
        SavedMessage m = messages.find(userId, ref)
                .orElseThrow(() -> new IllegalArgumentException("no saved message: " + ref));
        JsonNode d;
        try {
            d = mapper.readTree(m.getData() == null || m.getData().isBlank() ? "{}" : m.getData());
        } catch (Exception e) {
            throw new IllegalArgumentException("corrupt message data for '" + ref + "': " + e.getMessage());
        }
        String moduleAlias = text(d, "moduleAlias");
        String rootType = text(d, "rootType");
        if (moduleAlias == null || rootType == null)
            throw new IllegalArgumentException("message '" + ref + "' missing moduleAlias/rootType");
        String moduleId = aliases.resolveModuleId(userId, moduleAlias);
        String fixupsJson = d.has("fixups") ? d.get("fixups").toString() : "[]";
        String desc = text(d, "description");
        if (desc == null || desc.isBlank()) desc = "message:" + ref;
        return new Resolved(moduleId, rootType, fixupsJson, desc);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private String engineId(Long userId, String ref) {
        return refToEngineId.computeIfAbsent(userId + "|" + ref, k -> load(userId, ref));
    }

    private String reload(Long userId, String ref) {
        String id = load(userId, ref);
        refToEngineId.put(userId + "|" + ref, id);
        return id;
    }

    private String load(Long userId, String ref) {
        Resolved r = resolve(userId, ref);
        String tree = repo.tree(r.moduleId(), r.type());           // throws DefinitionNotFoundException on 404
        String metadata = buildMetadata(r.description(), r.fixupsJson());
        return engine.load(tree, metadata);
    }

    private String buildMetadata(String description, String fixupsJson) {
        try {
            ObjectNode meta = mapper.createObjectNode();
            meta.put("description", description);
            meta.set("fixups", mapper.readTree(fixupsJson == null || fixupsJson.isBlank() ? "[]" : fixupsJson));
            return mapper.writeValueAsString(meta);
        } catch (Exception e) {
            throw new RuntimeException("metadata build failed: " + e.getMessage(), e);
        }
    }

    public EngineResult convert(Long userId, String ref, byte[] payload, boolean binaryIn, String from, String to) {
        EngineResult res = engine.convert(engineId(userId, ref), payload, binaryIn, from, to);
        if (res.isEngineNotFound())
            res = engine.convert(reload(userId, ref), payload, binaryIn, from, to);
        return res;
    }

    public EngineResult generate(Long userId, String ref, String optionsJson) {
        EngineResult res = engine.generate(engineId(userId, ref), optionsJson);
        if (res.isEngineNotFound())
            res = engine.generate(reload(userId, ref), optionsJson);
        return res;
    }
}
