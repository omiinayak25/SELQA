package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.repositories.AuditRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for aggregate SQL (COUNT, SUM, AVG, GROUP BY) executed via
 * {@link QueryExecutor} and the repository {@code count*} / {@code sumTotal*} helpers.
 *
 * <p>These tests exercise the scalar and list-of-maps query paths of
 * {@link QueryExecutor} directly, complementing the typed-repository tests in
 * {@link OrderRepositoryTest}. They prove that raw aggregate SQL is correctly
 * transmitted and that the results are accurately parsed by the executor.</p>
 *
 * <p>Tests are guarded by {@code "database"} group and excluded from default
 * runs. All SQL uses {@code ?} placeholders — no string concatenation.</p>
 */
@Test(groups = "database", enabled = true)
public class AggregationQueryTest {

    private static final Logger LOG = LoggerFactory.getLogger(AggregationQueryTest.class);

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private static final String AGG_USER_EMAIL   = "agg_test_user@omiinqa.test";
    private static final String AGG_AUDIT_ACTOR  = "agg_audit_actor@omiinqa.test";
    private static final String AGG_CATEGORY     = "agg-test-cat";

    private QueryExecutor     executor;
    private UserRepository    userRepo;
    private ProductRepository productRepo;
    private OrderRepository   orderRepo;
    private AuditRepository   auditRepo;
    private DatabaseAssertions dbAssert;

    @BeforeClass
    public void setUp() {
        executor    = QueryExecutor.instance();
        userRepo    = new UserRepository(DB);
        productRepo = new ProductRepository(DB);
        orderRepo   = new OrderRepository(DB);
        auditRepo   = new AuditRepository(DB);
        dbAssert    = DatabaseAssertions.forDatabase(DB);
        LOG.info("AggregationQueryTest: ready for {}", DB);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        auditRepo.deleteByActor(AGG_AUDIT_ACTOR);
        userRepo.findByEmail(AGG_USER_EMAIL).ifPresent(u -> {
            orderRepo.deleteByUserId(u.getId());
            userRepo.deleteById(u.getId());
        });
        productRepo.findByCategory(AGG_CATEGORY)
                .forEach(p -> productRepo.deleteById(p.getId()));
        LOG.debug("AggregationQueryTest: cleanup complete");
    }

    // ---------------------------------------------------------------- COUNT via scalar

    @Test(groups = "database",
          description = "COUNT(*) via queryForScalar returns a non-negative value")
    public void countStarFromOrdersViScalar() {
        final Optional<Object> result = executor.queryForScalar(
                DB, "SELECT COUNT(*) FROM orders");
        assertThat(result).isPresent();
        assertThat(((Number) result.get()).longValue())
                .as("COUNT(*) on orders table should be >= 0")
                .isGreaterThanOrEqualTo(0L);
    }

    @Test(groups = "database",
          description = "COUNT with WHERE clause reduces result correctly")
    public void countWithWhereClauseFiltersRows() {
        userRepo.insert("Agg Count User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg Count Prod", AGG_CATEGORY, new BigDecimal("2.00"), 50, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("2.00"), "pending");
        orderRepo.insert(userId, productId, 1, new BigDecimal("2.00"), "pending");
        orderRepo.insert(userId, productId, 1, new BigDecimal("2.00"), "shipped");

        final Optional<Object> pendingCount = executor.queryForScalar(
                DB,
                "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = ?",
                userId, "pending");

        assertThat(pendingCount).isPresent();
        assertThat(((Number) pendingCount.get()).longValue())
                .as("COUNT of pending orders for user should be 2")
                .isEqualTo(2L);
    }

