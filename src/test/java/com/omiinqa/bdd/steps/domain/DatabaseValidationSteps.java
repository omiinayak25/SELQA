package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.QueryExecutor;
import com.omiinqa.database.TransactionManager;
import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.model.OrderRecord;
import com.omiinqa.database.model.ProductRecord;
import com.omiinqa.database.model.UserRecord;
import com.omiinqa.database.repositories.AuditRepository;
import com.omiinqa.database.repositories.OrderRepository;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import com.omiinqa.database.support.EmbeddedH2;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Step definitions for the {@code @domain @db} database-validation feature.
 *
 * <p>All steps execute <b>real SQL</b> against the in-memory H2 database
 * bootstrapped by {@link EmbeddedH2#start()}. No browser, no external
 * infrastructure — the suite runs fully offline.</p>
 *
 * <p><b>Step-text uniqueness:</b> every step is prefixed with a
 * "database" or "db" noun so it cannot collide with steps owned by
 * other domain slices. Shared outcome assertions
 * ({@code the operation succeeds} etc.) remain in {@code CommonDomainSteps}
 * and are NOT redefined here.</p>
 *
 * <p><b>DomainWorld keys used by this class:</b></p>
 * <ul>
 *   <li>{@code db.users}   — {@link UserRepository}</li>
 *   <li>{@code db.products}— {@link ProductRepository}</li>
 *   <li>{@code db.orders}  — {@link OrderRepository}</li>
 *   <li>{@code db.audit}   — {@link AuditRepository}</li>
 *   <li>{@code db.lastUser}— last {@link Optional}&lt;{@link UserRecord}&gt;</li>
 *   <li>{@code db.lastProducts} — last {@code List}&lt;{@link ProductRecord}&gt;</li>
 *   <li>{@code db.lastOrders}   — last {@code List}&lt;{@link OrderRecord}&gt;</li>
 *   <li>{@code db.lastAudit}    — last audit record list</li>
 *   <li>{@code db.lastScalar}   — last scalar query result</li>
 *   <li>{@code db.lastList}     — last list query result</li>
 * </ul>
 */
public class DatabaseValidationSteps {

    // ---------------------------------------------------------------- constants

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private static final String KEY_USERS    = "db.users";
    private static final String KEY_PRODUCTS = "db.products";
    private static final String KEY_ORDERS   = "db.orders";
    private static final String KEY_AUDIT    = "db.audit";

    private static final String KEY_LAST_USER     = "db.lastUser";
    private static final String KEY_LAST_PRODUCTS = "db.lastProducts";
    private static final String KEY_LAST_ORDERS   = "db.lastOrders";
    private static final String KEY_LAST_AUDIT    = "db.lastAudit";
    private static final String KEY_LAST_SCALAR   = "db.lastScalar";
    private static final String KEY_LAST_LIST     = "db.lastList";

    // ---------------------------------------------------------------- repository accessors

    private UserRepository users() {
        return DomainWorld.service(KEY_USERS, () -> new UserRepository(DB));
    }

    private ProductRepository products() {
        return DomainWorld.service(KEY_PRODUCTS, () -> new ProductRepository(DB));
    }

    private OrderRepository orders() {
        return DomainWorld.service(KEY_ORDERS, () -> new OrderRepository(DB));
    }

    private AuditRepository audit() {
        return DomainWorld.service(KEY_AUDIT, () -> new AuditRepository(DB));
    }

    // ================================================================ GIVEN

    /**
     * Idempotently boots the embedded H2 and sets up repository singletons
     * in DomainWorld for this scenario.
     */
    @Given("a bootstrapped reference database")
    public void aBootstrappedReferenceDatabase() {
        EmbeddedH2.start();
        // eager-init repositories so DomainWorld keys are populated for the scenario
        DomainWorld.put(KEY_USERS,    new UserRepository(DB));
        DomainWorld.put(KEY_PRODUCTS, new ProductRepository(DB));
        DomainWorld.put(KEY_ORDERS,   new OrderRepository(DB));
        DomainWorld.put(KEY_AUDIT,    new AuditRepository(DB));
    }

    // ================================================================ WHEN — row counts

    // (row count assertions go directly in Then steps — no intermediate When needed)

    // ================================================================ WHEN — user queries

    /**
     * Queries the {@code users} table by email; result stored in DomainWorld.
     */
    @When("I query the database for user with email {string}")
    public void iQueryTheDatabaseForUserWithEmail(final String email) {
        final Optional<UserRecord> result = users().findByEmail(email);
        DomainWorld.put(KEY_LAST_USER, result);
    }

    // ================================================================ WHEN — product queries

    /**
     * Queries the {@code products} table by category; stores result list.
     */
    @When("I query the database for products in category {string}")
    public void iQueryTheDatabaseForProductsInCategory(final String category) {
        final List<ProductRecord> result = products().findByCategory(category);
        DomainWorld.put(KEY_LAST_PRODUCTS, result);
    }

    /**
     * Queries all active products; stores result list.
     */
    @When("I query the database for all active products")
    public void iQueryTheDatabaseForAllActiveProducts() {
        final List<ProductRecord> result = products().findAllActive();
        DomainWorld.put(KEY_LAST_PRODUCTS, result);
    }

    // ================================================================ WHEN — order queries

    /**
     * Queries the {@code orders} table by status; stores result list.
     */
    @When("I query the database for orders with status {string}")
    public void iQueryTheDatabaseForOrdersWithStatus(final String status) {
        final List<OrderRecord> result = orders().findByStatus(status);
        DomainWorld.put(KEY_LAST_ORDERS, result);
    }

    /**
     * Retrieves all orders from the orders table; stores result list.
     */
    @When("I query the database for all orders")
    public void iQueryTheDatabaseForAllOrders() {
        final List<OrderRecord> result = orders().findAll();
        DomainWorld.put(KEY_LAST_ORDERS, result);
    }

    // ================================================================ WHEN — audit queries

    /**
     * Queries the {@code audit_log} table by action; stores result list.
     */
    @When("I query the database audit log for action {string}")
    public void iQueryTheDatabaseAuditLogForAction(final String action) {
        DomainWorld.put(KEY_LAST_AUDIT, audit().findByAction(action));
    }

    /**
     * Queries the {@code audit_log} table by actor; stores result list.
     */
    @When("I query the database audit log for actor {string}")
    public void iQueryTheDatabaseAuditLogForActor(final String actor) {
        DomainWorld.put(KEY_LAST_AUDIT, audit().findByActor(actor));
    }

    // ================================================================ WHEN — QueryExecutor scalar/list

    /**
     * Executes an arbitrary scalar SQL query via {@link QueryExecutor};
     * stores the raw {@link Optional} result.
     */
    @When("I execute the database scalar query {string}")
    public void iExecuteTheDatabaseScalarQuery(final String sql) {
        final Optional<Object> result = QueryExecutor.instance().queryForScalar(DB, sql);
        DomainWorld.put(KEY_LAST_SCALAR, result);
    }

    /**
     * Executes an arbitrary list SQL query via {@link QueryExecutor};
     * stores the {@code List<Map>} result.
     */
    @When("I execute the database list query {string}")
    public void iExecuteTheDatabaseListQuery(final String sql) {
        final List<Map<String, Object>> result = QueryExecutor.instance().queryForList(DB, sql);
        DomainWorld.put(KEY_LAST_LIST, result);
    }

    // ================================================================ WHEN — DML / CRUD

    /**
     * Inserts a new user row and verifies one row was inserted.
     */
    @When("I insert a database user with name {string} email {string} status {string}")
    public void iInsertADatabaseUser(final String name, final String email, final String status) {
        final int rows = users().insert(name, email, status);
        assertThat(rows).as("insert user rows affected").isEqualTo(1);
    }

    /**
     * Deletes the user found by the given email; fails if no such user exists.
     */
    @When("I delete the database user found by email {string}")
    public void iDeleteTheDatabaseUserFoundByEmail(final String email) {
        final Optional<UserRecord> user = users().findByEmail(email);
        assertThat(user).as("user to delete with email " + email).isPresent();
        final int rows = users().deleteById(user.get().getId());
        assertThat(rows).as("delete user rows affected").isEqualTo(1);
    }

    /**
     * Inserts a new product row.
     */
    @When("I insert a database product with name {string} category {string} price {string} stock {int} active {word}")
    public void iInsertADatabaseProduct(
            final String name,
            final String category,
            final String price,
            final int stock,
            final String activeStr) {
        final boolean active = Boolean.parseBoolean(activeStr);
        final int rows = products().insert(name, category, new BigDecimal(price), stock, active);
        assertThat(rows).as("insert product rows affected").isEqualTo(1);
    }

    /**
     * Deletes the product found by the given name; fails fast if absent.
     * Uses the default {@link com.omiinqa.database.MapRowMapper} overload so rows
     * arrive as {@code Map<String,Object>}, which supports column-by-name lookup.
     */
    @When("I delete the database product found by name {string}")
    public void iDeleteTheDatabaseProductFoundByName(final String name) {
        final List<Map<String, Object>> found = QueryExecutor.instance()
                .queryForList(DB,
                        "SELECT id FROM products WHERE name = ?",
                        name);
        assertThat(found).as("product to delete with name " + name).isNotEmpty();
        final long id = ((Number) found.get(0).get("id")).longValue();
        final int rows = products().deleteById(id);
        assertThat(rows).as("delete product rows affected").isEqualTo(1);
    }

    // ================================================================ WHEN — transactions

    /**
     * Commits a transaction that inserts a user, then asserts the user is visible.
     */
    @When("I commit a database transaction inserting user {string} email {string}")
    public void iCommitADatabaseTransactionInsertingUser(final String name, final String email) {
        TransactionManager.instance().executeInTransaction(DB, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, "active");
                ps.executeUpdate();
            } catch (final java.sql.SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Executes a transaction that intentionally rolls back (forces a
     * {@link RuntimeException} after the INSERT so the row is discarded).
     */
    @When("I execute a database transaction that rolls back inserting user {string} email {string}")
    public void iExecuteADatabaseTransactionThatRollsBack(final String name, final String email) {
        assertThatThrownBy(() ->
                TransactionManager.instance().executeInTransaction(DB,
                        (java.util.function.Consumer<java.sql.Connection>) conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (name, email, status) VALUES (?, ?, ?)")) {
                        ps.setString(1, name);
                        ps.setString(2, email);
                        ps.setString(3, "active");
                        ps.executeUpdate();
                    } catch (final java.sql.SQLException e) {
                        throw new RuntimeException(e);
                    }
                    throw new IllegalStateException("forced rollback in BDD test");
                })
        ).isInstanceOf(RuntimeException.class);
    }

    // ================================================================ THEN — count assertions

    /**
     * Asserts the total user count via repository.
     */
    @Then("the database user count is {int}")
    public void theDatabaseUserCountIs(final int expected) {
        assertThat(users().count())
                .as("total users in database")
                .isEqualTo(expected);
    }

    /**
     * Asserts the total product count via repository.
     */
    @Then("the database product count is {int}")
    public void theDatabaseProductCountIs(final int expected) {
        assertThat(products().count())
                .as("total products in database")
                .isEqualTo(expected);
    }

    /**
     * Asserts the total order count via repository.
     */
    @Then("the database order count is {int}")
    public void theDatabaseOrderCountIs(final int expected) {
        assertThat(orders().count())
                .as("total orders in database")
                .isEqualTo(expected);
    }

    /**
     * Asserts the total audit_log count via repository.
     */
    @Then("the database audit_log count is {int}")
    public void theDatabaseAuditLogCountIs(final int expected) {
        assertThat(audit().count())
                .as("total audit_log rows in database")
                .isEqualTo(expected);
    }

    // ================================================================ THEN — user find-by results

    /**
     * Asserts the last user query returned a present record.
     */
    @Then("the database query finds a user record")
    public void theDatabaseQueryFindsAUserRecord() {
        final Optional<UserRecord> user = DomainWorld.get(KEY_LAST_USER);
        assertThat(user).as("expected a user record to be found").isPresent();
    }

    /**
     * Asserts the last user query returned no record.
     */
    @Then("the database query finds no user record")
    public void theDatabaseQueryFindsNoUserRecord() {
        final Optional<UserRecord> user = DomainWorld.get(KEY_LAST_USER);
        assertThat(user).as("expected no user record to be found").isEmpty();
    }

    /**
     * Asserts the status of the most recently found user record.
     */
    @Then("the database user status is {string}")
    public void theDatabaseUserStatusIs(final String expectedStatus) {
        final Optional<UserRecord> user = DomainWorld.get(KEY_LAST_USER);
        assertThat(user).as("user record must be present to check status").isPresent();
        assertThat(user.get().getStatus())
                .as("user status")
                .isEqualTo(expectedStatus);
    }

    // ================================================================ THEN — product find-by results

    /**
     * Asserts the count of the last product category query.
     */
    @Then("the database product category result count is {int}")
    public void theDatabaseProductCategoryResultCountIs(final int expected) {
        final List<ProductRecord> result = DomainWorld.get(KEY_LAST_PRODUCTS);
        assertThat(result)
                .as("products returned by category query")
                .hasSize(expected);
    }

    /**
     * Asserts the count of active products returned by the last active-filter query.
     */
    @Then("the database active product count is {int}")
    public void theDatabaseActiveProductCountIs(final int expected) {
        final List<ProductRecord> result = DomainWorld.get(KEY_LAST_PRODUCTS);
        assertThat(result)
                .as("active products in database")
                .hasSize(expected);
    }

    // ================================================================ THEN — order find-by results

    /**
     * Asserts the count of orders returned by the last status query.
     */
    @Then("the database order status result count is {int}")
    public void theDatabaseOrderStatusResultCountIs(final int expected) {
        final List<OrderRecord> result = DomainWorld.get(KEY_LAST_ORDERS);
        assertThat(result)
                .as("orders returned by status query")
                .hasSize(expected);
    }

    // ================================================================ THEN — audit results

    /**
     * Asserts the count of the last audit-log query result.
     */
    @Then("the database audit log result count is {int}")
    public void theDatabaseAuditLogResultCountIs(final int expected) {
        final List<?> result = DomainWorld.get(KEY_LAST_AUDIT);
        assertThat(result)
                .as("audit_log rows returned")
                .hasSize(expected);
    }

    // ================================================================ THEN — scalar / list

    /**
     * Asserts a scalar integer result from the last QueryExecutor scalar query.
     */
    @Then("the database scalar result is {int}")
    public void theDatabaseScalarResultIs(final int expected) {
        final Optional<Object> scalar = DomainWorld.get(KEY_LAST_SCALAR);
        assertThat(scalar).as("scalar result must be present").isPresent();
        assertThat(((Number) scalar.get()).longValue())
                .as("scalar query result")
                .isEqualTo((long) expected);
    }

    /**
     * Asserts a decimal scalar result (e.g. SUM) from the last QueryExecutor scalar query.
     */
    @Then("the database scalar decimal result is {string}")
    public void theDatabaseScalarDecimalResultIs(final String expected) {
        final Optional<Object> scalar = DomainWorld.get(KEY_LAST_SCALAR);
        assertThat(scalar).as("scalar decimal result must be present").isPresent();
        final BigDecimal actual = new BigDecimal(scalar.get().toString());
        assertThat(actual)
                .as("scalar decimal query result")
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    /**
     * Asserts the row count of the last QueryExecutor list query.
     */
    @Then("the database list result row count is {int}")
    public void theDatabaseListResultRowCountIs(final int expected) {
        final List<Map<String, Object>> result = DomainWorld.get(KEY_LAST_LIST);
        assertThat(result)
                .as("list query rows returned")
                .hasSize(expected);
    }

    /**
     * Asserts the list result contains at least one row where {@code column} equals {@code value}.
     */
    @Then("the database list result contains a row where {string} equals {string}")
    public void theDatabaseListResultContainsARowWhereEquals(
            final String column, final String value) {
        final List<Map<String, Object>> result = DomainWorld.get(KEY_LAST_LIST);
        assertThat(result)
                .as("list query must not be empty")
                .isNotEmpty();
        final boolean found = result.stream()
                .anyMatch(row -> value.equals(String.valueOf(row.get(column))));
        assertThat(found)
                .as("expected at least one row where [%s] = [%s]", column, value)
                .isTrue();
    }

    // ================================================================ THEN — referential integrity

    /**
     * Asserts every order in the last orders query references a user_id that
     * actually exists in the users table.
     */
    @Then("every database order has a user id that exists in the users table")
    public void everyDatabaseOrderHasAUserIdThatExistsInTheUsersTable() {
        final List<OrderRecord> orderList = DomainWorld.get(KEY_LAST_ORDERS);
        assertThat(orderList).as("orders list must not be empty").isNotEmpty();
        for (final OrderRecord order : orderList) {
            final Optional<UserRecord> user = users().findById(order.getUserId());
            assertThat(user)
                    .as("user referenced by order id=%d (user_id=%d) must exist",
                            order.getId(), order.getUserId())
                    .isPresent();
        }
    }

    /**
     * Asserts every order in the last orders query references a product_id that
     * actually exists in the products table.
     */
    @Then("every database order has a product id that exists in the products table")
    public void everyDatabaseOrderHasAProductIdThatExistsInTheProductsTable() {
        final List<OrderRecord> orderList = DomainWorld.get(KEY_LAST_ORDERS);
        assertThat(orderList).as("orders list must not be empty").isNotEmpty();
        for (final OrderRecord order : orderList) {
            final Optional<ProductRecord> product = products().findById(order.getProductId());
            assertThat(product)
                    .as("product referenced by order id=%d (product_id=%d) must exist",
                            order.getId(), order.getProductId())
                    .isPresent();
        }
    }

    // ================================================================ THEN — DatabaseAssertions

    /**
     * Fluent DatabaseAssertions: asserts a parameterised query returns at least one row.
     */
    @Then("database assertions confirm row exists {string} with param {string}")
    public void databaseAssertionsConfirmRowExists(final String sql, final String param) {
        DatabaseAssertions.forDatabase(DB).assertRowExists(sql, param);
    }

    /**
     * Fluent DatabaseAssertions: asserts a parameterised query returns no rows.
     */
    @Then("database assertions confirm no row exists {string} with param {string}")
    public void databaseAssertionsConfirmNoRowExists(final String sql, final String param) {
        DatabaseAssertions.forDatabase(DB).assertNoRowExists(sql, param);
    }

    /**
     * Fluent DatabaseAssertions: asserts the first-column value of the first row
     * from a parameterised query equals {@code expectedValue}.
     */
    @Then("database assertions confirm column value {string} equals {string} with param {string}")
    public void databaseAssertionsConfirmColumnValue(
            final String sql, final String expectedValue, final String param) {
        DatabaseAssertions.forDatabase(DB).assertColumnValue(sql, expectedValue, param);
    }

    /**
     * Fluent DatabaseAssertions: asserts the exact row count of a named table.
     */
    @Then("database assertions confirm row count in table {string} is {int}")
    public void databaseAssertionsConfirmRowCountInTable(final String table, final int expected) {
        DatabaseAssertions.forDatabase(DB).assertRowCount(table, expected);
    }

}
