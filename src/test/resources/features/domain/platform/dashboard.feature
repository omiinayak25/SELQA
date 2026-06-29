@domain @platform @dashboard
Feature: Dashboard
  As the platform dashboard subsystem
  I want to maintain and query summary tiles (totalUsers, activeOrders, revenueToday, openTickets)
  So that business health metrics can be refreshed atomically, checked against thresholds,
  and reset — all with concrete value assertions and stable domain error codes

  Background:
    Given a clean dashboard service

  # ─── SMOKE / POSITIVE ────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: Refresh all tiles and getSummary returns the correct values
    When I refresh the dashboard with total users 1000 active orders 50 revenue today 9999.99 open tickets 12
    Then the operation succeeds
    And the dashboard total users is 1000
    And the dashboard active orders is 50
    And the dashboard revenue today is 9999.99
    And the dashboard open tickets is 12

  # ─── POSITIVE — individual setters ──────────────────────────────────────────

  @positive
  Scenario: Setting individual tiles reports the correct value for each
    When I set dashboard total users to 500
    Then the operation succeeds
    And the dashboard total users is 500
    When I set dashboard active orders to 30
    Then the operation succeeds
    And the dashboard active orders is 30
    When I set dashboard revenue today to 1234.56
    Then the operation succeeds
    And the dashboard revenue today is 1234.56
    When I set dashboard open tickets to 7
    Then the operation succeeds
    And the dashboard open tickets is 7

  @positive
  Scenario: getSummary after multiple individual sets has all correct values
    When I set dashboard total users to 200
    And I set dashboard active orders to 15
    And I set dashboard revenue today to 500.0
    And I set dashboard open tickets to 3
    Then the dashboard total users is 200
    And the dashboard active orders is 15
    And the dashboard revenue today is 500.0
    And the dashboard open tickets is 3

  # ─── POSITIVE — threshold ────────────────────────────────────────────────────

  @positive
  Scenario: Threshold is breached when tile value exceeds the threshold
    Given the dashboard has total users 100 active orders 0 revenue today 0.0 open tickets 0
    When I check dashboard threshold for tile "totalUsers" at 50.0
    Then the dashboard threshold is breached

  @positive
  Scenario: Threshold is not breached when tile value is below the threshold
    Given the dashboard has total users 10 active orders 0 revenue today 0.0 open tickets 0
    When I check dashboard threshold for tile "totalUsers" at 50.0
    Then the dashboard threshold is not breached

  # ─── BOUNDARY — reset ────────────────────────────────────────────────────────

  @boundary
  Scenario: Reset sets all tiles to zero
    Given the dashboard has total users 999 active orders 88 revenue today 7777.77 open tickets 55
    When I reset the dashboard
    Then the operation succeeds
    And the dashboard total users is 0
    And the dashboard active orders is 0
    And the dashboard revenue today is 0.0
    And the dashboard open tickets is 0

  @boundary
  Scenario: Threshold at exact value is not breached — strictly greater than
    Given the dashboard has total users 0 active orders 0 revenue today 100.0 open tickets 0
    When I check dashboard threshold for tile "revenueToday" at 100.0
    Then the dashboard threshold is not breached

  # ─── BUSINESS — atomic refresh ───────────────────────────────────────────────

  @business
  Scenario: Refresh atomically updates all four tiles in a single call
    Given the dashboard has total users 1 active orders 2 revenue today 3.0 open tickets 4
    When I refresh the dashboard with total users 100 active orders 200 revenue today 300.0 open tickets 400
    Then the dashboard total users is 100
    And the dashboard active orders is 200
    And the dashboard revenue today is 300.0
    And the dashboard open tickets is 400

  @business
  Scenario: getSummary after sequential individual sets reflects all current tile values
    When I set dashboard total users to 42
    And I set dashboard active orders to 8
    And I set dashboard revenue today to 999.0
    And I set dashboard open tickets to 1
    Then the dashboard total users is 42
    And the dashboard active orders is 8
    And the dashboard revenue today is 999.0
    And the dashboard open tickets is 1

  # ─── NEGATIVE — unknown tile ─────────────────────────────────────────────────

  @negative
  Scenario: Checking threshold for an unknown tile raises DASHBOARD_UNKNOWN_TILE
    Given the dashboard has total users 10 active orders 5 revenue today 50.0 open tickets 2
    When I check dashboard threshold for tile "unknownTile" at 5.0
    Then a domain error "DASHBOARD_UNKNOWN_TILE" is raised

  # ─── NEGATIVE — negative value outline ───────────────────────────────────────

  @negative @validation
  Scenario Outline: Setting a negative value for any tile raises DASHBOARD_INVALID_VALUE
    When I set dashboard <step> to <value>
    Then a domain error "DASHBOARD_INVALID_VALUE" is raised

    Examples:
      | step                   | value   |
      | total users            | -1      |
      | active orders          | -5      |
      | open tickets           | -100    |

  @negative @validation
  Scenario: Setting a negative revenue today raises DASHBOARD_INVALID_VALUE
    When I set dashboard revenue today to -0.01
    Then a domain error "DASHBOARD_INVALID_VALUE" is raised

  # ─── REGRESSION — refresh outline ────────────────────────────────────────────

  @regression @sanity
  Scenario Outline: Refresh outline — multiple tile combinations all produce correct summaries
    When I refresh the dashboard with total users <totalUsers> active orders <activeOrders> revenue today <revenueToday> open tickets <openTickets>
    Then the operation succeeds
    And the dashboard total users is <totalUsers>
    And the dashboard active orders is <activeOrders>
    And the dashboard revenue today is <revenueToday>
    And the dashboard open tickets is <openTickets>

    Examples:
      | totalUsers | activeOrders | revenueToday | openTickets |
      | 0          | 0            | 0.0          | 0           |
      | 1500       | 250          | 45000.00     | 33          |
      | 9999       | 1000         | 999999.99    | 500         |
      | 1          | 1            | 0.01         | 1           |

  # ─── VALIDATION — threshold outline ─────────────────────────────────────────

  @validation @regression
  Scenario Outline: Threshold outline — various tile and threshold combinations
    Given the dashboard has total users <totalUsers> active orders <activeOrders> revenue today <revenueToday> open tickets <openTickets>
    When I check dashboard threshold for tile "<tile>" at <threshold>
    Then the dashboard threshold <result>

    Examples:
      | totalUsers | activeOrders | revenueToday | openTickets | tile          | threshold | result          |
      | 200        | 0            | 0.0          | 0           | totalUsers    | 100.0     | is breached     |
      | 50         | 0            | 0.0          | 0           | totalUsers    | 100.0     | is not breached |
      | 0          | 75           | 0.0          | 0           | activeOrders  | 50.0      | is breached     |
      | 0          | 0            | 500.0        | 0           | revenueToday  | 500.0     | is not breached |
      | 0          | 0            | 0.0          | 10          | openTickets   | 9.0       | is breached     |
      | 0          | 0            | 0.0          | 5           | openTickets   | 10.0      | is not breached |
