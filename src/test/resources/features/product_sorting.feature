@ui @saucedemo @sorting
Feature: SauceDemo product sorting
  As a shopper
  I want to sort products by different criteria
  So that I can find items easily

  Background:
    Given the standard user is logged in and on the products page

  @smoke @regression
  Scenario: Default sort order is Name A to Z
    Then the products are sorted alphabetically ascending by name

  @regression
  Scenario: Sorting by Name Z to A reverses alphabetical order
    When the user sorts products by "Name (Z to A)"
    Then the products are sorted alphabetically descending by name

  @regression
  Scenario: Sorting by Price low to high orders by price ascending
    When the user sorts products by "Price (low to high)"
    Then the products are sorted by price ascending

  @regression
  Scenario: Sorting by Price high to low orders by price descending
    When the user sorts products by "Price (high to low)"
    Then the products are sorted by price descending

  @regression
  Scenario Outline: Each sort option produces a valid product list of 6 items
    When the user sorts products by "<sortOption>"
    Then the inventory contains 6 products

    Examples:
      | sortOption          |
      | Name (A to Z)       |
      | Name (Z to A)       |
      | Price (low to high) |
      | Price (high to low) |

  @regression
  Scenario: Sorting does not remove any products from the page
    When the user sorts products by "Price (high to low)"
    Then the following products are visible in the inventory:
      | Sauce Labs Backpack              |
      | Sauce Labs Bike Light            |
      | Sauce Labs Bolt T-Shirt          |
      | Sauce Labs Fleece Jacket         |
      | Sauce Labs Onesie                |
      | Test.allTheThings() T-Shirt (Red) |

  @regression
  Scenario: After sorting Z to A the first product starts with T
    When the user sorts products by "Name (Z to A)"
    Then the first product name starts with "T"

  @regression
  Scenario: Cheapest product appears first when sorted by price low to high
    When the user sorts products by "Price (low to high)"
    Then the first product name is "Sauce Labs Onesie"
