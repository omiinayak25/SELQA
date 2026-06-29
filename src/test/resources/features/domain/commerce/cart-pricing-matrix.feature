@domain @commerce @cart
Feature: Cart Pricing Matrix — qty/coupon combinations to subtotal/total
  As the commerce platform
  I want to verify that every combination of products, quantities, and coupons
  produces the mathematically correct subtotal and total
  So that customers are always charged the right amount

  Background:
    Given a clean cart and product registry

  # ------------------------------------------------------------------ #
  #  Subtotal matrix — single SKU, varying quantity                      #
  #  Prices: SKU-BOOK=19.99, SKU-MUG=8.50, SKU-LAPTOP=45.00            #
  #          SKU-HEADSET=149.99, SKU-CHAIR=299.00, SKU-RARE=9.99        #
  # ------------------------------------------------------------------ #

  @positive @business @regression @boundary
  Scenario Outline: Single-product subtotal equals unit-price times quantity
    Given I have already added <qty> units of product <sku> to the cart
    Then the cart subtotal is "<subtotal>"
    And the cart total is "<subtotal>"

    Examples:
      | qty | sku           | subtotal |
      | 1   | "SKU-BOOK"    | 19.99    |
      | 2   | "SKU-BOOK"    | 39.98    |
      | 5   | "SKU-BOOK"    | 99.95    |
      | 10  | "SKU-BOOK"    | 199.90   |
      | 1   | "SKU-MUG"     | 8.50     |
      | 3   | "SKU-MUG"     | 25.50    |
      | 10  | "SKU-MUG"     | 85.00    |
      | 1   | "SKU-LAPTOP"  | 45.00    |
      | 2   | "SKU-LAPTOP"  | 90.00    |
      | 5   | "SKU-LAPTOP"  | 225.00   |
      | 1   | "SKU-HEADSET" | 149.99   |
      | 2   | "SKU-HEADSET" | 299.98   |
      | 3   | "SKU-HEADSET" | 449.97   |
      | 1   | "SKU-CHAIR"   | 299.00   |
      | 2   | "SKU-CHAIR"   | 598.00   |
      | 1   | "SKU-RARE"    | 9.99     |

  # ------------------------------------------------------------------ #
  #  Multi-SKU subtotals: two-product combinations                       #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Multi-product subtotal is the sum of each line total
    Given I have already added <qty1> units of product <sku1> to the cart
    And I have already added <qty2> units of product <sku2> to the cart
    Then the cart subtotal is "<subtotal>"
    And the cart has <lines> distinct lines
    And the cart has <items> items

    Examples:
      | qty1 | sku1          | qty2 | sku2          | subtotal | lines | items |
      | 1    | "SKU-BOOK"    | 1    | "SKU-MUG"     | 28.49    | 2     | 2     |
      | 2    | "SKU-BOOK"    | 1    | "SKU-MUG"     | 48.48    | 2     | 3     |
      | 1    | "SKU-BOOK"    | 5    | "SKU-MUG"     | 62.49    | 2     | 6     |
      | 1    | "SKU-LAPTOP"  | 1    | "SKU-BOOK"    | 64.99    | 2     | 2     |
      | 2    | "SKU-LAPTOP"  | 3    | "SKU-MUG"     | 115.50   | 2     | 5     |
      | 1    | "SKU-HEADSET" | 1    | "SKU-LAPTOP"  | 194.99   | 2     | 2     |
      | 1    | "SKU-CHAIR"   | 1    | "SKU-LAPTOP"  | 344.00   | 2     | 2     |
      | 2    | "SKU-HEADSET" | 2    | "SKU-CHAIR"   | 897.98   | 2     | 4     |
      | 1    | "SKU-RARE"    | 1    | "SKU-MUG"     | 18.49    | 2     | 2     |

  # ------------------------------------------------------------------ #
  #  Coupon + single-product totals                                      #
  #  SAVE10=10%, SAVE20=20%, HALFOFF=50%, FREESHIP=0%                   #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Applying a coupon to a single-product cart produces the correct discounted total
    Given I have already added <qty> units of product <sku> to the cart
    When I apply coupon code "<coupon>" to the cart
    Then no domain error is raised
    And the active coupon is "<coupon>"
    And the cart subtotal is "<subtotal>"
    And the cart total is "<total>"

    Examples:
      | qty | sku           | coupon   | subtotal | total  |
      | 1   | "SKU-BOOK"    | SAVE10   | 19.99    | 17.99  |
      | 2   | "SKU-BOOK"    | SAVE10   | 39.98    | 35.98  |
      | 5   | "SKU-BOOK"    | SAVE10   | 99.95    | 89.96  |
      | 1   | "SKU-BOOK"    | SAVE20   | 19.99    | 15.99  |
      | 2   | "SKU-BOOK"    | SAVE20   | 39.98    | 31.98  |
      | 1   | "SKU-BOOK"    | HALFOFF  | 19.99    | 10.00  |
      | 2   | "SKU-BOOK"    | HALFOFF  | 39.98    | 19.99  |
      | 1   | "SKU-BOOK"    | FREESHIP | 19.99    | 19.99  |
      | 1   | "SKU-MUG"     | SAVE10   | 8.50     | 7.65   |
      | 3   | "SKU-MUG"     | SAVE10   | 25.50    | 22.95  |
      | 1   | "SKU-MUG"     | SAVE20   | 8.50     | 6.80   |
      | 10  | "SKU-MUG"     | SAVE20   | 85.00    | 68.00  |
      | 1   | "SKU-MUG"     | HALFOFF  | 8.50     | 4.25   |
      | 1   | "SKU-LAPTOP"  | SAVE10   | 45.00    | 40.50  |
      | 1   | "SKU-LAPTOP"  | SAVE20   | 45.00    | 36.00  |
      | 1   | "SKU-LAPTOP"  | HALFOFF  | 45.00    | 22.50  |
      | 1   | "SKU-LAPTOP"  | FREESHIP | 45.00    | 45.00  |
      | 1   | "SKU-HEADSET" | SAVE10   | 149.99   | 134.99 |
      | 1   | "SKU-HEADSET" | SAVE20   | 149.99   | 119.99 |
      | 1   | "SKU-HEADSET" | HALFOFF  | 149.99   | 75.00  |
      | 2   | "SKU-HEADSET" | SAVE10   | 299.98   | 269.98 |
      | 1   | "SKU-CHAIR"   | SAVE10   | 299.00   | 269.10 |
      | 1   | "SKU-CHAIR"   | SAVE20   | 299.00   | 239.20 |
      | 1   | "SKU-CHAIR"   | HALFOFF  | 299.00   | 149.50 |

  # ------------------------------------------------------------------ #
  #  Coupon + multi-product totals                                       #
  # ------------------------------------------------------------------ #

  @positive @business @regression
  Scenario Outline: Applying a coupon to a multi-product cart discounts the combined subtotal
    Given I have already added <qty1> units of product <sku1> to the cart
    And I have already added <qty2> units of product <sku2> to the cart
    When I apply coupon code "<coupon>" to the cart
    Then no domain error is raised
    And the cart subtotal is "<subtotal>"
    And the cart total is "<total>"

    Examples:
      | qty1 | sku1          | qty2 | sku2         | coupon   | subtotal | total  |
      | 2    | "SKU-BOOK"    | 1    | "SKU-MUG"    | SAVE10   | 48.48    | 43.63  |
      | 2    | "SKU-BOOK"    | 1    | "SKU-MUG"    | SAVE20   | 48.48    | 38.78  |
      | 2    | "SKU-BOOK"    | 1    | "SKU-MUG"    | HALFOFF  | 48.48    | 24.24  |
      | 1    | "SKU-LAPTOP"  | 1    | "SKU-BOOK"   | SAVE10   | 64.99    | 58.49  |
      | 1    | "SKU-LAPTOP"  | 1    | "SKU-BOOK"   | SAVE20   | 64.99    | 51.99  |
      | 1    | "SKU-LAPTOP"  | 1    | "SKU-BOOK"   | HALFOFF  | 64.99    | 32.50  |
      | 1    | "SKU-HEADSET" | 1    | "SKU-LAPTOP" | SAVE10   | 194.99   | 175.49 |
      | 1    | "SKU-HEADSET" | 1    | "SKU-LAPTOP" | HALFOFF  | 194.99   | 97.50  |

  # ------------------------------------------------------------------ #
  #  Update-quantity then assert subtotal                                #
  # ------------------------------------------------------------------ #

  @positive @business @boundary
  Scenario Outline: Updating cart quantity adjusts the subtotal correctly
    Given I have already added 1 units of product <sku> to the cart
    When I update product <sku> quantity in the cart to <new_qty>
    Then the operation succeeds
    And the cart line for product <sku> has quantity <new_qty>
    And the cart subtotal is "<subtotal>"

    Examples:
      | sku           | new_qty | subtotal |
      | "SKU-BOOK"    | 3       | 59.97    |
      | "SKU-BOOK"    | 10      | 199.90   |
      | "SKU-MUG"     | 5       | 42.50    |
      | "SKU-MUG"     | 25      | 212.50   |
      | "SKU-LAPTOP"  | 2       | 90.00    |
      | "SKU-LAPTOP"  | 5       | 225.00   |
      | "SKU-HEADSET" | 2       | 299.98   |
      | "SKU-HEADSET" | 3       | 449.97   |
      | "SKU-CHAIR"   | 2       | 598.00   |

  # ------------------------------------------------------------------ #
  #  Invalid coupon codes — always raises CART_BAD_COUPON                #
  # ------------------------------------------------------------------ #

  @negative @validation @boundary
  Scenario Outline: Invalid coupon codes raise CART_BAD_COUPON
    When I apply coupon code "<coupon>" to the cart
    Then a domain error "CART_BAD_COUPON" is raised

    Examples:
      | coupon     |
      | FAKE99     |
      | SAVE50     |
      | BOGUS      |
      | FREE       |
      | DISCOUNT   |
      | 10OFF      |
      | PERCENT20  |
      |            |

  # ------------------------------------------------------------------ #
  #  Stock boundary: exactly at limit succeeds, one-over fails           #
  # ------------------------------------------------------------------ #

  @positive @boundary
  Scenario Outline: Adding exactly the available quantity succeeds and records correct subtotal
    When I add product <sku> to the cart with quantity <available>
    Then no domain error is raised
    And the cart subtotal is "<subtotal>"

    Examples:
      | sku           | available | subtotal |
      | "SKU-BOOK"    | 10        | 199.90   |
      | "SKU-MUG"     | 25        | 212.50   |
      | "SKU-LAPTOP"  | 5         | 225.00   |
      | "SKU-HEADSET" | 3         | 449.97   |
      | "SKU-CHAIR"   | 2         | 598.00   |
      | "SKU-RARE"    | 1         | 9.99     |

  @negative @boundary
  Scenario Outline: Adding one more than available stock raises CART_OUT_OF_STOCK
    When I add product <sku> to the cart with quantity <over>
    Then a domain error "CART_OUT_OF_STOCK" is raised

    Examples:
      | sku           | over |
      | "SKU-BOOK"    | 11   |
      | "SKU-MUG"     | 26   |
      | "SKU-LAPTOP"  | 6    |
      | "SKU-HEADSET" | 4    |
      | "SKU-CHAIR"   | 3    |
      | "SKU-RARE"    | 2    |
      | "SKU-ZERO"    | 1    |
