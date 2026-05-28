package main.monitoring;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NotificationFilter - Filters and controls notification spam
 * Based on re.mind filtering system
 * 
 * Features:
 * - IP-based filtering
 * - Endpoint-based filtering  
 * - User-specific filtering (like MAXI filtering in re.mind)
 * - Frequency limiting
 * 
 * @author v2x.tools
 */
public class NotificationFilter {
    
    // IPs to filter (similar to MAXI user filtering in re.mind)
    private final Set<String> filteredIPs = new HashSet<>();
    
    // Endpoints that should have reduced notifications
    private final Set<String> quietEndpoints = new HashSet<>();
    
    // Track notification frequency per IP
    private final ConcurrentHashMap<String, Long> lastNotificationTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> notificationCount = new ConcurrentHashMap<>();
    
    // Rate limiting settings
    private final long NOTIFICATION_WINDOW = 300000; // 5 minutes
    private final int MAX_NOTIFICATIONS_PER_IP = 10;
    
    public NotificationFilter() {
        initializeFilters();
    }
    
    private void initializeFilters() {
        // Add development IPs (similar to MAXI filtering in re.mind)
        // Note: Don't filter 127.0.0.1 as it might be nginx proxy
        // Note: Don't filter private IPs by default as they might be from load balancers
        
        // Only filter specific known development IPs
        filteredIPs.add("::1"); // IPv6 localhost
        
        // Add quiet endpoints that shouldn't generate too many notifications
        quietEndpoints.add("/index.html");
        quietEndpoints.add("/favicon.ico");
        quietEndpoints.add("/robots.txt");
        quietEndpoints.add("/styles.css");
        quietEndpoints.add("/v2xConverter.js");
        quietEndpoints.add("/sitemap.xml");
        quietEndpoints.add("/api-docs");
        quietEndpoints.add("/swagger-ui");
    }
    
    /**
     * Check if we should notify about API usage
     */
    public boolean shouldNotifyApiUsage(String endpoint, String clientIP) {
        // Filter development IPs
        if (isFilteredIP(clientIP)) {
            return false;
        }
        
        // Filter quiet endpoints
        if (isQuietEndpoint(endpoint)) {
            return false;
        }
        
        // Check rate limiting
        return canNotifyIP(clientIP);
    }
    
    /**
     * Check if we should notify about errors (always notify errors unless severe spam)
     */
    public boolean shouldNotifyError(String clientIP) {
        // Always notify errors from unknown IPs
        if (!isFilteredIP(clientIP)) {
            return true;
        }
        
        // For filtered IPs, only notify if not too frequent
        return canNotifyIP(clientIP);
    }
    
    /**
     * Check if we should notify about status changes (always notify)
     */
    public boolean shouldNotifyStatus() {
        return true;
    }
    
    /**
     * Check if we should notify about security events (always notify)
     */
    public boolean shouldNotifySecurity(String clientIP) {
        return true;
    }
    
    private boolean isFilteredIP(String clientIP) {
        if (clientIP == null) return false;
        
        // Clean IP (remove port if present)
        String cleanIP = clientIP.split(":")[0];
        cleanIP = cleanIP.replace("/", "");
        
        // Only filter explicitly added IPs
        // Don't automatically filter private ranges as they might be legitimate
        // (e.g., from load balancers, Docker networks, etc.)
        return filteredIPs.contains(cleanIP);
    }
    
    private boolean isQuietEndpoint(String endpoint) {
        if (endpoint == null) return false;
        
        return quietEndpoints.contains(endpoint) ||
               endpoint.endsWith(".css") ||
               endpoint.endsWith(".js") ||
               endpoint.endsWith(".ico") ||
               endpoint.endsWith(".png") ||
               endpoint.endsWith(".jpg") ||
               endpoint.endsWith(".jpeg") ||
               endpoint.endsWith(".gif");
    }
    
    private boolean canNotifyIP(String clientIP) {
        long currentTime = System.currentTimeMillis();
        String key = cleanIP(clientIP);
        
        // Clean old entries
        cleanOldEntries(currentTime);
        
        // Check if this IP has exceeded the notification limit
        int count = notificationCount.getOrDefault(key, 0);
        if (count >= MAX_NOTIFICATIONS_PER_IP) {
            return false;
        }
        
        // Update counters
        lastNotificationTime.put(key, currentTime);
        notificationCount.merge(key, 1, Integer::sum);
        
        return true;
    }
    
    private void cleanOldEntries(long currentTime) {
        lastNotificationTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > NOTIFICATION_WINDOW);
        notificationCount.entrySet().removeIf(entry -> 
            !lastNotificationTime.containsKey(entry.getKey()));
    }
    
    private String cleanIP(String clientIP) {
        if (clientIP == null) return "unknown";
        return clientIP.split(":")[0].replace("/", "");
    }
    
    /**
     * Add an IP to the filter list (for development/testing)
     */
    public void addFilteredIP(String ip) {
        filteredIPs.add(cleanIP(ip));
    }
    
    /**
     * Remove an IP from the filter list
     */
    public void removeFilteredIP(String ip) {
        filteredIPs.remove(cleanIP(ip));
    }
    
    /**
     * Add an endpoint to the quiet list
     */
    public void addQuietEndpoint(String endpoint) {
        quietEndpoints.add(endpoint);
    }
    
    /**
     * Get filter statistics
     */
    public String getStats() {
        return "🔍 Notification Filter Stats\n" +
               "🚫 Filtered IPs: " + filteredIPs.size() + "\n" +
               "🔇 Quiet endpoints: " + quietEndpoints.size() + "\n" +
               "📊 Active IP counters: " + notificationCount.size() + "\n" +
               "⏱️ Window: " + (NOTIFICATION_WINDOW / 1000 / 60) + " minutes\n" +
               "📈 Max per IP: " + MAX_NOTIFICATIONS_PER_IP;
    }
}