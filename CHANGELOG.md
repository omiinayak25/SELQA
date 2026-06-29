# Changelog

All notable changes to OmiinQA are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); versioning is [SemVer](https://semver.org/).

## [Unreleased]

### Added — v2.2 BDD reference-domain expansion (~15% Gherkin)
- **Executable reference domain.** A real in-memory business core under
  `com.omiinqa.reference.*` (42 services: identity, access/RBAC, catalog, commerce, orders,
  platform, files, security) with genuine validation, business rules and stable error codes.
- **2,561 offline-executable `@domain` BDD scenarios** (2,712 total BDD across 67 feature files)
  that run real domain logic with real assertions — **no browser, no fakes**. Plus `@db`
  scenarios running real SQL on the embedded H2, and `@i18n` over six real locale bundles
  (incl. Arabic RTL).
- **Tag-conditional Cucumber hooks**: a browser starts only for `@ui`/`@e2e`; `@domain`/`@api`
  scenarios run offline. Shared `DomainWorld` + `CommonDomainSteps` (outcome assertions defined
  once → zero duplicate/ambiguous steps across 35 domain step classes).
- Gherkin grew from 1.5% → **15.63%** of the codebase (GitHub Linguist target met) while every
  scenario remains genuinely executable. Dry-run: 2,712 scenarios, 0 undefined/0 ambiguous.
- Docs: [BDD_GUIDE.md](docs/BDD_GUIDE.md), internal BDD domain contract.

### Added — v2.1 Enterprise capability expansion (production-readiness pass)
- **Cloud:** BrowserStack / Sauce Labs / LambdaTest capability strategies + `CloudDriverProvider`.
- **Observability:** correlation IDs (MDC), execution timeline, metric registry, Prometheus
  exposition exporter, OpenTelemetry-shaped tracer/spans (zero deps).
- **Reporting exporters:** JSON/CSV/XML/Markdown + GitHub Step Summary / Slack / Teams / Email,
  plus a results aggregator and facade.
- **API protocols:** SOAP, WebSocket, SSE, multipart upload, OAuth2 (client-credentials/password
  + token cache) — all on JDK `java.net.http`, zero new dependencies.
- **Intelligence:** self-healing `SmartLocator`, locator suggester, failure categorizer, flaky
  detector, and an optional credentialed `AiAssistant` hook (heuristic-first, no fake AI).
- **i18n:** locale manager, resource bundles (en/es/fr), localization validator, text direction.
- **Resilience:** retry policy, circuit breaker, bulkhead, deterministic chaos injector.
- **Data:** synthetic generator + PII masker + unique pool + relational dataset; XXE-hardened
  XML reader.
- **DevOps/security:** LICENSE (MIT), issue/PR templates, CODEOWNERS, dependabot, FUNDING,
  CODE_OF_CONDUCT, SUPPORT; CodeQL (SAST), gitleaks secret scan, OWASP + CycloneDX SBOM,
  release-drafter; `.gitlab-ci.yml`; PIT (mutation) + SBOM Maven profiles.
- **Docs:** Mermaid diagrams, feature/technology matrices, ADRs, troubleshooting, FAQ, cloud &
  best-practices guides, known-limitations.
- **Totals:** 1,004 `@Test` + 151 executable BDD = **1,155+ scenarios** (from 665). +57 new
  source files, +435 new offline tests (all green). CI build/smoke/embedded-DB remain green.

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
