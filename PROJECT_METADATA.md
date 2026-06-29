# OmiinQA — Project Metadata

> Generated from the repository at commit `0aac3f8` (2026-06-29). Machine-readable
> companion: [`project-metadata.json`](project-metadata.json).

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
| Main source files | 109 |
| Test source files | 19 |
| Main lines of code | 10,482 |
| Test lines of code | 2,169 |
| `@Test` methods | 108 |
| BDD feature files / scenarios | 2 / 5 |
| TestNG suites | 8 |
| Test-data files / JSON schemas | 6 / 3 |
| Git-tracked files | 176 |

## Module map — `src/main/java/com/omiinqa` (109 files)
| Package | Files | Package | Files | Package | Files |
|---|---|---|---|---|---|
| api | 28 | database | 12 | data | 11 |
| utils | 9 | pages | 9 | components | 9 |
| driver | 8 | exceptions | 6 | core | 3 |
| enums | 3 | businessflows | 3 | listeners | 3 |
| config | 2 | reports | 2 | constants | 1 |

## Test inventory by layer
| Layer | Files | Tests | Suite | External need |
|---|---|---|---|---|
| Unit | 2 | 9 | `testng-smoke.xml` | none (offline) |
| API | 5 | 56 | `testng-api.xml` | network |
| UI | 2 | 6 | `testng-ui.xml` | browser / Grid |
| E2E | 1 | 1 | `testng-e2e.xml` | browser + network |
| Database | 4 | 36 | `testng-database.xml` | live DB |
| BDD | 5 | 5 scenarios | `testng-bdd.xml` | browser / Grid |

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

## Entry points
- Build: `mvn clean test-compile`
- Smoke (offline): `mvn test -Dsuite.file=testng-smoke.xml`
- Suites: `src/test/resources/suites/*.xml`
- Config: `src/main/resources/config/` · Logging: `src/main/resources/log4j2.xml`
