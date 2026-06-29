@domain @catalog @search
Feature: Search Matrix — query-to-count and query-to-contains coverage
  As the search service
  I want to confirm that every distinct query term returns the exact expected result set
  So that ranking, tag-matching, and case-folding behave correctly for all fixture products

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  # ------------------------------------------------------------------ #
  #  Scenario Outline: exhaustive query -> expected count matrix         #
  #  Covers distinct equivalence classes: name-only, tag-only,          #
  #  multi-match, case variants, partial, numeric, special-char.        #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Query term produces the expected number of search results
    When I search the catalog for "<term>"
    Then the search returns <count> products

    Examples:
      | term        | count |
      | speaker     | 1     |
      | bluetooth   | 1     |
      | smart       | 1     |
      | watch       | 1     |
      | yoga        | 1     |
      | pants       | 1     |
      | maker       | 1     |
      | coffee      | 1     |
      | purifier    | 1     |
      | air         | 1     |
      | ergonomic   | 1     |
      | stand       | 1     |
      | lamp        | 1     |
      | tracker     | 1     |
      | running     | 1     |
      | resistance  | 1     |
      | shirt       | 1     |
      | kitchen     | 1     |
      | health      | 1     |
      | audio       | 2     |
      | wireless    | 2     |
      | wearable    | 2     |
      | desk        | 2     |
      | fitness     | 3     |
      | sport       | 5     |
      | xylophone   | 0     |
      | 99999       | 0     |
      | !@#         | 0     |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: case-insensitive variants of key terms            #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Case variant of a search term returns the same count as the lower-case form
    When I search the catalog for "<term>"
    Then the search returns <count> products

    Examples:
      | term        | count |
      | SPEAKER     | 1     |
      | Bluetooth   | 1     |
      | SMART       | 1     |
      | YOGA        | 1     |
      | COFFEE      | 1     |
      | PURIFIER    | 1     |
      | AUDIO       | 2     |
      | WIRELESS    | 2     |
      | FITNESS     | 3     |
      | SPORT       | 5     |
      | WiReLeSs    | 2     |
      | FiTnEsS     | 3     |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: name-match results contain the expected product   #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Searching by product name term returns that product in results
    When I search the catalog for "<term>"
    Then the search results contain product "<product>"

    Examples:
      | term        | product               |
      | headphones  | Wireless Headphones   |
      | speaker     | Bluetooth Speaker     |
      | watch       | Smart Watch           |
      | yoga        | Yoga Pants            |
      | coffee      | Coffee Maker          |
      | purifier    | Air Purifier          |
      | lamp        | Desk Lamp             |
      | tracker     | Fitness Tracker       |
      | running     | Running Shoes         |
      | resistance  | Resistance Bands      |
      | shirt       | Sports T-Shirt        |
      | stand       | Laptop Stand          |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: tag-only match results contain the expected item  #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Searching by a tag returns the tag-owning product
    When I search the catalog for "<tag>"
    Then the search results contain product "<product>"

    Examples:
      | tag         | product               |
      | bluetooth   | Bluetooth Speaker     |
      | wearable    | Smart Watch           |
      | ergonomic   | Laptop Stand          |
      | appliance   | Coffee Maker          |
      | health      | Air Purifier          |
      | lighting    | Desk Lamp             |
      | bands       | Resistance Bands      |
      | shoes       | Running Shoes         |
      | kitchen     | Coffee Maker          |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: name-ranked-first when term appears in name       #
  # ------------------------------------------------------------------ #

  @positive @business
  Scenario Outline: When a term appears in a product name that product ranks first in results
    When I search the catalog for "<term>"
    Then the first search result is "<top_product>"

    Examples:
      | term        | top_product           |
      | sport       | Sports T-Shirt        |
      | audio       | Wireless Headphones   |
      | desk        | Desk Lamp             |
      | fitness     | Fitness Tracker       |
      | wearable    | Smart Watch           |

  # ------------------------------------------------------------------ #
  #  Scenario Outline: exclusion — unrelated products not in results     #
  # ------------------------------------------------------------------ #

  @positive @sanity
  Scenario Outline: Products unrelated to the query term are excluded from results
    When I search the catalog for "<term>"
    Then the search results do not contain product "<absent>"

    Examples:
      | term       | absent              |
      | headphones | Bluetooth Speaker   |
      | yoga       | Running Shoes       |
      | coffee     | Air Purifier        |
      | purifier   | Coffee Maker        |
      | lamp       | Fitness Tracker     |
      | tracker    | Desk Lamp           |
      | running    | Yoga Pants          |
      | speaker    | Wireless Headphones |
