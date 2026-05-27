package main.services;

import a.MessageId;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import repoClient.RepoClient;
import wind_parser.WindParser;
import wind_parser.i.WindParserProject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-user MessagesApp instances.
 * Each user gets an independent engine loaded with the configured message aliases.
 * Engines are created lazily on first access and cached in memory.
 */
@Service
public class WindEngineService {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    // Aliases to load for each user — hardcoded for now
    private static final List<String> ALIASES = List.of(
        "cam_v2", "denm_v2", "mapem_v2", "spatem_v2", "srem_v2", "ssem_v2", "vam_v3"
    );

    private final Map<Long, MessagesApp> engines = new ConcurrentHashMap<>();

    /**
     * Returns the engine for the given userId, creating it lazily if it doesn't exist yet.
     */
    public MessagesApp getOrCreate(Long userId) {
        return engines.computeIfAbsent(userId, this::createEngine);
    }

    /**
     * Evicts the engine for the given userId (forces reload on next getOrCreate).
     */
    public void evict(Long userId) {
        engines.remove(userId);
    }

    private MessagesApp createEngine(Long userId) {
        A.p("WindEngine: creating engine for userId=%d", userId);
        MessagesApp engine = MessagesApp.create();
        RepoClient repoClient = new RepoClient(repoUrl, userId);
        int registered = 0;

        for (String alias : ALIASES) {
            try {
                Map<String, Object> meta = repoClient.getModuleMeta(alias);
                String mainType     = (String)  meta.get("mainType");
                int messageId       = (Integer) meta.get("messageId");
                int protocolVersion = (Integer) meta.get("protocolVersion");

                WindParserProject project = new WindParser().parseByAlias(alias, repoClient);
                MessageId mid = MessageId.create(messageId, protocolVersion);
                engine.registerMessage(mid, () -> WindGeneric.build(project, mainType));

                A.p("WindEngine[%d] registered: %s / %s (msgId=%d, prot=%d)",
                        userId, alias, mainType, messageId, protocolVersion);
                registered++;
            } catch (Exception e) {
                A.p("WindEngine[%d] ERROR loading %s: %s", userId, alias, e.getMessage());
            }
        }

        A.p("WindEngine[%d] ready: %d/%d messages loaded", userId, registered, ALIASES.size());
        return engine;
    }
}
