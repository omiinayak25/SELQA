@domain @validation @quality
Feature: Charset and Length Validation Matrix
  As the validation layer
  I want every length boundary and charset boundary precisely specified
  So that InputGuard and Validations protect all fields from structural violations

  # InputGuard.MAX_LENGTH = 255
  # InputGuard.SAFE_CHARSET = ^[\x20-\x7E]*$ (printable ASCII 0x20–0x7E)
  # InputGuard error codes: VALIDATION_REQUIRED, VALIDATION_TOO_LONG, VALIDATION_BAD_CHARSET
  # Validations error codes: AUTH_BLANK (requireNotBlank with caller-supplied code)

  Background:
    Given a clean validation context

  # ---------------------------------------------------------------------------
  # Block A — requireMaxLength boundary table (default MAX_LENGTH = 255)
  # ---------------------------------------------------------------------------

  @validation @boundary @negative @regression
  Scenario Outline: requireMaxLength raises VALIDATION_TOO_LONG for inputs that exceed the limit
    When I validate validation max-length <max> on field "<field>" with value of length <actual>
    Then the validation error code is "VALIDATION_TOO_LONG"

    Examples:
      | max | actual | field      | boundary-note                              |
      | 255 | 256    | username   | exactly one over the global default        |
      | 255 | 257    | username   | two over default                           |
      | 255 | 300    | username   | well over default                          |
      | 255 | 500    | email      | far above default                          |
      | 255 | 1000   | bio        | very large                                 |
      | 100 | 101    | displayName| exactly one over a custom limit            |
      | 100 | 200    | displayName| double the custom limit                    |
      | 50  | 51     | phone      | one over a short limit                     |
      | 50  | 100    | phone      | double the short limit                     |
      | 10  | 11     | pin        | one over a very short limit                |
      | 10  | 15     | pin        | 50% over a very short limit                |
      | 1   | 2      | flag       | smallest possible limit — one over         |
      | 0   | 1      | empty      | zero-length limit — any char exceeds it    |

  @validation @boundary @positive
  Scenario Outline: requireMaxLength passes when input is within the configured limit
    When I validate validation max-length <max> on field "<field>" with value of length <actual>
    Then no validation error is raised

    Examples:
      | max | actual | field      | boundary-note                              |
      | 255 | 255    | username   | exactly at global default (inclusive)      |
      | 255 | 254    | username   | one under global default                   |
      | 255 | 1      | username   | minimum non-empty                          |
      | 255 | 0      | username   | zero length — null guard does not fire     |
      | 100 | 100    | displayName| exactly at custom limit                    |
      | 100 | 99     | displayName| one under custom limit                     |
      | 100 | 0      | displayName| zero under custom limit                    |
      | 50  | 50     | phone      | exactly at short limit                     |
      | 50  | 49     | phone      | one under short limit                      |
      | 10  | 10     | pin        | exactly at very short limit                |
      | 10  | 0      | pin        | zero under very short limit                |
      | 1   | 1      | flag       | exactly at smallest limit                  |
      | 1   | 0      | flag       | zero under smallest limit                  |

  # ---------------------------------------------------------------------------
  # Block B — requireSafeCharset: ASCII boundary table
  # Charset [\x20-\x7E]: 0x20=SPACE (32) to 0x7E=TILDE (126) inclusive
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: requireSafeCharset passes for all printable ASCII characters
    When I validate validation charset on field "input" with value "<value>"
    Then no validation error is raised

    Examples:
      | value                       | charset-note                                    |
      | hello world                 | basic letters and space                         |
      | Alice123                    | alphanumeric                                    |
      | user@example.com            | email with @ and dot                            |
      | !@#$%^&*()                  | common punctuation                              |
      | UPPER_lower-1234            | underscore and hyphen                           |
      | ~`{}[]                      | tilde, backtick, braces — no pipe               |
      | !#$%&()*+,-./               | printable ASCII punctuation — no quotes or pipe  |
      | 0123456789:;<=>?@           | digits and symbol run                           |
      | ABCDEFGHIJKLMNOPQRSTUVWXYZ  | uppercase letters                               |
      | abcdefghijklmnopqrstuvwxyz  | lowercase letters                               |
      | pqrstuvwxyz{}~              | lowercase letters and final printable chars     |
      |                             | single space (0x20 — minimum printable)         |
      | ~                           | tilde (0x7E — maximum printable)                |
      | P@ssw0rd!                   | password-like value with specials               |
      | /path/to/resource           | forward slash in field                          |
      | 192.168.0.1                 | IP address format                               |

  @validation @boundary @negative @regression
  Scenario Outline: requireSafeCharset raises VALIDATION_BAD_CHARSET for non-printable or non-ASCII chars
    When I validate validation charset on field "input" with value "<value>"
    Then the validation error code is "VALIDATION_BAD_CHARSET"

    Examples:
      | value              | charset-note                                     |
      | café               | 'é' is U+00E9 (0xE9 > 0x7E)                      |
      | naïve              | 'ï' is U+00EF                                    |
      | résumé             | 'é' repeated                                     |
      | Ünïcödé            | multiple non-ASCII chars                         |
      | Straße             | ß is U+00DF — beyond 0x7E                        |
      | Ünïcödé            | multiple non-ASCII Latin chars                   |
      | Привет             | Cyrillic chars — all > 0x7E                      |

  # ---------------------------------------------------------------------------
  # Block C — InputGuard.requireNotBlank: VALIDATION_REQUIRED boundary table
  # Tests null, empty-string, and whitespace-only inputs for multiple field names
  # ---------------------------------------------------------------------------

  @validation @boundary @negative @regression
  Scenario Outline: InputGuard.requireNotBlank raises VALIDATION_REQUIRED for blank inputs
    When I validate validation required on field "<field>" with value "<value>"
    Then the validation error code is "VALIDATION_REQUIRED"

    Examples:
      | field        | value  | blank-class                                    |
      | username     |        | empty string                                   |
      | email        |        | empty string                                   |
      | password     |        | empty string                                   |
      | token        |        | empty string                                   |
      | phone        |        | empty string                                   |
      | displayName  |        | empty string                                   |
      | bio          |        | empty string                                   |
      | firstName    |        | empty string                                   |
      | lastName     |        | empty string                                   |
      | username     | <null> | null value — mapped to null in step            |
      | email        | <null> | null value                                     |
      | token        | <null> | null value                                     |
      | path         | <null> | null value                                     |

  @validation @boundary @positive
  Scenario Outline: InputGuard.requireNotBlank passes for non-blank field values
    When I validate validation required on field "<field>" with value "<value>"
    Then no validation error is raised

    Examples:
      | field        | value              | note                           |
      | username     | alice              | regular username               |
      | email        | a@b.io             | short valid email              |
      | password     | Sup3rSecret        | strong password                |
      | token        | VRFY-abc-123       | token format                   |
      | phone        | +1-555-0100        | phone with special chars       |
      | displayName  | Alice Smith        | display name with space        |
      | bio          | Hello, world!      | bio with punctuation           |
      | firstName    | 名                 | single Unicode char            |
      | username     |  a                 | leading space then char        |

  # ---------------------------------------------------------------------------
  # Block D — Validations.isBlank / requireNotBlank comprehensive matrix
  # Covers every whitespace variant that isBlank treats as blank
  # ---------------------------------------------------------------------------

  @validation @boundary @positive
  Scenario Outline: isBlank returns true for all whitespace-only and null-equivalent strings
    When I check validation blank on "<value>"
    Then the validation result is "true"

    Examples:
      | value | blank-class              |
      |       | empty string             |

  @validation @boundary @negative
  Scenario Outline: isBlank returns false for strings with at least one non-whitespace character
    When I check validation blank on "<value>"
    Then the validation result is "false"

    Examples:
      | value     | note                                |
      | a         | single char                         |
      | 0         | digit zero                          |
      | .         | dot                                 |
      | !         | exclamation                         |
      |  a        | leading space then char             |
      | a         | trailing space after char           |
      | hello     | normal word                         |
      | Alice 123 | word with space and digit           |
      | @         | at-sign                             |
      | -         | hyphen                              |
      | _         | underscore                          |

  # ---------------------------------------------------------------------------
  # Block E — Validations.requireNotBlank with caller-supplied code
  # Covers all field + code combinations used across identity domain
  # ---------------------------------------------------------------------------

  @validation @boundary @negative @regression
  Scenario Outline: Validations.requireNotBlank raises the caller's error code for blank values
    When I validate validation not-blank field "<field>" value "<value>" code "<code>"
    Then the validation error code is "<code>"

    Examples:
      | field    | value | code               | domain-usage                     |
      | username |       | AUTH_BLANK         | AuthService.register             |
      | username |       | REG_BLANK          | RegistrationService.begin        |
      | email    |       | AUTH_BLANK         | AuthService identity check       |
      | password |       | AUTH_BLANK         | AuthService identity check       |
      | token    |       | AUTH_BLANK         | Generic token field              |
      | field1   |       | CUSTOM_BLANK       | Any caller-defined code          |
      | field2   |       | MY_ERR             | Any caller-defined code          |

  @validation @boundary @positive
  Scenario Outline: Validations.requireNotBlank passes for non-blank values with any caller code
    When I validate validation not-blank field "<field>" value "<value>" code "<code>"
    Then no validation error is raised

    Examples:
      | field    | value        | code              |
      | username | alice        | AUTH_BLANK        |
      | email    | x@y.io       | REG_BLANK         |
      | token    | abc-123      | AUTH_BLANK        |
      | field1   | notempty     | CUSTOM_BLANK      |

  # ---------------------------------------------------------------------------
  # Block F — assertSafe passes for clean inputs and raises for attack payloads
  # Covers all three threat classes: SQLi, XSS, path traversal
  # ---------------------------------------------------------------------------

  @validation @positive @security @smoke
  Scenario Outline: assertSafe passes for clean, threat-free inputs
    When I run validation assertSafe on "<input>"
    Then no validation error is raised

    Examples:
      | input                     | note                          |
      | cleanuser99               | standard username             |
      | Alice Smith               | name with space               |
      | test@email.com            | email format                  |
      | Sup3rSecret               | password                      |
      | report2024                | identifier                    |
      | hello-world               | hyphen                        |
      | order_12345               | underscore and digits         |
      | The quick brown fox       | natural language              |
      | item.description          | dot-separated name            |
      | 100.00                    | decimal number                |

  @validation @negative @security @regression
  Scenario Outline: assertSafe raises SEC_SQLI for SQL-injection payloads
    When I run validation assertSafe on "<input>"
    Then a domain error "SEC_SQLI" is raised

    Examples:
      | input                            | sqli-class                   |
      | ' OR '1'='1                      | classic tautology            |
      | ' OR 1=1 --                      | comment bypass               |
      | admin' --                        | admin bypass                 |
      | ' OR '1'='1' /*                  | block-comment bypass         |
      | ' UNION SELECT NULL --           | UNION exfiltration           |
      | 1' AND '1'='2                    | conditional injection        |
      | ') OR ('1'='1                    | parenthesis variant          |

  @validation @negative @security @regression
  Scenario Outline: assertSafe raises SEC_XSS for XSS payloads
    When I run validation assertSafe on "<input>"
    Then a domain error "SEC_XSS" is raised

    Examples:
      | input                                     | xss-class                   |
      | <script>alert('xss')</script>             | classic script tag          |
      | <img src=x onerror=alert(1)>              | img onerror                 |
      | javascript:alert(document.cookie)        | javascript: URI             |
      | <svg/onload=alert(1)>                     | SVG onload                  |
      | <body onload=alert('xss')>               | body event                  |

  @validation @negative @security @regression
  Scenario Outline: assertSafe raises SEC_TRAVERSAL for path-traversal payloads
    When I run validation assertSafe on "<input>"
    Then a domain error "SEC_TRAVERSAL" is raised

    Examples:
      | input                         | traversal-class              |
      | ../../../../etc/passwd        | Unix-style traversal         |
      | ..\..\..\windows\win.ini      | Windows-style traversal      |
      | %2e%2e%2f%2e%2e%2f            | URL-encoded traversal        |
