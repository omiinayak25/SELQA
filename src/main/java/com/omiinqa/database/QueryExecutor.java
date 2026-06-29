package com.omiinqa.database;

import com.omiinqa.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe, parameterised JDBC query executor (Command + Template Method).
 *
 * <p><b>Security — SQL injection prevention:</b>
 * Every query is executed via a {@link PreparedStatement}. User-supplied values
 * are always bound as typed parameters using {@link PreparedStatement#setObject},
 * which causes the JDBC driver to send the value separately from the SQL text.
 * The database parses the SQL template <em>once</em> and treats parameters as
 * opaque data, so a value like {@code "'; DROP TABLE users; --"} can never alter
 * the query structure. <strong>String concatenation to build SQL is explicitly
 * prohibited throughout this class.</strong></p>
 *
 * <p><b>Design patterns:</b></p>
 * <ul>
 *   <li><em>Template Method</em> — the private {@link #execute} helper owns the
 *       full JDBC lifecycle (obtain connection, prepare statement, bind params,
 *       execute, map result, close resources). Each public method supplies only
 *       the callback that differs: how to consume the {@link ResultSet}.</li>
 *   <li><em>Strategy</em> — {@link RowMapper} is the Strategy abstraction;
 *       callers inject the row-conversion algorithm.</li>
 *   <li><em>Facade</em> — this class hides the verbosity of raw JDBC behind
 *       intention-revealing method names used in test assertions.</li>
 * </ul>
 *
 * <p><b>Resource management:</b> All JDBC resources ({@link Connection},
 * {@link PreparedStatement}, {@link ResultSet}) are closed in finally blocks
 * (or try-with-resources) regardless of success or failure. The connection is
 * returned to the HikariCP pool via its {@code close()} method.</p>
 *
 * <p><b>Thread safety:</b> This class is stateless; a single shared instance is
 * safe for concurrent test methods. Each method acquires and releases its own
 * connection, so parallel threads never share a connection.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   QueryExecutor exec = QueryExecutor.instance();
 *   List<Map<String,Object>> rows = exec.queryForList(
 *       DatabaseType.POSTGRESQL,
 *       "SELECT id, email FROM users WHERE status = ?",
 *       RowMapper.toMap(),
 *       "active");
 * }</pre>
 */
public final class QueryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutor.class);

    /** Singleton — stateless so a single instance serves all callers safely. */
    private static final QueryExecutor INSTANCE = new QueryExecutor();

    private QueryExecutor() { }

    /**
     * Returns the shared {@code QueryExecutor} instance.
     *
     * @return the singleton executor
     */
    public static QueryExecutor instance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- public API

    /**
     * Executes a parameterised SELECT query and returns all rows mapped by
     * the supplied {@link RowMapper}.
     *
     * <p><strong>SQL injection note:</strong> {@code sql} must be a static
     * template with {@code ?} placeholders. Never concatenate user input into
     * the SQL string — pass it via {@code params} instead.</p>
     *
     * @param <T>    the target row type
     * @param type   the database vendor to connect to
     * @param sql    a parameterised SQL SELECT statement (use {@code ?} placeholders)
     * @param mapper strategy that converts each {@link ResultSet} row to {@code T}
     * @param params zero or more bind-parameter values (matched to {@code ?} positionally)
     * @return an unmodifiable list of mapped rows; empty if the query returns no rows
     * @throws DatabaseException if a JDBC error occurs
     */
    public <T> List<T> queryForList(
            final DatabaseType type,
            final String sql,
            final RowMapper<T> mapper,
            final Object... params) {
        LOG.debug("queryForList [{}]: {}", type, sql);
        return execute(type, sql, params, ps -> {
            final List<T> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapper.mapRow(rs, rowNum++));
                }
            }
            return List.copyOf(results);
        });
    }

    /**
     * Convenience overload of {@link #queryForList} using the default
     * {@link MapRowMapper} — returns rows as {@code List<Map<String,Object>>}.
     *
     * <p><strong>SQL injection note:</strong> parameters must be bound via
     * {@code params}; never embed them in the SQL string.</p>
     *
     * @param type   the database vendor
     * @param sql    a parameterised SQL SELECT statement
     * @param params bind-parameter values
     * @return list of rows as column-name-to-value maps
     * @throws DatabaseException if a JDBC error occurs
     */
    public List<Map<String, Object>> queryForList(
            final DatabaseType type,
            final String sql,
            final Object... params) {
        return queryForList(type, sql, RowMapper.toMap(), params);
    }

    /**
     * Executes a parameterised SELECT and returns the first row mapped by
     * {@code mapper}, or {@link Optional#empty()} if the result set is empty.
     *
     * <p><strong>SQL injection note:</strong> user-supplied values must be
     * passed via {@code params}, never concatenated into {@code sql}.</p>
     *
     * @param <T>    the target row type
     * @param type   the database vendor
     * @param sql    a parameterised SQL SELECT statement
     * @param mapper row mapping strategy
     * @param params bind-parameter values
     * @return an {@link Optional} containing the first row, or empty
     * @throws DatabaseException if a JDBC error occurs or more than one row is
     *                           returned (use {@link #queryForList} for multi-row queries)
     */
    public <T> Optional<T> queryForObject(
            final DatabaseType type,
            final String sql,
            final RowMapper<T> mapper,
            final Object... params) {
        LOG.debug("queryForObject [{}]: {}", type, sql);
        return execute(type, sql, params, ps -> {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                final T value = mapper.mapRow(rs, 0);
                if (rs.next()) {
                    throw new DatabaseException(
                            "queryForObject: expected at most 1 row but got more for SQL: " + sql);
                }
                return Optional.of(value);
            }
        });
    }

    /**
     * Executes a parameterised SELECT and returns the value of the first column
     * of the first row as a raw {@link Object}, or {@link Optional#empty()} if
     * no rows were returned.
     *
     * <p>Intended for scalar aggregates such as
     * {@code SELECT COUNT(*) FROM users} or
     * {@code SELECT email FROM users WHERE id = ?}.</p>
     *
     * <p><strong>SQL injection note:</strong> embed {@code ?} in the SQL for
     * any dynamic value; never use string concatenation.</p>
     *
     * @param type   the database vendor
     * @param sql    a parameterised scalar SELECT statement
     * @param params bind-parameter values
     * @return the first-column value of the first row, or {@link Optional#empty()}
     * @throws DatabaseException if a JDBC error occurs
     */
    public Optional<Object> queryForScalar(
            final DatabaseType type,
            final String sql,
            final Object... params) {
        LOG.debug("queryForScalar [{}]: {}", type, sql);
        return execute(type, sql, params, ps -> {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(rs.getObject(1));
            }
        });
    }

    /**
     * Executes a parameterised DML statement (INSERT, UPDATE, DELETE) and
     * returns the number of rows affected.
     *
     * <p><strong>SQL injection note:</strong> all dynamic values — including
     * identifiers derived from user input — must go through {@code params}.
     * Dynamic table/column names are inherently unsafe; prefer static SQL.</p>
     *
     * @param type   the database vendor
     * @param sql    a parameterised INSERT / UPDATE / DELETE statement
     * @param params bind-parameter values
     * @return the number of database rows affected
     * @throws DatabaseException if a JDBC error occurs
     */
    public int update(final DatabaseType type, final String sql, final Object... params) {
        LOG.debug("update [{}]: {}", type, sql);
        return execute(type, sql, params, PreparedStatement::executeUpdate);
    }

    // ---------------------------------------------------------------- internals

    /**
     * Template method: acquire connection → prepare statement → bind params →
     * execute callback → close resources.
     *
     * <p>This single method centralises all JDBC boilerplate so that every
     * public operation stays concise and follows identical resource-management
     * rules. {@link Connection} is always returned to the pool (via HikariCP's
     * proxy {@code close()}) in the {@code finally} block even when the
     * callback throws.</p>
     *
     * @param <R>      the return type produced by the callback
     * @param type     database vendor
     * @param sql      parameterised SQL (must use {@code ?} placeholders, no concatenation)
     * @param params   positional bind values
     * @param callback functional consumer of the prepared statement; produces R
     * @return whatever the callback returns
     * @throws DatabaseException wrapping any {@link SQLException}
     */
    private <R> R execute(
            final DatabaseType type,
            final String sql,
            final Object[] params,
            final SqlFunction<PreparedStatement, R> callback) {
        Connection conn = null;
        try {
            conn = ConnectionManager.get().getConnection(type);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                return callback.apply(ps);
            }
        } catch (final DatabaseException de) {
            throw de; // re-throw already-wrapped exceptions as-is
        } catch (final SQLException e) {
            throw new DatabaseException(
                    "JDBC error executing [" + type + "] SQL: " + sql
                    + " — " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Binds all {@code params} to the {@link PreparedStatement} positionally
     * using {@link PreparedStatement#setObject(int, Object)}.
     *
     * <p>Using {@code setObject} lets the JDBC driver perform type-appropriate
     * binding (including null handling via {@code Types.NULL}) without requiring
     * callers to know the exact SQL type of each column.</p>
     */
    private void bindParams(final PreparedStatement ps, final Object[] params)
            throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Closes a connection without propagating any exception (safe in finally blocks). */
    private void closeQuietly(final Connection conn) {
        if (conn != null) {
            try {
                conn.close(); // returns to HikariCP pool, does not close socket
            } catch (final SQLException e) {
                LOG.warn("Failed to close connection: {}", e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- helper type

    /**
     * Checked-exception-aware functional interface used internally by the
     * {@link #execute} template. Allows lambda callbacks that throw
     * {@link SQLException} without the need for nested try/catch inside lambdas.
     *
     * @param <T> input type
     * @param <R> return type
     */
    @FunctionalInterface
    private interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }
}
