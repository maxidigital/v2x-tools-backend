package main.config;

import main.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationManager {

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${telegram.chat.id:}")
    private String telegramChatId;

    @Value("${telegram.enabled:true}")
    private boolean telegramEnabled;

    @Value("${monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${monitoring.rate.limit.per.window:10}")
    private int rateLimitPerWindow;

    @Value("${monitoring.rate.limit.window.ms:60000}")
    private long rateLimitWindowMs;

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public boolean isTelegramEnabled() {
        return telegramEnabled;
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
        A.p("Telegram Bot Token: " + (telegramBotToken.isEmpty() ? "NOT SET" : "***SET***"));
        A.p("Telegram Chat ID: " + (telegramChatId.isEmpty() ? "NOT SET" : "***SET***"));
        A.p("Telegram Enabled: " + telegramEnabled);
        A.p("Monitoring Enabled: " + monitoringEnabled);
        A.p("Rate Limit Per Window: " + rateLimitPerWindow);
        A.p("Rate Limit Window (ms): " + rateLimitWindowMs);
        A.p("===========================");
    }
}
