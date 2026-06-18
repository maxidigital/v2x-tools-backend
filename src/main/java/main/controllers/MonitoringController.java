package main.controllers;

import jakarta.servlet.http.HttpServletRequest;
import main.config.ConfigurationManager;
import main.monitoring.NotificationType;
import main.monitoring.TelegramCenter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final TelegramCenter telegramCenter;
    private final ConfigurationManager config;

    public MonitoringController(TelegramCenter telegramCenter,
                                ConfigurationManager config) {
        this.telegramCenter = telegramCenter;
        this.config = config;
    }

    @PostMapping("/stats")
    public ResponseEntity<String> stats() {
        String stats = "V2X.tools Monitoring Dashboard\n\n" +
                "Configuration:\n" +
                "- Notification Service: " + config.getNotificationServiceUrl() + "\n\n" +
                telegramCenter.getStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/test")
    public ResponseEntity<String> test(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        telegramCenter.sendNotification(
                "Test from " + clientIp + " via /api/monitoring/test",
                NotificationType.STATUS
        );

        String response = "Test notification sent!\n\n" +
                "Notification Service: " + config.getNotificationServiceUrl() + "\n\n" +
                "Check your Telegram for the test message.";
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
