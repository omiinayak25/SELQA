@ui @saucedemo @inventory
Feature: SauceDemo inventory / products listing page
  As a signed-in shopper
  I want to see all products on the inventory page
  So that I can choose items to purchase

  Background:
    Given the standard user is logged in and on the products page

  @smoke @regression
  Scenario: Inventory page shows exactly six products
    Then the inventory contains 6 products

  @regression
  Scenario: All expected product names are present
    Then the following products are visible in the inventory:
      | Sauce Labs Backpack              |
      | Sauce Labs Bike Light            |
      | Sauce Labs Bolt T-Shirt          |
      | Sauce Labs Fleece Jacket         |
      | Sauce Labs Onesie                |
      | Test.allTheThings() T-Shirt (Red) |

  @regression
  Scenario: Cart badge is absent when no items have been added
    Then the cart badge is not visible

  @regression
  Scenario: Adding one item increments the cart badge to 1
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    Then the cart badge shows 1

  @regression
  Scenario: Adding two items increments the cart badge to 2
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user adds "Sauce Labs Bike Light" to the cart from the inventory
    Then the cart badge shows 2

  @regression
  Scenario: Removing an item from inventory decrements the cart badge
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user removes "Sauce Labs Backpack" from the inventory
    Then the cart badge is not visible

  @regression
  Scenario: Opening a product detail page shows the correct product name
    When the user opens the product detail for "Sauce Labs Fleece Jacket"
    Then the product detail page shows name "Sauce Labs Fleece Jacket"

  @regression
  Scenario: Product detail page shows a price
    When the user opens the product detail for "Sauce Labs Onesie"
    Then the product detail page shows a price

  @regression
  Scenario: Back button on detail page returns to inventory
    When the user opens the product detail for "Sauce Labs Bike Light"
    And the user navigates back to the inventory from the detail page
    Then the inventory contains 6 products

  @regression
  Scenario: Adding a product from detail page increments cart badge
    When the user opens the product detail for "Sauce Labs Bolt T-Shirt"
    And the user adds the product to cart from the detail page
    Then the cart badge shows 1
