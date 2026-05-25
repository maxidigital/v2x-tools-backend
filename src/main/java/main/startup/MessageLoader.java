package main.startup;

import a.MessageId;
import a.generic.GenericSequence;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generator.Wind;
import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repoClient.RepoClient;
import wind_parser.WindParserException;

import java.util.List;

@Component
public class MessageLoader {

    @Value("${wind.repo.url:https://v2x-tools-repo-production.up.railway.app/}")
    private String repoUrl;

    private static class MessageConfig {
        final String alias;
        final String moduleName;
        final int messageId;
        final int protocolVersion;

        MessageConfig(String alias, String moduleName, int messageId, int protocolVersion) {
            this.alias = alias;
            this.moduleName = moduleName;
            this.messageId = messageId;
            this.protocolVersion = protocolVersion;
        }
    }

    private static final List<MessageConfig> MESSAGES = List.of(
        new MessageConfig("cam_v2", "CAM", 2, 2),
        new MessageConfig("denm_v2", "DecentralizedEnvironmentalNotificationMessage", 1, 2)
    );

    public void loadAll() {
        A.p("Loading V2X message definitions from repo: " + repoUrl);

        RepoClient repoClient = new RepoClient(repoUrl);
        Wind wind = new Wind();
        int registered = 0;

        for (MessageConfig mc : MESSAGES) {
            try {
                GenericSequence gs = wind.buildGeneric(mc.alias, repoClient, mc.moduleName);
                MessageId mid = MessageId.create(mc.messageId, mc.protocolVersion);
                MessagesApp.getInstance().registerMessage(mid, () -> {
                    try {
                        return wind.buildGeneric(mc.alias, repoClient, mc.moduleName);
                    } catch (WindParserException e) {
                        throw new RuntimeException(e);
                    }
                });
                A.p("Registered: " + mc.alias + "/" + mc.moduleName + " (messageId=" + mc.messageId + ", protocolVersion=" + mc.protocolVersion + ")");
                registered++;

            } catch (Exception e) {
                A.p("ERROR loading " + mc.alias + ": " + e.getMessage());
            }
        }

        A.p("Message loading complete. " + registered + "/" + MESSAGES.size() + " modules registered.");
    }
}
