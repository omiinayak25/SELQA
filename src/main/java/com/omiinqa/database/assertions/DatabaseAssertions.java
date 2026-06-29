package com.omiinqa.database.assertions;

import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.QueryExecutor;
import com.omiinqa.exceptions.DatabaseException;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fluent, AssertJ-backed utility class for validating database state in tests.
 *
 * <p><b>Design pattern — Fluent Interface (Builder-of-assertions):</b>
 * Each {@code assert*} method returns {@code this}, enabling method chaining:
 * <pre>{@code
 *   DatabaseAssertions.forDatabase(POSTGRESQL)
 *       .assertRowCount("users", 3)
 *       .assertRowExists("SELECT 1 FROM users WHERE email = ?", "alice@example.com")
 *       .assertColumnValue("SELECT status FROM users WHERE id = ?", "active", 1L);
 * }</pre>
 * </p>
 *
 * <p><b>Design pattern — Facade:</b>
 * Wraps {@link QueryExecutor} and AssertJ's {@link Assertions} behind a
 * domain-specific assertion vocabulary. Test authors express intent
 * ({@code assertRowExists}) rather than mechanics ({@code executeQuery, check size}).
 * This dramatically reduces the number of lines needed to verify database state
 * after an API call or UI action.</p>
 *
 * <p><b>SQL injection prevention:</b> All assertions that accept dynamic values
 * pass them through {@link QueryExecutor}'s parameterised-query path (
 * {@code PreparedStatement} with {@code ?} placeholders). Test authors must
 * never embed values in SQL strings — always use the {@code params} vararg.</p>
 *
 * <p><b>Error messages:</b> Every assertion failure produces an AssertJ error
 * message that includes the SQL and parameters, making CI log analysis fast.</p>
 *
 * <p><b>Reusability:</b> An instance is scoped to a single {@link DatabaseType}
 * obtained via {@link #forDatabase(DatabaseType)}. Create once per test class
 * and reuse across {@code @Test} methods.</p>
 */
public final class DatabaseAssertions {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseAssertions.class);

    private final DatabaseType dbType;
    private final QueryExecutor executor;

    // ---------------------------------------------------------------- factory

    /**
     * Creates a {@code DatabaseAssertions} instance targeting the given vendor.
     *
     * @param dbType the database vendor all assertions will be executed against
     * @return a new {@code DatabaseAssertions} instance
     */
    public static DatabaseAssertions forDatabase(final DatabaseType dbType) {
        return new DatabaseAssertions(dbType);
    }

    private DatabaseAssertions(final DatabaseType dbType) {
        this.dbType = dbType;
        this.executor = QueryExecutor.instance();
    }

    // ---------------------------------------------------------------- assertions

    /**
     * Asserts that at least one row is returned by the given parameterised
     * existence query.
     *
     * <p>Best used with an existence check such as
     * {@code "SELECT 1 FROM users WHERE email = ?"} — the column value is
     * irrelevant; only the presence of a row matters.</p>
     *
     * <p><strong>SQL injection note:</strong> embed {@code ?} for all dynamic
     * values; pass the values via {@code params}.</p>
     *
     * @param sql    a parameterised SELECT that returns at least one row if the
     *               expected data exists
     * @param params bind values for {@code ?} placeholders
     * @return {@code this} for fluent chaining
     * @throws AssertionError if no rows are returned
     */
    public DatabaseAssertions assertRowExists(final String sql, final Object... params) {
        LOG.debug("assertRowExists [{}]: {}", dbType, sql);
        final List<Map<String, Object>> rows = executor.queryForList(dbType, sql, params);
        Assertions.assertThat(rows)
                .as("Expected at least one row from [%s] SQL: %s (params: %s)",
                        dbType, sql, java.util.Arrays.toString(params))
                .isNotEmpty();
        return this;
    }

    /**
     * Asserts that the exact row count in {@code tableName} equals
     * {@code expectedCount}.
     *
     * <p>The table name is embedded in the SQL string. Because this method
     * constructs the SQL from {@code tableName}, callers must pass only
     * static, known-safe table names — never user-supplied input.</p>
     *
     * @param tableName     the table to count (must be a known, static name)
     * @param expectedCount the expected number of rows
     * @return {@code this} for fluent chaining
     * @throws AssertionError    if the actual count differs from {@code expectedCount}
     * @throws DatabaseException if the count query fails
     */
    public DatabaseAssertions assertRowCount(final String tableName, final long expectedCount) {
        LOG.debug("assertRowCount [{}]: table={}, expected={}", dbType, tableName, expectedCount);
        // NOTE: tableName is a static identifier (not user input); it is safe to embed here.
        final String sql = "SELECT COUNT(*) FROM " + tableName;
        final long actualCount = executor.queryForScalar(dbType, sql)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
        Assertions.assertThat(actualCount)
                .as("Row count in table [%s.%s]", dbType, tableName)
                .isEqualTo(expectedCount);
        return this;
    }

    /**
     * Asserts that the exact row count in {@code tableName} is greater than
     * or equal to {@code minimumCount}.
     *
     * @param tableName    the table to count (must be a static, known-safe name)
     * @param minimumCount the inclusive lower bound
     * @return {@code this} for fluent chaining
     * @throws AssertionError if the actual count is below {@code minimumCount}
     */
    public DatabaseAssertions assertRowCountAtLeast(
            final String tableName,
            final long minimumCount) {
        LOG.debug("assertRowCountAtLeast [{}]: table={}, min={}", dbType, tableName, minimumCount);
        final String sql = "SELECT COUNT(*) FROM " + tableName;
        final long actualCount = executor.queryForScalar(dbType, sql)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
        Assertions.assertThat(actualCount)
                .as("Row count in table [%s.%s] should be >= %d", dbType, tableName, minimumCount)
                .isGreaterThanOrEqualTo(minimumCount);
        return this;
    }

    /**
     * Asserts that the first column of the first row returned by {@code sql}
     * equals {@code expectedValue} (using {@link Objects#equals} with
     * {@code toString} normalisation for cross-vendor type compatibility).
     *
     * <p>Useful for verifying a specific column in a specific row:
     * {@code "SELECT status FROM users WHERE id = ?"} with expected {@code "active"}.</p>
     *
     * <p><strong>SQL injection note:</strong> pass dynamic values through
     * {@code params}; never concatenate into {@code sql}.</p>
     *
     * @param sql           a parameterised SELECT returning the scalar value to check
     * @param expectedValue the expected value (compared via {@link String#valueOf})
     * @param params        bind values for {@code ?} placeholders
     * @return {@code this} for fluent chaining
     * @throws AssertionError if no row is found or the column value does not match
     */
    public DatabaseAssertions assertColumnValue(
            final String sql,
            final Object expectedValue,
            final Object... params) {
        LOG.debug("assertColumnValue [{}]: {}", dbType, sql);
        final Optional<Object> scalar = executor.queryForScalar(dbType, sql, params);
        Assertions.assertThat(scalar)
                .as("Expected a row from SQL [%s] (params: %s)", sql,
                        java.util.Arrays.toString(params))
                .isPresent();
        final String actual   = String.valueOf(scalar.get());
        final String expected = String.valueOf(expectedValue);
        Assertions.assertThat(actual)
                .as("Column value from SQL [%s] (params: %s)", sql,
                        java.util.Arrays.toString(params))
                .isEqualTo(expected);
        return this;
    }

    /**
     * Asserts that the row returned by {@code sql} contains all key-value pairs
     * in {@code expectedRecord}.
     *
     * <p>The query should return exactly one row. The assertion checks that for
     * every entry {@code (column, value)} in {@code expectedRecord}, the row
     * contains a matching entry (string-normalised comparison to handle
     * vendor-specific type wrapping like {@code BigDecimal} vs {@code Double}).</p>
     *
     * <p><strong>SQL injection note:</strong> dynamic WHERE-clause values must
     * be in {@code params}; column names in {@code expectedRecord} come from
     * the test author, not end-user input, and are only read as map keys.</p>
     *
     * @param sql            a parameterised SELECT expected to return one row
     * @param expectedRecord map of {@code columnName -> expectedValue} pairs to verify
     * @param params         bind values for {@code ?} placeholders in {@code sql}
     * @return {@code this} for fluent chaining
     * @throws AssertionError if no row is found, or any field does not match
     */
    public DatabaseAssertions assertRecordMatches(
            final String sql,
            final Map<String, Object> expectedRecord,
            final Object... params) {
        LOG.debug("assertRecordMatches [{}]: {}", dbType, sql);
        final Optional<Map<String, Object>> rowOpt =
                executor.queryForObject(dbType, sql, (rs, rowNum) -> {
                    final java.sql.ResultSetMetaData meta = rs.getMetaData();
                    final int cols = meta.getColumnCount();
                    final Map<String, Object> row = new java.util.LinkedHashMap<>(cols * 2);
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    return row;
                }, params);

        Assertions.assertThat(rowOpt)
                .as("Expected a row from SQL [%s] (params: %s)", sql,
                        java.util.Arrays.toString(params))
                .isPresent();

        final Map<String, Object> actualRow = rowOpt.get();
        for (final Map.Entry<String, Object> expected : expectedRecord.entrySet()) {
            final String col = expected.getKey().toLowerCase();
            final String expectedStr = String.valueOf(expected.getValue());
            final String actualStr   = String.valueOf(actualRow.get(col));
            Assertions.assertThat(actualStr)
                    .as("Column [%s] in SQL [%s]", col, sql)
                    .isEqualTo(expectedStr);
        }
        return this;
    }

    /**
     * Asserts that <em>no</em> rows are returned by the given parameterised query.
     *
     * <p>Useful as the inverse of {@link #assertRowExists} — for example,
     * after a DELETE, assert no row with that id exists.</p>
     *
     * <p><strong>SQL injection note:</strong> pass dynamic values via
     * {@code params}.</p>
     *
     * @param sql    a parameterised SELECT
     * @param params bind values for {@code ?} placeholders
     * @return {@code this} for fluent chaining
     * @throws AssertionError if one or more rows are returned
     */
    public DatabaseAssertions assertNoRowExists(final String sql, final Object... params) {
        LOG.debug("assertNoRowExists [{}]: {}", dbType, sql);
        final List<Map<String, Object>> rows = executor.queryForList(dbType, sql, params);
        Assertions.assertThat(rows)
                .as("Expected NO rows from [%s] SQL: %s (params: %s)",
                        dbType, sql, java.util.Arrays.toString(params))
                .isEmpty();
        return this;
    }

    /**
     * Returns the {@link DatabaseType} this instance is scoped to.
     *
     * @return the database vendor
     */
    public DatabaseType getDatabaseType() {
        return dbType;
    }
}
