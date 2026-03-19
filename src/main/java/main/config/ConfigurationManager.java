package main.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import main.A;

/**
 * ConfigurationManager - Manages application configuration from properties and environment
 * 
 * @author v2x.tools
 */
public class ConfigurationManager {
    
    private static ConfigurationManager instance;
    private final Properties properties = new Properties();
    
    private ConfigurationManager() {
        loadConfiguration();
    }
    
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        // Try to load from config.properties file
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
            fis.close();
            A.p("Configuration loaded from config.properties");
        } catch (IOException e) {
            A.p("Warning: Could not load config.properties: " + e.getMessage());
        }
        
        // Override with environment variables if present
        loadEnvironmentVariables();
    }
    
    private void loadEnvironmentVariables() {
        // Load Telegram configuration
        String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        if (botToken != null) {
            properties.setProperty("telegram.bot.token", botToken);
        }
        
        String chatId = System.getenv("TELEGRAM_CHAT_ID");
        if (chatId != null) {
            properties.setProperty("telegram.chat.id", chatId);
        }
        
        String enabled = System.getenv("TELEGRAM_ENABLED");
        if (enabled != null) {
            properties.setProperty("telegram.enabled", enabled);
        }
    }
    
    public String getTelegramBotToken() {
        return properties.getProperty("telegram.bot.token", "").trim();
    }
    
    public String getTelegramChatId() {
        return properties.getProperty("telegram.chat.id", "").trim();
    }
    
    public boolean isTelegramEnabled() {
        return Boolean.parseBoolean(properties.getProperty("telegram.enabled", "true"));
    }
    
    public boolean isMonitoringEnabled() {
        return Boolean.parseBoolean(properties.getProperty("monitoring.enabled", "true"));
    }
    
    public int getRateLimitPerWindow() {
        return Integer.parseInt(properties.getProperty("monitoring.rate.limit.per.window", "10"));
    }
    
    public long getRateLimitWindowMs() {
        return Long.parseLong(properties.getProperty("monitoring.rate.limit.window.ms", "60000"));
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Debug method to print all loaded configuration
     */
    public void printConfiguration() {
        A.p("=== Current Configuration ===");
        A.p("Telegram Bot Token: " + (getTelegramBotToken().isEmpty() ? "NOT SET" : "***SET***"));
        A.p("Telegram Chat ID: " + getTelegramChatId());
        A.p("Telegram Enabled: " + isTelegramEnabled());
        A.p("Monitoring Enabled: " + isMonitoringEnabled());
        A.p("Rate Limit Per Window: " + getRateLimitPerWindow());
        A.p("Rate Limit Window (ms): " + getRateLimitWindowMs());
        A.p("===========================");
    }
}