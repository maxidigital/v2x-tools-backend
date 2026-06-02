package main.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import main.A;
import main.engine.EngineClient;
import main.repo.DefinitionNotFoundException;
import main.repo.RepoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves message definitions from the repo and pushes them into the engine (over HTTP).
 * This is the bridge repo↔engine; it touches no wind types.
 */
@Service
public class MessageLoader {

    /** How long an "absent in repo" result is remembered before re-asking (ms). */
    private static final long NEGATIVE_TTL_MS = 60_000;

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    private final EngineClient engine;
    private final Map<String, Long> negative = new ConcurrentHashMap<>();

    public MessageLoader(EngineClient engine) {
        this.engine = engine;
    }

    public record LoadResult(List<String> registered, List<String> skipped, Map<String, String> errors) {}

    /** Fetch each alias's definition from the repo and push it into the engine. */
    public LoadResult load(Long userId, List<String> aliases) {
        RepoClient repo = new RepoClient(repoUrl, userId);
        List<String> registered = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (String alias : aliases) {
            try {
                engine.load(userId, repo.getDefinition(alias));
                registered.add(alias);
            } catch (Exception e) {
                A.p("Loader[%d] ERROR loading %s: %s", userId, alias, e.getMessage());
                errors.put(alias, e.getMessage());
            }
        }
        return new LoadResult(registered, List.of(), errors);
    }

    /**
     * Lazy cache-miss: resolve the message by id from the repo and push it into the engine.
     * Throws {@link MessageNotAvailableException} if the repo has none (remembered for a short TTL).
     */
    public void ensureLoaded(Long userId, int messageId, int protocolVersion) {
        String nk = userId + ":" + messageId + ":" + protocolVersion;
        long now = System.currentTimeMillis();
        Long expiry = negative.get(nk);
        if (expiry != null && expiry > now)
            throw new MessageNotAvailableException(messageId, protocolVersion);

        RepoClient repo = new RepoClient(repoUrl, userId);
        try {
            engine.load(userId, repo.getDefinitionByMessage(messageId, protocolVersion));
            negative.remove(nk);
        } catch (DefinitionNotFoundException e) {
            negative.put(nk, now + NEGATIVE_TTL_MS);
            throw new MessageNotAvailableException(messageId, protocolVersion);
        }
    }
}
