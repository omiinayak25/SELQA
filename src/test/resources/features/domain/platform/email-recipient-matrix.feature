@domain @platform @email
Feature: Email Recipient Matrix
  As the platform email subsystem
  I want every recipient string, subject, and body length combination to produce
  the exact expected success or EMAIL_* error code
  So that boundary conditions at 5000 chars and RFC-format validation are fully exercised

  Background:
    Given a clean email service

  # ─── VALID RECIPIENT MATRIX — diverse RFC-compliant addresses succeed ────────────

  @positive @validation @sanity
  Scenario Outline: Valid RFC-style recipient addresses are accepted and stored in the outbox
    When I send an email to "<recipient>" with subject "Subject" and body "Body text"
    Then the operation succeeds
    And the email outbox size is 1
    And the email last recipient is "<recipient>"

    Examples:
      | recipient                          |
      | alice@example.com                  |
      | bob@omiinqa.test                   |
      | user.name@domain.org               |
      | user+tag@sub.domain.co.uk          |
      | firstname.lastname@company.io      |
      | test123@numbers123.net             |
      | a@b.cc                             |
      | x@y.co                             |
      | no-reply@platform.omiinqa.test     |
      | admin@omiinqa.test                 |
      | support+ticket@helpdesk.example.com|
      | data.export@reports.test           |

  # ─── INVALID RECIPIENT MATRIX — diverse malformed addresses raise EMAIL_BAD_RECIPIENT ─

  @negative @validation
  Scenario Outline: Malformed recipient addresses raise EMAIL_BAD_RECIPIENT
    When I send an email to "<recipient>" with subject "Test subject" and body "Test body"
    Then a domain error "EMAIL_BAD_RECIPIENT" is raised

    Examples:
      | recipient              |
      |                        |
      | notanemail             |
      | @nodomain.com          |
      | missingat.example.com  |
      | double@@example.com    |
      | user@                  |
      | @                      |
      | user name@example.com  |
      | user@domain            |
      | plaintext              |

  # ─── BLANK SUBJECT MATRIX — various empty-ish subjects raise EMAIL_BLANK_SUBJECT ─

  @negative @validation
  Scenario Outline: Blank subject lines raise EMAIL_BLANK_SUBJECT regardless of body
    When I send an email to "<recipient>" with subject "" and body "<body>"
    Then a domain error "EMAIL_BLANK_SUBJECT" is raised

    Examples:
      | recipient             | body             |
      | alice@example.com     | Some body text   |
      | bob@omiinqa.test      | Another body     |
      | user@platform.test    | Third body text  |

  # ─── BODY LENGTH BOUNDARY MATRIX — lengths around 5000 char limit ────────────────

  @boundary @positive
  Scenario Outline: Body at or below the 5000-character limit is accepted
    When I send an email to "boundary@example.com" with subject "Subject" and body of length <length>
    Then the operation succeeds
    And the email outbox size is 1

    Examples:
      | length |
      | 0      |
      | 1      |
      | 100    |
      | 1000   |
      | 2500   |
      | 4000   |
      | 4500   |
      | 4998   |
      | 4999   |
      | 5000   |

  @boundary @negative
  Scenario Outline: Body exceeding the 5000-character limit raises EMAIL_BODY_TOO_LONG
    When I send an email to "boundary@example.com" with subject "Subject" and body of length <length>
    Then a domain error "EMAIL_BODY_TOO_LONG" is raised

    Examples:
      | length |
      | 5001   |
      | 5002   |
      | 6000   |
      | 10000  |
      | 50000  |

  # ─── FIND BY RECIPIENT MATRIX — multiple sends and per-recipient counts ──────────

  @positive @business
  Scenario Outline: findByRecipient returns exactly <count> emails for the given address
    When I send an email to "<r1>" with subject "Sub1" and body "Body1"
    And I send an email to "<r2>" with subject "Sub2" and body "Body2"
    And I send an email to "<r1>" with subject "Sub3" and body "Body3"
    Then finding emails by recipient "<r1>" returns <count> emails
    And finding emails by recipient "<r2>" returns 1 email

    Examples:
      | r1                    | r2                    | count |
      | alice@example.com     | bob@example.com       | 2     |
      | carol@omiinqa.test    | dave@omiinqa.test     | 2     |
      | support@platform.test | admin@platform.test   | 2     |

  # ─── BULK SEND MATRIX — mixed-validity recipient lists ───────────────────────────

  @business @validation
  Scenario Outline: Bulk send to a mixed recipient list yields the correct success and failure counts
    When I bulk send an email with subject "Bulk test" and body "Hello" to recipients:
      | <r1> |
      | <r2> |
      | <r3> |
      | <r4> |
    Then the bulk send success count is <success>
    And the bulk send failure count is <failure>
    And the email outbox size is <success>

    Examples:
      | r1                     | r2             | r3                    | r4             | success | failure |
      | a@example.com          | b@example.com  | c@example.com         | d@example.com  | 4       | 0       |
      | good@example.com       | not-email      | also@example.com      | bad-too        | 2       | 2       |
      | not-email-1            | not-email-2    | not-email-3           | not-email-4    | 0       | 4       |
      | only@example.com       | bad1           | bad2                  | bad3           | 1       | 3       |
      | bad1                   | bad2           | bad3                  | good@test.com  | 1       | 3       |

  # ─── OUTBOX ACCUMULATION MATRIX — sequential sends accumulate correctly ──────────

  @positive @regression
  Scenario Outline: Sending <count> valid emails yields outbox size of <count>
    When I send an email to "r1@example.com" with subject "S1" and body "B1"
    And I send an email to "r2@example.com" with subject "S2" and body "B2"
    And I send an email to "r3@example.com" with subject "S3" and body "B3"
    And I send an email to "r4@example.com" with subject "S4" and body "B4"
    And I send an email to "r5@example.com" with subject "S5" and body "B5"
    Then the email outbox size is 5
    And finding emails by recipient "r3@example.com" returns 1 email

    Examples:
      | count |
      | 5     |

  # ─── CLEAR THEN RESEND MATRIX — outbox state resets cleanly ─────────────────────

  @positive @boundary
  Scenario Outline: After clear, resending <count> emails leaves outbox at exactly <count>
    When I send an email to "pre@example.com" with subject "Pre" and body "Pre body"
    And I send an email to "pre2@example.com" with subject "Pre2" and body "Pre body2"
    And I clear the email outbox
    And I send an email to "<to>" with subject "<subject>" and body "<body>"
    Then the email outbox size is 1
    And the email last recipient is "<to>"

    Examples:
      | to                   | subject        | body        |
      | post1@example.com    | Post-clear 1   | Fresh body  |
      | post2@omiinqa.test   | Post-clear 2   | Fresh body2 |
      | admin@platform.test  | Post-clear 3   | Fresh body3 |
