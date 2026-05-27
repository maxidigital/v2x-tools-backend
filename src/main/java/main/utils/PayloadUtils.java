package main.utils;

import a.enums.Encoding;
import a.messages.Payload;
import main.ContentTypes;

/**
 * PayloadUtils - Centralized utility for V2X payload creation and validation
 * Eliminates code duplication across handlers
 * 
 * @author v2x.tools
 */
public class PayloadUtils {
    
    /**
     * Create V2X payload from byte array and content type
     * Centralizes logic previously duplicated in UPER2JSONHandler and V2XConversionService
     */
    public static Payload createV2XPayload(byte[] blobData, String contentType) throws IllegalArgumentException {
        if (blobData == null || blobData.length == 0) {
            return Payload.EMPTY;
        }

        if (contentType != null && contentType.equals(ContentTypes.CT_TEXT_PLAIN)) {
            try {
                String hex = new String(blobData);
                return Payload.create(hex);
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
        return Payload.create(blobData, Encoding.UPER);
    }
    
    /**
     * Create V2X payload from hex string
     * Convenience method for string-based payloads
     */
    public static Payload createV2XPayload(String hexString) throws IllegalArgumentException {
        if (hexString == null || hexString.trim().isEmpty()) {
            return Payload.EMPTY;
        }
        return Payload.create(hexString.trim());
    }
    
    /**
     * Validate payload is not empty
     */
    public static boolean isValidPayload(Payload payload) {
        return payload != null && payload != Payload.EMPTY && payload.getLength() > 0;
    }
    
    /**
     * Get safe payload description for logging (truncated if too long)
     */
    public static String getPayloadDescription(Payload payload, int maxLength) {
        if (payload == null || payload == Payload.EMPTY) {
            return "EMPTY_PAYLOAD";
        }
        
        String description = payload.getHexWithEncoding();
        if (description.length() > maxLength) {
            return description.substring(0, maxLength) + "...";
        }
        return description;
    }
    
    /**
     * Get safe payload description for logging with default max length
     */
    public static String getPayloadDescription(Payload payload) {
        return getPayloadDescription(payload, 100);
    }
}