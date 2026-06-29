package com.omiinqa.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Strategy interface that converts one row of a JDBC {@link ResultSet} into a
 * strongly-typed domain object.
 *
 * <p><b>Design pattern — Strategy:</b> {@code RowMapper} is the Strategy
 * abstraction in the classic Template Method + Strategy combination used by
 * {@link QueryExecutor}. The executor owns the iteration loop (the template);
 * callers supply the conversion algorithm (the strategy). This decouples
 * query execution from result-object construction and makes both independently
 * testable and replaceable.</p>
 *
 * <p><b>Functional interface:</b> Declaring the single-method interface
 * {@code @FunctionalInterface} allows callers to supply lambda expressions
 * instead of anonymous inner classes, keeping test code concise:</p>
 * <pre>{@code
 *   RowMapper<String> emailMapper = rs -> rs.getString("email");
 *   List<String> emails = executor.queryForList(POSTGRESQL, sql, emailMapper);
 * }</pre>
 *
 * <p>The default implementation {@link MapRowMapper} is returned by
 * {@link #toMap()} and covers the common case of ad-hoc validation where the
 * caller just needs a column-name-to-value lookup without a domain class.</p>
 *
 * @param <T> the type that each row maps to
 *
 * @see MapRowMapper
 * @see QueryExecutor
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the given {@link ResultSet} to an instance of
     * type {@code T}.
     *
     * <p>Implementations must NOT advance the cursor — {@link QueryExecutor}
     * controls cursor movement. The {@code ResultSet} is already positioned
     * at the correct row when this method is called.</p>
     *
     * @param rs         a {@code ResultSet} positioned at a row to be mapped;
     *                   must not be closed or advanced by the implementation
     * @param rowNumber  zero-based index of the current row within the result
     *                   set (useful for ordered-list construction or debugging)
     * @return the domain object representing this row; must not be {@code null}
     *         unless the caller's contract explicitly allows it
     * @throws SQLException if any JDBC accessor call fails
     */
    T mapRow(ResultSet rs, int rowNumber) throws SQLException;

    /**
     * Convenience factory that returns the canonical {@link MapRowMapper},
     * which converts each row to a {@code Map<String, Object>} keyed by
     * lower-cased column label.
     *
     * <p>Useful for quick validation assertions where a domain class does not
     * yet exist:</p>
     * <pre>{@code
     *   List<Map<String,Object>> rows =
     *       executor.queryForList(POSTGRESQL, sql, RowMapper.toMap());
     * }</pre>
     *
     * @return a {@code RowMapper} that produces {@code Map<String, Object>}
     */
    static RowMapper<java.util.Map<String, Object>> toMap() {
        return new MapRowMapper();
    }
}
