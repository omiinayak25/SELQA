@api @reqres @users
Feature: ReqRes user API operations
  As an API tester
  I want to validate the ReqRes user endpoints
  So that the API contract is upheld

  @smoke @regression
  Scenario: Listing users on page 1 returns HTTP 200
    When I request the ReqRes user list on page 1
    Then the API response status is 200

  @smoke @regression
  Scenario: Listing users on page 2 returns HTTP 200
    When I request the ReqRes user list on page 2
    Then the API response status is 200

  @regression
  Scenario: User list page 1 contains exactly 6 users
    When I request the ReqRes user list on page 1
    Then the user list contains 6 users

  @regression
  Scenario: User list response includes a page field
    When I request the ReqRes user list on page 1
    Then the user list JSON path "page" equals 1

  @regression
  Scenario: User list response includes a total_pages field
    When I request the ReqRes user list on page 1
    Then the user list JSON path "total_pages" is not null

  @regression
  Scenario Outline: Fetching each single user by ID returns HTTP 200
    When I request ReqRes user with id <userId>
    Then the API response status is 200

    Examples:
      | userId |
      | 1      |
      | 2      |
      | 3      |
      | 4      |
      | 5      |
      | 6      |

  @regression
  Scenario: Single user response contains an email field
    When I request ReqRes user with id 2
    Then the single user JSON path "data.email" is not null

  @regression
  Scenario: Single user response contains a first name
    When I request ReqRes user with id 1
    Then the single user JSON path "data.first_name" is not null

  @negative @regression
  Scenario: Fetching a non-existent user returns HTTP 404
    When I request ReqRes user with id 999
    Then the API response status is 404

  @regression
  Scenario: Creating a new ReqRes user returns HTTP 201
    When I create a ReqRes user with name "John Tester" and job "QA Engineer"
    Then the API response status is 201

  @regression
  Scenario: Created user response contains the submitted name
    When I create a ReqRes user with name "Jane Tester" and job "SDET"
    Then the create user response contains name "Jane Tester"

  @regression
  Scenario: Created user response contains a generated id
    When I create a ReqRes user with name "Bob Builder" and job "Developer"
    Then the create user response contains a non-null id

  @regression
  Scenario: Updating a ReqRes user with PUT returns HTTP 200
    When I update ReqRes user 2 with name "Updated Name" and job "Updated Job"
    Then the API response status is 200

  @regression
  Scenario: PATCH updating a ReqRes user returns HTTP 200
    When I patch ReqRes user 2 with name "Patched Name" and job "Patched Job"
    Then the API response status is 200

  @regression
  Scenario: Deleting a ReqRes user returns HTTP 204
    When I delete ReqRes user with id 2
    Then the API response status is 204

  @regression
  Scenario: Content-Type header is application/json for user list
    When I request the ReqRes user list on page 1
    Then the response content type contains "application/json"
