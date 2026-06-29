@ui @saucedemo @checkout @validation
Feature: SauceDemo checkout form validation
  As a shopper
  I want to see clear error messages when I submit incomplete checkout information
  So that I can correct my mistakes before placing an order

  Background:
    Given the standard user has added "Sauce Labs Backpack" to the cart and reached checkout step one

  @smoke @regression
  Scenario: All three fields missing shows first name required error
    When the user submits the checkout form without filling any field
    Then the checkout step one error contains "First Name is required"

  @negative @regression
  Scenario: Missing first name shows first name required error
    When the user submits checkout step one with first name "" last name "Doe" zip "12345"
    Then the checkout step one error contains "First Name is required"

  @negative @regression
  Scenario: Missing last name shows last name required error
    When the user submits checkout step one with first name "John" last name "" zip "12345"
    Then the checkout step one error contains "Last Name is required"

  @negative @regression
  Scenario: Missing zip code shows postal code required error
    When the user submits checkout step one with first name "John" last name "Doe" zip ""
    Then the checkout step one error contains "Postal Code is required"

  @negative @regression
  Scenario Outline: Each missing field triggers the corresponding error
    When the user submits checkout step one with first name "<first>" last name "<last>" zip "<zip>"
    Then the checkout step one error contains "<error>"

    Examples:
      | first | last | zip   | error                   |
      |       | Doe  | 10001 | First Name is required  |
      | Jane  |      | 10001 | Last Name is required   |
      | Jane  | Doe  |       | Postal Code is required |

  @regression
  Scenario: Cancelling checkout step one returns to cart
    When the user cancels checkout step one
    Then the cart page is displayed with the item "Sauce Labs Backpack"

  @regression
  Scenario: Valid customer info navigates to checkout overview
    When the user submits checkout step one with first name "Alice" last name "Smith" zip "94103"
    Then the checkout overview page is displayed

  @regression
  Scenario: Checkout overview shows the correct product
    When the user submits checkout step one with first name "Bob" last name "Jones" zip "90210"
    Then the checkout overview contains the item "Sauce Labs Backpack"

  @regression
  Scenario: Cancelling from checkout overview returns to inventory
    When the user submits checkout step one with first name "Carol" last name "White" zip "30301"
    And the user cancels from the checkout overview
    Then the inventory contains 6 products
