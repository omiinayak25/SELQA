@domain @security
Feature: Input Security — Injection and Payload Rejection
  As the security layer
  I want every known attack payload to be detected and rejected before reaching business logic
  So that SQL injection, XSS, and path-traversal attacks never succeed

  Background:
    Given a clean security input guard

  # ---------------------------------------------------------------------------
  # SQL Injection — InputGuard.rejectSqlInjection
  # Note: Gherkin {string} is delimited by double-quotes so we use the payloads
  # from SecurityPayloads that do not themselves embed a literal double-quote.
  # ---------------------------------------------------------------------------

  @security @negative @regression
  Scenario Outline: InputGuard rejects every known SQL-injection payload
    When I guard against SQL injection with payload "<payload>"
    Then the security guard rejects the input as "SEC_SQLI"

    Examples:
      | payload                     |
      | ' OR '1'='1                 |
      | ' OR 1=1 --                 |
      | admin' --                   |
      | ' OR '1'='1' /*             |
      | ' UNION SELECT NULL --      |
      | 1' AND '1'='2               |
      | ') OR ('1'='1               |

  @security @positive @smoke
  Scenario: InputGuard accepts a clean username via SQL injection check
    When I guard against SQL injection with payload "alice_admin"
    Then the security guard accepts the input

  @security @positive
  Scenario: InputGuard accepts an empty string via SQL injection check (no payload = no threat)
    When I guard against SQL injection with payload ""
    Then the security guard accepts the input

  @security @positive
  Scenario: InputGuard accepts a plain numeric string via SQL injection check
    When I guard against SQL injection with payload "12345"
    Then the security guard accepts the input

  # ---------------------------------------------------------------------------
  # XSS — InputGuard.rejectXss
  # Payloads from SecurityPayloads.XSS that have no embedded double-quote.
  # ---------------------------------------------------------------------------

  @security @negative @regression
  Scenario Outline: InputGuard rejects every known XSS payload
    When I guard against XSS with payload "<payload>"
    Then the security guard rejects the input as "SEC_XSS"

    Examples:
      | payload                              |
      | <script>alert('xss')</script>        |
      | <img src=x onerror=alert(1)>         |
      | javascript:alert(document.cookie)    |
      | <svg/onload=alert(1)>                |
      | <body onload=alert('xss')>           |

  @security @positive
  Scenario: InputGuard accepts a clean display name via XSS check
    When I guard against XSS with payload "John Smith"
    Then the security guard accepts the input

  @security @positive
  Scenario: InputGuard accepts an HTML-entity-encoded string that has no live XSS
    When I guard against XSS with payload "Hello and World"
    Then the security guard accepts the input

  @security @positive
  Scenario: InputGuard accepts a plain email address via XSS check
    When I guard against XSS with payload "user@example.com"
    Then the security guard accepts the input

  # ---------------------------------------------------------------------------
  # Path Traversal — InputGuard.rejectPathTraversal
  # ---------------------------------------------------------------------------

  @security @negative @regression
  Scenario Outline: InputGuard rejects every known path-traversal payload
    When I guard against path traversal with payload "<payload>"
    Then the security guard rejects the input as "SEC_TRAVERSAL"

    Examples:
      | payload                          |
      | ../../../../etc/passwd           |
      | ..\..\..\windows\win.ini         |
      | %2e%2e%2f%2e%2e%2f               |

  @security @positive
  Scenario: InputGuard accepts a relative path without traversal sequences
    When I guard against path traversal with payload "reports/2024/summary.csv"
    Then the security guard accepts the input

  @security @positive
  Scenario: InputGuard accepts a simple filename via path traversal check
    When I guard against path traversal with payload "invoice.pdf"
    Then the security guard accepts the input

  # ---------------------------------------------------------------------------
  # assertSafe — combined SQLi + XSS + traversal in one call
  # ---------------------------------------------------------------------------

  @security @negative @sanity
  Scenario Outline: assertSafe rejects combined-threat payloads with the right SEC_* code
    When I assert input is safe with payload "<payload>"
    Then no injection succeeds through the guard

    Examples:
      | payload                           |
      | ' OR '1'='1                       |
      | <script>alert('xss')</script>     |
      | ../../../../etc/passwd            |
      | admin' --                         |
      | <img src=x onerror=alert(1)>      |
      | %2e%2e%2f%2e%2e%2f                |
      | ' UNION SELECT NULL --            |
      | <svg/onload=alert(1)>             |
      | ' OR 1=1 --                       |
      | javascript:alert(document.cookie) |

  @security @positive @smoke
  Scenario: assertSafe passes a completely clean input
    When I assert input is safe with payload "ValidUser99"
    Then the security guard accepts the input

  @security @positive
  Scenario: assertSafe passes a clean email address
    When I assert input is safe with payload "alice@omiinqa.test"
    Then the security guard accepts the input

  # ---------------------------------------------------------------------------
  # Auth registration blocked for hostile usernames
  # ---------------------------------------------------------------------------

  @security @negative @regression
  Scenario Outline: AuthService registration is blocked when the username contains a SQLi payload
    When I register a security user with SQLi username "<username>" and safe email "safe@test.com" password "Safe1Pass"
    Then the security guard rejects the input as "SEC_SQLI"

    Examples:
      | username               |
      | ' OR '1'='1            |
      | admin' --              |
      | ' OR 1=1 --            |
      | ' UNION SELECT NULL -- |
      | ') OR ('1'='1          |
      | 1' AND '1'='2          |

  @security @negative @regression
  Scenario Outline: AuthService registration is blocked when the username contains an XSS payload
    When I register a security user with XSS username "<username>" and safe email "safe@test.com" password "Safe1Pass"
    Then the security guard rejects the input as "SEC_XSS"

    Examples:
      | username                          |
      | <script>alert('xss')</script>     |
      | <img src=x onerror=alert(1)>      |
      | javascript:alert(document.cookie) |
      | <svg/onload=alert(1)>             |
      | <body onload=alert('xss')>        |

  # ---------------------------------------------------------------------------
  # Auth login blocked for hostile identifiers
  # ---------------------------------------------------------------------------

  @security @negative @regression
  Scenario Outline: AuthService login identifier blocked for SQLi payloads
    When I login a security user with SQLi identifier "<identifier>" and password "AnyPass1"
    Then the security guard rejects the input as "SEC_SQLI"

    Examples:
      | identifier             |
      | ' OR '1'='1            |
      | admin' --              |
      | 1' AND '1'='2          |
      | ') OR ('1'='1          |
      | ' OR '1'='1' /*        |
      | ' OR 1=1 --            |

  @security @negative @regression
  Scenario Outline: AuthService login identifier blocked for XSS payloads
    When I login a security user with XSS identifier "<identifier>" and password "AnyPass1"
    Then the security guard rejects the input as "SEC_XSS"

    Examples:
      | identifier                        |
      | <script>alert('xss')</script>     |
      | javascript:alert(document.cookie) |
      | <body onload=alert('xss')>        |
      | <img src=x onerror=alert(1)>      |
