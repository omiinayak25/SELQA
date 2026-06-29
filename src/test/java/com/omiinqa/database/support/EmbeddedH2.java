package com.omiinqa.database.support;

import com.omiinqa.exceptions.DatabaseException;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Boots an in-memory H2 database in PostgreSQL-compatibility mode and points the
 * framework's config at it, so the real database stack (ConnectionManager →
 * HikariCP → QueryExecutor/TransactionManager/repositories) runs end-to-end with
 * <b>zero external infrastructure</b>.
 *
 * <p>How it integrates without touching production code: {@code ConfigManager}
 * resolves system properties ahead of files, so setting {@code db.postgres.url}
 * (and {@code db.mysql.url}) to the H2 URL before the first
 * {@code ConnectionManager.getConnection(...)} transparently redirects the pool
 * to H2. HikariCP auto-detects the H2 driver from the {@code jdbc:h2:} URL.</p>
 *
 * <p>{@code DB_CLOSE_DELAY=-1} keeps the in-memory database alive for the JVM's
 * lifetime even when no connection is held; {@code DATABASE_TO_LOWER=TRUE} keeps
 * unquoted identifiers lowercase to match the repositories' column names.</p>
 */
public final class EmbeddedH2 {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedH2.class);

    public static final String URL =
            "jdbc:h2:mem:omiinqa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                    + "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    public static final String USER = "sa";
    public static final String PASSWORD = "";

    private static final String BOOTSTRAP_SCRIPT = "db/embedded-h2.sql";

    private static volatile boolean started;

    private EmbeddedH2() {
    }

    /** Idempotently redirect config to H2 and (re)load schema + seed data. */
    public static synchronized void start() {
        redirectConfig();
        runBootstrapScript();
        started = true;
        LOG.info("Embedded H2 ready at {}", URL);
    }

    public static boolean isStarted() {
        return started;
    }

    private static void redirectConfig() {
        // Both vendor types resolve to the same in-memory database.
        System.setProperty("db.postgres.url", URL);
        System.setProperty("db.postgres.user", USER);
        System.setProperty("db.mysql.url", URL);
        System.setProperty("db.mysql.user", USER);
        // Passwords intentionally blank (H2 default sa account).
    }

    private static void runBootstrapScript() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(BOOTSTRAP_SCRIPT)) {
            if (in == null) {
                throw new DatabaseException("Bootstrap script not found: " + BOOTSTRAP_SCRIPT);
            }
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                RunScript.execute(conn, reader);
            }
        } catch (final Exception e) {
            throw new DatabaseException("Failed to bootstrap embedded H2", e);
        }
    }
}
