package main.repo;

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
 * HTTP client for the module repo (the ASN.1 authority). The repo is machine-keyed: trees are fetched
 * by the derived {@code moduleId} only; modules are listed (moduleId+oid+name) and their types browsed.
 */
@Component
public class RepoClient {

    @Value("${wind.repo.url:http://localhost:8081}")
    private String repoUrl;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String base() {
        String u = repoUrl.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!u.contains("://")) u = (u.contains(".railway.internal") ? "http://" : "https://") + u;
        return u;
    }

    /** Digested tree for (moduleId, type). Throws DefinitionNotFoundException on 404. */
    public String tree(String moduleId, String type) {
        String url = base() + "/api/modules/tree?moduleId=" + enc(moduleId) + "&type=" + enc(type);
        HttpResponse<String> resp = get(url);
        if (resp.statusCode() == 404)
            throw new DefinitionNotFoundException("no tree for " + moduleId + ":" + type + " — " + resp.body());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + " fetching tree " + moduleId + ":" + type);
        return resp.body();
    }

    /** Modules owned by a user (userId 0 = public): [{ id, moduleId, oid, uploadedAt }]. */
    public List<JsonNode> modulesByUser(Long userId) {
        HttpResponse<String> resp = get(base() + "/api/modules/by-user?userId=" + userId);
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + " listing modules");
        List<JsonNode> out = new ArrayList<>();
        try {
            mapper.readTree(resp.body()).forEach(out::add);
        } catch (Exception e) {
            throw new RuntimeException("invalid modules response: " + e.getMessage(), e);
        }
        return out;
    }

    /** Type names declared in a module (by derived moduleId). */
    public List<String> types(String moduleId) {
        HttpResponse<String> resp = get(base() + "/api/modules/types?moduleId=" + enc(moduleId));
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + " listing types for " + moduleId);
        List<String> out = new ArrayList<>();
        try {
            mapper.readTree(resp.body()).forEach(n -> out.add(n.asText()));
        } catch (Exception e) {
            throw new RuntimeException("invalid types response: " + e.getMessage(), e);
        }
        return out;
    }

    /** The module with the given OID (detail incl. moduleId), or null if not found. */
    public JsonNode moduleByOid(String oid) {
        HttpResponse<String> resp = get(base() + "/api/modules/by-oid?oid=" + enc(oid));
        if (resp.statusCode() == 404) return null;
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + " fetching module by oid");
        try {
            return mapper.readTree(resp.body());
        } catch (Exception e) {
            throw new RuntimeException("invalid by-oid response: " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("repo request failed for " + url + ": " + e.getMessage(), e);
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
}
