@domain @platform @settings
Feature: Settings Validation Matrix
  As the platform settings subsystem
  I want every key×value combination to be validated against its type, range and enum constraints
  So that the real 8 built-in setting definitions produce exactly the expected success or error code

  Background:
    Given a clean settings service

  # ─── POSITIVE MATRIX — every built-in key with every valid boundary/edge value ──

  @positive @boundary @sanity
  Scenario Outline: Every built-in setting accepts its boundary-valid value and stores it correctly
    When I set the settings key "<key>" to value "<value>"
    Then the operation succeeds
    And the settings key "<key>" has value "<value>"

    Examples:
      | key                  | value           |
      | app.name             | A               |
      | app.name             | x               |
      | app.name             | OmiinQA-v2      |
      | app.name             | My Platform 123 |
      | max.retries          | 1               |
      | max.retries          | 2               |
      | max.retries          | 5               |
      | max.retries          | 9               |
      | max.retries          | 10              |
      | session.timeout      | 5               |
      | session.timeout      | 6               |
      | session.timeout      | 30              |
      | session.timeout      | 60              |
      | session.timeout      | 300             |
      | session.timeout      | 720             |
      | session.timeout      | 1439            |
      | session.timeout      | 1440            |
      | debug.enabled        | true            |
      | debug.enabled        | false           |
      | debug.enabled        | TRUE            |
      | debug.enabled        | FALSE           |
      | debug.enabled        | True            |
      | debug.enabled        | False           |
      | notifications.email  | true            |
      | notifications.email  | false           |
      | notifications.email  | True            |
      | notifications.email  | False           |
      | log.level            | DEBUG           |
      | log.level            | INFO            |
      | log.level            | WARN            |
      | log.level            | ERROR           |
      | theme                | LIGHT           |
      | theme                | DARK            |
      | theme                | HIGH_CONTRAST   |
      | timezone             | UTC             |
      | timezone             | Europe/London   |
      | timezone             | America/New_York|
      | timezone             | Asia/Tokyo      |
      | timezone             | Pacific/Auckland|

  # ─── NEGATIVE MATRIX — every invalid key×value combination with exact error code ─

  @negative @validation
  Scenario Outline: Invalid key or value combination raises the exact expected error code
    When I set the settings key "<key>" to value "<value>"
    Then a domain error "<code>" is raised

    Examples:
      | key                  | value           | code                  |
      | unknown.key          | anything        | SETTINGS_UNKNOWN_KEY  |
      | missing.setting      | 42              | SETTINGS_UNKNOWN_KEY  |
      | app.version          | 1.0             | SETTINGS_UNKNOWN_KEY  |
      | log.format           | JSON            | SETTINGS_UNKNOWN_KEY  |
      | retry.count          | 3               | SETTINGS_UNKNOWN_KEY  |
      | ui.theme             | DARK            | SETTINGS_UNKNOWN_KEY  |
      | app.name             |                 | SETTINGS_REQUIRED     |
      | max.retries          | 0               | SETTINGS_OUT_OF_RANGE |
      | max.retries          | -1              | SETTINGS_OUT_OF_RANGE |
      | max.retries          | 11              | SETTINGS_OUT_OF_RANGE |
      | max.retries          | 100             | SETTINGS_OUT_OF_RANGE |
      | max.retries          | notANumber      | SETTINGS_OUT_OF_RANGE |
      | max.retries          | 3.5             | SETTINGS_OUT_OF_RANGE |
      | max.retries          | 1e2             | SETTINGS_OUT_OF_RANGE |
      | max.retries          |                 | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | 4               | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | 0               | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | -1              | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | 1441            | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | 9999            | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | abc             | SETTINGS_OUT_OF_RANGE |
      | session.timeout      | 30.5            | SETTINGS_OUT_OF_RANGE |
      | log.level            | VERBOSE         | SETTINGS_BAD_ENUM     |
      | log.level            | TRACE           | SETTINGS_BAD_ENUM     |
      | log.level            | debug           | SETTINGS_BAD_ENUM     |
      | log.level            | info            | SETTINGS_BAD_ENUM     |
      | log.level            | warn            | SETTINGS_BAD_ENUM     |
      | log.level            | CRITICAL        | SETTINGS_BAD_ENUM     |
      | log.level            | ALL             | SETTINGS_BAD_ENUM     |
      | theme                | BLUE            | SETTINGS_BAD_ENUM     |
      | theme                | WHITE           | SETTINGS_BAD_ENUM     |
      | theme                | light           | SETTINGS_BAD_ENUM     |
      | theme                | dark            | SETTINGS_BAD_ENUM     |
      | theme                | NIGHT           | SETTINGS_BAD_ENUM     |
      | debug.enabled        | yes             | SETTINGS_BAD_ENUM     |
      | debug.enabled        | no              | SETTINGS_BAD_ENUM     |
      | debug.enabled        | 1               | SETTINGS_BAD_ENUM     |
      | debug.enabled        | 0               | SETTINGS_BAD_ENUM     |
      | debug.enabled        | on              | SETTINGS_BAD_ENUM     |
      | debug.enabled        | off             | SETTINGS_BAD_ENUM     |
      | notifications.email  | yes             | SETTINGS_BAD_ENUM     |
      | notifications.email  | no              | SETTINGS_BAD_ENUM     |
      | notifications.email  | 1               | SETTINGS_BAD_ENUM     |
      | notifications.email  | enabled         | SETTINGS_BAD_ENUM     |

  # ─── RESET MATRIX — set each key to a non-default, then reset and confirm default ─

  @regression @positive
  Scenario Outline: Changing a setting then resetting it restores its declared default
    When I set the settings key "<key>" to value "<changed>"
    Then the operation succeeds
    When I reset the settings key "<key>"
    Then the operation succeeds
    And the settings key "<key>" has default value "<default>"

    Examples:
      | key                  | changed         | default  |
      | app.name             | AltName         | OmiinQA  |
      | max.retries          | 1               | 3        |
      | max.retries          | 10              | 3        |
      | debug.enabled        | true            | false    |
      | log.level            | DEBUG           | INFO     |
      | log.level            | WARN            | INFO     |
      | log.level            | ERROR           | INFO     |
      | theme                | DARK            | LIGHT    |
      | theme                | HIGH_CONTRAST   | LIGHT    |
      | session.timeout      | 5               | 30       |
      | session.timeout      | 1440            | 30       |
      | notifications.email  | false           | true     |
      | timezone             | America/Chicago | UTC      |
      | timezone             | Asia/Kolkata    | UTC      |

  # ─── isDefined MATRIX — all 8 built-in keys are defined; arbitrary others are not ─

  @positive @sanity
  Scenario Outline: All 8 built-in setting keys report isDefined as true
    Then the settings key "<key>" is defined

    Examples:
      | key                  |
      | app.name             |
      | max.retries          |
      | debug.enabled        |
      | log.level            |
      | theme                |
      | session.timeout      |
      | notifications.email  |
      | timezone             |

  @negative
  Scenario Outline: Unregistered keys report isDefined as false
    Then the settings key "<key>" is not defined

    Examples:
      | key                  |
      | unknown.key          |
      | app.version          |
      | feature.flag         |
      | max.connections      |

  # ─── getAll count — always 8 after any combination of sets ──────────────────────

  @business @sanity
  Scenario Outline: getAll always returns exactly 8 entries regardless of values set
    When I set the settings key "<k1>" to value "<v1>"
    And I set the settings key "<k2>" to value "<v2>"
    Then the operation succeeds
    And the settings all count is 8

    Examples:
      | k1              | v1     | k2              | v2    |
      | log.level       | DEBUG  | theme           | DARK  |
      | max.retries     | 7      | session.timeout | 120   |
      | debug.enabled   | true   | notifications.email | false |
