package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.model.OrderRecord;
import com.omiinqa.database.repositories.OrderRepository;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OrderRepository} against a live PostgreSQL instance.
 *
 * <p>These tests are guarded by the {@code "database"} TestNG group and are
 * excluded from the default smoke/regression suites. A running PostgreSQL server
 * configured with the schema in {@code src/test/resources/db/schema.sql} is
 * required.</p>
 *
 * <p>Tests do NOT extend {@link com.omiinqa.core.BaseTest} — no browser or
 * Selenium driver is needed for pure JDBC validation. Each test seeds its own
 * test data and cleans up in {@link #cleanup()} to guarantee predictable state
 * across parallel TestNG threads.</p>
 *
 * <p>All SQL in {@link OrderRepository} uses {@link java.sql.PreparedStatement},
 * so test data values containing SQL metacharacters are handled safely.</p>
 */
@Test(groups = "database", enabled = true)
public class OrderRepositoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(OrderRepositoryTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    // Sentinel values that identify test data owned by this test class.
    private static final String TEST_USER_EMAIL   = "order_test_user@omiinqa.test";
    private static final String TEST_USER_EMAIL_2 = "order_test_user2@omiinqa.test";

    private OrderRepository   orderRepo;
    private UserRepository    userRepo;
    private ProductRepository productRepo;
    private DatabaseAssertions dbAssert;

    @BeforeClass
    public void setUpRepositories() {
        orderRepo   = new OrderRepository(DB);
        userRepo    = new UserRepository(DB);
        productRepo = new ProductRepository(DB);
        dbAssert    = DatabaseAssertions.forDatabase(DB);
        LOG.info("OrderRepositoryTest: repositories initialised for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        // Delete orders first (FK dependency), then users.
        userRepo.findByEmail(TEST_USER_EMAIL).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        userRepo.findByEmail(TEST_USER_EMAIL_2).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        LOG.debug("OrderRepositoryTest: cleanup complete");
    }

    // ---------------------------------------------------------------- helper

    /**
     * Seeds a test user and a product, then inserts one order.
     * Returns an array: [userId, productId, orderId].
     */
    private long[] seedSingleOrder(
            final String userEmail,
            final int quantity,
            final BigDecimal total,
            final String status) {
        userRepo.insert("Order Test User", userEmail, "active");
        final long userId = userRepo.findByEmail(userEmail).orElseThrow().getId();

        productRepo.insert("Test Product", "test-category",
                new BigDecimal("9.99"), 100, true);
        // Re-query last inserted product for this test user – use count heuristic
        // by finding active product in test category.
        final List<com.omiinqa.database.model.ProductRecord> prods =
                productRepo.findByCategory("test-category");
        final long productId = prods.get(prods.size() - 1).getId();

        orderRepo.insert(userId, productId, quantity, total, status);
        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        final long orderId = orders.get(0).getId();
        return new long[]{userId, productId, orderId};
    }

    // ---------------------------------------------------------------- CRUD tests

    @Test(groups = "database",
          description = "insert() creates a row; findById() retrieves it with correct fields")
    public void insertAndFindById() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 2, new BigDecimal("19.98"), "pending");
        final long orderId = ids[2];

        final Optional<OrderRecord> found = orderRepo.findById(orderId);
        assertThat(found).as("newly inserted order should be findable by id").isPresent();

        final OrderRecord order = found.get();
        assertThat(order.getId()).isPositive();
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getTotal()).isEqualByComparingTo(new BigDecimal("19.98"));
        assertThat(order.getStatus()).isEqualTo("pending");
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test(groups = "database",
          description = "findById() returns Optional.empty for non-existent primary key")
    public void findByNonExistentIdReturnsEmpty() {
        final Optional<OrderRecord> result = orderRepo.findById(Long.MAX_VALUE);
        assertThat(result)
                .as("findById with a fabricated id should return Optional.empty")
                .isEmpty();
    }

    @Test(groups = "database",
          description = "updateStatus() changes status; findById() reflects the new value")
    public void updateStatusChangesField() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 1, new BigDecimal("9.99"), "pending");
        final long orderId = ids[2];

        final int affected = orderRepo.updateStatus(orderId, "confirmed");
        assertThat(affected).as("updateStatus should affect 1 row").isEqualTo(1);

        final OrderRecord updated = orderRepo.findById(orderId).orElseThrow();
        assertThat(updated.getStatus())
                .as("status should reflect the updated value")
                .isEqualTo("confirmed");
    }

    @Test(groups = "database",
          description = "deleteById() removes the row; subsequent findById() returns empty")
    public void deleteByIdRemovesOrder() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 3, new BigDecimal("29.97"), "pending");
        final long orderId = ids[2];

        final int deleted = orderRepo.deleteById(orderId);
        assertThat(deleted).as("deleteById should report 1 row affected").isEqualTo(1);

        final Optional<OrderRecord> afterDelete = orderRepo.findById(orderId);
        assertThat(afterDelete).as("order should be absent after deleteById").isEmpty();
    }

    @Test(groups = "database",
          description = "count() returns a non-negative long from the orders table")
    public void countReturnsNonNegativeValue() {
        final long count = orderRepo.count();
        assertThat(count)
                .as("orders table row count should be >= 0")
                .isGreaterThanOrEqualTo(0L);
    }

    @Test(groups = "database",
          description = "count increments by 1 after a single insert")
    public void countIncrementsAfterInsert() {
        userRepo.insert("Count Test User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Count Prod", "test-category", new BigDecimal("5.00"), 10, true);
        final long productId = productRepo.findByCategory("test-category")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final long before = orderRepo.count();
        orderRepo.insert(userId, productId, 1, new BigDecimal("5.00"), "pending");
        final long after = orderRepo.count();

        assertThat(after)
                .as("count should increase by exactly 1 after a single insert")
                .isEqualTo(before + 1);
    }

    // ---------------------------------------------------------------- findByUserId

    @Test(groups = "database",
          description = "findByUserId() returns orders for the correct user only")
    public void findByUserIdReturnsCorrectedOrders() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 5, new BigDecimal("49.95"), "pending");
        final long userId = ids[0];

        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        assertThat(orders).as("user should have at least one order").isNotEmpty();
        assertThat(orders)
                .allMatch(o -> o.getUserId().equals(userId),
                        "all returned orders should belong to the test user");
    }

    @Test(groups = "database",
          description = "findByUserId() returns empty list for user with no orders")
    public void findByUserIdReturnsEmptyForUserWithNoOrders() {
        userRepo.insert("No Orders User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();

        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        assertThat(orders)
                .as("user with no orders should return empty list")
                .isEmpty();
    }

    @Test(groups = "database",
          description = "countByUserId() matches the number of inserted orders")
    public void countByUserIdMatchesInsertedOrders() {
        userRepo.insert("Count User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Count Prod2", "test-category", new BigDecimal("3.00"), 50, true);
        final long productId = productRepo.findByCategory("test-category")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("3.00"), "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("6.00"), "pending");

        final long count = orderRepo.countByUserId(userId);
        assertThat(count)
                .as("countByUserId should return 2 after inserting 2 orders")
                .isEqualTo(2L);
    }

    // ---------------------------------------------------------------- findByStatus

    @Test(groups = "database", dataProvider = "statusProvider",
          description = "findByStatus() returns only orders with the matching status")
    public void findByStatusFiltersCorrectly(final String status) {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 1, new BigDecimal("10.00"), status);

        final List<OrderRecord> orders = orderRepo.findByStatus(status);
        // The seeded order must appear in the results.
        assertThat(orders)
                .as("findByStatus('%s') should contain the seeded order", status)
                .anyMatch(o -> o.getId().equals(ids[2]));
        assertThat(orders)
                .allMatch(o -> status.equals(o.getStatus()),
                        "every order returned should have status '" + status + "'");
    }

    @DataProvider(name = "statusProvider")
    public Object[][] statusProvider() {
        return new Object[][] {
                {"pending"},
                {"confirmed"},
                {"shipped"},
                {"delivered"},
                {"cancelled"}
        };
    }

    // ---------------------------------------------------------------- findByUserIdAndStatus

    @Test(groups = "database",
          description = "findByUserIdAndStatus() applies both filters correctly")
    public void findByUserIdAndStatusAppliesBothFilters() {
        userRepo.insert("Combo Filter User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Combo Prod", "test-category", new BigDecimal("7.50"), 20, true);
        final long productId = productRepo.findByCategory("test-category")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("7.50"), "pending");
        orderRepo.insert(userId, productId, 1, new BigDecimal("7.50"), "shipped");

        final List<OrderRecord> pending = orderRepo.findByUserIdAndStatus(userId, "pending");
        final List<OrderRecord> shipped = orderRepo.findByUserIdAndStatus(userId, "shipped");

        assertThat(pending).hasSize(1);
        assertThat(shipped).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo("pending");
        assertThat(shipped.get(0).getStatus()).isEqualTo("shipped");
    }

    // ---------------------------------------------------------------- totals

    @Test(groups = "database",
          description = "sumTotalByUserId() returns accurate sum for multiple orders")
    public void sumTotalByUserIdCalculatesCorrectly() {
        userRepo.insert("Sum User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Sum Prod", "test-category", new BigDecimal("5.00"), 30, true);
        final long productId = productRepo.findByCategory("test-category")
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("10.00"), "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("20.00"), "confirmed");
        orderRepo.insert(userId, productId, 3, new BigDecimal("30.00"), "shipped");

        final BigDecimal sum = orderRepo.sumTotalByUserId(userId);
        assertThat(sum)
                .as("sum of 10.00 + 20.00 + 30.00 should equal 60.00")
                .isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test(groups = "database",
          description = "sumTotalByUserId() returns BigDecimal.ZERO for user with no orders")
    public void sumTotalReturnsZeroForUserWithNoOrders() {
        userRepo.insert("Zero Sum User", TEST_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(TEST_USER_EMAIL).orElseThrow().getId();

        final BigDecimal sum = orderRepo.sumTotalByUserId(userId);
        assertThat(sum)
                .as("user with no orders should have a zero sum total")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------- DatabaseAssertions

    @Test(groups = "database",
          description = "DatabaseAssertions confirms order row exists after insert")
    public void databaseAssertionsConfirmOrderExists() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 1, new BigDecimal("15.00"), "pending");

        dbAssert.assertRowExists(
                "SELECT 1 FROM orders WHERE id = ?", ids[2]);
        dbAssert.assertColumnValue(
                "SELECT status FROM orders WHERE id = ?", "pending", ids[2]);
    }

    @Test(groups = "database",
          description = "DatabaseAssertions confirms order row absent after delete")
    public void databaseAssertionsConfirmOrderAbsent() {
        final long[] ids = seedSingleOrder(
                TEST_USER_EMAIL, 1, new BigDecimal("15.00"), "pending");
        orderRepo.deleteById(ids[2]);

        dbAssert.assertNoRowExists(
                "SELECT 1 FROM orders WHERE id = ?", ids[2]);
    }

    @Test(groups = "database",
          description = "findAll() returns a non-null list (possibly empty)")
    public void findAllReturnsNonNullList() {
        final List<OrderRecord> all = orderRepo.findAll();
        assertThat(all)
                .as("findAll() should never return null; an empty list is acceptable")
                .isNotNull();
    }
}
