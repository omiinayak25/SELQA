@domain @identity @registration
Feature: Registration
  As the identity platform
  I want to manage the full user sign-up lifecycle
  So that only users who accept terms and verify their email become active members

  Background:
    Given a clean registration service

  @smoke @positive
  Scenario: A user who accepts terms receives a verification token
    When I begin registration with username "alice" email "alice@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then no domain error is raised
    And the registration token is not null

  @positive
  Scenario: Verifying the token activates the account and marks email as verified
    When I begin registration with username "alice" email "alice@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    Then no domain error is raised
    And the registered account "alice@omiinqa.test" is email-verified

  @positive
  Scenario: Verified count increments after successful verification
    When I begin registration with username "bob" email "bob@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    Then the verified registration count is 1

  @business
  Scenario: After successful verification the pending count returns to zero
    When I begin registration with username "carol" email "carol@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then the pending registration count is 1
    When I verify the registration email with the issued token
    Then the pending registration count is 0

  @negative @business
  Scenario: Registration is rejected when terms are not accepted
    When I begin registration with username "dan" email "dan@omiinqa.test" password "Sup3rSecret" and terms accepted "false"
    Then a domain error "REG_TERMS_REQUIRED" is raised

  @negative
  Scenario: Duplicate email address is rejected
    When I begin registration with username "eve1" email "eve@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with the issued token
    When I begin registration with username "eve2" email "eve@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then a domain error "REG_DUP_EMAIL" is raised

  @negative
  Scenario: A duplicate email pending verification is also rejected
    When I begin registration with username "frank1" email "frank@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    When I begin registration with username "frank2" email "frank@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then a domain error "REG_DUP_EMAIL" is raised

  @negative
  Scenario: An invalid verification token is rejected
    When I begin registration with username "grace" email "grace@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I verify the registration email with token "VRFY-totally-wrong-token"
    Then a domain error "REG_TOKEN_INVALID" is raised

  @negative @business
  Scenario: An expired verification token is rejected
    When I begin registration with username "henry" email "henry@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by 6
    And I verify the registration email with the issued token
    Then a domain error "REG_TOKEN_EXPIRED" is raised

  @positive @boundary
  Scenario: A token verified exactly at the TTL boundary succeeds
    When I begin registration with username "iris" email "iris@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    And I advance the registration service tick by 5
    And I verify the registration email with the issued token
    Then no domain error is raised
    And the registered account "iris@omiinqa.test" is email-verified

  @negative
  Scenario: No verified account exists before email verification completes
    When I begin registration with username "jake" email "jake@omiinqa.test" password "Sup3rSecret" and terms accepted "true"
    Then no verified account exists for email "jake@omiinqa.test"

  @negative @validation
  Scenario Outline: Registration rejects invalid inputs with specific codes
    When I begin registration with username "<username>" email "<email>" password "<password>" and terms accepted "true"
    Then a domain error "<code>" is raised

    Examples:
      | username | email                | password    | code              |
      |          | valid@omiinqa.test   | Sup3rSecret | REG_BLANK         |
      | newuser  | not-an-email         | Sup3rSecret | REG_BAD_EMAIL     |
      | newuser  | valid@omiinqa.test   | weakpwd     | REG_WEAK_PASSWORD |
      | newuser  | valid@omiinqa.test   | alllower1   | REG_WEAK_PASSWORD |
      | newuser  | valid@omiinqa.test   | NOLOWER1    | REG_WEAK_PASSWORD |
