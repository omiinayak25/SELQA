package com.omiinqa.database.repositories;

import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.RowMapper;
import com.omiinqa.database.model.OrderRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@code orders} table (Repository + Template Method patterns).
 *
 * <p><b>Design pattern — Repository:</b>
 * Centralises all SQL targeting the {@code orders} table. Test classes that
 * need to verify order creation, status transitions, or fulfilment totals call
 * intention-revealing methods such as {@link #findByUserId} or
 * {@link #sumTotalByUserId} rather than crafting ad-hoc SQL. This achieves:</p>
 * <ol>
 *   <li>A single point of change when the orders schema evolves (rename a
 *       column once here, not in every test that queries orders).</li>
 *   <li>Consistent parameterised-query discipline — no test can accidentally
 *       introduce a SQL-injection vulnerability by concatenating values.</li>
 *   <li>Readable test intent: {@code repo.findByStatus("shipped")} communicates
 *       domain meaning instantly; a raw JDBC call does not.</li>
 * </ol>
 *
 * <p><b>Design pattern — Template Method:</b>
 * All JDBC boilerplate (acquire connection, prepare statement, bind params,
 * iterate result set, close resources) lives in {@link AbstractRepository}.
 * This class supplies only the SQL constants, the {@link RowMapper} strategy,
 * and the domain-specific finder/DML methods — the variable steps of the
 * algorithm.</p>
 *
 * <p><b>SQL injection prevention:</b>
 * Every method uses {@code ?} placeholders and delegates binding to
 * {@link com.omiinqa.database.QueryExecutor}, which exclusively uses
 * {@link java.sql.PreparedStatement}. No value is ever concatenated into a SQL
 * string. This is enforced here and verified during adversarial code review.</p>
 *
 * <p><b>Thread safety:</b> Stateless beyond the immutable {@code dbType}
 * field inherited from {@link AbstractRepository}. Safe for concurrent access
 * from parallel TestNG threads.</p>
 *
 * <p><b>Schema</b> (table: {@code orders}):</p>
 * <pre>
 *   id          BIGINT PRIMARY KEY AUTO_INCREMENT / SERIAL
 *   user_id     BIGINT NOT NULL REFERENCES users(id)
 *   product_id  BIGINT NOT NULL REFERENCES products(id)
 *   quantity    INT NOT NULL CHECK (quantity {@literal >} 0)
 *   total       NUMERIC(12,2) NOT NULL
 *   status      VARCHAR(50) NOT NULL DEFAULT 'pending'
 *   created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
 * </pre>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   OrderRepository repo = new OrderRepository(DatabaseType.POSTGRESQL);
 *   List<OrderRecord> pending = repo.findByStatus("pending");
 *   assertThat(pending).allMatch(o -> o.getTotal().compareTo(BigDecimal.ZERO) > 0);
 * }</pre>
 */
public class OrderRepository extends AbstractRepository<OrderRecord> {

    // ---------------------------------------------------------------- SQL constants

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, product_id, quantity, total, status, created_at"
            + " FROM orders WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, user_id, product_id, quantity, total, status, created_at"
            + " FROM orders ORDER BY id";

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT id, user_id, product_id, quantity, total, status, created_at"
            + " FROM orders WHERE user_id = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT id, user_id, product_id, quantity, total, status, created_at"
            + " FROM orders WHERE status = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_USER_AND_STATUS =
            "SELECT id, user_id, product_id, quantity, total, status, created_at"
            + " FROM orders WHERE user_id = ? AND status = ? ORDER BY created_at DESC";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM orders";

    private static final String SQL_COUNT_BY_USER =
            "SELECT COUNT(*) FROM orders WHERE user_id = ?";

    private static final String SQL_COUNT_BY_STATUS =
            "SELECT COUNT(*) FROM orders WHERE status = ?";

    private static final String SQL_SUM_TOTAL_BY_USER =
            "SELECT COALESCE(SUM(total), 0) FROM orders WHERE user_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO orders (user_id, product_id, quantity, total, status)"
            + " VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE orders SET status = ? WHERE id = ?";

    private static final String SQL_DELETE_BY_ID =
            "DELETE FROM orders WHERE id = ?";

    private static final String SQL_DELETE_BY_USER_ID =
            "DELETE FROM orders WHERE user_id = ?";

    // ---------------------------------------------------------------- row mapper

    /**
     * Converts a single JDBC row into an {@link OrderRecord}.
     *
     * <p>Declared as a static field (not an anonymous class or per-call lambda)
     * so the instance is allocated once and reused across all query invocations
     * in this repository — consistent with the allocation pattern used by
     * {@link UserRepository} and {@link ProductRepository}.</p>
     *
     * <p>{@code total} is fetched via {@link java.sql.ResultSet#getBigDecimal}
     * to preserve NUMERIC(12,2) precision without IEEE-754 rounding.</p>
     */
    private static final RowMapper<OrderRecord> ORDER_MAPPER = (rs, rowNum) ->
            OrderRecord.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getLong("user_id"))
                    .productId(rs.getLong("product_id"))
                    .quantity(rs.getInt("quantity"))
                    .total(rs.getBigDecimal("total"))
                    .status(rs.getString("status"))
                    .createdAt(rs.getObject("created_at"))
                    .build();

    // ---------------------------------------------------------------- constructor

    /**
     * Creates an {@code OrderRepository} targeting the specified database vendor.
     *
     * @param dbType the database vendor ({@link DatabaseType#POSTGRESQL} or
     *               {@link DatabaseType#MYSQL})
     */
    public OrderRepository(final DatabaseType dbType) {
        super(dbType);
    }

    // ---------------------------------------------------------------- query methods

    /**
     * Looks up an order by its surrogate primary key.
     *
     * @param id the database-generated primary key
     * @return an {@link Optional} containing the {@link OrderRecord}, or
     *         {@link Optional#empty()} if no row with that id exists
     */
    public Optional<OrderRecord> findById(final long id) {
        return one(SQL_FIND_BY_ID, ORDER_MAPPER, id);
    }

    /**
     * Returns all rows from the {@code orders} table ordered by primary key.
     *
     * <p>Intended for small test data sets. Do not use on a table with millions
     * of rows without adding a LIMIT clause.</p>
     *
     * @return immutable list of all order records
     */
    public List<OrderRecord> findAll() {
        return list(SQL_FIND_ALL, ORDER_MAPPER);
    }

    /**
     * Returns all orders placed by a specific user, most-recent first.
     *
     * <p>This is the primary lookup used in end-to-end tests that verify the
     * checkout flow: after a simulated purchase, the test asserts that an order
     * exists in the database for the logged-in user.</p>
     *
     * @param userId the primary key of the user whose orders to retrieve
     * @return list of orders for the user; empty if the user has no orders
     */
    public List<OrderRecord> findByUserId(final long userId) {
        return list(SQL_FIND_BY_USER_ID, ORDER_MAPPER, userId);
    }

    /**
     * Returns all orders with the specified status, most-recent first.
     *
     * <p>Useful for assertions such as "all orders created in this test batch
     * start with {@code 'pending'} status before fulfilment processing".</p>
     *
     * @param status the order status to filter by (e.g. {@code "pending"},
     *               {@code "shipped"})
     * @return list of matching orders; empty if none have that status
     */
    public List<OrderRecord> findByStatus(final String status) {
        return list(SQL_FIND_BY_STATUS, ORDER_MAPPER, status);
    }

    /**
     * Returns all orders for a specific user with the specified status.
     *
     * <p>Combines user and status filters in a single parameterised query,
     * avoiding an in-memory filter that would require fetching all user orders
     * across a large result set.</p>
     *
     * @param userId the user's primary key
     * @param status the order status filter
     * @return matching orders; empty list if none found
     */
    public List<OrderRecord> findByUserIdAndStatus(final long userId, final String status) {
        return list(SQL_FIND_BY_USER_AND_STATUS, ORDER_MAPPER, userId, status);
    }

    /**
     * Returns the total number of rows in the {@code orders} table.
     *
     * @return row count (0 if the table is empty)
     */
    public long count() {
        return scalar(SQL_COUNT)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of orders placed by a specific user.
     *
     * @param userId the user's primary key
     * @return order count for that user (0 if none)
     */
    public long countByUserId(final long userId) {
        return scalar(SQL_COUNT_BY_USER, userId)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of orders with the specified status.
     *
     * @param status the order status to count
     * @return count of orders matching that status
     */
    public long countByStatus(final String status) {
        return scalar(SQL_COUNT_BY_STATUS, status)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the sum of all order totals for a specific user.
     *
     * <p>{@code COALESCE(SUM(total), 0)} ensures that a user with no orders
     * returns {@link BigDecimal#ZERO} rather than {@code null}, preventing
     * {@link NullPointerException}s in caller code and assertion failures on
     * {@code Optional.empty()}.</p>
     *
     * @param userId the user's primary key
     * @return the sum of all order totals for the user; {@link BigDecimal#ZERO}
     *         if the user has no orders
     */
    public BigDecimal sumTotalByUserId(final long userId) {
        return scalar(SQL_SUM_TOTAL_BY_USER, userId)
                .map(v -> new BigDecimal(v.toString()))
                .orElse(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------- DML methods

    /**
     * Inserts a new order row.
     *
     * <p>The {@code id} and {@code created_at} columns are generated by the
     * database. After insertion, use {@link #findByUserId} to retrieve the
     * generated values if needed.</p>
     *
     * <p><strong>SQL injection prevention:</strong> all five parameters are bound
     * via {@code PreparedStatement.setObject} — never concatenated into SQL.</p>
     *
     * @param userId    the customer's primary key (FK to {@code users.id})
     * @param productId the product's primary key (FK to {@code products.id})
     * @param quantity  number of units ordered (must be {@literal >} 0)
     * @param total     order monetary total (use {@link BigDecimal} for precision)
     * @param status    initial lifecycle status (typically {@code "pending"})
     * @return number of rows inserted (expected: {@code 1})
     */
    public int insert(
            final long userId,
            final long productId,
            final int quantity,
            final BigDecimal total,
            final String status) {
        return execute(SQL_INSERT, userId, productId, quantity, total, status);
    }

    /**
     * Updates the lifecycle status of an existing order.
     *
     * <p>Status transitions (e.g. {@code pending → confirmed → shipped →
     * delivered}) are driven by application logic; this method is the single
     * parameterised update path for those transitions.</p>
     *
     * @param id        the order's primary key
     * @param newStatus the new status string
     * @return number of rows updated ({@code 0} if no order with that id,
     *         {@code 1} if updated successfully)
     */
    public int updateStatus(final long id, final String newStatus) {
        return execute(SQL_UPDATE_STATUS, newStatus, id);
    }

    /**
     * Hard-deletes an order by primary key.
     *
     * <p>Intended for test-data cleanup in {@code @AfterMethod}. In production
     * scenarios, prefer a soft-delete or status update to preserve audit history.</p>
     *
     * @param id the order's primary key
     * @return number of rows deleted ({@code 0} or {@code 1})
     */
    public int deleteById(final long id) {
        return execute(SQL_DELETE_BY_ID, id);
    }

    /**
     * Deletes all orders for a specific user.
     *
     * <p>Convenience cleanup method for tests that insert multiple orders for a
     * test user and need to restore the database to its pre-test state.</p>
     *
     * @param userId the user's primary key
     * @return number of rows deleted
     */
    public int deleteByUserId(final long userId) {
        return execute(SQL_DELETE_BY_USER_ID, userId);
    }
}
