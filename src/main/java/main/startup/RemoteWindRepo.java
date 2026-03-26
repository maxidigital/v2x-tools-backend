package main.startup;

import a.WindId;
import de.dlr.ts.v2x.wind_repo.WindRepo;
import i.WindException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WindRepo implementation that downloads files from v2x-tools-repo REST API
 * into a local temp directory on demand.
 */
public class RemoteWindRepo implements WindRepo {

    private final String repoUrl;
    private final Path tempDir;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public RemoteWindRepo(String repoUrl, Path tempDir) {
        this.repoUrl = repoUrl.endsWith("/") ? repoUrl : repoUrl + "/";
        this.tempDir = tempDir;
    }

    @Override
    public String getRepoPath() {
        return tempDir.toString();
    }

    @Override
    public String getProjectFullPath(WindId windId) throws WindException {
        String group = windId.getGroup();
        String id = windId.getId();
        String version = windId.isComplete() ? windId.getVersion() : resolveLatestVersion(group, id);

        Path projectDir = tempDir.resolve(group).resolve(id).resolve(version);

        if (!Files.exists(projectDir)) {
            downloadProject(group, id, version, projectDir);
        }

        return projectDir.toString() + "/";
    }

    private String resolveLatestVersion(String group, String id) throws WindException {
        try {
            String url = repoUrl + "api/repo/" + group + "/" + id;
            String json = get(url);
            List<Map<String, Object>> versions = mapper.readValue(json, new TypeReference<>() {});
            if (versions.isEmpty())
                throw new WindException("No versions found for %s/%s", group, id);
            return (String) versions.get(versions.size() - 1).get("version");
        } catch (IOException e) {
            throw new WindException("Failed to resolve version for %s/%s: %s", group, id, e.getMessage());
        }
    }

    private void downloadProject(String group, String id, String version, Path targetDir) throws WindException {
        try {
            Files.createDirectories(targetDir);

            String url = repoUrl + "api/repo/" + group + "/" + id + "/" + version;
            String json = get(url);
            List<Map<String, Object>> files = mapper.readValue(json, new TypeReference<>() {});

            for (Map<String, Object> file : files) {
                String filename = (String) file.get("filename");
                String fileUrl = url + "/" + filename;
                String content = get(fileUrl);
                Files.writeString(targetDir.resolve(filename), content);
            }
        } catch (IOException e) {
            throw new WindException("Failed to download project %s/%s/%s: %s", group, id, version, e.getMessage());
        }
    }

    private String get(String url) throws WindException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new WindException("HTTP %d for %s", response.statusCode(), url);
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new WindException("Request failed for %s: %s", url, e.getMessage());
        }
    }
}
