@domain @identity @registration
Feature: Registration Boundary Matrix
  As the identity platform
  I want every combination of username/email/password boundary to produce an exact error code
  So that the RegistrationService enforces its contract precisely under all inputs

  Background:
    Given a clean registration service

  # ---------------------------------------------------------------------------
  # Block A — beginRegistration rejects invalid combinations with exact REG_ codes
  # Each row is a distinct equivalence class or boundary condition
  # ---------------------------------------------------------------------------

  @registration @negative @boundary @validation
  Scenario Outline: beginRegistration produces the correct error code for invalid inputs
    When I begin registration with username "<username>" email "<email>" password "<password>" and terms accepted "<terms>"
    Then a domain error "<code>" is raised

    Examples:
      | username  | email                  | password      | terms | code               | boundary-note                                   |
      | alice     | alice@omiinqa.test     | Secret1A      | false | REG_TERMS_REQUIRED | terms=false checked before all other validation  |
      |           | valid@omiinqa.test     | Sup3rSecret   | true  | REG_BLANK          | blank username — single-space not trimmed        |
      | user2     | valid2@omiinqa.test    | nouppercase1  | true  | REG_WEAK_PASSWORD  | no uppercase letter in password                 |
      | user1     | notanemail             | Sup3rSecret   | true  | REG_BAD_EMAIL      | invalid email — no @                            |
      | user1     | @nodomain.com          | Sup3rSecret   | true  | REG_BAD_EMAIL      | empty local part                                |
      | user1     | user@                  | Sup3rSecret   | true  | REG_BAD_EMAIL      | no domain after @                               |
      | user1     | user@.com              | Sup3rSecret   | true  | REG_BAD_EMAIL      | domain starts with dot                          |
      | user1     | user@com.              | Sup3rSecret   | true  | REG_BAD_EMAIL      | trailing dot after TLD                          |
      | user1     | user@example.c         | Sup3rSecret   | true  | REG_BAD_EMAIL      | TLD is 1 char                                   |
      | user1     | valid@omiinqa.test     | short1A       | true  | REG_WEAK_PASSWORD  | password too short (7 chars)                    |
      | user1     | valid@omiinqa.test     | alllowercase1 | true  | REG_WEAK_PASSWORD  | missing uppercase                               |
      | user1     | valid@omiinqa.test     | ALLUPPERCASE1 | true  | REG_WEAK_PASSWORD  | missing lowercase                               |
      | user1     | valid@omiinqa.test     | NoDigitHere   | true  | REG_WEAK_PASSWORD  | missing digit                                   |
      | user1     | valid@omiinqa.test     | 12345678      | true  | REG_WEAK_PASSWORD  | digits only                                     |
      | user1     | valid@omiinqa.test     | Abcdefg       | true  | REG_WEAK_PASSWORD  | 7 chars — below minimum                         |
      | user1     | valid@omiinqa.test     |               | true  | REG_WEAK_PASSWORD  | empty password                                  |

  # ---------------------------------------------------------------------------
  # Block B — terms=false is the FIRST check (before email/password validation)
  # Demonstrates that REG_TERMS_REQUIRED fires regardless of how bad other fields are
  # ---------------------------------------------------------------------------

  @registration @negative @business
  Scenario Outline: terms=false is rejected before any other field is validated
    When I begin registration with username "<username>" email "<email>" password "<password>" and terms accepted "false"
    Then a domain error "REG_TERMS_REQUIRED" is raised

    Examples:
      | username | email           | password   | note                                   |
      | user1    | invalid-email   | weakpwd    | terms checked before email+pwd         |
      |          | valid@test.io   | Sup3rSecret| terms checked before blank username    |
      | user1    | valid@test.io   | Sup3rSecret| valid other fields — only terms failed |

  # ---------------------------------------------------------------------------
  # Block C — duplicate detection for both email and username
  # Covers: pending + verified state duplication
  # ---------------------------------------------------------------------------

  @registration @negative @business
  Scenario: Duplicate email is rejected when first registration is still pending
    When I begin registration with username "pendingA" email "dup@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    When I begin registration with username "pendingB" email "dup@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then a domain error "REG_DUP_EMAIL" is raised

  @registration @negative @business
  Scenario: Duplicate email is rejected after first registration is fully verified
    When I begin registration with username "verifiedA" email "verified@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    When I begin registration with username "verifiedB" email "verified@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then a domain error "REG_DUP_EMAIL" is raised

  @registration @negative @business
  Scenario: Duplicate username is rejected after the first user is verified
    When I begin registration with username "shareduser" email "user1@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    When I begin registration with username "shareduser" email "user2@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then a domain error "REG_DUP_USERNAME" is raised

  # ---------------------------------------------------------------------------
  # Block D — token lifecycle boundary conditions
  # ---------------------------------------------------------------------------

  @registration @negative @boundary @business
  Scenario Outline: Token verification fails with REG_TOKEN_INVALID for unrecognised tokens
    When I begin registration with username "<username>" email "<email>" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by <ticks>
    And I verify the registration email with token "<token>"
    Then a domain error "REG_TOKEN_INVALID" is raised

    Examples:
      | username | email                | ticks | token                           | boundary-note                        |
      | tokA1    | tokA1@omiinqa.test   | 0     | VRFY-completely-wrong           | wrong token — not in store           |
      | tokA2    | tokA2@omiinqa.test   | 0     | VRFY-tampered-xxxxxxxxxxx       | tampered token                       |
      | tokA3    | tokA3@omiinqa.test   | 0     | invalid                         | short invalid token                  |
      | tokA4    | tokA4@omiinqa.test   | 2     | VRFY-wrong-token-here           | wrong token even with ticks advanced |

  @registration @negative @boundary
  Scenario: Token expired one tick past the TTL boundary raises REG_TOKEN_EXPIRED
    When I begin registration with username "expiredUser" email "expired@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by 6
    And I verify the registration email with the issued token
    Then a domain error "REG_TOKEN_EXPIRED" is raised

  @registration @positive @boundary
  Scenario: Token verified at exactly TTL boundary (tick=5) succeeds
    When I begin registration with username "boundaryUser" email "boundary@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by 5
    And I verify the registration email with the issued token
    Then no domain error is raised
    And the registered account "boundary@omiinqa.test" is email-verified

  @registration @positive @boundary
  Scenario: Token verified at tick=4 (one before boundary) succeeds
    When I begin registration with username "preBoundary" email "preboundary@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by 4
    And I verify the registration email with the issued token
    Then no domain error is raised
    And the registered account "preboundary@omiinqa.test" is email-verified

  @registration @negative @boundary
  Scenario: Unrecognised token raises REG_TOKEN_INVALID
    When I begin registration with username "badToken" email "badtoken@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with token "VRFY-invalid-token-xyz"
    Then a domain error "REG_TOKEN_INVALID" is raised

  # ---------------------------------------------------------------------------
  # Block E — Successful end-to-end flow produces correct state changes
  # ---------------------------------------------------------------------------

  @registration @positive @smoke
  Scenario Outline: Successful registrations each produce an email-verified account
    When I begin registration with username "<username>" email "<email>" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    Then no domain error is raised
    And the registered account "<email>" is email-verified

    Examples:
      | username | email                    |
      | alpha    | alpha@omiinqa.test       |
      | beta     | beta@omiinqa.test        |
      | gamma    | gamma@omiinqa.test       |

  @registration @positive @business
  Scenario: Multiple independent registrations accumulate in the verified count
    When I begin registration with username "reg1" email "reg1@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    When I begin registration with username "reg2" email "reg2@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    Then the verified registration count is 2
    And the pending registration count is 0
