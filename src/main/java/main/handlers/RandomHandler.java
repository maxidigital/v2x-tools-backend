package main.handlers;

import a.MessageId;
import a.P;
import a.enums.Encoding;
import a.messages.Payload;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import i.Sequence;
import i.WindException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.A;

/**
 *
 * @author bott_ma
 */
public class RandomHandler implements HttpHandler
{

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();

        // Parse the query string into a map of key-value pairs
        Map<String, String> params = parseQuery(query);

        // Access parameters by their names
        String midParam = params.get("mid");
        String formatParam = params.get("format");
        String minimalParam = params.get("minimal");
        
        // Default to UPER if format is not specified
        if (formatParam == null || formatParam.isEmpty()) {
            formatParam = "UPER";
        }
        
        // Parse minimal boolean parameter
        boolean isMinimal = "true".equalsIgnoreCase(minimalParam);

        MessageId mid = MessageId.createFromStringId(midParam);
        String response = "Random message request: " + mid.toString() + " in format: " + formatParam + ", minimal: " + isMinimal;
        A.p(response);

        if (mid.isUnknown()) {
            response = P.f("Unknown messageId(%s)", mid);
        } else {
            try {
                // Generate the message
                Sequence seq = MessagesApp.getInstance().createEmptyMessage(mid);
                
                // Only randomize if minimal flag is not set
                if (isMinimal) {
                    seq = MessagesApp.getInstance().initialize(seq);
                }
                else {
                    seq = MessagesApp.getInstance().randomize(seq);
                }
                // If minimal is true, we just use the empty message (default values)
                
                // Determine which encoding to use based on requested format
                Encoding encoding;
                switch (formatParam.toUpperCase()) {
                    case "WER":
                        encoding = Encoding.WER;
                        break;
                    case "XML":
                        encoding = Encoding.XML;
                        break;
                    case "JSON":
                        encoding = Encoding.JSON;
                        break;
                    case "UPER":
                    default:
                        encoding = Encoding.UPER;
                        break;
                }
                
                // Encode the message directly to the requested format
                Payload payload = MessagesApp.getInstance().encode(seq, encoding);
                
                // Return the appropriate format
                if (encoding == Encoding.XML || encoding == Encoding.JSON) {
                    // For XML and JSON, return the text content
                    response = payload.toText();
                } else {
                    // For UPER and WER, return the hex with encoding prefix
                    response = payload.getHexWithEncoding();
                }
            } catch (WindException ex) {
                Logger.getLogger(RandomHandler.class.getName()).log(Level.SEVERE, null, ex);
                response = "Error generating random message: " + ex.getMessage();
            }
        }

        //A.p("Responding random: " + response);

        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                params.put(key, value);
            }
        }
        return params;
    }
}
