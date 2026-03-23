package main.controllers;

import jakarta.servlet.http.HttpServletRequest;
import main.config.ConfigurationManager;
import main.monitoring.NotificationType;
import main.monitoring.TelegramCenter;
import main.services.V2XConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final V2XConversionService conversionService;
    private final TelegramCenter telegramCenter;
    private final ConfigurationManager config;

    public MonitoringController(V2XConversionService conversionService,
                                TelegramCenter telegramCenter,
                                ConfigurationManager config) {
        this.conversionService = conversionService;
        this.telegramCenter = telegramCenter;
        this.config = config;
    }

    @PostMapping("/stats")
    public ResponseEntity<String> stats() {
        String stats = "V2X.tools Monitoring Dashboard\n\n" +
                "Configuration:\n" +
                "- Telegram Enabled: " + config.isTelegramEnabled() + "\n" +
                "- Bot Token: " + (config.getTelegramBotToken().isEmpty() ? "NOT SET" : "SET") + "\n" +
                "- Chat ID: " + config.getTelegramChatId() + "\n\n" +
                conversionService.getStats() + "\n\n" +
                telegramCenter.getStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/test")
    public ResponseEntity<String> test(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        telegramCenter.sendNotification(
                "Test Notification\nFrom: " + clientIp + "\nEndpoint: /api/monitoring/test\nMonitoring system is working!",
                NotificationType.STATUS
        );

        String response = "Test notification sent!\n\n" +
                "Configuration:\n" +
                "- Telegram Enabled: " + config.isTelegramEnabled() + "\n" +
                "- Bot Token: " + (config.getTelegramBotToken().isEmpty() ? "NOT SET" : "SET") + "\n" +
                "- Chat ID: " + config.getTelegramChatId() + "\n\n" +
                "Check your Telegram for the test message.";
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
