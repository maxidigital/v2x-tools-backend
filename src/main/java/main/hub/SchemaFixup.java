package main.hub;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time schema cleanup on startup. The {@code saved_message} refactor dropped four columns from the
 * entity, but {@code ddl-auto=update} never removes columns — and the old {@code module_id}/{@code type_name}
 * were {@code NOT NULL}, so inserts of the new (blob-only) entity violate them. Drop the orphans here.
 * Idempotent ({@code IF EXISTS}); safe to run on every boot.
 */
@Component
public class SchemaFixup implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public SchemaFixup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String col : new String[]{"module_id", "type_name", "fixups", "description"}) {
            try {
                jdbc.execute("ALTER TABLE saved_message DROP COLUMN IF EXISTS " + col);
            } catch (Exception e) {
                System.err.println("SchemaFixup: could not drop saved_message." + col + ": " + e.getMessage());
            }
        }
    }
}
