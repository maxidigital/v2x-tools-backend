package main.services;

import a.enums.Encoding;
import a.messages.Payload;
import i.WindException;
import main.A;
import main.Decoder;
import main.json.JsonOut;
import main.json.JsonIn;
import main.monitoring.TelegramCenter;
import main.monitoring.NotificationType;
import main.stats.CSVLine;
import main.utils.PayloadUtils;
import org.springframework.stereotype.Service;

@Service
public class V2XConversionService {

    private final TelegramCenter telegramCenter;

    public V2XConversionService(TelegramCenter telegramCenter) {
        this.telegramCenter = telegramCenter;
    }

    public ConversionResult convert(String inputData, String fromFormat, String toFormat, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();

        try {
            if (inputData == null || inputData.trim().isEmpty()) {
                return ConversionResult.error("Input data is empty", 400);
            }

            V2XFormat sourceFormat = parseFormat(fromFormat);
            V2XFormat targetFormat = parseFormat(toFormat);

            if (sourceFormat == null || targetFormat == null) {
                return ConversionResult.error("Unsupported format conversion", 400);
            }

            JsonOut result = performConversion(inputData, sourceFormat, targetFormat);
            long responseTime = System.currentTimeMillis() - startTime;

            logConversion(inputData, result, clientIP, endpoint, responseTime);
            sendMonitoringNotification(endpoint, clientIP, inputData, responseTime);

            return ConversionResult.success(result.getData(), result.getResponseCode());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            A.p("Conversion error: " + e.getMessage());
            telegramCenter.notifyError(e.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Error decoding V2X message: " + e.getMessage(), 500);
        }
    }

    public ConversionResult convertUperToJson(byte[] inputData, String contentType, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();

        try {
            Payload payload = createV2XPayload(inputData, contentType);

            if (payload == null || payload == Payload.EMPTY) {
                return ConversionResult.error("Invalid input data", 400);
            }

            A.p("Received payload: len(%s) %s", payload.getLength(), payload.getHexWithEncoding());

            JsonOut result = new Decoder().decodeUPER2JSON(payload.getBytes());
            long responseTime = System.currentTimeMillis() - startTime;

            CSVLine csvLine = new CSVLine(CSVLine.Origin.API,
                    System.currentTimeMillis(), clientIP,
                    payload.getHexWithEncoding(), result.getData());
            A.counterLogger(csvLine.getLine());

            sendMonitoringNotification(endpoint, clientIP, payload.getHexWithEncoding(), responseTime);

            if (endpoint.contains("uper2json")) {
                sendDeprecationNotification(endpoint, clientIP);
            }

            return ConversionResult.success(result.getData(), result.getResponseCode());

        } catch (WindException | IllegalArgumentException ex) {
            telegramCenter.notifyError(ex.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Error decoding V2X message", 500);

        } catch (Exception ex) {
            telegramCenter.notifyError(ex.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Internal server error", 500);
        }
    }

    public ConversionResult convertFromWebRequest(String jsonRequestString, String clientIP, String endpoint) {
        long startTime = System.currentTimeMillis();

        try {
            JsonIn jsonIn = new JsonIn(jsonRequestString);
            JsonOut result = new Decoder().decode(jsonIn);
            long responseTime = System.currentTimeMillis() - startTime;

            logConversion(CSVLine.Origin.WEB, jsonIn.getTextData(), result, clientIP, responseTime);
            sendMonitoringNotification(endpoint, clientIP, jsonIn.getTextData(), responseTime);

            return ConversionResult.success(result.getData(), result.getResponseCode());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            A.p("Web conversion error: " + e.getMessage());
            telegramCenter.notifyError(e.getMessage(), endpoint, clientIP);
            return ConversionResult.error("Message could not be decoded", 500);
        }
    }

    private Payload createV2XPayload(byte[] blobData, String contentType) {
        return PayloadUtils.createV2XPayload(blobData, contentType);
    }

    private JsonOut performConversion(String inputData, V2XFormat from, V2XFormat to) throws WindException {
        Encoding fromEncoding = mapToEncoding(from);
        Encoding toEncoding = mapToEncoding(to);

        if (fromEncoding == null || toEncoding == null) {
            throw new UnsupportedOperationException("Conversion from " + from + " to " + to + " not supported");
        }

        JsonIn jsonIn = new JsonIn(createJsonRequest(inputData, fromEncoding, toEncoding));
        return new Decoder().decode(jsonIn);
    }

    private Encoding mapToEncoding(V2XFormat format) {
        switch (format) {
            case UPER: return Encoding.UPER;
            case JSON: return Encoding.JSON;
            case XML: return Encoding.XML;
            case WER: return Encoding.WER;
            default: return null;
        }
    }

    private String createJsonRequest(String textData, Encoding sendFormat, Encoding receiveFormat) {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("textData", textData);
        json.put("sendFormat", sendFormat.name());
        json.put("receiveFormat", receiveFormat.name());
        return json.toString();
    }

    private V2XFormat parseFormat(String format) {
        if (format == null) return null;
        switch (format.toUpperCase().trim()) {
            case "UPER": return V2XFormat.UPER;
            case "JSON": return V2XFormat.JSON;
            case "XML": return V2XFormat.XML;
            case "WER": return V2XFormat.WER;
            default: return null;
        }
    }

    public void logConversion(CSVLine.Origin origin, String inputData, JsonOut result, String clientIP, long responseTime) {
        CSVLine csvLine = new CSVLine(origin, System.currentTimeMillis(), clientIP,
                inputData.length() > 100 ? inputData.substring(0, 100) + "..." : inputData,
                result.getData());
        A.counterLogger(csvLine.getLine());
        A.p("Conversion completed: origin=%s, client=%s, responseTime=%dms", origin, clientIP, responseTime);
    }

    public void logConversion(CSVLine.Origin origin, Payload payload, JsonOut result, String clientIP, long responseTime) {
        String inputDescription = PayloadUtils.getPayloadDescription(payload);
        CSVLine csvLine = new CSVLine(origin, System.currentTimeMillis(), clientIP, inputDescription, result.getData());
        A.counterLogger(csvLine.getLine());
        A.p("Conversion completed: origin=%s, client=%s, payload=%s, responseTime=%dms", origin, clientIP, inputDescription, responseTime);
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

    private void sendDeprecationNotification(String endpoint, String clientIP) {
        try {
            String message = String.format(
                "DEPRECATED API USAGE\nEndpoint: %s\nClient: %s\nSunset: 2025-06-01\nMigration: Use /api/v2x/uper/json",
                endpoint, clientIP
            );
            telegramCenter.sendNotification(message, NotificationType.SECURITY);
        } catch (Exception e) {
            A.p("Failed to send deprecation notification: " + e.getMessage());
        }
    }

    public String getStats() {
        return "V2X Conversion Service Stats\n" +
               "Service: Active\n" +
               "Formats supported: UPER, JSON, XML, WER\n" +
               "Monitoring: Enabled";
    }

    public enum V2XFormat {
        UPER, JSON, XML, WER
    }
}
