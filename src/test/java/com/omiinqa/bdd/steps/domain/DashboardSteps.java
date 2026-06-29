package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.DashboardService;
import com.omiinqa.reference.platform.DashboardService.DashboardSummary;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / dashboard domain.
 *
 * <p>Drives the real {@link DashboardService} (in-memory, per-instance state) —
 * no browser, no external storage, no Spring. Outcomes are recorded via
 * {@link DomainWorld} so the shared assertions in {@code CommonDomainSteps}
 * ("the operation succeeds", "a domain error X is raised") apply unchanged.</p>
 *
 * <p>All step text is prefixed with the noun <em>dashboard</em> /
 * <em>Dashboard</em> to guarantee global uniqueness across the full Cucumber
 * step registry and to avoid collision with existing prefixes such as
 * {@code audit} and {@code report}.</p>
 *
 * <p>Domain behaviour covered:</p>
 * <ul>
 *   <li>{@code DASHBOARD_INVALID_VALUE} — any tile set to a negative value via
 *       individual setter or bulk {@link DashboardService#refresh}</li>
 *   <li>{@code DASHBOARD_UNKNOWN_TILE} — unrecognised tile name passed to
 *       {@link DashboardService#isThresholdBreached}</li>
 *   <li>Tile correctness: summary reflects exactly what was set</li>
 *   <li>Threshold: strictly-greater-than comparison, not breached at equality</li>
 *   <li>Reset: {@link DashboardService#resetAll()} zeroes all tiles</li>
 *   <li>Atomicity: {@link DashboardService#refresh} sets all tiles at once</li>
 * </ul>
 */
public class DashboardSteps {

    private static final String SVC                = "dashboardService";
    private static final String THRESHOLD_BREACHED = "dashboard.thresholdBreached";

    // ─── Service accessor ─────────────────────────────────────────────────────

    private DashboardService service() {
        return DomainWorld.service(SVC, DashboardService::new);
    }

    // ─── Given — setup ────────────────────────────────────────────────────────

    /**
     * Resets the dashboard service to a clean, all-zero state at the start of
     * each scenario. Called from the feature Background.
     */
    @Given("a clean dashboard service")
    public void cleanDashboardService() {
        DomainWorld.put(SVC, new DashboardService());
    }

    /**
     * Precondition step that refreshes all four tiles in one atomic call.
     * Used in Given blocks where we need a known state without asserting the
     * outcome of the refresh operation itself.
     *
     * @param totalUsers   initial total user count
     * @param activeOrders initial active order count
     * @param revenueToday initial today's revenue
     * @param openTickets  initial open ticket count
     */
    @Given("the dashboard has total users {long} active orders {long} revenue today {double} open tickets {long}")
    public void theDashboardHas(final long totalUsers,
                                final long activeOrders,
                                final double revenueToday,
                                final long openTickets) {
        service().refresh(totalUsers, activeOrders, revenueToday, openTickets);
    }

    // ─── When — individual setters ────────────────────────────────────────────

    /**
     * Sets the {@code totalUsers} tile, capturing any domain error.
     *
     * @param count the value to set
     */
    @When("I set dashboard total users to {long}")
    public void setDashboardTotalUsers(final long count) {
        DomainWorld.run(() -> service().setTotalUsers(count));
    }

    /**
     * Sets the {@code activeOrders} tile, capturing any domain error.
     *
     * @param count the value to set
     */
    @When("I set dashboard active orders to {long}")
    public void setDashboardActiveOrders(final long count) {
        DomainWorld.run(() -> service().setActiveOrders(count));
    }

    /**
     * Sets the {@code revenueToday} tile, capturing any domain error.
     *
     * @param amount the value to set
     */
    @When("I set dashboard revenue today to {double}")
    public void setDashboardRevenueToday(final double amount) {
        DomainWorld.run(() -> service().setRevenueToday(amount));
    }

    /**
     * Sets the {@code openTickets} tile, capturing any domain error.
     *
     * @param count the value to set
     */
    @When("I set dashboard open tickets to {long}")
    public void setDashboardOpenTickets(final long count) {
        DomainWorld.run(() -> service().setOpenTickets(count));
    }

    // ─── When — bulk operations ───────────────────────────────────────────────

    /**
     * Atomically refreshes all four tiles via
     * {@link DashboardService#refresh(long, long, double, long)}, capturing any
     * domain error. The resulting {@link DashboardSummary} is stored for
     * subsequent Then assertions.
     *
     * @param totalUsers   total user count
     * @param activeOrders active order count
     * @param revenueToday revenue for today
     * @param openTickets  open ticket count
     */
    @When("I refresh the dashboard with total users {long} active orders {long} revenue today {double} open tickets {long}")
    public void refreshDashboard(final long totalUsers,
                                 final long activeOrders,
                                 final double revenueToday,
                                 final long openTickets) {
        final DashboardSummary summary = DomainWorld.capture(
                () -> service().refresh(totalUsers, activeOrders, revenueToday, openTickets));
        if (summary != null) {
            DomainWorld.put("dashboard.summary", summary);
        }
    }

    /**
     * Calls {@link DashboardService#resetAll()}, setting all tiles back to zero.
     * Any unexpected domain error is captured via {@link DomainWorld#run}.
     */
    @When("I reset the dashboard")
    public void resetDashboard() {
        DomainWorld.run(() -> service().resetAll());
    }

    /**
     * Calls {@link DashboardService#isThresholdBreached(String, double)},
     * capturing any domain error, and stores the boolean result under
     * {@code dashboard.thresholdBreached} for subsequent Then assertions.
     *
     * @param tile      the tile name to check
     * @param threshold the threshold value
     */
    @When("I check dashboard threshold for tile {string} at {double}")
    public void checkDashboardThreshold(final String tile, final double threshold) {
        final Boolean result = DomainWorld.capture(
                () -> service().isThresholdBreached(tile, threshold));
        DomainWorld.put(THRESHOLD_BREACHED, result);
    }

    // ─── Then — tile value assertions ─────────────────────────────────────────

    /**
     * Asserts that the service's current {@code totalUsers} tile equals
     * {@code expected}.
     *
     * @param expected expected total user count
     */
    @Then("the dashboard total users is {long}")
    public void dashboardTotalUsersIs(final long expected) {
        assertThat(service().getSummary().getTotalUsers())
                .as("dashboard totalUsers tile")
                .isEqualTo(expected);
    }

    /**
     * Asserts that the service's current {@code activeOrders} tile equals
     * {@code expected}.
     *
     * @param expected expected active order count
     */
    @Then("the dashboard active orders is {long}")
    public void dashboardActiveOrdersIs(final long expected) {
        assertThat(service().getSummary().getActiveOrders())
                .as("dashboard activeOrders tile")
                .isEqualTo(expected);
    }

    /**
     * Asserts that the service's current {@code revenueToday} tile equals
     * {@code expected} within a tolerance of {@code 0.001}.
     *
     * @param expected expected revenue for today
     */
    @Then("the dashboard revenue today is {double}")
    public void dashboardRevenueTodayIs(final double expected) {
        assertThat(service().getSummary().getRevenueToday())
                .as("dashboard revenueToday tile")
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(0.001));
    }

    /**
     * Asserts that the service's current {@code openTickets} tile equals
     * {@code expected}.
     *
     * @param expected expected open ticket count
     */
    @Then("the dashboard open tickets is {long}")
    public void dashboardOpenTicketsIs(final long expected) {
        assertThat(service().getSummary().getOpenTickets())
                .as("dashboard openTickets tile")
                .isEqualTo(expected);
    }

    // ─── Then — threshold assertions ──────────────────────────────────────────

    /**
     * Asserts that the last threshold check indicated that the tile value
     * <em>is</em> breached (strictly greater than the threshold).
     */
    @Then("the dashboard threshold is breached")
    public void dashboardThresholdIsBreached() {
        final Boolean breached = DomainWorld.get(THRESHOLD_BREACHED);
        assertThat(breached)
                .as("dashboard threshold should be breached (true)")
                .isNotNull()
                .isTrue();
    }

    /**
     * Asserts that the last threshold check indicated that the tile value is
     * <em>not</em> breached (at or below the threshold).
     */
    @Then("the dashboard threshold is not breached")
    public void dashboardThresholdIsNotBreached() {
        final Boolean breached = DomainWorld.get(THRESHOLD_BREACHED);
        assertThat(breached)
                .as("dashboard threshold should not be breached (false)")
                .isNotNull()
                .isFalse();
    }
}
