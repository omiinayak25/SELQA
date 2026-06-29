@domain @access @authorization
Feature: Authorization — Permission Enforcement
  As the access-control platform
  I want to enforce permissions when actors request protected operations
  So that a missing permission always results in a denied error and not a silent pass

  Background:
    Given a clean authorization service

  @smoke @positive
  Scenario: An admin actor is authorized for the admin wildcard
    Given principal "sysadmin" has been registered with role "ADMIN"
    When I require permission "admin:*" for principal "sysadmin"
    Then the operation succeeds

  @positive
  Scenario: A manager actor is authorized for order-read
    Given principal "mgr" has been registered with role "MANAGER"
    When I require permission "order:read" for principal "mgr"
    Then the operation succeeds

  @positive
  Scenario: A customer actor is authorized for profile-read
    Given principal "cust" has been registered with role "CUSTOMER"
    When I require permission "profile:read" for principal "cust"
    Then the operation succeeds

  @positive
  Scenario: A guest actor is authorized for catalog-read
    Given principal "visitor" has been registered with role "GUEST"
    When I require permission "catalog:read" for principal "visitor"
    Then the operation succeeds

  @negative @security
  Scenario: A customer is denied an admin permission
    Given principal "cust2" has been registered with role "CUSTOMER"
    When I require permission "admin:*" for principal "cust2"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A guest is denied an order-place permission
    Given principal "guest2" has been registered with role "GUEST"
    When I require permission "order:place" for principal "guest2"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A manager is denied user-delete permission
    Given principal "mgr2" has been registered with role "MANAGER"
    When I require permission "user:delete" for principal "mgr2"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: Requiring permission for an unknown principal raises user-unknown error
    When I require permission "order:read" for unknown principal "nobody"
    Then a domain error "AC_USER_UNKNOWN" is raised

  @security @boundary
  Scenario: Privilege escalation — a customer cannot self-escalate to admin permission
    Given principal "escalator" has been registered with role "CUSTOMER"
    When I require permission "user:create" for principal "escalator"
    Then a domain error "AC_DENIED" is raised

  @security
  Scenario: Privilege escalation — a manager cannot access admin-only operations
    Given principal "mgr3" has been registered with role "MANAGER"
    When I require permission "admin:*" for principal "mgr3"
    Then a domain error "AC_DENIED" is raised

  @positive @business
  Scenario: An admin holds every sub-permission through the wildcard
    Given principal "power" has been registered with role "ADMIN"
    When I require permission "report:view" for principal "power"
    Then the operation succeeds

  @negative @boundary
  Scenario Outline: Each role is denied permissions outside its scope
    Given principal "subject" has been registered with role "<role>"
    When I require permission "<denied>" for principal "subject"
    Then a domain error "AC_DENIED" is raised

    Examples:
      | role     | denied       |
      | GUEST    | order:place  |
      | GUEST    | profile:read |
      | CUSTOMER | order:manage |
      | CUSTOMER | user:create  |
      | MANAGER  | user:delete  |
      | MANAGER  | admin:*      |
