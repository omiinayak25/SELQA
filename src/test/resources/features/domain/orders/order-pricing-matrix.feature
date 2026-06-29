@domain @orders @pricing
Feature: Order Pricing Matrix — Tax, Shipping and Coupon Combinations
  As the orders platform
  I want every (subtotal, coupon) combination to produce exact tax, shipping, discount and total values
  So that no rounding error or boundary mistake reaches production

  # Rules (from CheckoutService):
  #   tax      = subtotal * 0.08, rounded HALF_UP to 2 d.p.
  #   shipping = 0.00 when subtotal >= 50.00, else 5.99
  #   SAVE10   = subtotal * 0.10, rounded HALF_UP to 2 d.p.
  #   FLAT5    = min(5.00, subtotal), exact
  #   NONE / unknown coupon = 0.00 discount
  #   total    = subtotal + tax + shipping - discount, floor 0, HALF_UP 2 d.p.

  Background:
    Given a clean checkout service

  # ---------------------------------------------------------------------------
  # No-coupon full breakdown — free-shipping boundary and beyond
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario Outline: No-coupon pricing is exact across a wide subtotal range
    Given I add a domain cart item "<item>" priced "<unitPrice>" qty <qty>
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "NONE"
    Then  no domain error is raised
    And   the order subtotal is "<subtotal>"
    And   the order tax is "<tax>"
    And   the order shipping is "<shipping>"
    And   the order discount is "0.00"
    And   the order total is "<total>"

    Examples:
      | item         | unitPrice | qty | subtotal | tax   | shipping | total   |
      | Penny Widget |  0.01     |   1 |  0.01    |  0.00 |  5.99    |   6.00  |
      | Budget A     |  1.00     |   1 |  1.00    |  0.08 |  5.99    |   7.07  |
      | Budget B     |  5.00     |   1 |  5.00    |  0.40 |  5.99    |  11.39  |
      | Mid A        | 12.50     |   1 | 12.50    |  1.00 |  5.99    |  19.49  |
      | Mid B        | 25.00     |   1 | 25.00    |  2.00 |  5.99    |  32.99  |
      | Under thresh | 49.99     |   1 | 49.99    |  4.00 |  5.99    |  59.98  |
      | Exact thresh | 50.00     |   1 | 50.00    |  4.00 |  0.00    |  54.00  |
      | Above thresh | 50.01     |   1 | 50.01    |  4.00 |  0.00    |  54.01  |
      | Mid-high     | 75.00     |   1 | 75.00    |  6.00 |  0.00    |  81.00  |
      | High A       | 99.99     |   1 | 99.99    |  8.00 |  0.00    | 107.99  |
      | High B       |100.00     |   1 |100.00    |  8.00 |  0.00    | 108.00  |
      | High C       |150.00     |   1 |150.00    | 12.00 |  0.00    | 162.00  |
      | Multi low    | 10.00     |   4 | 40.00    |  3.20 |  5.99    |  49.19  |
      | Multi thresh | 10.00     |   5 | 50.00    |  4.00 |  0.00    |  54.00  |
      | Multi high   | 20.00     |   5 |100.00    |  8.00 |  0.00    | 108.00  |
      | Multi hih2   | 33.34     |   3 |100.02    |  8.00 |  0.00    | 108.02  |

  # ---------------------------------------------------------------------------
  # SAVE10 coupon — 10 % off subtotal, free-shipping boundary included
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario Outline: SAVE10 coupon produces correct discount, tax and total across subtotals
    Given I add a domain cart item "<item>" priced "<unitPrice>" qty <qty>
    When  I perform a domain checkout with address "VALID" payment "PAYPAL" coupon "SAVE10"
    Then  no domain error is raised
    And   the order subtotal is "<subtotal>"
    And   the order discount is "<discount>"
    And   the order tax is "<tax>"
    And   the order shipping is "<shipping>"
    And   the order total is "<total>"

    Examples:
      | item        | unitPrice | qty | subtotal | discount | tax  | shipping | total  |
      | Save A      | 10.00     |   1 | 10.00    |  1.00    | 0.80 |  5.99    | 15.79  |
      | Save B      | 20.00     |   1 | 20.00    |  2.00    | 1.60 |  5.99    | 25.59  |
      | Save C      | 30.00     |   1 | 30.00    |  3.00    | 2.40 |  5.99    | 35.39  |
      | Save D      | 49.99     |   1 | 49.99    |  5.00    | 4.00 |  5.99    | 54.98  |
      | Save Thresh | 50.00     |   1 | 50.00    |  5.00    | 4.00 |  0.00    | 49.00  |
      | Save E      | 60.00     |   1 | 60.00    |  6.00    | 4.80 |  0.00    | 58.80  |
      | Save F      | 80.00     |   1 | 80.00    |  8.00    | 6.40 |  0.00    | 78.40  |
      | Save G      |100.00     |   1 |100.00    | 10.00    | 8.00 |  0.00    | 98.00  |
      | Save H      |200.00     |   1 |200.00    | 20.00    |16.00 |  0.00    |196.00  |
      | Save Multi  | 25.00     |   4 |100.00    | 10.00    | 8.00 |  0.00    | 98.00  |

  # ---------------------------------------------------------------------------
  # FLAT5 coupon — $5 flat off, capped at subtotal when subtotal < $5
  # ---------------------------------------------------------------------------

  @business @regression @boundary
  Scenario Outline: FLAT5 coupon deducts exactly five dollars or is capped at subtotal
    Given I add a domain cart item "<item>" priced "<unitPrice>" qty <qty>
    When  I perform a domain checkout with address "VALID" payment "APPLE_PAY" coupon "FLAT5"
    Then  no domain error is raised
    And   the order subtotal is "<subtotal>"
    And   the order discount is "<discount>"
    And   the order tax is "<tax>"
    And   the order shipping is "<shipping>"
    And   the order total is "<total>"

    Examples:
      | item          | unitPrice | qty | subtotal | discount | tax  | shipping | total  |
      | Tiny          |  1.00     |   1 |  1.00    |  1.00    | 0.08 |  5.99    |  6.07  |
      | Below Five    |  3.50     |   1 |  3.50    |  3.50    | 0.28 |  5.99    |  6.27  |
      | Exact Five    |  5.00     |   1 |  5.00    |  5.00    | 0.40 |  5.99    |  6.39  |
      | Above Five    |  6.00     |   1 |  6.00    |  5.00    | 0.48 |  5.99    |  7.47  |
      | Mid Flat5     | 30.00     |   1 | 30.00    |  5.00    | 2.40 |  5.99    | 33.39  |
      | Flat Thresh   | 50.00     |   1 | 50.00    |  5.00    | 4.00 |  0.00    | 49.00  |
      | Flat High     |100.00     |   1 |100.00    |  5.00    | 8.00 |  0.00    |103.00  |

  # ---------------------------------------------------------------------------
  # Unknown / null coupon — zero discount (silent ignore)
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario Outline: An unrecognised coupon is silently ignored and produces zero discount
    Given I add a domain cart item "<item>" priced "<unitPrice>" qty 1
    When  I perform a domain checkout with address "VALID" payment "BANK_TRANSFER" coupon "<coupon>"
    Then  no domain error is raised
    And   the order discount is "0.00"
    And   the order total is "<total>"

    Examples:
      | item       | unitPrice | coupon    | total   |
      | Ignored A  | 20.00     | BOGUS     | 27.59   |
      | Ignored B  | 50.00     | XMAS50    | 54.00   |
      | Ignored C  | 10.00     | SAVE10X   | 16.79   |
      | Ignored D  | 30.00     | FLAT5X    | 38.39   |
      | Ignored E  | 75.00     | 10OFF     | 81.00   |
