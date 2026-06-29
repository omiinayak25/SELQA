@domain @orders @checkout @validation-matrix
Feature: Checkout Validation Matrix — Address, Payment and Cart Field Combinations
  As the orders platform
  I want every invalid field combination to produce the correct error code immediately
  So that bad orders are rejected at the validation gate, not silently accepted

  # Validation order in CheckoutService.checkout():
  #   1. validateCart  -> CHK_EMPTY_CART   (empty / null list)
  #   2. validateAddress -> CHK_BAD_ADDRESS (any of four fields blank / null)
  #   3. validatePayment -> CHK_BAD_PAYMENT (null payment method)

  Background:
    Given a clean checkout service

  # ---------------------------------------------------------------------------
  # Address field matrix — each missing field independently raises CHK_BAD_ADDRESS
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: A single missing address field causes CHK_BAD_ADDRESS regardless of payment
    Given I add a domain cart item "Test Item" priced "20.00" qty 1
    When  I perform a domain checkout with incomplete address missing "<missingField>" and payment "<payment>"
    Then  a domain error "CHK_BAD_ADDRESS" is raised

    Examples:
      | missingField  | payment       |
      | recipientName | CREDIT_CARD   |
      | street        | CREDIT_CARD   |
      | city          | CREDIT_CARD   |
      | postalCode    | CREDIT_CARD   |
      | recipientName | PAYPAL        |
      | street        | PAYPAL        |
      | city          | PAYPAL        |
      | postalCode    | PAYPAL        |
      | recipientName | APPLE_PAY     |
      | street        | APPLE_PAY     |
      | city          | APPLE_PAY     |
      | postalCode    | APPLE_PAY     |
      | recipientName | BANK_TRANSFER |
      | street        | BANK_TRANSFER |
      | city          | BANK_TRANSFER |
      | postalCode    | BANK_TRANSFER |

  # ---------------------------------------------------------------------------
  # Empty-cart validation — cart checked before address or payment
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: Empty-cart checkout raises CHK_EMPTY_CART regardless of payment method
    When I perform a domain checkout with an empty cart and address "VALID" payment "<payment>"
    Then a domain error "CHK_EMPTY_CART" is raised

    Examples:
      | payment       |
      | CREDIT_CARD   |
      | PAYPAL        |
      | APPLE_PAY     |
      | BANK_TRANSFER |

  # ---------------------------------------------------------------------------
  # Null payment raises CHK_BAD_PAYMENT (cart and address valid)
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: A valid cart and address but null payment raises CHK_BAD_PAYMENT
    Given I add a domain cart item "<item>" priced "<price>" qty 1
    When  I perform a domain checkout with address "VALID" and no payment method
    Then  a domain error "CHK_BAD_PAYMENT" is raised

    Examples:
      | item         | price  |
      | Budget Item  | 10.00  |
      | Mid Item     | 30.00  |
      | Premium Item | 100.00 |

  # ---------------------------------------------------------------------------
  # Null address raises CHK_BAD_ADDRESS (cart valid)
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: A valid cart but null address raises CHK_BAD_ADDRESS regardless of payment
    Given I add a domain cart item "Any Item" priced "<price>" qty 1
    When  I perform a domain checkout with no address and payment "<payment>"
    Then  a domain error "CHK_BAD_ADDRESS" is raised

    Examples:
      | price  | payment       |
      | 10.00  | CREDIT_CARD   |
      | 50.00  | PAYPAL        |
      | 100.00 | APPLE_PAY     |

  # ---------------------------------------------------------------------------
  # Happy-path matrix — all four payment methods succeed with a valid cart+address
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: A valid checkout completes for every accepted payment method
    Given I add a domain cart item "Standard Product" priced "<price>" qty 1
    When  I perform a domain checkout with address "VALID" payment "<payment>" coupon "<coupon>"
    Then  no domain error is raised
    And   the order status is "CREATED"
    And   the order subtotal is "<price>"
    And   the order total is "<total>"

    Examples:
      | price | payment       | coupon | total  |
      | 20.00 | CREDIT_CARD   | NONE   | 27.59  |
      | 50.00 | PAYPAL        | NONE   | 54.00  |
      | 80.00 | APPLE_PAY     | SAVE10 | 78.40  |
      | 30.00 | BANK_TRANSFER | FLAT5  | 33.39  |

  # ---------------------------------------------------------------------------
  # Multi-item cart with varied quantities — subtotals and totals are exact
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: Multi-item carts with mixed quantities produce correct totals
    Given I add domain cart items:
      | name       | price        | qty       |
      | <item1>    | <price1>     | <qty1>    |
      | <item2>    | <price2>     | <qty2>    |
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "<coupon>"
    Then  no domain error is raised
    And   the order subtotal is "<subtotal>"
    And   the order total is "<total>"

    Examples:
      | item1   | price1 | qty1 | item2   | price2 | qty2 | coupon | subtotal | total  |
      | Alpha   | 10.00  |    2 | Beta    |  5.00  |    2 | NONE   | 30.00    | 38.39  |
      | Gamma   | 15.00  |    2 | Delta   | 20.00  |    1 | NONE   | 50.00    | 54.00  |
      | Epsilon | 10.00  |    3 | Zeta    | 10.00  |    2 | SAVE10 | 50.00    | 49.00  |
      | Eta     | 25.00  |    2 | Theta   | 10.00  |    2 | FLAT5  | 70.00    | 70.60  |
