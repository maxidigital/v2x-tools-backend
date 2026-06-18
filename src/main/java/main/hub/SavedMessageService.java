package main.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import main.hub.entity.SavedMessage;
import main.hub.repo.SavedMessageRepository;
import org.springframework.stereotype.Service;

/**
 * Saved messages: a named handle whose definition lives in a {@code data} JSON blob
 * ({@code moduleAlias}, {@code rootType}, {@code fixups}, {@code description}), per user. The blob is
 * extensible; the hub parses it when resolving the ref.
 */
@Service
public class SavedMessageService {

    private final SavedMessageRepository repo;
    private final AliasService aliases;
    private final ObjectMapper mapper = new ObjectMapper();

    public SavedMessageService(SavedMessageRepository repo, AliasService aliases) {
        this.repo = repo;
        this.aliases = aliases;
    }

    public Optional<SavedMessage> find(Long userId, String name) {
        return repo.findByNameAndUserId(name, userId);
    }

    /** Create from the request body { name, moduleAlias, rootType, fixups[], description }. */
    public SavedMessage create(Long userId, Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.contains(":")) throw new IllegalArgumentException("message name must not contain ':'");
        String moduleAlias = str(body.get("moduleAlias"));
        if (moduleAlias == null || moduleAlias.isBlank()) throw new IllegalArgumentException("moduleAlias is required");
        String rootType = str(body.get("rootType"));
        if (rootType == null || rootType.isBlank()) throw new IllegalArgumentException("rootType is required");
        if (repo.existsByNameAndUserId(name, userId))
            throw new IllegalArgumentException("message already exists: " + name);

        ObjectNode data = mapper.createObjectNode();
        data.put("moduleAlias", moduleAlias);
        data.put("rootType", rootType);
        if (str(body.get("description")) != null) data.put("description", str(body.get("description")));
        try {
            Object fixups = body.get("fixups");
            data.set("fixups", fixups == null ? mapper.createArrayNode() : mapper.valueToTree(fixups));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid fixups: " + e.getMessage());
        }

        SavedMessage m = new SavedMessage();
        m.setName(name);
        m.setUserId(userId);
        m.setData(data.toString());
        return repo.save(m);
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SavedMessage m : repo.findByUserId(userId)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", m.getName());
            JsonNode d = parse(m.getData());
            e.put("moduleAlias", text(d, "moduleAlias"));
            e.put("rootType", text(d, "rootType"));
            e.put("description", text(d, "description"));
            e.put("fixups", d.has("fixups") ? d.get("fixups") : mapper.createArrayNode());
            out.add(e);
        }
        return out;
    }

    public boolean delete(Long userId, String name) {
        Optional<SavedMessage> m = repo.findByNameAndUserId(name, userId);
        if (m.isEmpty()) return false;
        repo.delete(m.get());
        return true;
    }

    private JsonNode parse(String json) {
        try {
            return json == null || json.isBlank() ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
}
