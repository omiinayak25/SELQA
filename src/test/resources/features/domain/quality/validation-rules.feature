@domain @validation
Feature: Validation Rules — Boundary Tables for Email, Password, Length and Charset
  As the validation layer
  I want every field boundary condition to be precisely defined and enforced
  So that business logic only processes structurally valid, safe input

  Background:
    Given a clean validation context

  # ---------------------------------------------------------------------------
  # Email — Validations.isValidEmail boolean boundary table
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: isValidEmail returns true for valid email addresses
    When I check validation email "<email>"
    Then the validation result is "true"

    Examples:
      | email                            |
      | user@example.com                 |
      | user+tag@example.com             |
      | user.name@sub.domain.org         |
      | a@b.io                           |
      | 123@numbers.net                  |
      | user_name@dash-domain.co.uk      |
      | very.long.email.address@test.com |

  @validation @boundary @negative
  Scenario Outline: isValidEmail returns false for malformed addresses
    When I check validation email "<email>"
    Then the validation result is "false"

    Examples:
      | email                    |
      | plainaddress             |
      | @missinglocal.com        |
      | missingatsign.com        |
      | user@                    |
      | user@.com                |
      | user@com.                |
      |                          |
      | user @example.com        |
      | user@@example.com        |

  @validation @negative @regression
  Scenario Outline: requireValidEmail raises AUTH_BAD_EMAIL for malformed addresses
    When I validate validation email "<email>" with code "AUTH_BAD_EMAIL"
    Then the validation error code is "AUTH_BAD_EMAIL"

    Examples:
      | email                    |
      | notanemail               |
      | @nodomain.com            |
      | nodomain@               |
      | user@com.                |

  @validation @positive @smoke
  Scenario Outline: requireValidEmail passes for well-formed addresses
    When I validate validation email "<email>" with code "AUTH_BAD_EMAIL"
    Then no validation error is raised

    Examples:
      | email                  |
      | alice@omiinqa.test     |
      | bob.smith@company.org  |
      | test+1@example.co.uk   |

  # ---------------------------------------------------------------------------
  # Password — Validations.isStrongPassword boolean boundary table
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: isStrongPassword returns true for passwords meeting all criteria
    When I check validation password strength "<password>"
    Then the validation result is "true"

    Examples:
      | password         |
      | Sup3rSecret      |
      | Password1        |
      | Abcdefgh1        |
      | A1bcdefg         |
      | Zy9xwvut         |
      | UPPER1lower      |
      | VeryLong1Password|

  @validation @boundary @negative
  Scenario Outline: isStrongPassword returns false for passwords failing criteria
    When I check validation password strength "<password>"
    Then the validation result is "false"

    Examples:
      | password         |
      | short1A          |
      | alllower1        |
      | ALLUPPER1        |
      | NoDigitsHere     |
      | 12345678         |
      | Ab1              |
      |                  |
      | Abcdefg          |

  @validation @negative @regression
  Scenario Outline: requireStrongPassword raises AUTH_WEAK_PASSWORD for weak passwords
    When I validate validation password "<password>" with code "AUTH_WEAK_PASSWORD"
    Then the validation error code is "AUTH_WEAK_PASSWORD"

    Examples:
      | password         |
      | weak             |
      | alllowercase1    |
      | ALLUPPERCASE1    |
      | NoDigitss        |
      | 12345678         |

  @validation @positive @smoke
  Scenario Outline: requireStrongPassword passes for strong passwords
    When I validate validation password "<password>" with code "AUTH_WEAK_PASSWORD"
    Then no validation error is raised

    Examples:
      | password         |
      | Sup3rSecret      |
      | MyP@ssw0rd       |
      | A1bCdEfGh        |

  # ---------------------------------------------------------------------------
  # Blank — Validations.isBlank boolean boundary table
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: isBlank returns true for blank values
    When I check validation blank on "<value>"
    Then the validation result is "true"

    Examples:
      | value  |
      |        |

  @validation @boundary @negative
  Scenario Outline: isBlank returns false for non-blank values
    When I check validation blank on "<value>"
    Then the validation result is "false"

    Examples:
      | value   |
      | a       |
      | hello   |
      | 1       |
      |  a      |

  @validation @negative @regression
  Scenario Outline: requireNotBlank raises AUTH_BLANK for blank or whitespace-only fields
    When I validate validation not-blank field "<field>" value "<value>" code "AUTH_BLANK"
    Then the validation error code is "AUTH_BLANK"

    Examples:
      | field    | value  |
      | username |        |
      | email    |        |
      | password |        |

  @validation @positive
  Scenario: requireNotBlank passes for a non-blank value
    When I validate validation not-blank field "username" value "alice" code "AUTH_BLANK"
    Then no validation error is raised

  # ---------------------------------------------------------------------------
  # Length — InputGuard.requireMaxLength boundary table
  # ---------------------------------------------------------------------------

  @validation @boundary @negative @regression
  Scenario Outline: requireMaxLength raises VALIDATION_TOO_LONG when length exceeds max
    When I validate validation max-length <max> on field "input" with value of length <actual>
    Then the validation error code is "VALIDATION_TOO_LONG"

    Examples:
      | max | actual |
      | 10  | 11     |
      | 10  | 100    |
      | 50  | 51     |
      | 50  | 200    |
      | 100 | 101    |
      | 255 | 256    |
      | 255 | 500    |

  @validation @boundary @positive
  Scenario Outline: requireMaxLength passes when length is within max
    When I validate validation max-length <max> on field "input" with value of length <actual>
    Then no validation error is raised

    Examples:
      | max | actual |
      | 10  | 0      |
      | 10  | 1      |
      | 10  | 10     |
      | 50  | 50     |
      | 255 | 255    |
      | 255 | 0      |

  # ---------------------------------------------------------------------------
  # Charset — InputGuard.requireSafeCharset boundary table
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: requireSafeCharset accepts printable-ASCII strings
    When I validate validation charset on field "input" with value "<value>"
    Then no validation error is raised

    Examples:
      | value                |
      | hello world          |
      | Alice123             |
      | user@example.com     |
      | !@#$%^&*()           |
      | UPPER_lower-1234     |
      | ~`{}[]               |

  @validation @boundary @negative @regression
  Scenario Outline: requireSafeCharset raises VALIDATION_BAD_CHARSET for non-ASCII or control chars
    When I validate validation charset on field "input" with value "<value>"
    Then the validation error code is "VALIDATION_BAD_CHARSET"

    Examples:
      | value         |
      | café          |
      | naïve         |
      | résumé        |

  # ---------------------------------------------------------------------------
  # InputGuard.requireNotBlank — VALIDATION_REQUIRED boundary
  # ---------------------------------------------------------------------------

  @validation @boundary @negative @regression
  Scenario Outline: InputGuard.requireNotBlank raises VALIDATION_REQUIRED for blank inputs
    When I validate validation required on field "<field>" with value "<value>"
    Then the validation error code is "VALIDATION_REQUIRED"

    Examples:
      | field    | value  |
      | username |        |
      | email    |        |
      | path     | <null> |
      | token    |        |

  @validation @positive
  Scenario: InputGuard.requireNotBlank passes for a non-blank value
    When I validate validation required on field "username" with value "alice"
    Then no validation error is raised

  # ---------------------------------------------------------------------------
  # assertSafe — multi-threat safe inputs pass all checks
  # ---------------------------------------------------------------------------

  @validation @positive @smoke
  Scenario Outline: assertSafe passes for clean, threat-free inputs
    When I run validation assertSafe on "<input>"
    Then no validation error is raised

    Examples:
      | input          |
      | validUser99    |
      | Alice Smith    |
      | test@email.com |
      | Sup3rSecret    |
      | report2024     |
