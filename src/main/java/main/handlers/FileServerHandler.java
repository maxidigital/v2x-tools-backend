package main.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import main.BadRequest;
import main.ContentTypes;
import main.FileExtensions;

/**
 *
 * @author bott_ma
 */
public class FileServerHandler implements HttpHandler
{

    private final String path;
    private String defaultPath = "web";

    public FileServerHandler(String pathToFile) {
        this.path = pathToFile;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    private String routePath(String path) {
        if (FileExtensions.isImage(path))
            return defaultPath + "/images" + path;
        
        // Serve MIGRATION_GUIDE.md from root directory
        if (path.equals("/MIGRATION_GUIDE.md"))
            return "MIGRATION_GUIDE.md";

        return defaultPath + path;
    }

    private static void respondWithFile(HttpExchange exchange, String pathToFile) throws IOException {
        OutputStream os = exchange.getResponseBody();
        Path htmlPath = Paths.get(pathToFile);

        try {
            Files.copy(htmlPath, os);
            os.close();
        } catch (IOException ex) {

            BadRequest badRequest = new BadRequest("File %s not found", new File(pathToFile).getName());
            badRequest.handle(exchange);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String pathToFile = path;

        if (Paths.get(pathToFile) == null) {
            BadRequest badRequest = new BadRequest(pathToFile + " not found");
            badRequest.handle(exchange);
        } else {
            if (pathToFile.equals("/"))
                pathToFile = "/index.html";

            ContentTypes.setContentTypeByFileType(exchange, path);
            main.SecurityHeaders.addSecurityHeaders(exchange);
            exchange.sendResponseHeaders(200, 0);

            pathToFile = routePath(pathToFile);

            System.out.println(":::::::::::::: " + pathToFile);
            respondWithFile(exchange, pathToFile);
        }
    }

}
