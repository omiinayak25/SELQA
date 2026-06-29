@domain @commerce @wishlist
Feature: Wishlist
  As a commerce platform
  I want to manage a customer's wishlist with real business rules
  So that saved products can be retrieved, deduplicated, and moved to the cart

  Background:
    Given a clean wishlist, cart and product registry

  # ------------------------------------------------------------------ #
  #  Positive — happy path                                              #
  # ------------------------------------------------------------------ #

  @smoke @positive
  Scenario: Adding a product to the wishlist succeeds
    When I add product "SKU-BOOK" to the wishlist
    Then the operation succeeds
    And the wishlist contains product "SKU-BOOK"
    And the wishlist has 1 items

  @positive
  Scenario: Adding multiple distinct products to the wishlist succeeds
    When I add product "SKU-BOOK" to the wishlist
    And I add product "SKU-MUG" to the wishlist
    And I add product "SKU-LAPTOP" to the wishlist
    Then no domain error is raised
    And the wishlist has 3 items

  @positive @business
  Scenario: Removing a product from the wishlist leaves others intact
    Given product "SKU-BOOK" is already in the wishlist
    And product "SKU-MUG" is already in the wishlist
    When I remove product "SKU-BOOK" from the wishlist
    Then the operation succeeds
    And the wishlist does not contain product "SKU-BOOK"
    And the wishlist contains product "SKU-MUG"
    And the wishlist has 1 items

  @positive @business
  Scenario: Removing a product that is not in the wishlist is a no-op
    Given product "SKU-BOOK" is already in the wishlist
    When I remove product "SKU-LAPTOP" from the wishlist
    Then the operation succeeds
    And the wishlist has 1 items

  @positive @business
  Scenario: Moving a product from the wishlist adds it to the cart and removes it from the wishlist
    Given product "SKU-BOOK" is already in the wishlist
    When I move product "SKU-BOOK" from the wishlist to the cart
    Then the operation succeeds
    And the moved product "SKU-BOOK" is now in the cart
    And the moved product "SKU-BOOK" is no longer in the wishlist
    And the wishlist is empty

  @positive @business
  Scenario: Wishlist and cart are independent - wishlist item stays after cart add
    Given product "SKU-MUG" is already in the wishlist
    When I add product "SKU-MUG" to the cart with quantity 2
    Then no domain error is raised
    And the wishlist contains product "SKU-MUG"

  @positive @business
  Scenario: Cart holds moved item with quantity 1 after move-to-cart
    Given product "SKU-LAPTOP" is already in the wishlist
    When I move product "SKU-LAPTOP" from the wishlist to the cart
    Then the operation succeeds
    And the cart line for product "SKU-LAPTOP" has quantity 1
    And the cart subtotal is "45.00"

  @positive @business
  Scenario: Moving product from wishlist to cart decrements wishlist size correctly
    Given product "SKU-BOOK" is already in the wishlist
    And product "SKU-MUG" is already in the wishlist
    When I move product "SKU-BOOK" from the wishlist to the cart
    Then the operation succeeds
    And the wishlist has 1 items
    And the wishlist contains product "SKU-MUG"

  # ------------------------------------------------------------------ #
  #  Negative — error codes                                             #
  # ------------------------------------------------------------------ #

  @negative
  Scenario: Adding an unknown product to the wishlist raises WISH_UNKNOWN_PRODUCT
    When I add product "SKU-GHOST" to the wishlist
    Then a domain error "WISH_UNKNOWN_PRODUCT" is raised

  @negative @business
  Scenario: Adding a duplicate product to the wishlist raises WISH_DUPLICATE
    Given product "SKU-BOOK" is already in the wishlist
    When I add product "SKU-BOOK" to the wishlist
    Then a domain error "WISH_DUPLICATE" is raised
    And the wishlist has 1 items

  @negative @boundary
  Scenario: Moving an out-of-stock product from wishlist to cart raises CART_OUT_OF_STOCK
    Given product "SKU-ZERO" is already in the wishlist
    When I move product "SKU-ZERO" from the wishlist to the cart
    Then a domain error "CART_OUT_OF_STOCK" is raised
    And the wishlist contains product "SKU-ZERO"

  @negative @business
  Scenario: Moving a product not in the wishlist raises WISH_UNKNOWN_PRODUCT
    When I move product "SKU-BOOK" from the wishlist to the cart
    Then a domain error "WISH_UNKNOWN_PRODUCT" is raised

  @negative @boundary
  Scenario: Moving a product whose stock drops to zero after seeding raises CART_OUT_OF_STOCK
    Given product "SKU-RARE" is already in the wishlist
    And product "SKU-RARE" has 0 units remaining in stock
    When I move product "SKU-RARE" from the wishlist to the cart
    Then a domain error "CART_OUT_OF_STOCK" is raised
    And the wishlist contains product "SKU-RARE"

  # ------------------------------------------------------------------ #
  #  Scenario Outline — add / existence assertions                       #
  # ------------------------------------------------------------------ #

  @positive @regression
  Scenario Outline: Each known product can be independently added to the wishlist
    When I add product <product_id> to the wishlist
    Then no domain error is raised
    And the wishlist contains product <product_id>

    Examples:
      | product_id    |
      | "SKU-BOOK"    |
      | "SKU-MUG"     |
      | "SKU-LAPTOP"  |
      | "SKU-HEADSET" |
      | "SKU-CHAIR"   |
      | "SKU-RARE"    |
      | "SKU-ZERO"    |

  @negative @regression
  Scenario Outline: Unknown product IDs are rejected with WISH_UNKNOWN_PRODUCT
    When I add product <product_id> to the wishlist
    Then a domain error "WISH_UNKNOWN_PRODUCT" is raised

    Examples:
      | product_id     |
      | "SKU-MISSING"  |
      | "SKU-NONEXIST" |

  # ------------------------------------------------------------------ #
  #  Business rule — move-to-cart, then re-add to wishlist              #
  # ------------------------------------------------------------------ #

  @positive @business
  Scenario: A product moved to cart can be re-added to the wishlist
    Given product "SKU-MUG" is already in the wishlist
    When I move product "SKU-MUG" from the wishlist to the cart
    Then the operation succeeds
    When I add product "SKU-MUG" to the wishlist
    Then the operation succeeds
    And the wishlist contains product "SKU-MUG"
    And the cart contains product "SKU-MUG"

  @positive @boundary
  Scenario: Move-to-cart for a product with exactly one unit in stock succeeds
    Given product "SKU-RARE" is already in the wishlist
    When I move product "SKU-RARE" from the wishlist to the cart
    Then the operation succeeds
    And the moved product "SKU-RARE" is now in the cart
    And the moved product "SKU-RARE" is no longer in the wishlist
