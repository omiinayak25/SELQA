@domain @catalog @filters
Feature: Filter Combinations — category × brand × price × rating × stock matrix
  As the filter service
  I want to correctly apply AND-combined filters across all filter axes
  So that every combination of criteria yields the exact right product subset

  Background:
    Given a clean catalog service
    And the catalog is seeded with the standard product set

  # ------------------------------------------------------------------ #
  #  Brand-only filter matrix (each brand in the fixture)                #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Filtering by each known brand returns the exact product count
    When I filter the catalog by brand "<brand>"
    Then the filter returns <count> products
    And all filter results are from brand "<brand>"

    Examples:
      | brand      | count |
      | SoundMax   | 2     |
      | TechWear   | 2     |
      | DeskPro    | 2     |
      | SprintGear | 2     |
      | FlexFit    | 2     |
      | BrewMaster | 2     |

  # ------------------------------------------------------------------ #
  #  Category × brand AND-filter combinations                            #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Filtering by category AND brand applies AND logic and returns exact count
    When I filter the catalog by category "<category>" and brand "<brand>"
    Then the filter returns <count> products
    And all filter results are in category "<category>"
    And all filter results are from brand "<brand>"

    Examples:
      | category    | brand      | count |
      | Electronics | SoundMax   | 2     |
      | Electronics | TechWear   | 1     |
      | Electronics | DeskPro    | 1     |
      | Electronics | SprintGear | 0     |
      | Electronics | FlexFit    | 0     |
      | Clothing    | SprintGear | 2     |
      | Clothing    | FlexFit    | 1     |
      | Clothing    | BrewMaster | 0     |
      | Home        | BrewMaster | 2     |
      | Home        | DeskPro    | 1     |
      | Home        | TechWear   | 0     |
      | Sports      | TechWear   | 1     |
      | Sports      | FlexFit    | 1     |
      | Sports      | SoundMax   | 0     |

  # ------------------------------------------------------------------ #
  #  Rating-only filter matrix — distinct rating thresholds              #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Filtering by minimum rating returns the correct count
    When I filter the catalog by minimum rating <minRating>
    Then the filter returns <count> products
    And all filter results have rating at least <minRating>

    Examples:
      | minRating | count |
      | 2.5       | 12    |
      | 3.5       | 12    |
      | 3.8       | 11    |
      | 3.9       | 10    |
      | 4.0       | 9     |
      | 4.1       | 8     |
      | 4.2       | 7     |
      | 4.3       | 6     |
      | 4.4       | 5     |
      | 4.5       | 4     |
      | 4.6       | 3     |
      | 4.7       | 2     |
      | 4.8       | 1     |
      | 5.0       | 0     |

  # ------------------------------------------------------------------ #
  #  Price-range filter matrix — narrower and tighter boundary bands     #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Price-range filter with distinct boundary bands returns the correct count
    When I filter the catalog by price between "<min>" and "<max>"
    Then the filter returns <count> products
    And all filter results have price between "<min>" and "<max>"

    Examples:
      | min    | max    | count |
      | 0.00   | 19.99  | 1     |
      | 19.99  | 19.99  | 1     |
      | 20.00  | 24.99  | 1     |
      | 24.99  | 24.99  | 1     |
      | 25.00  | 34.99  | 1     |
      | 34.99  | 34.99  | 1     |
      | 35.00  | 39.99  | 1     |
      | 39.99  | 39.99  | 1     |
      | 40.00  | 49.99  | 1     |
      | 49.99  | 49.99  | 1     |
      | 50.00  | 54.99  | 1     |
      | 54.99  | 54.99  | 1     |
      | 55.00  | 79.99  | 1     |
      | 79.99  | 79.99  | 1     |
      | 80.00  | 89.99  | 1     |
      | 89.99  | 89.99  | 1     |
      | 90.00  | 129.99 | 1     |
      | 129.99 | 129.99 | 1     |
      | 130.00 | 149.99 | 1     |
      | 149.99 | 149.99 | 1     |
      | 150.00 | 199.99 | 1     |
      | 199.99 | 199.99 | 1     |
      | 200.00 | 249.99 | 1     |
      | 249.99 | 249.99 | 1     |
      | 250.00 | 999.99 | 0     |
      | 0.00   | 100.00 | 8     |
      | 0.00   | 50.00  | 5     |
      | 50.00  | 100.00 | 3     |
      | 100.00 | 249.99 | 4     |

  # ------------------------------------------------------------------ #
  #  Stock + category compound filter                                    #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Filtering by category and stock status returns the correct count
    When I filter the catalog by category "<category>" in-stock and price between "0.00" and "9999.99"
    Then the filter returns <count> products
    And all filter results are in category "<category>"
    And all filter results are in stock

    Examples:
      | category    | count |
      | Electronics | 3     |
      | Clothing    | 2     |
      | Home        | 1     |
      | Sports      | 1     |

  # ------------------------------------------------------------------ #
  #  In-stock + price range                                              #
  # ------------------------------------------------------------------ #

  @positive @business @boundary
  Scenario Outline: Filtering in-stock products within a price range returns the correct count
    When I filter the catalog by price between "<min>" and "<max>"
    Then the filter returns <count> products

    Examples:
      | min    | max    | count |
      | 0.00   | 20.00  | 1     |
      | 20.00  | 60.00  | 5     |
      | 60.00  | 100.00 | 2     |
      | 100.00 | 300.00 | 4     |

  # ------------------------------------------------------------------ #
  #  Inverted price range — always raises FILTER_BAD_RANGE               #
  # ------------------------------------------------------------------ #

  @negative @validation @boundary
  Scenario Outline: Inverted price range always raises FILTER_BAD_RANGE regardless of values
    When I filter the catalog by price between "<min>" and "<max>" inverted
    Then a domain error "FILTER_BAD_RANGE" is raised
    And the domain error message contains "minPrice"

    Examples:
      | min    | max   |
      | 100.00 | 99.99 |
      | 50.00  | 49.99 |
      | 1.00   | 0.99  |
      | 999.99 | 0.01  |

  # ------------------------------------------------------------------ #
  #  Unknown category / brand → empty result                             #
  # ------------------------------------------------------------------ #

  @negative @boundary @regression
  Scenario Outline: Filtering by an unknown category or brand returns empty results
    When I filter the catalog by category "<category>"
    Then the filter results are empty

    Examples:
      | category     |
      | Furniture    |
      | Toys         |
      | Automotive   |
      | Groceries    |

  @negative @boundary @regression
  Scenario Outline: Filtering by an unknown brand returns empty results
    When I filter the catalog by brand "<brand>"
    Then the filter results are empty

    Examples:
      | brand         |
      | UnknownBrand  |
      | NoNameCorp    |
      | GhostBrand    |

  # ------------------------------------------------------------------ #
  #  Rating filter boundary — confirm individual products per rating     #
  # ------------------------------------------------------------------ #

  @positive @boundary
  Scenario Outline: Minimum rating exactly equal to a product rating includes that product
    When I filter the catalog by minimum rating <minRating>
    Then the filter results contain product "<product>"

    Examples:
      | minRating | product             |
      | 4.5       | Wireless Headphones |
      | 4.6       | Yoga Pants          |
      | 4.7       | Smart Watch         |
      | 4.8       | Air Purifier        |
      | 3.5       | Desk Lamp           |
      | 3.8       | Laptop Stand        |
      | 3.9       | Sports T-Shirt      |
      | 4.0       | Resistance Bands    |
      | 4.1       | Fitness Tracker     |
      | 4.2       | Bluetooth Speaker   |
      | 4.3       | Running Shoes       |
      | 4.4       | Coffee Maker        |

  @negative @boundary
  Scenario Outline: Minimum rating just above a product rating excludes that product
    When I filter the catalog by minimum rating <minRating>
    Then the filter results do not contain product "<product>"

    Examples:
      | minRating | product             |
      | 4.6       | Wireless Headphones |
      | 4.7       | Yoga Pants          |
      | 4.8       | Smart Watch         |
      | 3.6       | Desk Lamp           |
      | 3.9       | Laptop Stand        |
      | 4.0       | Sports T-Shirt      |
      | 4.1       | Resistance Bands    |
