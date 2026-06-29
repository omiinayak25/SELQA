@domain @error-handling
Feature: Error Handling — Systematic Bad-Input to Error-Code Mapping
  As the domain platform
  I want every bad input combination to produce a specific, asserted error code
  So that callers always receive machine-readable failure signals with no ambiguity

  Background:
    Given a clean error-handling service

  # ---------------------------------------------------------------------------
  # AuthService.register — validation error codes
  # ---------------------------------------------------------------------------

  @error-handling @negative @regression @validation
  Scenario Outline: AuthService.register maps each bad input to the exact error code
    When I attempt error-domain registration with username "<username>" email "<email>" password "<password>"
    Then the error-domain error code is "<code>"

    Examples:
      | username | email                     | password       | code               |
      |          | good@test.com             | Sup3rSecret    | AUTH_BLANK         |
      | user1    | not-an-email              | Sup3rSecret    | AUTH_BAD_EMAIL     |
      | user1    | @nolocalpart.com          | Sup3rSecret    | AUTH_BAD_EMAIL     |
      | user1    | missing-at-sign.com       | Sup3rSecret    | AUTH_BAD_EMAIL     |
      | user1    | user@nodot                | Sup3rSecret    | AUTH_BAD_EMAIL     |
      | user1    | good@test.com             | short          | AUTH_WEAK_PASSWORD |
      | user1    | good@test.com             | alllowercase1  | AUTH_WEAK_PASSWORD |
      | user1    | good@test.com             | ALLUPPERCASE1  | AUTH_WEAK_PASSWORD |
      | user1    | good@test.com             | NoDigitsHere   | AUTH_WEAK_PASSWORD |
      | user1    | good@test.com             | 12345678       | AUTH_WEAK_PASSWORD |

  @error-handling @negative @regression
  Scenario: AuthService.register raises AUTH_DUP_USERNAME for a taken username
    Given an error-domain registered user username "dup_user" email "dup@test.com" password "Sup3rSecret1"
    When I attempt error-domain registration with username "dup_user" email "other@test.com" password "Sup3rSecret1"
    Then the error-domain error code is "AUTH_DUP_USERNAME"

  @error-handling @negative @regression
  Scenario: AuthService.register raises AUTH_DUP_EMAIL for a taken email
    Given an error-domain registered user username "user_a" email "shared@test.com" password "Sup3rSecret1"
    When I attempt error-domain registration with username "user_b" email "shared@test.com" password "Sup3rSecret1"
    Then the error-domain error code is "AUTH_DUP_EMAIL"

  # ---------------------------------------------------------------------------
  # AuthService.login — error codes
  # ---------------------------------------------------------------------------

  @error-handling @negative @regression
  Scenario: AuthService.login raises AUTH_NOT_FOUND for unknown identifier
    When I attempt error-domain login with identifier "ghost_user" password "AnyPass1"
    Then the error-domain error code is "AUTH_NOT_FOUND"

  @error-handling @negative @regression
  Scenario: AuthService.login raises AUTH_NOT_FOUND for unknown email identifier
    When I attempt error-domain login with identifier "nobody@nowhere.com" password "AnyPass1"
    Then the error-domain error code is "AUTH_NOT_FOUND"

  @error-handling @negative @regression
  Scenario: AuthService.login raises AUTH_INVALID_CREDENTIALS for wrong password
    Given an error-domain registered user username "login_user" email "login@test.com" password "Sup3rSecret1"
    When I attempt error-domain login with identifier "login_user" password "wrong_pass"
    Then the error-domain error code is "AUTH_INVALID_CREDENTIALS"

  @error-handling @negative @boundary @regression
  Scenario: AuthService.login raises AUTH_LOCKED after five consecutive failures
    Given an error-domain registered user username "lock_user" email "lock@test.com" password "Sup3rSecret1"
    When I attempt error-domain login 5 times with identifier "lock_user" password "wrong"
    Then the error-domain error code is "AUTH_LOCKED"

  # ---------------------------------------------------------------------------
  # Validations — standalone email/password/blank error codes
  # ---------------------------------------------------------------------------

  @error-handling @negative @regression
  Scenario Outline: Validations.requireValidEmail raises AUTH_BAD_EMAIL for bad addresses
    When I validate error-domain email "<email>"
    Then the error-domain error code is "AUTH_BAD_EMAIL"

    Examples:
      | email                    |
      | plainaddress             |
      | @missinglocal.com        |
      | missingat.com            |
      | user@.com                |
      | user@com.                |
      | user@nodot               |

  @error-handling @positive @smoke
  Scenario Outline: Validations.requireValidEmail accepts well-formed addresses
    When I validate error-domain email "<email>"
    Then the error-domain operation is successful

    Examples:
      | email                    |
      | user@example.com         |
      | user+tag@sub.domain.org  |
      | a.b.c@d.e.f.com          |

  @error-handling @negative @regression
  Scenario Outline: Validations.requireStrongPassword raises AUTH_WEAK_PASSWORD
    When I validate error-domain password "<password>"
    Then the error-domain error code is "AUTH_WEAK_PASSWORD"

    Examples:
      | password       |
      | short          |
      | alllowercase1  |
      | ALLUPPERCASE1  |
      | NoDigitsHere   |
      | 12345678       |
      | Ab1            |

  @error-handling @negative @regression
  Scenario Outline: Validations.requireNotBlank raises AUTH_BLANK for empty fields
    When I validate error-domain blank field "<value>" named "<field>"
    Then the error-domain error code is "AUTH_BLANK"

    Examples:
      | value  | field    |
      |        | username |
      |        | password |
      |        | email    |

  # ---------------------------------------------------------------------------
  # InputGuard — security error codes
  # ---------------------------------------------------------------------------

  @error-handling @negative @security @regression
  Scenario Outline: InputGuard.rejectSqlInjection raises SEC_SQLI for known payloads
    When I guard error-domain input "<input>" against SQL injection
    Then the error-domain error code is "SEC_SQLI"

    Examples:
      | input                     |
      | ' OR '1'='1               |
      | ' OR 1=1 --               |
      | admin' --                 |
      | ' UNION SELECT NULL --    |
      | ') OR ('1'='1             |

  @error-handling @negative @security @regression
  Scenario Outline: InputGuard.rejectXss raises SEC_XSS for known payloads
    When I guard error-domain input "<input>" against XSS
    Then the error-domain error code is "SEC_XSS"

    Examples:
      | input                                   |
      | <script>alert('xss')</script>           |
      | <img src=x onerror=alert(1)>            |
      | javascript:alert(document.cookie)       |
      | <svg/onload=alert(1)>                   |
      | <body onload=alert('xss')>              |

  @error-handling @negative @security @regression
  Scenario Outline: InputGuard.rejectPathTraversal raises SEC_TRAVERSAL
    When I guard error-domain input "<input>" against path traversal
    Then the error-domain error code is "SEC_TRAVERSAL"

    Examples:
      | input                     |
      | ../../../../etc/passwd    |
      | ..\..\..\windows\win.ini  |
      | %2e%2e%2f%2e%2e%2f        |

  @error-handling @negative @regression
  Scenario Outline: InputGuard max-length check raises VALIDATION_TOO_LONG
    When I check error-domain input "<input>" max-length <max> field "username"
    Then the error-domain error code is "VALIDATION_TOO_LONG"

    Examples:
      | input                                                                                                                                                                                                                                                                       | max |
      | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa          | 255 |
      | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa                                                                                                                                                                                                                        | 10  |

  @error-handling @positive @regression
  Scenario: InputGuard assertSafe accepts a completely clean input
    When I guard error-domain input "ValidUser99" with assertSafe
    Then the error-domain operation is successful