    @Test(groups = "database",
          description = "Parameterised COUNT matches repository countByUserId()")
    public void rawCountMatchesRepositoryCount() {
        userRepo.insert("Agg Match User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg Match Prod", AGG_CATEGORY, new BigDecimal("1.00"), 10, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("1.00"), "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("2.00"), "confirmed");

        final long repoCount = orderRepo.countByUserId(userId);
        final Optional<Object> rawCount = executor.queryForScalar(
                DB, "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId);

        assertThat(rawCount).isPresent();
        assertThat(((Number) rawCount.get()).longValue())
                .as("raw COUNT should equal repository countByUserId")
                .isEqualTo(repoCount);
    }

    // ---------------------------------------------------------------- SUM via scalar

    @Test(groups = "database",
          description = "SUM(total) via queryForScalar returns accurate sum")
    public void sumTotalViaScalar() {
        userRepo.insert("Agg Sum User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg Sum Prod", AGG_CATEGORY, new BigDecimal("5.00"), 20, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("5.00"),  "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("10.00"), "pending");
        orderRepo.insert(userId, productId, 3, new BigDecimal("15.00"), "shipped");

        final Optional<Object> sumResult = executor.queryForScalar(
                DB,
                "SELECT COALESCE(SUM(total), 0) FROM orders WHERE user_id = ?",
                userId);

        assertThat(sumResult).isPresent();
        final BigDecimal actualSum = new BigDecimal(sumResult.get().toString());
        assertThat(actualSum)
                .as("SUM(total) should equal 5.00 + 10.00 + 15.00 = 30.00")
                .isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test(groups = "database",
          description = "COALESCE(SUM(total), 0) returns 0 for a user with no orders")
    public void sumReturnsZeroForUserWithNoOrders() {
        userRepo.insert("Agg NoOrders User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();

        final Optional<Object> sumResult = executor.queryForScalar(
                DB,
                "SELECT COALESCE(SUM(total), 0) FROM orders WHERE user_id = ?",
                userId);

        assertThat(sumResult).isPresent();
        final BigDecimal actualSum = new BigDecimal(sumResult.get().toString());
        assertThat(actualSum)
                .as("SUM for a user with no orders should be 0")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------- AVG via scalar

    @Test(groups = "database",
          description = "AVG(total) via queryForScalar returns correct arithmetic mean")
    public void avgTotalViaScalar() {
        userRepo.insert("Agg Avg User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg Avg Prod", AGG_CATEGORY, new BigDecimal("10.00"), 15, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // Insert 3 orders: totals 10, 20, 30 → average = 20.
        orderRepo.insert(userId, productId, 1, new BigDecimal("10.00"), "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("20.00"), "pending");
        orderRepo.insert(userId, productId, 3, new BigDecimal("30.00"), "shipped");

        final Optional<Object> avgResult = executor.queryForScalar(
                DB,
                "SELECT AVG(total) FROM orders WHERE user_id = ?",
                userId);

        assertThat(avgResult).isPresent();
        final BigDecimal avg = new BigDecimal(avgResult.get().toString())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(avg)
                .as("AVG(total) for totals 10+20+30 should be 20.00")
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }

    // ---------------------------------------------------------------- GROUP BY via queryForList

    @Test(groups = "database",
          description = "GROUP BY status returns one row per distinct status with correct count")
    public void groupByStatusReturnsCorrectRows() {
        userRepo.insert("Agg Group User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg Group Prod", AGG_CATEGORY, new BigDecimal("3.00"), 30, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        orderRepo.insert(userId, productId, 1, new BigDecimal("3.00"), "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("6.00"), "pending");
        orderRepo.insert(userId, productId, 3, new BigDecimal("9.00"), "shipped");

        final List<Map<String, Object>> groupedRows = executor.queryForList(
                DB,
                "SELECT status, COUNT(*) AS cnt FROM orders"
                + " WHERE user_id = ? GROUP BY status ORDER BY status",
                userId);

        assertThat(groupedRows)
                .as("GROUP BY should return 2 distinct status rows")
                .hasSize(2);

        // Build a lookup from status → count for validation.
        final Map<String, Long> statusCounts = groupedRows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.get("status").toString(),
                        r -> ((Number) r.get("cnt")).longValue()));

        assertThat(statusCounts.get("pending"))
                .as("pending count should be 2")
                .isEqualTo(2L);
        assertThat(statusCounts.get("shipped"))
                .as("shipped count should be 1")
                .isEqualTo(1L);
    }

    @Test(groups = "database",
          description = "GROUP BY with SUM aggregates order totals per status")
    public void groupBySumTotalPerStatus() {
        userRepo.insert("Agg Sum Group User", AGG_USER_EMAIL, "active");
        final long userId = userRepo.findByEmail(AGG_USER_EMAIL).orElseThrow().getId();
        productRepo.insert("Agg SG Prod", AGG_CATEGORY, new BigDecimal("4.00"), 25, true);
        final long productId = productRepo.findByCategory(AGG_CATEGORY)
                .stream().mapToLong(p -> p.getId()).max().orElseThrow();

        // pending: 4 + 8 = 12; confirmed: 16
        orderRepo.insert(userId, productId, 1, new BigDecimal("4.00"),  "pending");
        orderRepo.insert(userId, productId, 2, new BigDecimal("8.00"),  "pending");
        orderRepo.insert(userId, productId, 4, new BigDecimal("16.00"), "confirmed");

        final List<Map<String, Object>> rows = executor.queryForList(
                DB,
                "SELECT status, SUM(total) AS total_sum FROM orders"
                + " WHERE user_id = ? GROUP BY status ORDER BY status",
                userId);

        assertThat(rows).hasSize(2);

        for (final Map<String, Object> row : rows) {
            final String status   = row.get("status").toString();
            final BigDecimal sum  = new BigDecimal(row.get("total_sum").toString());
            if ("pending".equals(status)) {
                assertThat(sum).isEqualByComparingTo(new BigDecimal("12.00"));
            } else if ("confirmed".equals(status)) {
                assertThat(sum).isEqualByComparingTo(new BigDecimal("16.00"));
            }
        }
    }

    // ---------------------------------------------------------------- audit_log aggregation

    @Test(groups = "database", dataProvider = "auditActionProvider",
          description = "COUNT of audit entries per action matches seeded count")
    public void auditCountPerActionMatchesSeeded(final String action, final int seedCount) {
        for (int i = 0; i < seedCount; i++) {
            auditRepo.insert("order", action, AGG_AUDIT_ACTOR);
        }

        final long dbCount = auditRepo.countByEntityAndAction("order", action);
        assertThat(dbCount)
                .as("audit count for action '%s' should be %d", action, seedCount)
                .isEqualTo((long) seedCount);
    }

    @DataProvider(name = "auditActionProvider")
    public Object[][] auditActionProvider() {
        return new Object[][] {
                {"CREATE", 2},
                {"UPDATE", 3},
                {"DELETE", 1},
        };
    }

    @Test(groups = "database",
          description = "Total audit entries COUNT via scalar returns accurate sum after bulk insert")
    public void totalAuditCountAfterBulkInsert() {
        final long before = auditRepo.count();
        final int toInsert = 5;
        for (int i = 0; i < toInsert; i++) {
            auditRepo.insert("product", "READ", AGG_AUDIT_ACTOR);
        }
        final long after = auditRepo.count();
        assertThat(after)
                .as("audit count should increase by exactly %d after bulk insert", toInsert)
                .isEqualTo(before + toInsert);
    }
}
