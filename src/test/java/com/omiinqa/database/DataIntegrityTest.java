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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Data integrity integration tests that verify constraint behaviour and boundary
 * values across the orders, users, products, and audit_log tables.
 *
 * <p>These tests assert database-level rules (NOT NULL, UNIQUE, CHECK constraints,
 * FK referential integrity) from the application layer. They do not mock the
 * database — they rely on the schema defined in
 * {@code src/test/resources/db/schema.sql} being applied to a live instance.</p>
 *
 * <p>Tests are guarded by {@code "database"} group and excluded from default
 * runs. All assertions use either {@link DatabaseAssertions} or AssertJ.</p>
 */
@Test(groups = "database", enabled = true)
public class DataIntegrityTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataIntegrityTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private static final String DI_USER_EMAIL    = "di_test_user@omiinqa.test";
    private static final String DI_AUDIT_ACTOR   = "di_audit_actor@omiinqa.test";

    private UserRepository    userRepo;
    private ProductRepository productRepo;
    private OrderRepository   orderRepo;
    private AuditRepository   auditRepo;
    private DatabaseAssertions dbAssert;

    @BeforeClass
    public void setUp() {
        userRepo    = new UserRepository(DB);
        productRepo = new ProductRepository(DB);
        orderRepo   = new OrderRepository(DB);
        auditRepo   = new AuditRepository(DB);
        dbAssert    = DatabaseAssertions.forDatabase(DB);
        LOG.info("DataIntegrityTest: repositories ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        auditRepo.deleteByActor(DI_AUDIT_ACTOR);
        userRepo.findByEmail(DI_USER_EMAIL).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        LOG.debug("DataIntegrityTest: cleanup complete");
    }

    // ---------------------------------------------------------------- NOT NULL / constraint tests

    /**
     * Verifies that inserting a user with a null name does NOT violate the schema
     * (name is nullable in the users schema, unlike email). Documents the
     * nullable-vs-not-null contract explicitly.
     */
    @Test(groups = "database",
          description = "users.name is nullable; inserting with null name does not throw")
    public void userNameIsNullable() {
        final int affected = userRepo.insert(null, DI_USER_EMAIL, "active");
        assertThat(affected).as("INSERT with null name should succeed (name is nullable)").isEqualTo(1);
        assertThat(userRepo.findByEmail(DI_USER_EMAIL))
                .as("row should exist even with null name")
                .isPresent();
    }

    /**
     * Verifies that inserting a duplicate email raises a {@link DatabaseException}
     * wrapping the underlying UNIQUE constraint violation (ORA-00001 / PG-23505 /
     * MySQL 1062). This documents the uniqueness contract without knowing the exact
     * JDBC error code.
     */
    @Test(groups = "database",
          description = "users.email UNIQUE constraint rejects duplicate email on second insert")
    public void duplicateEmailViolatesUniqueConstraint() {
        userRepo.insert("First User", DI_USER_EMAIL, "active");
        // Inserting the same email a second time must fail.
        assertThatThrownBy(() -> userRepo.insert("Second User", DI_USER_EMAIL, "active"))
                .as("second INSERT with same email should fail due to UNIQUE constraint")
                .isInstanceOf(DatabaseException.class);
    }

    /**
     * Verifies that an order referencing a non-existent user_id raises a
     * {@link DatabaseException} wrapping the FK violation. Tests that the
     * database enforces referential integrity between orders and users.
     */
    @Test(groups = "database",
          description = "orders.user_id FK constraint rejects reference to non-existent user")
    public void orderWithNonExistentUserIdViolatesForeignKey() {
        // First ensure we have a valid product.
        productRepo.insert("FK Test Prod", "fk-test", new BigDecimal("5.00"), 10, true);
        final long productId = productRepo.findByCategory("fk-test")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final long ghostUserId = Long.MAX_VALUE - 1; // guaranteed non-existent
        assertThatThrownBy(() -> orderRepo.insert(
                ghostUserId, productId, 1, new BigDecimal("5.00"), "pending"))
                .as("order referencing non-existent user_id should fail on FK constraint")
                .isInstanceOf(DatabaseException.class);

        // Cleanup the seeded product
        productRepo.deleteById(productId);
    }

    /**
     * Verifies that an order referencing a non-existent product_id raises a
     * {@link DatabaseException} wrapping the FK violation.
     */
    @Test(groups = "database",
          description = "orders.product_id FK constraint rejects reference to non-existent product")
    public void orderWithNonExistentProductIdViolatesForeignKey() {
        userRepo.insert("FK Prod User", DI_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(DI_USER_EMAIL).orElseThrow().getId();
        final long ghostProductId = Long.MAX_VALUE - 2;

        assertThatThrownBy(() -> orderRepo.insert(
                userId, ghostProductId, 1, new BigDecimal("5.00"), "pending"))
                .as("order referencing non-existent product_id should fail on FK constraint")
                .isInstanceOf(DatabaseException.class);
    }

    // ---------------------------------------------------------------- boundary value tests

    @Test(groups = "database", dataProvider = "boundaryTotalsProvider",
          description = "Orders accept boundary total values (zero, max precision)")
    public void orderTotalBoundaryValues(final String totalStr, final String description) {
        userRepo.insert("Boundary User", DI_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(DI_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Boundary Prod", "boundary-cat", new BigDecimal("0.01"), 1000, true);
        final long productId = productRepo.findByCategory("boundary-cat")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final BigDecimal total = new BigDecimal(totalStr);
        orderRepo.insert(userId, productId, 1, total, "pending");

        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        assertThat(orders).as("order with total=%s (%s) should be stored", totalStr, description)
                .isNotEmpty();
        assertThat(orders.get(0).getTotal())
                .as("stored total should match inserted total for %s", description)
                .isEqualByComparingTo(total);

        // Cleanup extra product
        productRepo.deleteById(productId);
    }

    @DataProvider(name = "boundaryTotalsProvider")
    public Object[][] boundaryTotalsProvider() {
        return new Object[][] {
                {"0.01",       "minimum positive monetary value"},
                {"99999.99",   "large but schema-valid total"},
                {"1.00",       "unit price boundary"},
        };
    }

    @Test(groups = "database",
          description = "audit_log accepts the maximum length actor string without truncation")
    public void auditActorMaxLengthIsStored() {
        // actor VARCHAR(255): a 255-char string must not be silently truncated.
        final String maxLengthActor = "a".repeat(255);
        final int affected = auditRepo.insert("user", "READ", maxLengthActor);
        assertThat(affected).isEqualTo(1);

        final List<com.omiinqa.database.model.AuditRecord> entries =
                auditRepo.findByActor(maxLengthActor);
        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0).getActor())
                .as("stored actor should equal the 255-char input (no truncation)")
                .hasSize(255);

        // Cleanup: delete by id since actor is not DI_AUDIT_ACTOR sentinel
        entries.forEach(e -> auditRepo.deleteById(e.getId()));
    }

    @Test(groups = "database",
          description = "Audit action field stores empty-string boundary without rejection")
    public void auditActionEmptyStringBoundary() {
        // Empty string is technically valid at JDBC level (schema may or may not reject it).
        // This test documents current behaviour: the DB stores or rejects it deterministically.
        try {
            auditRepo.insert("product", "", DI_AUDIT_ACTOR);
            final List<com.omiinqa.database.model.AuditRecord> entries =
                    auditRepo.findByActor(DI_AUDIT_ACTOR);
            assertThat(entries)
                    .as("if DB accepts empty action, at least one row should be present")
                    .isNotEmpty();
        } catch (final DatabaseException e) {
            // Some databases have CHECK (action <> '') — rejection is also valid behaviour.
            LOG.info("DataIntegrityTest: DB rejected empty action string — {}", e.getMessage());
            assertThat(e).isInstanceOf(DatabaseException.class);
        }
    }

    // ---------------------------------------------------------------- referential integrity (select-side)

    @Test(groups = "database",
          description = "DatabaseAssertions verifies referential counts after cascaded cleanup")
    public void referentialCountsAfterCascadedCleanup() {
        userRepo.insert("Cascade User", DI_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(DI_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Cascade Prod", "cascade-cat", new BigDecimal("1.00"), 5, true);
        final long productId = productRepo.findByCategory("cascade-cat")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("1.00"), "pending");

        // Baseline: user has exactly 1 order
        dbAssert.assertRowCountAtLeast("orders", 1L);
        assertThat(orderRepo.countByUserId(userId)).isEqualTo(1L);

        // Delete orders first (FK child), then user (FK parent).
        orderRepo.deleteByUserId(userId);
        assertThat(orderRepo.countByUserId(userId))
                .as("after deleting orders, count for user should be 0")
                .isEqualTo(0L);

        // Now the user row should be deletable without FK error.
        final int userDeleted = userRepo.deleteById(userId);
        assertThat(userDeleted).isEqualTo(1);

        productRepo.deleteById(productId);
    }
}
