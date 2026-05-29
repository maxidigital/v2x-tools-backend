package main.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import a.WindId;
import wind_parser.i.Asn1Repo;
import wind_parser.i.Asn1RepoException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client that implements Asn1Repo against the v2x-tools-repo REST API.
 *
 * userId identifies whose private modules to load.
 * Alias-based lookups (getByAlias, getWindIdByAlias) pass userId — alias is user-defined
 * and can collide between users; no fallback (strict lookup).
 * OID-based lookups (getByNameAndOid, getWindIdByNameAndOid) do NOT pass userId —
 * OIDs are globally unique by ASN.1 standard.
 */
public class RepoClient implements Asn1Repo {

    private final String baseUrl;
    private final Long userId;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String, Object>> oidCache = new HashMap<>();

    /** Public modules only (userId=0). */
    public RepoClient(String baseUrl) {
        this(baseUrl, 0L);
    }

    /** Modules for the given userId. */
    public RepoClient(String baseUrl, Long userId) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.userId = userId;
    }

    /** Alias lookup — tries userId-specific, falls back to public on 404. */
    @Override
    public String getByAlias(String alias) throws Asn1RepoException {
        Object content = getAliasJson(alias).get("content");
        if (content == null)
            throw new Asn1RepoException("Module with alias '" + alias + "' has no content");
        return (String) content;
    }

    /** OID lookup — no userId (OIDs are globally unique). Caches the full JSON for reuse. */
    @Override
    public String getByNameAndOid(String name, String oid) throws Asn1RepoException {
        Object content = getOidJson(oid).get("content");
        if (content == null)
            throw new Asn1RepoException("Module '" + name + "' has no content");
        return (String) content;
    }

    /**
     * WindId for alias lookups: the alias itself IS the identity — no HTTP call needed.
     * groupId and version are artifacts of the old desktop workflow and are not used here.
     */
    @Override
    public WindId getWindIdByAlias(String alias) throws Asn1RepoException {
        return WindId.create("", alias, "1.0");
    }

    /**
     * WindId for OID lookups: extract the alias from the /by-oid response (already cached).
     * No extra HTTP call needed.
     */
    @Override
    public WindId getWindIdByNameAndOid(String name, String oid) throws Asn1RepoException {
        String alias = (String) getOidJson(oid).getOrDefault("alias", "");
        return WindId.create("", alias, "1.0");
    }

    /** Returns full module metadata (alias, mainType, messageId, protocolVersion, ...). */
    public Map<String, Object> getModuleMeta(String alias) throws Asn1RepoException {
        return getAliasJson(alias);
    }

    /** Fetches and caches the full module JSON by OID. OIDs are globally unique — no userId needed. */
    private Map<String, Object> getOidJson(String oid) throws Asn1RepoException {
        Map<String, Object> cached = oidCache.get(oid);
        if (cached != null) return cached;
        Map<String, Object> result = getJson("api/modules/by-oid?oid=" + encode(oid));
        oidCache.put(oid, result);
        return result;
    }

    /** Appends &userId when userId > 0. */
    private String aliasPath(String path) {
        if (userId != null && userId > 0)
            return path + "&userId=" + userId;
        return path;
    }

    /** Low-level HTTP GET — returns the response so callers can inspect status code. */
    private HttpResponse<String> sendGet(String url) throws Asn1RepoException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new Asn1RepoException("Request failed for " + url + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseJson(String body) throws Asn1RepoException {
        try {
            return mapper.readValue(body, new TypeReference<>() {});
        } catch (IOException e) {
            throw new Asn1RepoException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getJson(String path) throws Asn1RepoException {
        String url = baseUrl + path;
        HttpResponse<String> response = sendGet(url);
        if (response.statusCode() == 404)
            throw new Asn1RepoException("Not found: " + url);
        if (response.statusCode() != 200)
            throw new Asn1RepoException("HTTP " + response.statusCode() + " from " + url);
        return parseJson(response.body());
    }

    /**
     * Alias lookup with fallback: tries userId-specific first, falls back to public (userId=0) on 404.
     * OID-based lookups don't use this — OIDs are globally unique, no fallback needed.
     */
    private Map<String, Object> getAliasJson(String alias) throws Asn1RepoException {
        if (userId != null && userId > 0) {
            String url = baseUrl + aliasPath("api/modules/by-alias?alias=" + encode(alias));
            HttpResponse<String> resp = sendGet(url);
            if (resp.statusCode() == 200)
                return parseJson(resp.body());
            if (resp.statusCode() != 404)
                throw new Asn1RepoException("HTTP " + resp.statusCode() + " from " + url);
            // 404 → fall through to public
        }
        return getJson("api/modules/by-alias?alias=" + encode(alias));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
