package com.omiinqa.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default {@link RowMapper} implementation that converts each JDBC row into a
 * {@code Map<String, Object>} keyed by <em>lower-cased column label</em>.
 *
 * <p><b>Design pattern — Concrete Strategy:</b> This class is the default
 * concrete Strategy paired with the {@link RowMapper} interface. It is
 * intentionally general-purpose: any query result can be captured as a list of
 * maps without needing a domain class, which is ideal for ad-hoc database
 * assertions in test automation where the exact schema may not be worth
 * modelling as a POJO.</p>
 *
 * <p><b>Column ordering:</b> A {@link LinkedHashMap} is used so column order
 * is preserved as returned by the database, making log output and assertion
 * error messages predictable.</p>
 *
 * <p><b>Null handling:</b> Database {@code NULL} values are stored as Java
 * {@code null} in the map. Callers should use
 * {@link java.util.Optional} or null-safe assertions when reading values.</p>
 *
 * <p><b>Column label vs. column name:</b> {@link ResultSetMetaData#getColumnLabel(int)}
 * is used (not {@code getColumnName}) to honour SQL aliases
 * ({@code SELECT count(*) AS total ...}) and stay compatible with both
 * PostgreSQL and MySQL result-set conventions.</p>
 *
 * @see RowMapper
 * @see QueryExecutor
 */
public final class MapRowMapper implements RowMapper<Map<String, Object>> {

    /**
     * Converts the current result-set row into a column-name-to-value map.
     *
     * @param rs        the {@link ResultSet} positioned at the row to map
     * @param rowNumber zero-based row index (unused here but required by interface)
     * @return a {@link LinkedHashMap} with one entry per column, keys lowercased
     * @throws SQLException if a JDBC accessor call fails
     */
    @Override
    public Map<String, Object> mapRow(final ResultSet rs, final int rowNumber)
            throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = meta.getColumnCount();
        final Map<String, Object> row = new LinkedHashMap<>(columnCount * 2);
        for (int i = 1; i <= columnCount; i++) {
            // getColumnLabel honours aliases (e.g. SELECT name AS full_name)
            final String label = meta.getColumnLabel(i).toLowerCase();
            row.put(label, rs.getObject(i));
        }
        return row;
    }
}
