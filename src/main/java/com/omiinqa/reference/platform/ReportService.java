package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory reporting service for the reference platform domain.
 *
 * <p>Operates over a caller-supplied dataset of {@link OrderRecord} objects
 * injected via {@link #setDataset(List)}. State is per-instance so each BDD
 * scenario receives its own isolated service with no cross-contamination.</p>
 *
 * <p>All aggregation methods return safe zero-values on an empty dataset rather
 * than throwing, unless a business rule explicitly demands a failure (e.g. an
 * invalid date range). This design lets scenarios assert boundary behaviour
 * cleanly without extra guards.</p>
 *
 * <h2>Error codes</h2>
 * <ul>
 *   <li>{@code REPORT_BAD_RANGE} — {@link #filterByDateRange(LocalDate, LocalDate)}
 *       was called with {@code from} after {@code to}.</li>
 * </ul>
 */
public class ReportService {

    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    /**
     * Immutable record representing a single order in the reporting dataset.
     *
     * <p>The {@code status} field is expected to be one of the canonical values
     * {@code PENDING}, {@code COMPLETED}, {@code CANCELLED}, or {@code REFUNDED},
     * but the service does not enforce this so that BDD scenarios can inject
     * arbitrary status strings for boundary testing.</p>
     */
    public static final class OrderRecord {

        private final String orderId;
        private final String userId;
        private final String status;
        private final double revenue;
        private final LocalDate createdAt;

        /**
         * Constructs an {@link OrderRecord}.
         *
         * @param orderId   unique order identifier; must not be {@code null}
         * @param userId    identifier of the owning user; must not be {@code null}
         * @param status    order lifecycle status (e.g. {@code COMPLETED})
         * @param revenue   monetary value of the order; may be zero
         * @param createdAt calendar date the order was created; must not be {@code null}
         */
        public OrderRecord(final String orderId,
                           final String userId,
                           final String status,
                           final double revenue,
                           final LocalDate createdAt) {
            this.orderId = orderId;
            this.userId = userId;
            this.status = status;
            this.revenue = revenue;
            this.createdAt = createdAt;
        }

        /** Unique order identifier. */
        public String getOrderId() { return orderId; }

        /** Identifier of the user who placed the order. */
        public String getUserId() { return userId; }

        /**
         * Order lifecycle status. Expected values: {@code PENDING},
         * {@code COMPLETED}, {@code CANCELLED}, {@code REFUNDED}.
         */
        public String getStatus() { return status; }

        /** Monetary value of the order; zero or positive. */
        public double getRevenue() { return revenue; }

        /** Calendar date on which the order was created. */
        public LocalDate getCreatedAt() { return createdAt; }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Working dataset; replaced wholesale on each {@link #setDataset(List)} call. */
    private List<OrderRecord> dataset = Collections.emptyList();

    // -------------------------------------------------------------------------
    // Dataset injection
    // -------------------------------------------------------------------------

    /**
     * Replaces the current dataset with the supplied list of orders.
     *
     * <p>A defensive copy is taken so that later mutations to the caller's list
     * do not affect the service. Passing {@code null} is treated as an empty
     * dataset.</p>
     *
     * @param orders the new dataset; {@code null} is allowed and treated as empty
     */
    public void setDataset(final List<OrderRecord> orders) {
        this.dataset = (orders == null) ? Collections.emptyList()
                                        : Collections.unmodifiableList(new ArrayList<>(orders));
    }

    // -------------------------------------------------------------------------
    // Aggregations
    // -------------------------------------------------------------------------

    /**
     * Returns the total number of orders in the current dataset.
     *
     * @return non-negative count; {@code 0} when the dataset is empty
     */
    public long count() {
        return dataset.size();
    }

    /**
     * Returns the sum of {@link OrderRecord#getRevenue()} across all orders.
     *
     * @return total revenue; {@code 0.0} when the dataset is empty
     */
    public double sumRevenue() {
        return dataset.stream()
                .mapToDouble(OrderRecord::getRevenue)
                .sum();
    }

    /**
     * Returns the arithmetic mean of {@link OrderRecord#getRevenue()} across
     * all orders.
     *
     * <p>Returns {@code 0.0} on an empty dataset rather than throwing, because
     * "no orders yet" is a valid, non-exceptional business state.</p>
     *
     * @return average order value; {@code 0.0} when the dataset is empty
     */
    public double avgOrderValue() {
        if (dataset.isEmpty()) {
            return 0.0;
        }
        return dataset.stream()
                .mapToDouble(OrderRecord::getRevenue)
                .average()
                .orElse(0.0);
    }

    /**
     * Groups orders by their {@link OrderRecord#getStatus()} and counts how many
     * orders fall into each status bucket.
     *
     * @return mutable map of {@code status -> count}; empty map when the dataset
     *         is empty
     */
    public Map<String, Long> groupByStatus() {
        return dataset.stream()
                .collect(Collectors.groupingBy(OrderRecord::getStatus, Collectors.counting()));
    }

    /**
     * Returns the top-{@code n} orders sorted by {@link OrderRecord#getRevenue()}
     * in descending order.
     *
     * <p>When the dataset contains fewer than {@code n} orders the entire dataset
     * is returned sorted.</p>
     *
     * @param n the maximum number of orders to return; must be non-negative
     * @return list of up to {@code n} orders with highest revenue first; empty
     *         list when the dataset is empty or {@code n} is zero
     */
    public List<OrderRecord> topNByRevenue(final int n) {
        return dataset.stream()
                .sorted(Comparator.comparingDouble(OrderRecord::getRevenue).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Returns all orders whose {@link OrderRecord#getCreatedAt()} falls within
     * the inclusive date range {@code [from, to]}.
     *
     * @param from the lower bound of the range (inclusive); must not be after
     *             {@code to}
     * @param to   the upper bound of the range (inclusive)
     * @return list of matching orders; empty list when none match or the dataset
     *         is empty
     * @throws DomainException {@code REPORT_BAD_RANGE} when {@code from} is
     *                         strictly after {@code to}
     */
    public List<OrderRecord> filterByDateRange(final LocalDate from, final LocalDate to) {
        if (from.isAfter(to)) {
            throw new DomainException("REPORT_BAD_RANGE",
                    "Date range is invalid: 'from' (" + from + ") must not be after 'to' (" + to + ")");
        }
        return dataset.stream()
                .filter(o -> !o.getCreatedAt().isBefore(from) && !o.getCreatedAt().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Counts how many orders in the dataset belong to the given {@code userId}.
     *
     * @param userId the user identifier to filter on; comparison is case-sensitive
     * @return non-negative count; {@code 0} when no orders match or dataset is empty
     */
    public long countByUser(final String userId) {
        return dataset.stream()
                .filter(o -> o.getUserId().equals(userId))
                .count();
    }

    /**
     * Returns the sum of revenue for all orders whose
     * {@link OrderRecord#getStatus()} matches the supplied {@code status}.
     *
     * @param status the status value to filter on; comparison is case-sensitive
     * @return total revenue for the matching orders; {@code 0.0} when none match
     *         or the dataset is empty
     */
    public double sumRevenueByStatus(final String status) {
        return dataset.stream()
                .filter(o -> o.getStatus().equals(status))
                .mapToDouble(OrderRecord::getRevenue)
                .sum();
    }
}
