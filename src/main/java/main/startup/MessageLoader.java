package main.startup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generator.Wind;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repoClient.RepoClient;
import wind_parser.WindParserException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
public class MessageLoader {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public void loadAll() {
        A.p("Loading V2X message definitions from repo: " + repoUrl);

        List<Map<String, Object>> messages = fetchMessages();
        if (messages == null) return;

        RepoClient repoClient = new RepoClient(repoUrl);
        Wind wind = new Wind();
        int registered = 0;

        for (Map<String, Object> m : messages) {
            String alias           = (String)  m.get("alias");
            String mainType        = (String)  m.get("mainType");
            int    messageId       = (Integer) m.get("messageId");
            int    protocolVersion = (Integer) m.get("protocolVersion");

            try {
                wind.buildGeneric(alias, repoClient, mainType);
                a.MessageId mid = a.MessageId.create(messageId, protocolVersion);
                MessagesApp.getInstance().registerMessage(mid, () -> {
                    try {
                        return wind.buildGeneric(alias, repoClient, mainType);
                    } catch (WindParserException e) {
                        throw new RuntimeException(e);
                    }
                });
                A.p("Registered: " + alias + "/" + mainType + " (messageId=" + messageId + ", protocolVersion=" + protocolVersion + ")");
                registered++;
            } catch (Exception e) {
                A.p("ERROR loading " + alias + ": " + e.getMessage());
            }
        }

        A.p("Message loading complete. " + registered + "/" + messages.size() + " modules registered.");
    }

    private List<Map<String, Object>> fetchMessages() {
        String url = repoUrl + (repoUrl.endsWith("/") ? "" : "/") + "api/modules/messages";
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                A.p("ERROR fetching messages from repo: HTTP " + response.statusCode());
                return null;
            }
            return mapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            A.p("ERROR fetching messages from repo: " + e.getMessage());
            return null;
        }
    }
}
