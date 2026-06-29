package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.model.OrderRecord;
import com.omiinqa.database.repositories.AuditRepository;
import com.omiinqa.database.repositories.OrderRepository;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import com.omiinqa.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for multi-table transaction commit and rollback semantics
 * across the orders and audit_log tables.
 *
 * <p>These tests verify that {@link TransactionManager} correctly enforces
 * atomicity guarantees:</p>
 * <ul>
 *   <li>Both the order insert and the audit log insert commit together, or
 *       neither does (rollback on failure).</li>
 *   <li>A {@link RuntimeException} mid-transaction leaves the database in its
 *       pre-transaction state.</li>
 *   <li>A {@link DatabaseException} also triggers a full rollback.</li>
 *   <li>After a rollback, the connection pool is clean and subsequent
 *       transactions succeed.</li>
 *   <li>Read-committed isolation: changes from a committed transaction are
 *       visible to subsequent separate queries.</li>
 * </ul>
 *
 * <p>Tests are guarded by {@code "database"} group and require a live
 * PostgreSQL instance with the schema from
 * {@code src/test/resources/db/schema.sql}.</p>
 */
@Test(groups = "database", enabled = true)
public class TransactionRollbackTest {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionRollbackTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private static final String TXR_USER_EMAIL  = "txr_test_user@omiinqa.test";
    private static final String TXR_AUDIT_ACTOR = "txr_audit_actor@omiinqa.test";
    private static final String TXR_CATEGORY    = "txr-test-cat";

    private TransactionManager txManager;
    private QueryExecutor      executor;
    private UserRepository     userRepo;
    private ProductRepository  productRepo;
    private OrderRepository    orderRepo;
    private AuditRepository    auditRepo;
    private DatabaseAssertions dbAssert;

