@domain @identity @authentication
Feature: Authentication
  As the identity platform
  I want to authenticate users and protect accounts
  So that only valid credentials grant access and brute-force is contained

  Background:
    Given a clean authentication service

  @smoke @positive
  Scenario: A registered user authenticates with correct credentials
    Given a registered user with username "alice" email "alice@omiinqa.test" and password "Sup3rSecret"
    When I authenticate as "alice" with password "Sup3rSecret"
    Then the operation succeeds

  @positive
  Scenario: A user can authenticate by email address
    Given a registered user with username "bob" email "bob@omiinqa.test" and password "Sup3rSecret"
    When I authenticate as "bob@omiinqa.test" with password "Sup3rSecret"
    Then the operation succeeds

  @negative
  Scenario: Authentication fails with the wrong password
    Given a registered user with username "carol" email "carol@omiinqa.test" and password "Sup3rSecret"
    When I authenticate as "carol" with password "wrong"
    Then a domain error "AUTH_INVALID_CREDENTIALS" is raised

  @negative
  Scenario: Authentication fails for an unknown account
    When I authenticate as "ghost" with password "whatever"
    Then a domain error "AUTH_NOT_FOUND" is raised

  @security @boundary
  Scenario: An account locks after five consecutive failed attempts
    Given a registered user with username "dave" email "dave@omiinqa.test" and password "Sup3rSecret"
    When I authenticate 5 times as "dave" with wrong password "nope"
    Then the account "dave" has status "LOCKED"

  @negative @validation
  Scenario Outline: Registration rejects invalid input with a specific error code
    When I register a user with username "<username>" email "<email>" and password "<password>"
    Then a domain error "<code>" is raised

    Examples:
      | username | email               | password     | code              |
      |          | new@omiinqa.test    | Sup3rSecret  | AUTH_BLANK        |
      | newuser  | not-an-email        | Sup3rSecret  | AUTH_BAD_EMAIL    |
      | newuser  | new@omiinqa.test    | weak         | AUTH_WEAK_PASSWORD|
      | newuser  | new@omiinqa.test    | alllowercase | AUTH_WEAK_PASSWORD|
      | newuser  | new@omiinqa.test    | NOLOWER123   | AUTH_WEAK_PASSWORD|

  @negative
  Scenario: Duplicate usernames are rejected
    Given a registered user with username "erin" email "erin@omiinqa.test" and password "Sup3rSecret"
    When I register a user with username "erin" email "erin2@omiinqa.test" and password "Sup3rSecret"
    Then a domain error "AUTH_DUP_USERNAME" is raised

  @negative
  Scenario: Duplicate email addresses are rejected
    Given a registered user with username "frank" email "frank@omiinqa.test" and password "Sup3rSecret"
    When I register a user with username "frank2" email "frank@omiinqa.test" and password "Sup3rSecret"
    Then a domain error "AUTH_DUP_EMAIL" is raised

  @positive
  Scenario: Successful registration increases the account count
    Given a registered user with username "grace" email "grace@omiinqa.test" and password "Sup3rSecret"
    Then the registered account count is 1
