package main.config;

import main.A;
import main.monitoring.TelegramCenter;
import main.startup.MessageLoader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppLifecycle {

    private final ConfigurationManager config;
    private final TelegramCenter telegramCenter;
    private final MessageLoader messageLoader;

    public AppLifecycle(ConfigurationManager config, TelegramCenter telegramCenter, MessageLoader messageLoader) {
        this.config = config;
        this.telegramCenter = telegramCenter;
        this.messageLoader = messageLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        A.p("Initializing monitoring system...");
        config.printConfiguration();
        messageLoader.loadAll();

        if (config.isTelegramEnabled()) {
            telegramCenter.notifyServerStatus("Starting", "V2X.tools server started");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            A.p("Shutting down monitoring system...");
            telegramCenter.notifyServerStatus("Stopping", "V2X.tools server shutting down");
            telegramCenter.shutdown();
        }));

        A.p("Monitoring system initialized");
    }
}
