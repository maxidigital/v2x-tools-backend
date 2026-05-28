package main.services;

/**
 * ConversionResult - Wrapper for conversion operation results
 * Provides unified response format for all conversion operations
 * 
 * @author v2x.tools
 */
public class ConversionResult {
    
    private final boolean success;
    private final String data;
    private final String errorMessage;
    private final int httpStatusCode;
    private final long timestamp;
    
    private ConversionResult(boolean success, String data, String errorMessage, int httpStatusCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a successful result
     */
    public static ConversionResult success(String data, int httpStatusCode) {
        return new ConversionResult(true, data, null, httpStatusCode);
    }
    
    /**
     * Create a successful result with default 200 status
     */
    public static ConversionResult success(String data) {
        return new ConversionResult(true, data, null, 200);
    }
    
    /**
     * Create an error result
     */
    public static ConversionResult error(String errorMessage, int httpStatusCode) {
        return new ConversionResult(false, null, errorMessage, httpStatusCode);
    }
    
    /**
     * Create an error result with default 500 status
     */
    public static ConversionResult error(String errorMessage) {
        return new ConversionResult(false, null, errorMessage, 500);
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getData() {
        return data;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get response data for HTTP response
     */
    public String getResponseData() {
        if (success) {
            return data;
        } else {
            return errorMessage != null ? errorMessage : "Unknown error";
        }
    }
    
    @Override
    public String toString() {
        return "ConversionResult{" +
               "success=" + success +
               ", httpStatusCode=" + httpStatusCode +
               ", data=" + (data != null ? (data.length() > 100 ? data.substring(0, 100) + "..." : data) : "null") +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }
}