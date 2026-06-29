package com.omiinqa.database;

/**
 * Enumerates the relational database vendors supported by the JDBC validation layer.
 *
 * <p><b>Design rationale:</b> Encapsulating vendor identity in an enum lets every
 * downstream component — {@link ConnectionManager}, {@link QueryExecutor},
 * {@link TransactionManager}, and the repositories — branch on type without
 * scattering magic strings. Adding a new vendor requires only a new constant here
 * plus the corresponding config keys in {@code config.properties}.</p>
 *
 * <p><b>Config key convention</b> (resolved via
 * {@code com.omiinqa.config.ConfigManager.get().get(key)} or
 * {@code com.omiinqa.config.FrameworkConfig.get().raw(key, defaultValue)}):</p>
 * <pre>
 *   PostgreSQL:  db.postgres.url  /  db.postgres.user  /  db.postgres.password
 *   MySQL:       db.mysql.url     /  db.mysql.user      /  db.mysql.password
 *   Pool size:   db.pool.size     (shared; default 5)
 * </pre>
 */
public enum DatabaseType {

    /**
     * PostgreSQL — config prefix {@code db.postgres}.
     *
     * <p>Connection URL key: {@code db.postgres.url}<br>
     * Credential keys: {@code db.postgres.user}, {@code db.postgres.password}</p>
     */
    POSTGRESQL("db.postgres.url", "db.postgres.user", "db.postgres.password"),

    /**
     * MySQL / MariaDB — config prefix {@code db.mysql}.
     *
     * <p>Connection URL key: {@code db.mysql.url}<br>
     * Credential keys: {@code db.mysql.user}, {@code db.mysql.password}</p>
     */
    MYSQL("db.mysql.url", "db.mysql.user", "db.mysql.password");

    // ---------------------------------------------------------------- fields

    private final String urlKey;
    private final String userKey;
    private final String passwordKey;

    // ------------------------------------------------------------- constructor

    DatabaseType(final String urlKey, final String userKey, final String passwordKey) {
        this.urlKey = urlKey;
        this.userKey = userKey;
        this.passwordKey = passwordKey;
    }

    // ---------------------------------------------------------------- accessors

    /**
     * Returns the config key whose value is the JDBC connection URL for this vendor.
     *
     * @return property key, e.g. {@code "db.postgres.url"}
     */
    public String urlKey() {
        return urlKey;
    }

    /**
     * Returns the config key whose value is the JDBC username for this vendor.
     *
     * @return property key, e.g. {@code "db.postgres.user"}
     */
    public String userKey() {
        return userKey;
    }

    /**
     * Returns the config key whose value is the JDBC password for this vendor.
     *
     * @return property key, e.g. {@code "db.postgres.password"}
     */
    public String passwordKey() {
        return passwordKey;
    }
}
