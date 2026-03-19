package main.controllers;

import blue.underwater.email.admin.Email;
import blue.underwater.email.admin.EmailAdmin;
import blue.underwater.email.admin.EmailBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private static final Logger LOGGER = Logger.getLogger(ContactController.class.getName());
    private static final String TURNSTILE_SECRET_KEY = "0x4AAAAAABf1s4zKEi0Rh0nJ06Lkyyhqmg0";
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @PostMapping
    public ResponseEntity<String> handleContact(@RequestBody Map<String, String> formData) {

        boolean isShareRequest = formData.containsKey("recipientEmail") &&
                (formData.containsKey("inputContent") && formData.containsKey("outputContent") ||
                        formData.containsKey("htmlContent"));

        if (isShareRequest) {
            String error = validateShareData(formData);
            if (error != null) return ResponseEntity.badRequest().body(error);
            return handleShare(formData);
        } else {
            String error = validateFormData(formData);
            if (error != null) return ResponseEntity.badRequest().body(error);
            return handleContactForm(formData);
        }
    }

    private ResponseEntity<String> handleContactForm(Map<String, String> formData) {
        try {
            String htmlContent = "<html><body>" +
                    "<h2>New Contact Form Submission</h2>" +
                    "<p><strong>Name:</strong> " + formData.get("name") + "</p>" +
                    "<p><strong>Email:</strong> " + formData.get("email") + "</p>" +
                    "<p><strong>Subject:</strong> " + formData.get("subject") + "</p>" +
                    "<h3>Message:</h3>" +
                    "<p>" + formData.get("message").replace("\n", "<br/>") + "</p>" +
                    "</body></html>";

            Email email = EmailBuilder.create("noreply@v2x.tools", "contact@v2xnow.de", "v2x.tools");
            email.setSubject("[v2x.tools Contact] " + formData.get("subject")).setHtmlContent(htmlContent);
            EmailAdmin.getInstance().send(email);

            return ResponseEntity.ok("Message sent successfully!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send contact email", e);
            return ResponseEntity.internalServerError().body("Failed to send message. Please try again later.");
        }
    }

    private ResponseEntity<String> handleShare(Map<String, String> formData) {
        try {
            String recipientEmail = formData.get("recipientEmail");
            String htmlContent = formData.get("htmlContent");
            String subject = formData.getOrDefault("subject", "");

            boolean isProblemReport = subject.contains("Problem Report");
            String emailSubject = isProblemReport
                    ? "[v2x.tools] Conversion Problem Report"
                    : subject.contains("All Tabs")
                    ? "[v2x.tools] V2X All Tabs Conversion Data"
                    : "[v2x.tools] V2X Conversion Data Shared";

            Email shareEmail = EmailBuilder.create("noreply@v2x.tools", recipientEmail, "v2x.tools");
            shareEmail.setSubject(emailSubject).setHtmlContent(htmlContent);

            String csvContent = formData.get("csvContent");
            if (csvContent != null && !csvContent.trim().isEmpty()) {
                try {
                    shareEmail.addAttachment("v2x_conversion_data.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to add CSV attachment", e);
                }
            }

            if (isProblemReport) {
                Email adminEmail = EmailBuilder.create("noreply@v2x.tools", "contact@v2xnow.de", "v2x.tools");
                adminEmail.setSubject("[v2x.tools] Conversion Problem Report").setHtmlContent(htmlContent);
                EmailAdmin.getInstance().send(adminEmail);

                String reporterEmail = formData.get("email");
                if (reporterEmail != null && reporterEmail.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                    Email userEmail = EmailBuilder.create("noreply@v2x.tools", reporterEmail, "v2x.tools");
                    userEmail.setSubject("[v2x.tools] Problem Report Received - Confirmation")
                            .setHtmlContent("<html><body><h2>Problem Report Confirmation</h2><p>We received your report.</p><hr>" + htmlContent + "</body></html>");
                    try {
                        EmailAdmin.getInstance().send(userEmail);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to send confirmation to reporter", e);
                    }
                }
            } else {
                EmailAdmin.getInstance().send(shareEmail);
            }

            return ResponseEntity.ok("Email sent successfully!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send share email", e);
            return ResponseEntity.internalServerError().body("Failed to send message. Please try again later.");
        }
    }

    private boolean verifyTurnstile(String token) {
        if (token == null || token.trim().isEmpty()) return false;
        try {
            String params = "secret=" + TURNSTILE_SECRET_KEY + "&response=" + token;
            URL url = new URL(TURNSTILE_VERIFY_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }
            String response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                response = reader.lines().collect(Collectors.joining());
            }
            return new org.json.JSONObject(response).optBoolean("success", false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying Turnstile", e);
            return false;
        }
    }

    private String validateFormData(Map<String, String> formData) {
        if (isBlank(formData.get("name"))) return "Name is required";
        if (isBlank(formData.get("email"))) return "Email is required";
        if (isBlank(formData.get("subject"))) return "Subject is required";
        if (isBlank(formData.get("message"))) return "Message is required";
        if (!formData.get("email").matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) return "Invalid email format";
        if (!verifyTurnstile(formData.get("verification"))) return "Security verification failed. Please try again.";
        return null;
    }

    private String validateShareData(Map<String, String> formData) {
        if (isBlank(formData.get("recipientEmail"))) return "Recipient email is required";
        if (!formData.get("recipientEmail").matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) return "Invalid email format";
        if ("true".equals(formData.get("isHTMLShare")) && isBlank(formData.get("htmlContent"))) return "HTML content is required";
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}