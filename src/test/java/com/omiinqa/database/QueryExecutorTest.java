package com.omiinqa.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link QueryExecutor} against a live PostgreSQL instance.
 *
 * <p>Tests are guarded by {@code "database"} group and excluded from default
 * suites. They require a running PostgreSQL server with the {@code users} table.</p>
 *
 * <p>These tests exercise the executor directly rather than through repository
 * abstractions, verifying parameterised query handling, scalar extraction,
 * row-list mapping, and DML execution.</p>
 */
@Test(groups = "database", enabled = true)
public class QueryExecutorTest {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutorTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;
    private static final String TEST_EMAIL = "qe_test@omiinqa.test";

    private QueryExecutor executor;

    @BeforeClass
    public void setUp() {
        executor = QueryExecutor.instance();
        LOG.info("QueryExecutorTest: executor ready for {}", DB);
    }

    // ---------------------------------------------------------------- queryForScalar

    @Test(groups = "database", description = "queryForScalar returns count value from users table")
    public void scalarCountFromUsersTable() {
        final Optional<Object> result =
                executor.queryForScalar(DB, "SELECT COUNT(*) FROM users");
        assertThat(result).isPresent();
        assertThat(((Number) result.get()).longValue())
                .as("COUNT(*) should be >= 0")
                .isGreaterThanOrEqualTo(0L);
    }

    @Test(groups = "database", description = "queryForScalar with parameter returns empty for non-existent row")
    public void scalarEmptyForNonExistentEmail() {
        final Optional<Object> result = executor.queryForScalar(
                DB, "SELECT id FROM users WHERE email = ?", "definitely_not_real@noop.test");
        assertThat(result)
                .as("scalar for a non-existent email should be empty")
                .isEmpty();
    }

    // ---------------------------------------------------------------- queryForList (map)

    @Test(groups = "database", description = "queryForList returns list of maps with lower-cased column labels")
    public void queryForListReturnsColumnMaps() {
        final List<Map<String, Object>> rows =
                executor.queryForList(DB, "SELECT id, email FROM users ORDER BY id LIMIT 5");
        // Just verify structure — actual content depends on seed data.
        for (final Map<String, Object> row : rows) {
            assertThat(row).containsKey("id");
            assertThat(row).containsKey("email");
        }
    }

    @Test(groups = "database", description = "queryForList with parameter binds correctly (SQL injection safe)")
    public void queryForListWithParameterBinding() {
        // Deliberately includes SQL metacharacters to prove PreparedStatement binding
        final String injectionAttempt = "' OR '1'='1";
        final List<Map<String, Object>> rows = executor.queryForList(
                DB,
                "SELECT id, email FROM users WHERE email = ?",
                injectionAttempt);
        // Injection attempt should find 0 rows, not all rows.
        assertThat(rows)
                .as("SQL injection attempt should find 0 rows, not all rows")
                .isEmpty();
    }

    // ---------------------------------------------------------------- queryForObject

    @Test(groups = "database", description = "queryForObject returns mapped UserRecord-style data")
    public void queryForObjectMapsCorrectly() {
        // Insert then retrieve to test mapping; cleanup is implicit since
        // we delete by email at the end.
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "QE User", TEST_EMAIL, "active");

        try {
            final Optional<Map<String, Object>> row = executor.queryForObject(
                    DB,
                    "SELECT id, name, email, status FROM users WHERE email = ?",
                    RowMapper.toMap(),
                    TEST_EMAIL);
            assertThat(row).isPresent();
            assertThat(row.get().get("email")).isEqualTo(TEST_EMAIL);
            assertThat(row.get().get("name")).isEqualTo("QE User");
        } finally {
            executor.update(DB, "DELETE FROM users WHERE email = ?", TEST_EMAIL);
        }
    }

    @Test(groups = "database", description = "queryForObject returns empty Optional for no-match query")
    public void queryForObjectEmptyForNoMatch() {
        final Optional<Map<String, Object>> result = executor.queryForObject(
                DB,
                "SELECT id FROM users WHERE email = ?",
                RowMapper.toMap(),
                "nobody@noop.test");
        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------- update / DML

    @Test(groups = "database", description = "update() INSERT returns 1 row affected; DELETE cleans up")
    public void updateInsertThenDeleteReturnsCorrectRowCounts() {
        final int insertRows = executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "QE Delete User", TEST_EMAIL + ".del", "active");
        assertThat(insertRows).as("INSERT should affect 1 row").isEqualTo(1);

        final int deleteRows = executor.update(DB,
                "DELETE FROM users WHERE email = ?",
                TEST_EMAIL + ".del");
        assertThat(deleteRows).as("DELETE should affect 1 row").isEqualTo(1);
    }

    @Test(groups = "database",
          description = "update() for non-existent row returns 0 rows affected")
    public void updateNonExistentRowReturnsZero() {
        final int rows = executor.update(
                DB,
                "DELETE FROM users WHERE email = ?",
                "ghost@noop.test");
        assertThat(rows)
                .as("DELETE of non-existent row should affect 0 rows")
                .isEqualTo(0);
    }

    // ---------------------------------------------------------------- custom RowMapper

    @Test(groups = "database", description = "Custom RowMapper lambda extracts single column correctly")
    public void customRowMapperExtractsSingleColumn() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "RM User", TEST_EMAIL + ".rm", "active");
        try {
            final List<String> emails = executor.queryForList(
                    DB,
                    "SELECT email FROM users WHERE email = ?",
                    (rs, rowNum) -> rs.getString("email"),
                    TEST_EMAIL + ".rm");
            assertThat(emails).containsExactly(TEST_EMAIL + ".rm");
        } finally {
            executor.update(DB, "DELETE FROM users WHERE email = ?", TEST_EMAIL + ".rm");
        }
    }
}
