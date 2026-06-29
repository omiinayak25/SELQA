@domain @platform @settings
Feature: Settings
  As the platform settings subsystem
  I want to manage typed key-value settings with validation, defaults, and runtime changes
  So that configuration is safe, explicit, and resettable — with stable domain error codes
  asserted against real in-memory business logic

  Background:
    Given a clean settings service

  # ─── SMOKE ───────────────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: Getting a known key returns its registered default value
    When I get the settings key "app.name"
    Then the settings value is "OmiinQA"

  # ─── POSITIVE — individual typed set/get ─────────────────────────────────────

  @positive
  Scenario: Setting a valid STRING value and reading it back returns the updated value
    When I set the settings key "app.name" to value "MyApp"
    Then the operation succeeds
    And the settings key "app.name" has value "MyApp"

  @positive
  Scenario: Setting a valid INT value within range succeeds
    When I set the settings key "max.retries" to value "7"
    Then the operation succeeds
    And the settings key "max.retries" has value "7"

  @positive
  Scenario: Setting a valid ENUM value succeeds
    When I set the settings key "log.level" to value "DEBUG"
    Then the operation succeeds
    And the settings key "log.level" has value "DEBUG"

  @positive
  Scenario: Setting a valid BOOLEAN value succeeds
    When I set the settings key "debug.enabled" to value "true"
    Then the operation succeeds
    And the settings key "debug.enabled" has value "true"

  # ─── POSITIVE — reset / resetAll ─────────────────────────────────────────────

  @positive
  Scenario: Resetting a modified key restores its default value
    When I set the settings key "session.timeout" to value "60"
    And I reset the settings key "session.timeout"
    Then the operation succeeds
    And the settings key "session.timeout" has default value "30"

  @positive
  Scenario: resetAll restores all settings to their defaults after several changes
    When I set the settings key "app.name" to value "CustomApp"
    And I set the settings key "log.level" to value "WARN"
    And I set the settings key "debug.enabled" to value "true"
    And I reset all settings
    Then the operation succeeds
    And the settings key "app.name" has default value "OmiinQA"
    And the settings key "log.level" has default value "INFO"
    And the settings key "debug.enabled" has default value "false"

  # ─── NEGATIVE — unknown key ───────────────────────────────────────────────────

  @negative
  Scenario: Setting an unknown key raises SETTINGS_UNKNOWN_KEY
    When I set the settings key "does.not.exist" to value "anything"
    Then a domain error "SETTINGS_UNKNOWN_KEY" is raised

  @negative
  Scenario: Getting an unknown key raises SETTINGS_UNKNOWN_KEY
    When I get the settings key "no.such.key"
    Then a domain error "SETTINGS_UNKNOWN_KEY" is raised

  # ─── NEGATIVE — blank required setting ───────────────────────────────────────

  @negative @validation
  Scenario: Setting a required setting to blank raises SETTINGS_REQUIRED
    When I set the settings key "app.name" to value ""
    Then a domain error "SETTINGS_REQUIRED" is raised

  # ─── BOUNDARY — INT range ────────────────────────────────────────────────────

  @boundary @positive
  Scenario: Setting an INT to its minimum boundary value succeeds
    When I set the settings key "max.retries" to value "1"
    Then the operation succeeds
    And the settings key "max.retries" has value "1"

  @boundary @positive
  Scenario: Setting an INT to its maximum boundary value succeeds
    When I set the settings key "max.retries" to value "10"
    Then the operation succeeds
    And the settings key "max.retries" has value "10"

  @boundary @negative
  Scenario: Setting an INT to one below its minimum raises SETTINGS_OUT_OF_RANGE
    When I set the settings key "max.retries" to value "0"
    Then a domain error "SETTINGS_OUT_OF_RANGE" is raised

  @boundary @negative
  Scenario: Setting an INT to one above its maximum raises SETTINGS_OUT_OF_RANGE
    When I set the settings key "max.retries" to value "11"
    Then a domain error "SETTINGS_OUT_OF_RANGE" is raised

  # ─── BUSINESS — getAll count ─────────────────────────────────────────────────

  @business
  Scenario: After setting multiple typed settings getAll returns exactly 8 entries
    When I set the settings key "app.name" to value "OmiinQA"
    And I set the settings key "max.retries" to value "5"
    And I set the settings key "debug.enabled" to value "true"
    And I set the settings key "log.level" to value "WARN"
    And I set the settings key "theme" to value "DARK"
    And I set the settings key "session.timeout" to value "120"
    And I set the settings key "notifications.email" to value "false"
    And I set the settings key "timezone" to value "Europe/London"
    Then the settings all count is 8

  # ─── VALIDATION OUTLINE — big error-code table ───────────────────────────────

  @negative @validation
  Scenario Outline: Invalid values for various setting types raise the expected error code
    When I set the settings key "<key>" to value "<value>"
    Then a domain error "<code>" is raised

    Examples:
      | key              | value        | code                    |
      | does.not.exist   | anything     | SETTINGS_UNKNOWN_KEY    |
      | no.such.setting  | 42           | SETTINGS_UNKNOWN_KEY    |
      | app.name         |              | SETTINGS_REQUIRED       |
      | max.retries      | 0            | SETTINGS_OUT_OF_RANGE   |
      | max.retries      | 11           | SETTINGS_OUT_OF_RANGE   |
      | max.retries      | notANumber   | SETTINGS_OUT_OF_RANGE   |
      | session.timeout  | 4            | SETTINGS_OUT_OF_RANGE   |
      | session.timeout  | 1441         | SETTINGS_OUT_OF_RANGE   |
      | log.level        | VERBOSE      | SETTINGS_BAD_ENUM       |
      | log.level        | trace        | SETTINGS_BAD_ENUM       |
      | theme            | BLUE         | SETTINGS_BAD_ENUM       |
      | debug.enabled    | yes          | SETTINGS_BAD_ENUM       |
      | debug.enabled    | 1            | SETTINGS_BAD_ENUM       |
      | notifications.email | maybe     | SETTINGS_BAD_ENUM       |

  # ─── POSITIVE OUTLINE — valid set/get for multiple keys ──────────────────────

  @positive @sanity
  Scenario Outline: Valid values for each typed setting are accepted and stored correctly
    When I set the settings key "<key>" to value "<value>"
    Then the operation succeeds
    And the settings key "<key>" has value "<value>"

    Examples:
      | key                  | value          |
      | app.name             | SelQA Platform |
      | max.retries          | 5              |
      | debug.enabled        | false          |
      | log.level            | ERROR          |
      | theme                | HIGH_CONTRAST  |
      | session.timeout      | 1440           |
      | notifications.email  | true           |
      | timezone             | Asia/Kolkata   |

  # ─── REGRESSION OUTLINE — set / get / reset cycle ────────────────────────────

  @regression
  Scenario Outline: Set a key then reset it to confirm the default is restored
    When I set the settings key "<key>" to value "<changed>"
    Then the operation succeeds
    And the settings key "<key>" has value "<changed>"
    When I reset the settings key "<key>"
    Then the operation succeeds
    And the settings key "<key>" has default value "<default>"

    Examples:
      | key                  | changed      | default |
      | max.retries          | 8            | 3       |
      | log.level            | DEBUG        | INFO    |
      | theme                | DARK         | LIGHT   |
      | session.timeout      | 90           | 30      |
      | notifications.email  | false        | true    |
