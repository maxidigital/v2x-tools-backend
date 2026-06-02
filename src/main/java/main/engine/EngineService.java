package main.engine;

import a.MessageId;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import de.dlr.ts.v2x.wind_model.MessageDefinition;
import de.dlr.ts.v2x.wind_model.WindMessageCodec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import main.A;
import org.springframework.stereotype.Service;
import wind_parser.i.ParserSequence;

/**
 * Passive message engine: a per-user store of built message definitions over a MessagesApp.
 *
 * It only knows how to register a pushed definition and hand out the MessagesApp for
 * conversion. It does NOT know about the repo, HTTP, auth or where definitions come from —
 * something else (the loader) feeds it. Dependency rule for this package: no repo / no HTTP
 * out. Deps: wind_model (codec) + wind_generic (builder) + wind.lib (MessagesApp).
 */
@Service
public class EngineService {

    private final WindMessageCodec codec = new WindMessageCodec();
    private final Map<Long, MessagesApp> engines = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> loaded = new ConcurrentHashMap<>();

    /** The per-user MessagesApp, created empty on first use. */
    public MessagesApp getOrCreate(Long userId) {
        return engines.computeIfAbsent(userId, id -> {
            A.p("Engine: creating empty engine for userId=%d", id);
            return MessagesApp.create();
        });
    }

    public void evict(Long userId) {
        engines.remove(userId);
        loaded.remove(userId);
    }

    public List<String> getAliases(Long userId) {
        return List.copyOf(loaded.getOrDefault(userId, Set.of()));
    }

    public boolean isLoaded(Long userId, String alias) {
        return loaded.getOrDefault(userId, Set.of()).contains(alias);
    }

    /**
     * Registers a pushed message definition (JSON) into the user's engine, keyed by its
     * messageId+protocolVersion (build is lazy on first use). Returns the registered alias.
     */
    public String load(Long userId, String definitionJson) {
        MessageDefinition def = codec.parse(definitionJson);
        ParserSequence root = def.rootSequence();
        MessageId mid = MessageId.create(def.getMessageId(), def.getProtocolVersion());
        getOrCreate(userId).registerMessage(mid, () -> WindGeneric.build(root));
        loaded.computeIfAbsent(userId, id -> new LinkedHashSet<>()).add(def.getAlias());
        A.p("Engine[%d] loaded: %s (msgId=%d, prot=%d)",
                userId, def.getAlias(), def.getMessageId(), def.getProtocolVersion());
        return def.getAlias();
    }
}
