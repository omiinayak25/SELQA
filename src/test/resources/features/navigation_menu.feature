@ui @saucedemo @navigation
Feature: SauceDemo navigation and burger menu
  As a signed-in shopper
  I want to use the navigation menu
  So that I can move between pages efficiently

  Background:
    Given the standard user is logged in and on the products page

  @smoke @regression
  Scenario: Burger menu can be opened
    When the user opens the burger menu
    Then the burger menu is open

  @regression
  Scenario: Burger menu can be closed without selecting an option
    When the user opens the burger menu
    And the user closes the burger menu
    Then the inventory contains 6 products

  @regression
  Scenario: Logout via burger menu lands on the login page
    When the user logs out via the burger menu
    Then the login page is displayed again

  @regression
  Scenario: All Items menu item returns user to inventory from cart
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user opens the cart
    And the user navigates to all items via the burger menu
    Then the inventory contains 6 products

  @regression
  Scenario: Reset App State clears the cart badge
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the cart badge shows 1
    And the user resets the app state via the burger menu
    Then the cart badge is not visible

  @regression
  Scenario Outline: Navigation between inventory and cart is consistent
    When the user adds "<product>" to the cart from the inventory
    And the user opens the cart
    And the user clicks continue shopping
    Then the inventory contains 6 products

    Examples:
      | product                  |
      | Sauce Labs Backpack      |
      | Sauce Labs Bolt T-Shirt  |
      | Sauce Labs Fleece Jacket |

  @regression
  Scenario: Page title on the products page is "Products"
    Then the products page title is "Products"

  @regression
  Scenario: Page title on the cart page is "Your Cart"
    When the user opens the cart
    Then the cart page title is "Your Cart"
