# OmiinQA — Architecture

## 1. Goals & principles
OmiinQA is organized as a **layered (clean) architecture**. Dependencies point
inward: tests depend on flows/pages/services, which depend on the stable core
(config, driver, utils). The core depends on nothing in the framework above it.

Guiding principles: **SOLID, DRY, KISS, YAGNI**, composition over inheritance,
dependency injection of behavior (strategies/suppliers) over hard-coded branches,
and "no abstraction without at least two concrete users."

```
┌──────────────────────────────────────────────────────────────────────┐
│ TEST LAYER   ui · api · database · e2e · bdd · accessibility · visual  │
│              · performance · security  (assertions live ONLY here)     │
├──────────────────────────────────────────────────────────────────────┤
│ ORCHESTRATION  businessflows (Facade) · api.services · db.repositories │
├──────────────────────────────────────────────────────────────────────┤
│ DOMAIN/INTERACTION  pages · components · api.builder/validator · data  │
├──────────────────────────────────────────────────────────────────────┤
│ CORE  config · driver(ThreadLocal) · utils(wait/js/screenshot) ·       │
│       logging · exceptions · listeners · reports                       │
└──────────────────────────────────────────────────────────────────────┘
```

## 2. Configuration layer
- **`ConfigManager`** (Singleton, double-checked locking) loads `config.properties`,
  overlays `config/env/<env>.properties`, and lets `-Dkey=value` win. Read-only and
  therefore safe for parallel threads.
- **`FrameworkConfig`** (Facade) exposes typed, intention-revealing accessors
  (`browser()`, `explicitTimeout()`), isolating the rest of the code from raw keys.

**Why:** one source of truth, environment portability, and CI overridability without
recompilation. Precedence is explicit and testable (see `ConfigManagerTest`).

## 3. Driver layer
- **`DriverManager`** holds a `ThreadLocal<WebDriver>` — the linchpin of safe parallel
  execution. `startDriver` / `getDriver` / `quitDriver`; `quitDriver` always `remove()`s
  so pooled threads never leak dead sessions.
- **`DriverFactory`** (Factory) decides browser × location (local vs Grid/remote) and
  applies session policy (timeouts, window). Binaries come from **WebDriverManager** — no
  deprecated `System.setProperty` paths.
- **`BrowserOptionsStrategy`** (Strategy) — one class per browser's capabilities; adding a
  browser is additive (Open/Closed). `OptionsStrategyFactory` selects the strategy.
- **`DriverRetry`** wraps creation in bounded retries with backoff (transient Grid/boot
  failures shouldn't fail a test).

## 4. Core interaction layer
- **`BasePage`** — synchronized element actions (wait-then-act), **no assertions**. This
  keeps pages reusable across positive/negative/boundary scenarios.
- **`BaseComponent`** — components scoped to a root `WebElement`; pages **compose**
  components instead of inheriting mega-pages (composition over inheritance).
- **`WaitUtils`** — all synchronization via `FluentWait`; implicit waits kept at 0 to avoid
  the implicit/explicit compounding trap.
- **`BaseTest`** — Template Method for per-method driver lifecycle + listener attachment.

## 5. API layer
REST Assured wrapped by an **`ApiClient`** + **`RequestBuilder`** (Builder). Authentication
is pluggable via an **`AuthenticationStrategy`** (Strategy: NoAuth/Basic/Bearer/ApiKey).
**`ResponseValidator`** and **`SchemaValidator`** keep assertions fluent and schema-driven.
**Services** (Facade) expose business operations (`BookingService.createBooking`) and enable
request chaining (auth token → CRUD).

## 6. Database layer
**`ConnectionManager`** (Singleton) provides HikariCP pools per `DatabaseType`. **`QueryExecutor`**
uses `PreparedStatement` exclusively (parameterized — SQL-injection safe). **`TransactionManager`**
runs units of work with commit/rollback. **Repositories** (Repository pattern) encapsulate
table access; **`DatabaseAssertions`** provide fluent verification. Connections are lazy so the
suite builds and the offline smoke runs without a live database.

## 7. Data layer
Readers for JSON/CSV/Excel/YAML/Properties, a **`TestDataFaker`** over datafaker (seedable for
reproducibility), **Builders** and **Factories** for domain objects, and TestNG
**`DataProviders`**. Static data lives in `src/test/resources/testdata`.

## 8. Reporting, logging & listeners
- **`TestListener`** (Observer) — structured logging with per-test MDC, failure screenshots
  to Allure.
- **`ExtentReportListener`** (Adapter) — bridges TestNG events into Extent; flushes at suite end.
- **`RetryAnalyzer` + `RetryTransformer`** — configurable, auto-applied retry for genuine flakiness.
- **Log4j2** — console + rolling file, parallel-safe via ThreadContext.

## 9. Execution topology
Local (WebDriverManager) · Headless (CI) · Remote/Grid (Docker Compose: hub + Chrome/Firefox/
Edge nodes) · Parallel (`parallel="methods"`, ThreadLocal driver) · Cross-browser (suite per
browser, `parallel="tests"`).

## 10. Patterns ↔ where & why
| Pattern | Location | Why |
|---|---|---|
| Singleton | ConfigManager, DriverManager, ExtentManager, ConnectionManager | Single shared, safely-published instance |
| Factory | DriverFactory, OptionsStrategyFactory | Encapsulate construction choices |
| Strategy | BrowserOptionsStrategy, AuthenticationStrategy | Swap behavior without `switch` sprawl |
| Builder | RequestBuilder, data builders | Readable construction of complex objects |
| Repository | database.repositories | Isolate persistence access |
| Facade | FrameworkConfig, businessflows, api.services | Simple surface over subsystems |
| Template Method | BaseTest, BasePage | Fix the skeleton, vary the steps |
| Decorator/Adapter | RetryTransformer, ExtentReportListener | Extend/translate framework hooks |
| Command | DriverRetry, TransactionManager | Encapsulate a deferred unit of work |

## 11. Deliberate trade-offs
- `dependencyConvergence` is enforced in the **quality** profile (CI), not the always-on
  build, because REST Assured's legacy json-tools tree and axe-core would otherwise force
  dozens of pins for zero runtime benefit. Java/Maven version + duplicate-dependency
  enforcement stay always-on.
- Static-analysis gates are lenient locally and strict in CI, so day-to-day builds stay fast
  and green while the bar is still enforced before merge.
