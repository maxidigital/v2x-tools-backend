package main;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author bott_ma
 */
public class HttpTools
{

    public static String getTextFromRequest(HttpExchange exchange) throws IOException {
        // Get the request body as plain text
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        StringBuilder requestData = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestData.append(line);
        }

        A.pt("Received data from frontend: " + requestData.toString());

        return requestData.toString();
    }
}
