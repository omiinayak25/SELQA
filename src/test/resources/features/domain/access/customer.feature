@domain @access @customer
Feature: Customer Service — Scoped Resource Access
  As the customer platform
  I want customers to access only their own resources
  So that cross-customer data leakage and privilege escalation are impossible

  Background:
    Given a clean customer service
    And a registered customer "alice" with email "alice@omiinqa.test"
    And a registered customer "bob" with email "bob@omiinqa.test"

  @smoke @positive
  Scenario: A customer can view their own profile
    When customer "alice" views their own profile
    Then the operation succeeds

  @positive
  Scenario: A customer can place an order
    When customer "alice" places an order for "Widget A"
    Then the operation succeeds
    And customer "alice" has 1 orders

  @positive
  Scenario: A customer can view their own orders
    Given customer "alice" has placed an order for "Widget B"
    When customer "alice" views their orders
    Then the operation succeeds

  @positive
  Scenario: A customer can cancel their own order
    Given customer "alice" has placed an order for "Widget C"
    When customer "alice" cancels their last order
    Then the operation succeeds
    And customer "alice" has 0 orders

  @positive
  Scenario: A customer can update their own display name
    When customer "alice" updates their display name to "Alice Wonder"
    Then the operation succeeds
    And customer "alice" profile display name is "Alice Wonder"

  @negative @security
  Scenario: A customer cannot view another customer's profile
    When customer "alice" attempts to view profile of "bob"
    Then a domain error "AC_FORBIDDEN_RESOURCE" is raised

  @negative @security
  Scenario: A customer cannot update another customer's profile
    When customer "alice" attempts to update profile of "bob" to "HackedName"
    Then a domain error "AC_FORBIDDEN_RESOURCE" is raised

  @negative @security
  Scenario: A customer cannot view another customer's orders
    Given customer "bob" has placed an order for "Widget D"
    When customer "alice" attempts to view orders of "bob"
    Then a domain error "AC_FORBIDDEN_RESOURCE" is raised

  @negative @security
  Scenario: Cross-tenant access — customer cannot cancel another customer's order
    Given customer "bob" has placed an order for "Widget E"
    When customer "alice" attempts to cancel order "ORD-BOB-1"
    Then a domain error "CUST_ORDER_NOT_FOUND" is raised

  @negative @security
  Scenario: A guest actor cannot place orders
    Given a non-customer actor "visitor" with role "GUEST"
    When actor "visitor" attempts to place an order for "Widget F"
    Then a domain error "AC_DENIED" is raised

  @negative @boundary
  Scenario: Cancelling a non-existent order is rejected
    When customer "alice" attempts to cancel order "ORD-FAKE-999"
    Then a domain error "CUST_ORDER_NOT_FOUND" is raised

  @positive @boundary
  Scenario: Multiple orders accumulate correctly per customer
    When customer "alice" places an order for "Item 1"
    And customer "alice" places an order for "Item 2"
    And customer "bob" places an order for "Item 3"
    Then customer "alice" has 2 orders
    And customer "bob" has 1 orders

  @positive @business
  Scenario: Customer display name defaults to username
    Then customer "alice" profile display name is "alice"

  @negative @boundary
  Scenario Outline: Cross-customer resource access is always rejected
    When customer "<actor>" attempts to view profile of "<target>"
    Then a domain error "AC_FORBIDDEN_RESOURCE" is raised

    Examples:
      | actor | target |
      | alice | bob    |
      | bob   | alice  |
