@domain @catalog @pagination
Feature: Product Pagination
  As the pagination service
  I want to slice any product list into pages
  So that large catalogs are browsable without loading all items

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  @smoke @positive
  Scenario: First page of 5 from a 12-product catalog has 5 items
    When I paginate the full catalog with page 1 and size 5
    Then page 1 has 5 items
    And the page total is 12 products
    And the total pages is 3
    And the result has next page
    And the result has no previous page

  @positive
  Scenario: Second page of 5 items is a middle page
    When I paginate the full catalog with page 2 and size 5
    Then page 2 has 5 items
    And the result has next page
    And the result has previous page

  @positive @boundary
  Scenario: Last page may have fewer items than the page size
    When I paginate the full catalog with page 3 and size 5
    Then page 3 has 2 items
    And the result has no next page
    And the result has previous page

  @positive @boundary
  Scenario: Requesting a page beyond total returns empty items without error
    When I paginate the full catalog with page 99 and size 5
    Then the page items are empty
    And the operation succeeds

  @positive @boundary
  Scenario: Single-page result when size equals total
    When I paginate the full catalog with page 1 and size 12
    Then page 1 has 12 items
    And the total pages is 1
    And the result has no next page
    And the result has no previous page

  @positive @boundary
  Scenario: Oversized page returns all products on page 1
    When I paginate the full catalog with page 1 and size 100
    Then page 1 has 12 items
    And the result has no next page

  @positive
  Scenario: Page size of 1 creates as many pages as there are products
    When I paginate the full catalog with page 1 and size 1
    Then page 1 has 1 items
    And the total pages is 12
    And the result has next page

  @positive @boundary
  Scenario: Paginating an empty list returns empty items on page 1
    When I paginate an empty list with page 1 and size 5
    Then the page items are empty
    And the page total is 0 products
    And the total pages is 1
    And the result has no next page
    And the result has no previous page

  @negative @validation
  Scenario: Page number of zero is rejected
    When I paginate the full catalog with invalid page 0 and size 5
    Then a domain error "PAGE_BAD_PARAMS" is raised

  @negative @validation
  Scenario: Negative page number is rejected
    When I paginate the full catalog with invalid page -1 and size 5
    Then a domain error "PAGE_BAD_PARAMS" is raised

  @negative @validation
  Scenario: Page size of zero is rejected
    When I paginate the full catalog with invalid page 1 and size 0
    Then a domain error "PAGE_BAD_PARAMS" is raised

  @negative @validation
  Scenario: Negative size is rejected
    When I paginate the full catalog with invalid page 1 and size -5
    Then a domain error "PAGE_BAD_PARAMS" is raised
    And the domain error message contains "size=-5"

  @positive @regression
  Scenario: Page 2 with size 4 has correct items and totals
    When I paginate the full catalog with page 2 and size 4
    Then page 2 has 4 items
    And the page total is 12 products
    And the total pages is 3
    And the result has next page
    And the result has previous page
    And the page size is 4

  @positive @regression @boundary
  Scenario Outline: Various page/size combinations yield the expected item counts
    When I paginate the full catalog with page <page> and size <size>
    Then page <page> has <items> items

    Examples:
      | page | size | items |
      | 1    | 3    | 3     |
      | 2    | 3    | 3     |
      | 4    | 3    | 3     |
      | 1    | 12   | 12    |
      | 1    | 6    | 6     |
      | 2    | 6    | 6     |
      | 1    | 7    | 7     |
      | 2    | 7    | 5     |
      | 99   | 5    | 0     |
