package main.services;

import a.MessageId;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import de.dlr.ts.v2x.wind_model.MessageDefinition;
import de.dlr.ts.v2x.wind_model.WindMessageCodec;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import main.repo.RepoClient;
import wind_parser.i.ParserSequence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-user MessagesApp instances.
 * Each engine starts empty — messages are loaded explicitly via load().
 */
@Service
public class WindEngineService {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    private final WindMessageCodec codec = new WindMessageCodec();

    private final Map<Long, MessagesApp> engines = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> loadedAliases = new ConcurrentHashMap<>();

    /**
     * Returns the engine for the given userId, creating an empty one lazily if needed.
     */
    public MessagesApp getOrCreate(Long userId) {
        return engines.computeIfAbsent(userId, id -> {
            A.p("WindEngine: creating empty engine for userId=%d", id);
            return MessagesApp.create();
        });
    }

    /**
     * Evicts the engine for the given userId.
     */
    public void evict(Long userId) {
        engines.remove(userId);
        loadedAliases.remove(userId);
    }

    public List<String> getAliases(Long userId) {
        return List.copyOf(loadedAliases.getOrDefault(userId, Set.of()));
    }

    public record LoadResult(List<String> registered, List<String> skipped, Map<String, String> errors) {}

    /**
     * Loads the given aliases into the user's engine (creates it if needed).
     * Each alias is parsed once; the supplier is instantaneous on createEmptyMessage().
     */
    public LoadResult load(Long userId, List<String> aliases) {
        MessagesApp engine = getOrCreate(userId);
        RepoClient repoClient = new RepoClient(repoUrl, userId);
        List<String> registered = new ArrayList<>();
        List<String> skipped    = new ArrayList<>();
        Map<String, String> errors = new java.util.LinkedHashMap<>();

        Set<String> already = loadedAliases.computeIfAbsent(userId, id -> new LinkedHashSet<>());

        for (String alias : aliases) {
            if (already.contains(alias)) {
                A.p("WindEngine[%d] skip (already loaded): %s", userId, alias);
                skipped.add(alias);
                continue;
            }
            try {
                // The repo digested the ASN.1; the engine just deserializes + reuses the builder.
                MessageDefinition def = codec.parse(repoClient.getDefinition(alias));
                ParserSequence root = def.rootSequence();
                MessageId mid = MessageId.create(def.getMessageId(), def.getProtocolVersion());
                engine.registerMessage(mid, () -> WindGeneric.build(root));
                A.p("WindEngine[%d] loaded: %s (msgId=%d, prot=%d)",
                        userId, alias, def.getMessageId(), def.getProtocolVersion());
                registered.add(alias);
                already.add(alias);
            } catch (Exception e) {
                A.p("WindEngine[%d] ERROR loading %s: %s", userId, alias, e.getMessage());
                errors.put(alias, e.getMessage());
            }
        }
        return new LoadResult(registered, skipped, errors);
    }
}
