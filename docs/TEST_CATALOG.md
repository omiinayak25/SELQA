# OmiinQA — Test Catalog

Snapshot of the current automated coverage. Counts are mechanical
(`grep '@Test'` + Gherkin scenarios) and grow toward the 500+ target on the roadmap.

## Summary
| Layer | Files | Tests | Suite | External need |
|---|---|---|---|---|
| Unit (framework core) | 2 | 9 | `testng-smoke.xml` | none (offline) |
| API | 5 | 56 | `testng-api.xml` | network (public sandboxes) |
| UI | 2 | 6 | `testng-ui.xml` | browser / Grid |
| E2E | 1 | 1 | `testng-e2e.xml` | browser / Grid + network |
| Database | 4 | 36 | `testng-database.xml` | live PostgreSQL/MySQL |
| BDD (Cucumber) | 5 | 5 scenarios (+outline rows) | `testng-bdd.xml` | browser / Grid |
| **Total** | **19** | **108 @Test + BDD** | | |

## Unit (`com.omiinqa.unit`) — offline smoke gate
- `ConfigManagerTest` — base load, int defaults, missing-key failure, `-D` precedence, typed facade.
- `EnumParsingTest` — browser/env/mode parsing, options-strategy factory mapping & capability build.

## API (`com.omiinqa.api`)
- `ReqResUserApiTest` (16) — GET positive/boundary/negative, pagination, POST (data-driven), PUT, PATCH, DELETE.
- `BookingCrudApiTest` (13) — auth, CRUD with request chaining (token→create→get→update→delete), 403/404 negatives.
- `DummyJsonApiTest` (17) — product list/paging/skip, single product, search, categories, login→profile chain, 401.
- `SchemaValidationApiTest` (10) — JSON-schema contract validation for ReqRes & Booking.
- Coverage types: positive · negative · boundary · CRUD · chaining · schema.

## UI (`com.omiinqa.ui.saucedemo`)
- `LoginTest` (3+) — standard login, locked-out rejection, invalid/empty creds (data-driven).
- `ProductsTest` (3) — inventory listing, cart-badge updates, sort ordering.

## E2E (`com.omiinqa.e2e`)
- `PurchaseJourneyE2ETest` — full login→cart→checkout→confirmation via `CheckoutFlow`.

## Database (`com.omiinqa.database`) — group `database`
- `UserRepositoryTest`, `ProductRepository` coverage, `QueryExecutorTest`
  (incl. SQL-injection resistance), `TransactionTest` (commit/rollback), `DatabaseAssertionsTest`.

## BDD (`src/test/resources/features`)
- `login.feature` — successful login, locked-out, invalid-credentials outline.
- `checkout.feature` — two-item and single-item purchase.

## Coverage dimensions (per the brief)
Positive · Negative · Boundary · Cross-browser (suite) · Data-driven (CSV/JSON/Excel/Faker) ·
API · Database · E2E · BDD. Accessibility / Visual / Performance / Security layers are
scaffolded and scheduled in [ROADMAP.md](ROADMAP.md) v1.1.
