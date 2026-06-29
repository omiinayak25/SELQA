# OmiinQA — Test Traceability Matrix

Maps requirements/capabilities to the tests that verify them. Extend this row-per-requirement
as scenarios grow; CI can later assert every requirement has ≥1 linked, passing test.

| ID | Requirement / Capability | Layer | Verifying test(s) | Suite | Status |
|---|---|---|---|---|---|
| REQ-CFG-01 | Config precedence `-D` > env > base | Unit | `ConfigManagerTest.systemPropertyOverridesFile` | smoke | ✅ |
| REQ-CFG-02 | Missing required key fails fast | Unit | `ConfigManagerTest.throwsOnMissingRequiredKey` | smoke | ✅ |
| REQ-DRV-01 | Browser string parsing & aliases | Unit | `EnumParsingTest.browserTypeParsesAliasesAndSeparators` | smoke | ✅ |
| REQ-DRV-02 | Options strategy per browser | Unit | `EnumParsingTest.factoryReturnsMatchingStrategy…` | smoke | ✅ |
| REQ-AUTH-01 | Standard user can log in | UI | `LoginTest.standardUserCanLogIn` | ui | ✅ |
| REQ-AUTH-02 | Locked-out user rejected | UI | `LoginTest.lockedOutUserIsRejected` | ui | ✅ |
| REQ-AUTH-03 | Invalid/empty creds rejected | UI/BDD | `LoginTest.invalidCredentialsAreRejected`, `login.feature` | ui/bdd | ✅ |
| REQ-PROD-01 | Inventory lists all products | UI | `ProductsTest.inventoryListsAllProducts` | ui | ✅ |
| REQ-PROD-02 | Cart badge reflects add/remove | UI | `ProductsTest.addingItemsUpdatesCartBadge` | ui | ✅ |
| REQ-PROD-03 | Sorting reorders inventory | UI | `ProductsTest.sortByNameDescendingReordersList` | ui | ✅ |
| REQ-E2E-01 | Complete a purchase | E2E/BDD | `PurchaseJourneyE2ETest`, `checkout.feature` | e2e/bdd | ✅ |
| REQ-API-01 | ReqRes user CRUD | API | `ReqResUserApiTest` | api | ✅ |
| REQ-API-02 | Booking CRUD + auth chaining | API | `BookingCrudApiTest` | api | ✅ |
| REQ-API-03 | DummyJSON products + login chain | API | `DummyJsonApiTest` | api | ✅ |
| REQ-API-04 | Contract (JSON schema) validation | API | `SchemaValidationApiTest` | api | ✅ |
| REQ-DB-01 | Repository CRUD | DB | `UserRepositoryTest` | database | ✅* |
| REQ-DB-02 | Parameterized queries (SQLi-safe) | DB | `QueryExecutorTest` | database | ✅* |
| REQ-DB-03 | Transaction commit/rollback | DB | `TransactionTest`, `TransactionRollbackTest` | database | ✅* |
| REQ-DB-04 | Order/Audit repository CRUD | DB | `OrderRepositoryTest` | database | ✅* |
| REQ-DB-05 | Integrity & constraints | DB | `DataIntegrityTest` | database | ✅* |
| REQ-DB-06 | Aggregation (COUNT/SUM/AVG/GROUP BY) | DB | `AggregationQueryTest` | database | ✅* |
| REQ-API-05 | JSONPlaceholder resources CRUD | API | `Posts/Comments/Todos/Users/AlbumsPhotos ApiTest` | api | ✅ |
| REQ-API-06 | HTTP mechanics (headers/auth/status/cookies) | API | `Http*Test` (httpbin) | api | ✅ |
| REQ-API-07 | Petstore CRUD + chaining | API | `PetCrud/StoreOrder/PetstoreUser ApiTest` | api | ✅ |
| REQ-API-08 | GraphQL queries/variables/errors | API | `Country/Continent/Variables/Negative QueryTest` | api | ✅ |
| REQ-UI-04 | OrangeHRM PIM/Admin/nav | UI | `Pim/AdminUserSearch/Dashboard/OrangeAuthentication Test` | ui | ✅ |
| REQ-UI-05 | UI mechanics (windows/frames/alerts/upload/storage) | UI | `*MechanicsTest` (the-internet) | ui | ✅ |
| REQ-UI-06 | Checkout totals math (subtotal/tax/total) | UI | `CheckoutTotalsTest` | ui | ✅ |
| REQ-A11Y-01 | WCAG 2.1 AA — no critical/serious | A11y | `AccessibilityTest` | specialized | ✅ |
| REQ-PERF-01 | Page-load navigation-timing budget | Perf | `PerformanceSmokeTest` | specialized | ✅ |
| REQ-VIS-01 | Visual baseline regression | Visual | `VisualRegressionTest` | specialized | ✅ |
| REQ-SEC-01 | SQLi/XSS rejected; session/secret checks | Security | `SecurityTest` | specialized | ✅ |
| REQ-RESP-01 | Responsive viewport matrix | Responsive | `ResponsiveTest` | specialized | ✅ |
| REQ-E2E-02 | API→UI→DB journeys | E2E | `ApiUiDbE2ETest`, `ApiDrivenUiE2ETest` | e2e | ✅* |
| REQ-BDD-01 | Business-readable coverage (13 features) | BDD | 11 step classes, 151 scenarios | bdd | ✅ |

`✅` verified (compile + dry-run/CI) · `✅*` implemented; requires a live DB to execute.
