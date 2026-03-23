package main.controllers;

import main.stats.StatsHandler;
import main.stats.CSVLine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StatsController {

    @PostMapping("/api/access-stats")
    public ResponseEntity<String> accessStats() {
        try {
            StatsHandler handler = new StatsHandler("counter");
            List<CSVLine> lines = handler.readLines();

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(lines.get(i).toJson());
            }
            sb.append("]");

            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error reading stats");
        }
    }
}
