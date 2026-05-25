package main.config;

import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationManager {

    @Value("${notification.service.url:https://notification-service-1.up.railway.app}")
    private String notificationServiceUrl;

    @Value("${monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${monitoring.rate.limit.per.window:10}")
    private int rateLimitPerWindow;

    @Value("${monitoring.rate.limit.window.ms:60000}")
    private long rateLimitWindowMs;

    public String getNotificationServiceUrl() {
        return notificationServiceUrl;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public int getRateLimitPerWindow() {
        return rateLimitPerWindow;
    }

    public long getRateLimitWindowMs() {
        return rateLimitWindowMs;
    }

    public void printConfiguration() {
        A.p("=== Current Configuration ===");
        A.p("Notification Service URL: " + notificationServiceUrl);
        A.p("Monitoring Enabled: " + monitoringEnabled);
        A.p("Rate Limit Per Window: " + rateLimitPerWindow);
        A.p("Rate Limit Window (ms): " + rateLimitWindowMs);
        A.p("===========================");
    }
}
