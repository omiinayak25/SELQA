@domain @platform @notifications
Feature: Notification Channel Matrix
  As the platform notification subsystem
  I want every channel×preference combination to produce the correct delivered/dropped/unread outcome
  So that user-specific muting rules are enforced across all four channels

  Background:
    Given a clean notification service

  # ─── CHANNEL DELIVER MATRIX — every channel delivers when not muted ─────────────

  @positive @boundary @sanity
  Scenario Outline: Notification is delivered on every channel when preference is enabled
    When I set notification preference for user "<user>" on channel "<channel>" to enabled "true"
    And I enqueue a notification for user "<user>" on channel "<channel>" of type "<type>" with message "<msg>"
    Then no domain error is raised
    And the notification total count for user "<user>" is 1
    And the notification unread count for user "<user>" is 1

    Examples:
      | user | channel | type         | msg              |
      | ua1  | IN_APP  | ALERT        | In-app alert     |
      | ua2  | EMAIL   | ALERT        | Email alert      |
      | ua3  | SMS     | ALERT        | SMS alert        |
      | ua4  | PUSH    | ALERT        | Push alert       |
      | ua5  | IN_APP  | PROMO        | In-app promo     |
      | ua6  | EMAIL   | PROMO        | Email promo      |
      | ua7  | SMS     | PROMO        | SMS promo        |
      | ua8  | PUSH    | PROMO        | Push promo       |
      | ua9  | IN_APP  | INFO         | In-app info      |
      | ua10 | EMAIL   | INFO         | Email info       |
      | ua11 | SMS     | INFO         | SMS info         |
      | ua12 | PUSH    | INFO         | Push info        |
      | ua13 | IN_APP  | ORDER_SHIPPED| Shipped in-app   |
      | ua14 | EMAIL   | ORDER_SHIPPED| Shipped email    |
      | ua15 | SMS     | ORDER_SHIPPED| Shipped SMS      |
      | ua16 | PUSH    | ORDER_SHIPPED| Shipped push     |

  # ─── CHANNEL DROP MATRIX — every channel is silently dropped when muted ──────────

  @business @negative
  Scenario Outline: Notification is silently dropped on every channel when preference is disabled
    When I disable notification channel "<channel>" for user "<user>"
    And I enqueue a notification for user "<user>" on channel "<channel>" of type "ALERT" with message "Should drop"
    Then no domain error is raised
    And the notification total count for user "<user>" is 0
    And the notification unread count for user "<user>" is 0

    Examples:
      | user | channel |
      | md1  | IN_APP  |
      | md2  | EMAIL   |
      | md3  | SMS     |
      | md4  | PUSH    |

  # ─── CROSS-CHANNEL ISOLATION — muting one channel must not affect others ─────────

  @business @positive
  Scenario Outline: Muting <muted> does not suppress delivery on the other three channels
    When I disable notification channel "<muted>" for user "<user>"
    And I enqueue a notification for user "<user>" on channel "<ch1>" of type "INFO" with message "Ch1 msg"
    And I enqueue a notification for user "<user>" on channel "<ch2>" of type "INFO" with message "Ch2 msg"
    And I enqueue a notification for user "<user>" on channel "<ch3>" of type "INFO" with message "Ch3 msg"
    Then the notification total count for user "<user>" is 3
    And the notification unread count for user "<user>" is 3

    Examples:
      | user | muted  | ch1    | ch2   | ch3  |
      | ci1  | SMS    | IN_APP | EMAIL | PUSH |
      | ci2  | PUSH   | IN_APP | EMAIL | SMS  |
      | ci3  | EMAIL  | IN_APP | SMS   | PUSH |
      | ci4  | IN_APP | EMAIL  | SMS   | PUSH |

  # ─── MUTED + ACTIVE COMBO MATRIX — delivered/dropped counts ─────────────────────

  @business
  Scenario Outline: Sending to muted and active channel yields total count 1 and unread count 1
    When I disable notification channel "<muted>" for user "<user>"
    And I enqueue a notification for user "<user>" on channel "<muted>" of type "ALERT" with message "Dropped"
    And I enqueue a notification for user "<user>" on channel "<active>" of type "ALERT" with message "Delivered"
    Then the notification total count for user "<user>" is 1
    And the notification unread count for user "<user>" is 1

    Examples:
      | user | muted  | active  |
      | mc1  | IN_APP | EMAIL   |
      | mc2  | IN_APP | SMS     |
      | mc3  | IN_APP | PUSH    |
      | mc4  | EMAIL  | IN_APP  |
      | mc5  | EMAIL  | SMS     |
      | mc6  | EMAIL  | PUSH    |
      | mc7  | SMS    | IN_APP  |
      | mc8  | SMS    | EMAIL   |
      | mc9  | SMS    | PUSH    |
      | mc10 | PUSH   | IN_APP  |
      | mc11 | PUSH   | EMAIL   |
      | mc12 | PUSH   | SMS     |

  # ─── RE-ENABLE MATRIX — disable then re-enable, check delivery resumes ───────────

  @business @regression
  Scenario Outline: Re-enabling a channel after muting restores delivery
    When I disable notification channel "<channel>" for user "<user>"
    And I enqueue a notification for user "<user>" on channel "<channel>" of type "ALERT" with message "Dropped"
    And I enable notification channel "<channel>" for user "<user>"
    And I enqueue a notification for user "<user>" on channel "<channel>" of type "ALERT" with message "Delivered"
    Then the notification total count for user "<user>" is 1
    And the notification unread count for user "<user>" is 1

    Examples:
      | user | channel |
      | re1  | IN_APP  |
      | re2  | EMAIL   |
      | re3  | SMS     |
      | re4  | PUSH    |

  # ─── READ STATE MATRIX — unread count after markRead and markAllRead ─────────────

  @positive @regression
  Scenario Outline: After marking all read the unread count is zero and total count is unchanged
    When I enqueue a notification for user "<user>" on channel "IN_APP" of type "ALERT" with message "A"
    And I enqueue a notification for user "<user>" on channel "EMAIL" of type "PROMO" with message "B"
    And I enqueue a notification for user "<user>" on channel "SMS" of type "INFO" with message "C"
    And I mark all notifications as read for user "<user>"
    Then the notification unread count for user "<user>" is 0
    And the notification total count for user "<user>" is 3

    Examples:
      | user |
      | rs1  |
      | rs2  |
      | rs3  |

  # ─── LIST FILTER MATRIX — channel/type/read filters return exact sizes ───────────

  @positive @sanity
  Scenario Outline: Channel filter returns exactly the notifications on that channel
    When I enqueue a notification for user "lf1" on channel "IN_APP" of type "A1" with message "m1"
    And I enqueue a notification for user "lf1" on channel "EMAIL" of type "A2" with message "m2"
    And I enqueue a notification for user "lf1" on channel "SMS" of type "A3" with message "m3"
    And I enqueue a notification for user "lf1" on channel "PUSH" of type "A4" with message "m4"
    Then the notification list for user "lf1" filtered by channel "<channel>" has <count> item

    Examples:
      | channel | count |
      | IN_APP  | 1     |
      | EMAIL   | 1     |
      | SMS     | 1     |
      | PUSH    | 1     |

  # ─── USER ISOLATION MATRIX — different users see only their own notifications ────

  @boundary
  Scenario Outline: Each user's notification count is independent of other users
    When I enqueue a notification for user "<u1>" on channel "IN_APP" of type "ALERT" with message "For u1"
    And I enqueue a notification for user "<u1>" on channel "EMAIL" of type "PROMO" with message "For u1 b"
    And I enqueue a notification for user "<u2>" on channel "IN_APP" of type "ALERT" with message "For u2"
    Then the notification total count for user "<u1>" is 2
    And the notification unread count for user "<u1>" is 2
    And the notification total count for user "<u2>" is 1
    And the notification unread count for user "<u2>" is 1

    Examples:
      | u1    | u2    |
      | iso1a | iso1b |
      | iso2a | iso2b |
      | iso3a | iso3b |
