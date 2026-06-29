# OmiinQA — Interview Guide

Talking points and Q&A for presenting this framework in a senior QA automation interview.

## 60-second pitch
"OmiinQA is a layered Selenium+Java framework. A stable core — configuration, a
ThreadLocal driver lifecycle, fluent waits, logging — is wrapped by interaction layers
(pages, components, API services, DB repositories), orchestrated by business-flow facades,
and exercised by tests that read like specifications. It runs cross-browser, in parallel,
on a Dockerized Selenium Grid, with Allure/Extent reporting and three CI providers. Every
design choice is documented and justified."

## Architecture questions
**Q: How do you make parallel execution safe?**
A ThreadLocal `WebDriver` in `DriverManager`: each TestNG method thread owns an isolated
browser; `quitDriver` always `remove()`s so pooled threads never carry a dead session.

**Q: Why no assertions in page objects?**
Pages model *interaction*; tests own *verification*. Keeping assertions out of pages lets
the same page serve positive, negative and boundary scenarios, and keeps failures pointing
at the test's intent, not the page.

**Q: Implicit vs explicit waits?**
Implicit waits are pinned to 0. Mixing them with explicit waits compounds timeouts
unpredictably. All synchronization runs through `WaitUtils` (FluentWait) — one policy, one place.

**Q: How do you add a new browser?**
Add a `BrowserOptionsStrategy` and a switch arm in two factories. The Strategy pattern keeps
it additive (Open/Closed) rather than editing scattered conditionals.

**Q: How is configuration resolved?**
`-Dkey=value` > `config/env/<env>.properties` > `config/config.properties`. `ConfigManager`
(Singleton) loads it; `FrameworkConfig` (Facade) exposes typed accessors. Verified by unit tests.

## Design-pattern cheat sheet
Singleton (config/driver/report/DB pool) · Factory (driver/options) · Strategy
(browser options/API auth) · Builder (requests/test data) · Repository (DB) · Facade
(flows/services/typed config) · Template Method (base test/page) · Adapter/Decorator
(report bridge/retry transformer) · Command (driver retry/transactions).

## API / DB questions
**Q: How do you prevent SQL injection in the DB layer?** `PreparedStatement` exclusively —
no string concatenation; documented in `QueryExecutor`.
**Q: Request chaining?** `BookingService` creates an auth token, then threads it through
create/get/update/delete — state carried in typed models, asserted via `ResponseValidator`
and JSON-schema validation.

## Reliability questions
**Q: Retries — don't they hide bugs?** Retry budget is small (default 2), configurable, and
every retry is logged so a chronically-retried test is visible and quarantined. It shields
genuine UI/network flakiness, not product defects.

## Things to demo live
1. `mvn test -Dsuite.file=testng-smoke.xml` — offline core smoke in seconds.
2. `docker compose -f docker/docker-compose-grid.yml up -d` then a parallel UI run on Grid.
3. `mvn allure:serve` — steps, history, failure screenshots.
4. Walk the layered diagram in [ARCHITECTURE.md](ARCHITECTURE.md).
