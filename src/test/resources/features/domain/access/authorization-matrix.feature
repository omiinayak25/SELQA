@domain @access @authorization @authz-matrix
Feature: Authorization Matrix — Actor Role x Operation Enforcement
  As the access-control platform
  I want require() to succeed or raise AC_DENIED for every role x operation combination
  So that the enforcement gate is exhaustively validated, not just spot-checked

  # AccessControlService.require() throws AC_DENIED when the actor lacks the permission.
  # AccessControlService.require() throws AC_USER_UNKNOWN when the principal is not registered.
  # Seeded role permissions are the same as in the RBAC matrix.

  Background:
    Given a clean authorization service

  # ---------------------------------------------------------------------------
  # ADMIN — require() succeeds for every operation (wildcard)
  # ---------------------------------------------------------------------------

  @positive @security @regression
  Scenario Outline: require() succeeds for an ADMIN actor on any operation
    Given principal "<principal>" has been registered with role "ADMIN"
    When  I require permission "<permission>" for principal "<principal>"
    Then  the operation succeeds

    Examples:
      | principal | permission      |
      | aAdmin1   | admin:*         |
      | aAdmin2   | user:create     |
      | aAdmin3   | user:delete     |
      | aAdmin4   | user:disable    |
      | aAdmin5   | user:read       |
      | aAdmin6   | role:assign     |
      | aAdmin7   | role:revoke     |
      | aAdmin8   | audit:view      |
      | aAdmin9   | order:read      |
      | aAdmin10  | order:manage    |
      | aAdmin11  | report:view     |
      | aAdmin12  | profile:read    |
      | aAdmin13  | order:place     |
      | aAdmin14  | catalog:read    |

  # ---------------------------------------------------------------------------
  # MANAGER — require() succeeds for seeded permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: require() succeeds for a MANAGER actor on allowed operations
    Given principal "<principal>" has been registered with role "MANAGER"
    When  I require permission "<permission>" for principal "<principal>"
    Then  the operation succeeds

    Examples:
      | principal | permission   |
      | aMgr1     | order:read   |
      | aMgr2     | order:manage |
      | aMgr3     | report:view  |
      | aMgr4     | user:read    |

  # ---------------------------------------------------------------------------
  # MANAGER — require() raises AC_DENIED on non-granted operations
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: require() raises AC_DENIED for a MANAGER actor on disallowed operations
    Given principal "<principal>" has been registered with role "MANAGER"
    When  I require permission "<permission>" for principal "<principal>"
    Then  a domain error "AC_DENIED" is raised

    Examples:
      | principal | permission      |
      | aMgrD1    | admin:*         |
      | aMgrD2    | user:create     |
      | aMgrD3    | user:delete     |
      | aMgrD4    | user:disable    |
      | aMgrD5    | role:assign     |
      | aMgrD6    | role:revoke     |
      | aMgrD7    | audit:view      |
      | aMgrD8    | profile:read    |
      | aMgrD9    | profile:update  |
      | aMgrD10   | order:place     |
      | aMgrD11   | order:cancel    |
      | aMgrD12   | catalog:read    |

  # ---------------------------------------------------------------------------
  # CUSTOMER — require() succeeds for seeded permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: require() succeeds for a CUSTOMER actor on allowed operations
    Given principal "<principal>" has been registered with role "CUSTOMER"
    When  I require permission "<permission>" for principal "<principal>"
    Then  the operation succeeds

    Examples:
      | principal | permission      |
      | aCust1    | profile:read    |
      | aCust2    | profile:update  |
      | aCust3    | order:place     |
      | aCust4    | order:cancel    |
      | aCust5    | order:read      |
      | aCust6    | catalog:read    |

  # ---------------------------------------------------------------------------
  # CUSTOMER — require() raises AC_DENIED on admin/management operations
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: require() raises AC_DENIED for a CUSTOMER actor on disallowed operations
    Given principal "<principal>" has been registered with role "CUSTOMER"
    When  I require permission "<permission>" for principal "<principal>"
    Then  a domain error "AC_DENIED" is raised

    Examples:
      | principal | permission   |
      | aCustD1   | admin:*      |
      | aCustD2   | user:create  |
      | aCustD3   | user:delete  |
      | aCustD4   | user:disable |
      | aCustD5   | role:assign  |
      | aCustD6   | role:revoke  |
      | aCustD7   | audit:view   |
      | aCustD8   | order:manage |
      | aCustD9   | report:view  |

  # ---------------------------------------------------------------------------
  # GUEST — require() succeeds only for catalog:read
  # ---------------------------------------------------------------------------

  @positive @boundary @regression
  Scenario Outline: require() succeeds for a GUEST actor only on catalog:read
    Given principal "<principal>" has been registered with role "GUEST"
    When  I require permission "<permission>" for principal "<principal>"
    Then  the operation succeeds

    Examples:
      | principal | permission   |
      | aGuest1   | catalog:read |

  # ---------------------------------------------------------------------------
  # GUEST — require() raises AC_DENIED on all non-catalog operations
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: require() raises AC_DENIED for a GUEST actor on all non-catalog operations
    Given principal "<principal>" has been registered with role "GUEST"
    When  I require permission "<permission>" for principal "<principal>"
    Then  a domain error "AC_DENIED" is raised

    Examples:
      | principal | permission      |
      | aGuestD1  | admin:*         |
      | aGuestD2  | user:create     |
      | aGuestD3  | user:delete     |
      | aGuestD4  | user:disable    |
      | aGuestD5  | user:read       |
      | aGuestD6  | role:assign     |
      | aGuestD7  | role:revoke     |
      | aGuestD8  | audit:view      |
      | aGuestD9  | order:read      |
      | aGuestD10 | order:manage    |
      | aGuestD11 | report:view     |
      | aGuestD12 | profile:read    |
      | aGuestD13 | profile:update  |
      | aGuestD14 | order:place     |
      | aGuestD15 | order:cancel    |

  # ---------------------------------------------------------------------------
  # Unknown principal — require() raises AC_USER_UNKNOWN regardless of permission
  # ---------------------------------------------------------------------------

  @negative @boundary @regression
  Scenario Outline: require() raises AC_USER_UNKNOWN for an unregistered principal
    When  I require permission "<permission>" for unknown principal "<principal>"
    Then  a domain error "AC_USER_UNKNOWN" is raised

    Examples:
      | principal   | permission    |
      | phantom1    | order:read    |
      | phantom2    | catalog:read  |
      | phantom3    | admin:*       |
      | phantom4    | profile:read  |

  # ---------------------------------------------------------------------------
  # Privilege escalation — customers cannot self-escalate
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: A customer cannot require admin-tier permissions (privilege escalation blocked)
    Given principal "<principal>" has been registered with role "CUSTOMER"
    When  I require permission "<permission>" for principal "<principal>"
    Then  a domain error "AC_DENIED" is raised

    Examples:
      | principal   | permission   |
      | escalator1  | user:create  |
      | escalator2  | user:delete  |
      | escalator3  | role:assign  |
      | escalator4  | audit:view   |
      | escalator5  | order:manage |
      | escalator6  | report:view  |

  # ---------------------------------------------------------------------------
  # Multi-role principal — gains union of all assigned role permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario: A principal with both MANAGER and CUSTOMER roles gains the union of permissions
    Given a registered principal "dual"
    And   principal "dual" is assigned role "MANAGER"
    And   principal "dual" is assigned role "CUSTOMER"
    Then  user "dual" has permission "order:read"
    And   user "dual" has permission "order:manage"
    And   user "dual" has permission "profile:read"
    And   user "dual" has permission "order:place"
    And   user "dual" has permission "catalog:read"
    And   user "dual" has permission "report:view"

  @positive @security @regression
  Scenario: A principal with MANAGER then upgraded to ADMIN gains wildcard access
    Given a registered principal "upgraded"
    And   principal "upgraded" is assigned role "MANAGER"
    And   principal "upgraded" is assigned role "ADMIN"
    Then  user "upgraded" has permission "user:delete"
    And   user "upgraded" has permission "audit:view"
    And   user "upgraded" has permission "role:assign"
