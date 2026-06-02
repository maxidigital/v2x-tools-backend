package main.loader;

import a.MessageId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import main.A;
import main.engine.EngineService;
import main.repo.DefinitionNotFoundException;
import main.repo.RepoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves message definitions from the repo and feeds them to the passive EngineService.
 * This is the part that is NOT dumb: it knows the repo. The engine never talks to the repo.
 */
@Service
public class MessageLoader {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    /** How long an "absent in repo" result is remembered before re-asking (ms). */
    private static final long NEGATIVE_TTL_MS = 60_000;

    private final EngineService engine;
    private final Map<String, Long> negative = new ConcurrentHashMap<>();

    public MessageLoader(EngineService engine) {
        this.engine = engine;
    }

    /**
     * Lazy cache-miss: ensures the user's engine has the definition for this message,
     * resolving it from the repo by messageId. Throws {@link MessageNotAvailableException}
     * if the repo has none (and remembers the absence for a short TTL to avoid re-asking).
     */
    public void ensureLoaded(Long userId, MessageId mid) {
        if (engine.isLoaded(userId, mid))
            return;

        String nk = userId + ":" + mid.getId() + ":" + mid.getProtocolVersion();
        long now = System.currentTimeMillis();
        Long expiry = negative.get(nk);
        if (expiry != null && expiry > now)
            throw new MessageNotAvailableException(mid);

        RepoClient repo = new RepoClient(repoUrl, userId);
        try {
            engine.load(userId, repo.getDefinitionByMessage(mid.getId(), mid.getProtocolVersion()));
            negative.remove(nk);
        } catch (DefinitionNotFoundException e) {
            negative.put(nk, now + NEGATIVE_TTL_MS);
            throw new MessageNotAvailableException(mid);
        }
    }

    public record LoadResult(List<String> registered, List<String> skipped, Map<String, String> errors) {}

    /** Fetches each alias's digested definition from the repo and pushes it into the engine. */
    public LoadResult load(Long userId, List<String> aliases) {
        RepoClient repo = new RepoClient(repoUrl, userId);
        List<String> registered = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();

        for (String alias : aliases) {
            if (engine.isLoaded(userId, alias)) {
                skipped.add(alias);
                continue;
            }
            try {
                engine.load(userId, repo.getDefinition(alias));
                registered.add(alias);
            } catch (Exception e) {
                A.p("Loader[%d] ERROR loading %s: %s", userId, alias, e.getMessage());
                errors.put(alias, e.getMessage());
            }
        }
        return new LoadResult(registered, skipped, errors);
    }
}
