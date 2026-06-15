package main.services;

import main.A;
import main.engine.EngineClient;
import main.engine.EngineResult;
import main.loader.MessageLoader;
import main.loader.MessageNotAvailableException;
import main.monitoring.TelegramCenter;
import main.stats.CSVLine;
import org.springframework.stereotype.Service;

/**
 * The hub's engine reactor (convert + generate). It does NOT do V2X itself — it asks the engine
 * (over HTTP via EngineClient) and reacts to the typed result: on notFound it resolves the
 * definition (loader → repo → engine) and retries; ok/decodeError pass through. No wind types here.
 *
 * Server-side lazy-load runs ONLY for the public/anonymous user (id 0); other users manage their
 * own loaded state explicitly (via /messages/load) and get a plain notFound if it's missing.
 */
@Service
public class V2XConversionService {

    private final TelegramCenter telegramCenter;
    private final EngineClient engine;
    private final MessageLoader loader;

    public V2XConversionService(TelegramCenter telegramCenter, EngineClient engine, MessageLoader loader) {
        this.telegramCenter = telegramCenter;
        this.engine = engine;
        this.loader = loader;
    }

    public ConversionResult convert(String inputData, String fromFormat, String toFormat,
            String clientIP, String endpoint, Long userId) {
        long start = System.currentTimeMillis();
        if (inputData == null || inputData.trim().isEmpty())
            return ConversionResult.error("Input data is empty", 400);

        try {
            EngineResult r = engine.convert(userId, inputData, fromFormat, toFormat, null);

            if ("notFound".equals(r.status) && isPublicUser(userId)) {
                try {
                    loader.ensureLoaded(userId, r.messageId, r.protocolVersion);
                } catch (MessageNotAvailableException e) {
                    return ConversionResult.error(
                            "Message type not available (" + r.messageId + ":" + r.protocolVersion + ")", 404);
                }
                r = engine.convert(userId, inputData, fromFormat, toFormat, null); // retry, now loaded
            }

            if ("ok".equals(r.status)) {
                long rt = System.currentTimeMillis() - start;
                log(inputData, r.data, clientIP, rt);
                notifyUsage(endpoint, clientIP, inputData, rt);
                return ConversionResult.success(r.data, 200);
            }
            if ("decodeError".equals(r.status)) {
                telegramCenter.notifyError(r.error, endpoint, clientIP);
                return ConversionResult.error(r.error, 400);
            }
            return ConversionResult.error("Message type not available", 404);

        } catch (Exception e) {
            A.p("Unexpected error during conversion: %s", e.getMessage());
            telegramCenter.notifyError(String.valueOf(e.getMessage()), endpoint, clientIP);
            return ConversionResult.error("Internal error during conversion", 500);
        }
    }

    /**
     * Generates a sample payload. Same reactor as convert: on notFound (only for the public user 0)
     * it lazy-loads the definition from the repo and retries. The mid carries the message type.
     */
    public ConversionResult generate(Long userId, String mid, String format, boolean minimal) {
        try {
            EngineResult r = engine.generate(userId, mid, format, minimal);

            if ("notFound".equals(r.status) && isPublicUser(userId)) {
                try {
                    loader.ensureLoaded(userId, r.messageId, r.protocolVersion);
                } catch (MessageNotAvailableException e) {
                    return ConversionResult.error(
                            "Message type not available (" + r.messageId + ":" + r.protocolVersion + ")", 404);
                }
                r = engine.generate(userId, mid, format, minimal); // retry, now loaded
            }

            if ("ok".equals(r.status))
                return ConversionResult.success(r.data, 200);
            if ("decodeError".equals(r.status))
                return ConversionResult.error(r.error, 400);
            return ConversionResult.error("Message type not available (" + r.messageId + ":" + r.protocolVersion + ")", 404);

        } catch (Exception e) {
            A.p("Unexpected error during generate: %s", e.getMessage());
            return ConversionResult.error("Internal error during generate", 500);
        }
    }

    /** Server-side lazy-load is reserved for the public/anonymous user (id 0). */
    private static boolean isPublicUser(Long userId) {
        return userId != null && userId == 0L;
    }

    private void log(String inputData, String data, String clientIP, long responseTime) {
        String in = inputData.length() > 100 ? inputData.substring(0, 100) + "..." : inputData;
        A.counterLogger(new CSVLine(CSVLine.Origin.API, System.currentTimeMillis(), clientIP, in, data).getLine());
        A.p("Conversion completed: client=%s, responseTime=%dms", clientIP, responseTime);
    }

    private void notifyUsage(String endpoint, String clientIP, String data, long responseTime) {
        try {
            telegramCenter.notifyApiUsage(endpoint, clientIP, data, responseTime);
        } catch (Exception e) {
            A.p("Failed to send monitoring notification: " + e.getMessage());
        }
    }

    public String getStats() {
        return "V2X Conversion Service Stats\n"
                + "Service: Active\n"
                + "Formats supported: UPER, JSON, XML, WER\n"
                + "Monitoring: Enabled";
    }
}
