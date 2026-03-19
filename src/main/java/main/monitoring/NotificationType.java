package main.monitoring;

/**
 * Types of notifications that can be sent through the monitoring system
 * 
 * @author v2x.tools
 */
public enum NotificationType {
    API_USAGE("API Usage"),
    ERROR("Error"),
    STATUS("Status"),
    SECURITY("Security Alert");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}