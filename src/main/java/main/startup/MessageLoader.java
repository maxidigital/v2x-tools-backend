package main.startup;

import a.MessageId;
import a.WindId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import wind_parser.WindParser;
import wind_parser.WindParserException;
import wind_parser.i.WindParserModule;
import wind_parser.i.WindParserProject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class MessageLoader {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public void loadAll() {
        A.p("Loading V2X message definitions from repo: " + repoUrl);

        List<Map<String, Object>> modules;
        try {
            modules = fetchAllMessageModules();
        } catch (Exception e) {
            A.p("WARNING: Could not reach wind repo: " + e.getMessage());
            return;
        }

        if (modules.isEmpty()) {
            A.p("No V2X message modules found in repo.");
            return;
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("wind_repo_");
        } catch (IOException e) {
            A.p("ERROR: Could not create temp directory: " + e.getMessage());
            return;
        }

        RemoteWindRepo remoteRepo = new RemoteWindRepo(repoUrl, tempDir);

        for (Map<String, Object> mod : modules) {
            String group = (String) mod.get("group");
            String id = (String) mod.get("id");
            String version = (String) mod.get("version");
            String moduleName = (String) mod.get("moduleName");
            String messageIdStr = (String) mod.get("messageId");
            String protocolVersionStr = (String) mod.get("protocolVersion");

            try {
                int messageId = Integer.parseInt(messageIdStr);
                int protocolVersion = protocolVersionStr.isEmpty() ? 1 : Integer.parseInt(protocolVersionStr);
                MessageId mid = MessageId.create(messageId, protocolVersion);

                WindId windId = WindId.create(group, id, version);
                WindParserProject project = new WindParser(remoteRepo).parse(windId);

                WindParserModule module = project.getModules().stream()
                        .filter(m -> m.getModuleId().getName().equals(moduleName))
                        .findFirst()
                        .orElse(null);

                if (module == null) {
                    A.p("WARNING: Module " + moduleName + " not found in parsed project");
                    continue;
                }

                MessagesApp.getInstance().registerMessage(mid, () -> WindGeneric.build(module));
                A.p("Registered: " + moduleName + " (messageId=" + messageId + ", protocolVersion=" + protocolVersion + ")");

            } catch (WindParserException e) {
                A.p("ERROR parsing " + group + "/" + id + "/" + version + ": " + e.getMessage());
            } catch (Exception e) {
                A.p("ERROR loading module " + moduleName + ": " + e.getMessage());
            }
        }

        A.p("Message loading complete. " + modules.size() + " modules processed.");
    }

    private List<Map<String, Object>> fetchAllMessageModules() throws Exception {
        String url = (repoUrl.endsWith("/") ? repoUrl : repoUrl + "/") + "api/repo/modules/all";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        return mapper.readValue(response.body(), new TypeReference<>() {});
    }
}
