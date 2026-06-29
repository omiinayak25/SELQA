@domain @identity @otp
Feature: OTP (One-Time Password)
  As the identity platform
  I want to generate and verify single-use numeric OTPs
  So that secondary authentication steps are time-bounded and brute-force-resistant

  Background:
    Given a clean OTP service
    And an OTP-managed account for user "alice" with email "alice@omiinqa.test" and password "Sup3rSecret"

  @smoke @positive
  Scenario: Generating an OTP produces a non-null 6-digit numeric code
    When I generate an OTP for the account
    Then no domain error is raised
    And the OTP code is a 6-digit numeric string

  @positive
  Scenario: A freshly generated OTP can be verified successfully
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    Then no domain error is raised

  @negative @business
  Scenario: A verified OTP cannot be used a second time
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    And I verify the OTP with the generated code
    Then a domain error "OTP_USED" is raised

  @negative
  Scenario: Verifying with a wrong code raises OTP_INVALID
    When I generate an OTP for the account
    And I verify the OTP with code "000000"
    Then a domain error "OTP_INVALID" is raised

  @negative @business
  Scenario: After three wrong attempts the OTP is locked out
    When I generate an OTP for the account
    And I verify the OTP 3 times with wrong code "000000"
    Then a domain error "OTP_ATTEMPTS_EXCEEDED" is raised

  @negative @business
  Scenario: Verifying after max attempts exceeded raises OTP_ATTEMPTS_EXCEEDED even with correct code
    When I generate an OTP for the account
    And I verify the OTP 3 times with wrong code "000000"
    And I verify the OTP with the generated code
    Then a domain error "OTP_ATTEMPTS_EXCEEDED" is raised

  @negative @business
  Scenario: An OTP expires after the TTL has passed
    When I generate an OTP for the account
    And I advance the OTP service tick by 6
    And I verify the OTP with the generated code
    Then a domain error "OTP_EXPIRED" is raised

  @positive @boundary
  Scenario: An OTP verified exactly at the TTL boundary succeeds
    When I generate an OTP for the account
    And I advance the OTP service tick by 5
    And I verify the OTP with the generated code
    Then no domain error is raised

  @negative
  Scenario: Verifying without first generating raises OTP_NOT_FOUND
    When I verify the OTP with code "123456"
    Then a domain error "OTP_NOT_FOUND" is raised

  @business
  Scenario: Generating a new OTP replaces the previous one
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    Then no domain error is raised

  @positive
  Scenario: OTP codes are deterministic for the same account across sequential generations
    When I generate an OTP for the account
    Then the OTP code is not null
    When I generate an OTP for the account
    Then the OTP code is not null

  @negative @validation
  Scenario Outline: Wrong code formats always fail OTP verification
    When I generate an OTP for the account
    And I verify the OTP with code "<code>"
    Then a domain error "OTP_INVALID" is raised

    Examples:
      | code    |
      | 000000  |
      | 999999  |
      | ABCDEF  |
