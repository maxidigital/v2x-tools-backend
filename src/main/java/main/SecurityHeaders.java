package main;

import com.sun.net.httpserver.HttpExchange;

/**
 * Security headers to prevent search engine indexing and improve security
 */
public class SecurityHeaders {
    
    /**
     * Add security headers to prevent indexing and improve security
     * @param exchange The HTTP exchange to add headers to
     */
    public static void addSecurityHeaders(HttpExchange exchange) {
        // Prevent search engine indexing
        exchange.getResponseHeaders().add("X-Robots-Tag", "noindex, nofollow, noarchive, nosnippet, noodp, noimageindex");
        
        // Security headers
        exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer");
        
        // Prevent caching of sensitive content
        exchange.getResponseHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate, private");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.getResponseHeaders().add("Expires", "0");
        
        // Content Security Policy
        exchange.getResponseHeaders().add("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net https://challenges.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.tailwindcss.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data:; " +
            "frame-src https://challenges.cloudflare.com; " +
            "connect-src 'self' https://challenges.cloudflare.com");
    }
}