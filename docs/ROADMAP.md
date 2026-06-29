# OmiinQA — Roadmap

Status: ✅ done · 🚧 in progress · ⏭️ planned

## v1.0 — Foundation (✅ complete, build green)
Build, config, driver/Grid, core base classes, UI POM, API framework, DB layer, data
layer, reporting, BDD, Docker, CI/CD, quality gates, docs. 108 `@Test` + 5 BDD scenarios.

## v2.0 — Enterprise coverage expansion (✅ targets met)
Goal: **600+ meaningful, non-duplicate scenarios** + **150–200 reusable BDD scenarios**.
**Delivered: 558 `@Test` + 151 executable BDD = 665+ scenarios.** Full UI/API/DB/E2E
coverage, specialized layers, synchronized docs — all compiling, smoke green, BDD dry-run clean.

### Target scenario distribution
| Domain | Target | Primary source |
|---|---|---|
| Authentication | 60 | SauceDemo, OrangeHRM, DummyJSON |
| Authorization | 40 | OrangeHRM, API tokens |
| Dashboard | 30 | OrangeHRM |
| Navigation | 30 | SauceDemo, OrangeHRM |
| Inventory / Products | 80 | SauceDemo, DummyJSON, Petstore |
| Shopping Cart | 60 | SauceDemo |
| Checkout | 80 | SauceDemo |
| Forms & Validation | 50 | SauceDemo, OrangeHRM |
| Search & Filters | 40 | DummyJSON, JSONPlaceholder |
| Tables & Pagination | 40 | OrangeHRM, ReqRes, JSONPlaceholder |
| File Upload / Download | 30 | httpbin, Petstore |
| Browser Compatibility | 25 | cross-browser suite |
| Responsive / Mobile | 25 | viewport matrix |
| Accessibility | 35 | axe-core |
| Visual Regression | 30 | baseline diff |
| Performance Smoke | 20 | navigation timing |
| API Automation | 80 | all 7 API apps |
| Database Validation | 50 | repositories |
| API → UI → DB E2E | 40 | cross-layer |
| Security | 25 | headers, SQLi/XSS |
| Network & Retry | 20 | httpbin, retry |
| Error Handling | 30 | all layers |
| **Total** | **600+** | |

### Implementation waves
- **Wave A — API breadth** ✅: JSONPlaceholder (69), httpbin (43), GraphQL (28), Petstore (42).
- **Wave B — UI breadth** ✅: SauceDemo deep (67), OrangeHRM (41), the-internet UI mechanics (63).
- **Wave C — specialized layers** ✅: accessibility, performance, visual, security, responsive (+utilities).
- **Wave D — DB + E2E + BDD** ✅: DB depth (88), E2E (36), BDD (+139 scenarios).
- **Wave E — sync** ✅: docs, traceability, metadata regen, final report.

Each wave: build → integrate → `mvn test-compile` → offline smoke → commit. Done.

## v2.1+ — Reliability & scale (⏭️ later)
Flaky detector + quarantine, Allure history/trends, test-impact selection, cloud Grid
(LambdaTest/BrowserStack), Jira/Xray traceability sync, containerized DB in CI.

## Definition of done (per scenario)
Deterministic · isolated · asserted in the test (not the page) · tagged for the right suite ·
distinct business value (no duplicate checks) · reflected in catalog + traceability · green in CI.
