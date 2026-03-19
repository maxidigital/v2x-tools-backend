package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

import static main.ContentTypes.CT_TEXT_PLAIN;

public class BadRequest implements HttpHandler
{

    private String errorMessage;

    public BadRequest() {
        this(null);
    }

    /**
     *
     * @param errorMessage
     * @param args
     */
    public BadRequest(String errorMessage, Object... args) {
        this.errorMessage = String.format(errorMessage, args);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            A.pt("Responding 400: " + errorMessage);
            exchange.sendResponseHeaders(400, errorMessage.length());
            exchange.getResponseHeaders().set("Content-Type", CT_TEXT_PLAIN);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorMessage.getBytes());
            }
        } else {
            A.pt("Responding 400 without message");
            exchange.sendResponseHeaders(400, -1);
        }
    }
}
