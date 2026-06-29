package com.omiinqa.database;

import com.omiinqa.database.model.UserRecord;
import com.omiinqa.database.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRepository} against a live PostgreSQL instance.
 *
 * <p>These tests are guarded by the {@code "database"} TestNG group and are
 * excluded from the default smoke/regression suites. They require a running
 * PostgreSQL server configured via {@code db.postgres.*} config keys (or
 * the corresponding {@code -D} system properties).</p>
 *
 * <p>Tests do NOT extend {@link com.omiinqa.core.BaseTest} — no browser is
 * needed for JDBC validation. Each test cleans up its own data in
 * {@link #cleanup()} to keep the database state predictable across runs.</p>
 *
 * <p>All SQL in the repository uses {@link java.sql.PreparedStatement}, so
 * the test data values below are safe even if they contain SQL metacharacters.</p>
 */
@Test(groups = "database", enabled = true)
public class UserRepositoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepositoryTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;
    private static final String TEST_EMAIL = "testuser_repo@omiinqa.test";

    private UserRepository repo;

    @BeforeClass
    public void setUpRepository() {
        repo = new UserRepository(DB);
        LOG.info("UserRepositoryTest: repository initialised for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        // Best-effort cleanup — delete test row if it was inserted.
        repo.findByEmail(TEST_EMAIL).ifPresent(u -> repo.deleteById(u.getId()));
        LOG.debug("UserRepositoryTest: cleanup complete");
    }

    @Test(groups = "database", description = "count() returns a non-negative long from the users table")
    public void countReturnsNonNegativeValue() {
        final long count = repo.count();
        assertThat(count)
                .as("users table row count should be >= 0")
                .isGreaterThanOrEqualTo(0L);
    }

    @Test(groups = "database", description = "insert() creates a row; findByEmail() retrieves it")
    public void insertAndFindByEmail() {
        final int inserted = repo.insert("Alice Test", TEST_EMAIL, "active");
        assertThat(inserted).as("insert should affect exactly 1 row").isEqualTo(1);

        final Optional<UserRecord> found = repo.findByEmail(TEST_EMAIL);
        assertThat(found).as("newly inserted user should be findable by email").isPresent();

        final UserRecord user = found.get();
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.getName()).isEqualTo("Alice Test");
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(user.getId()).as("id should be database-generated positive value").isPositive();
    }

    @Test(groups = "database", description = "findById() returns the correct record after insert")
    public void findByIdMatchesInsertedRow() {
        repo.insert("Bob Test", TEST_EMAIL, "inactive");
        final Optional<UserRecord> byEmail = repo.findByEmail(TEST_EMAIL);
        assertThat(byEmail).isPresent();

        final long id = byEmail.get().getId();
        final Optional<UserRecord> byId = repo.findById(id);
        assertThat(byId).as("findById should return the same record as findByEmail").isPresent();
        assertThat(byId.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test(groups = "database", description = "findAll() returns a list and contains at least the inserted row")
    public void findAllContainsInsertedRow() {
        repo.insert("Charlie Test", TEST_EMAIL, "active");
        final List<UserRecord> all = repo.findAll();
        assertThat(all).as("findAll should return at least the inserted row").isNotEmpty();
        assertThat(all)
                .extracting(UserRecord::getEmail)
                .contains(TEST_EMAIL);
    }

    @Test(groups = "database", description = "deleteById() removes the row; subsequent findByEmail returns empty")
    public void deleteByIdRemovesRow() {
        repo.insert("Dave Test", TEST_EMAIL, "active");
        final long id = repo.findByEmail(TEST_EMAIL).map(UserRecord::getId).orElseThrow();

        final int deleted = repo.deleteById(id);
        assertThat(deleted).as("deleteById should report 1 row affected").isEqualTo(1);

        final Optional<UserRecord> afterDelete = repo.findByEmail(TEST_EMAIL);
        assertThat(afterDelete).as("row should be gone after deleteById").isEmpty();
    }

    @Test(groups = "database", description = "findById for non-existent id returns Optional.empty")
    public void findByNonExistentIdReturnsEmpty() {
        final Optional<UserRecord> result = repo.findById(Long.MAX_VALUE);
        assertThat(result)
                .as("findById with a non-existent id should return Optional.empty")
                .isEmpty();
    }

    @Test(groups = "database", description = "count increases by 1 after insert")
    public void countIncrementsAfterInsert() {
        final long before = repo.count();
        repo.insert("Eve Test", TEST_EMAIL, "active");
        final long after = repo.count();
        assertThat(after)
                .as("row count should increase by exactly 1 after a single insert")
                .isEqualTo(before + 1);
    }
}
