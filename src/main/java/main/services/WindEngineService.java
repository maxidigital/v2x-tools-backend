package main.services;

import a.MessageId;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import main.repo.RepoClient;
import wind_parser.WindParser;
import wind_parser.i.WindParserProject;

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

    /**
     * Loads the given aliases into the user's engine (creates it if needed).
     * Each alias is parsed once; the supplier is instantaneous on createEmptyMessage().
     * Returns the list of aliases that were successfully registered.
     */
    public List<String> load(Long userId, List<String> aliases) {
        MessagesApp engine = getOrCreate(userId);
        RepoClient repoClient = new RepoClient(repoUrl, userId);
        List<String> registered = new ArrayList<>();

        for (String alias : aliases) {
            try {
                Map<String, Object> meta = repoClient.getModuleMeta(alias);
                String mainType     = (String)  meta.get("mainType");
                int messageId       = (Integer) meta.get("messageId");
                int protocolVersion = (Integer) meta.get("protocolVersion");

                WindParserProject project = new WindParser().parseByAlias(alias, repoClient);
                MessageId mid = MessageId.create(messageId, protocolVersion);
                engine.registerMessage(mid, () -> WindGeneric.build(project, mainType));

                A.p("WindEngine[%d] loaded: %s / %s (msgId=%d, prot=%d)",
                        userId, alias, mainType, messageId, protocolVersion);
                registered.add(alias);
                loadedAliases.computeIfAbsent(userId, id -> new LinkedHashSet<>()).add(alias);
            } catch (Exception e) {
                A.p("WindEngine[%d] ERROR loading %s: %s", userId, alias, e.getMessage());
            }
        }
        return registered;
    }
}
