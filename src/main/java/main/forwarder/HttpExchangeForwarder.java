package main.forwarder;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpExchangeForwarder
{

    public static void forwardExchange(HttpExchange exchange, String localServerUrl) throws IOException {
        // Create a connection to the local server
        URL url = new URL(localServerUrl + exchange.getRequestURI().toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Set up the connection
        connection.setRequestMethod(exchange.getRequestMethod());

        // Copy headers from the original request
        for (String headerName : exchange.getRequestHeaders().keySet()) {
            String headerValue = exchange.getRequestHeaders().getFirst(headerName);
            connection.setRequestProperty(headerName, headerValue);
        }

        // Forward the request body if it exists
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())
                || "PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            connection.setDoOutput(true);
            try (InputStream input = exchange.getRequestBody();
                    OutputStream output = connection.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        }

        // Get the response from the local server
        int responseCode = connection.getResponseCode();
        exchange.sendResponseHeaders(responseCode, 0);

        // Copy response headers
        for (String headerName : connection.getHeaderFields().keySet()) {
            if (headerName != null) {
                exchange.getResponseHeaders().set(headerName, connection.getHeaderField(headerName));
            }
        }

        // Forward the response body
        try (InputStream input = connection.getInputStream();
                OutputStream output = exchange.getResponseBody()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        exchange.close();
    }
}
