@domain @orders @lifecycle
Feature: Order Lifecycle Transition Matrix — Legal and Illegal Moves
  As the orders platform
  I want every from-status x to-status combination to either succeed or produce the correct error
  So that the lifecycle is exhaustively guarded by the test suite

  # Legal transitions (OrderService):
  #   CREATED  -> PAID                   (forward)
  #   PAID     -> SHIPPED                (forward)
  #   SHIPPED  -> DELIVERED              (forward)
  #   CREATED  -> CANCELLED              (cancel before shipment)
  #   PAID     -> CANCELLED              (cancel before shipment)
  #   PAID     -> REFUNDED               (refund eligible)
  #   DELIVERED -> REFUNDED              (refund eligible)
  # Illegal transitions by category:
  #   Cancelling SHIPPED/DELIVERED/REFUNDED -> ORDER_CANNOT_CANCEL
  #   Refunding CREATED/SHIPPED/CANCELLED   -> ORDER_CANNOT_REFUND
  #   Any other forward skip / backward     -> ORDER_BAD_TRANSITION

  Background:
    Given a clean checkout service

  # ---------------------------------------------------------------------------
  # Legal forward progression (single steps)
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: Legal single-step forward transitions succeed
    Given an existing order "<orderId>" with status "<from>" and total "50.00"
    When  I transition order "<orderId>" to status "<to>"
    Then  no domain error is raised
    And   the order status is "<to>"

    Examples:
      | orderId | from      | to        |
      | 7001    | CREATED   | PAID      |
      | 7002    | PAID      | SHIPPED   |
      | 7003    | SHIPPED   | DELIVERED |

  # ---------------------------------------------------------------------------
  # Legal cancellation (any state before shipment)
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: Cancellation is permitted when the order has not yet shipped
    Given an existing order "<orderId>" with status "<from>" and total "30.00"
    When  I transition order "<orderId>" to status "CANCELLED"
    Then  no domain error is raised
    And   the order status is "CANCELLED"

    Examples:
      | orderId | from    |
      | 7010    | CREATED |
      | 7011    | PAID    |

  # ---------------------------------------------------------------------------
  # Legal refund states
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: Refund is permitted only from PAID or DELIVERED
    Given an existing order "<orderId>" with status "<from>" and total "80.00"
    When  I transition order "<orderId>" to status "REFUNDED"
    Then  no domain error is raised
    And   the order status is "REFUNDED"

    Examples:
      | orderId | from      |
      | 7020    | PAID      |
      | 7021    | DELIVERED |

  # ---------------------------------------------------------------------------
  # Illegal cancellation after shipment -> ORDER_CANNOT_CANCEL
  # ---------------------------------------------------------------------------

  @negative @business @regression
  Scenario Outline: Cancellation after shipment raises ORDER_CANNOT_CANCEL
    Given an existing order "<orderId>" with status "<from>" and total "54.00"
    When  I transition order "<orderId>" to status "CANCELLED"
    Then  a domain error "ORDER_CANNOT_CANCEL" is raised

    Examples:
      | orderId | from      |
      | 7030    | SHIPPED   |
      | 7031    | DELIVERED |
      | 7032    | REFUNDED  |

  # ---------------------------------------------------------------------------
  # Illegal refund from non-refundable states -> ORDER_CANNOT_REFUND
  # ---------------------------------------------------------------------------

  @negative @business @regression
  Scenario Outline: Refund from a non-refundable state raises ORDER_CANNOT_REFUND
    Given an existing order "<orderId>" with status "<from>" and total "54.00"
    When  I transition order "<orderId>" to status "REFUNDED"
    Then  a domain error "ORDER_CANNOT_REFUND" is raised

    Examples:
      | orderId | from      |
      | 7040    | CREATED   |
      | 7041    | SHIPPED   |
      | 7042    | CANCELLED |

  # ---------------------------------------------------------------------------
  # Illegal forward skips and backward moves -> ORDER_BAD_TRANSITION
  # ---------------------------------------------------------------------------

  @negative @validation @regression
  Scenario Outline: Forward skips and backward moves raise ORDER_BAD_TRANSITION
    Given an existing order "<orderId>" with status "<from>" and total "54.00"
    When  I transition order "<orderId>" to status "<to>"
    Then  a domain error "ORDER_BAD_TRANSITION" is raised

    Examples:
      | orderId | from      | to        |
      | 7050    | CREATED   | SHIPPED   |
      | 7051    | CREATED   | DELIVERED |
      | 7053    | PAID      | CREATED   |
      | 7054    | PAID      | DELIVERED |
      | 7055    | SHIPPED   | CREATED   |
      | 7056    | SHIPPED   | PAID      |
      | 7057    | DELIVERED | CREATED   |
      | 7058    | DELIVERED | PAID      |
      | 7059    | DELIVERED | SHIPPED   |
      | 7061    | CANCELLED | CREATED   |
      | 7062    | CANCELLED | PAID      |
      | 7063    | CANCELLED | SHIPPED   |
      | 7064    | CANCELLED | DELIVERED |

  # ---------------------------------------------------------------------------
  # Revenue counting — only PAID/SHIPPED/DELIVERED count
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario Outline: Revenue counting status matrix — included vs excluded
    Given an existing order "<orderId>" with status "<status>" and total "<total>"
    Then  the total revenue is "<expectedRevenue>"

    Examples:
      | orderId | status    | total  | expectedRevenue |
      | 8001    | PAID      | 100.00 | 100.00          |
      | 8002    | SHIPPED   |  80.00 |  80.00          |
      | 8003    | DELIVERED |  60.00 |  60.00          |
      | 8004    | CREATED   | 200.00 |   0.00          |
      | 8005    | CANCELLED |  50.00 |   0.00          |

  # ---------------------------------------------------------------------------
  # Multi-status revenue — aggregate across a mixed portfolio
  # ---------------------------------------------------------------------------

  @business @regression
  Scenario: Revenue aggregates only PAID and SHIPPED and DELIVERED orders correctly
    Given an existing order "8010" with status "PAID" and total "120.00"
    And   an existing order "8011" with status "SHIPPED" and total "80.00"
    And   an existing order "8012" with status "DELIVERED" and total "60.00"
    And   an existing order "8013" with status "CREATED" and total "500.00"
    And   an existing order "8014" with status "CANCELLED" and total "250.00"
    And   an existing order "8015" with status "REFUNDED" and total "40.00"
    Then  the total revenue is "260.00"

  # ---------------------------------------------------------------------------
  # Order-count queries per status
  # ---------------------------------------------------------------------------

  @positive @business
  Scenario: Listing by status returns correctly filtered result sets
    Given an existing order "9001" with status "CREATED" and total "10.00"
    And   an existing order "9002" with status "CREATED" and total "20.00"
    And   an existing order "9003" with status "PAID" and total "30.00"
    And   an existing order "9004" with status "SHIPPED" and total "40.00"
    And   an existing order "9005" with status "DELIVERED" and total "50.00"
    And   an existing order "9006" with status "CANCELLED" and total "60.00"
    Then  the order count for status "CREATED" is 2
    And   the order count for status "PAID" is 1
    And   the order count for status "SHIPPED" is 1
    And   the order count for status "DELIVERED" is 1
    And   the order count for status "CANCELLED" is 1
    And   the order count for status "REFUNDED" is 0
