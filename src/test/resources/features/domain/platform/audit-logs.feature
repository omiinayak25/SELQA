@domain @platform @audit
Feature: Audit Logs
  As the platform audit subsystem
  I want to record, query, and protect audit events in an append-only in-memory log
  So that every actor action is traceable, immutable, and queryable by multiple dimensions

  Background:
    Given a clean audit service

  # ─── POSITIVE ────────────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: A single audit event is recorded and the total count is 1
    When I audit record actor "alice" action "LOGIN" entity "Session" id "s-001"
    Then the operation succeeds
    And the audit total count is 1

  @positive
  Scenario: Total count increments with each recorded event
    When I audit record actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I audit record actor "alice" action "LOGOUT" entity "Session" id "s-001"
    And I audit record actor "bob" action "LOGIN" entity "Session" id "s-002"
    Then the audit total count is 3

  @positive
  Scenario: Query by actor returns only that actor's events
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I record an audit event actor "alice" action "VIEW" entity "Report" id "r-001"
    And I record an audit event actor "bob" action "LOGIN" entity "Session" id "s-002"
    When I audit query by actor "alice"
    Then the audit result count is 2

  @positive
  Scenario: Query by action returns only events with that action verb
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I record an audit event actor "bob" action "LOGIN" entity "Session" id "s-002"
    And I record an audit event actor "alice" action "LOGOUT" entity "Session" id "s-001"
    When I audit query by action "LOGIN"
    Then the audit result count is 2

  @positive
  Scenario: Query by entity returns only events for that entity type
    Given I record an audit event actor "alice" action "CREATE" entity "Order" id "o-001"
    And I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I record an audit event actor "bob" action "UPDATE" entity "Order" id "o-002"
    When I audit query by entity "Order"
    Then the audit result count is 2

  @positive
  Scenario: latestFor returns the most recently recorded entry for an entity
    Given I record an audit event actor "alice" action "CREATE" entity "Order" id "o-001"
    And I record an audit event actor "bob" action "UPDATE" entity "Order" id "o-001"
    Then the audit latest entity for "Order" has action "UPDATE"

  @positive
  Scenario: Time-range query returns events whose timestamps fall within the range
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I record an audit event actor "bob" action "VIEW" entity "Report" id "r-001"
    When I audit query by time range "2000-01-01T00:00:00Z" to "2999-12-31T23:59:59Z"
    Then the audit result count is 2

  # ─── NEGATIVE ────────────────────────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Blank parameters are rejected with AUDIT_BLANK
    When I audit record actor "<actor>" action "<action>" entity "<entity>" id "<entityId>"
    Then a domain error "AUDIT_BLANK" is raised

    Examples:
      | actor | action | entity  | entityId |
      |       | LOGIN  | Session | s-001    |
      | alice |        | Session | s-001    |
      | alice | LOGIN  |         | s-001    |
      | alice | LOGIN  | Session |          |

  @negative @business
  Scenario: Delete attempt is always rejected with AUDIT_IMMUTABLE
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    When I attempt to delete an audit record
    Then a domain error "AUDIT_IMMUTABLE" is raised
    And the domain error message contains "append-only"

  # ─── BOUNDARY ────────────────────────────────────────────────────────────────

  @boundary
  Scenario: An empty audit log has total count zero
    Then the audit total count is 0

  @boundary
  Scenario: Querying a non-existent actor returns an empty result
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    When I audit query by actor "nobody"
    Then the audit result is empty

  @boundary
  Scenario: latestFor on an entity with no records returns empty — asserted via query
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    When I audit query by entity "Order"
    Then the audit result is empty

  # ─── BUSINESS ────────────────────────────────────────────────────────────────

  @business
  Scenario: Multiple actors' events are isolated by actor query
    Given I record an audit event actor "alice" action "LOGIN" entity "Session" id "s-001"
    And I record an audit event actor "alice" action "VIEW" entity "Report" id "r-001"
    And I record an audit event actor "bob" action "LOGIN" entity "Session" id "s-002"
    And I record an audit event actor "carol" action "DELETE" entity "File" id "f-001"
    When I audit query by actor "bob"
    Then the audit result count is 1
    And the first audit result has actor "bob"
    When I audit query by actor "carol"
    Then the audit result count is 1
    And the first audit result has actor "carol"

  @business
  Scenario: Query results are ordered newest first — last recorded event appears first
    Given I record an audit event actor "alice" action "CREATE" entity "Order" id "o-001"
    And I record an audit event actor "alice" action "UPDATE" entity "Order" id "o-001"
    When I audit query by actor "alice"
    Then the audit result count is 2
    And the first audit result has action "UPDATE"

  # ─── VALIDATION OUTLINE ──────────────────────────────────────────────────────

  @positive @sanity
  Scenario Outline: Various valid audit events are all recorded successfully
    When I audit record actor "<actor>" action "<action>" entity "<entity>" id "<entityId>"
    Then the operation succeeds

    Examples:
      | actor     | action  | entity   | entityId  |
      | alice     | LOGIN   | Session  | s-001     |
      | bob       | LOGOUT  | Session  | s-002     |
      | system    | PURGE   | File     | f-999     |
      | carol     | EXPORT  | Report   | r-042     |
      | dave      | VIEW    | Order    | o-007     |

  @regression @sanity
  Scenario: Audit log is isolated per scenario — no data leaks from prior scenarios
    Then the audit total count is 0
