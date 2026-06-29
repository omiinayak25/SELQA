@domain @catalog @product-management
Feature: Product Catalog Management
  As the product catalog service
  I want to manage products with real validation
  So that only valid products enter the catalog and lookups return correct data

  Background:
    Given a clean catalog service

  @smoke @positive
  Scenario: A valid product can be added to the catalog
    When I add a catalog product named "Wireless Keyboard" in category "Electronics" brand "TypeMaster" priced at "59.99"
    Then the operation succeeds
    And the catalog contains 1 products

  @positive
  Scenario: Adding multiple products increases the catalog size
    When I add a catalog product named "Keyboard" in category "Electronics" brand "TypeMaster" priced at "59.99"
    And I add a catalog product named "Mouse" in category "Electronics" brand "TypeMaster" priced at "29.99"
    And I add a catalog product named "Monitor" in category "Electronics" brand "ViewTech" priced at "299.99"
    Then the catalog contains 3 products

  @positive
  Scenario: A product can be retrieved by id after being added
    When I add a catalog product named "Gaming Chair" in category "Furniture" brand "ErgoSeat" priced at "349.99"
    Then the operation succeeds
    When I look up the last added catalog product by id
    Then the operation succeeds
    And the catalog product name is "Gaming Chair"
    And the catalog product category is "Furniture"

  @negative @validation
  Scenario: Adding a product with a blank name is rejected
    When I add a catalog product with blank name in category "Electronics" priced at "49.99"
    Then a domain error "CATALOG_BLANK_NAME" is raised

  @negative @validation
  Scenario: Adding a product with a negative price is rejected
    When I add a catalog product named "Broken Widget" with negative price "-10.00"
    Then a domain error "CATALOG_BAD_PRICE" is raised

  @negative @boundary
  Scenario: Looking up a non-existent product id raises an error
    When I look up catalog product by id 999999
    Then a domain error "CATALOG_NOT_FOUND" is raised
    And the domain error message contains "999999"

  @positive @regression
  Scenario: A product can have a zero price (free product)
    When I add a catalog product named "Free Sample" in category "Samples" brand "TestBrand" priced at "0.00"
    Then the operation succeeds
    And the catalog contains 1 products

  @negative @validation @regression
  Scenario Outline: Invalid product attributes are rejected with specific error codes
    When I add a catalog product named "<name>" with negative price "<price>"
    Then a domain error "<code>" is raised

    Examples:
      | name          | price  | code             |
      | ValidName     | -1.00  | CATALOG_BAD_PRICE |
      | AnotherItem   | -99.99 | CATALOG_BAD_PRICE |

  @positive @boundary
  Scenario: Catalog starts empty before any product is added
    Then the catalog contains 0 products

  @positive @sanity
  Scenario: Products are seeded correctly by the standard fixture
    Given the catalog is seeded with the standard product set
    Then the catalog contains 12 products
