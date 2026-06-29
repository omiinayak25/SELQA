<h1 align="center">OmiinQA</h1>
<p align="center"><b>Enterprise Selenium + Java Automation Framework</b></p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21_LTS-orange">
  <img alt="Selenium" src="https://img.shields.io/badge/Selenium-4.27-43B02A">
  <img alt="TestNG" src="https://img.shields.io/badge/TestNG-7.10-blue">
  <img alt="REST Assured" src="https://img.shields.io/badge/REST_Assured-5.5-green">
  <img alt="Cucumber" src="https://img.shields.io/badge/Cucumber-7.20-23D96C">
  <img alt="Allure" src="https://img.shields.io/badge/Allure-2.29-yellow">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-lightgrey">
</p>
<p align="center">
  <a href="https://github.com/omiinayak25/SELQA/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/omiinayak25/SELQA/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://github.com/omiinayak25/SELQA/actions/workflows/codeql.yml"><img alt="CodeQL" src="https://github.com/omiinayak25/SELQA/actions/workflows/codeql.yml/badge.svg"></a>
  <a href="https://github.com/omiinayak25/SELQA/actions/workflows/security.yml"><img alt="Security" src="https://github.com/omiinayak25/SELQA/actions/workflows/security.yml/badge.svg"></a>
  <img alt="Tests" src="https://img.shields.io/badge/tests-1000%2B-success">
  <img alt="PRs" src="https://img.shields.io/badge/PRs-welcome-brightgreen">
</p>

> A production-grade, layered test-automation framework demonstrating the engineering
> practices of a senior QA automation team: UI, API, Database, E2E, BDD, Accessibility,
> Visual, Performance and Security testing — with parallel, cross-browser, Selenium Grid
> and Docker execution, full CI/CD, and rich reporting.

<p align="center">
  <b>665+ meaningful scenarios</b> · 558 TestNG <code>@Test</code> · 151 executable BDD scenarios ·
  7 API apps · 3 UI apps · 10 specialized layers
</p>

---

