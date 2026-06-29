@domain @identity @validation
Feature: Email Validation Matrix
  As the identity platform
  I want every email-address boundary condition covered by the actual regex
  So that RFC-edge cases are accepted or rejected deterministically

  # Regex: ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
  # Local-part charset: A-Z a-z 0-9 . _ % + -
  # Domain charset:     A-Z a-z 0-9 . -
  # TLD must be [A-Za-z]{2,}

  Background:
    Given a clean validation context

  # ---------------------------------------------------------------------------
  # Block A — isValidEmail returns TRUE (positive equivalence + RFC edges)
  # ---------------------------------------------------------------------------

  @validation @positive @boundary
  Scenario Outline: isValidEmail accepts well-formed email addresses
    When I check validation email "<email>"
    Then the validation result is "true"

    Examples:
      | email                                       | notes                                          |
      | user@example.com                            | baseline                                       |
      | USER@EXAMPLE.COM                            | all-uppercase — regex is case-insensitive      |
      | u@e.io                                      | single-char local, 2-char TLD                  |
      | a@b.co                                      | minimal valid                                  |
      | a@b.museum                                  | long TLD                                       |
      | user.name@example.com                       | dot in local part                              |
      | user+tag@example.com                        | plus sign in local part                        |
      | user-name@example.com                       | hyphen in local part                           |
      | user_name@example.com                       | underscore in local part                       |
      | user%tag@example.com                        | percent in local part                          |
      | 123@example.com                             | all-digit local part                           |
      | user@sub.example.com                        | single subdomain                               |
      | user@a.b.c.example.com                      | deep subdomain chain                           |
      | user@example.co.uk                          | two-part TLD                                   |
      | user@example.org                            | .org TLD                                       |
      | user@example.net                            | .net TLD                                       |
      | user@example.info                           | .info TLD                                      |
      | user@example.travel                         | 6-char TLD                                     |
      | very.long.local.part.here@example.com       | long local part with dots                      |
      | user@very-long-domain-name.example.com      | hyphen in domain label                         |
      | user@123domain.com                          | digit-leading domain label                     |
      | u+tag_1%2@sub-domain.example.co.uk          | many special local chars + multi-level TLD     |
      | a.b.c.d.e@f.g.h.io                          | multiple dots in both parts                    |
      | test+filter@mail.server.co.nz               | plus-tag with multi-part domain and TLD        |
      | ALICE@EXAMPLE.ORG                           | uppercase throughout                           |

  # ---------------------------------------------------------------------------
  # Block B — isValidEmail returns FALSE (negative equivalence + boundary)
  # ---------------------------------------------------------------------------

  @validation @negative @boundary
  Scenario Outline: isValidEmail rejects malformed email addresses
    When I check validation email "<email>"
    Then the validation result is "false"

    Examples:
      | email                               | notes                                              |
      | plainaddress                        | no @ at all                                        |
      | @nodomain.com                       | empty local part                                   |
      | noatsign.com                        | no @ sign                                          |
      | user@                               | no domain after @                                  |
      | user@.com                           | domain starts with dot                             |
      | user@com.                           | TLD ends with dot (trailing dot after TLD)         |
      | user@.                              | domain is just a dot                               |
      | user@@example.com                   | double @                                           |
      | user @example.com                   | space in local part                                |
      | user@ example.com                   | space in domain part                               |
      | user@exam ple.com                   | space inside domain                                |
      | user@example.c                      | TLD is only 1 char                                 |
      | user@example.12                     | TLD is all-digits                                  |
      | user@example.1a                     | TLD starts with digit — not in [A-Za-z]{2,}        |
      | [user]@example.com                  | square brackets not in local charset               |
      | user(comment)@example.com           | parentheses not in local charset                   |
      | user@[192.168.1.1]                  | IP-literal domain — not matched by regex           |
      |                                     | empty string                                       |
      | user@exam_ple.com                   | underscore in domain (not in [A-Za-z0-9.-])        |

  # ---------------------------------------------------------------------------
  # Block C — requireValidEmail raises error code for each malformed address
  # ---------------------------------------------------------------------------

  @validation @negative @regression
  Scenario Outline: requireValidEmail raises the supplied code for malformed addresses
    When I validate validation email "<email>" with code "REG_BAD_EMAIL"
    Then the validation error code is "REG_BAD_EMAIL"

    Examples:
      | email                      |
      | plainaddress               |
      | @missinglocal.com          |
      | user@                      |
      | user@.com                  |
      | user@com.                  |
      | user@@example.com          |
      | user@example.c             |
      | user@example.12            |
      | user@example.1x            |
      | [brackets]@example.com     |

  # ---------------------------------------------------------------------------
  # Block D — requireValidEmail passes for additional valid addresses
  # ---------------------------------------------------------------------------

  @validation @positive @smoke
  Scenario Outline: requireValidEmail raises no error for well-formed addresses
    When I validate validation email "<email>" with code "AUTH_BAD_EMAIL"
    Then no validation error is raised

    Examples:
      | email                         |
      | alice@omiinqa.test            |
      | bob+filter@company.co.uk      |
      | admin123@sub.domain.org       |
      | test.user_1%tag@example.info  |
      | Z@Z.io                        |
      | x@longdomainlabel.museum      |
