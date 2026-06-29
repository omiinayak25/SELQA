@domain @catalog @search
Feature: Product Search
  As the search service
  I want to find products by keyword over name and tags
  So that customers can discover relevant products efficiently

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  @smoke @positive
  Scenario: Searching by a term in the product name returns matching products
    When I search the catalog for "headphones"
    Then the search returns 1 products
    And the search results contain product "Wireless Headphones"

  @positive
  Scenario: Search is case-insensitive for name matches
    When I search the catalog for "HEADPHONES"
    Then the search returns 1 products
    And the search results contain product "Wireless Headphones"

  @positive
  Scenario: Search matches products by tag
    When I search the catalog for "bluetooth"
    Then the search returns 1 products
    And the search results contain product "Bluetooth Speaker"

  @positive
  Scenario: Search returns multiple products when term matches several
    When I search the catalog for "wireless"
    Then the search returns 2 products
    And the search results contain product "Wireless Headphones"
    And the search results contain product "Bluetooth Speaker"

  @positive @business
  Scenario: Name-matched product ranks above tag-only matched product
    When I search the catalog for "sport"
    Then the search results contain product "Sports T-Shirt"
    And the first search result is "Sports T-Shirt"

  @negative @boundary
  Scenario: Searching for a non-existent term returns empty results
    When I search the catalog for "xylophone"
    Then the search results are empty

  @positive @boundary
  Scenario: Empty query returns all products
    When I search the catalog with an empty query
    Then the search returns 12 products

  @positive @regression
  Scenario: Search for a partial name term returns matching products
    When I search the catalog for "watch"
    Then the search returns 1 products
    And the search results contain product "Smart Watch"

  @positive @regression
  Scenario: Search for "fitness" matches both name and tag products
    When I search the catalog for "fitness"
    Then the search returns 3 products
    And the search results contain product "Fitness Tracker"

  @negative @boundary
  Scenario: Search for a number string returns no results
    When I search the catalog for "12345"
    Then the search results are empty

  @positive @regression
  Scenario: Search for "audio" tag matches speaker and headphones
    When I search the catalog for "audio"
    Then the search returns 2 products
    And the search results contain product "Wireless Headphones"
    And the search results contain product "Bluetooth Speaker"

  @positive @boundary @regression
  Scenario Outline: Search terms return the expected product counts
    When I search the catalog for "<term>"
    Then the search returns <count> products

    Examples:
      | term       | count |
      | headphones | 1     |
      | wireless   | 2     |
      | sport      | 5     |
      | wearable   | 2     |
      | desk       | 2     |
      | coffee     | 1     |
      | purifier   | 1     |
      | bands      | 1     |
      | lamp       | 1     |
      | xylophone  | 0     |

  @positive @sanity
  Scenario: Search results do not include unrelated products
    When I search the catalog for "headphones"
    Then the search results do not contain product "Bluetooth Speaker"
    And the search results do not contain product "Running Shoes"
