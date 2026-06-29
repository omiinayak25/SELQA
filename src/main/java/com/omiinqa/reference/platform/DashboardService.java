package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;

/**
 * In-memory dashboard service that maintains a set of summary tiles
 * ({@code totalUsers}, {@code activeOrders}, {@code revenueToday},
 * {@code openTickets}) and provides atomic refresh, threshold-breach
 * detection, and reset operations.
 *
 * <p>State is per-instance so each BDD scenario gets an isolated world.
 * No Spring, no external dependencies — values are plain {@code long}
 * and {@code double} fields.</p>
 *
 * <p>Error codes (asserted by scenarios):</p>
 * <ul>
 *   <li>{@code DASHBOARD_INVALID_VALUE} — any tile set to a negative value</li>
 *   <li>{@code DASHBOARD_UNKNOWN_TILE} — unrecognised tile name passed to
 *       {@link #isThresholdBreached(String, double)}</li>
 * </ul>
 */
public class DashboardService {

    private long totalUsers;
    private long activeOrders;
    private double revenueToday;
    private long openTickets;

    /**
     * Constructs a new {@code DashboardService} with all tiles initialised to
     * zero.
     */
    public DashboardService() {
        this.totalUsers = 0L;
        this.activeOrders = 0L;
        this.revenueToday = 0.0;
        this.openTickets = 0L;
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    /**
     * Sets the {@code totalUsers} tile.
     *
     * @param count non-negative user count
     * @throws DomainException {@code DASHBOARD_INVALID_VALUE} if {@code count} is
     *                         negative
     */
    public void setTotalUsers(final long count) {
        requireNonNegative(count, "totalUsers");
        this.totalUsers = count;
    }

    /**
     * Sets the {@code activeOrders} tile.
     *
     * @param count non-negative order count
     * @throws DomainException {@code DASHBOARD_INVALID_VALUE} if {@code count} is
     *                         negative
     */
    public void setActiveOrders(final long count) {
        requireNonNegative(count, "activeOrders");
        this.activeOrders = count;
    }

    /**
     * Sets the {@code revenueToday} tile.
     *
     * @param amount non-negative revenue amount
     * @throws DomainException {@code DASHBOARD_INVALID_VALUE} if {@code amount} is
     *                         negative
     */
    public void setRevenueToday(final double amount) {
        requireNonNegativeDouble(amount, "revenueToday");
        this.revenueToday = amount;
    }

    /**
     * Sets the {@code openTickets} tile.
     *
     * @param count non-negative ticket count
     * @throws DomainException {@code DASHBOARD_INVALID_VALUE} if {@code count} is
     *                         negative
     */
    public void setOpenTickets(final long count) {
        requireNonNegative(count, "openTickets");
        this.openTickets = count;
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    /**
     * Returns an immutable snapshot of the current tile values.
     *
     * @return a {@link DashboardSummary} capturing the state at this instant
     */
    public DashboardSummary getSummary() {
        return new DashboardSummary(totalUsers, activeOrders, revenueToday, openTickets);
    }

    // ─── Bulk operations ─────────────────────────────────────────────────────

    /**
     * Atomically sets all four tiles and returns an updated snapshot.
     * All four values are validated before any tile is mutated, so on a
     * validation failure the service state remains unchanged.
     *
     * @param totalUsers   non-negative total user count
     * @param activeOrders non-negative active order count
     * @param revenueToday non-negative revenue for today
     * @param openTickets  non-negative open ticket count
     * @return an immutable {@link DashboardSummary} reflecting the new values
     * @throws DomainException {@code DASHBOARD_INVALID_VALUE} if any argument is
     *                         negative
     */
    public DashboardSummary refresh(final long totalUsers,
                                    final long activeOrders,
                                    final double revenueToday,
                                    final long openTickets) {
        requireNonNegative(totalUsers, "totalUsers");
        requireNonNegative(activeOrders, "activeOrders");
        requireNonNegativeDouble(revenueToday, "revenueToday");
        requireNonNegative(openTickets, "openTickets");
        this.totalUsers = totalUsers;
        this.activeOrders = activeOrders;
        this.revenueToday = revenueToday;
        this.openTickets = openTickets;
        return getSummary();
    }

    /**
     * Checks whether the named tile's current value exceeds {@code threshold}.
     *
     * <p>The comparison is <em>strictly greater than</em>: a tile whose value
     * equals the threshold is <em>not</em> considered breached.</p>
     *
     * @param tile      one of {@code "totalUsers"}, {@code "activeOrders"},
     *                  {@code "revenueToday"}, {@code "openTickets"}
     * @param threshold the threshold to compare against
     * @return {@code true} if the tile value is strictly greater than
     *         {@code threshold}
     * @throws DomainException {@code DASHBOARD_UNKNOWN_TILE} if {@code tile} is
     *                         not a recognised tile name
     */
    public boolean isThresholdBreached(final String tile, final double threshold) {
        switch (tile) {
            case "totalUsers":
                return (double) totalUsers > threshold;
            case "activeOrders":
                return (double) activeOrders > threshold;
            case "revenueToday":
                return revenueToday > threshold;
            case "openTickets":
                return (double) openTickets > threshold;
            default:
                throw new DomainException("DASHBOARD_UNKNOWN_TILE",
                        "Unknown dashboard tile: " + tile);
        }
    }

    /**
     * Resets all tiles to zero.
     */
    public void resetAll() {
        this.totalUsers = 0L;
        this.activeOrders = 0L;
        this.revenueToday = 0.0;
        this.openTickets = 0L;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private static void requireNonNegative(final long value, final String tile) {
        if (value < 0) {
            throw new DomainException("DASHBOARD_INVALID_VALUE",
                    tile + " must not be negative, got: " + value);
        }
    }

    private static void requireNonNegativeDouble(final double value, final String tile) {
        if (value < 0.0) {
            throw new DomainException("DASHBOARD_INVALID_VALUE",
                    tile + " must not be negative, got: " + value);
        }
    }

    // ─── Inner class: DashboardSummary ────────────────────────────────────────

    /**
     * Immutable snapshot of all four dashboard tile values at a point in time.
     *
     * <p>Instances are produced by {@link DashboardService#getSummary()} and
     * {@link DashboardService#refresh(long, long, double, long)}. Once created,
     * the snapshot is independent of the service and will not reflect subsequent
     * mutations.</p>
     */
    public static final class DashboardSummary {

        private final long totalUsers;
        private final long activeOrders;
        private final double revenueToday;
        private final long openTickets;

        private DashboardSummary(final long totalUsers,
                                 final long activeOrders,
                                 final double revenueToday,
                                 final long openTickets) {
            this.totalUsers = totalUsers;
            this.activeOrders = activeOrders;
            this.revenueToday = revenueToday;
            this.openTickets = openTickets;
        }

        /**
         * Returns the total user count captured in this snapshot.
         *
         * @return total user count
         */
        public long getTotalUsers() {
            return totalUsers;
        }

        /**
         * Returns the active order count captured in this snapshot.
         *
         * @return active order count
         */
        public long getActiveOrders() {
            return activeOrders;
        }

        /**
         * Returns the today's revenue captured in this snapshot.
         *
         * @return revenue for today
         */
        public double getRevenueToday() {
            return revenueToday;
        }

        /**
         * Returns the open ticket count captured in this snapshot.
         *
         * @return open ticket count
         */
        public long getOpenTickets() {
            return openTickets;
        }
    }
}
