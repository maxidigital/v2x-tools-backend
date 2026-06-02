package main.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import main.A;
import main.engine.EngineService;
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

    private final EngineService engine;

    public MessageLoader(EngineService engine) {
        this.engine = engine;
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
