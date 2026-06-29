package com.omiinqa.database;

import com.omiinqa.database.repositories.UserRepository;
import com.omiinqa.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link TransactionManager} (commit / rollback paths).
 *
 * <p>Tests are guarded by {@code "database"} group and require a live PostgreSQL
 * instance. The suite verifies that:</p>
 * <ul>
 *   <li>Successful transactions are committed and data is visible afterwards.</li>
 *   <li>Transactions are rolled back when the unit-of-work throws a
 *       {@link RuntimeException}, leaving the database unchanged.</li>
 *   <li>The connection is always returned to the pool (auto-commit restored)
 *       regardless of outcome.</li>
 * </ul>
 */
@Test(groups = "database", enabled = true)
public class TransactionTest {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;
    private static final String COMMIT_EMAIL   = "tx_commit@omiinqa.test";
    private static final String ROLLBACK_EMAIL = "tx_rollback@omiinqa.test";

    private TransactionManager txManager;
    private QueryExecutor executor;
    private UserRepository repo;

    @BeforeClass
    public void setUp() {
        txManager = TransactionManager.instance();
        executor  = QueryExecutor.instance();
        repo      = new UserRepository(DB);
        LOG.info("TransactionTest: infrastructure ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        executor.update(DB, "DELETE FROM users WHERE email IN (?, ?)",
                COMMIT_EMAIL, ROLLBACK_EMAIL);
    }

    // ---------------------------------------------------------------- commit path

    @Test(groups = "database",
          description = "Successful unit-of-work is committed; data visible after transaction")
    public void successfulTransactionIsCommitted() {
        final int rows = txManager.executeInTransaction(DB, (Connection conn) -> {
            return executor.update(DB,
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                    "TX Commit User", COMMIT_EMAIL, "active");
        });

        assertThat(rows).as("INSERT inside transaction should affect 1 row").isEqualTo(1);

        final Optional<?> found = repo.findByEmail(COMMIT_EMAIL);
        assertThat(found)
                .as("committed row should be visible outside the transaction")
                .isPresent();
    }

    @Test(groups = "database",
          description = "Multi-statement transaction commits atomically")
    public void multiStatementTransactionCommitsAtomically() {
        txManager.executeInTransaction(DB, (Connection conn) -> {
            executor.update(DB,
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                    "TX Multi User", COMMIT_EMAIL, "pending");
            executor.update(DB,
                    "UPDATE users SET status = ? WHERE email = ?",
                    "active", COMMIT_EMAIL);
            return null;
        });

        final Optional<com.omiinqa.database.model.UserRecord> user =
                repo.findByEmail(COMMIT_EMAIL);
        assertThat(user).isPresent();
        assertThat(user.get().getStatus())
                .as("status after multi-statement transaction should be 'active'")
                .isEqualTo("active");
    }

    // ---------------------------------------------------------------- rollback path

    @Test(groups = "database",
          description = "RuntimeException inside unit-of-work triggers rollback; data unchanged")
    public void runtimeExceptionTriggersRollback() {
        assertThatThrownBy(() ->
            txManager.executeInTransaction(DB, (Connection conn) -> {
                executor.update(DB,
                        "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                        "TX Rollback User", ROLLBACK_EMAIL, "active");
                // Deliberately simulate a business-logic failure mid-transaction.
                throw new IllegalStateException("Simulated failure — rollback expected");
            })
        ).isInstanceOf(DatabaseException.class)
         .hasMessageContaining("Simulated failure");

        final Optional<?> notFound = repo.findByEmail(ROLLBACK_EMAIL);
        assertThat(notFound)
                .as("rolled-back INSERT should not be visible in the database")
                .isEmpty();
    }

    @Test(groups = "database",
          description = "After rollback, another transaction can succeed on the same connection")
    public void connectionIsCleanAfterRollback() {
        // Step 1: trigger a rollback.
        try {
            txManager.executeInTransaction(DB, (Connection conn) -> {
                throw new RuntimeException("Intentional rollback trigger");
            });
        } catch (final DatabaseException ignored) {
            // Expected — we just want the connection returned to pool.
        }

        // Step 2: verify pool is still healthy by running a successful transaction.
        txManager.executeInTransaction(DB, (Connection conn) -> {
            executor.update(DB,
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                    "TX Post-Rollback User", COMMIT_EMAIL, "active");
            return null;
        });

        assertThat(repo.findByEmail(COMMIT_EMAIL))
                .as("post-rollback transaction should succeed; connection pool unaffected")
                .isPresent();
    }

    // ---------------------------------------------------------------- consumer overload

    @Test(groups = "database",
          description = "Consumer overload (void unit-of-work) commits without return value")
    public void consumerOverloadCommitsVoidWork() {
        txManager.executeInTransaction(DB, (Connection conn) ->
            executor.update(DB,
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
                    "TX Consumer User", COMMIT_EMAIL, "active")
        );

        assertThat(repo.findByEmail(COMMIT_EMAIL))
                .as("void-consumer transaction should commit and be visible")
                .isPresent();
    }
}
