@domain @platform @reports
Feature: Report Aggregation Matrix
  As the platform reporting subsystem
  I want many distinct datasets to produce verified COUNT, SUM, AVG, GROUP-BY, TOP-N and date-range results
  So that every aggregation path is proven with real numeric assertions

  Background:
    Given a clean report service

  # ─── COUNT MATRIX — various dataset sizes and compositions ───────────────────────

  @positive @sanity
  Scenario Outline: Dataset of <total> orders yields count of <total>
    Given the report dataset has <total> orders with revenue 1.0 each status "COMPLETED" on date "2024-01-01"
    When I compute the report count
    Then the report long result is <total>

    Examples:
      | total |
      | 1     |
      | 2     |
      | 5     |
      | 10    |
      | 20    |
      | 50    |
      | 100   |

  # ─── SUM REVENUE MATRIX — variety of per-order revenue × count combinations ─────

  @positive @business
  Scenario Outline: Dataset of <count> orders each with revenue <rev> yields sum <sum>
    Given the report dataset has <count> orders with revenue <rev> each status "COMPLETED" on date "2024-02-01"
    When I compute the report revenue sum
    Then the report numeric result is <sum>

    Examples:
      | count | rev    | sum     |
      | 1     | 10.0   | 10.0    |
      | 2     | 10.0   | 20.0    |
      | 5     | 20.0   | 100.0   |
      | 4     | 25.0   | 100.0   |
      | 3     | 33.33  | 99.99   |
      | 10    | 5.5    | 55.0    |
      | 7     | 0.0    | 0.0     |
      | 1     | 999.99 | 999.99  |
      | 2     | 500.0  | 1000.0  |
      | 6     | 100.0  | 600.0   |

  # ─── AVG ORDER VALUE MATRIX — mixed-status datasets with known average ──────────

  @positive @business
  Scenario Outline: Mixed dataset produces expected average order value
    Given the report dataset has <c1> orders with revenue <r1> each status "COMPLETED" on date "2024-03-01"
    And the report dataset has <c2> orders with revenue <r2> each status "PENDING" on date "2024-03-02"
    When I compute the report average order value
    Then the report numeric result is <avg>

    Examples:
      | c1 | r1    | c2 | r2    | avg    |
      | 1  | 100.0 | 1  | 0.0   | 50.0   |
      | 2  | 50.0  | 2  | 150.0 | 100.0  |
      | 3  | 30.0  | 3  | 60.0  | 45.0   |
      | 4  | 25.0  | 0  | 0.0   | 25.0   |
      | 1  | 200.0 | 4  | 50.0  | 80.0   |
      | 5  | 10.0  | 5  | 20.0  | 15.0   |

  # ─── GROUP-BY STATUS MATRIX — diverse status compositions ────────────────────────

  @positive @business
  Scenario Outline: Group-by-status counts match the injected status distribution
    Given the report dataset has <comp> orders with revenue 1.0 each status "COMPLETED" on date "2024-04-01"
    And the report dataset has <pend> orders with revenue 1.0 each status "PENDING" on date "2024-04-01"
    And the report dataset has <canc> orders with revenue 1.0 each status "CANCELLED" on date "2024-04-01"
    When I compute the report group by status
    Then the report group count for status "COMPLETED" is <comp>
    And the report group count for status "PENDING" is <pend>
    And the report group count for status "CANCELLED" is <canc>

    Examples:
      | comp | pend | canc |
      | 5    | 3    | 2    |
      | 1    | 1    | 1    |
      | 10   | 0    | 0    |
      | 0    | 5    | 0    |
      | 0    | 0    | 7    |
      | 3    | 3    | 3    |
      | 8    | 2    | 1    |
      | 1    | 9    | 0    |

  # ─── GROUP-BY STATUS — REFUNDED bucket ──────────────────────────────────────────

  @positive @business
  Scenario Outline: REFUNDED status group is correctly counted
    Given the report dataset has <comp> orders with revenue 50.0 each status "COMPLETED" on date "2024-05-01"
    And the report dataset has <ref> orders with revenue 50.0 each status "REFUNDED" on date "2024-05-01"
    When I compute the report group by status
    Then the report group count for status "COMPLETED" is <comp>
    And the report group count for status "REFUNDED" is <ref>

    Examples:
      | comp | ref |
      | 3    | 2   |
      | 5    | 1   |
      | 0    | 4   |
      | 6    | 3   |

  # ─── TOP-N BY REVENUE MATRIX — varying N and dataset sizes ──────────────────────

  @positive @business
  Scenario Outline: Top-<n> returns <n> items with the highest revenue first
    Given I add a report order id "tn-001" user "topuser" status "COMPLETED" revenue <r1> date "2024-06-01"
    And I add a report order id "tn-002" user "topuser" status "COMPLETED" revenue <r2> date "2024-06-02"
    And I add a report order id "tn-003" user "topuser" status "COMPLETED" revenue <r3> date "2024-06-03"
    And I add a report order id "tn-004" user "topuser" status "COMPLETED" revenue <r4> date "2024-06-04"
    And I add a report order id "tn-005" user "topuser" status "COMPLETED" revenue <r5> date "2024-06-05"
    When I compute the report top <n> by revenue
    Then the report result list size is <n>
    And the report top result first has revenue <top>

    Examples:
      | n | r1    | r2    | r3    | r4    | r5    | top   |
      | 1 | 100.0 | 200.0 | 50.0  | 300.0 | 75.0  | 300.0 |
      | 2 | 10.0  | 90.0  | 40.0  | 60.0  | 80.0  | 90.0  |
      | 3 | 15.0  | 5.0   | 25.0  | 35.0  | 20.0  | 35.0  |
      | 5 | 1.0   | 2.0   | 3.0   | 4.0   | 5.0   | 5.0   |

  # ─── COUNT BY USER MATRIX — multi-user datasets ──────────────────────────────────

  @positive @business
  Scenario Outline: countByUser returns only orders for the target user
    Given I add a report order id "cu-001" user "<u1>" status "COMPLETED" revenue 10.0 date "2024-07-01"
    And I add a report order id "cu-002" user "<u1>" status "PENDING" revenue 20.0 date "2024-07-02"
    And I add a report order id "cu-003" user "<u2>" status "COMPLETED" revenue 30.0 date "2024-07-03"
    And I add a report order id "cu-004" user "<u2>" status "CANCELLED" revenue 5.0 date "2024-07-04"
    And I add a report order id "cu-005" user "<u2>" status "COMPLETED" revenue 15.0 date "2024-07-05"
    When I compute the report count by user "<target>"
    Then the report long result is <expected>

    Examples:
      | u1    | u2    | target | expected |
      | alice | bob   | alice  | 2        |
      | alice | bob   | bob    | 3        |
      | carol | dave  | carol  | 2        |
      | carol | dave  | dave   | 3        |
      | eve   | frank | eve    | 2        |

  # ─── REVENUE SUM BY STATUS MATRIX — status-scoped sums ──────────────────────────

  @positive @business
  Scenario Outline: sumRevenueByStatus returns the correct total for the target status
    Given the report dataset has <comp> orders with revenue <compRev> each status "COMPLETED" on date "2024-08-01"
    And the report dataset has <pend> orders with revenue <pendRev> each status "PENDING" on date "2024-08-01"
    When I compute the report revenue sum by status "COMPLETED"
    Then the report numeric result is <compSum>

    Examples:
      | comp | compRev | pend | pendRev | compSum |
      | 2    | 100.0   | 3    | 50.0    | 200.0   |
      | 5    | 20.0    | 2    | 30.0    | 100.0   |
      | 1    | 500.0   | 4    | 25.0    | 500.0   |
      | 3    | 33.33   | 3    | 66.67   | 99.99   |
      | 4    | 0.0     | 1    | 10.0    | 0.0     |

  # ─── DATE RANGE FILTER MATRIX — inclusive range membership ──────────────────────

  @positive @regression
  Scenario Outline: Date-range filter returns exactly <expected> orders between <from> and <to>
    Given I add a report order id "dr-001" user "rangeuser" status "COMPLETED" revenue 10.0 date "<d1>"
    And I add a report order id "dr-002" user "rangeuser" status "COMPLETED" revenue 10.0 date "<d2>"
    And I add a report order id "dr-003" user "rangeuser" status "COMPLETED" revenue 10.0 date "<d3>"
    When I compute the report filter by date range "<from>" to "<to>"
    Then the report date range result size is <expected>

    Examples:
      | d1         | d2         | d3         | from       | to         | expected |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-01-01 | 2024-12-31 | 3        |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-01-01 | 2024-06-15 | 2        |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-06-15 | 2024-12-31 | 2        |
      | 2024-01-01 | 2024-06-15 | 2024-12-31 | 2024-06-16 | 2024-12-30 | 0        |
      | 2024-03-01 | 2024-03-15 | 2024-03-30 | 2024-03-01 | 2024-03-01 | 1        |
      | 2024-03-01 | 2024-03-15 | 2024-03-30 | 2024-03-30 | 2024-03-30 | 1        |
      | 2024-03-01 | 2024-03-15 | 2024-03-30 | 2024-03-15 | 2024-03-15 | 1        |
      | 2024-03-01 | 2024-03-15 | 2024-03-30 | 2024-04-01 | 2024-04-30 | 0        |

  # ─── BAD DATE RANGE MATRIX — extended set of invalid ranges ─────────────────────

  @negative @boundary
  Scenario Outline: Inverted date range raises REPORT_BAD_RANGE
    Given the report dataset has 1 orders with revenue 10.0 each status "COMPLETED" on date "2024-01-15"
    When I compute the report filter by date range "<from>" to "<to>"
    Then a domain error "REPORT_BAD_RANGE" is raised

    Examples:
      | from       | to         |
      | 2024-12-31 | 2024-01-01 |
      | 2025-01-01 | 2024-12-31 |
      | 2024-06-02 | 2024-06-01 |
      | 2024-07-15 | 2024-07-14 |
      | 2025-12-31 | 2024-12-31 |

  # ─── EMPTY DATASET SAFETY — all aggregations safe on zero rows ───────────────────

  @boundary
  Scenario Outline: <aggregation> on empty dataset returns safe zero value
    Given the report dataset is empty
    When I compute the report <action>
    Then the report result is zero

    Examples:
      | aggregation            | action                             |
      | count                  | count                              |
      | sumRevenue             | revenue sum                        |
      | avgOrderValue          | average order value                |

  @boundary
  Scenario: Top-N on empty dataset returns empty list not an error
    Given the report dataset is empty
    When I compute the report top 3 by revenue
    Then the report result list is empty

  @boundary
  Scenario: countByUser for unknown user on non-empty dataset returns zero
    Given the report dataset has 5 orders with revenue 10.0 each status "COMPLETED" on date "2024-09-01"
    When I compute the report count by user "nobody"
    Then the report long result is 0

  @boundary
  Scenario: sumRevenueByStatus for missing status bucket returns zero
    Given the report dataset has 3 orders with revenue 50.0 each status "COMPLETED" on date "2024-10-01"
    When I compute the report revenue sum by status "REFUNDED"
    Then the report result is zero
