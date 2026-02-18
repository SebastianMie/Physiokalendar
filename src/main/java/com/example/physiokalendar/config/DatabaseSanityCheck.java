package com.example.physiokalendar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Startup sanity check to ensure the configured datasource database name matches the active profile.
 * Prevents accidental connections to prod DB when running dev/test locally.
 */
@Component
public class DatabaseSanityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSanityCheck.class);

    private final Environment env;
    private final String datasourceUrl;

    public DatabaseSanityCheck(Environment env, @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.env = env;
        this.datasourceUrl = datasourceUrl == null ? "" : datasourceUrl.trim();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] profiles = env.getActiveProfiles();
        String active = profiles.length > 0 ? String.join(",", profiles) : env.getProperty("spring.profiles.active", "default");

        if (datasourceUrl.isEmpty()) {
            log.warn("spring.datasource.url is not configured; skipping DB-name sanity check");
            return;
        }

        String dbName = parseDbNameFromJdbcUrl(datasourceUrl);
        if (dbName.isEmpty()) {
            log.warn("Could not parse database name from URL='{}'", datasourceUrl);
            return;
        }

        // Dev / Test / Prod safety assertions
        if (active.contains("dev") && !dbName.endsWith("_dev")) {
            throw new IllegalStateException("Active profile='dev' but datasource points to '" + dbName + "'. " +
                    "Set SPRING_DATASOURCE_URL to jdbc:mysql://localhost:3306/physiocalendar_dev or change APP_ENVIRONMENT to match the DB.");
        }

        if (active.contains("test") && !dbName.endsWith("_test")) {
            throw new IllegalStateException("Active profile='test' but datasource points to '" + dbName + "'. " +
                    "Set SPRING_DATASOURCE_URL to jdbc:mysql://localhost:3307/physiocalendar_test or use the test compose stack.");
        }

        if (active.contains("prod") && (dbName.endsWith("_dev") || dbName.endsWith("_test"))) {
            throw new IllegalStateException("Active profile='prod' but datasource points to '" + dbName + "' (dev/test DB). Aborting startup.");
        }

        log.debug("Database sanity check passed (profile='{}', url='{}', db='{}')", active, datasourceUrl, dbName);
    }

    private String parseDbNameFromJdbcUrl(String url) {
        // Expecting formats like: jdbc:mysql://host:port/dbname[?params]
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash == url.length() - 1) return "";
        String after = url.substring(slash + 1);
        int q = after.indexOf('?');
        if (q >= 0) after = after.substring(0, q);
        return after.trim();
    }
}
