@domain @commerce @cart
Feature: Shopping Cart
  As a commerce platform
  I want to manage a customer's shopping cart with real business rules
  So that items, quantities, stock limits and discounts are correctly enforced

  Background:
    Given a clean cart and product registry

  # ------------------------------------------------------------------ #
  #  Positive — happy path                                              #
  # ------------------------------------------------------------------ #

  @smoke @positive
  Scenario: Adding a single product creates one cart line
    When I add product "SKU-BOOK" to the cart with quantity 1
    Then the operation succeeds
    And the cart has 1 items
    And the cart has 1 distinct lines
    And the cart contains product "SKU-BOOK"

  @positive
  Scenario: Adding two different products creates two distinct lines
    When I add product "SKU-BOOK" to the cart with quantity 2
    And I add product "SKU-MUG" to the cart with quantity 3
    Then no domain error is raised
    And the cart has 5 items
    And the cart has 2 distinct lines

  @positive @business
  Scenario: Adding the same product twice merges into one line
    When I add product "SKU-BOOK" to the cart with quantity 1
    And I add product "SKU-BOOK" to the cart with quantity 2
    Then no domain error is raised
    And the cart has 1 distinct lines
    And the cart line for product "SKU-BOOK" has quantity 3

  @positive
  Scenario: Cart subtotal is sum of unit-price times quantity for each line
    When I add product "SKU-BOOK" to the cart with quantity 2
    And I add product "SKU-MUG" to the cart with quantity 1
    Then no domain error is raised
    And the cart subtotal is "48.48"

  @positive @business
  Scenario: Removing a product leaves the remaining lines intact
    Given I have already added 2 units of product "SKU-BOOK" to the cart
    And I have already added 1 units of product "SKU-MUG" to the cart
    When I remove product "SKU-BOOK" from the cart
    Then the operation succeeds
    And the cart does not contain product "SKU-BOOK"
    And the cart contains product "SKU-MUG"
    And the cart has 1 distinct lines

  @positive
  Scenario: Clearing the cart removes all items
    Given I have already added 3 units of product "SKU-BOOK" to the cart
    And I have already added 2 units of product "SKU-MUG" to the cart
    When I clear the cart
    Then the operation succeeds
    And the cart is empty

  @positive @business
  Scenario: Updating quantity to a new valid value is reflected in the subtotal
    Given I have already added 1 units of product "SKU-LAPTOP" to the cart
    When I update product "SKU-LAPTOP" quantity in the cart to 3
    Then the operation succeeds
    And the cart line for product "SKU-LAPTOP" has quantity 3
    And the cart subtotal is "135.00"

  @positive @business
  Scenario: SAVE10 coupon reduces the total by exactly 10 percent
    Given I have already added 2 units of product "SKU-BOOK" to the cart
    When I apply coupon code "SAVE10" to the cart
    Then the operation succeeds
    And the active coupon is "SAVE10"
    And the cart subtotal is "39.98"
    And the cart total is "35.98"

  @positive @business
  Scenario: HALFOFF coupon halves the cart total
    Given I have already added 1 units of product "SKU-LAPTOP" to the cart
    When I apply coupon code "HALFOFF" to the cart
    Then the operation succeeds
    And the cart subtotal is "45.00"
    And the cart total is "22.50"

  # ------------------------------------------------------------------ #
  #  Scenario Outline — coupon math across all valid codes              #
  # ------------------------------------------------------------------ #

  @positive @business
  Scenario Outline: Applying a valid coupon produces the correct discounted total
    Given I have already added <qty> units of product <sku> to the cart
    When I apply coupon code <coupon> to the cart
    Then no domain error is raised
    And the cart total is <expected_total>

    Examples:
      | qty | sku           | coupon      | expected_total |
      | 1   | "SKU-BOOK"    | "SAVE10"    | "17.99"        |
      | 2   | "SKU-MUG"     | "SAVE20"    | "13.60"        |
      | 1   | "SKU-HEADSET" | "HALFOFF"   | "75.00"        |
      | 1   | "SKU-LAPTOP"  | "FREESHIP"  | "45.00"        |

  # ------------------------------------------------------------------ #
  #  Scenario Outline — quantity boundary cases                          #
  # ------------------------------------------------------------------ #

  @negative @boundary @validation
  Scenario Outline: Adding a non-positive quantity raises CART_BAD_QTY
    When I add product "SKU-BOOK" to the cart with quantity <qty>
    Then a domain error "CART_BAD_QTY" is raised

    Examples:
      | qty |
      | 0   |
      | -1  |
      | -99 |

  @negative @boundary
  Scenario Outline: Updating to a non-positive quantity raises CART_BAD_QTY
    Given I have already added 1 units of product "SKU-MUG" to the cart
    When I update product "SKU-MUG" quantity in the cart to <qty>
    Then a domain error "CART_BAD_QTY" is raised

    Examples:
      | qty |
      | 0   |
      | -5  |

  # ------------------------------------------------------------------ #
  #  Negative — error codes                                             #
  # ------------------------------------------------------------------ #

  @negative
  Scenario: Adding an unknown product raises CART_UNKNOWN_PRODUCT
    When I add product "SKU-GHOST" to the cart with quantity 1
    Then a domain error "CART_UNKNOWN_PRODUCT" is raised

  @negative @boundary
  Scenario: Adding more than available stock raises CART_OUT_OF_STOCK
    When I add product "SKU-RARE" to the cart with quantity 2
    Then a domain error "CART_OUT_OF_STOCK" is raised
    And the domain error message contains "1"

  @negative @boundary
  Scenario: Requesting any quantity of an out-of-stock product raises CART_OUT_OF_STOCK
    When I add product "SKU-ZERO" to the cart with quantity 1
    Then a domain error "CART_OUT_OF_STOCK" is raised

  @negative @business
  Scenario: Merging quantities beyond stock raises CART_OUT_OF_STOCK
    Given I have already added 1 units of product "SKU-RARE" to the cart
    When I add product "SKU-RARE" to the cart with quantity 1
    Then a domain error "CART_OUT_OF_STOCK" is raised

  @negative
  Scenario: An unknown coupon code raises CART_BAD_COUPON
    When I apply coupon code "FAKE99" to the cart
    Then a domain error "CART_BAD_COUPON" is raised

  @negative @boundary
  Scenario: A blank coupon code raises CART_BAD_COUPON
    When I apply coupon code "" to the cart
    Then a domain error "CART_BAD_COUPON" is raised

  @negative
  Scenario: Updating a product that is not in the cart raises CART_UNKNOWN_PRODUCT
    When I update product "SKU-BOOK" quantity in the cart to 2
    Then a domain error "CART_UNKNOWN_PRODUCT" is raised

  @negative @boundary
  Scenario Outline: Adding beyond stock limit at exact boundary raises CART_OUT_OF_STOCK
    When I add product <sku> to the cart with quantity <requested>
    Then a domain error "CART_OUT_OF_STOCK" is raised

    Examples:
      | sku          | requested |
      | "SKU-CHAIR"  | 3         |
      | "SKU-RARE"   | 2         |
      | "SKU-LAPTOP" | 6         |

  # ------------------------------------------------------------------ #
  #  Boundary — exactly at stock limit succeeds                         #
  # ------------------------------------------------------------------ #

  @positive @boundary
  Scenario Outline: Adding exactly the available stock quantity succeeds
    When I add product <sku> to the cart with quantity <available>
    Then no domain error is raised
    And the cart has <available> items

    Examples:
      | sku          | available |
      | "SKU-CHAIR"  | 2         |
      | "SKU-RARE"   | 1         |
      | "SKU-LAPTOP" | 5         |
