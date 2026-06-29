# OmiinQA — BDD Guide

OmiinQA's BDD layer has **two complementary kinds** of Cucumber scenarios, so every scenario
is genuinely executable — there are **no fake UI steps**.

## 1. Two scenario kinds
| Kind | Tag | What it drives | Browser? |
|---|---|---|---|
| **UI / E2E** | `@ui`, `@e2e` | Real demo apps (SauceDemo, OrangeHRM) via Page Objects | yes (local/Grid) |
| **API** | `@api` | Live public sandbox APIs via the API services | no |
| **Reference domain** | `@domain` | A real **in-memory business domain** (identity, commerce, platform, files, i18n, security, DB) | **no — fully offline** |

A browser starts **only** for `@ui`/`@e2e` scenarios (see `CucumberHooks` tag-conditional
hooks). The large `@domain` suite executes offline in milliseconds and runs in CI.

## 2. The reference domain
`@domain` scenarios exercise a genuinely-implemented in-memory domain under
`com.omiinqa.reference.*` — each service has real validation, business rules and stable error
codes (e.g. `CART_OUT_OF_STOCK`, `ORDER_BAD_TRANSITION`, `AUTH_LOCKED`). Scenarios pass or
fail against actual logic; nothing is stubbed or trivially true.

Bounded contexts:
- **identity** — authentication, registration, profile, password management, sessions, OTP, MFA
- **access** — RBAC, authorization, admin, customer (cross-resource/privilege rules)
- **catalog** — products, search, filters, pagination
- **commerce** — shopping cart, wishlist
- **orders** — checkout (tax/shipping/discount), order lifecycle transitions
- **platform** — notifications, email, audit logs, reports, dashboard, settings
- **files** — upload/download (type/size/quota/checksum)
- **i18n** — localization & internationalization (real resource bundles, RTL/LTR)
- **security / quality** — SQLi/XSS/traversal rejection, error-handling & validation tables
- **database** — real SQL on the embedded H2 (zero-infra)

## 3. How a domain scenario executes
```
Feature step  →  <Domain>Steps  →  DomainWorld.run(() -> service.call(...))
                                     │ captures success value or DomainException
Then          →  CommonDomainSteps  ("the operation succeeds" / "a domain error X is raised")
                  asserts the recorded outcome / error code
```
`DomainWorld` (per-scenario, ThreadLocal) shares the service and the last outcome between
steps. Generic success/error assertions live **once** in `CommonDomainSteps` so no step is
duplicated across domains.

## 4. Folder & package layout
```
src/test/resources/features/
  *.feature                         # @ui / @api (SauceDemo, public APIs)
  domain/
    identity/  access/  catalog/  commerce/  orders/
    platform/  files/  i18n/  quality/  database/

src/main/java/com/omiinqa/reference/<context>/      # real domain services
src/test/java/com/omiinqa/bdd/
  steps/domain/<Domain>Steps.java                   # one class per domain
  steps/domain/CommonDomainSteps.java               # shared outcome assertions
  support/DomainWorld.java                          # per-scenario world
  hooks/CucumberHooks.java                          # tag-conditional driver
  runners/CucumberTestRunner.java                   # AbstractTestNGCucumberTests
```

## 5. Tags
`@domain` (offline domain) · `@ui`/`@e2e` (browser) · `@api` (network) · `@db` (embedded H2) ·
plus `@smoke @sanity @regression @positive @negative @boundary @validation @business @security
@authorization` and context tags (`@identity @access @catalog @commerce @orders @platform
@files @i18n @quality`).

## 6. Running BDD
```bash
# Validate every step is defined (offline, no browser) — zero undefined/ambiguous
mvn test -Dtest=CucumberTestRunner -Dcucumber.execution.dry-run=true

# Run the whole offline reference-domain suite (no browser, no network)
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@domain'

# A single context / tag combo
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@domain and @orders'
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@domain and @db'

# UI BDD (needs a browser or Grid)
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@ui'

# API BDD (needs network)
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@api'
```

## 7. Authoring rules (Cucumber best practice)
- Reuse the shared outcome steps; give new steps **domain-prefixed, unique** text.
- One step-definition method per unique phrase; no duplicate/ambiguous definitions.
- `Background` creates a clean service; `Scenario Outline + Examples` for data-driven cases.
- Every `Then` asserts a real value or error code — never trivially true.
- `@domain` scenarios must not use `@ui` (no browser) and must run fully offline.
- See [internal/BDD_DOMAIN_CONTRACT.md](internal/BDD_DOMAIN_CONTRACT.md) for the full contract.
