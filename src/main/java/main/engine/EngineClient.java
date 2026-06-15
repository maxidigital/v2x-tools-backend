package main.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The backend's only way to reach the engine: HTTP. Mirrors the engine's contract
 * (load / convert / generate / messages / evict). No wind types cross this boundary.
 */
@Component
public class EngineClient {

    @Value("${wind.engine.url:http://localhost:8090}")
    private String engineUrl;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String base() {
        return engineUrl.endsWith("/") ? engineUrl.substring(0, engineUrl.length() - 1) : engineUrl;
    }

    /** Convert a payload; returns the engine's typed result. */
    public EngineResult convert(Long userId, String payload, String from, String to, String messageId) {
        String url = base() + "/engine/convert?from=" + enc(from) + "&to=" + enc(to);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("X-User-Id", uid(userId))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        if (messageId != null && !messageId.isBlank())
            b.header("X-Message-Id", messageId);
        HttpResponse<String> resp = send(b.build());
        try {
            return mapper.readValue(resp.body(), EngineResult.class);
        } catch (Exception e) {
            throw new RuntimeException("invalid engine response: " + e.getMessage(), e);
        }
    }

    /** Push a digested definition into the engine for a user. */
    public void load(Long userId, String definitionJson) {
        String url = base() + "/engine/load";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("X-User-Id", uid(userId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(definitionJson, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = send(req);
        if (resp.statusCode() != 200)
            throw new RuntimeException("engine load failed: HTTP " + resp.statusCode());
    }

    /** Generate a sample payload. Typed result, like convert: ok / notFound / decodeError. */
    public EngineResult generate(Long userId, String mid, String format, boolean minimal) {
        String url = base() + "/engine/generate?mid=" + enc(mid) + "&format=" + enc(format) + "&minimal=" + minimal;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("X-User-Id", uid(userId)).GET().build();
        HttpResponse<String> resp = send(req);
        try {
            return mapper.readValue(resp.body(), EngineResult.class);
        } catch (Exception e) {
            throw new RuntimeException("invalid engine response: " + e.getMessage(), e);
        }
    }

    /** List the messages loaded for a user in the engine. */
    public List<String> messages(Long userId) {
        String url = base() + "/engine/messages";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("X-User-Id", uid(userId)).GET().build();
        List<String> out = new ArrayList<>();
        try {
            JsonNode loaded = mapper.readTree(send(req).body()).get("loaded");
            if (loaded != null)
                loaded.forEach(n -> out.add(n.asText()));
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Evict the user's engine. */
    public void evict(Long userId) {
        String url = base() + "/engine/messages";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("X-User-Id", uid(userId)).DELETE().build();
        send(req);
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("engine request failed: " + e.getMessage(), e);
        }
    }

    private static String uid(Long userId) { return String.valueOf(userId == null ? 0L : userId); }
    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
}
