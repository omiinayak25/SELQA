@api @dummyjson @products
Feature: DummyJSON product API operations
  As an API tester
  I want to validate the DummyJSON products endpoints
  So that product data is returned correctly

  @smoke @regression
  Scenario: Fetching all products returns HTTP 200
    When I request all DummyJSON products
    Then the API response status is 200

  @regression
  Scenario: Product list contains a products array
    When I request all DummyJSON products
    Then the DummyJSON product list JSON path "products" is not null

  @regression
  Scenario: Product list contains a total field
    When I request all DummyJSON products
    Then the DummyJSON product list JSON path "total" is not null

  @regression
  Scenario Outline: Fetching individual products by ID returns HTTP 200
    When I request DummyJSON product with id <productId>
    Then the API response status is 200

    Examples:
      | productId |
      | 1         |
      | 2         |
      | 5         |
      | 10        |
      | 20        |

  @regression
  Scenario: Single product response contains a title field
    When I request DummyJSON product with id 1
    Then the DummyJSON product JSON path "title" is not null

  @regression
  Scenario: Single product response contains a price field
    When I request DummyJSON product with id 1
    Then the DummyJSON product JSON path "price" is not null

  @regression
  Scenario: Single product response contains a category field
    When I request DummyJSON product with id 1
    Then the DummyJSON product JSON path "category" is not null

  @negative @regression
  Scenario: Fetching a non-existent product returns HTTP 404
    When I request DummyJSON product with id 99999
    Then the API response status is 404

  @regression
  Scenario: Searching products for "phone" returns results
    When I search DummyJSON products for "phone"
    Then the API response status is 200
    And the DummyJSON product list JSON path "products" is not null

  @regression
  Scenario: Searching products for "laptop" returns HTTP 200
    When I search DummyJSON products for "laptop"
    Then the API response status is 200

  @regression
  Scenario Outline: Various product search terms return HTTP 200
    When I search DummyJSON products for "<term>"
    Then the API response status is 200

    Examples:
      | term      |
      | phone     |
      | laptop    |
      | shirt     |
      | beauty    |

  @regression
  Scenario: Fetching product categories returns HTTP 200
    When I request DummyJSON product categories
    Then the API response status is 200

  @regression
  Scenario: Product categories response is not empty
    When I request DummyJSON product categories
    Then the categories response body is not empty

  @regression
  Scenario: Paginated product request with limit 5 returns HTTP 200
    When I request DummyJSON products with limit 5 and skip 0
    Then the API response status is 200

  @regression
  Scenario: DummyJSON authentication returns an access token
    When I authenticate to DummyJSON with username "emilys" and password "emilyspass"
    Then the API response status is 200
    And the auth response contains an access token

  @negative @regression
  Scenario: DummyJSON authentication with bad credentials returns non-200
    When I authenticate to DummyJSON with username "baduser" and password "badpass"
    Then the API response status is 400
