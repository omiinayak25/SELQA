@domain @catalog @filters
Feature: Product Filtering
  As the filter service
  I want to narrow the product list by category, brand, price, rating, and stock
  So that customers see only relevant products

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  @smoke @positive
  Scenario: Filtering by category returns only products in that category
    When I filter the catalog by category "Electronics"
    Then the filter returns 4 products
    And all filter results are in category "Electronics"

  @positive
  Scenario: Filtering by brand returns only products from that brand
    When I filter the catalog by brand "SoundMax"
    Then the filter returns 2 products
    And all filter results are from brand "SoundMax"
    And the filter results contain product "Wireless Headphones"
    And the filter results contain product "Bluetooth Speaker"

  @positive
  Scenario: Filtering by price range returns products within the range
    When I filter the catalog by price between "20.00" and "60.00"
    Then the filter returns 5 products
    And all filter results have price between "20.00" and "60.00"

  @negative @validation
  Scenario: Filtering with inverted price range raises an error
    When I filter the catalog by price between "100.00" and "50.00" inverted
    Then a domain error "FILTER_BAD_RANGE" is raised
    And the domain error message contains "minPrice"

  @positive
  Scenario: Filtering by minimum rating returns highly-rated products
    When I filter the catalog by minimum rating 4.5
    Then the filter returns 4 products
    And all filter results have rating at least 4.5

  @positive
  Scenario: Filtering to in-stock products only
    When I filter the catalog to in-stock products only
    Then the filter returns 7 products
    And all filter results are in stock

  @positive
  Scenario: Filtering to out-of-stock products only
    When I filter the catalog to out-of-stock products only
    Then the filter returns 5 products
    And all filter results are out of stock

  @positive @business
  Scenario: Combining category and brand filters applies AND logic
    When I filter the catalog by category "Electronics" and brand "SoundMax"
    Then the filter returns 2 products
    And all filter results are in category "Electronics"
    And all filter results are from brand "SoundMax"

  @positive @business
  Scenario: Three-way filter combines category, in-stock, and price
    When I filter the catalog by category "Clothing" in-stock and price between "20.00" and "100.00"
    Then the filter returns 2 products
    And all filter results are in category "Clothing"
    And all filter results are in stock

  @negative @boundary
  Scenario: Filtering by a category that has no products returns empty results
    When I filter the catalog by category "Furniture"
    Then the filter results are empty

  @negative @boundary
  Scenario: Filtering by an unknown brand returns empty results
    When I filter the catalog by brand "UnknownBrand"
    Then the filter results are empty

  @positive @boundary
  Scenario: Filtering with a very wide price range returns all products
    When I filter the catalog by price between "0.00" and "9999.99"
    Then the filter returns 12 products

  @positive @boundary
  Scenario: Filtering with exact price boundary (min equals max) returns matching products
    When I filter the catalog by price between "79.99" and "79.99"
    Then the filter returns 1 products
    And the filter results contain product "Wireless Headphones"

  @positive @regression @boundary
  Scenario Outline: Category filter returns the expected product count
    When I filter the catalog by category "<category>"
    Then the filter returns <count> products

    Examples:
      | category    | count |
      | Electronics | 4     |
      | Clothing    | 3     |
      | Home        | 3     |
      | Sports      | 2     |
      | Furniture   | 0     |

  @positive @regression @boundary
  Scenario Outline: Price range filter returns the expected product count
    When I filter the catalog by price between "<min>" and "<max>"
    Then the filter returns <count> products

    Examples:
      | min   | max    | count |
      | 0.00  | 30.00  | 2     |
      | 30.00 | 60.00  | 4     |
      | 60.00 | 100.00 | 2     |
      | 100.00| 999.99 | 4     |
      | 0.00  | 9999.99| 12    |
