@domain @orders
Feature: Domain Order Lifecycle
  As the orders platform
  I want to enforce the legal order status lifecycle and provide revenue queries
  So that orders progress correctly from creation to delivery and illegal transitions are rejected

  Background:
    Given a clean checkout service

  # ---------------------------------------------------------------------------
  # Retrieve — positive / negative
  # ---------------------------------------------------------------------------

  @smoke @positive
  Scenario: A placed order can be retrieved by its ID
    Given I add a domain cart item "Gadget Pro" priced "60.00" qty 1
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "NONE"
    And   the order status is "CREATED"
    Then  no domain error is raised

  @negative
  Scenario: Retrieving a non-existent order raises ORDER_NOT_FOUND
    When I retrieve a non-existent order "9999"
    Then a domain error "ORDER_NOT_FOUND" is raised

  # ---------------------------------------------------------------------------
  # Status transitions — CREATED -> PAID -> SHIPPED -> DELIVERED
  # ---------------------------------------------------------------------------

  @positive @business
  Scenario: An order progresses through the full CREATED → PAID → SHIPPED → DELIVERED lifecycle
    Given an existing order "1001" with status "CREATED" and total "54.00"
    When  I transition order "1001" to status "PAID"
    Then  the order status is "PAID"
    When  I transition order "1001" to status "SHIPPED"
    Then  the order status is "SHIPPED"
    When  I transition order "1001" to status "DELIVERED"
    Then  the order status is "DELIVERED"

  @positive @business
  Scenario: A CREATED order can be cancelled
    Given an existing order "1002" with status "CREATED" and total "30.00"
    When  I transition order "1002" to status "CANCELLED"
    Then  the order status is "CANCELLED"

  @positive @business
  Scenario: A PAID order can be cancelled before shipping
    Given an existing order "1003" with status "PAID" and total "30.00"
    When  I transition order "1003" to status "CANCELLED"
    Then  the order status is "CANCELLED"

  @positive @business
  Scenario: A PAID order can be refunded
    Given an existing order "1004" with status "PAID" and total "108.00"
    When  I transition order "1004" to status "REFUNDED"
    Then  the order status is "REFUNDED"

  @positive @business
  Scenario: A DELIVERED order can be refunded
    Given an existing order "1005" with status "DELIVERED" and total "81.00"
    When  I transition order "1005" to status "REFUNDED"
    Then  the order status is "REFUNDED"

  # ---------------------------------------------------------------------------
  # Illegal transitions — negative
  # ---------------------------------------------------------------------------

  @negative @business
  Scenario: Cancelling a SHIPPED order raises ORDER_CANNOT_CANCEL
    Given an existing order "2001" with status "SHIPPED" and total "54.00"
    When  I transition order "2001" to status "CANCELLED"
    Then  a domain error "ORDER_CANNOT_CANCEL" is raised

  @negative @business
  Scenario: Cancelling a DELIVERED order raises ORDER_CANNOT_CANCEL
    Given an existing order "2002" with status "DELIVERED" and total "54.00"
    When  I transition order "2002" to status "CANCELLED"
    Then  a domain error "ORDER_CANNOT_CANCEL" is raised

  @negative @business
  Scenario: Refunding a CREATED order raises ORDER_CANNOT_REFUND
    Given an existing order "2003" with status "CREATED" and total "54.00"
    When  I transition order "2003" to status "REFUNDED"
    Then  a domain error "ORDER_CANNOT_REFUND" is raised

  @negative @business
  Scenario: Refunding a SHIPPED order raises ORDER_CANNOT_REFUND
    Given an existing order "2004" with status "SHIPPED" and total "54.00"
    When  I transition order "2004" to status "REFUNDED"
    Then  a domain error "ORDER_CANNOT_REFUND" is raised

  @negative @business
  Scenario: Refunding a CANCELLED order raises ORDER_CANNOT_REFUND
    Given an existing order "2005" with status "CANCELLED" and total "54.00"
    When  I transition order "2005" to status "REFUNDED"
    Then  a domain error "ORDER_CANNOT_REFUND" is raised

  # ---------------------------------------------------------------------------
  # Illegal transition matrix — Scenario Outline
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: Illegal forward transitions raise ORDER_BAD_TRANSITION
    Given an existing order "3001" with status "<from>" and total "50.00"
    When  I transition order "3001" to status "<to>"
    Then  a domain error "<error>" is raised

    Examples:
      | from      | to        | error                |
      | CREATED   | SHIPPED   | ORDER_BAD_TRANSITION |
      | CREATED   | DELIVERED | ORDER_BAD_TRANSITION |
      | PAID      | CREATED   | ORDER_BAD_TRANSITION |
      | PAID      | DELIVERED | ORDER_BAD_TRANSITION |
      | SHIPPED   | CREATED   | ORDER_BAD_TRANSITION |
      | SHIPPED   | PAID      | ORDER_BAD_TRANSITION |
      | DELIVERED | PAID      | ORDER_BAD_TRANSITION |
      | DELIVERED | SHIPPED   | ORDER_BAD_TRANSITION |

  # ---------------------------------------------------------------------------
  # Revenue and listing
  # ---------------------------------------------------------------------------

  @positive @business
  Scenario: Total revenue includes PAID and DELIVERED orders but excludes CREATED and CANCELLED
    Given an existing order "4001" with status "PAID" and total "100.00"
    And an existing order "4002" with status "DELIVERED" and total "50.00"
    And an existing order "4003" with status "CREATED" and total "200.00"
    And an existing order "4004" with status "CANCELLED" and total "75.00"
    Then the total revenue is "150.00"

  @positive @business
  Scenario: Listing orders by status returns only matching orders
    Given an existing order "5001" with status "PAID" and total "30.00"
    And an existing order "5002" with status "PAID" and total "40.00"
    And an existing order "5003" with status "SHIPPED" and total "60.00"
    Then the order count for status "PAID" is 2
    And the order count for status "SHIPPED" is 1
    And the order count for status "CREATED" is 0

  @boundary @business
  Scenario: Revenue is zero when there are no paid orders
    Given an existing order "6001" with status "CREATED" and total "99.00"
    And an existing order "6002" with status "CANCELLED" and total "55.00"
    Then the total revenue is "0.00"

  # ---------------------------------------------------------------------------
  # Integration — checkout then transition
  # ---------------------------------------------------------------------------

  @positive @sanity
  Scenario: A domain checkout order transitions correctly through to DELIVERED
    Given I add domain cart items:
      | name        | price | qty |
      | Laptop Bag  | 45.00 | 1   |
      | USB Hub     | 20.00 | 1   |
    When  I perform a domain checkout with address "VALID" payment "CREDIT_CARD" coupon "SAVE10"
    Then  no domain error is raised
    And   the order status is "CREATED"
