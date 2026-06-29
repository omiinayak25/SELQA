@domain @platform @notifications
Feature: Notification Service
  As the platform notification subsystem
  I want to enqueue, deliver, and manage notifications per user and channel
  So that users receive relevant alerts based on their preferences

  Background:
    Given a clean notification service

  # ─── POSITIVE ────────────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: A notification is enqueued and counted as unread
    When I enqueue a notification for user "u1" on channel "IN_APP" of type "ALERT" with message "Hello"
    Then the notification unread count for user "u1" is 1
    And the notification total count for user "u1" is 1

  @positive
  Scenario: Multiple notifications are enqueued for the same user
    When I enqueue a notification for user "u2" on channel "IN_APP" of type "ALERT" with message "First"
    And I enqueue a notification for user "u2" on channel "EMAIL" of type "PROMO" with message "Second"
    And I enqueue a notification for user "u2" on channel "SMS" of type "ALERT" with message "Third"
    Then the notification unread count for user "u2" is 3
    And the notification total count for user "u2" is 3

  @positive
  Scenario: markRead transitions a notification to read
    When I enqueue a notification for user "u3" on channel "IN_APP" of type "ALERT" with message "Read me"
    And I mark the last notification as read for user "u3"
    Then the notification unread count for user "u3" is 0
    And the notification total count for user "u3" is 1

  @positive
  Scenario: markAllRead marks every unread notification for a user
    When I enqueue a notification for user "u4" on channel "IN_APP" of type "INFO" with message "Msg1"
    And I enqueue a notification for user "u4" on channel "EMAIL" of type "PROMO" with message "Msg2"
    And I enqueue a notification for user "u4" on channel "PUSH" of type "ALERT" with message "Msg3"
    And I mark all notifications as read for user "u4"
    Then the notification unread count for user "u4" is 0
    And the notification total count for user "u4" is 3

  @positive
  Scenario: markAllRead returns the count of notifications that were marked
    When I enqueue a notification for user "u5" on channel "IN_APP" of type "ALERT" with message "A"
    And I enqueue a notification for user "u5" on channel "SMS" of type "ALERT" with message "B"
    Then marking all notifications as read for user "u5" returns count 2

  @positive
  Scenario: Listing notifications by channel returns only matching items
    When I enqueue a notification for user "u6" on channel "IN_APP" of type "ALERT" with message "App notif"
    And I enqueue a notification for user "u6" on channel "EMAIL" of type "PROMO" with message "Email notif"
    And I enqueue a notification for user "u6" on channel "SMS" of type "ALERT" with message "SMS notif"
    Then the notification list for user "u6" filtered by channel "IN_APP" has 1 item
    And the notification list for user "u6" filtered by channel "EMAIL" has 1 item
    And the notification list for user "u6" filtered by channel "SMS" has 1 item

  @positive
  Scenario: Listing notifications by type returns only matching items
    When I enqueue a notification for user "u7" on channel "IN_APP" of type "ALERT" with message "Alert notif"
    And I enqueue a notification for user "u7" on channel "EMAIL" of type "PROMO" with message "Promo notif"
    And I enqueue a notification for user "u7" on channel "PUSH" of type "ALERT" with message "Alert push"
    Then the notification list for user "u7" filtered by type "ALERT" has 2 items
    And the notification list for user "u7" filtered by type "PROMO" has 1 item

  @positive
  Scenario: Listing read notifications returns only read items
    When I enqueue a notification for user "u8" on channel "IN_APP" of type "ALERT" with message "Unread"
    And I enqueue a notification for user "u8" on channel "EMAIL" of type "PROMO" with message "To read"
    And I mark all notifications as read for user "u8"
    And I enqueue a notification for user "u8" on channel "SMS" of type "ALERT" with message "New unread"
    Then the notification list for user "u8" filtered by read "true" has 2 items
    And the notification list for user "u8" filtered by read "false" has 1 item

  # ─── PREFERENCE / MUTE ───────────────────────────────────────────────────────

  @business @positive
  Scenario: A muted channel silently drops notifications — enqueue returns success but no notification stored
    When I disable notification channel "SMS" for user "mute1"
    And I enqueue a notification for user "mute1" on channel "SMS" of type "ALERT" with message "Dropped"
    Then no domain error is raised
    And the notification total count for user "mute1" is 0
    And the notification unread count for user "mute1" is 0

  @business @positive
  Scenario: A muted channel does not affect other channels
    When I disable notification channel "EMAIL" for user "mute2"
    And I enqueue a notification for user "mute2" on channel "EMAIL" of type "PROMO" with message "Dropped email"
    And I enqueue a notification for user "mute2" on channel "IN_APP" of type "ALERT" with message "Delivered"
    Then the notification total count for user "mute2" is 1
    And the notification unread count for user "mute2" is 1

  @business
  Scenario: Re-enabling a muted channel allows notifications to be stored again
    When I disable notification channel "PUSH" for user "mute3"
    And I enqueue a notification for user "mute3" on channel "PUSH" of type "ALERT" with message "Dropped"
    And I enable notification channel "PUSH" for user "mute3"
    And I enqueue a notification for user "mute3" on channel "PUSH" of type "ALERT" with message "Delivered"
    Then the notification total count for user "mute3" is 1

  @business
  Scenario Outline: Per-user preference matrix — muted channel drops, others deliver
    When I disable notification channel "<muted_channel>" for user "prefuser"
    And I enqueue a notification for user "prefuser" on channel "<muted_channel>" of type "ALERT" with message "Dropped"
    And I enqueue a notification for user "prefuser" on channel "<active_channel>" of type "ALERT" with message "Delivered"
    Then the notification total count for user "prefuser" is 1
    And the notification unread count for user "prefuser" is 1

    Examples:
      | muted_channel | active_channel |
      | SMS           | IN_APP         |
      | PUSH          | EMAIL          |
      | EMAIL         | SMS            |
      | IN_APP        | PUSH           |

  # ─── NEGATIVE / VALIDATION ───────────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Enqueue rejects invalid input with a specific error code
    When I enqueue a notification for user "<userId>" on channel "<channel>" of type "<type>" with message "<message>"
    Then a domain error "<code>" is raised

    Examples:
      | userId | channel | type  | message | code              |
      |        | IN_APP  | ALERT | Hello   | NOTIF_BLANK       |
      | u9     | IN_APP  |       | Hello   | NOTIF_BLANK       |
      | u9     | IN_APP  | ALERT |         | NOTIF_BLANK       |
      | u9     | BADCHAN | ALERT | Hello   | NOTIF_BAD_CHANNEL |

  @negative @validation
  Scenario: markRead raises NOTIF_NOT_FOUND for a non-existent notification id
    When I mark notification id 99999 as read
    Then a domain error "NOTIF_NOT_FOUND" is raised

  @negative @validation
  Scenario: setPreference raises NOTIF_BLANK when userId is blank
    When I set notification preference for user "" on channel "IN_APP" to enabled "true"
    Then a domain error "NOTIF_BLANK" is raised

  @negative @validation
  Scenario: setPreference raises NOTIF_BAD_CHANNEL when channel is invalid
    When I set notification preference for user "u10" on channel "INVALID" to enabled "true"
    Then a domain error "NOTIF_BAD_CHANNEL" is raised

  # ─── BOUNDARY ────────────────────────────────────────────────────────────────

  @boundary
  Scenario: Notifications for different users are isolated
    When I enqueue a notification for user "userA" on channel "IN_APP" of type "ALERT" with message "For A"
    And I enqueue a notification for user "userB" on channel "IN_APP" of type "ALERT" with message "For B"
    Then the notification total count for user "userA" is 1
    And the notification total count for user "userB" is 1
    And the notification unread count for user "userA" is 1
    And the notification unread count for user "userB" is 1

  @boundary
  Scenario: markAllRead on a user with no notifications returns 0
    Then marking all notifications as read for user "nobody" returns count 0

  @boundary
  Scenario Outline: Each channel independently accepts a notification when not muted
    When I enqueue a notification for user "chantest" on channel "<channel>" of type "ALERT" with message "Test"
    Then no domain error is raised
    And the notification total count for user "chantest" is 1
    And the notification unread count for user "chantest" is 1

    Examples:
      | channel |
      | IN_APP  |
      | EMAIL   |
      | SMS     |
      | PUSH    |
