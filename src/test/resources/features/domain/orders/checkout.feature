@domain @orders @checkout
Feature: Domain Checkout
  As the orders platform
  I want to validate carts, addresses and payment methods, then compute tax, shipping and discounts
  So that every order total is accurate and only valid checkouts are accepted

  Background:
    Given a clean checkout service

  # ---------------------------------------------------------------------------
  # Positive — happy path
  # ---------------------------------------------------------------------------

  @smoke @positive
  Scenario: A cart below the free-shipping threshold incurs a flat shipping charge
    Given I add a domain cart item "Widget A" priced "20.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "NONE"
    Then  no domain error is raised
    And   the order status is "CREATED"
    And   the order subtotal is "20.00"
    And   the order tax is "1.60"
    And   the order shipping is "5.99"
    And   the order discount is "0.00"
    And   the order total is "27.59"

  @positive
  Scenario: A cart at exactly the free-shipping threshold gets free shipping
    Given I add a domain cart item "Product X" priced "50.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "PAYPAL" coupon "NONE"
    Then  no domain error is raised
    And   the order shipping is "0.00"
    And   the order tax is "4.00"
    And   the order total is "54.00"

  @positive
  Scenario: A cart above the free-shipping threshold gets free shipping
    Given I add a domain cart item "Expensive Item" priced "100.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "APPLE_PAY" coupon "NONE"
    Then  no domain error is raised
    And   the order shipping is "0.00"
    And   the order subtotal is "100.00"
    And   the order tax is "8.00"
    And   the order total is "108.00"

  @positive
  Scenario: Multiple line items produce a correct combined subtotal
    Given I add domain cart items:
      | name      | price | qty |
      | Gadget    | 15.00 | 2   |
      | Cable     | 5.00  | 3   |
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "NONE"
    Then  no domain error is raised
    And   the order subtotal is "45.00"
    And   the order tax is "3.60"
    And   the order shipping is "5.99"
    And   the order total is "54.59"

  @positive
  Scenario: SAVE10 coupon applies a 10 percent discount on the subtotal
    Given I add a domain cart item "Big Ticket" priced "80.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "SAVE10"
    Then  no domain error is raised
    And   the order subtotal is "80.00"
    And   the order discount is "8.00"
    And   the order shipping is "0.00"
    And   the order tax is "6.40"
    And   the order total is "78.40"

  @positive
  Scenario: FLAT5 coupon deducts a flat five dollars from the subtotal
    Given I add a domain cart item "Standard Item" priced "30.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "BANK_TRANSFER" coupon "FLAT5"
    Then  no domain error is raised
    And   the order subtotal is "30.00"
    And   the order discount is "5.00"
    And   the order shipping is "5.99"
    And   the order tax is "2.40"
    And   the order total is "33.39"

  @positive
  Scenario: An unknown coupon code is silently ignored — zero discount applied
    Given I add a domain cart item "Widget B" priced "25.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "INVALID99"
    Then  no domain error is raised
    And   the order discount is "0.00"
    And   the order total is "32.99"

  @positive
  Scenario: All accepted payment methods complete checkout successfully
    Given I add a domain cart item "Sample" priced "10.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "APPLE_PAY" coupon "NONE"
    Then  no domain error is raised
    And   the order status is "CREATED"

  # ---------------------------------------------------------------------------
  # Negative — validation failures
  # ---------------------------------------------------------------------------

  @negative @validation
  Scenario: Checkout with an empty cart raises CHK_EMPTY_CART
    When I perform a domain checkout with an empty cart and address "VALID" payment "CREDIT_CARD"
    Then a domain error "CHK_EMPTY_CART" is raised

  @negative @validation
  Scenario: Checkout with a null address raises CHK_BAD_ADDRESS
    Given I add a domain cart item "Widget C" priced "10.00" qty 1
    When  I perform a domain checkout with no address and payment "CREDIT_CARD"
    Then  a domain error "CHK_BAD_ADDRESS" is raised

  @negative @validation
  Scenario: Checkout with a null payment method raises CHK_BAD_PAYMENT
    Given I add a domain cart item "Widget D" priced "10.00" qty 1
    When  I perform a domain checkout with address "VALID" and no payment method
    Then  a domain error "CHK_BAD_PAYMENT" is raised

  @negative @validation
  Scenario Outline: Checkout with a missing required address field raises CHK_BAD_ADDRESS
    Given I add a domain cart item "Widget E" priced "10.00" qty 1
    When  I perform a domain checkout with incomplete address missing "<field>" and payment "CREDIT_CARD"
    Then  a domain error "CHK_BAD_ADDRESS" is raised

    Examples:
      | field         |
      | recipientName |
      | street        |
      | city          |
      | postalCode    |

  # ---------------------------------------------------------------------------
  # Business rules — Scenario Outline: subtotal -> tax / shipping / total math
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario Outline: Tax and shipping compute correctly across the free-shipping boundary
    Given I add a domain cart item "Test Product" priced "<unitPrice>" qty <qty>
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "NONE"
    Then  no domain error is raised
    And   the order subtotal is "<subtotal>"
    And   the order tax is "<tax>"
    And   the order shipping is "<shipping>"
    And   the order total is "<total>"

    Examples:
      | unitPrice | qty | subtotal | tax  | shipping | total  |
      | 10.00     | 1   | 10.00    | 0.80 | 5.99     | 16.79  |
      | 25.00     | 1   | 25.00    | 2.00 | 5.99     | 32.99  |
      | 49.99     | 1   | 49.99    | 4.00 | 5.99     | 59.98  |
      | 50.00     | 1   | 50.00    | 4.00 | 0.00     | 54.00  |
      | 75.00     | 1   | 75.00    | 6.00 | 0.00     | 81.00  |
      | 10.00     | 10  | 100.00   | 8.00 | 0.00     | 108.00 |

  @business @regression
  Scenario Outline: Coupon codes produce correct discount amounts
    Given I add a domain cart item "Outline Item" priced "<unitPrice>" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "<coupon>"
    Then  no domain error is raised
    And   the order discount is "<discount>"

    Examples:
      | unitPrice | coupon  | discount |
      | 100.00    | SAVE10  | 10.00    |
      | 50.00     | SAVE10  | 5.00     |
      | 20.00     | SAVE10  | 2.00     |
      | 100.00    | FLAT5   | 5.00     |
      | 4.00      | FLAT5   | 4.00     |
      | 100.00    | NONE    | 0.00     |

  # ---------------------------------------------------------------------------
  # Boundary — FLAT5 capped at subtotal when subtotal < $5
  # ---------------------------------------------------------------------------

  @boundary
  Scenario: FLAT5 discount is capped at the subtotal when the subtotal is less than five dollars
    Given I add a domain cart item "Cheap Widget" priced "3.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "FLAT5"
    Then  no domain error is raised
    And   the order discount is "3.00"
