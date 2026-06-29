@domain @identity @mfa
Feature: Multi-Factor Authentication (MFA)
  As the identity platform
  I want to enrol users in TOTP-based MFA with one-time backup codes
  So that accounts are protected by a second factor that can be recovered securely

  Background:
    Given a clean MFA service
    And an MFA-managed account for user "alice" with email "alice@omiinqa.test" and password "Sup3rSecret"

  @smoke @positive
  Scenario: Enrolling an account in MFA returns a secret and backup codes
    When I enrol the account in MFA
    Then no domain error is raised
    And the account is enrolled in MFA
    And the MFA enrolment secret is not null
    And the MFA enrolment result contains 8 backup codes

  @positive
  Scenario: Enrolled backup codes are all distinct
    When I enrol the account in MFA
    Then the MFA backup codes are all distinct

  @positive
  Scenario: A valid TOTP code is accepted after enrolment
    When I enrol the account in MFA
    And I verify the MFA code for the account
    Then no domain error is raised

  @negative @business
  Scenario: An incorrect TOTP code is rejected
    When I enrol the account in MFA
    And I verify the MFA code "000000" for the account
    Then a domain error "MFA_BAD_CODE" is raised

  @negative @business
  Scenario: Enrolling an already-enrolled account is rejected
    When I enrol the account in MFA
    And I enrol the account in MFA
    Then a domain error "MFA_ALREADY_ENROLLED" is raised

  @negative
  Scenario: Verifying a code without enrolment raises MFA_NOT_ENROLLED
    When I verify the MFA code "123456" for the account
    Then a domain error "MFA_NOT_ENROLLED" is raised

  @positive @business
  Scenario: A valid backup code can be used for verification
    When I enrol the account in MFA
    And I use the first MFA backup code for the account
    Then no domain error is raised

  @negative @business
  Scenario: The same backup code cannot be used twice
    When I enrol the account in MFA
    And I use the first MFA backup code for the account
    And I use the first MFA backup code for the account
    Then a domain error "MFA_BACKUP_USED" is raised

  @negative
  Scenario: An unrecognised backup code raises MFA_BACKUP_INVALID
    When I enrol the account in MFA
    And I use MFA backup code "BKP-FAKE00" for the account
    Then a domain error "MFA_BACKUP_INVALID" is raised

  @positive @business
  Scenario: MFA can be disabled with the correct TOTP code
    When I enrol the account in MFA
    And I disable MFA for the account using the correct code
    Then no domain error is raised
    And the account is not enrolled in MFA

  @negative @business
  Scenario: Disabling MFA with an incorrect code is rejected
    When I enrol the account in MFA
    And I disable MFA for the account using code "000000"
    Then a domain error "MFA_BAD_CODE" is raised
    And the account is enrolled in MFA

  @positive @boundary
  Scenario: The expected code at the current tick is accepted even after the tick advances by one
    When I enrol the account in MFA
    And I verify the MFA code for the account
    And I advance the MFA service tick by 1
    And I verify the MFA code for the account
    Then no domain error is raised

  @negative @validation
  Scenario Outline: Various invalid TOTP codes are rejected
    When I enrol the account in MFA
    And I verify the MFA code "<code>" for the account
    Then a domain error "MFA_BAD_CODE" is raised

    Examples:
      | code   |
      | 000000 |
      | 999999 |
      | abc123 |
      | 1234   |
