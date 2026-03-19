//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import main.ContentTypes;
import blue.underwater.email.admin.Email;
import blue.underwater.email.admin.EmailAdmin;
import blue.underwater.email.admin.EmailBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handler for processing contact form submissions
 */
public class ContactFormHandler implements HttpHandler {
    
    private static final Logger LOGGER = Logger.getLogger(ContactFormHandler.class.getName());
    private static final String TURNSTILE_SECRET_KEY = "0x4AAAAAABf1s4zKEi0Rh0nJ06Lkyyhqmg0";
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    
    /**
     * Verifies Cloudflare Turnstile token
     * 
     * @param token The Turnstile token from the client
     * @return true if the token is valid, false otherwise
     */
    private boolean verifyTurnstile(String token) {
        if (token == null || token.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Turnstile token is null or empty");
            return false;
        }
        
        
        LOGGER.log(Level.INFO, "Verifying Turnstile token (length: {0})", token.length());
        
        try {
            // Create URL with parameters
            String params = "secret=" + TURNSTILE_SECRET_KEY + "&response=" + token;
            
            LOGGER.log(Level.INFO, "Sending verification request to Turnstile API");
            
            // Create connection to Turnstile verification endpoint
            URL url = new URL(TURNSTILE_VERIFY_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            // Send the request
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(params.getBytes(StandardCharsets.UTF_8));
            }
            
            // Read the response
            String response;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                response = reader.lines().collect(Collectors.joining());
            }
            
            // Parse the JSON response
            LOGGER.log(Level.INFO, "Turnstile API response: {0}", response);
            
            JSONObject jsonResponse = new JSONObject(response);
            boolean success = jsonResponse.optBoolean("success", false);
            
            if (!success) {
                String errorCodes = jsonResponse.optJSONArray("error-codes") != null ? 
                    jsonResponse.getJSONArray("error-codes").toString() : "Unknown error";
                LOGGER.log(Level.WARNING, "Turnstile verification failed: {0}", errorCodes);
            } else {
                LOGGER.log(Level.INFO, "Turnstile verification successful");
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying Turnstile", e);
            return false;
        }
    }
    
