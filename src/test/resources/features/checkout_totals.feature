@ui @saucedemo @checkout @totals
Feature: SauceDemo checkout order totals
  As a shopper
  I want to verify that the order summary shows correct pricing information
  So that I am charged the right amount

  Background:
    Given the standard user is logged in and on the products page

  @smoke @regression
  Scenario: Checkout overview shows a non-empty subtotal for a single item
    When the user adds "Sauce Labs Bike Light" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Eve" "Green" "11201"
    Then the checkout overview subtotal is not empty

  @regression
  Scenario: Checkout overview shows a non-empty tax amount
    When the user adds "Sauce Labs Backpack" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Frank" "Hill" "60601"
    Then the checkout overview tax is not empty

  @regression
  Scenario: Checkout overview total is not empty
    When the user adds "Sauce Labs Fleece Jacket" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Grace" "Lee" "77001"
    Then the checkout overview total is not empty

  @regression
  Scenario: Subtotal label contains dollar sign
    When the user adds "Sauce Labs Onesie" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Henry" "Kim" "98101"
    Then the checkout overview subtotal contains "$"

  @regression
  Scenario: Total label contains dollar sign
    When the user adds "Sauce Labs Bolt T-Shirt" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Iris" "Cho" "02101"
    Then the checkout overview total contains "$"

  @regression
  Scenario Outline: Each product generates a non-empty total at checkout
    When the user adds "<product>" to the cart from the inventory
    And the user proceeds to checkout overview with customer info "Jack" "Vu" "33101"
    Then the checkout overview total is not empty

    Examples:
      | product                           |
      | Sauce Labs Backpack               |
      | Sauce Labs Bike Light             |
      | Sauce Labs Bolt T-Shirt           |
      | Sauce Labs Fleece Jacket          |
      | Sauce Labs Onesie                 |
      | Test.allTheThings() T-Shirt (Red) |

  @regression
  Scenario: Two-item order shows both products on the overview
    When the user adds the following products to the cart:
      | Sauce Labs Backpack     |
      | Sauce Labs Bolt T-Shirt |
    And the user proceeds to checkout overview with customer info "Karen" "Patel" "10001"
    Then the checkout overview contains the item "Sauce Labs Backpack"
    And the checkout overview contains the item "Sauce Labs Bolt T-Shirt"
