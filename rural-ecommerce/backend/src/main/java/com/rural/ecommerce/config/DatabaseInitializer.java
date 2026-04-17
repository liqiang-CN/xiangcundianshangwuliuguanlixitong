package com.rural.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs the database initialisation script on application startup.
 * <p>
 * On first boot the {@code order_info} table (used as a sentinel) will not
 * exist, so the component reads {@code db/init.sql} from the classpath and
 * executes every statement against the configured datasource.  All DDL uses
 * {@code CREATE TABLE IF NOT EXISTS} and all seed inserts use
 * {@code INSERT IGNORE}, so subsequent restarts are completely safe.
 */
@Slf4j
@Component
@Order(1)
public class DatabaseInitializer implements InitializingBean {

    private static final String SENTINEL_TABLE = "order_info";
    private static final String INIT_SCRIPT    = "db/init.sql";

    @Autowired
    private DataSource dataSource;

    @Override
    public void afterPropertiesSet() {
        try (Connection conn = dataSource.getConnection()) {
            if (schemaExists(conn)) {
                log.info("DatabaseInitializer: schema already present – skipping init.");
                return;
            }

            log.info("DatabaseInitializer: '{}' table not found – running {} ...",
                    SENTINEL_TABLE, INIT_SCRIPT);
            List<String> statements = loadStatements();
            executeStatements(conn, statements);
            log.info("DatabaseInitializer: schema initialisation complete ({} statements executed).",
                    statements.size());

        } catch (Exception e) {
            log.error("DatabaseInitializer: failed to initialise database schema – {}", e.getMessage(), e);
            throw new RuntimeException("Database schema initialisation failed", e);
        }
    }

    /**
     * Returns {@code true} when the sentinel table already exists in the
     * current database, meaning the schema has been initialised previously.
     */
    private boolean schemaExists(Connection conn) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(
                null, null, SENTINEL_TABLE, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Reads {@code db/init.sql} from the classpath and splits it into
     * individual statements, skipping blank lines and comment-only lines.
     */
    private List<String> loadStatements() throws Exception {
        ClassPathResource resource = new ClassPathResource(INIT_SCRIPT);
        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }

        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            // Skip pure comment lines and blank lines when building statements
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            current.append(line).append("\n");
            if (trimmed.endsWith(";")) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            }
        }

        // Flush any remaining content without a trailing semicolon
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    private void executeStatements(Connection conn, List<String> statements) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    // Log and continue – "already exists" / duplicate-key errors are non-fatal
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("already exists") || msg.contains("Duplicate entry"))) {
                        log.debug("DatabaseInitializer: skipping (already exists): {}", msg);
                    } else {
                        log.warn("DatabaseInitializer: error executing statement – {}", msg);
                        log.debug("Failed statement: {}", sql);
                    }
                }
            }
        }
    }
}
