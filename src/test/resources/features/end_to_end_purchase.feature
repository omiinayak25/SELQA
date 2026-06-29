@ui @saucedemo @e2e @regression
Feature: SauceDemo end-to-end purchase journeys
  As a shopper
  I want to complete full purchase journeys
  So that the entire checkout flow works from login to confirmation

  @smoke
  Scenario: E2E — single item purchase shows order confirmation
    Given the user is on the SauceDemo login page
    When the customer purchases the following items:
      | Sauce Labs Fleece Jacket |
    Then the order confirmation is shown

  @smoke
  Scenario: E2E — three item purchase completes successfully
    Given the user is on the SauceDemo login page
    When the customer purchases the following items:
      | Sauce Labs Backpack     |
      | Sauce Labs Bike Light   |
      | Sauce Labs Onesie       |
    Then the order confirmation is shown

  @regression
  Scenario: E2E — confirmation page shows dispatch text
    Given the user is on the SauceDemo login page
    When the customer purchases the following items:
      | Sauce Labs Bolt T-Shirt |
    Then the order confirmation body text contains "dispatched"

  @regression
  Scenario: E2E — Back Home button on confirmation returns to inventory
    Given the user is on the SauceDemo login page
    When the customer purchases the following items:
      | Sauce Labs Bike Light |
    Then the order confirmation is shown
    And the user clicks Back Home and lands on the products page

  @regression
  Scenario Outline: E2E — each individual product can be purchased end-to-end
    Given the user is on the SauceDemo login page
    When the customer purchases the following items:
      | <product> |
    Then the order confirmation is shown

    Examples:
      | product                           |
      | Sauce Labs Backpack               |
      | Sauce Labs Bolt T-Shirt           |
      | Sauce Labs Fleece Jacket          |
      | Sauce Labs Onesie                 |
      | Test.allTheThings() T-Shirt (Red) |

  @regression
  Scenario: E2E — sorting then purchasing top item works
    Given the standard user is logged in and on the products page
    When the user sorts products by "Price (low to high)"
    And the user adds "Sauce Labs Onesie" to the cart from the inventory
    And the user opens the cart
    And the user checks out with customer info "Tara" "Quinn" "50001"
    Then the order confirmation is shown
