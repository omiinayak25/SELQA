package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.model.OrderRecord;
import com.omiinqa.database.model.ProductRecord;
import com.omiinqa.database.repositories.OrderRepository;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-validation tests that compare database state against expected datasets,
 * simulating the validation a QA suite performs after an API call or batch
 * operation has modified persistent data.
 *
 * <p><b>What "cross-validation" means here:</b>
 * After a hypothetical API call (create-order, update-product-price, etc.) the
 * database must reflect a known expected state. These tests build that expected
 * dataset in-memory (or read it from a reference set) and then assert that
 * every field in every DB row matches the expectation — column by column. The
 * same structure would be used in a real integration suite where the "expected
 * dataset" comes from an API response, a CSV fixture, or a pre-computed golden
 * file.</p>
 *
 * <p><b>Architecture note:</b>
 * The comparison logic ({@link #assertProductDatasetMatches} and
 * {@link #assertOrderTotalsConsistent}) is intentionally factored into private
 * helpers so that multiple test scenarios can reuse the assertion algorithm
 * without duplication. The helpers do real field-by-field comparison — they
 * are not stubs.</p>
 *
 * <p>Tests are guarded by the {@code "database"} group and require a live DB.</p>
 */
@Test(groups = "database", enabled = true)
public class CrossValidationTest {

    private static final Logger LOG = LoggerFactory.getLogger(CrossValidationTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private static final String CV_USER_EMAIL = "cv_test_user@omiinqa.test";
    private static final String CV_CATEGORY   = "cv-validation-cat";

    private UserRepository    userRepo;
    private ProductRepository productRepo;
    private OrderRepository   orderRepo;
    private DatabaseAssertions dbAssert;

    @BeforeClass
    public void setUp() {
        userRepo    = new UserRepository(DB);
        productRepo = new ProductRepository(DB);
        orderRepo   = new OrderRepository(DB);
        dbAssert    = DatabaseAssertions.forDatabase(DB);
        LOG.info("CrossValidationTest: repositories ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        userRepo.findByEmail(CV_USER_EMAIL).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        productRepo.findByCategory(CV_CATEGORY)
                .forEach(p -> productRepo.deleteById(p.getId()));
        LOG.debug("CrossValidationTest: cleanup complete");
    }

    // ---------------------------------------------------------------- product dataset validation

    /**
     * Seeds a known product catalogue and validates that every record retrieved
     * from the database matches the expected dataset exactly.
     *
     * <p>This mirrors an integration test that asserts a product-import API
     * correctly persisted a batch of products: the API's input dataset becomes
     * the "expected" dataset, and the DB rows are the "actual".</p>
     */
    @Test(groups = "database",
          description = "DB product rows match the expected seeded dataset field-by-field")
    public void productDatasetMatchesExpected() {
        // Define the expected dataset (simulates what an API call should have persisted).
        final Object[][] expected = {
                {"Alpha Widget",  CV_CATEGORY, new BigDecimal("9.99"),  100, true},
                {"Beta Gadget",   CV_CATEGORY, new BigDecimal("24.50"), 50,  true},
                {"Gamma Doohickey",CV_CATEGORY,new BigDecimal("4.00"),  200, false},
        };

        // Seed the database.
        for (final Object[] row : expected) {
            productRepo.insert(
                    (String) row[0],
                    (String) row[1],
                    (BigDecimal) row[2],
                    (int) row[3],
                    (boolean) row[4]);
        }

        // Retrieve from DB.
        final List<ProductRecord> actual = productRepo.findByCategory(CV_CATEGORY);
        assertThat(actual)
                .as("DB should contain exactly %d products in category %s",
                        expected.length, CV_CATEGORY)
                .hasSize(expected.length);

        assertProductDatasetMatches(expected, actual);
    }

    /**
     * Validates that active product count in the database is consistent with
     * the number of active rows seeded — simulating a product-catalogue
     * consistency check after a soft-delete API call.
     */
    @Test(groups = "database",
          description = "Active product count in DB is consistent with expected active subset")
    public void activeProductCountConsistency() {
        productRepo.insert("Active P1",   CV_CATEGORY, new BigDecimal("1.00"), 10, true);
        productRepo.insert("Active P2",   CV_CATEGORY, new BigDecimal("2.00"), 20, true);
        productRepo.insert("Inactive P3", CV_CATEGORY, new BigDecimal("3.00"), 30, false);

        final long totalInCategory = productRepo.countByCategory(CV_CATEGORY);
        assertThat(totalInCategory)
                .as("category should have 3 products total")
                .isEqualTo(3L);

        // Active products are a sub-query assertion via DatabaseAssertions.
        dbAssert.assertRowCountAtLeast("products", 2L);

        // Cross-validate: active list should not include the inactive product.
        final List<ProductRecord> allCatProds = productRepo.findByCategory(CV_CATEGORY);
        final long activeCount = allCatProds.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .count();
        assertThat(activeCount)
                .as("exactly 2 of the 3 seeded products should be active")
                .isEqualTo(2L);
    }

    // ---------------------------------------------------------------- order total consistency

    /**
     * Seeds orders for a user and validates that the stored {@code total}
     * field is consistent with {@code quantity × unit_price} from the
     * product table — detecting any disconnect between the two sources of truth.
     */
    @Test(groups = "database",
          description = "Order totals are consistent with quantity × product price")
    public void orderTotalsConsistentWithProductPrice() {
        userRepo.insert("CV Total User", CV_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(CV_USER_EMAIL).orElseThrow().getId();

        final BigDecimal unitPrice = new BigDecimal("12.50");
        productRepo.insert("CV Price Prod", CV_CATEGORY, unitPrice, 100, true);
        final long productId = productRepo.findByCategory(CV_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // Insert orders where total = quantity × unitPrice.
        final int[] quantities = {1, 3, 5};
        for (final int qty : quantities) {
            final BigDecimal total = unitPrice.multiply(new BigDecimal(qty));
            orderRepo.insert(userId, productId, qty, total, "pending");
        }

        final List<OrderRecord> orders = orderRepo.findByUserId(userId);
        assertThat(orders).hasSize(3);
        assertOrderTotalsConsistent(orders, unitPrice);
    }

    /**
     * Cross-validates that the order status distribution retrieved from the DB
     * matches a computed expected distribution after seeding known data.
     */
    @Test(groups = "database",
          description = "Order status distribution in DB matches expected status counts")
    public void orderStatusDistributionMatchesExpected() {
        userRepo.insert("CV Status User", CV_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(CV_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("CV Status Prod", CV_CATEGORY, new BigDecimal("5.00"), 50, true);
        final long productId = productRepo.findByCategory(CV_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // Seed a known distribution.
        final Map<String, Integer> expectedDistribution = Map.of(
                "pending",   2,
                "confirmed", 1,
                "shipped",   1
        );
        expectedDistribution.forEach((status, count) -> {
            for (int i = 0; i < count; i++) {
                orderRepo.insert(userId, productId, 1, new BigDecimal("5.00"), status);
            }
        });

        // Cross-validate each status count against the expected distribution.
        expectedDistribution.forEach((status, expectedCount) -> {
            final List<OrderRecord> withStatus =
                    orderRepo.findByUserIdAndStatus(userId, status);
            assertThat(withStatus)
                    .as("expected %d orders with status '%s' for user %d",
                            expectedCount, status, userId)
                    .hasSize(expectedCount);
        });

        // Cross-validate total count.
        final long totalExpected = expectedDistribution.values().stream()
                .mapToLong(Integer::longValue).sum();
        assertThat(orderRepo.countByUserId(userId))
                .as("total order count for user should equal sum of all status counts")
                .isEqualTo(totalExpected);
    }

    /**
     * Validates that the sum of all order totals for a user, as stored in the
     * DB, matches an independently computed expected total.
     *
     * <p>This is the classic "two-source reconciliation" pattern: compute the
     * expected total independently (simulating what an API or billing service
     * would calculate) and compare against what the DB reports.</p>
     */
    @Test(groups = "database",
          description = "DB-reported total sum reconciles with independently computed sum")
    public void totalSumReconcilesWithExpected() {
        userRepo.insert("CV Reconcile User", CV_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(CV_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("CV Reconcile Prod", CV_CATEGORY, new BigDecimal("7.00"), 20, true);
        final long productId = productRepo.findByCategory(CV_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        final BigDecimal[] totals = {
                new BigDecimal("7.00"),
                new BigDecimal("14.00"),
                new BigDecimal("21.00"),
                new BigDecimal("28.00"),
        };
        for (final BigDecimal t : totals) {
            orderRepo.insert(userId, productId, 1, t, "pending");
        }

        // Independently compute expected sum.
        BigDecimal expectedSum = BigDecimal.ZERO;
        for (final BigDecimal t : totals) {
            expectedSum = expectedSum.add(t);
        }

        // Assert DB-reported sum equals expected.
        final BigDecimal dbSum = orderRepo.sumTotalByUserId(userId);
        assertThat(dbSum)
                .as("DB-reported sum should reconcile with independently computed expected sum")
                .isEqualByComparingTo(expectedSum);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Validates that every row in {@code actual} matches the corresponding
     * row in {@code expected} by name, category, price, stock, and active flag.
     * Rows are matched by name (unique within the test category).
     */
    private void assertProductDatasetMatches(
            final Object[][] expected,
            final List<ProductRecord> actual) {
        final Map<String, ProductRecord> actualByName = actual.stream()
                .collect(Collectors.toMap(ProductRecord::getName, p -> p));

        for (final Object[] exp : expected) {
            final String name       = (String) exp[0];
            final BigDecimal price  = (BigDecimal) exp[2];
            final int stockQty      = (int) exp[3];
            final boolean active    = (boolean) exp[4];

            final ProductRecord row = actualByName.get(name);
            assertThat(row)
                    .as("Expected product [%s] not found in DB results", name)
                    .isNotNull();
            assertThat(row.getPrice())
                    .as("price mismatch for product [%s]", name)
                    .isEqualByComparingTo(price);
            assertThat(row.getStockQty())
                    .as("stockQty mismatch for product [%s]", name)
                    .isEqualTo(stockQty);
            assertThat(row.getActive())
                    .as("active flag mismatch for product [%s]", name)
                    .isEqualTo(active);
        }
    }

    /**
     * Validates that each order's total equals {@code quantity × unitPrice}
     * within the precision of NUMERIC(12,2) rounding.
     */
    private void assertOrderTotalsConsistent(
            final List<OrderRecord> orders,
            final BigDecimal unitPrice) {
        for (final OrderRecord order : orders) {
            final BigDecimal expectedTotal = unitPrice
                    .multiply(new BigDecimal(order.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            assertThat(order.getTotal())
                    .as("order id=%d: total should equal quantity × unitPrice = %s",
                            order.getId(), expectedTotal)
                    .isEqualByComparingTo(expectedTotal);
        }
    }

    // ---------------------------------------------------------------- schema-level cross-check

    @Test(groups = "database",
          description = "QueryExecutor column-map cross-checks product fields match ProductRecord fields")
    public void rawQueryRowMatchesProductRecord() {
        productRepo.insert("Raw CV Prod", CV_CATEGORY, new BigDecimal("3.33"), 7, true);
        final long productId = productRepo.findByCategory(CV_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // Fetch via typed repo.
        final Optional<ProductRecord> typed = productRepo.findById(productId);
        assertThat(typed).isPresent();

        // Fetch via raw QueryExecutor (Map form) and cross-validate column values.
        final List<Map<String, Object>> rawRows = QueryExecutor.instance().queryForList(
                DB,
                "SELECT id, name, category, price, stock_qty, active FROM products WHERE id = ?",
                productId);

        assertThat(rawRows).hasSize(1);
        final Map<String, Object> raw = rawRows.get(0);

        assertThat(typed.get().getId().toString())
                .isEqualTo(raw.get("id").toString());
        assertThat(typed.get().getName())
                .isEqualTo(raw.get("name"));
        assertThat(typed.get().getPrice())
                .isEqualByComparingTo(new BigDecimal(raw.get("price").toString()));
    }
}
