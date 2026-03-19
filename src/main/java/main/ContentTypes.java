package main;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ContentTypes
{

    public static final String CT_JAVASCRIPT = "application/javascript";
    public static final String CT_OCTET_STREAM = "application/octet-stream";
    public static final String CT_JSON = "application/json";

    public static final String CT_TEXT_HTML = "text/html";
    public static final String CT_TEXT_PLAIN = "text/plain";
    public static final String CT_TEXT_CSS = "text/css";

    public static final String CT_IMAGE_ICON = "image/x-icon";
    public static final String CT_IMAGE_PNG = "image/png";
    public static final String CT_IMAGE_JPEG = "image/jpeg";

    /**
     *
     * @param exchange
     * @param pathToFile
     */
    public static void setContentTypeByFileType(HttpExchange exchange, String pathToFile) {
        String contentType = getContentTypeFromPath(pathToFile);
        exchange.getResponseHeaders().set("Content-Type", contentType);
    }

    public static String getContentType(HttpExchange exchange) {
        Map<String, List<String>> headers = exchange.getRequestHeaders();

        // Get Content-Type header
        List<String> contentTypeHeader = headers.get("Content-Type");

        if (contentTypeHeader != null && !contentTypeHeader.isEmpty()) {
            String contentType = contentTypeHeader.get(0);
            //A.p("Content-Type: " + contentType);

            return contentType;
        } else {
            A.p("Content-Type header not present in the request");
        }
        return null;
    }

    private static String getContentTypeFromPath(String pathToFile) {
        String lowercasePath = pathToFile.toLowerCase();
        if (lowercasePath.endsWith(".css")) {
            return CT_TEXT_CSS;
        } else if (lowercasePath.endsWith(".ico")) {
            return CT_IMAGE_ICON;
        } else if (lowercasePath.endsWith(".js")) {
            return CT_JAVASCRIPT;
        } else if (lowercasePath.endsWith(".json")) {
            return CT_JSON;
        } else if (lowercasePath.endsWith(".png")) {
            return CT_IMAGE_PNG;
        } else if (lowercasePath.endsWith(".txt")) {
            return CT_TEXT_PLAIN;
        } else if (lowercasePath.endsWith(".html") || lowercasePath.endsWith(".htm")) {
            return CT_TEXT_HTML;
        } else {
            return CT_OCTET_STREAM; // Default to octet-stream for unknown types
        }
    }

    public static String probeContentType(String pathToFile) {
        try {
            Path path = Paths.get(pathToFile);
            String probedType = Files.probeContentType(path);
            return probedType != null ? probedType : CT_OCTET_STREAM;
        } catch (Exception e) {
            return CT_OCTET_STREAM;
        }
    }
}
