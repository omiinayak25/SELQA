@domain @identity @validation
Feature: Password Policy Matrix
  As the identity platform
  I want every password-strength boundary case covered with an exact failing rule
  So that the policy rejects every class of weak credential precisely

  # Policy: length >= 8, contains [A-Z], contains [a-z], contains [0-9]
  # All four conditions must hold simultaneously.

  Background:
    Given a clean validation context

  # ---------------------------------------------------------------------------
  # Block A — isStrongPassword returns TRUE
  # Covers: exactly-8-char boundary, various character combinations
  # ---------------------------------------------------------------------------

  @validation @positive @boundary
  Scenario Outline: isStrongPassword returns true for passwords satisfying all four rules
    When I check validation password strength "<password>"
    Then the validation result is "true"

    Examples:
      | password              | rule-notes                                        |
      | Abcdefg1              | exactly 8 chars — lower boundary met              |
      | Password1             | 9 chars standard                                  |
      | Sup3rSecret           | mixed case + digit mid-word                       |
      | A1bCdEfGhIjKlMn       | long alternating                                  |
      | UPPER1lower           | upper+digit before lower                          |
      | lower1UPPER           | lower+digit before upper                          |
      | 1Abcdefg              | digit first                                       |
      | Abcdefg1              | digit last                                        |
      | aB3dEfGh              | 8-char with digit in position 3                   |
      | ZzZzZzZ9              | alternating case + trailing digit                 |
      | MyP@ssw0rd            | special char (not required but allowed)           |
      | C0mplexP4ssw0rd       | multiple digits                                   |
      | xX1yY2zZ3             | all three criteria met multiple times             |
      | Aa000000              | minimum upper + lower + many digits               |
      | AAAAAa1b              | many uppers, single lower, single digit           |
      | aaaaA1bb              | many lowers, single upper, single digit           |
      | Aabcdef9              | upper at start, digit at end                      |
      | abcdefG1              | lower chars + upper + digit — 8 chars             |
      | Correct1H             | 9 chars, all rules met                            |

  # ---------------------------------------------------------------------------
  # Block B — isStrongPassword returns FALSE
  # Each row isolates exactly ONE failing rule
  # ---------------------------------------------------------------------------

  @validation @negative @boundary
  Scenario Outline: isStrongPassword returns false when exactly one rule is violated
    When I check validation password strength "<password>"
    Then the validation result is "false"

    Examples:
      | password        | violated-rule                                   |
      | Abcdefg         | length < 8 (7 chars), has upper+lower, no digit  |
      | Ab1             | length < 8 (3 chars) — all other rules met       |
      | ABCDEF1         | no lowercase — 7 chars also < 8                  |
      | abcdef1         | no uppercase — 7 chars also < 8                  |
      | Abcdefgh        | no digit — length=8 otherwise valid              |
      | abcdefgh        | no uppercase, no digit                           |
      | ABCDEFGH        | no lowercase, no digit                           |
      | 12345678        | no uppercase, no lowercase                       |
      | alllowercase    | no uppercase, no digit                           |
      | ALLUPPERCASE    | no lowercase, no digit                           |
      | NoDigitHere     | no digit — 11 chars otherwise ok                 |
      | 1234567A        | no lowercase — 8 chars                           |
      | 1234567a        | no uppercase — 8 chars                           |
      | aA              | too short — only 2 chars                         |
      |                 | empty string — all rules violated                |
      | Short1A         | exactly 7 chars — below minimum                  |

  # ---------------------------------------------------------------------------
  # Block C — requireStrongPassword raises PWD_WEAK for each weak password
  # Covering all distinct password-weakness classes
  # ---------------------------------------------------------------------------

  @validation @negative @regression
  Scenario Outline: requireStrongPassword raises the supplied code for every class of weak password
    When I validate validation password "<password>" with code "PWD_WEAK"
    Then the validation error code is "PWD_WEAK"

    Examples:
      | password          | weakness-class                     |
      | short1A           | too-short                          |
      | alllowercase1     | missing uppercase                  |
      | ALLUPPERCASE1     | missing lowercase                  |
      | NoDigitHere       | missing digit                      |
      | 12345678          | missing upper and lower            |
      | Abcdefg           | too-short (7) + missing digit      |
      |                   | empty                              |
      | aB3               | too-short (3)                      |
      | AAAAAAAA          | missing lower and digit            |
      | aaaaaaaa          | missing upper and digit            |

  # ---------------------------------------------------------------------------
  # Block D — requireStrongPassword passes for strong passwords
  # More variety than the existing feature to cover additional combinations
  # ---------------------------------------------------------------------------

  @validation @positive @smoke
  Scenario Outline: requireStrongPassword passes for passwords meeting all criteria
    When I validate validation password "<password>" with code "AUTH_WEAK_PASSWORD"
    Then no validation error is raised

    Examples:
      | password          |
      | Abcdefg1          |
      | Z1abcdef          |
      | xX1yY2zZ          |
      | Secret99X         |
      | Tr0ub4dor         |
      | hunter2A          |
      | P4ssword          |
      | 8CharsOk          |

  # ---------------------------------------------------------------------------
  # Block E — AUTH service rejects weak passwords at registration boundary
  # Distinct from Block C — exercises AuthService.register path not just Validations
  # ---------------------------------------------------------------------------

  @validation @negative @boundary
  Scenario Outline: Authentication service rejects weak passwords at registration with AUTH_WEAK_PASSWORD
    When I register a user with username "testuser" email "test@omiinqa.test" and password "<password>"
    Then a domain error "AUTH_WEAK_PASSWORD" is raised

    Examples:
      | password       |
      | short1A        |
      | alllower123    |
      | ALLUPPER123    |
      | NoDigitHere    |
      | 12345678       |
      | Abcdefg        |
