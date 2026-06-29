# OmiinQA — Project Metadata

> Generated from the repository at commit `847cb84` (2026-06-29). Machine-readable
> companion: [`project-metadata.json`](project-metadata.json).
>
> **v2.1 coverage:** 1,004 `@Test` + 151 executable BDD scenarios = **1,150+ meaningful
> scenarios** across 227 main + 112 test sources (≈28.2k main / 21.4k test LOC), 24 modules.
> Enterprise modules: cloud, observability, reporting-exporters, API protocols (SOAP/WS/SSE/
> OAuth2), intelligence (self-healing/AI), i18n, resilience/chaos, synthetic data.

## Identity
| Field | Value |
|---|---|
| Project | OmiinQA — Enterprise Selenium + Java Automation Framework |
| Repository | https://github.com/omiinayak25/SELQA |
| Group / Artifact | `com.omiinqa` / `omiinqa-selenium-framework` |
| Version | `1.0.0-SNAPSHOT` |
| License | MIT |
| Branch / Commit | `main` / `0aac3f8` |

## Toolchain
| Field | Value |
|---|---|
| Language | Java 21 (Eclipse Temurin 21.0.11 LTS) |
| Build | Maven 3.9.9 |
| Declared dependencies | 39 |

## Metrics (computed from source)
| Metric | Value |
|---|---|
| Main source files | 227 |
| Test source files | 112 |
| Main lines of code | 28,200 |
| Test lines of code | 21,422 |
| `@Test` methods | 1,004 |
| BDD feature files / scenarios | 13 / 107 declared (151 executable) |
| Total meaningful scenarios | 1,150+ |
| Top-level modules | 24 |
| TestNG suites | 11 |
| Doc files / CI workflows | 25 / 4 |
| Test-data files / JSON schemas | 8 / 7 |
| Git-tracked files | 446 |

## Module map — `src/main/java/com/omiinqa` (227 files, 24 packages)
| Package | Files | Package | Files | Package | Files |
|---|---|---|---|---|---|
| api | 62 | pages | 32 | database | 16 |
| reports | 14 | data | 13 | utils | 11 |
| intelligence | 10 | components | 9 | cloud | 8 |
| driver | 8 | exceptions | 6 | observability | 6 |
| i18n | 4 | resilience | 4 | core | 3 |
| enums | 3 | businessflows | 3 | listeners | 3 |
| performance | 3 | config | 2 | accessibility | 2 |
| visual | 2 | security | 2 | constants | 1 |

## Test inventory by layer
| Layer | Tests | Suite | External need |
|---|---|---|---|
| Unit | 9 | `testng-smoke.xml` | none (offline) |
| API | 238 | `testng-api.xml` | network |
| UI | 171 | `testng-ui.xml` | browser / Grid |
| E2E | 36 | `testng-e2e.xml` | browser + network (+DB subset) |
| Database | 88 | `testng-database.xml` | live DB |
| Accessibility / Performance / Visual / Security / Responsive | 16 | `testng-specialized.xml` | browser / Grid |
| Cloud / Observability / Reporting-export / API-protocols (offline) | ~130 | (by group) | none |
| Intelligence / i18n / Resilience / Synthetic-data (offline) | ~305 | (by group) | none |
| BDD | 151 executable (107 declared) | `testng-bdd.xml` | browser / Grid |
| **Total `@Test`** | **1,004** | | |

## Technology stack
Selenium 4.27 · TestNG 7.10 · WebDriverManager 5.9 · REST Assured 5.5 · Cucumber 7.20 ·
Jackson 2.18 · Lombok 1.18 · Log4j2 2.24 / SLF4J 2.0 · Apache POI 5.3 · OpenCSV 5.9 ·
datafaker 2.4 · PostgreSQL 42.7 / MySQL 9.1 · HikariCP 6.2 · Allure 2.29 · Extent 5.1 ·
Docker / Selenium Grid 4 · GitHub Actions / Jenkins / Azure DevOps ·
Checkstyle / PMD / SpotBugs / JaCoCo / OWASP Dependency-Check / Maven Enforcer.

## Design patterns
Singleton · Factory · Strategy · Builder · Repository · Facade · Template Method ·
Adapter · Decorator · Command. (See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).)

## Applications under test
UI: SauceDemo, OrangeHRM. API: Restful-Booker, ReqRes, DummyJSON, Swagger Petstore,
JSONPlaceholder, httpbin, GraphQL Countries.

## Build verification
| Step | Command | Result |
|---|---|---|
| Compile | `mvn clean test-compile` | SUCCESS |
| Smoke (offline) | `mvn test -Dsuite.file=testng-smoke.xml` | 9/9 passing |
| BDD dry-run | `mvn test -Dsuite.file=testng-bdd.xml -Dcucumber.execution.dry-run=true` | 151 scenarios, 0 undefined/ambiguous |

## Entry points
- Build: `mvn clean test-compile`
- Smoke (offline): `mvn test -Dsuite.file=testng-smoke.xml`
- Suites: `src/test/resources/suites/*.xml`
- Config: `src/main/resources/config/` · Logging: `src/main/resources/log4j2.xml`
