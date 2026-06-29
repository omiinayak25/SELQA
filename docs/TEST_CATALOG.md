# OmiinQA — Test Catalog

Snapshot of automated coverage at commit `0ad0aae`. Counts are mechanical
(`grep '@Test'` + Gherkin scenarios). **558 `@Test` methods + 151 executable BDD
scenarios = 665+ meaningful, non-duplicate scenarios.**

## Summary by layer
| Layer | Tests | Suite | External need |
|---|---|---|---|
| Unit (framework core) | 9 | `testng-smoke.xml` | none (offline) |
| API | 238 | `testng-api.xml` | network |
| UI | 171 | `testng-ui.xml` | browser / Grid |
| E2E | 36 | `testng-e2e.xml` | browser + network (+DB subset) |
| Database | 88 | `testng-database.xml` | live PostgreSQL/MySQL |
| Accessibility | 4 | `testng-specialized.xml` | browser / Grid |
| Performance | 4 | `testng-specialized.xml` | browser / Grid |
| Visual | 2 | `testng-specialized.xml` | browser / Grid |
| Security | 4 | `testng-specialized.xml` | browser / Grid |
| Responsive | 2 | `testng-specialized.xml` | browser / Grid |
| **Total `@Test`** | **558** | | |
| BDD scenarios | 107 decl / **151 exec** | `testng-bdd.xml` | browser / Grid + network |

## API (238) — by application
| Application | Tests | Coverage |
|---|---|---|
| ReqRes + Restful-Booker + DummyJSON | 56 | CRUD, auth, chaining, schema, pagination, negative |
| JSONPlaceholder | 69 | posts/comments/albums/photos/todos/users CRUD, nested, filtering, schema |
| httpbin | 43 | methods, headers, Basic/Bearer auth, status codes, cookies, gzip/deflate, redirects, timing |
| Petstore | 42 | pet/store/user CRUD with chaining, findByStatus, inventory, schema |
| GraphQL Countries | 28 | queries, variables, nested, aliases, error handling |

## UI (171) — by application
| Application | Tests | Coverage |
|---|---|---|
| SauceDemo | 67 | auth (all users/boundary), inventory, sorting matrix, product detail, cart, checkout form validation, totals math, journeys |
| OrangeHRM | 41 | auth, dashboard, sidebar nav, PIM add/search employee, Admin user search, forgot-password |
| the-internet (UI mechanics) | 63 | windows/tabs, frames/iframes (TinyMCE), JS alerts, dynamic loading/controls, hovers, drag-drop, key presses, context menu, add/remove DOM, sortable tables, basic auth, file upload, status codes, cookies, local/session storage |

## E2E (36)
API-driven UI, multi-step shopping integrity, pure-API CRUD lifecycles, and API→UI→DB
journeys (DB subset tagged `database`, excluded by default).

## Database (88 + 11 embedded) — groups `database` / `db-embedded`
Repository CRUD (User/Product/Order/Audit), parameterized-query safety, transactions
(commit/rollback/isolation), data integrity & constraints, cross-validation, aggregation
(COUNT/SUM/AVG/GROUP BY). DDL in `src/test/resources/db/schema.sql`.
- `database` (88): require a live PostgreSQL/MySQL — excluded from default runs.
- **`db-embedded` (11): `EmbeddedDbIntegrationTest` runs the real stack against in-memory H2
  (PostgreSQL-compat) — zero external infra, verified 11/11 green** (`testng-db-embedded.xml`).

## Specialized layers (16)
Accessibility (axe WCAG 2.1 AA budgets), Performance (navigation-timing budgets), Visual
(baseline pixel-diff w/ ignore-regions), Security (SQLi/XSS/session/secret-leak), Responsive
(6-viewport matrix).

## BDD (151 executable across 13 feature files)
authentication-advanced, inventory, product-sorting, shopping-cart, checkout-validation,
checkout-totals, navigation-menu, api-users (ReqRes), api-products (DummyJSON),
end-to-end-purchase, error-handling, plus original login/checkout. Validated by Cucumber
dry-run (151 scenarios, 0 undefined/ambiguous steps).

## Coverage dimensions
Positive · Negative · Boundary (BVA) · Equivalence partitioning · Decision-table · Data-driven
(JSON/CSV/Excel/Faker/Outline) · Cross-browser · Responsive · Accessibility · Visual ·
Performance · Security · API · Database · E2E · BDD · Request-chaining · Transaction/rollback.
