package main.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import main.hub.entity.SavedMessage;
import main.hub.repo.SavedMessageRepository;
import org.springframework.stereotype.Service;

/** Saved messages: a named (moduleId, typeName) + sticky fixups + description, per user. */
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

    /** Create from the request body { name, module(moduleId|alias), type, fixups[], description }. */
    public SavedMessage create(Long userId, Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.contains(":")) throw new IllegalArgumentException("message name must not contain ':'");
        String type = str(body.get("type"));
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type is required");
        String moduleRef = str(body.get("module"));
        if (moduleRef == null || moduleRef.isBlank()) throw new IllegalArgumentException("module is required");
        String moduleId = aliases.resolveModuleId(userId, moduleRef);
        if (repo.existsByNameAndUserId(name, userId))
            throw new IllegalArgumentException("message already exists: " + name);

        SavedMessage m = new SavedMessage();
        m.setName(name);
        m.setUserId(userId);
        m.setModuleId(moduleId);
        m.setTypeName(type);
        m.setDescription(str(body.get("description")));
        try {
            Object fixups = body.get("fixups");
            m.setFixups(fixups == null ? "[]" : mapper.writeValueAsString(fixups));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid fixups: " + e.getMessage());
        }
        return repo.save(m);
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SavedMessage m : repo.findByUserId(userId)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", m.getName());
            e.put("moduleId", m.getModuleId());
            e.put("type", m.getTypeName());
            e.put("description", m.getDescription());
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

    private static String str(Object o) { return o == null ? null : o.toString(); }
}
