@ui @saucedemo @e2e
Feature: SauceDemo checkout
  As a signed-in shopper
  I want to check out my cart
  So that I can complete a purchase

  @smoke
  Scenario: Complete a purchase of two items
    When the customer purchases the following items:
      | Sauce Labs Backpack    |
      | Sauce Labs Bolt T-Shirt |
    Then the order confirmation is shown

  Scenario: Complete a single-item purchase
    When the customer purchases the following items:
      | Sauce Labs Bike Light |
    Then the order confirmation is shown
