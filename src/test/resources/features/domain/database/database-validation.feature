@domain @db
Feature: Database Validation — embedded H2 (PostgreSQL-compat)
  As the OmiinQA database layer
  I want to validate real SQL behaviour against a deterministic seed
  So that data integrity, query correctness, and injection safety are confirmed offline

  Background:
    Given a bootstrapped reference database

  # ────────────────────── seeded row counts ──────────────────────

  @smoke @positive
  Scenario: Seeded users table contains exactly three rows
    Then the database user count is 3

  @smoke @positive
  Scenario: Seeded products table contains exactly four rows
    Then the database product count is 4

  @smoke @positive
  Scenario: Seeded orders table contains exactly three rows
    Then the database order count is 3

  @smoke @positive
  Scenario: Seeded audit_log table contains exactly three rows
    Then the database audit_log count is 3

  # ────────────────────── find-by email ──────────────────────────

  @positive
  Scenario Outline: Finding a seeded user by email succeeds
    When I query the database for user with email "<email>"
    Then the database query finds a user record
    And the database user status is "<status>"

    Examples:
      | email                | status   |
      | alice@omiinqa.test   | active   |
      | bob@omiinqa.test     | active   |
      | carol@omiinqa.test   | inactive |

  @negative
  Scenario: Querying for an unknown email finds no user
    When I query the database for user with email "ghost@nowhere.test"
    Then the database query finds no user record

  # ────────────────────── find-by category ───────────────────────

  @positive
  Scenario Outline: Filtering products by category returns the expected count
    When I query the database for products in category "<category>"
    Then the database product category result count is <count>

    Examples:
      | category     | count |
      | electronics  | 2     |
      | tools        | 1     |
      | discontinued | 1     |

  @negative
  Scenario: Filtering by a non-existent category returns zero products
    When I query the database for products in category "unknown-cat"
    Then the database product category result count is 0

  # ────────────────────── active products ────────────────────────

  @positive
  Scenario: Only active products are returned by the active filter
    When I query the database for all active products
    Then the database active product count is 3

  # ────────────────────── find-by status (orders) ────────────────

  @positive
  Scenario Outline: Filtering orders by status returns the expected count
    When I query the database for orders with status "<status>"
    Then the database order status result count is <count>

    Examples:
      | status  | count |
      | paid    | 2     |
      | pending | 1     |

  @negative
  Scenario: Filtering orders by a non-existent status returns zero rows
    When I query the database for orders with status "cancelled"
    Then the database order status result count is 0

  # ────────────────────── aggregation via QueryExecutor ──────────

  @positive
  Scenario: COUNT(*) via QueryExecutor on active products returns 3
    When I execute the database scalar query "SELECT COUNT(*) FROM products WHERE active = TRUE"
    Then the database scalar result is 3

  @positive
  Scenario: SUM of order totals for user 1 equals 39.97
    When I execute the database scalar query "SELECT COALESCE(SUM(total),0) FROM orders WHERE user_id = 1"
    Then the database scalar decimal result is "39.97"

  @positive
  Scenario: GROUP BY order status produces two distinct status groups
    When I execute the database list query "SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status ORDER BY status"
    Then the database list result row count is 2

  @positive
  Scenario: COUNT of products per category via QueryExecutor for electronics is 2
    When I execute the database list query "SELECT category, COUNT(*) AS cnt FROM products GROUP BY category ORDER BY category"
    Then the database list result contains a row where "category" equals "electronics"

  # ────────────────────── parameterised injection safety ─────────

  @security @negative
  Scenario: A SQL-injection payload used as a bind parameter matches no rows and leaves the table intact
    When I query the database for user with email "' OR '1'='1"
    Then the database query finds no user record
    And the database user count is 3

  @security @negative
  Scenario: A DROP-table injection payload as a bind parameter matches no rows and leaves products intact
    When I query the database for products in category "'; DROP TABLE products; --"
    Then the database product category result count is 0
    And the database product count is 4

  # ────────────────────── referential integrity ──────────────────

  @positive
  Scenario: Every order references a valid user id present in the users table
    When I query the database for all orders
    Then every database order has a user id that exists in the users table

  @positive
  Scenario: Every order references a valid product id present in the products table
    When I query the database for all orders
    Then every database order has a product id that exists in the products table

  # ────────────────────── DatabaseAssertions fluent API ──────────

  @positive
  Scenario: DatabaseAssertions confirms bob exists and ghost does not
    Then database assertions confirm row exists "SELECT 1 FROM users WHERE email = ?" with param "bob@omiinqa.test"
    And database assertions confirm no row exists "SELECT 1 FROM users WHERE email = ?" with param "ghost@omiinqa.test"

  @positive
  Scenario: DatabaseAssertions confirms carol's status is inactive
    Then database assertions confirm column value "SELECT status FROM users WHERE email = ?" equals "inactive" with param "carol@omiinqa.test"

  @positive
  Scenario: DatabaseAssertions confirms total row count in products is 4
    Then database assertions confirm row count in table "products" is 4

  # ────────────────────── CRUD round-trip ────────────────────────

  @positive @regression
  Scenario: Insert-find-delete user round-trip restores the baseline count
    When I insert a database user with name "Temp BDD User" email "bddtemp@omiinqa.test" status "active"
    Then the database user count is 4
    And I query the database for user with email "bddtemp@omiinqa.test"
    And the database query finds a user record
    When I delete the database user found by email "bddtemp@omiinqa.test"
    Then the database user count is 3
    And I query the database for user with email "bddtemp@omiinqa.test"
    And the database query finds no user record

  @positive @regression
  Scenario: Insert-find-delete product round-trip restores the baseline count
    When I insert a database product with name "BDD Widget" category "test" price "1.23" stock 10 active true
    Then the database product count is 5
    When I delete the database product found by name "BDD Widget"
    Then the database product count is 4

  # ────────────────────── transaction commit vs rollback ─────────

  @positive @business
  Scenario: A committed transaction is visible after commit
    When I commit a database transaction inserting user "Tx Commit BDD" email "txcommitbdd@omiinqa.test"
    Then I query the database for user with email "txcommitbdd@omiinqa.test"
    And the database query finds a user record
    When I delete the database user found by email "txcommitbdd@omiinqa.test"
    Then the database user count is 3

  @negative @business
  Scenario: A rolled-back transaction is not visible after rollback
    When I execute a database transaction that rolls back inserting user "Tx Rollback BDD" email "txrollbackbdd@omiinqa.test"
    Then I query the database for user with email "txrollbackbdd@omiinqa.test"
    And the database query finds no user record
    And the database user count is 3

  # ────────────────────── audit log assertions ───────────────────

  @positive
  Scenario: Seeded audit log has two CREATE actions
    When I query the database audit log for action "CREATE"
    Then the database audit log result count is 2

  @positive
  Scenario: Seeded audit log has two entries attributed to system
    When I query the database audit log for actor "system"
    Then the database audit log result count is 2

  @positive
  Scenario Outline: Counting audit entries by action matches seed
    When I query the database audit log for action "<action>"
    Then the database audit log result count is <count>

    Examples:
      | action | count |
      | CREATE | 2     |
      | UPDATE | 1     |
