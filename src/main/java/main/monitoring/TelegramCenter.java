package main.monitoring;

import main.A;
import main.config.ConfigurationManager;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class TelegramCenter {

    private final String notificationServiceUrl;
    private final NotificationFilter filter;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public TelegramCenter(ConfigurationManager config) {
        this.notificationServiceUrl = config.getNotificationServiceUrl();
        this.filter = new NotificationFilter();
        A.p("TelegramCenter initialized → " + notificationServiceUrl);
    }

    public void notifyApiUsage(String endpoint, String clientIP, String data, long responseTimeMs) {
        if (!filter.shouldNotifyApiUsage(endpoint, clientIP)) return;
        String truncated = data.length() > 100 ? data.substring(0, 100) + "..." : data;
        send("INFO", endpoint + " | " + clientIP + " | " + responseTimeMs + "ms | " + truncated);
    }

    public void notifyError(String error, String context, String clientIP) {
        send("ERROR", context + " | " + clientIP + " | " + error);
    }

    public void notifyServerStatus(String status, String details) {
        send("INFO", status + " — " + details);
    }

    public void sendNotification(String message, NotificationType type) {
        send(levelFor(type), message);
    }

    private void send(String level, String message) {
        executor.submit(() -> {
            try {
                URL url = new URL(notificationServiceUrl + "/notify");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String body = String.format(
                    "{\"app\":\"v2x.tools\",\"level\":\"%s\",\"message\":\"%s\"}",
                    level,
                    message.replace("\"", "'").replace("\n", " ")
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }

                int status = conn.getResponseCode();
                if (status != 200) A.p("Notification service returned: " + status);
            } catch (Exception e) {
                A.p("Failed to send notification: " + e.getMessage());
            }
        });
    }

    private String levelFor(NotificationType type) {
        switch (type) {
            case ERROR:    return "ERROR";
            case SECURITY: return "WARN";
            default:       return "INFO";
        }
    }

    public String getStats() {
        return "Notification service: " + notificationServiceUrl;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
