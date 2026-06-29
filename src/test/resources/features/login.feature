@ui @saucedemo
Feature: SauceDemo authentication
  As a shopper
  I want to sign in to SauceDemo
  So that I can browse and purchase products

  Background:
    Given the user is on the SauceDemo login page

  @smoke
  Scenario: Standard user signs in successfully
    When the user logs in as "standard_user" with password "secret_sauce"
    Then the products page is displayed
    And the inventory contains 6 products

  @negative
  Scenario: Locked-out user is rejected
    When the user logs in as "locked_out_user" with password "secret_sauce"
    Then a login error containing "locked out" is shown

  @negative
  Scenario Outline: Invalid credentials are rejected
    When the user logs in as "<username>" with password "<password>"
    Then a login error containing "<message>" is shown

    Examples:
      | username       | password       | message              |
      | standard_user  | wrong_password | do not match         |
      | ghost_user     | secret_sauce   | do not match         |
      |                | secret_sauce   | Username is required |
      | standard_user  |                | Password is required |
