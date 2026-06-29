# OmiinQA — Project Metadata

> Generated from the repository at commit `0ad0aae` (2026-06-29). Machine-readable
> companion: [`project-metadata.json`](project-metadata.json).
>
> **v2.0 coverage:** 558 `@Test` + 151 executable BDD scenarios = **665+ meaningful scenarios**
> across 170 main + 78 test sources (≈18.2k main / 15.1k test LOC).

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
| Main source files | 170 |
| Test source files | 78 |
| Main lines of code | 18,240 |
| Test lines of code | 15,122 |
| `@Test` methods | 558 |
| BDD feature files / scenarios | 13 / 107 declared (151 executable) |
| Total meaningful scenarios | 665+ |
| TestNG suites | 9 |
| Test-data files / JSON schemas | 6 / 7 |
| Git-tracked files | 316 |

## Module map — `src/main/java/com/omiinqa` (170 files)
| Package | Files | Package | Files | Package | Files |
|---|---|---|---|---|---|
| api | 52 | pages | 32 | database | 16 |
| data | 11 | utils | 10 | components | 9 |
| driver | 8 | exceptions | 6 | core | 3 |
| enums | 3 | businessflows | 3 | listeners | 3 |
| performance | 3 | config | 2 | reports | 2 |
| accessibility | 2 | visual | 2 | security | 2 |
| constants | 1 | | | | |

## Test inventory by layer
| Layer | Tests | Suite | External need |
|---|---|---|---|
| Unit | 9 | `testng-smoke.xml` | none (offline) |
| API | 238 | `testng-api.xml` | network |
| UI | 171 | `testng-ui.xml` | browser / Grid |
| E2E | 36 | `testng-e2e.xml` | browser + network (+DB subset) |
| Database | 88 | `testng-database.xml` | live DB |
| Accessibility / Performance / Visual / Security / Responsive | 16 | `testng-specialized.xml` | browser / Grid |
| BDD | 151 executable (107 declared) | `testng-bdd.xml` | browser / Grid |

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
