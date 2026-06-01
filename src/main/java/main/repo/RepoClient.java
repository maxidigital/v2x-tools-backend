package main.repo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal HTTP client for the module repo. The engine no longer parses ASN.1, so this
 * only fetches the digested message definition (JSON) the engine consumes.
 *
 * userId scopes the lookup to the user's private modules; alias lookups fall back to
 * public (userId=0) on 404.
 */
public class RepoClient {

    private final String baseUrl;
    private final Long userId;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Public modules only (userId=0). */
    public RepoClient(String baseUrl) {
        this(baseUrl, 0L);
    }

    /** Modules for the given userId. */
    public RepoClient(String baseUrl, Long userId) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.userId = userId;
    }

    /** Fetches the digested message definition (JSON) for an alias, with public fallback. */
    public String getDefinition(String alias) {
        String path = "api/modules/definition?alias=" + encode(alias);
        if (userId != null && userId > 0) {
            HttpResponse<String> resp = sendGet(baseUrl + aliasPath(path));
            if (resp.statusCode() == 200)
                return resp.body();
            if (resp.statusCode() != 404)
                throw new RuntimeException("HTTP " + resp.statusCode() + " fetching definition for " + alias);
            // 404 → fall through to public
        }
        HttpResponse<String> resp = sendGet(baseUrl + path);
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + " fetching definition for " + alias);
        return resp.body();
    }

    private String aliasPath(String path) {
        return (userId != null && userId > 0) ? path + "&userId=" + userId : path;
    }

    private HttpResponse<String> sendGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Request failed for " + url + ": " + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
