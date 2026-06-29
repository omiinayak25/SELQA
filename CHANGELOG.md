# Changelog

All notable changes to OmiinQA are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); versioning is [SemVer](https://semver.org/).

## [Unreleased]

### Added — Framework foundation (v1.0.0 in progress)
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
