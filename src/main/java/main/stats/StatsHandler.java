package main.stats;

import a.tools.FileTools;
import a.tools.Lines;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.A;
import main.BadRequest;
import main.ContentTypes;

/**
 *
 * @author bott_ma
 */
public class StatsHandler implements HttpHandler
{

    private final String folder;

    /**
     *
     * @param folder
     */
    public StatsHandler(String folder) {
        this.folder = folder;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //String contentType = ContentTypes.getContentType(exchange);
        //String textFromRequest = HttpTools.getTextFromRequest(exchange);

        List<CSVLine> readLines = readLines();
        String response = createResponse(readLines);
        respondSuccess(exchange, response);
    }

    private String createResponse(List<CSVLine> readLines) {
        StringBuilder sb = new StringBuilder();

        for (CSVLine readLine : readLines) {
            //System.out.println(readLine);
            sb.append(",").append(readLine.toJson());
        }

        String line = sb.toString();
        if (!line.isEmpty())
            line = line.substring(1);

        return "[" + line + "]";
    }

    private void respondSuccess(HttpExchange exchange, String response) {

        try {
            String _response = response.replaceAll("\\n", "");
            _response = _response.length() < 100 ? _response : _response.substring(0, 100);
            A.pt("Responding: %s", _response);

            // Send the response
            exchange.getResponseHeaders().set("Content-Type", ContentTypes.CT_TEXT_PLAIN);
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException ex) {
            BadRequest badRequest = new BadRequest("Error processing request");
            try {
                badRequest.handle(exchange);
            } catch (IOException ex1) {
                Logger.getLogger(StatsHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        StatsHandler statsHandler = new StatsHandler("counter");
        statsHandler.transform();
    }

    private void transform() throws IOException {
        List<CSVLine> ret = new ArrayList<>();

        File _folder = new File(folder);
        File[] listFiles = _folder.listFiles();
        for (File f : _folder.listFiles()) {
            if (f.getName().endsWith("csv")) {
                Lines lines = FileTools.readTextFile(f.getAbsolutePath());
                for (String line : lines) {
                    try {
                        ret.add(new CSVLine(line));
                        FileTools.appendToTextFile("c:/temp/cacho.csv", new CSVLine(line).getLine());
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     *
     * @return @throws IOException
     */
    public List<CSVLine> readLines() throws IOException {
        List<CSVLine> ret = new ArrayList<>();

        File _folder = new File(folder);
        File[] listFiles = _folder.listFiles();
        for (File f : _folder.listFiles()) {
            if (f.getName().endsWith("csv")) {
                Lines lines = FileTools.readTextFile(f.getAbsolutePath());
                for (String line : lines) {
                    try {
                        ret.add(new CSVLine(line));
                    } catch (Exception e) {
                    }
                }
            }
        }

        return ret;
    }
}
