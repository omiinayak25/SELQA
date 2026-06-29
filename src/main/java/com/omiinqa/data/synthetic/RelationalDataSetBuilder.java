package com.omiinqa.data.synthetic;

import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Product;
import com.omiinqa.data.model.User;
import com.omiinqa.exceptions.DataException;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds a small, <em>internally consistent relational dataset</em> for use in
 * database seeding and end-to-end test scenarios.
 *
 * <p><strong>Pattern:</strong> Builder — a dedicated builder class (not Lombok's
 * generic one) that gives full control over the construction sequence so that
 * referential integrity can be enforced before the dataset is sealed.</p>
 *
 * <h2>What "relational" means here</h2>
 * <p>The builder produces three correlated in-memory collections:
 * <ol>
 *   <li>{@link User} list — the parent table.</li>
 *   <li>{@link Product} list — the catalogue.</li>
 *   <li>{@link OrderRecord} list — the child table; every order's
 *       {@code userId} is drawn from the generated user list and every order's
 *       {@code productId} is drawn from the product list, so referential
 *       integrity is guaranteed by construction.</li>
 * </ol>
 * </p>
 *
 * <h2>Determinism</h2>
 * <p>The seed is passed to {@link SyntheticDataGenerator} which delegates it to
 * {@link TestDataFaker}. An additional {@link Random} is kept for distribution
 * decisions (how many orders per user, which product is ordered) and is seeded
 * with the same value, so the entire dataset is reproducible.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   RelationalDataSet ds = RelationalDataSetBuilder.withSeed(42L)
 *       .users(5)
 *       .products(10)
 *       .ordersPerUser(3)
 *       .build();
 *
 *   List&lt;User&gt;        users    = ds.getUsers();
 *   List&lt;Product&gt;     products = ds.getProducts();
 *   List&lt;OrderRecord&gt; orders   = ds.getOrders();
 *
 *   // Referential integrity guaranteed: every orderId.userId is in users
 *   Set&lt;String&gt; userIds = users.stream().map(User::getId).collect(toSet());
 *   orders.forEach(o -&gt; assert userIds.contains(o.getUserId()));
 * }</pre>
 * </p>
 */
public final class RelationalDataSetBuilder {

    private static final Logger log = LoggerFactory.getLogger(RelationalDataSetBuilder.class);

    private final long seed;
    private final SyntheticDataGenerator generator;
    private final Random rng;

    private int userCount    = 5;
    private int productCount = 10;
    private int ordersPerUser = 2;

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a builder seeded for deterministic output.
     *
     * @param seed random seed; the same seed, datafaker version, and JDK produces
     *             an identical dataset
     * @return configured builder
     */
    public static RelationalDataSetBuilder withSeed(final long seed) {
        return new RelationalDataSetBuilder(seed);
    }

    /**
     * Creates a builder with a random (non-reproducible) seed.
     * Useful when only structural correctness (referential integrity) matters
     * rather than exact data values.
     *
     * @return configured builder
     */
    public static RelationalDataSetBuilder random() {
        return new RelationalDataSetBuilder(System.nanoTime());
    }

    private RelationalDataSetBuilder(final long seed) {
        this.seed      = seed;
        this.generator = new SyntheticDataGenerator(seed);
        this.rng       = new Random(seed);
        log.debug("RelationalDataSetBuilder created with seed={}", seed);
    }

    // -------------------------------------------------------------------------
    // Fluent configuration
    // -------------------------------------------------------------------------

    /**
     * Sets the number of users (parent rows) to generate.
     *
     * @param count number of users; must be &gt;= 1
     * @return {@code this} for chaining
     * @throws DataException if count &lt; 1
     */
    public RelationalDataSetBuilder users(final int count) {
        if (count < 1) {
            throw new DataException("users count must be >= 1 but was " + count);
        }
        this.userCount = count;
        return this;
    }

    /**
     * Sets the number of products (catalogue rows) to generate.
     *
     * @param count number of products; must be &gt;= 1
     * @return {@code this} for chaining
     * @throws DataException if count &lt; 1
     */
    public RelationalDataSetBuilder products(final int count) {
        if (count < 1) {
            throw new DataException("products count must be >= 1 but was " + count);
        }
        this.productCount = count;
        return this;
    }

    /**
     * Sets the number of orders to generate per user.
     *
     * @param count orders per user; must be &gt;= 0
     * @return {@code this} for chaining
     * @throws DataException if count &lt; 0
     */
    public RelationalDataSetBuilder ordersPerUser(final int count) {
        if (count < 0) {
            throw new DataException("ordersPerUser must be >= 0 but was " + count);
        }
        this.ordersPerUser = count;
        return this;
    }

    // -------------------------------------------------------------------------
    // Terminal operation
    // -------------------------------------------------------------------------

