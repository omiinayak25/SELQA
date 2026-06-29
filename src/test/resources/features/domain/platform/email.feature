@domain @platform @email
Feature: Email Service
  As the platform email subsystem
  I want to send, validate, and track emails in an in-memory outbox
  So that platform components can dispatch messages reliably with enforced business rules

  Background:
    Given a clean email service

  # ─── POSITIVE ────────────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: A valid email is sent and stored in the outbox
    When I send an email to "alice@omiinqa.test" with subject "Welcome" and body "Hello Alice"
    Then the operation succeeds
    And the email outbox size is 1
    And the email last recipient is "alice@omiinqa.test"

  @positive
  Scenario: Multiple emails accumulate in the outbox
    When I send an email to "bob@omiinqa.test" with subject "Notice 1" and body "Body one"
    And I send an email to "carol@omiinqa.test" with subject "Notice 2" and body "Body two"
    And I send an email to "dave@omiinqa.test" with subject "Notice 3" and body "Body three"
    Then the email outbox size is 3

  @positive
  Scenario: findByRecipient returns only emails for the given address
    When I send an email to "eve@omiinqa.test" with subject "Sub A" and body "Body A"
    And I send an email to "frank@omiinqa.test" with subject "Sub B" and body "Body B"
    And I send an email to "eve@omiinqa.test" with subject "Sub C" and body "Body C"
    Then finding emails by recipient "eve@omiinqa.test" returns 2 emails
    And finding emails by recipient "frank@omiinqa.test" returns 1 email

  @positive
  Scenario: lastTo returns the most recently sent recipient
    When I send an email to "first@omiinqa.test" with subject "First" and body "Body first"
    And I send an email to "second@omiinqa.test" with subject "Second" and body "Body second"
    Then the email last recipient is "second@omiinqa.test"

  @positive
  Scenario: Email body at exactly the maximum length is accepted
    When I send an email to "max@omiinqa.test" with subject "Boundary" and body of length 5000
    Then the operation succeeds
    And the email outbox size is 1

  @positive
  Scenario: Clearing the outbox resets all stored emails
    When I send an email to "clear@omiinqa.test" with subject "To clear" and body "Temp body"
    And I clear the email outbox
    Then the email outbox size is 0

  # ─── BULK SEND ───────────────────────────────────────────────────────────────

  @positive @business
  Scenario: Bulk send to all valid recipients succeeds with correct counts
    When I bulk send an email with subject "Bulk notice" and body "Hello everyone" to recipients:
      | grace@omiinqa.test  |
      | henry@omiinqa.test  |
      | irene@omiinqa.test  |
    Then the bulk send success count is 3
    And the bulk send failure count is 0
    And the email outbox size is 3

  @business
  Scenario: Bulk send with some invalid recipients produces a partial-failure report
    When I bulk send an email with subject "Mixed bulk" and body "Test body" to recipients:
      | valid@omiinqa.test   |
      | not-an-email         |
      | also@omiinqa.test    |
      | @missinglocal.com    |
    Then the bulk send success count is 2
    And the bulk send failure count is 2
    And the email outbox size is 2
    And the bulk send error list is not empty

  @business
  Scenario: Bulk send to all invalid recipients has zero successes
    When I bulk send an email with subject "All bad" and body "Body" to recipients:
      | notanemail           |
      | also-not-an-email    |
    Then the bulk send success count is 0
    And the bulk send failure count is 2
    And the email outbox size is 0

  # ─── NEGATIVE / VALIDATION ───────────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Send rejects invalid recipient email addresses
    When I send an email to "<recipient>" with subject "Test" and body "Body"
    Then a domain error "EMAIL_BAD_RECIPIENT" is raised

    Examples:
      | recipient           |
      |                     |
      | not-an-email        |
      | @nodomain           |
      | missingat.com       |
      | double@@omiinqa.test|
      | user@               |

  @negative @validation
  Scenario: Send rejects a blank subject
    When I send an email to "valid@omiinqa.test" with subject "" and body "Body"
    Then a domain error "EMAIL_BLANK_SUBJECT" is raised

  @negative @validation
  Scenario: Send rejects body that exceeds the maximum allowed length
    When I send an email to "long@omiinqa.test" with subject "Too long" and body of length 5001
    Then a domain error "EMAIL_BODY_TOO_LONG" is raised

  @negative @validation
  Scenario: Send rejects body that far exceeds the maximum allowed length
    When I send an email to "huge@omiinqa.test" with subject "Huge body" and body of length 10000
    Then a domain error "EMAIL_BODY_TOO_LONG" is raised

  # ─── BOUNDARY ────────────────────────────────────────────────────────────────

  @boundary
  Scenario: Body at exactly one character below the limit is accepted
    When I send an email to "nearboundary@omiinqa.test" with subject "Almost max" and body of length 4999
    Then the operation succeeds
    And the email outbox size is 1

  @boundary
  Scenario: lastTo returns null when the outbox is empty
    Then the email last recipient is null

  @boundary
  Scenario: findByRecipient returns empty list when recipient has no emails
    Then finding emails by recipient "nobody@omiinqa.test" returns 0 emails

  @boundary
  Scenario Outline: Recipient address validation covers diverse formats
    When I send an email to "<recipient>" with subject "Valid" and body "Body"
    Then the operation succeeds

    Examples:
      | recipient                     |
      | simple@example.com            |
      | user.name+tag@sub.domain.org  |
      | x@y.co                        |

  @regression
  Scenario: Outbox is isolated per scenario — prior scenario data does not leak
    Then the email outbox size is 0
    And the email last recipient is null
