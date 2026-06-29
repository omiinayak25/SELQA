@domain @platform @reports
Feature: Reports
  As the platform reporting subsystem
  I want to aggregate, filter, and analyse order data over an in-memory dataset
  So that business metrics such as revenue totals, averages, status breakdowns,
  and top-performer rankings can be asserted with real values and concrete error codes

  Background:
    Given a clean report service

  # ─── POSITIVE / SMOKE ────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: Total order count on a dataset of three orders
    Given the report dataset has 3 orders with revenue 100.0 each status "COMPLETED" on date "2024-03-15"
    When I compute the report count
    Then the report long result is 3

  @positive
  Scenario: Revenue sum is the product of count and per-order revenue
    Given the report dataset has 4 orders with revenue 25.0 each status "COMPLETED" on date "2024-03-15"
    When I compute the report revenue sum
    Then the report numeric result is 100.0

  @positive
  Scenario: Average order value is revenue sum divided by count
    Given the report dataset has 2 orders with revenue 50.0 each status "COMPLETED" on date "2024-03-15"
    And the report dataset has 2 orders with revenue 150.0 each status "PENDING" on date "2024-03-15"
    When I compute the report average order value
    Then the report numeric result is 100.0

  @positive
  Scenario: Group-by-status returns correct counts for each status bucket
    Given the report dataset has 3 orders with revenue 10.0 each status "COMPLETED" on date "2024-01-10"
    And the report dataset has 2 orders with revenue 10.0 each status "PENDING" on date "2024-01-10"
    And the report dataset has 1 orders with revenue 10.0 each status "CANCELLED" on date "2024-01-10"
    When I compute the report group by status
    Then the report group count for status "COMPLETED" is 3
    And the report group count for status "PENDING" is 2
    And the report group count for status "CANCELLED" is 1

  @positive
  Scenario: Top-2 by revenue returns the two highest-revenue orders
    Given I add a report order id "o-001" user "alice" status "COMPLETED" revenue 200.0 date "2024-02-01"
    And I add a report order id "o-002" user "alice" status "COMPLETED" revenue 500.0 date "2024-02-02"
    And I add a report order id "o-003" user "alice" status "COMPLETED" revenue 350.0 date "2024-02-03"
    When I compute the report top 2 by revenue
    Then the report result list size is 2
    And the report top result first has revenue 500.0

  @positive
  Scenario: Date-range filter returns only orders within the inclusive range
    Given I add a report order id "o-10" user "bob" status "COMPLETED" revenue 100.0 date "2024-01-01"
    And I add a report order id "o-11" user "bob" status "COMPLETED" revenue 100.0 date "2024-06-15"
    And I add a report order id "o-12" user "bob" status "COMPLETED" revenue 100.0 date "2024-12-31"
    When I compute the report filter by date range "2024-01-01" to "2024-06-30"
    Then the report date range result size is 2

  @positive
  Scenario: Count by user returns only orders belonging to that user
    Given I add a report order id "o-20" user "carol" status "COMPLETED" revenue 99.0 date "2024-05-01"
    And I add a report order id "o-21" user "carol" status "PENDING" revenue 49.0 date "2024-05-02"
    And I add a report order id "o-22" user "dave" status "COMPLETED" revenue 79.0 date "2024-05-03"
    When I compute the report count by user "carol"
    Then the report long result is 2

  @positive
  Scenario: Revenue sum by status aggregates only the matching status bucket
    Given I add a report order id "o-30" user "eve" status "COMPLETED" revenue 120.0 date "2024-04-01"
    And I add a report order id "o-31" user "eve" status "COMPLETED" revenue 80.0 date "2024-04-02"
    And I add a report order id "o-32" user "eve" status "REFUNDED" revenue 50.0 date "2024-04-03"
    When I compute the report revenue sum by status "COMPLETED"
    Then the report numeric result is 200.0

  # ─── NEGATIVE ────────────────────────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Bad date range raises REPORT_BAD_RANGE
    Given the report dataset has 1 orders with revenue 10.0 each status "COMPLETED" on date "2024-06-01"
    When I compute the report filter by date range "<from>" to "<to>"
    Then a domain error "REPORT_BAD_RANGE" is raised

    Examples:
      | from       | to         |
      | 2024-06-30 | 2024-06-01 |
      | 2025-01-01 | 2024-12-31 |
      | 2024-03-15 | 2024-03-14 |

  # ─── BOUNDARY ────────────────────────────────────────────────────────────────

  @boundary
  Scenario: Empty dataset — count is zero
    Given the report dataset is empty
    When I compute the report count
    Then the report long result is 0

  @boundary
  Scenario: Empty dataset — revenue sum is zero
    Given the report dataset is empty
    When I compute the report revenue sum
    Then the report result is zero

  @boundary
  Scenario: Empty dataset — average order value is zero not an error
    Given the report dataset is empty
    When I compute the report average order value
    Then the operation succeeds
    And the report result is zero

  @boundary
  Scenario: Empty dataset — top-N returns empty list
    Given the report dataset is empty
    When I compute the report top 5 by revenue
    Then the report result list is empty

  @boundary
  Scenario: Date range where from equals to is valid — single-day range
    Given I add a report order id "o-40" user "frank" status "COMPLETED" revenue 55.0 date "2024-07-04"
    And I add a report order id "o-41" user "frank" status "COMPLETED" revenue 55.0 date "2024-07-05"
    When I compute the report filter by date range "2024-07-04" to "2024-07-04"
    Then no domain error is raised
    And the report date range result size is 1

  # ─── BUSINESS ────────────────────────────────────────────────────────────────

  @business
  Scenario Outline: Top-1 by revenue always returns the single highest-revenue order
    Given I add a report order id "o-high" user "gina" status "COMPLETED" revenue <high> date "2024-08-01"
    And I add a report order id "o-low" user "gina" status "COMPLETED" revenue <low> date "2024-08-02"
    And I add a report order id "o-mid" user "gina" status "COMPLETED" revenue <mid> date "2024-08-03"
    When I compute the report top 1 by revenue
    Then the report result list size is 1
    And the report top result first has revenue <high>

    Examples:
      | high  | low  | mid  |
      | 999.0 | 1.0  | 50.0 |
      | 500.0 | 10.0 | 250.0 |

  @business @regression
  Scenario Outline: Group-by-status aggregation with mixed status inputs
    Given the report dataset has <completed> orders with revenue 10.0 each status "COMPLETED" on date "2024-09-01"
    And the report dataset has <pending> orders with revenue 10.0 each status "PENDING" on date "2024-09-01"
    When I compute the report group by status
    Then the report group count for status "COMPLETED" is <completed>
    And the report group count for status "PENDING" is <pending>

    Examples:
      | completed | pending |
      | 4         | 2       |
      | 1         | 5       |
      | 3         | 3       |

  # ─── REGRESSION ──────────────────────────────────────────────────────────────

  @regression
  Scenario Outline: Date-range filter with multiple date configurations
    Given I add a report order id "r-001" user "hank" status "COMPLETED" revenue 10.0 date "<date1>"
    And I add a report order id "r-002" user "hank" status "COMPLETED" revenue 10.0 date "<date2>"
    And I add a report order id "r-003" user "hank" status "COMPLETED" revenue 10.0 date "<date3>"
    When I compute the report filter by date range "<from>" to "<to>"
    Then the report date range result size is <expected>

    Examples:
      | date1      | date2      | date3      | from       | to         | expected |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-01-01 | 2024-12-31 | 3        |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-01-01 | 2024-06-14 | 1        |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-06-16 | 2024-12-31 | 1        |
      | 2024-03-01 | 2024-03-15 | 2024-03-30 | 2024-03-10 | 2024-03-20 | 1        |
