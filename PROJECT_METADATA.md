# OmiinQA — Project Metadata

| Field | Value |
|---|---|
| Project | OmiinQA — Enterprise Selenium + Java Automation Framework |
| Repository | https://github.com/omiinayak25/SELQA |
| Group / Artifact | `com.omiinqa` / `omiinqa-selenium-framework` |
| Version | `1.0.0-SNAPSHOT` |
| Language | Java 21 (LTS) |
| Build | Maven 3.9+ |
| License | MIT |

## Tech stack
Selenium 4.27 · TestNG 7.10 · REST Assured 5.5 · Cucumber 7.20 · Jackson 2.18 · Lombok 1.18 ·
Log4j2 2.24 / SLF4J 2.0 · Apache POI 5.3 · OpenCSV 5.9 · datafaker 2.4 · JDBC (PostgreSQL 42.7 /
MySQL 9.1) · HikariCP 6.2 · Allure 2.29 · Extent 5.1 · Docker / Selenium Grid 4 · GitHub Actions /
Jenkins / Azure DevOps · Checkstyle / PMD / SpotBugs / JaCoCo / OWASP Dependency-Check / Enforcer.

## Applications under test
UI: SauceDemo, OrangeHRM. API: Restful-Booker, ReqRes, DummyJSON, Swagger Petstore,
JSONPlaceholder, httpbin, GraphQL Countries.

## Module map (main classes)
config 2 · driver 8 · core 3 · utils 9 · pages 9 · components 9 · businessflows 3 ·
api 28 · database 12 · data 11 · reports 2 · listeners 3 — **109 source files**.

## Test inventory
108 `@Test` methods + 5 BDD scenarios across unit / api / ui / e2e / database / bdd.
See [docs/TEST_CATALOG.md](docs/TEST_CATALOG.md).

## Build verification
`mvn clean test-compile` → SUCCESS · `mvn test -Dsuite.file=testng-smoke.xml` → 9/9 passing.

## Entry points
- Build: `mvn clean test-compile`
- Smoke (offline): `mvn test -Dsuite.file=testng-smoke.xml`
- Suites: `src/test/resources/suites/*.xml`
- Config: `src/main/resources/config/` · Logging: `src/main/resources/log4j2.xml`