    /**
     * Escapes HTML entities in a string to ensure XML tags display correctly
     * 
     * @param input The string to escape
     * @return The escaped string
     */
    private String escapeHtmlEntities(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#039;");
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Only handle POST requests
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Set CORS headers for cross-origin requests
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            // Handle preflight OPTIONS request
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            // Get the request body
            String requestBody = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            LOGGER.log(Level.INFO, "Received contact form submission: {0}", requestBody);
            
            // Parse JSON request
            LOGGER.log(Level.INFO, "Parsing form data...");
            Map<String, String> formData = parseContactFormData(requestBody);
            LOGGER.log(Level.INFO, "Form data parsed successfully, checking request type...");
            
            // Check if this is a share request
            boolean isShareRequest = formData.containsKey("recipientEmail") && 
                                     (formData.containsKey("inputContent") && formData.containsKey("outputContent") ||
                                      formData.containsKey("htmlContent"));
            
            // Different validation and email handling for share requests
            if (isShareRequest) {
                String validationError = validateShareData(formData);
                if (validationError != null) {
                    sendResponse(exchange, 400, validationError);
                    return;
                }
            } else {
                // Standard contact form validation
                LOGGER.log(Level.INFO, "Processing standard contact form...");
                String validationError = validateFormData(formData);
                LOGGER.log(Level.INFO, "Validation result: {0}", validationError == null ? "PASSED" : validationError);
                if (validationError != null) {
                    sendResponse(exchange, 400, validationError);
                    return;
                }
            }
            
            // Send the email using email.admin
            boolean emailSent = false;
            try {
                // Handle share request
                if (isShareRequest) {
                    String recipientEmail = formData.get("recipientEmail");
                    String htmlContent;
                    String emailSubject;
                    
                    // All shares now use HTML content from frontend
                    htmlContent = formData.get("htmlContent");
                    
                    // Determine email subject based on share type
                    if (formData.containsKey("subject") && formData.get("subject").contains("Problem Report")) {
                        emailSubject = "[v2x.tools] Conversion Problem Report";
                    } else if (formData.containsKey("csvContent") && formData.get("subject").contains("All Tabs")) {
                        emailSubject = "[v2x.tools] V2X All Tabs Conversion Data";
                    } else {
                        emailSubject = "[v2x.tools] V2X Conversion Data Shared";
                    }
                    
                    // Create email using the EmailBuilder
                    Email shareEmail = EmailBuilder.create(
                        "noreply@v2x.tools",    // from email
                        recipientEmail,         // to email - recipient email
                        "v2x.tools"             // from name
                    );
                    
                    shareEmail.setSubject(emailSubject)
                             .setHtmlContent(htmlContent);
                    
                    // Add CSV attachment for HTML shares (multiple tabs)
                    if (formData.containsKey("csvContent")) {
                        String csvContent = formData.get("csvContent");
                        if (csvContent != null && !csvContent.trim().isEmpty()) {
                            try {
                                // Create CSV attachment
                                shareEmail.addAttachment("v2x_conversion_data.csv", "text/csv", csvContent.getBytes("UTF-8"));
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Failed to add CSV attachment: {0}", e.getMessage());
                            }
                        }
                    }
                
                    // For problem reports, send two completely separate emails (no CC anywhere)
                    if (formData.containsKey("subject") && formData.get("subject").contains("Problem Report")) {
                        // EMAIL 1: Send to admin (contact@v2xnow.de)
                        Email adminEmail = EmailBuilder.create(
                            "noreply@v2x.tools",    // from
                            "contact@v2xnow.de",    // to admin  
                            "v2x.tools"             // from name
                        );
                        adminEmail.setSubject("[v2x.tools] Conversion Problem Report")
                                 .setHtmlContent(htmlContent);
                        
                        EmailAdmin.getInstance().send(adminEmail);
                        emailSent = true;
                        
                        // EMAIL 2: Send to reporter if email provided
                        String reporterEmail = formData.get("email");
                        if (reporterEmail != null && !reporterEmail.trim().isEmpty() && 
                            !reporterEmail.equals("noreply@v2x.tools") && 
                            reporterEmail.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                            
                            // Create confirmation email content
                            String confirmationHtml = 
                                "<html><body>" +
                                "<h2>Problem Report Confirmation</h2>" +
                                "<p>Thank you for reporting a problem with v2x.tools conversion.</p>" +
                                "<p>We have received your report and will investigate the issue.</p>" +
                                "<hr>" +
                                "<h3>Your Report:</h3>" +
                                htmlContent +
                                "<hr>" +
                                "<p><em>This is a copy of your problem report for your records.</em></p>" +
                                "<p>Best regards,<br>The v2x.tools Team</p>" +
                                "</body></html>";
                            
                            Email userEmail = EmailBuilder.create(
                                "noreply@v2x.tools",   // from
                                reporterEmail,          // to user
                                "v2x.tools"             // from name
                            );
                            userEmail.setSubject("[v2x.tools] Problem Report Received - Confirmation")
                                    .setHtmlContent(confirmationHtml);
                            
                            try {
                                EmailAdmin.getInstance().send(userEmail);
                            } catch (Exception e) {
                                // Log error but don't fail the main request
                                LOGGER.log(Level.WARNING, "Failed to send confirmation email to reporter: {0}", e.getMessage());
                            }
                        }
                    } else {
                        // For regular shares, send normally
                        EmailAdmin.getInstance().send(shareEmail);
                        emailSent = true;
                    }
                
                    // Email sent successfully
                    sendResponse(exchange, 200, "Email sent successfully!");
                    return;
                } 
                // Handle standard contact form
                else {
                    String name = formData.get("name");
                    String userEmail = formData.get("email");
                    String subject = formData.get("subject");
                    String messageBody = formData.get("message");
                    
                    
                    try {
                    
                    // Create HTML content
                    String htmlContent = "<html><body>" +
                        "<h2>New Contact Form Submission</h2>" +
                        "<p><strong>Name:</strong> " + name + "</p>" +
                        "<p><strong>Email:</strong> " + userEmail + "</p>" +
                        "<p><strong>Subject:</strong> " + subject + "</p>" +
                        "<h3>Message:</h3>" +
                        "<p>" + messageBody.replace("\n", "<br/>") + "</p>" +
                        "</body></html>";
                    
                    // Create email using the EmailBuilder
                    Email email = EmailBuilder.create(
                        "noreply@v2x.tools",    // from email
                        "contact@v2xnow.de",     // to email - admin email
                        "v2x.tools"              // from name
                    );
                    
                    email.setSubject("[v2x.tools Contact] " + subject)
                         .setHtmlContent(htmlContent);
                
                    // Send the email
                    EmailAdmin.getInstance().send(email);
                    emailSent = true;
                
                    // Email sent successfully
                    sendResponse(exchange, 200, "Message sent successfully!");
                    
                    } catch (Exception emailEx) {
                        LOGGER.log(Level.SEVERE, "Error sending email in contact form", emailEx);
                        throw emailEx;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to send contact form email", e);
                // Failed to send email
                sendResponse(exchange, 500, "Failed to send message. Please try again later.");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing contact form submission", e);
            sendResponse(exchange, 500, "Internal server error");
        }
    }
    
    /**
     * Parses the contact form data from JSON
     * 
     * @param jsonData The JSON data as a string
     * @return A map of form field names to values
     * @throws JSONException If the JSON is invalid
     */
    private Map<String, String> parseContactFormData(String jsonData) throws JSONException {
        Map<String, String> formData = new HashMap<>();
        
        JSONObject json = new JSONObject(jsonData);
        
        if (json.has("name")) {
            formData.put("name", json.getString("name"));
        }
        
        if (json.has("email")) {
            formData.put("email", json.getString("email"));
        }
        
        if (json.has("subject")) {
            formData.put("subject", json.getString("subject"));
        }
        
        if (json.has("message")) {
            formData.put("message", json.getString("message"));
        }
        
        if (json.has("verification")) {
            formData.put("verification", json.getString("verification"));
        }
        
        // For share via email functionality
        if (json.has("recipientEmail")) {
            formData.put("recipientEmail", json.getString("recipientEmail"));
        }
        
        if (json.has("inputContent")) {
            formData.put("inputContent", json.getString("inputContent"));
        }
        
        if (json.has("outputContent")) {
            formData.put("outputContent", json.getString("outputContent"));
        }
        
        if (json.has("inputFormat")) {
            formData.put("inputFormat", json.getString("inputFormat"));
        }
        
        if (json.has("outputFormat")) {
            formData.put("outputFormat", json.getString("outputFormat"));
        }
        
        if (json.has("htmlContent")) {
            formData.put("htmlContent", json.getString("htmlContent"));
        }
        
        if (json.has("isHTMLShare")) {
            formData.put("isHTMLShare", json.getString("isHTMLShare"));
        }
        
        if (json.has("csvContent")) {
            formData.put("csvContent", json.getString("csvContent"));
        }
        
        if (json.has("recaptcha")) {
            formData.put("recaptcha", json.getString("recaptcha"));
        }
        
        return formData;
    }
    
    /**
     * Validates the contact form data
     * 
     * @param formData The form data to validate
     * @return null if validation passes, or an error message if validation fails
     */
    private String validateFormData(Map<String, String> formData) {
        // Check required fields
        if (!formData.containsKey("name") || formData.get("name").trim().isEmpty()) {
            return "Name is required";
        }
        
        if (!formData.containsKey("email") || formData.get("email").trim().isEmpty()) {
            return "Email is required";
        }
        
        if (!formData.containsKey("subject") || formData.get("subject").trim().isEmpty()) {
            return "Subject is required";
        }
        
        if (!formData.containsKey("message") || formData.get("message").trim().isEmpty()) {
            return "Message is required";
        }
        
        // Email format validation (basic)
        String email = formData.get("email").trim();
        if (!email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            return "Invalid email format";
        }
        
        // Turnstile verification check
        String verification = formData.get("verification");
        if (verification == null || verification.trim().isEmpty()) {
            return "Security verification is required";
        }
        
        // Verify the Turnstile token
        if (!verifyTurnstile(verification)) {
            return "Security verification failed. Please try again.";
        }
        
        return null; // Validation passed
    }
    
    /**
     * Validates the share data
     * 
     * @param formData The form data to validate
     * @return null if validation passes, or an error message if validation fails
     */
    private String validateShareData(Map<String, String> formData) {
        // Check recipient email
        if (!formData.containsKey("recipientEmail") || formData.get("recipientEmail").trim().isEmpty()) {
            return "Recipient email is required";
        }
        
        // Email format validation (basic)
        String email = formData.get("recipientEmail").trim();
        if (!email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            return "Invalid email format";
        }
        
        // Check content - either HTML content or input/output content
        if (formData.containsKey("isHTMLShare") && "true".equals(formData.get("isHTMLShare"))) {
            // HTML share validation
            if (!formData.containsKey("htmlContent") || formData.get("htmlContent").trim().isEmpty()) {
                return "HTML content is required";
            }
        } else {
            // Regular share validation
            if (!formData.containsKey("inputContent") || formData.get("inputContent").trim().isEmpty()) {
                return "Input content is required";
            }
            
            if (!formData.containsKey("outputContent") || formData.get("outputContent").trim().isEmpty()) {
                return "Output content is required";
            }
        }
        
        return null; // Validation passed
    }
    
    /**
     * Sends a HTTP response
     * 
     * @param exchange The HTTP exchange
     * @param statusCode The HTTP status code
     * @param message The response message
     * @throws IOException If an I/O error occurs
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject response = new JSONObject();
        response.put("status", statusCode < 300 ? "success" : "error");
        response.put("message", message);
        
        byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", ContentTypes.CT_JSON);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}