@domain @commerce @wishlist
Feature: Wishlist Rules — add/dup/move-to-cart/boundary variations
  As the commerce platform
  I want to enforce wishlist business rules across all product IDs, states, and stock conditions
  So that saves, duplicates, moves, and capacity limits are all correctly handled

  Background:
    Given a clean wishlist, cart and product registry

  # ------------------------------------------------------------------ #
  #  Add every known SKU independently — positive regression matrix      #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Every registered product can be saved to a fresh wishlist
    When I add product <sku> to the wishlist
    Then the operation succeeds
    And the wishlist contains product <sku>
    And the wishlist has 1 items

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |
      | "SKU-ZERO"    |

  # ------------------------------------------------------------------ #
  #  Duplicate detection — every known SKU raises WISH_DUPLICATE         #
  # ------------------------------------------------------------------ #

  @negative @business @regression
  Scenario Outline: Adding an already-wishlisted product raises WISH_DUPLICATE
    Given product <sku> is already in the wishlist
    When I add product <sku> to the wishlist
    Then a domain error "WISH_DUPLICATE" is raised
    And the wishlist has 1 items

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |
      | "SKU-ZERO"    |

  # ------------------------------------------------------------------ #
  #  Unknown product IDs — WISH_UNKNOWN_PRODUCT                          #
  # ------------------------------------------------------------------ #

  @negative @validation @regression
  Scenario Outline: Adding an unknown product ID raises WISH_UNKNOWN_PRODUCT
    When I add product <sku> to the wishlist
    Then a domain error "WISH_UNKNOWN_PRODUCT" is raised
    And the wishlist is empty

    Examples:
      | sku             |
      | "SKU-MISSING"   |
      | "SKU-NONEXIST"  |
      | "SKU-GHOST"     |
      | "SKU-DELETED"   |
      | "UNKNOWN"       |
      | ""              |

  # ------------------------------------------------------------------ #
  #  Move-to-cart — in-stock SKUs succeed                                #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Moving an in-stock product from wishlist to cart succeeds
    Given product <sku> is already in the wishlist
    When I move product <sku> from the wishlist to the cart
    Then the operation succeeds
    And the moved product <sku> is now in the cart
    And the moved product <sku> is no longer in the wishlist
    And the cart line for product <sku> has quantity 1

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |

  # ------------------------------------------------------------------ #
  #  Move-to-cart — out-of-stock SKU raises CART_OUT_OF_STOCK            #
  # ------------------------------------------------------------------ #

  @negative @boundary @regression
  Scenario Outline: Moving a product with zero stock from wishlist to cart raises CART_OUT_OF_STOCK
    Given product <sku> is already in the wishlist
    And product <sku> has 0 units remaining in stock
    When I move product <sku> from the wishlist to the cart
    Then a domain error "CART_OUT_OF_STOCK" is raised
    And the wishlist contains product <sku>

    Examples:
      | sku           |
      | "SKU-ZERO"    |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |

  # ------------------------------------------------------------------ #
  #  Remove operations — idempotent and membership checks               #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Removing a product from the wishlist leaves the rest intact
    Given product "SKU-BOOK" is already in the wishlist
    And product "SKU-MUG" is already in the wishlist
    And product "SKU-LAPTOP" is already in the wishlist
    When I remove product <sku> from the wishlist
    Then the operation succeeds
    And the wishlist does not contain product <sku>
    And the wishlist has <remaining> items

    Examples:
      | sku           | remaining |
      | "SKU-BOOK"    | 2         |
      | "SKU-MUG"     | 2         |
      | "SKU-LAPTOP"  | 2         |

  @positive @business @boundary
  Scenario Outline: Removing a product not in the wishlist is a no-op
    Given product "SKU-BOOK" is already in the wishlist
    When I remove product <sku> from the wishlist
    Then the operation succeeds
    And the wishlist has 1 items

    Examples:
      | sku            |
      | "SKU-MUG"      |
      | "SKU-LAPTOP"   |
      | "SKU-HEADSET"  |
      | "SKU-MISSING"  |

  # ------------------------------------------------------------------ #
  #  Move does not affect other wishlisted products                      #
  # ------------------------------------------------------------------ #

  @positive @business
  Scenario Outline: Moving one product leaves other wishlist items unaffected
    Given product "SKU-BOOK" is already in the wishlist
    And product "SKU-MUG" is already in the wishlist
    And product "SKU-LAPTOP" is already in the wishlist
    When I move product <moved> from the wishlist to the cart
    Then the operation succeeds
    And the moved product <moved> is now in the cart
    And the moved product <moved> is no longer in the wishlist
    And the wishlist has 2 items
    And the wishlist contains product <remains1>
    And the wishlist contains product <remains2>

    Examples:
      | moved         | remains1      | remains2      |
      | "SKU-BOOK"    | "SKU-MUG"     | "SKU-LAPTOP"  |
      | "SKU-MUG"     | "SKU-BOOK"    | "SKU-LAPTOP"  |
      | "SKU-LAPTOP"  | "SKU-BOOK"    | "SKU-MUG"     |

  # ------------------------------------------------------------------ #
  #  Re-add after move                                                   #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: A product moved to the cart can be re-added to the wishlist
    Given product <sku> is already in the wishlist
    When I move product <sku> from the wishlist to the cart
    Then the operation succeeds
    When I add product <sku> to the wishlist
    Then the operation succeeds
    And the wishlist contains product <sku>
    And the cart contains product <sku>

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |

  # ------------------------------------------------------------------ #
  #  Cart and wishlist independence                                      #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Adding a product to the cart does not remove it from the wishlist
    Given product <sku> is already in the wishlist
    When I add product <sku> to the cart with quantity 1
    Then no domain error is raised
    And the wishlist contains product <sku>
    And the cart contains product <sku>

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |

  # ------------------------------------------------------------------ #
  #  Move non-wishlisted product raises WISH_UNKNOWN_PRODUCT             #
  # ------------------------------------------------------------------ #

  @negative @business @regression
  Scenario Outline: Moving a product not in the wishlist raises WISH_UNKNOWN_PRODUCT
    When I move product <sku> from the wishlist to the cart
    Then a domain error "WISH_UNKNOWN_PRODUCT" is raised

    Examples:
      | sku           |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
