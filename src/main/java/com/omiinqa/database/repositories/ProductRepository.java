package com.omiinqa.database.repositories;

import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.RowMapper;
import com.omiinqa.database.model.ProductRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@code products} table (Repository pattern).
 *
 * <p><b>Design pattern — Repository:</b>
 * Encapsulates all SQL targeting the {@code products} table. Test assertions
 * about product inventory, pricing, and catalogue state use this repository's
 * named methods rather than ad-hoc SQL, ensuring consistent query logic and
 * a single point of change when the schema evolves.</p>
 *
 * <p><b>SQL injection prevention:</b> Every method uses parameterised queries
 * via {@link com.omiinqa.database.QueryExecutor}. User-controllable values
 * (product name, category, price) are always bound through {@code ?}
 * placeholders — never concatenated into the SQL string.</p>
 *
 * <p><b>Schema</b> (table: {@code products}):</p>
 * <pre>
 *   id          BIGINT PRIMARY KEY AUTO_INCREMENT / SERIAL
 *   name        VARCHAR(255) NOT NULL
 *   category    VARCHAR(100)
 *   price       NUMERIC(10,2)
 *   stock_qty   INT          DEFAULT 0
 *   active      BOOLEAN      DEFAULT TRUE
 * </pre>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   ProductRepository repo = new ProductRepository(DatabaseType.MYSQL);
 *   List<ProductRecord> active = repo.findByCategory("electronics");
 *   assertThat(active).allMatch(p -> p.getActive());
 * }</pre>
 */
public class ProductRepository extends AbstractRepository<ProductRecord> {

    // ---------------------------------------------------------------- SQL constants

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, category, price, stock_qty, active FROM products WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, name, category, price, stock_qty, active FROM products ORDER BY id";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT id, name, category, price, stock_qty, active FROM products "
            + "WHERE category = ? ORDER BY name";

    private static final String SQL_FIND_ACTIVE =
            "SELECT id, name, category, price, stock_qty, active FROM products "
            + "WHERE active = TRUE ORDER BY name";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM products";

    private static final String SQL_COUNT_BY_CATEGORY =
            "SELECT COUNT(*) FROM products WHERE category = ?";

    private static final String SQL_INSERT =
            "INSERT INTO products (name, category, price, stock_qty, active) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_STOCK =
            "UPDATE products SET stock_qty = ? WHERE id = ?";

    private static final String SQL_DELETE_BY_ID =
            "DELETE FROM products WHERE id = ?";

    private static final String SQL_DEACTIVATE =
            "UPDATE products SET active = FALSE WHERE id = ?";

    // ---------------------------------------------------------------- row mapper

    /**
     * Converts a JDBC row to a {@link ProductRecord}.
     *
     * <p>{@link BigDecimal} is used for price via {@code getBigDecimal} to
     * avoid floating-point imprecision during assertion comparisons.</p>
     */
    private static final RowMapper<ProductRecord> PRODUCT_MAPPER = (rs, rowNum) ->
            ProductRecord.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .category(rs.getString("category"))
                    .price(rs.getBigDecimal("price"))
                    .stockQty(rs.getInt("stock_qty"))
                    .active(rs.getBoolean("active"))
                    .build();

    // ---------------------------------------------------------------- constructor

    /**
     * Creates a {@code ProductRepository} targeting the specified database vendor.
     *
     * @param dbType the database vendor ({@link DatabaseType#POSTGRESQL} or
     *               {@link DatabaseType#MYSQL})
     */
    public ProductRepository(final DatabaseType dbType) {
        super(dbType);
    }

    // ---------------------------------------------------------------- query methods

    /**
     * Looks up a product by primary key.
     *
     * @param id the surrogate primary key
     * @return optional {@link ProductRecord}; empty if no row with that id exists
     */
    public Optional<ProductRecord> findById(final long id) {
        return one(SQL_FIND_BY_ID, PRODUCT_MAPPER, id);
    }

    /**
     * Returns all products ordered by primary key.
     *
     * @return immutable list of all product records
     */
    public List<ProductRecord> findAll() {
        return list(SQL_FIND_ALL, PRODUCT_MAPPER);
    }

    /**
     * Returns all products in the specified category, ordered by name.
     *
     * @param category the category string (case-sensitive, exact match)
     * @return list of matching products; empty if no products in that category
     */
    public List<ProductRecord> findByCategory(final String category) {
        return list(SQL_FIND_BY_CATEGORY, PRODUCT_MAPPER, category);
    }

    /**
     * Returns all products where {@code active = TRUE}, ordered by name.
     *
     * @return list of active product records
     */
    public List<ProductRecord> findAllActive() {
        return list(SQL_FIND_ACTIVE, PRODUCT_MAPPER);
    }

    /**
     * Returns the total number of rows in the {@code products} table.
     *
     * @return row count (0 if empty)
     */
    public long count() {
        return scalar(SQL_COUNT)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of products in the specified category.
     *
     * @param category the category to count
     * @return row count for that category
     */
    public long countByCategory(final String category) {
        return scalar(SQL_COUNT_BY_CATEGORY, category)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    // ---------------------------------------------------------------- DML methods

    /**
     * Inserts a new product record.
     *
     * @param name     product name
     * @param category product category
     * @param price    unit price (use {@link BigDecimal} to avoid rounding issues)
     * @param stockQty initial stock quantity
     * @param active   whether the product is catalogue-visible
     * @return number of rows inserted (expected: {@code 1})
     */
    public int insert(
            final String name,
            final String category,
            final BigDecimal price,
            final int stockQty,
            final boolean active) {
        return execute(SQL_INSERT, name, category, price, stockQty, active);
    }

    /**
     * Updates the stock quantity for a product by its primary key.
     *
     * @param id       the product's primary key
     * @param newStock the new stock quantity
     * @return number of rows updated ({@code 0} or {@code 1})
     */
    public int updateStock(final long id, final int newStock) {
        return execute(SQL_UPDATE_STOCK, newStock, id);
    }

    /**
     * Soft-deletes a product by setting {@code active = FALSE}.
     * Preferred over hard delete to preserve audit history.
     *
     * @param id the product's primary key
     * @return number of rows updated ({@code 0} or {@code 1})
     */
    public int deactivate(final long id) {
        return execute(SQL_DEACTIVATE, id);
    }

    /**
     * Hard-deletes a product row by primary key.
     * Use only when cleaning up test data in {@code @AfterMethod}.
     *
     * @param id the product's primary key
     * @return number of rows deleted ({@code 0} or {@code 1})
     */
    public int deleteById(final long id) {
        return execute(SQL_DELETE_BY_ID, id);
    }
}
