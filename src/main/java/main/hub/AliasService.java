package main.hub;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import main.hub.entity.ModuleAlias;
import main.hub.repo.ModuleAliasRepository;
import main.repo.RepoClient;
import org.springframework.stereotype.Service;

/** Module aliases: resolve a name → repo moduleId; CRUD for personal aliases (user>0). */
@Service
public class AliasService {

    private final ModuleAliasRepository repo;
    private final RepoClient repoClient;

    public AliasService(ModuleAliasRepository repo, RepoClient repoClient) {
        this.repo = repo;
        this.repoClient = repoClient;
    }

    /** alias → moduleId: the user's alias first, then the public (user 0); else treat it as a literal moduleId. */
    public String resolveModuleId(Long userId, String aliasOrModuleId) {
        return repo.findByAliasAndUserId(aliasOrModuleId, userId)
                .or(() -> repo.findByAliasAndUserId(aliasOrModuleId, 0L))
                .map(ModuleAlias::getModuleId)
                .orElse(aliasOrModuleId);
    }

    /** Resolve the module identified by OID to its repo moduleId. */
    public String moduleIdForOid(String oid) {
        JsonNode m = repoClient.moduleByOid(oid);
        if (m == null || m.get("moduleId") == null || m.get("moduleId").asText().isBlank())
            throw new IllegalArgumentException("no module for oid: " + oid);
        return m.get("moduleId").asText();
    }

    public ModuleAlias create(Long userId, String alias, String oid) {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("alias is required");
        if (alias.contains(":")) throw new IllegalArgumentException("alias must not contain ':'");
        String moduleId = moduleIdForOid(oid);
        if (repo.existsByAliasAndUserId(alias, userId))
            throw new IllegalArgumentException("alias already exists: " + alias);
        return repo.save(new ModuleAlias(alias, moduleId, userId));
    }

    /** The user's aliases plus the public (user 0) defaults. */
    public List<Map<String, Object>> list(Long userId) {
        List<ModuleAlias> all = new ArrayList<>(repo.findByUserId(0L));
        if (userId != null && userId != 0L) all.addAll(repo.findByUserId(userId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ModuleAlias a : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("alias", a.getAlias());
            m.put("moduleId", a.getModuleId());
            m.put("scope", a.getUserId() == 0L ? "default" : "personal");
            out.add(m);
        }
        return out;
    }

    public boolean delete(Long userId, String alias) {
        Optional<ModuleAlias> a = repo.findByAliasAndUserId(alias, userId);
        if (a.isEmpty()) return false;
        repo.delete(a.get());
        return true;
    }
}
