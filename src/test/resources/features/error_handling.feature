@ui @saucedemo @negative @error-handling
Feature: SauceDemo error handling and edge cases
  As a QA engineer
  I want to verify that error states are handled gracefully
  So that users see meaningful feedback for unexpected inputs

  @negative @regression
  Scenario: Attempting checkout with an empty cart still navigates to checkout
    Given the standard user is logged in and on the products page
    When the user opens the cart
    And the user attempts to start checkout from an empty cart
    Then the checkout step one page is displayed

  @negative @regression
  Scenario: Problem user login still loads the inventory page
    Given the user is on the SauceDemo login page
    When the user logs in as "problem_user" with password "secret_sauce"
    Then the products page is displayed

  @negative @regression
  Scenario Outline: Checkout form validation fires for missing fields
    Given the standard user has added "Sauce Labs Backpack" to the cart and reached checkout step one
    When the user submits checkout step one with first name "<first>" last name "<last>" zip "<zip>"
    Then the checkout step one error is displayed

    Examples:
      | first | last  | zip   |
      |       | Smith | 10001 |
      | Alice |       | 10001 |
      | Alice | Smith |       |

  @negative @regression
  Scenario: Whitespace-only first name triggers validation error
    Given the standard user has added "Sauce Labs Backpack" to the cart and reached checkout step one
    When the user submits checkout step one with first name "   " last name "Doe" zip "12345"
    Then the checkout step one error contains "First Name is required"

  @negative @regression
  Scenario: Very long zip code is accepted and user proceeds to overview
    Given the standard user has added "Sauce Labs Backpack" to the cart and reached checkout step one
    When the user submits checkout step one with first name "Long" last name "Zip" zip "123456789012345"
    Then the checkout overview page is displayed

  @negative @regression
  Scenario: Special characters in customer name fields are accepted
    Given the standard user has added "Sauce Labs Backpack" to the cart and reached checkout step one
    When the user submits checkout step one with first name "O'Brien" last name "Müller" zip "99999"
    Then the checkout overview page is displayed
