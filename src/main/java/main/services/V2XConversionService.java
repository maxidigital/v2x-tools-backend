package main.services;

import a.enums.Encoding;
import a.messages.Payload;
import i.WindException;
import main.A;
import main.ContentTypes;
import main.Decoder;
import main.json.JsonOut;
import main.json.JsonIn;
import main.monitoring.TelegramCenter;
import main.monitoring.NotificationType;
import main.stats.CSVLine;
import main.utils.PayloadUtils;

/**
 * V2XConversionService - Centralized conversion logic for all V2X operations
 * Extracts common logic from existing handlers and provides unified interface
 * 
 * @author v2x.tools
 */
public class V2XConversionService {
    
    private static V2XConversionService instance;
    private final TelegramCenter telegramCenter;
    
    private V2XConversionService() {
        this.telegramCenter = TelegramCenter.getInstance();
    }
    
    public static synchronized V2XConversionService getInstance() {
        if (instance == null) {
            instance = new V2XConversionService();
        }
        return instance;
    }
    
    /**
     * Convert V2X data from one format to another
     */
    public ConversionResult convert(String inputData, String fromFormat, String toFormat, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate inputs
            if (inputData == null || inputData.trim().isEmpty()) {
                return ConversionResult.error("Input data is empty", 400);
            }
            
            // Parse input format
            V2XFormat sourceFormat = parseFormat(fromFormat);
            V2XFormat targetFormat = parseFormat(toFormat);
            
            if (sourceFormat == null || targetFormat == null) {
                return ConversionResult.error("Unsupported format conversion", 400);
            }
            
            // Perform conversion
            JsonOut result = performConversion(inputData, sourceFormat, targetFormat);
            
            // Calculate response time
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log statistics
            logConversion(inputData, result, clientIP, endpoint, responseTime);
            
            // Send monitoring notification
            sendMonitoringNotification(endpoint, clientIP, inputData, responseTime);
            
            return ConversionResult.success(result.getData(), result.getResponseCode());
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            A.p("Conversion error: " + e.getMessage());
            
            // Notify error
            telegramCenter.notifyError(e.getMessage(), endpoint, clientIP);
            
            return ConversionResult.error("Error decoding V2X message: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Convert UPER to JSON (legacy method for backward compatibility)
     */
    public ConversionResult convertUperToJson(byte[] inputData, String contentType, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Create payload from input data (existing logic from UPER2JSONHandler)
            Payload payload = createV2XPayload(inputData, contentType);
            
            if (payload == null || payload == Payload.EMPTY) {
                return ConversionResult.error("Invalid input data", 400);
            }
            
            A.p("Received payload: len(%s) %s", payload.getLength(), payload.getHexWithEncoding());
            
            // Decode UPER to JSON (existing logic)
            JsonOut result = new Decoder().decodeUPER2JSON(payload.getBytes());
            
            // Calculate response time
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log statistics (existing logic)
            CSVLine csvLine = new CSVLine(CSVLine.Origin.API,
                    System.currentTimeMillis(),
                    clientIP,
                    payload.getHexWithEncoding(),
                    result.getData());
            A.counterLogger(csvLine.getLine());
            
            // Send monitoring notification
            sendMonitoringNotification(endpoint, clientIP, payload.getHexWithEncoding(), responseTime);
            
            // Send deprecation notification if using legacy endpoint
            if (endpoint.contains("uper2json")) {
                sendDeprecationNotification(endpoint, clientIP);
            }
            
            return ConversionResult.success(result.getData(), result.getResponseCode());
            
        } catch (WindException | IllegalArgumentException ex) {
            long responseTime = System.currentTimeMillis() - startTime;
            telegramCenter.notifyError(ex.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Error decoding V2X message", 500);
            
        } catch (Exception ex) {
            long responseTime = System.currentTimeMillis() - startTime;
            telegramCenter.notifyError(ex.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Internal server error", 500);
        }
    }
    
    /**
     * Convert V2X data from JSON request (web interface format)
     * Used by PostRequestHandler for web interface compatibility
     */
    public ConversionResult convertFromWebRequest(String jsonRequestString, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse web interface JSON request
            JsonIn jsonIn = new JsonIn(jsonRequestString);
            
            // Perform conversion using existing decoder
            JsonOut result = new Decoder().decode(jsonIn);
            
            // Calculate response time
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log statistics using centralized logging
            logConversion(CSVLine.Origin.WEB, jsonIn.getTextData(), result, clientIP, responseTime);
            
            // Send monitoring notification
            sendMonitoringNotification(endpoint, clientIP, jsonIn.getTextData(), responseTime);
            
            return ConversionResult.success(result.getData(), result.getResponseCode());
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            A.p("Web conversion error: " + e.getMessage());
            
            // Notify error
            telegramCenter.notifyError(e.getMessage(), endpoint, clientIP);
            
            return ConversionResult.error("Message could not be decoded", 500);
        }
    }
    
    private Payload createV2XPayload(byte[] blobData, String contentType) throws IllegalArgumentException {
        return PayloadUtils.createV2XPayload(blobData, contentType);
    }
    
    private JsonOut performConversion(String inputData, V2XFormat from, V2XFormat to) throws WindException {
        // Map V2XFormat to Encoding for compatibility with existing Decoder
        Encoding fromEncoding = mapToEncoding(from);
        Encoding toEncoding = mapToEncoding(to);
        
        if (fromEncoding == null || toEncoding == null) {
            throw new UnsupportedOperationException("Conversion from " + from + " to " + to + " not supported");
        }
        
        // Create JsonIn for existing Decoder
        JsonIn jsonIn = new JsonIn(createJsonRequest(inputData, fromEncoding, toEncoding));
        
        // Use existing Decoder which already handles all conversion types
        return new Decoder().decode(jsonIn);
    }
    
    /**
     * Map V2XFormat to legacy Encoding enum
     */
    private Encoding mapToEncoding(V2XFormat format) {
        switch (format) {
            case UPER: return Encoding.UPER;
            case JSON: return Encoding.JSON;
            case XML: return Encoding.XML;
            case WER: return Encoding.WER;
            default: return null;
        }
    }
    
    /**
     * Create JSON request string for JsonIn constructor
     */
    private String createJsonRequest(String textData, Encoding sendFormat, Encoding receiveFormat) {
        // Use JSONObject to properly escape text data
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("textData", textData);
        json.put("sendFormat", sendFormat.name());
        json.put("receiveFormat", receiveFormat.name());
        return json.toString();
    }
    
    private V2XFormat parseFormat(String format) {
        if (format == null) return null;
        
        String upperFormat = format.toUpperCase().trim();
        switch (upperFormat) {
            case "UPER": return V2XFormat.UPER;
            case "JSON": return V2XFormat.JSON;
            case "XML": return V2XFormat.XML;
            case "WER": return V2XFormat.WER;
            default: return null;
        }
    }
    
    /**
     * Centralized CSV logging for all conversion operations
     */
    public void logConversion(CSVLine.Origin origin, String inputData, JsonOut result, String clientIP, long responseTime) {
        CSVLine csvLine = new CSVLine(origin,
                System.currentTimeMillis(),
                clientIP,
                inputData.length() > 100 ? inputData.substring(0, 100) + "..." : inputData,
                result.getData());
        A.counterLogger(csvLine.getLine());
        
        A.p("Conversion completed: origin=%s, client=%s, responseTime=%dms", 
            origin, clientIP, responseTime);
    }
    
    /**
     * Centralized CSV logging with payload-based input
     */
    public void logConversion(CSVLine.Origin origin, Payload payload, JsonOut result, String clientIP, long responseTime) {
        String inputDescription = PayloadUtils.getPayloadDescription(payload);
        CSVLine csvLine = new CSVLine(origin,
                System.currentTimeMillis(),
                clientIP,
                inputDescription,
                result.getData());
        A.counterLogger(csvLine.getLine());
        
        A.p("Conversion completed: origin=%s, client=%s, payload=%s, responseTime=%dms", 
            origin, clientIP, inputDescription, responseTime);
    }
    
    private void logConversion(String inputData, JsonOut result, String clientIP, String endpoint, long responseTime) {
        logConversion(CSVLine.Origin.API, inputData, result, clientIP, responseTime);
    }
    
    private void sendMonitoringNotification(String endpoint, String clientIP, String data, long responseTime) {
        try {
            telegramCenter.notifyApiUsage(endpoint, clientIP, data, responseTime);
        } catch (Exception e) {
            A.p("Failed to send monitoring notification: " + e.getMessage());
        }
    }
    
    /**
     * Send deprecation notification for legacy endpoints
     */
    private void sendDeprecationNotification(String endpoint, String clientIP) {
        try {
            String message = String.format(
                "⚠️ DEPRECATED API USAGE\n" +
                "Endpoint: %s\n" +
                "Client: %s\n" +
                "Sunset: 2025-06-01\n" +
                "Migration: Use /api/v2x/uper/json",
                endpoint, clientIP
            );
            
            // Send deprecation notification (rate limited by TelegramCenter)
            telegramCenter.sendNotification(message, main.monitoring.NotificationType.SECURITY);
            
        } catch (Exception e) {
            A.p("Failed to send deprecation notification: " + e.getMessage());
        }
    }
    
    /**
     * Get service statistics
     */
    public String getStats() {
        return "📊 V2X Conversion Service Stats\n" +
               "🔄 Service: Active\n" +
               "📈 Formats supported: UPER, JSON, XML, WER/BINARY, HEX\n" +
               "🎯 Monitoring: " + (telegramCenter != null ? "Enabled" : "Disabled");
    }
    
    /**
     * Supported V2X formats
     */
    public enum V2XFormat {
        UPER, JSON, XML, WER
    }
}