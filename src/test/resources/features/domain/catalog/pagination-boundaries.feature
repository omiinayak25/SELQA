@domain @catalog @pagination
Feature: Pagination Boundaries — exhaustive (total, page, size) → (items, hasNext, hasPrev, totalPages)
  As the pagination service
  I want to confirm every boundary and equivalence class of the paginate operation
  So that page navigation state flags and item counts are always correct

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  # ------------------------------------------------------------------ #
  #  Large Scenario Outline: (page, size) → item count on full catalog  #
  #  (total = 12 products)                                               #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Full-catalog pagination yields the correct item count per page
    When I paginate the full catalog with page <page> and size <size>
    Then page <page> has <items> items

    Examples:
      | page | size | items |
      | 1    | 1    | 1     |
      | 2    | 1    | 1     |
      | 12   | 1    | 1     |
      | 13   | 1    | 0     |
      | 1    | 2    | 2     |
      | 2    | 2    | 2     |
      | 6    | 2    | 2     |
      | 7    | 2    | 0     |
      | 1    | 3    | 3     |
      | 2    | 3    | 3     |
      | 3    | 3    | 3     |
      | 4    | 3    | 3     |
      | 5    | 3    | 0     |
      | 1    | 4    | 4     |
      | 2    | 4    | 4     |
      | 3    | 4    | 4     |
      | 4    | 4    | 0     |
      | 1    | 5    | 5     |
      | 2    | 5    | 5     |
      | 3    | 5    | 2     |
      | 4    | 5    | 0     |
      | 1    | 6    | 6     |
      | 2    | 6    | 6     |
      | 3    | 6    | 0     |
      | 1    | 7    | 7     |
      | 2    | 7    | 5     |
      | 3    | 7    | 0     |
      | 1    | 8    | 8     |
      | 2    | 8    | 4     |
      | 3    | 8    | 0     |
      | 1    | 10   | 10    |
      | 2    | 10   | 2     |
      | 3    | 10   | 0     |
      | 1    | 11   | 11    |
      | 2    | 11   | 1     |
      | 3    | 11   | 0     |
      | 1    | 12   | 12    |
      | 2    | 12   | 0     |
      | 1    | 13   | 12    |
      | 1    | 100  | 12    |
      | 99   | 5    | 0     |
      | 1000 | 1    | 0     |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: total pages calculation for each size             #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Total pages is computed correctly for each page size against 12 products
    When I paginate the full catalog with page 1 and size <size>
    Then the total pages is <totalPages>
    And the page total is 12 products

    Examples:
      | size | totalPages |
      | 1    | 12         |
      | 2    | 6          |
      | 3    | 4          |
      | 4    | 3          |
      | 5    | 3          |
      | 6    | 2          |
      | 7    | 2          |
      | 8    | 2          |
      | 9    | 2          |
      | 10   | 2          |
      | 11   | 2          |
      | 12   | 1          |
      | 13   | 1          |
      | 100  | 1          |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: hasNext flag correctness                          #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: hasNext is true for pages that are not the last page
    When I paginate the full catalog with page <page> and size <size>
    Then the result has next page

    Examples:
      | page | size |
      | 1    | 5    |
      | 2    | 5    |
      | 1    | 1    |
      | 1    | 11   |
      | 1    | 6    |
      | 1    | 7    |
      | 1    | 2    |
      | 5    | 2    |
      | 1    | 4    |
      | 2    | 4    |

  @positive @regression @boundary
  Scenario Outline: hasNext is false for the final page or beyond
    When I paginate the full catalog with page <page> and size <size>
    Then the result has no next page

    Examples:
      | page | size |
      | 3    | 5    |
      | 1    | 12   |
      | 1    | 100  |
      | 2    | 12   |
      | 2    | 6    |
      | 2    | 11   |
      | 4    | 3    |
      | 99   | 5    |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: hasPrev flag correctness                          #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: hasPrev is true for any page > 1
    When I paginate the full catalog with page <page> and size <size>
    Then the result has previous page

    Examples:
      | page | size |
      | 2    | 5    |
      | 3    | 5    |
      | 2    | 1    |
      | 12   | 1    |
      | 2    | 6    |
      | 3    | 4    |
      | 2    | 7    |
      | 99   | 5    |

  @positive @regression @boundary
  Scenario Outline: hasPrev is false for page 1 regardless of size
    When I paginate the full catalog with page 1 and size <size>
    Then the result has no previous page

    Examples:
      | size |
      | 1    |
      | 5    |
      | 12   |
      | 100  |
      | 3    |
      | 7    |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: invalid params → PAGE_BAD_PARAMS                  #
  # ------------------------------------------------------------------ #

  @negative @validation @boundary
  Scenario Outline: Invalid page or size values raise PAGE_BAD_PARAMS
    When I paginate the full catalog with invalid page <page> and size <size>
    Then a domain error "PAGE_BAD_PARAMS" is raised

    Examples:
      | page | size |
      | 0    | 5    |
      | -1   | 5    |
      | -100 | 5    |
      | 1    | 0    |
      | 1    | -1   |
      | 1    | -10  |
      | 0    | 0    |
      | -1   | -1   |
      | 0    | 1    |

  # ------------------------------------------------------------------ #
  #  Empty list — all boundary assertions                                #
  # ------------------------------------------------------------------ #

  @positive @boundary
  Scenario Outline: Paginating an empty list always has 0 total, 1 totalPage, empty items
    When I paginate an empty list with page <page> and size <size>
    Then the page items are empty
    And the page total is 0 products
    And the total pages is 1
    And the result has no next page

    Examples:
      | page | size |
      | 1    | 1    |
      | 1    | 5    |
      | 1    | 100  |
      | 5    | 10   |
      | 99   | 3    |

  # ------------------------------------------------------------------ #
  #  Page-size property assertion                                        #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: The page size property reflects the requested size
    When I paginate the full catalog with page 1 and size <size>
    Then the page size is <size>

    Examples:
      | size |
      | 1    |
      | 3    |
      | 5    |
      | 7    |
      | 12   |