## Table of Contents
1. [Why this framework](#why-this-framework)
2. [Capabilities](#capabilities)
3. [Technology stack](#technology-stack)
4. [Architecture at a glance](#architecture-at-a-glance)
5. [Project layout](#project-layout)
6. [Getting started](#getting-started)
7. [Running tests](#running-tests)
8. [Cross-browser, Grid & Docker](#cross-browser-grid--docker)
9. [Reporting](#reporting)
10. [Design patterns used](#design-patterns-used)
11. [Quality gates](#quality-gates)
12. [CI/CD](#cicd)
13. [Documentation index](#documentation-index)

---

## Why this framework
Most "Selenium POM" samples stop at a login test. OmiinQA is built the way a real
enterprise suite is maintained: a stable **core** (config, driver lifecycle, waits,
logging) that rarely changes, surrounded by **layers** (pages, flows, API, DB) that grow
with the product, and **tests** that read like specifications. Every architectural choice
is documented and justified — this repo doubles as a teaching and interview reference.

## Capabilities
| Domain | What's covered |
|---|---|
| **UI** | SauceDemo + OrangeHRM, advanced Page Object Model, reusable components, business flows |
| **API** | REST Assured client, request builder, auth strategies (Basic/Bearer/API-key), schema validation, request chaining |
| **Database** | JDBC + HikariCP pool, repository pattern, transactions/rollback, fluent DB assertions (PostgreSQL & MySQL) |
| **E2E** | UI + API + DB orchestrated journeys |
| **BDD** | Cucumber 7, business-readable Gherkin, reusable steps, hooks |
| **Cross-browser** | Chrome, Firefox, Edge — local, headless, remote, Grid |
| **Parallel** | ThreadLocal driver, TestNG `parallel="methods"` |
| **Data-driven** | JSON, CSV, Excel (POI), YAML, Properties, datafaker, builders & factories |
| **Accessibility** | axe-core integration |
| **Visual** | Baseline/snapshot comparison with dynamic-region masking |
| **Performance** | Navigation-timing + page-load budget validation |
| **Security** | Auth/session/cookie checks, header validation, SQLi/XSS payload probes |

## Technology stack
Java 21 · Selenium 4 · Maven · TestNG · REST Assured · Cucumber · Jackson · Lombok ·
Log4j2/SLF4J · Apache POI · OpenCSV · datafaker · JDBC (PostgreSQL/MySQL) · HikariCP ·
Allure · Extent Reports · Docker · Selenium Grid · GitHub Actions · Jenkins · Azure DevOps ·
Checkstyle · PMD · SpotBugs · JaCoCo · OWASP Dependency-Check · Maven Enforcer.

## Architecture at a glance
```
        ┌────────────────────────────────────────────────────────┐
  Tests │  ui · api · database · e2e · bdd · a11y · visual · perf │
        └───────────────┬───────────────────────┬────────────────┘
                        │                        │
   Business flows ┌─────▼──────┐         ┌───────▼────────┐  Services / repositories
   (Facade)       │  Pages &   │         │  API services  │  (Facade / Repository)
                  │ components │         │  DB repositories│
                  └─────┬──────┘         └───────┬────────┘
                        │                        │
        ┌───────────────▼────────────────────────▼────────────────┐
  Core  │ config · driver(ThreadLocal) · waits · utils · logging   │
        │ exceptions · listeners · reporting                       │
        └──────────────────────────────────────────────────────────┘
```
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full description.

## Project layout
See [docs/FOLDER_STRUCTURE.md](docs/FOLDER_STRUCTURE.md). Top level:
```
src/main/java/com/omiinqa
  config/        Layered configuration (Singleton + Facade)
  constants/     Compile-time constants & config keys
  enums/         BrowserType, Environment, ExecutionMode
  exceptions/    Unchecked framework exception hierarchy
  driver/        DriverManager (ThreadLocal), DriverFactory, options strategies, retry
  core/          BasePage, BaseComponent, BaseTest (Template Method)
  utils/         Waits, JS, screenshots; utils/data readers (JSON/CSV/Excel/YAML)
  pages/         Page objects (saucedemo, orangehrm)
  components/    Reusable UI components (header, table, modal, …)
  businessflows/ End-to-end journeys (Facade)
  api/           REST Assured client, builder, auth, validators, services, models
  database/      Connection pool, query executor, transactions, repositories, assertions
  data/          Models, builders, factories, faker, data providers
  reports/       Extent integration
  listeners/     TestNG retry + lifecycle listeners
src/test/java/com/omiinqa
  unit/ api/ ui/ database/ e2e/ bdd/ accessibility/ visual/ performance/ security/
src/test/resources
  config/ suites/ testdata/ schemas/ features/
docker/ .github/ config/ docs/
```

## Getting started
**Prerequisites:** JDK 21, Maven 3.9+. (Browsers are auto-provisioned by WebDriverManager;
Docker only needed for Grid runs.)
```bash
git clone https://github.com/omiinayak25/SELQA.git
cd SELQA
mvn -B clean test-compile        # build everything
mvn -B test -Dsuite.file=testng-smoke.xml   # offline core smoke (no browser needed)
```

## Running tests
| Goal | Command |
|---|---|
| Offline core smoke | `mvn test -Dsuite.file=testng-smoke.xml` |
| API regression | `mvn test -Dsuite.file=testng-api.xml` |
| UI (local Chrome, headless) | `mvn test -Dsuite.file=testng-ui.xml -Dbrowser=chrome -Dheadless=true` |
| UI (headed Firefox) | `mvn test -Dsuite.file=testng-ui.xml -Dbrowser=firefox -Dheadless=false` |
| Full regression | `mvn test -Dsuite.file=testng-regression.xml` |
| BDD (Cucumber) | `mvn test -Dtest=CucumberTestRunner` |
| Pick environment | add `-Denv=qa|staging|dev` |

**Config precedence:** `-Dkey=value` (CLI) → `config/env/<env>.properties` → `config/config.properties`.

## Cross-browser, Grid & Docker
```bash
# Start a Grid (3 Chrome + Firefox + Edge nodes)
docker compose -f docker/docker-compose-grid.yml up -d --scale chrome=3
# Console: http://localhost:4444

# Run UI suite against the Grid
mvn test -Dsuite.file=testng-ui.xml \
         -Dexecution.mode=grid -Dbrowser=remote-chrome \
         -Dgrid.url=http://localhost:4444/wd/hub

# Or run the whole suite inside a container
docker build -f docker/Dockerfile -t omiinqa:latest .
```

## Reporting
- **Allure** — `mvn allure:serve` after a run (rich history, steps, attachments).
- **Extent** — `extent-reports/OmiinQA-Report.html` (self-contained HTML).
- **Logs** — `logs/omiinqa.log` (rolling), per-test MDC tokens for parallel readability.
- **Evidence** — failure screenshots auto-attached to Allure and saved under `screenshots/`.

## Design patterns used
Singleton (config, driver holder, report manager) · Factory (driver & options) ·
Strategy (browser options, API auth) · Builder (requests, test data) · Repository (DB) ·
Facade (business flows, API services, typed config) · Template Method (base test/page) ·
Decorator/Adapter (listeners, report bridge) · Command (driver retry, transactions).
Each is justified in the relevant class Javadoc and in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Quality gates
`mvn -P quality verify` runs Checkstyle, PMD and SpotBugs; `-P security verify` runs OWASP
Dependency-Check; JaCoCo produces coverage; Maven Enforcer pins the JDK/Maven baseline and
guards dependency hygiene. Day-to-day builds stay green; gates are enforced in CI.

## CI/CD
- **GitHub Actions** — [.github/workflows/ci.yml](.github/workflows/ci.yml): build → quality → API → UI-on-Grid → Allure publish.
- **Jenkins** — [Jenkinsfile](Jenkinsfile): parameterized, parallel stages, Allure.
- **Azure DevOps** — [azure-pipelines.yml](azure-pipelines.yml): cached, staged.

## Documentation index
**Architecture & design:** [ARCHITECTURE](docs/ARCHITECTURE.md) · [Diagrams (Mermaid)](docs/DIAGRAMS.md) ·
[Folder Structure](docs/FOLDER_STRUCTURE.md) · [Framework Flow](docs/FRAMEWORK_FLOW.md) ·
[ADRs](docs/adr/README.md) · [Feature Matrix](docs/FEATURE_MATRIX.md) · [Technology Matrix](docs/TECHNOLOGY_MATRIX.md)

**Guides:** [Developer](docs/DEVELOPER_GUIDE.md) · [Execution](docs/EXECUTION_GUIDE.md) ·
[Cloud](docs/CLOUD_GUIDE.md) · [Best Practices](docs/BEST_PRACTICES.md) ·
[Troubleshooting](docs/TROUBLESHOOTING.md) · [FAQ](docs/FAQ.md) · [Interview](docs/INTERVIEW_GUIDE.md)

**Tests & quality:** [Test Catalog](docs/TEST_CATALOG.md) · [Traceability Matrix](docs/TEST_TRACEABILITY_MATRIX.md) ·
[Project Analysis](PROJECT_ANALYSIS.md) · [Known Limitations](docs/KNOWN_LIMITATIONS.md)

**Project:** [Contributing](CONTRIBUTING.md) · [Code of Conduct](CODE_OF_CONDUCT.md) ·
[Support](SUPPORT.md) · [Security](SECURITY.md) · [Changelog](CHANGELOG.md) ·
[Roadmap](docs/ROADMAP.md) · [AI Handover](docs/AI_HANDOVER.md) · [License](LICENSE)

---
<p align="center"><sub>Built as a portfolio-grade reference for senior QA automation engineering.</sub></p>
