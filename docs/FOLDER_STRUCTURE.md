# OmiinQA — Folder Structure Guide

Every folder exists to hold one kind of responsibility. If a folder would only
contain glue or a single trivial class, it isn't created (YAGNI).

## `src/main/java/com/omiinqa` — the framework library
| Folder | Why it exists |
|---|---|
| `config/` | Load and expose configuration with clear precedence (Singleton + Facade). |
| `constants/` | Compile-time constants and canonical config keys — kills magic strings. |
| `enums/` | Closed sets the framework branches on (browser, environment, mode). |
| `exceptions/` | One unchecked hierarchy so callers catch a single base type. |
| `driver/` | WebDriver lifecycle: ThreadLocal manager, factory, retry. |
| `driver/options/` | Per-browser capability strategies (Open/Closed for new browsers). |
| `core/` | Base classes that fix the test/page skeleton (Template Method). |
| `utils/` | Cross-cutting helpers: waits, JS, screenshots. |
| `utils/data/` | Readers for JSON/CSV/Excel/YAML/Properties. |
| `pages/` | Page objects, grouped per application (`saucedemo`, `orangehrm`). |
| `components/` | Reusable UI widgets scoped to a root element (composition). |
| `businessflows/` | Multi-page journeys as a Facade — reused by tests & BDD. |
| `api/` | REST Assured client, builder, auth, validators, services, models. |
| `database/` | Connection pool, query executor, transactions, repositories, assertions. |
| `data/` | Domain models, builders, factories, faker, TestNG data providers. |
| `reports/` | Extent integration (Adapter into TestNG events). |
| `listeners/` | Retry analyzer/transformer and lifecycle listener (Observer). |

## `src/test/java/com/omiinqa` — the tests
| Folder | Contents |
|---|---|
| `unit/` | Offline framework self-tests (config, enums) — the smoke gate. |
| `ui/` | UI tests per application. |
| `api/` | API tests (no browser). |
| `database/` | DB tests (group `database`, needs a live DB). |
| `e2e/` | Cross-layer journeys. |
| `bdd/` | Cucumber runner, hooks, step definitions, scenario context. |
| `accessibility/` `visual/` `performance/` `security/` | Specialized layers. |

## `src/test/resources`
| Folder | Contents |
|---|---|
| `suites/` | TestNG suite XMLs (smoke/api/ui/regression/e2e/cross-browser/bdd/database). |
| `testdata/` | Static JSON/CSV inputs. |
| `schemas/` | JSON schemas for API contract validation. |
| `features/` | Gherkin `.feature` files. |
| `config/` | (optional) untracked `secrets.properties`. |

## Repository root
| Path | Purpose |
|---|---|
| `pom.xml` | Build, dependency management (BOMs), profiles, quality plugins. |
| `docker/` | Grid compose file + runner Dockerfile. |
| `.github/workflows/` | GitHub Actions CI. |
| `Jenkinsfile`, `azure-pipelines.yml` | Other CI providers. |
| `config/` | Checkstyle ruleset, OWASP suppressions. |
| `docs/` | Architecture and guides. |
| `src/main/resources/` | `config.properties`, env overlays, `log4j2.xml`. |
