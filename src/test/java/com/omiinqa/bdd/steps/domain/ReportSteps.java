package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.ReportService;
import com.omiinqa.reference.platform.ReportService.OrderRecord;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / reports domain.
 *
 * <p>Drives the real {@link ReportService} (in-memory, dataset-driven) with no
 * browser, no external storage, and no Spring. Outcomes are recorded via
 * {@link DomainWorld} so the shared assertions in {@code CommonDomainSteps}
 * ("the operation succeeds", "a domain error X is raised") apply unchanged.</p>
 *
 * <p>All step text is prefixed with the noun <em>report</em> / <em>Report</em>
 * to guarantee global uniqueness across the full Cucumber step registry and to
 * avoid collision with existing prefixes such as {@code audit}.</p>
 *
 * <p>Domain behaviour covered:</p>
 * <ul>
 *   <li>{@code REPORT_BAD_RANGE} — {@code from} date is after {@code to} date in
 *       {@link ReportService#filterByDateRange}</li>
 *   <li>Aggregations: {@code count}, {@code sumRevenue}, {@code avgOrderValue},
 *       {@code groupByStatus}, {@code topNByRevenue}</li>
 *   <li>Filters: {@code filterByDateRange}, {@code countByUser},
 *       {@code sumRevenueByStatus}</li>
 *   <li>Boundary: empty dataset safe-returns for all numeric aggregations</li>
 * </ul>
 */
public class ReportSteps {

    private static final String SVC           = "reportService";
    private static final String DATASET_KEY   = "report.dataset";
    private static final String NUMERIC_RESULT = "report.numericResult";
    private static final String LONG_RESULT   = "report.longResult";
    private static final String LIST_RESULT   = "report.listResult";
    private static final String MAP_RESULT    = "report.mapResult";

    /** Auto-incremented orderId seed; reset per {@link #cleanReportService()}. */
    private final AtomicInteger orderSeed = new AtomicInteger(1);

    // -------------------------------------------------------------------------
    // Service accessor
    // -------------------------------------------------------------------------

    private ReportService service() {
        return DomainWorld.service(SVC, ReportService::new);
    }

    // -------------------------------------------------------------------------
    // Given — setup
    // -------------------------------------------------------------------------

    /**
     * Resets the report service and its working dataset to a clean, empty state.
     * Called from the feature Background so every scenario starts isolated.
     */
    @Given("a clean report service")
    public void cleanReportService() {
        orderSeed.set(1);
        final ReportService svc = new ReportService();
        svc.setDataset(new ArrayList<>());
        DomainWorld.put(SVC, svc);
        DomainWorld.put(DATASET_KEY, new ArrayList<OrderRecord>());
    }

    /**
     * Adds {@code count} {@link OrderRecord} objects all sharing the same
     * {@code status}, {@code revenue}, and {@code date} to the working dataset
     * and pushes it into the service. The {@code orderId} is auto-generated as
     * {@code order-N} where N increments globally within the scenario.
     *
     * @param count   number of orders to add
     * @param revenue revenue value for every added order
     * @param status  lifecycle status for every added order
     * @param date    creation date (yyyy-MM-dd) for every added order
     */
    @Given("the report dataset has {int} orders with revenue {double} each status {string} on date {string}")
    public void reportDatasetHasOrders(final int count,
                                       final double revenue,
                                       final String status,
                                       final String date) {
        final LocalDate createdAt = LocalDate.parse(date);
        final List<OrderRecord> dataset = getOrCreateDataset();
        for (int i = 0; i < count; i++) {
            final String orderId = "order-" + orderSeed.getAndIncrement();
            dataset.add(new OrderRecord(orderId, "user-default", status, revenue, createdAt));
        }
        pushDataset(dataset);
    }

    /**
     * Resets the working dataset to empty and synchronises the service.
     */
    @Given("the report dataset is empty")
    public void reportDatasetIsEmpty() {
        final List<OrderRecord> empty = new ArrayList<>();
        DomainWorld.put(DATASET_KEY, empty);
        service().setDataset(empty);
    }

    /**
     * Adds a single, fully-specified {@link OrderRecord} to the working dataset.
     *
     * @param orderId  unique order identifier
     * @param userId   owning user identifier
     * @param status   lifecycle status
     * @param revenue  monetary value
     * @param date     creation date as {@code yyyy-MM-dd}
     */
    @Given("I add a report order id {string} user {string} status {string} revenue {double} date {string}")
    public void addReportOrder(final String orderId,
                               final String userId,
                               final String status,
                               final double revenue,
                               final String date) {
        final LocalDate createdAt = LocalDate.parse(date);
        final List<OrderRecord> dataset = getOrCreateDataset();
        dataset.add(new OrderRecord(orderId, userId, status, revenue, createdAt));
        pushDataset(dataset);
    }

    // -------------------------------------------------------------------------
    // When — compute operations
    // -------------------------------------------------------------------------

    /**
     * Computes the total order count and stores it as the long result.
     */
    @When("I compute the report count")
    public void computeReportCount() {
        final long result = DomainWorld.capture(() -> service().count());
        DomainWorld.put(LONG_RESULT, result);
    }

    /**
     * Computes the total revenue sum and stores it as the numeric (double) result.
     */
    @When("I compute the report revenue sum")
    public void computeReportRevenueSum() {
        final double result = DomainWorld.capture(() -> service().sumRevenue());
        DomainWorld.put(NUMERIC_RESULT, result);
    }

    /**
     * Computes the average order value and stores it as the numeric (double) result.
     */
    @When("I compute the report average order value")
    public void computeReportAvgOrderValue() {
        final double result = DomainWorld.capture(() -> service().avgOrderValue());
        DomainWorld.put(NUMERIC_RESULT, result);
    }

    /**
     * Computes the group-by-status map and stores it as the map result.
     */
    @When("I compute the report group by status")
    public void computeReportGroupByStatus() {
        final Map<String, Long> result = DomainWorld.capture(() -> service().groupByStatus());
        DomainWorld.put(MAP_RESULT, result);
    }

    /**
     * Computes the top-{@code n} orders by revenue (descending) and stores
     * them as the list result.
     *
     * @param n maximum number of orders to return
     */
    @When("I compute the report top {int} by revenue")
    public void computeReportTopN(final int n) {
        final List<OrderRecord> result = DomainWorld.capture(() -> service().topNByRevenue(n));
        DomainWorld.put(LIST_RESULT, result);
    }

    /**
     * Computes the date-range filter and stores the result as the list result.
     * Any {@code REPORT_BAD_RANGE} domain error is captured by
     * {@link DomainWorld#capture} for assertion in subsequent Then steps.
     *
     * @param from lower bound date as {@code yyyy-MM-dd}
     * @param to   upper bound date as {@code yyyy-MM-dd}
     */
    @When("I compute the report filter by date range {string} to {string}")
    public void computeReportFilterByDateRange(final String from, final String to) {
        final LocalDate fromDate = LocalDate.parse(from);
        final LocalDate toDate = LocalDate.parse(to);
        final List<OrderRecord> result =
                DomainWorld.capture(() -> service().filterByDateRange(fromDate, toDate));
        DomainWorld.put(LIST_RESULT, result);
    }

    /**
     * Counts orders belonging to the specified user and stores the value as
     * the long result.
     *
     * @param userId the user identifier to filter on
     */
    @When("I compute the report count by user {string}")
    public void computeReportCountByUser(final String userId) {
        final long result = DomainWorld.capture(() -> service().countByUser(userId));
        DomainWorld.put(LONG_RESULT, result);
    }

    /**
     * Sums revenue for orders in the specified status and stores the value as
     * the numeric (double) result.
     *
     * @param status the status bucket to filter on
     */
    @When("I compute the report revenue sum by status {string}")
    public void computeReportRevenueSumByStatus(final String status) {
        final double result = DomainWorld.capture(() -> service().sumRevenueByStatus(status));
        DomainWorld.put(NUMERIC_RESULT, result);
    }

    // -------------------------------------------------------------------------
    // Then — result assertions
    // -------------------------------------------------------------------------

    /**
     * Asserts that the last numeric (double) result equals {@code expected}.
     *
     * @param expected the expected double value (compared with a tolerance of
     *                 {@code 0.001})
     */
    @Then("the report numeric result is {double}")
    public void reportNumericResultIs(final double expected) {
        final Double actual = DomainWorld.get(NUMERIC_RESULT);
        assertThat(actual)
                .as("report numeric result")
                .isNotNull()
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(0.001));
    }

    /**
     * Asserts that the last long result equals {@code expected}.
     *
     * @param expected the expected long value
     */
    @Then("the report long result is {long}")
    public void reportLongResultIs(final long expected) {
        final Long actual = DomainWorld.get(LONG_RESULT);
        assertThat(actual)
                .as("report long result")
                .isNotNull()
                .isEqualTo(expected);
    }

    /**
     * Asserts that the group-by-status map contains the given {@code status}
     * with a count of exactly {@code expected}.
     *
     * @param status   the status key to look up
     * @param expected expected count for that status
     */
    @Then("the report group count for status {string} is {long}")
    public void reportGroupCountForStatus(final String status, final long expected) {
        final Map<String, Long> map = DomainWorld.get(MAP_RESULT);
        assertThat(map)
                .as("report group-by-status map")
                .isNotNull();
        final long actual = map.getOrDefault(status, 0L);
        assertThat(actual)
                .as("count for status '%s'", status)
                .isEqualTo(expected);
    }

    /**
     * Asserts that the last list result contains exactly {@code expected} items.
     *
     * @param expected expected list size
     */
    @Then("the report result list size is {int}")
    public void reportResultListSize(final int expected) {
        final List<?> list = DomainWorld.get(LIST_RESULT);
        assertThat(list)
                .as("report result list")
                .isNotNull()
                .hasSize(expected);
    }

    /**
     * Asserts that the first item in the top-N result list has the specified
     * revenue (within a tolerance of {@code 0.001}).
     *
     * @param expected revenue expected on the first (highest-revenue) order
     */
    @Then("the report top result first has revenue {double}")
    public void reportTopResultFirstHasRevenue(final double expected) {
        final List<OrderRecord> list = DomainWorld.get(LIST_RESULT);
        assertThat(list)
                .as("report top-N result list must not be empty")
                .isNotNull()
                .isNotEmpty();
        assertThat(list.get(0).getRevenue())
                .as("revenue of first top-N result")
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(0.001));
    }

    /**
     * Asserts that the date-range filter result list contains exactly
     * {@code expected} items.
     *
     * @param expected expected number of orders in the filtered result
     */
    @Then("the report date range result size is {int}")
    public void reportDateRangeResultSize(final int expected) {
        final List<?> list = DomainWorld.get(LIST_RESULT);
        assertThat(list)
                .as("report date-range result list")
                .isNotNull()
                .hasSize(expected);
    }

    /**
     * Asserts that the last numeric or long result is zero (works for both
     * {@code count} and revenue aggregations stored as numeric/long).
     */
    @Then("the report result is zero")
    public void reportResultIsZero() {
        // Try numeric (double) first, then long
        final Double numeric = DomainWorld.get(NUMERIC_RESULT);
        if (numeric != null) {
            assertThat(numeric)
                    .as("report numeric result should be zero")
                    .isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
            return;
        }
        final Long longVal = DomainWorld.get(LONG_RESULT);
        assertThat(longVal)
                .as("report long result should be zero")
                .isNotNull()
                .isEqualTo(0L);
    }

    /**
     * Asserts that the last list result is not null and contains no items.
     */
    @Then("the report result list is empty")
    public void reportResultListIsEmpty() {
        final List<?> list = DomainWorld.get(LIST_RESULT);
        assertThat(list)
                .as("report result list should be empty")
                .isNotNull()
                .isEmpty();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<OrderRecord> getOrCreateDataset() {
        List<OrderRecord> dataset = DomainWorld.get(DATASET_KEY);
        if (dataset == null) {
            dataset = new ArrayList<>();
            DomainWorld.put(DATASET_KEY, dataset);
        }
        return dataset;
    }

    private void pushDataset(final List<OrderRecord> dataset) {
        service().setDataset(dataset);
    }
}
