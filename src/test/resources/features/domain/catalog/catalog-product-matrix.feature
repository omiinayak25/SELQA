@domain @catalog @product-management
Feature: Catalog Product Matrix — multi-axis product add/validate/lookup
  As the product catalog service
  I want to confirm that every valid product attribute combination is accepted
  and every invalid combination is rejected with the correct error code
  So that the catalog invariants hold for all boundary and equivalence classes

  Background:
    Given a clean catalog service

  # ------------------------------------------------------------------ #
  #  Valid product additions — distinct category/brand/price combos      #
  # ------------------------------------------------------------------ #

  @positive @regression @boundary
  Scenario Outline: Valid products across all categories and brands can be added
    When I add a catalog product named "<name>" in category "<category>" brand "<brand>" priced at "<price>"
    Then the operation succeeds
    And the catalog contains 1 products

    Examples:
      | name                    | category    | brand      | price  |
      | Wireless Keyboard       | Electronics | TypeMaster | 59.99  |
      | USB Hub                 | Electronics | DeskPro    | 29.99  |
      | Gaming Monitor          | Electronics | ViewTech   | 399.99 |
      | Winter Jacket           | Clothing    | NorthStyle | 149.00 |
      | Compression Socks       | Clothing    | FlexFit    | 12.99  |
      | Blender Pro             | Home        | BrewMaster | 89.99  |
      | Smart Thermostat        | Home        | TechWear   | 179.99 |
      | Yoga Block              | Sports      | FlexFit    | 9.99   |
      | Protein Shaker          | Sports      | SprintGear | 14.99  |
      | Free Sample             | Samples     | TestBrand  | 0.00   |
      | One-cent Widget         | Misc        | GenericCo  | 0.01   |
      | Premium Subscription    | Services    | TechWear   | 999.99 |

  # ------------------------------------------------------------------ #
  #  Standard fixture size after seeding                                 #
  # ------------------------------------------------------------------ #

  @positive @sanity
  Scenario: The standard fixture contains exactly 12 products
    Given the catalog is seeded with the standard product set
    Then the catalog contains 12 products

  # ------------------------------------------------------------------ #
  #  Adding products beyond the seeded set grows the count correctly     #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Adding <extra> products to a seeded catalog yields <expected> total products
    Given the catalog is seeded with the standard product set
    When I add a catalog product named "Extra Product <extra>" in category "General" brand "TestBrand" priced at "1.00"
    Then the catalog contains <expected> products

    Examples:
      | extra | expected |
      | 1     | 13       |

  # ------------------------------------------------------------------ #
  #  Negative price — rejected for any name or category                  #
  # ------------------------------------------------------------------ #

  @negative @validation @regression @boundary
  Scenario Outline: Products with a negative price are rejected with CATALOG_BAD_PRICE
    When I add a catalog product named "<name>" with negative price "<price>"
    Then a domain error "CATALOG_BAD_PRICE" is raised

    Examples:
      | name              | price    |
      | NegativeBook      | -0.01    |
      | CheapWidget       | -1.00    |
      | MidWidget         | -49.99   |
      | HighWidget        | -100.00  |
      | MaxNegWidget      | -9999.99 |

  # ------------------------------------------------------------------ #
  #  Blank name — rejected                                               #
  # ------------------------------------------------------------------ #

  @negative @validation @regression @boundary
  Scenario Outline: Products with a blank name are rejected with CATALOG_BLANK_NAME
    When I add a catalog product with blank name in category "<category>" priced at "<price>"
    Then a domain error "CATALOG_BLANK_NAME" is raised

    Examples:
      | category    | price  |
      | Electronics | 49.99  |
      | Clothing    | 19.99  |
      | Home        | 99.99  |
      | Sports      | 29.99  |
      | General     | 0.01   |

  # ------------------------------------------------------------------ #
  #  Lookup by non-existent id — CATALOG_NOT_FOUND                       #
  # ------------------------------------------------------------------ #

  @negative @boundary @regression
  Scenario Outline: Looking up a non-existent catalog product id raises CATALOG_NOT_FOUND
    When I look up catalog product by id <id>
    Then a domain error "CATALOG_NOT_FOUND" is raised
    And the domain error message contains "<fragment>"

    Examples:
      | id     | fragment |
      | 999999 | 999999   |
      | 0      | 0        |
      | -1     | -1       |
      | 1      | 1        |
      | 100    | 100      |

  # ------------------------------------------------------------------ #
  #  Retrieve a just-added product by id                                 #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: A just-added product is retrievable by id with the correct attributes
    When I add a catalog product named "<name>" in category "<category>" brand "<brand>" priced at "<price>"
    Then the operation succeeds
    When I look up the last added catalog product by id
    Then the operation succeeds
    And the catalog product name is "<name>"
    And the catalog product category is "<category>"

    Examples:
      | name              | category    | brand      | price  |
      | Gaming Chair      | Furniture   | ErgoSeat   | 349.99 |
      | Standing Desk     | Furniture   | DeskPro    | 499.99 |
      | Coffee Grinder    | Home        | BrewMaster | 79.99  |
      | Hiking Boots      | Clothing    | SprintGear | 119.99 |
      | Smart Scale       | Sports      | TechWear   | 59.99  |

  # ------------------------------------------------------------------ #
  #  Seeded products are accessible via standard category-named entries  #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Products added with real fixture values are found by catalog size
    Given a catalog product "<name>" in category "<category>" brand "<brand>" priced at "<price>" rated <rating> and "<stock>"
    Then the catalog contains 1 products

    Examples:
      | name                | category    | brand      | price  | rating | stock   |
      | Wireless Headphones | Electronics | SoundMax   | 79.99  | 4.5    | inStock |
      | Bluetooth Speaker   | Electronics | SoundMax   | 49.99  | 4.2    | inStock |
      | Smart Watch         | Electronics | TechWear   | 249.99 | 4.7    | inStock |
      | Laptop Stand        | Electronics | DeskPro    | 39.99  | 3.8    | outOfStock |
      | Running Shoes       | Clothing    | SprintGear | 89.99  | 4.3    | inStock |
      | Sports T-Shirt      | Clothing    | SprintGear | 24.99  | 3.9    | outOfStock |
      | Yoga Pants          | Clothing    | FlexFit    | 54.99  | 4.6    | inStock |
      | Coffee Maker        | Home        | BrewMaster | 129.99 | 4.4    | outOfStock |
      | Air Purifier        | Home        | BrewMaster | 199.99 | 4.8    | outOfStock |
      | Desk Lamp           | Home        | DeskPro    | 34.99  | 3.5    | inStock |
      | Fitness Tracker     | Sports      | TechWear   | 149.99 | 4.1    | outOfStock |
      | Resistance Bands    | Sports      | FlexFit    | 19.99  | 4.0    | inStock |
