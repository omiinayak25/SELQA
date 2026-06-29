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
| REQ-DB-03 | Transaction commit/rollback | DB | `TransactionTest` | database | ✅* |

`✅` verified in CI · `✅*` implemented; requires a live DB to execute.
