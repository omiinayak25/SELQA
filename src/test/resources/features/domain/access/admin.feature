@domain @access @admin
Feature: Admin Service — Privileged Operations
  As the admin platform
  I want admin actors to perform privileged user management
  So that only authorised admins can create, disable, and manage users

  Background:
    Given a clean admin service
    And an admin actor "root"

  @smoke @positive
  Scenario: An admin can create a new managed user
    When admin "root" creates user "newbie"
    Then the operation succeeds
    And managed user "newbie" exists

  @positive
  Scenario: An admin can disable a managed user
    Given a managed user "victim" created by admin "root"
    When admin "root" disables user "victim"
    Then the operation succeeds
    And managed user "victim" is disabled

  @positive
  Scenario: An admin can assign a role to a managed user
    Given a managed user "assignee" created by admin "root"
    When admin "root" assigns role "MANAGER" to user "assignee"
    Then the operation succeeds
    And principal "assignee" has role "MANAGER"

  @positive
  Scenario: Creating a user records an audit log entry
    When admin "root" creates user "audited"
    Then the audit log is not empty
    And the audit log entry 1 contains "CREATE_USER"

  @positive
  Scenario: An admin can view the audit configuration
    When actor "root" attempts to view audit config
    Then the operation succeeds

  @negative @security
  Scenario: A non-admin actor is denied user creation
    Given a non-admin actor "unprivileged" with role "CUSTOMER"
    When actor "unprivileged" attempts to create user "illegaluser"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A manager actor is denied user creation
    Given a non-admin actor "mgr" with role "MANAGER"
    When actor "mgr" attempts to create user "illegaluser2"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A non-admin actor is denied user disable
    Given a managed user "target" created by admin "root"
    And a non-admin actor "attacker" with role "CUSTOMER"
    When actor "attacker" attempts to disable user "target"
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A non-admin actor is denied audit config access
    Given a non-admin actor "spy" with role "GUEST"
    When actor "spy" attempts to view audit config
    Then a domain error "AC_DENIED" is raised

  @negative @security
  Scenario: A non-admin cannot assign roles
    Given a managed user "target2" created by admin "root"
    And a non-admin actor "usurper" with role "MANAGER"
    When actor "usurper" attempts to assign role "ADMIN" to user "target2"
    Then a domain error "AC_DENIED" is raised

  @negative @validation
  Scenario: Creating a user with a blank name is rejected
    When admin "root" creates user ""
    Then a domain error "ADMIN_BLANK" is raised

  @negative @boundary
  Scenario: Creating a duplicate user is rejected
    Given a managed user "dupe" created by admin "root"
    When admin "root" creates user "dupe"
    Then a domain error "ADMIN_USER_EXISTS" is raised

  @negative @boundary
  Scenario: Disabling a non-existent user is rejected
    When admin "root" disables user "phantom"
    Then a domain error "ADMIN_USER_NOT_FOUND" is raised

  @positive @business
  Scenario: Multiple admin operations accumulate audit log entries
    When admin "root" creates user "u1"
    And admin "root" creates user "u2"
    And admin "root" disables user "u1"
    Then the audit log contains 3 entries