    @BeforeClass
    public void setUp() {
        txManager   = TransactionManager.instance();
        executor    = QueryExecutor.instance();
        userRepo    = new UserRepository(DB);
        productRepo = new ProductRepository(DB);
        orderRepo   = new OrderRepository(DB);
        auditRepo   = new AuditRepository(DB);
        dbAssert    = DatabaseAssertions.forDatabase(DB);
        LOG.info("TransactionRollbackTest: all infrastructure ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        auditRepo.deleteByActor(TXR_AUDIT_ACTOR);
        userRepo.findByEmail(TXR_USER_EMAIL).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        productRepo.findByCategory(TXR_CATEGORY)
                .forEach(p -> productRepo.deleteById(p.getId()));
        LOG.debug("TransactionRollbackTest: cleanup complete");
    }

    // ---------------------------------------------------------------- helpers

    private long seedUserAndProduct() {
        userRepo.insert("TxR Test User", TXR_USER_EMAIL, "active");
        productRepo.insert("TxR Prod", TXR_CATEGORY, new BigDecimal("8.00"), 20, true);
        return productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();
    }

    // ---------------------------------------------------------------- commit path (multi-table)

    @Test(groups = "database",
          description = "Multi-table commit: order insert + audit insert both visible after commit")
    public void multiTableCommitBothInsertsVisible() {
        userRepo.insert("TxR Commit User", TXR_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("TxR Commit Prod", TXR_CATEGORY, new BigDecimal("10.00"), 5, true);
        final long productId = productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final long[] orderIdHolder = new long[1];
        txManager.executeInTransaction(DB, (Connection conn) -> {
            // Insert order within transaction.
            executor.update(DB,
                    "INSERT INTO orders (user_id, product_id, quantity, total, status)"
                    + " VALUES (?, ?, ?, ?, ?)",
                    userId, productId, 1, new BigDecimal("10.00"), "pending");

            // Insert audit entry within the same transaction.
            executor.update(DB,
                    "INSERT INTO audit_log (entity, action, actor) VALUES (?, ?, ?)",
                    "order", "CREATE", TXR_AUDIT_ACTOR);
            return null;
        });

        // Both should be visible after commit.
        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        assertThat(orders)
                .as("order should be visible after multi-table commit")
                .hasSize(1);

        final List<com.omiinqa.database.model.AuditRecord> auditEntries =
                auditRepo.findByActor(TXR_AUDIT_ACTOR);
        assertThat(auditEntries)
                .as("audit entry should be visible after multi-table commit")
                .hasSize(1);
        assertThat(auditEntries.get(0).getAction()).isEqualTo("CREATE");
    }

    @Test(groups = "database",
          description = "Committed status update is visible outside the transaction boundary")
    public void committedStatusUpdateIsVisibleOutsideTransaction() {
        final long productId = seedUserAndProduct();
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        orderRepo.insert(userId, productId, 1, new BigDecimal("8.00"), "pending");
        final long orderId = orderRepo.findByUserId(userId).get(0).getId();

        txManager.executeInTransaction(DB, (Connection conn) -> {
            executor.update(DB,
                    "UPDATE orders SET status = ? WHERE id = ?",
                    "confirmed", orderId);
            return null;
        });

        final Optional<OrderRecord> updated = orderRepo.findById(orderId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus())
                .as("status update committed inside transaction should be visible outside")
                .isEqualTo("confirmed");
    }

    // ---------------------------------------------------------------- rollback path

    @Test(groups = "database",
          description = "RuntimeException mid-transaction rolls back both order and audit inserts")
    public void runtimeExceptionRollsBackBothInserts() {
        userRepo.insert("TxR Rollback User", TXR_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("TxR Rollback Prod", TXR_CATEGORY, new BigDecimal("5.00"), 10, true);
        final long productId = productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        assertThatThrownBy(() ->
            txManager.executeInTransaction(DB, (Connection conn) -> {
                executor.update(DB,
                        "INSERT INTO orders (user_id, product_id, quantity, total, status)"
                        + " VALUES (?, ?, ?, ?, ?)",
                        userId, productId, 2, new BigDecimal("10.00"), "pending");
                executor.update(DB,
                        "INSERT INTO audit_log (entity, action, actor) VALUES (?, ?, ?)",
                        "order", "CREATE", TXR_AUDIT_ACTOR);
                // Deliberate failure after both inserts.
                throw new IllegalStateException("Simulated failure — full rollback expected");
            })
        ).isInstanceOf(DatabaseException.class)
         .hasMessageContaining("Simulated failure");

        // Neither insert should be visible.
        assertThat(orderRepo.findByUserId(userId))
                .as("rolled-back order insert should not be visible")
                .isEmpty();

        assertThat(auditRepo.findByActor(TXR_AUDIT_ACTOR))
                .as("rolled-back audit insert should not be visible")
                .isEmpty();
    }

    @Test(groups = "database",
          description = "Partial rollback: first DML done, exception on second, nothing persisted")
    public void partialWorkRollsBackOnSecondStatementFailure() {
        userRepo.insert("TxR Partial User", TXR_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("TxR Partial Prod", TXR_CATEGORY, new BigDecimal("3.00"), 30, true);
        final long productId = productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final long countBefore = orderRepo.countByUserId(userId);

        assertThatThrownBy(() ->
            txManager.executeInTransaction(DB, (Connection conn) -> {
                // First statement succeeds inside the transaction.
                executor.update(DB,
                        "INSERT INTO orders (user_id, product_id, quantity, total, status)"
                        + " VALUES (?, ?, ?, ?, ?)",
                        userId, productId, 1, new BigDecimal("3.00"), "pending");
                // Second statement fails by referencing a non-existent table.
                // This triggers a DatabaseException which the TransactionManager wraps.
                executor.update(DB, "UPDATE nonexistent_table_xyz SET x = 1 WHERE id = ?", 1L);
                return null;
            })
        ).isInstanceOf(DatabaseException.class);

        // The first INSERT must also have been rolled back.
        final long countAfter = orderRepo.countByUserId(userId);
        assertThat(countAfter)
                .as("count after partial rollback should equal count before transaction")
                .isEqualTo(countBefore);
    }

    @Test(groups = "database",
          description = "Connection pool is clean after rollback; next transaction succeeds")
    public void connectionCleanAfterRollback() {
        // Step 1: trigger a rollback.
        try {
            txManager.executeInTransaction(DB, (Connection conn) -> {
                throw new RuntimeException("Intentional pool-cleanliness test trigger");
            });
        } catch (final DatabaseException ignored) {
            // Expected — we only care that the pool is undamaged.
        }

        // Step 2: execute a successful transaction immediately after.
        userRepo.insert("TxR PostRollback User", TXR_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("TxR PostRollback Prod", TXR_CATEGORY, new BigDecimal("1.00"), 10, true);
        final long productId = productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        txManager.executeInTransaction(DB, (Connection conn) -> {
            executor.update(DB,
                    "INSERT INTO orders (user_id, product_id, quantity, total, status)"
                    + " VALUES (?, ?, ?, ?, ?)",
                    userId, productId, 1, new BigDecimal("1.00"), "confirmed");
            return null;
        });

        assertThat(orderRepo.findByUserId(userId))
                .as("transaction after rollback should succeed; pool is clean")
                .hasSize(1);
    }

    // ---------------------------------------------------------------- isolation

    @Test(groups = "database",
          description = "Rolled-back changes are not visible to a concurrent SELECT outside transaction")
    public void rolledBackChangesNotVisibleOutsideTransaction() {
        userRepo.insert("TxR Isolation User", TXR_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TXR_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("TxR Isolation Prod", TXR_CATEGORY, new BigDecimal("6.00"), 5, true);
        final long productId = productRepo.findByCategory(TXR_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // Execute a transaction that rolls back.
        assertThatThrownBy(() ->
            txManager.executeInTransaction(DB, (Connection conn) -> {
                executor.update(DB,
                        "INSERT INTO orders (user_id, product_id, quantity, total, status)"
                        + " VALUES (?, ?, ?, ?, ?)",
                        userId, productId, 10, new BigDecimal("60.00"), "pending");
                throw new RuntimeException("deliberate rollback");
            })
        ).isInstanceOf(DatabaseException.class);

        // A subsequent independent SELECT must see no orders for this user.
        dbAssert.assertNoRowExists(
                "SELECT 1 FROM orders WHERE user_id = ? AND total = ?",
                userId, new BigDecimal("60.00"));
    }

    @Test(groups = "database",
          description = "Consumer overload (void) commits audit insert; visible after transaction")
    public void consumerOverloadCommitsAuditInsert() {
        txManager.executeInTransaction(DB,
                (Connection conn) -> executor.update(DB,
                        "INSERT INTO audit_log (entity, action, actor) VALUES (?, ?, ?)",
                        "user", "DELETE", TXR_AUDIT_ACTOR));

        assertThat(auditRepo.findByActor(TXR_AUDIT_ACTOR))
                .as("void-consumer overload should commit; audit entry must be visible")
                .hasSize(1);
        assertThat(auditRepo.findByActor(TXR_AUDIT_ACTOR).get(0).getAction())
                .isEqualTo("DELETE");
    }
}
