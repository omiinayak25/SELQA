@domain @identity @profile
Feature: Profile
  As the identity platform
  I want to let users manage their personal profile data
  So that display names, contact details and bios are always accurate and validated

  Background:
    Given a clean profile service
    And a profile account for user "alice" with email "alice@omiinqa.test" and password "Sup3rSecret"

  @smoke @positive
  Scenario: A user can update their first and last name
    When I update the profile name to first "Alice" last "Smith"
    Then no domain error is raised
    And the profile first name is "Alice"
    And the profile last name is "Smith"

  @positive
  Scenario: A user can update their display name within limits
    When I update the profile display name to "AliceInWonderland"
    Then no domain error is raised
    And the profile display name is "AliceInWonderland"

  @positive
  Scenario: A user can update their phone with a valid E.164-like number
    When I update the profile phone to "+14155551234"
    Then no domain error is raised
    And the profile phone is "+14155551234"

  @positive
  Scenario: A user can set a phone number without leading plus sign
    When I update the profile phone to "14155551234"
    Then no domain error is raised
    And the profile phone is "14155551234"

  @positive
  Scenario: A user can set a bio within the character limit
    When I update the profile bio to "I love testing software."
    Then no domain error is raised

  @positive @boundary
  Scenario: A bio of exactly 300 characters is accepted
    When I update the profile bio to a string of 300 characters
    Then no domain error is raised
    And the profile bio length is 300

  @negative @boundary
  Scenario: A bio of 301 characters is rejected
    When I update the profile bio to a string of 301 characters
    Then a domain error "PROFILE_BIO_TOO_LONG" is raised

  @negative @validation
  Scenario: An invalid phone number is rejected
    When I update the profile phone to "not-a-phone"
    Then a domain error "PROFILE_BAD_PHONE" is raised

  @negative @validation
  Scenario: A blank first name is rejected
    When I update the profile name to first "" last "Smith"
    Then a domain error "PROFILE_BLANK_NAME" is raised

  @negative @boundary
  Scenario: A display name exceeding 50 characters is rejected
    When I update the profile display name to "ThisDisplayNameIsWayTooLongAndExceedsTheFiftyCharacterLimit"
    Then a domain error "PROFILE_DISPLAY_NAME_TOO_LONG" is raised

  @positive @business
  Scenario: A user can request and confirm an email change
    When I request a profile email change to "alice.new@omiinqa.test"
    And I confirm the profile email change with the issued token
    Then no domain error is raised
    And the profile email is "alice.new@omiinqa.test"

  @negative @business
  Scenario: An email change with a wrong token is rejected
    When I request a profile email change to "alice.new@omiinqa.test"
    And I confirm the profile email change with token "WRONG-TOKEN"
    Then a domain error "PROFILE_BAD_EMAIL" is raised

  @negative @validation
  Scenario: Requesting a change to an invalid email address is rejected
    When I request a profile email change to "not-an-email"
    Then a domain error "PROFILE_BAD_EMAIL" is raised

  @negative @validation
  Scenario Outline: Phone number validation rejects non-E.164 formats
    When I update the profile phone to "<phone>"
    Then a domain error "PROFILE_BAD_PHONE" is raised

    Examples:
      | phone         |
      | abc           |
      | 123456        |
      | +             |
      | 123456789012345678 |
