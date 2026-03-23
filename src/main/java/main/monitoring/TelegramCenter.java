package main.monitoring;

import main.A;
import main.config.ConfigurationManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class TelegramCenter {

    private final String BOT_TOKEN;
    private final String CHAT_ID;
    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:8081";

    // Rate limiting
    private final ConcurrentHashMap<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> messageCount = new ConcurrentHashMap<>();
    private final long RATE_LIMIT_WINDOW;
    private final int MAX_MESSAGES_PER_WINDOW;

    // Async executor
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // Statistics
    private long totalMessagesSent = 0;
    private long totalMessagesFiltered = 0;
    private final long startTime = System.currentTimeMillis();

    private final NotificationFilter filter;

    public TelegramCenter(ConfigurationManager config) {
        this.BOT_TOKEN = config.getTelegramBotToken();
        this.CHAT_ID = config.getTelegramChatId();
        this.RATE_LIMIT_WINDOW = config.getRateLimitWindowMs();
        this.MAX_MESSAGES_PER_WINDOW = config.getRateLimitPerWindow();
        this.filter = new NotificationFilter();

        A.p("TelegramCenter initialized successfully");
        A.p("Using Notification Service: " + NOTIFICATION_SERVICE_URL);
        A.p("Rate Limit: " + MAX_MESSAGES_PER_WINDOW + " messages per " + (RATE_LIMIT_WINDOW / 1000) + "s");

        testNotificationService();
    }

    public void sendNotification(String message, NotificationType type) {
        executor.submit(() -> {
            try {
                sendNotificationInternal(message, type);
            } catch (Exception e) {
                A.p("Error sending notification: " + e.getMessage());
            }
        });
    }

    public void notifyApiUsage(String endpoint, String clientIP, String data, long responseTimeMs) {
        if (!filter.shouldNotifyApiUsage(endpoint, clientIP)) {
            totalMessagesFiltered++;
            return;
        }
        sendNotification(buildApiUsageMessage(endpoint, clientIP, data, responseTimeMs), NotificationType.API_USAGE);
    }

    public void notifyError(String error, String context, String clientIP) {
        sendNotification(buildErrorMessage(error, context, clientIP), NotificationType.ERROR);
    }

    public void notifyServerStatus(String status, String details) {
        sendNotification(buildStatusMessage(status, details), NotificationType.STATUS);
    }

    private void sendNotificationInternal(String message, NotificationType type) {
        if (!canSendMessage(type.toString())) {
            totalMessagesFiltered++;
            return;
        }
        try {
            String formattedMessage = formatMessage(message, type);
            boolean sent = sendNotificationToService(formattedMessage, type);
            if (sent) {
                totalMessagesSent++;
                updateRateLimit(type.toString());
            }
        } catch (Exception e) {
            A.p("Failed to send notification: " + e.getMessage());
        }
    }

    private boolean sendNotificationToService(String message, NotificationType type) {
        try {
            URL url = new URL(NOTIFICATION_SERVICE_URL + "/notify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            String level = mapTypeToLevel(type);
            String jsonPayload = String.format(
                "{\"app\":\"v2x.tools\",\"level\":\"%s\",\"message\":\"%s\"}",
                level,
                message.replace("\"", "\\\"").replace("\n", "\\n")
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                A.p("Notification sent successfully via service");
                return true;
            } else {
                A.p("Notification service error: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            A.p("Error sending notification to service: " + e.getMessage());
            return false;
        }
    }

    private String mapTypeToLevel(NotificationType type) {
        switch (type) {
            case ERROR: return "ERROR";
            case SECURITY: return "ERROR";
            default: return "INFO";
        }
    }

    private void testNotificationService() {
        executor.submit(() -> {
            try {
                URL url = new URL(NOTIFICATION_SERVICE_URL + "/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    A.p("Notification service is available");
                } else {
                    A.p("Notification service returned: " + responseCode);
                }
            } catch (Exception e) {
                A.p("Cannot reach notification service: " + e.getMessage());
            }
        });
    }

    private boolean canSendMessage(String messageType) {
        long currentTime = System.currentTimeMillis();
        lastMessageTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > RATE_LIMIT_WINDOW);
        messageCount.entrySet().removeIf(entry -> !lastMessageTime.containsKey(entry.getKey()));
        return messageCount.getOrDefault(messageType, 0) < MAX_MESSAGES_PER_WINDOW;
    }

    private void updateRateLimit(String messageType) {
        lastMessageTime.put(messageType, System.currentTimeMillis());
        messageCount.merge(messageType, 1, Integer::sum);
    }

    private String formatMessage(String message, NotificationType type) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return getEmojiForType(type) + " <b>V2X.tools " + type.getDisplayName() + "</b>\n" +
               "🕒 " + timestamp + "\n" +
               message;
    }

    private String getEmojiForType(NotificationType type) {
        switch (type) {
            case API_USAGE: return "🔄";
            case ERROR: return "❌";
            case STATUS: return "📊";
            case SECURITY: return "🔒";
            default: return "ℹ️";
        }
    }

    private String buildApiUsageMessage(String endpoint, String clientIP, String data, long responseTimeMs) {
        String truncatedData = data.length() > 50 ? data.substring(0, 50) + "..." : data;
        return "📝 <code>" + endpoint + "</code>\n" +
               "🌐 IP: <code>" + clientIP + "</code>\n" +
               "📊 Time: <code>" + responseTimeMs + "ms</code>\n" +
               "💾 Data: <code>" + truncatedData + "</code>";
    }

    private String buildErrorMessage(String error, String context, String clientIP) {
        return "⚠️ <b>Error occurred</b>\n" +
               "📝 " + error + "\n" +
               "🔍 Context: " + context + "\n" +
               "🌐 IP: <code>" + clientIP + "</code>";
    }

    private String buildStatusMessage(String status, String details) {
        return "📈 <b>Status: " + status + "</b>\n" +
               "📋 " + details + "\n" +
               "📊 Uptime: " + getUptimeString();
    }

    private String getUptimeString() {
        long uptimeMs = System.currentTimeMillis() - startTime;
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "h " + minutes + "m";
    }

    public String getStats() {
        return "📈 V2X.tools Monitoring Stats\n" +
               "📤 Messages sent: " + totalMessagesSent + "\n" +
               "🚫 Messages filtered: " + totalMessagesFiltered + "\n" +
               "⏱️ Uptime: " + getUptimeString() + "\n" +
               "🔧 Rate limit: " + MAX_MESSAGES_PER_WINDOW + "/min\n" +
               "🔗 Service: " + NOTIFICATION_SERVICE_URL;
    }

    public void shutdown() {
        A.p("Shutting down TelegramCenter...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