    /**
     * Builds and returns an immutable {@link RelationalDataSet}.
     *
     * <p>The generation sequence is:
     * <ol>
     *   <li>Generate {@code userCount} users.</li>
     *   <li>Generate {@code productCount} products.</li>
     *   <li>For each user, generate {@code ordersPerUser} orders where each order
     *       randomly picks a product from the product list. User IDs and product
     *       IDs are drawn from step 1 and 2 respectively, guaranteeing referential
     *       integrity.</li>
     * </ol>
     * </p>
     *
     * @return sealed, immutable-view dataset
     */
    public RelationalDataSet build() {
        log.debug("Building RelationalDataSet: {} users, {} products, {} orders/user",
                userCount, productCount, ordersPerUser);

        List<User>    users    = generator.generateUsers(userCount);
        List<Product> products = generator.generateProducts(productCount);
        List<OrderRecord> orders = buildOrders(users, products);

        log.info("RelationalDataSet built: {} users, {} products, {} orders (seed={})",
                users.size(), products.size(), orders.size(), seed);

        return new RelationalDataSet(users, products, orders);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<OrderRecord> buildOrders(final List<User> users,
                                          final List<Product> products) {
        List<OrderRecord> orders = new ArrayList<>(users.size() * ordersPerUser);
        // Fixed epoch anchor (2024-01-01T00:00:00Z) so createdAt is fully
        // deterministic for a given seed; using Instant.now() would make the
        // dataset wall-clock-dependent and break the reproducibility contract.
        Instant base = Instant.parse("2024-01-01T00:00:00Z");

        int orderSeq = 0;
        for (User user : users) {
            for (int i = 0; i < ordersPerUser; i++) {
                // Pick a random product from the catalogue — referential integrity guaranteed
                Product product = products.get(rng.nextInt(products.size()));
                int qty = 1 + rng.nextInt(5);

                OrderRecord order = OrderRecord.builder()
                        // Deterministic, seed-derived order id (NOT UUID.randomUUID(),
                        // which is non-seedable and would break reproducibility).
                        .orderId(String.format("ord-%05d-%08x", orderSeq++, rng.nextInt()))
                        .userId(user.getId())          // FK → users.id
                        .productId(product.getId())    // FK → products.id
                        .quantity(qty)
                        .unitPrice(product.getPrice())
                        .totalPrice(product.getPrice()
                                .multiply(BigDecimal.valueOf(qty))
                                .setScale(2, RoundingMode.HALF_UP))
                        .status(randomStatus())
                        .createdAt(base.minus(rng.nextInt(30), ChronoUnit.DAYS))
                        .build();
                orders.add(order);
            }
        }
        return orders;
    }

    private String randomStatus() {
        String[] statuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        return statuses[rng.nextInt(statuses.length)];
    }

    // =========================================================================
    // Inner class: OrderRecord
    // =========================================================================

    /**
     * Lightweight in-memory order record that represents a row in the orders
     * child table.
     *
     * <p>Fields mirror a typical e-commerce orders schema:
     * {@code order_id}, {@code user_id} (FK → users), {@code product_id}
     * (FK → products), {@code quantity}, {@code unit_price}, {@code total_price},
     * {@code status}, {@code created_at}.</p>
     *
     * <p>Lombok {@link Data} + {@link Builder} are used to keep this a clean
     * value object with no handwritten boilerplate.</p>
     */
    @Data
    @Builder
    public static final class OrderRecord {

        /** Unique order identifier (UUID string). */
        private final String orderId;

        /**
         * Foreign key referencing the owning user's {@link User#getId()}.
         * Guaranteed to match an entry in the dataset's user list.
         */
        private final String userId;

        /**
         * Foreign key referencing the ordered product's {@link Product#getId()}.
         * Guaranteed to match an entry in the dataset's product list.
         */
        private final String productId;

        /** Number of units ordered. */
        private final int quantity;

        /** Price per unit at the time of the order. */
        private final BigDecimal unitPrice;

        /** Pre-calculated total: {@code unitPrice × quantity}. */
        private final BigDecimal totalPrice;

        /**
         * Order lifecycle status: one of
         * {@code PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED}.
         */
        private final String status;

        /** Timestamp when the order was placed. */
        private final Instant createdAt;
    }

    // =========================================================================
    // Inner class: RelationalDataSet
    // =========================================================================

    /**
     * Immutable container for the three generated collections returned by
     * {@link RelationalDataSetBuilder#build()}.
     *
     * <p>All lists are wrapped in {@link Collections#unmodifiableList} to prevent
     * accidental mutation between test setup and the actual test body.</p>
     */
    public static final class RelationalDataSet {

        private final List<User>        users;
        private final List<Product>     products;
        private final List<OrderRecord> orders;

        private RelationalDataSet(final List<User> users,
                                  final List<Product> products,
                                  final List<OrderRecord> orders) {
            this.users    = Collections.unmodifiableList(users);
            this.products = Collections.unmodifiableList(products);
            this.orders   = Collections.unmodifiableList(orders);
        }

        /**
         * Returns the generated users (parent table).
         *
         * @return unmodifiable list of users
         */
        public List<User> getUsers() {
            return users;
        }

        /**
         * Returns the generated product catalogue.
         *
         * @return unmodifiable list of products
         */
        public List<Product> getProducts() {
            return products;
        }

        /**
         * Returns the generated orders (child table).
         * Every order's {@code userId} exists in {@link #getUsers()} and
         * every order's {@code productId} exists in {@link #getProducts()}.
         *
         * @return unmodifiable list of order records
         */
        public List<OrderRecord> getOrders() {
            return orders;
        }

        /**
         * Convenience summary for logging.
         *
         * @return human-readable dataset dimensions
         */
        @Override
        public String toString() {
            return String.format("RelationalDataSet{users=%d, products=%d, orders=%d}",
                    users.size(), products.size(), orders.size());
        }
    }
}
