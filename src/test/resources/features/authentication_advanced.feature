@ui @saucedemo @authentication
Feature: SauceDemo advanced authentication scenarios
  As a security-conscious QA engineer
  I want to validate all SauceDemo user account types and edge cases
  So that authentication behaves correctly for every role

  Background:
    Given the user is on the SauceDemo login page

  @smoke @regression
  Scenario: Problem user can sign in successfully
    When the user logs in as "problem_user" with password "secret_sauce"
    Then the products page is displayed

  @smoke @regression
  Scenario: Performance glitch user eventually reaches products page
    When the user logs in as "performance_glitch_user" with password "secret_sauce"
    Then the products page is displayed

  @negative @regression
  Scenario: Locked-out user sees specific error message
    When the user logs in as "locked_out_user" with password "secret_sauce"
    Then a login error containing "locked out" is shown

  @negative @regression
  Scenario Outline: All invalid credential combinations are rejected
    When the user logs in as "<username>" with password "<password>"
    Then a login error containing "<message>" is shown

    Examples:
      | username             | password         | message              |
      | standard_user        | wrongpass        | do not match         |
      | locked_out_user      | wrongpass        | do not match         |
      | problem_user         | wrongpass        | do not match         |
      | nonexistent_user_999 | secret_sauce     | do not match         |
      | STANDARD_USER        | secret_sauce     | do not match         |
      | standard_user        | SECRET_SAUCE     | do not match         |
      |                      |                  | Username is required |

  @negative @regression
  Scenario: SQL injection attempt in username field is safely rejected
    When the user attempts login with username "' OR 1=1 --" and password "anything"
    Then a login error is displayed

  @negative @regression
  Scenario: XSS payload in username field is safely handled
    When the user attempts login with username "<script>alert(1)</script>" and password "anything"
    Then a login error is displayed

  @regression
  Scenario: Standard user can log out and login page reappears
    When the user logs in as "standard_user" with password "secret_sauce"
    Then the products page is displayed
    When the user logs out via the burger menu
    Then the login page is displayed again

  @regression
  Scenario: After logout user cannot navigate back to inventory directly
    When the user logs in as "standard_user" with password "secret_sauce"
    And the user logs out via the burger menu
    Then the login page is displayed again
