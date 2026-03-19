package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

import static main.ContentTypes.CT_TEXT_PLAIN;

/**
 *
 * @author bott_ma
 */
public class EmptyResponse implements HttpHandler
{

    private String errorMessage;

    public EmptyResponse() {
        this(null);
    }

    public EmptyResponse(String errorMessage, Object... args) {
        this.errorMessage = String.format(errorMessage, args);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            A.pt("Responding 22: " + errorMessage);
            exchange.sendResponseHeaders(200, errorMessage.length());
            exchange.getResponseHeaders().set("Content-Type", CT_TEXT_PLAIN);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorMessage.getBytes());
            }
        } else {
            A.pt("Responding 200 with empty error message");
            exchange.sendResponseHeaders(200, -1);
        }
    }

}
