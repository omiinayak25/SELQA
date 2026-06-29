# OmiinQA — Project Analysis & Gap Report

> Baseline: commit `d144027`. This report drives the coverage-expansion roadmap.
> The repository is the source of truth; all expansion **extends** existing assets.

## 1. Architecture verdict
The foundation is sound and complete: layered clean architecture, ThreadLocal driver,
config Facade, base page/component/test, API client + builder + auth strategies +
validators, JDBC repository layer, data readers/builders/factories, reporting, BDD, CI/CD.
**No redesign required.** Expansion is additive: new tests, new feature files, new service/
page classes for untapped applications, and the specialized test layers.

## 2. Reusable assets to build on (do not duplicate)
- **Pages:** SauceDemo (Login, Products, ProductDetail, Cart, Checkout×3), OrangeHRM (Login, Dashboard).
- **Components:** Header, Footer, Nav, Table, Pagination, Dropdown, Modal, Toast, Breadcrumb.
- **Flows:** LoginFlow, AddToCartFlow, CheckoutFlow.
- **API:** ApiClient, RequestBuilder, ResponseValidator, SchemaValidator, auth strategies;
  services for ReqRes, Booking, DummyJSON.
- **DB:** ConnectionManager, QueryExecutor, TransactionManager, repositories, DatabaseAssertions.
- **Data:** JSON/CSV/Excel/YAML/Properties readers, TestDataFaker, builders, factories, DataProviders.
- **Utils:** WaitUtils, JavaScriptUtils, ScreenshotUtils.

## 3. Gap analysis (current → required)
| # | Area | Current | Gap | Severity |
|---|---|---|---|---|
| G1 | API apps used | ReqRes, Booking, DummyJSON | Petstore, JSONPlaceholder, httpbin, GraphQL Countries unused | **Critical** |
| G2 | API depth | CRUD + schema | pagination/filter/sort/bulk/upload/perf/contract/security depth | High |
| G3 | UI auth | 3 tests | session, all users, boundary, error-state, storage/cookies | **Critical** |
| G4 | UI inventory/cart/checkout | ~7 tests | totals math, form validation, boundary, detail, sort matrix | **Critical** |
| G5 | OrangeHRM | login+dashboard only | PIM add/search employee, admin, nav, forms | High |
| G6 | UI mechanics | none | windows/tabs, frames, cookies/storage, downloads, waits, drag&drop | High |
| G7 | Accessibility | dep present, 0 tests | axe scans + violation budgets across pages | High |
| G8 | Visual | none | baseline capture + diff util + tests | Medium |
| G9 | Performance smoke | JS timing util only | navigation budgets, page-load assertions | Medium |
| G10 | Security | none | headers, cookies, SQLi/XSS payloads (UI+API) | High |
| G11 | Responsive | none | breakpoint/viewport matrix | Medium |
| G12 | Cross-browser | suite exists | parameterized tests reading browser, factored coverage | Medium |
| G13 | Database | 36 tests (need live DB) | integrity, cross-validation with UI/API, more repos | Medium |
| G14 | E2E | 1 test | API→UI→DB journeys | High |
| G15 | BDD | 5 scenarios | 150–200 across all domains | **Critical** |
| G16 | Negative/Boundary | partial | systematic BVA/EP/decision-table coverage | High |
| G17 | Data-driven | providers exist | wire across new tests; more datasets | Medium |
| G18 | Docs | good | keep synced with expansion; PROJECT_ANALYSIS (this) | Low |

## 4. Honest constraints (no inflated numbers)
- **SauceDemo / OrangeHRM are small apps.** Genuinely distinct UI scenarios are finite;
  breadth is reached legitimately via data-driven variants (BVA/EP), cross-browser
  parameterization, responsive viewports, accessibility/visual/perf lenses on the same
  pages — each asserting a *different* property, not re-running the same check.
- **Volume leans on the richer APIs** (JSONPlaceholder, httpbin, Petstore, GraphQL, DummyJSON)
  where distinct, meaningful endpoints and contracts genuinely exist.
- **DB/live-network tests** are authored to compile and run when infrastructure is present;
  they are tagged and excluded from the offline gate. This is stated, not hidden.

## 5. Execution plan
Implemented in waves (A–E), each: build via parallel module agents → integrate → `mvn
test-compile` → offline smoke → commit. See [docs/ROADMAP.md](docs/ROADMAP.md) for the
scenario distribution and module sequence.
