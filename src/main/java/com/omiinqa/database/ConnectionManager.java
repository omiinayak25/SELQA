package com.omiinqa.database;

import com.omiinqa.config.ConfigManager;
import com.omiinqa.exceptions.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * HikariCP-backed JDBC connection-pool manager (Singleton + Multiton).
 *
 * <p><b>Design pattern — Singleton:</b> A single {@code ConnectionManager} instance
 * is created per JVM (double-checked locking with volatile guarantees safe
 * publication across threads). This prevents multiple pool instances competing
 * for the same database server connections, which would exhaust the server-side
 * connection limit and degrade throughput under parallel TestNG execution.</p>
 *
 * <p><b>Design pattern — Multiton (pool-per-vendor):</b> One
 * {@link HikariDataSource} is maintained per {@link DatabaseType} inside an
 * {@link EnumMap}. This means a test suite that touches both PostgreSQL and
 * MySQL keeps completely independent pools — no cross-vendor interference —
 * while still sharing the single manager entry point.</p>
 *
 * <p><b>Lazy initialisation:</b> Pools are NOT created at class-load or at
 * manager construction. Each pool is built on the first
 * {@link #getConnection(DatabaseType)} call for that vendor. This is critical
 * in CI environments where no database server is running: the framework
 * starts up, loads configuration, and runs non-database tests without
 * ever attempting a TCP handshake to a database host.</p>
 *
 * <p><b>Configuration keys</b> are read via
 * {@link com.omiinqa.config.ConfigManager#get(String, String)}:</p>
 * <ul>
 *   <li>{@code db.postgres.url / db.postgres.user / db.postgres.password}</li>
 *   <li>{@code db.mysql.url   / db.mysql.user   / db.mysql.password}</li>
 *   <li>{@code db.pool.size}  — maximum pool size per vendor (default: 5)</li>
 * </ul>
 *
 * <p>Secrets should be supplied via environment variables or
 * {@code -Ddb.postgres.password=secret} rather than committed properties
 * files, following the twelve-factor app principle.</p>
 *
 * <p><b>Thread safety:</b> A per-vendor {@link ReentrantLock} serialises pool
 * construction. Once a {@code HikariDataSource} is created it is itself
 * thread-safe and lock-free for {@code getConnection()} calls.</p>
 */
public final class ConnectionManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /** Config key for the shared pool size across all vendors. */
    private static final String KEY_POOL_SIZE = "db.pool.size";
    private static final int DEFAULT_POOL_SIZE = 5;

    /** Connection-idle timeout in milliseconds. */
    private static final long IDLE_TIMEOUT_MS = 600_000L;   // 10 min

    /** Maximum lifetime of a connection in the pool, in milliseconds. */
    private static final long MAX_LIFETIME_MS = 1_800_000L; // 30 min

    /** Connection-acquisition timeout, in milliseconds. */
    private static final long CONNECTION_TIMEOUT_MS = 30_000L;

    // ----------------------------------------------------------------- Singleton

    private static volatile ConnectionManager instance;

    /** Per-vendor pool map; entries are created lazily. */
    private final Map<DatabaseType, HikariDataSource> pools =
            new EnumMap<>(DatabaseType.class);

    /**
     * Per-vendor construction locks — serialises first-touch pool creation for
     * each vendor independently so two threads can initialise POSTGRESQL and
     * MYSQL simultaneously without blocking each other.
     */
    private final Map<DatabaseType, ReentrantLock> initLocks =
            new EnumMap<>(DatabaseType.class);

    private ConnectionManager() {
        // Populate a lock for every known vendor up front (cheap, just lock objects).
        for (final DatabaseType type : DatabaseType.values()) {
            initLocks.put(type, new ReentrantLock());
        }
    }

    /**
     * Returns the global {@code ConnectionManager} instance, creating it if
     * needed (double-checked locking, safe under Java memory model with
     * {@code volatile}).
     *
     * @return the singleton {@code ConnectionManager}
     */
    public static ConnectionManager get() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }

    // ----------------------------------------------------------------- public API

    /**
     * Borrows a pooled {@link Connection} for the specified database vendor.
     *
     * <p>The pool for {@code type} is lazily built on the first call. The
     * caller <em>must</em> close the connection (in a try-with-resources block)
     * to return it to the pool — HikariCP wraps the real connection so
     * {@code close()} does not actually close the socket.</p>
     *
     * @param type the target database vendor
     * @return a live, pooled {@code Connection}; caller must close it
     * @throws DatabaseException if the pool cannot be created or a connection
     *                           cannot be acquired within the timeout
     */
    public Connection getConnection(final DatabaseType type) {
        final HikariDataSource ds = getOrCreatePool(type);
        try {
            return ds.getConnection();
        } catch (final SQLException e) {
            throw new DatabaseException(
                    "Failed to acquire connection from " + type + " pool: " + e.getMessage(), e);
        }
    }

    /**
     * Closes and discards the pool for the given vendor, releasing all pooled
     * connections back to the database server. Subsequent calls to
     * {@link #getConnection(DatabaseType)} will rebuild the pool.
     *
     * <p>Useful in tests that want an isolated, fresh pool mid-suite.</p>
     *
     * @param type the database vendor whose pool should be closed
     */
    public void close(final DatabaseType type) {
        final ReentrantLock lock = initLocks.get(type);
        lock.lock();
        try {
            final HikariDataSource ds = pools.remove(type);
            if (ds != null && !ds.isClosed()) {
                ds.close();
                LOG.info("ConnectionManager: closed pool for {}", type);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Shuts down all active pools, releasing every pooled connection.
     *
     * <p>Call from a test suite {@code @AfterSuite} method or JVM shutdown
     * hook to avoid connection leaks after a full run.</p>
     */
    public void shutdown() {
        for (final DatabaseType type : DatabaseType.values()) {
            close(type);
        }
        LOG.info("ConnectionManager: all pools shut down.");
    }

    // ----------------------------------------------------------------- internals

    /**
     * Returns the existing {@link HikariDataSource} for {@code type}, or
     * creates it if this is the first access (per-vendor lazy init).
     */
    private HikariDataSource getOrCreatePool(final DatabaseType type) {
        // Fast path — pool already built, no locking needed.
        final HikariDataSource existing = pools.get(type);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }

        // Slow path — acquire vendor-specific lock, double-check, then build.
        final ReentrantLock lock = initLocks.get(type);
        lock.lock();
        try {
            final HikariDataSource recheck = pools.get(type);
            if (recheck != null && !recheck.isClosed()) {
                return recheck;
            }
            final HikariDataSource ds = buildPool(type);
            pools.put(type, ds);
            return ds;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Constructs and validates a new {@link HikariDataSource} for the vendor.
     *
     * @param type vendor to configure
     * @return a ready-to-use, validated data source
     * @throws DatabaseException if configuration is missing or the pool fails
     *                           its initial connection validation
     */
    private HikariDataSource buildPool(final DatabaseType type) {
        LOG.info("ConnectionManager: initialising HikariCP pool for {}", type);

        final ConfigManager cfg = ConfigManager.get();
        final String url  = cfg.get(type.urlKey(),      "");
        final String user = cfg.get(type.userKey(),     "");
        final String pass = cfg.get(type.passwordKey(), "");
        final int poolSize = cfg.getInt(KEY_POOL_SIZE, DEFAULT_POOL_SIZE);

        if (url.isBlank()) {
            throw new DatabaseException(
                    "Missing or blank config key '" + type.urlKey()
                    + "' — cannot initialise " + type + " pool.");
        }

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(1);
        config.setIdleTimeout(IDLE_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setPoolName("OmiinQA-" + type.name());
        config.setAutoCommit(true);

        // Vendor-specific validation queries keep pool-health checks lightweight.
        switch (type) {
            case POSTGRESQL -> config.setConnectionTestQuery("SELECT 1");
            case MYSQL      -> config.setConnectionTestQuery("/* ping */ SELECT 1");
            default         -> config.setConnectionTestQuery("SELECT 1");
        }

        try {
            final HikariDataSource ds = new HikariDataSource(config);
            LOG.info("ConnectionManager: {} pool ready (maxSize={})", type, poolSize);
            return ds;
        } catch (final Exception e) {
            throw new DatabaseException(
                    "Failed to create HikariCP pool for " + type + ": " + e.getMessage(), e);
        }
    }
}
