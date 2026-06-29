@domain @access @rbac @rbac-matrix
Feature: RBAC Permission Matrix — Every Role x Every Permission
  As the access-control platform
  I want every role to grant exactly the permissions it was seeded with and deny all others
  So that no permission boundary drift goes undetected

  # Seeded permissions (from AccessControlService.seedStandardRoles()):
  #   ADMIN    : admin:*                                                  (wildcard)
  #   MANAGER  : order:read, order:manage, report:view, user:read
  #   CUSTOMER : profile:read, profile:update, order:place, order:cancel, order:read, catalog:read
  #   GUEST    : catalog:read

  Background:
    Given a clean access-control service

  # ---------------------------------------------------------------------------
  # ADMIN wildcard grants every specific permission we care about
  # ---------------------------------------------------------------------------

  @positive @security @regression
  Scenario Outline: ADMIN role grants every permission through the wildcard
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "ADMIN"
    Then  user "<principal>" has permission "<permission>"

    Examples:
      | principal | permission      |
      | adm1      | admin:*         |
      | adm2      | user:create     |
      | adm3      | user:delete     |
      | adm4      | user:disable    |
      | adm5      | user:read       |
      | adm6      | role:assign     |
      | adm7      | role:revoke     |
      | adm8      | audit:view      |
      | adm9      | order:read      |
      | adm10     | order:manage    |
      | adm11     | report:view     |
      | adm12     | profile:read    |
      | adm13     | profile:update  |
      | adm14     | order:place     |
      | adm15     | order:cancel    |
      | adm16     | catalog:read    |

  # ---------------------------------------------------------------------------
  # MANAGER — granted permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: MANAGER role grants only its seeded permissions
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "MANAGER"
    Then  user "<principal>" has permission "<permission>"

    Examples:
      | principal | permission   |
      | mgr1      | order:read   |
      | mgr2      | order:manage |
      | mgr3      | report:view  |
      | mgr4      | user:read    |

  # ---------------------------------------------------------------------------
  # MANAGER — denied permissions
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: MANAGER role does not grant admin or customer-only permissions
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "MANAGER"
    Then  user "<principal>" is denied permission "<permission>"

    Examples:
      | principal | permission      |
      | mgrD1     | admin:*         |
      | mgrD2     | user:create     |
      | mgrD3     | user:delete     |
      | mgrD4     | user:disable    |
      | mgrD5     | role:assign     |
      | mgrD6     | role:revoke     |
      | mgrD7     | audit:view      |
      | mgrD8     | profile:read    |
      | mgrD9     | profile:update  |
      | mgrD10    | order:place     |
      | mgrD11    | order:cancel    |
      | mgrD12    | catalog:read    |

  # ---------------------------------------------------------------------------
  # CUSTOMER — granted permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: CUSTOMER role grants exactly its seeded permissions
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "CUSTOMER"
    Then  user "<principal>" has permission "<permission>"

    Examples:
      | principal | permission      |
      | cust1     | profile:read    |
      | cust2     | profile:update  |
      | cust3     | order:place     |
      | cust4     | order:cancel    |
      | cust5     | order:read      |
      | cust6     | catalog:read    |

  # ---------------------------------------------------------------------------
  # CUSTOMER — denied permissions
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: CUSTOMER role does not grant admin or management permissions
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "CUSTOMER"
    Then  user "<principal>" is denied permission "<permission>"

    Examples:
      | principal | permission   |
      | custD1    | admin:*      |
      | custD2    | user:create  |
      | custD3    | user:delete  |
      | custD4    | user:disable |
      | custD5    | role:assign  |
      | custD6    | role:revoke  |
      | custD7    | audit:view   |
      | custD8    | order:manage |
      | custD9    | report:view  |

  # ---------------------------------------------------------------------------
  # GUEST — granted permissions
  # ---------------------------------------------------------------------------

  @positive @business @regression
  Scenario Outline: GUEST role grants only catalog-read
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "GUEST"
    Then  user "<principal>" has permission "<permission>"

    Examples:
      | principal | permission   |
      | guest1    | catalog:read |

  # ---------------------------------------------------------------------------
  # GUEST — denied permissions
  # ---------------------------------------------------------------------------

  @negative @security @regression
  Scenario Outline: GUEST role denies every non-catalog permission
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "GUEST"
    Then  user "<principal>" is denied permission "<permission>"

    Examples:
      | principal | permission      |
      | guestD1   | admin:*         |
      | guestD2   | user:create     |
      | guestD3   | user:delete     |
      | guestD4   | user:disable    |
      | guestD5   | user:read       |
      | guestD6   | role:assign     |
      | guestD7   | role:revoke     |
      | guestD8   | audit:view      |
      | guestD9   | order:read      |
      | guestD10  | order:manage    |
      | guestD11  | report:view     |
      | guestD12  | profile:read    |
      | guestD13  | profile:update  |
      | guestD14  | order:place     |
      | guestD15  | order:cancel    |

  # ---------------------------------------------------------------------------
  # No roles — a principal with zero assigned roles is denied everything
  # ---------------------------------------------------------------------------

  @negative @boundary @regression
  Scenario Outline: A principal with no roles is denied every permission
    Given a registered principal "<principal>"
    Then  user "<principal>" is denied permission "<permission>"

    Examples:
      | principal | permission      |
      | noRole1   | catalog:read    |
      | noRole2   | order:read      |
      | noRole3   | profile:read    |
      | noRole4   | admin:*         |
      | noRole5   | order:place     |
      | noRole6   | report:view     |

  # ---------------------------------------------------------------------------
  # Role revocation — permission is lost immediately after revoke
  # ---------------------------------------------------------------------------

  @positive @negative @regression
  Scenario Outline: Revoking a role removes its permissions from the principal
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "<role>"
    When  I revoke role "<role>" from principal "<principal>"
    Then  user "<principal>" is denied permission "<deniedAfterRevoke>"

    Examples:
      | principal | role     | deniedAfterRevoke |
      | rev1      | MANAGER  | order:read        |
      | rev2      | MANAGER  | report:view       |
      | rev3      | CUSTOMER | profile:read      |
      | rev4      | CUSTOMER | order:place       |
      | rev5      | GUEST    | catalog:read      |

  # ---------------------------------------------------------------------------
  # Role definition check — seeded role names are all registered
  # ---------------------------------------------------------------------------

  @positive @smoke
  Scenario Outline: Each standard role is defined after service initialisation
    Then role "<role>" is defined

    Examples:
      | role     |
      | ADMIN    |
      | MANAGER  |
      | CUSTOMER |
      | GUEST    |

  # ---------------------------------------------------------------------------
  # Role x permission grant/deny — combined positive+negative per row
  # ---------------------------------------------------------------------------

  @positive @negative @business @regression
  Scenario Outline: Each seeded role has exactly the right grant/deny boundary
    Given a registered principal "<principal>"
    And   principal "<principal>" is assigned role "<role>"
    Then  user "<principal>" has permission "<granted>"
    And   user "<principal>" is denied permission "<denied>"

    Examples:
      | principal | role     | granted        | denied         |
      | combined1 | MANAGER  | user:read      | role:assign    |
      | combined2 | MANAGER  | order:manage   | user:create    |
      | combined3 | MANAGER  | user:read      | order:cancel   |
      | combined4 | CUSTOMER | order:cancel   | order:manage   |
      | combined5 | CUSTOMER | catalog:read   | audit:view     |
      | combined6 | GUEST    | catalog:read   | order:place    |
      | combined7 | GUEST    | catalog:read   | profile:update |
