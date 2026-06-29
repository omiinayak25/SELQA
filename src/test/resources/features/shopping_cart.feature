@ui @saucedemo @cart
Feature: SauceDemo shopping cart management
  As a shopper
  I want to manage items in my shopping cart
  So that I can review and modify my selection before checkout

  Background:
    Given the standard user is logged in and on the products page

  @smoke @regression
  Scenario: Cart is empty when the user first logs in
    When the user opens the cart
    Then the cart contains 0 items

  @regression
  Scenario: One item added to cart appears in the cart page
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user opens the cart
    Then the cart contains 1 item
    And the cart contains the product "Sauce Labs Backpack"

  @regression
  Scenario: Multiple items added to cart all appear in the cart page
    When the user adds the following products to the cart:
      | Sauce Labs Backpack     |
      | Sauce Labs Bolt T-Shirt |
      | Sauce Labs Bike Light   |
    And the user opens the cart
    Then the cart contains 3 items
    And the cart contains the product "Sauce Labs Backpack"
    And the cart contains the product "Sauce Labs Bolt T-Shirt"
    And the cart contains the product "Sauce Labs Bike Light"

  @regression
  Scenario: Removing an item from the cart decrements item count
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user adds "Sauce Labs Bike Light" to the cart from the inventory
    And the user opens the cart
    And the user removes "Sauce Labs Backpack" from the cart
    Then the cart contains 1 item

  @regression
  Scenario: Removing all items leaves the cart empty
    When the user adds "Sauce Labs Onesie" to the cart from the inventory
    And the user opens the cart
    And the user removes "Sauce Labs Onesie" from the cart
    Then the cart contains 0 items

  @regression
  Scenario: Continue shopping from cart returns to inventory
    When the user adds "Sauce Labs Fleece Jacket" to the cart from the inventory
    And the user opens the cart
    And the user clicks continue shopping
    Then the inventory contains 6 products

  @regression
  Scenario: Cart persists items after continuing shopping and returning
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user opens the cart
    And the user clicks continue shopping
    And the user adds "Sauce Labs Onesie" to the cart from the inventory
    And the user opens the cart
    Then the cart contains 2 items

  @regression
  Scenario Outline: Adding each individual product results in a cart with 1 item
    When the user adds "<product>" to the cart from the inventory
    And the user opens the cart
    Then the cart contains 1 item
    And the cart contains the product "<product>"

    Examples:
      | product                           |
      | Sauce Labs Backpack               |
      | Sauce Labs Bike Light             |
      | Sauce Labs Bolt T-Shirt           |
      | Sauce Labs Fleece Jacket          |
      | Sauce Labs Onesie                 |
      | Test.allTheThings() T-Shirt (Red) |
