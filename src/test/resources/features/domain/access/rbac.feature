@domain @access @rbac
Feature: Role-Based Access Control
  As the access-control platform
  I want to manage roles and permissions for principals
  So that only authorised actors can perform protected operations

  Background:
    Given a clean access-control service

  @smoke @positive
  Scenario: Standard roles are seeded at service initialisation
    Then the standard roles ADMIN, MANAGER, CUSTOMER, GUEST are seeded

  @positive
  Scenario: ADMIN role grants the wildcard permission
    Then role "ADMIN" grants permission "admin:*"

  @positive
  Scenario: MANAGER role grants order-read permission
    Then role "MANAGER" grants permission "order:read"

  @positive
  Scenario: CUSTOMER role grants profile-read permission
    Then role "CUSTOMER" grants permission "profile:read"

  @positive
  Scenario: GUEST role grants catalog-read permission
    Then role "GUEST" grants permission "catalog:read"

  @negative
  Scenario: GUEST role does not grant admin permission
    Then role "GUEST" does not grant permission "admin:*"

  @negative
  Scenario: CUSTOMER role does not grant user-delete permission
    Then role "CUSTOMER" does not grant permission "user:delete"

  @positive
  Scenario: A custom role can be defined with specific permissions
    When I define a role "AUDITOR" with permissions "audit:view, report:view"
    Then the operation succeeds
    And role "AUDITOR" is defined

  @negative
  Scenario: Defining a duplicate role is rejected
    When I define a role "ADMIN" with permissions "user:read"
    Then a domain error "AC_ROLE_EXISTS" is raised

  @validation @negative
  Scenario: Defining a role with a blank name is rejected
    When I define a role "" with permissions "user:read"
    Then a domain error "AC_BLANK" is raised

  @positive
  Scenario: A principal can be assigned a role and gains its permissions
    Given a registered principal "alice"
    When I assign role "MANAGER" to principal "alice"
    Then principal "alice" has role "MANAGER"
    And user "alice" has permission "order:read"

  @positive
  Scenario: A role can be revoked from a principal
    Given a registered principal "bob"
    And principal "bob" is assigned role "CUSTOMER"
    When I revoke role "CUSTOMER" from principal "bob"
    Then principal "bob" does not have role "CUSTOMER"
    And user "bob" is denied permission "order:place"

  @negative
  Scenario: Assigning an unknown role is rejected
    Given a registered principal "carol"
    When I assign role "NONEXISTENT" to principal "carol"
    Then a domain error "AC_ROLE_UNKNOWN" is raised

  @negative
  Scenario: Assigning a role to an unknown principal is rejected
    When I assign role "CUSTOMER" to principal "ghost"
    Then a domain error "AC_USER_UNKNOWN" is raised

  @boundary @positive
  Scenario: A principal with no roles is denied all permissions
    Given a registered principal "nobody"
    Then user "nobody" is denied permission "order:read"
    And user "nobody" is denied permission "profile:read"
    And user "nobody" is denied permission "catalog:read"

  @security @positive
  Scenario: ADMIN wildcard grants every specific permission
    Given a registered principal "superadmin"
    And principal "superadmin" is assigned role "ADMIN"
    Then user "superadmin" has permission "user:create"
    And user "superadmin" has permission "user:delete"
    And user "superadmin" has permission "audit:view"
    And user "superadmin" has permission "order:manage"

  @positive @boundary
  Scenario Outline: Each standard role grants and denies expected permissions
    Given a registered principal "testuser"
    And principal "testuser" is assigned role "<role>"
    Then user "testuser" has permission "<granted>"
    And user "testuser" is denied permission "<denied>"

    Examples:
      | role     | granted       | denied       |
      | MANAGER  | order:read    | user:delete  |
      | MANAGER  | report:view   | admin:*      |
      | CUSTOMER | profile:read  | order:manage |
      | CUSTOMER | order:place   | user:create  |
      | GUEST    | catalog:read  | order:place  |
      | GUEST    | catalog:read  | profile:read |
