@domain @identity @session-management
Feature: Session Management
  As the identity platform
  I want to create, validate, expire and revoke user sessions
  So that access is controlled precisely and concurrent-session limits are enforced

  Background:
    Given a clean session service
    And a session-managed account for user "alice" with email "alice@omiinqa.test" and password "Sup3rSecret"

  @smoke @positive
  Scenario: Creating a session returns a non-null token
    When I create a session for the account
    Then no domain error is raised
    And the session token is not null

  @positive
  Scenario: A newly created session can be validated and belongs to the account
    When I create a session for the account
    Then the validated session belongs to the account

  @positive
  Scenario: The live session count reflects created sessions
    When I create 2 sessions for the account
    Then no domain error is raised
    And the live session count for the account is 2

  @negative @business
  Scenario: Session creation fails when the per-user limit is reached
    When I create 3 sessions for the account
    And I create a session for the account
    Then a domain error "SESSION_LIMIT" is raised

  @positive @boundary
  Scenario: Exactly the maximum number of sessions can be created
    When I create 3 sessions for the account
    Then no domain error is raised
    And the live session count for the account is 3

  @negative
  Scenario: Validating an unknown token raises SESSION_INVALID
    When I validate session token "FAKE-SESSION-TOKEN"
    Then a domain error "SESSION_INVALID" is raised

  @negative @business
  Scenario: A revoked session token is invalid on subsequent validation
    When I create a session for the account
    And I revoke the last session token
    And I validate the last session token
    Then a domain error "SESSION_INVALID" is raised

  @positive @business
  Scenario: Revoking all sessions brings the live count to zero
    When I create 2 sessions for the account
    And I revoke all sessions for the account
    Then the live session count for the account is 0

  @negative @business
  Scenario: An expired session token raises SESSION_EXPIRED
    When I create a session for the account
    And I advance the session service tick by 11
    And I validate the last session token
    Then a domain error "SESSION_EXPIRED" is raised

  @positive @boundary
  Scenario: A session validated exactly at the TTL boundary succeeds
    When I create a session for the account
    And I advance the session service tick by 10
    And I validate the last session token
    Then no domain error is raised

  @positive @business
  Scenario: After a session expires it does not count against the concurrent limit
    When I create 3 sessions for the account
    And I advance the session service tick by 11
    And I create a session for the account
    Then no domain error is raised
    And the live session count for the account is 1

  @negative @validation
  Scenario Outline: Various invalid tokens raise SESSION_INVALID
    When I validate session token "<token>"
    Then a domain error "SESSION_INVALID" is raised

    Examples:
      | token          |
      | FAKE-TOKEN-1   |
      | null-token     |
      | SES-0-000000   |
