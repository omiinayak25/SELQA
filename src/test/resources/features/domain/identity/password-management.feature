@domain @identity @password-management
Feature: Password Management
  As the identity platform
  I want to allow secure password changes and resets
  So that users can update credentials safely without reusing recent passwords

  Background:
    Given a clean password service
    And a password-managed account for user "alice" with email "alice@omiinqa.test" and password "Sup3rSecret"

  @smoke @positive
  Scenario: A user can change their password by providing the correct current password
    When I change the account password from "Sup3rSecret" to "NewP@ss1word"
    Then no domain error is raised
    And the stored account password is "NewP@ss1word"

  @negative
  Scenario: A password change fails when the current password is wrong
    When I change the account password from "WrongPassword" to "NewP@ss1word"
    Then a domain error "PWD_WRONG_CURRENT" is raised

  @negative @validation
  Scenario: A password change fails when the new password is too weak
    When I change the account password from "Sup3rSecret" to "weakpassword"
    Then a domain error "PWD_WEAK" is raised

  @negative @business
  Scenario: A password cannot be reused immediately
    When I change the account password from "Sup3rSecret" to "NewP@ss1word"
    And I change the account password from "NewP@ss1word" to "Sup3rSecret"
    Then a domain error "PWD_REUSED" is raised

  @negative @business
  Scenario: A password cannot be reused within the history window
    When I change the account password from "Sup3rSecret" to "Second1Pass"
    And I change the account password from "Second1Pass" to "Third1Pass!"
    And I change the account password from "Third1Pass!" to "Sup3rSecret"
    Then a domain error "PWD_REUSED" is raised

  @positive @business
  Scenario: A password is allowed once it falls outside the history window
    # History keeps the last 3 passwords; Sup3rSecret is evicted only after 4
    # distinct changes, after which it may be reused.
    When I change the account password from "Sup3rSecret" to "Second1Pass"
    And I change the account password from "Second1Pass" to "Third1Pass!"
    And I change the account password from "Third1Pass!" to "Fourth1Pass"
    And I change the account password from "Fourth1Pass" to "Fifth1Pass!"
    And I change the account password from "Fifth1Pass!" to "Sup3rSecret"
    Then no domain error is raised
    And the stored account password is "Sup3rSecret"

  @positive
  Scenario: A reset token is issued and the password can be reset with it
    When I issue a password reset token for the account
    Then no domain error is raised
    And the password reset token is not null
    When I reset the account password to "NewP@ss1word" using the issued reset token
    Then no domain error is raised
    And the stored account password is "NewP@ss1word"

  @negative @business
  Scenario: A reset token cannot be used twice
    When I issue a password reset token for the account
    And I reset the account password to "NewP@ss1word" using the issued reset token
    And I reset the account password to "Another1Pass" using the issued reset token
    Then a domain error "PWD_RESET_TOKEN_USED" is raised

  @negative
  Scenario: An invalid reset token is rejected
    When I reset the account password to "NewP@ss1word" using reset token "FAKE-TOKEN"
    Then a domain error "PWD_RESET_TOKEN_INVALID" is raised

  @negative @business
  Scenario: An expired reset token is rejected
    When I issue a password reset token for the account
    And I advance the password service tick by 6
    And I reset the account password to "NewP@ss1word" using the issued reset token
    Then a domain error "PWD_RESET_TOKEN_EXPIRED" is raised

  @negative @validation
  Scenario: Reset with a weak password is rejected even with a valid token
    When I issue a password reset token for the account
    And I reset the account password to "weakpass" using the issued reset token
    Then a domain error "PWD_WEAK" is raised

  @negative @validation
  Scenario Outline: Password strength rules are enforced on change
    When I change the account password from "Sup3rSecret" to "<newPassword>"
    Then a domain error "PWD_WEAK" is raised

    Examples:
      | newPassword  |
      | short1A      |
      | alllowercase1 |
      | ALLUPPERCASE1 |
      | NoDigitHere  |
