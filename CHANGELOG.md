# Changelog

All notable changes to OmiinQA are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); versioning is [SemVer](https://semver.org/).

## [Unreleased]

### Added — v2.0 Enterprise coverage expansion
- **API breadth (+182):** JSONPlaceholder (69), httpbin (43), Petstore (42), GraphQL Countries (28)
  — services, models, schemas, all reusing `ApiClient`/`RequestBuilder`/`ResponseValidator`.
- **UI breadth (+165):** SauceDemo deep (auth/inventory/sorting/detail/cart/validation/totals/journeys),
  OrangeHRM (PIM/Admin/MyInfo/forgot-password pages + tests), the-internet UI mechanics
  (19 page objects: windows/frames/alerts/dynamic/hovers/drag-drop/keys/upload/cookies/storage).
- **Specialized layers (+16, +utilities):** accessibility (axe WCAG 2.1 AA), performance
  (navigation-timing budgets), visual (baseline pixel-diff), security (SQLi/XSS/session),
  responsive (viewport matrix).
- **BDD (+139 scenarios):** 11 feature files, 9 step classes across auth/inventory/sorting/cart/
  checkout/navigation/api domains. Cucumber dry-run validated (151 scenarios, 0 undefined/ambiguous).
- **E2E (+35):** API-driven UI, multi-step shopping, pure-API lifecycles, API→UI→DB.
- **Database depth (+52, +2 repos):** Order/Audit repositories, integrity, cross-validation,
  aggregation, transaction-rollback tests; `schema.sql` (PostgreSQL + MySQL).
- **Totals:** 558 `@Test` + 151 executable BDD scenarios = **665+ meaningful scenarios**
  (from 108). New suite `testng-specialized.xml`; suite package globs fixed to include subpackages.
- **Docs:** PROJECT_ANALYSIS gap report; synchronized metadata, test catalog, traceability, roadmap.

### Added — Framework foundation (v1.0.0)
- **Build:** Java 21 + Maven, full enterprise dependency stack via BOMs; quality
  (Checkstyle/PMD/SpotBugs/JaCoCo) and `security` (OWASP) profiles; Maven Enforcer baseline.
- **Config:** layered `ConfigManager` (Singleton) + typed `FrameworkConfig` (Facade) with
  CLI > env-overlay > base precedence.
- **Driver:** `DriverManager` ThreadLocal lifecycle, `DriverFactory` (local/remote/Grid),
  per-browser options strategies (Chrome/Firefox/Edge), bounded driver retry.
- **Core:** `BasePage`, `BaseComponent`, `BaseTest`; `WaitUtils`, `JavaScriptUtils`,
  `ScreenshotUtils`.
- **UI:** SauceDemo + OrangeHRM page objects, reusable components, business flows (Facade).
- **API:** REST Assured `ApiClient`, `RequestBuilder` (Builder), auth strategies, response &
  schema validators, services and typed models.
- **Database:** HikariCP connection pool, `QueryExecutor` (parameterized), `TransactionManager`,
  repositories and fluent DB assertions.
- **Data:** JSON/CSV/Excel/YAML/Properties readers, datafaker wrapper, builders, factories,
  TestNG data providers.
- **Reporting/Logging:** Allure + Extent integration, Log4j2 rolling logs, retry + lifecycle
  listeners.
- **Execution:** TestNG suites (smoke/api/ui/regression/e2e/cross-browser), Selenium Grid via
  Docker Compose, runner Dockerfile.
- **CI/CD:** GitHub Actions, Jenkins, Azure DevOps pipelines.
- **Docs:** README, ARCHITECTURE and supporting guides.

[Unreleased]: https://github.com/omiinayak25/SELQA/commits/main
