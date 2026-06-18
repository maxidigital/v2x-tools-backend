package main.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The hub's only way to reach the engine: HTTP. Mirrors the engine's content-addressed contract:
 * load(tree+metadata)→engineId; convert/generate by {@code X-Engine-Id}. No wind types cross this.
 * The hub↔engine link is always text (EngineResult JSON); the hub renders binary to the end user.
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

    /** Load tree + metadata; returns the engineId the engine assigns (hash of the content). */
    public String load(String treeJson, String metadataJson) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.set("tree", mapper.readTree(treeJson));
            body.set("metadata", metadataJson == null || metadataJson.isBlank()
                    ? mapper.createObjectNode() : mapper.readTree(metadataJson));
            HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/engine/load"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = send(req);
            if (resp.statusCode() != 200)
                throw new RuntimeException("engine load failed: HTTP " + resp.statusCode() + " " + resp.body());
            return mapper.readTree(resp.body()).get("engineId").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("engine load failed: " + e.getMessage(), e);
        }
    }

    /** Convert a payload using the loaded definition (engineId). Output is text (hex/json/xml). */
    public EngineResult convert(String engineId, byte[] payload, boolean binaryIn, String from, String to) {
        String url = base() + "/engine/convert?from=" + enc(from) + "&to=" + enc(to);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("X-Engine-Id", engineId)
                .header("Content-Type", binaryIn ? "application/octet-stream" : "text/plain")
                .header("Accept", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        return parse(send(req));
    }

    /** Generate a sample. {@code optionsJson} = GenerateOptions{format,minimal,size,overrides} (forwarded). */
    public EngineResult generate(String engineId, String optionsJson) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/engine/generate"))
                .timeout(Duration.ofSeconds(20))
                .header("X-Engine-Id", engineId)
                .header("Content-Type", "application/json")
                .header("Accept", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(
                        optionsJson == null || optionsJson.isBlank() ? "{}" : optionsJson, StandardCharsets.UTF_8))
                .build();
        return parse(send(req));
    }

    public void evict(String engineId) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/engine/loaded/" + enc(engineId)))
                .timeout(Duration.ofSeconds(10)).DELETE().build();
        send(req);
    }

    private EngineResult parse(HttpResponse<String> resp) {
        try {
            return mapper.readValue(resp.body(), EngineResult.class);
        } catch (Exception e) {
            throw new RuntimeException("invalid engine response: " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("engine request failed: " + e.getMessage(), e);
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
}
