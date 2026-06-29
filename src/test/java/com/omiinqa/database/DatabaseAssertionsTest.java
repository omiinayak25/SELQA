package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link DatabaseAssertions} against a live PostgreSQL instance.
 *
 * <p>Tests are guarded by {@code "database"} group. Each test verifies a specific
 * assertion method on {@link DatabaseAssertions}: the happy path (assertion
 * passes) and where practical the failure path (assertion throws
 * {@link AssertionError} for a mismatched expectation).</p>
 *
 * <p>Test data is isolated by using a distinctive email prefix and cleaned up
 * unconditionally in {@link #cleanup()} to prevent cross-test contamination in
 * the shared database.</p>
 */
@Test(groups = "database", enabled = true)
public class DatabaseAssertionsTest {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseAssertionsTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;
    private static final String TEST_EMAIL = "da_test@omiinqa.test";

    private DatabaseAssertions dbAssert;
    private QueryExecutor executor;

    @BeforeClass
    public void setUp() {
        dbAssert  = DatabaseAssertions.forDatabase(DB);
        executor  = QueryExecutor.instance();
        LOG.info("DatabaseAssertionsTest: ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        executor.update(DB, "DELETE FROM users WHERE email LIKE ?", "da_%@omiinqa.test");
    }

    // ---------------------------------------------------------------- assertRowExists

    @Test(groups = "database",
          description = "assertRowExists passes when the row is present")
    public void assertRowExistsPassesWhenRowPresent() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA User", TEST_EMAIL, "active");

        dbAssert.assertRowExists(
                "SELECT 1 FROM users WHERE email = ?",
                TEST_EMAIL);
    }

    @Test(groups = "database",
          description = "assertRowExists fails (AssertionError) when no row matches")
    public void assertRowExistsFailsWhenRowAbsent() {
        assertThatThrownBy(() ->
            dbAssert.assertRowExists(
                    "SELECT 1 FROM users WHERE email = ?",
                    "absolutely_not_there@omiinqa.test")
        ).isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------- assertRowCount

    @Test(groups = "database",
          description = "assertRowCount passes when table count matches expectation")
    public void assertRowCountPassesForCorrectCount() {
        final long before = executor.queryForScalar(DB, "SELECT COUNT(*) FROM users")
                .map(v -> ((Number) v).longValue()).orElse(0L);

        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Count User", TEST_EMAIL, "active");

        dbAssert.assertRowCount("users", before + 1);
    }

    @Test(groups = "database",
          description = "assertRowCount fails (AssertionError) when count is wrong")
    public void assertRowCountFailsForWrongCount() {
        final long actualCount = executor.queryForScalar(DB, "SELECT COUNT(*) FROM users")
                .map(v -> ((Number) v).longValue()).orElse(0L);

        assertThatThrownBy(() ->
            dbAssert.assertRowCount("users", actualCount + 999L)
        ).isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------- assertColumnValue

    @Test(groups = "database",
          description = "assertColumnValue passes when the column holds the expected value")
    public void assertColumnValuePassesForCorrectValue() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Col User", TEST_EMAIL, "active");

        dbAssert.assertColumnValue(
                "SELECT status FROM users WHERE email = ?",
                "active",
                TEST_EMAIL);
    }

    @Test(groups = "database",
          description = "assertColumnValue fails (AssertionError) when value does not match")
    public void assertColumnValueFailsForWrongValue() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Col User Wrong", TEST_EMAIL, "active");

        assertThatThrownBy(() ->
            dbAssert.assertColumnValue(
                    "SELECT status FROM users WHERE email = ?",
                    "inactive",    // wrong expected value
                    TEST_EMAIL)
        ).isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------- assertRecordMatches

    @Test(groups = "database",
          description = "assertRecordMatches passes when all expected fields are present and correct")
    public void assertRecordMatchesPassesForMatchingRecord() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Record User", TEST_EMAIL, "active");

        final Map<String, Object> expected = Map.of(
                "name",   "DA Record User",
                "email",  TEST_EMAIL,
                "status", "active");

        dbAssert.assertRecordMatches(
                "SELECT name, email, status FROM users WHERE email = ?",
                expected,
                TEST_EMAIL);
    }

    @Test(groups = "database",
          description = "assertRecordMatches fails (AssertionError) when a field value is wrong")
    public void assertRecordMatchesFailsForMismatch() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Mismatch User", TEST_EMAIL, "active");

        final Map<String, Object> wrongExpected = Map.of("status", "inactive");

        assertThatThrownBy(() ->
            dbAssert.assertRecordMatches(
                    "SELECT status FROM users WHERE email = ?",
                    wrongExpected,
                    TEST_EMAIL)
        ).isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------- assertNoRowExists

    @Test(groups = "database",
          description = "assertNoRowExists passes when query returns no rows")
    public void assertNoRowExistsPassesWhenAbsent() {
        dbAssert.assertNoRowExists(
                "SELECT 1 FROM users WHERE email = ?",
                "ghost_no_row@omiinqa.test");
    }

    @Test(groups = "database",
          description = "assertNoRowExists fails (AssertionError) when a row is found")
    public void assertNoRowExistsFailsWhenPresent() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA NoRow User", TEST_EMAIL, "active");

        assertThatThrownBy(() ->
            dbAssert.assertNoRowExists(
                    "SELECT 1 FROM users WHERE email = ?",
                    TEST_EMAIL)
        ).isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------- fluent chaining

    @Test(groups = "database",
          description = "Fluent chaining of multiple assertions in one statement compiles and passes")
    public void fluentChainingCompilesAndPasses() {
        executor.update(DB,
                "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                "DA Chain User", TEST_EMAIL, "active");

        // Verifies the fluent API: each method returns DatabaseAssertions.
        DatabaseAssertions.forDatabase(DB)
                .assertRowExists("SELECT 1 FROM users WHERE email = ?", TEST_EMAIL)
                .assertRowCountAtLeast("users", 1L)
                .assertColumnValue(
                        "SELECT status FROM users WHERE email = ?", "active", TEST_EMAIL);
    }
}
