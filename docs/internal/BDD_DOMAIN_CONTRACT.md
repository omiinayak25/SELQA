# BDD Reference-Domain Contract (READ FIRST)

You build a slice of the **reference domain** + its **executable BDD**. Every scenario must
run REAL Java business logic with REAL assertions — **no browser, no fakes, no no-op steps**.
The exemplar (`identity/authentication`) is already built; mirror its style exactly.

## Hard rules
- **Services**: `src/main/java/com/omiinqa/reference/<area>/` — plain Java, real business rules,
  per-instance in-memory state (each scenario gets a fresh service). Use
  `com.omiinqa.reference.core.DomainException(code, message)` for failures with a stable,
  asserted error `code` (e.g. `CART_OUT_OF_STOCK`). Reuse
  `com.omiinqa.reference.core.Validations` (email/password/blank). Production Javadoc.
- **Steps**: `src/test/java/com/omiinqa/bdd/steps/domain/<Domain>Steps.java`. Drive your service;
  wrap mutating calls in `DomainWorld.run(...)` / `DomainWorld.capture(...)` so shared outcome
  assertions work. Get/create your service via `DomainWorld.service("<key>", YourService::new)`.
- **Features**: `src/test/resources/features/domain/<area>/<name>.feature`.
  Tag every Feature with `@domain` plus an area tag and per-scenario tags
  (`@positive @negative @boundary @validation @business @security @smoke @regression @sanity`).
  **NEVER use `@ui` or `@e2e`** (those start a real browser — your scenarios must run offline).
- **Use a `Background`** that creates a clean service. Use **Scenario Outline + Examples** for
  data-driven / boundary cases. Business-readable Gherkin only.

## Reuse these SHARED steps (already defined — do NOT redefine them):
- `Then the operation succeeds`
- `Then no domain error is raised`
- `Then a domain error {string} is raised`     ← asserts `DomainException.code()`
- `Then the domain error message contains {string}`
`DomainWorld` API: `service(key, supplier)`, `run(Runnable)`, `capture(Supplier)`,
`lastError()`, `lastResult()`, `put(key,val)`, `get(key)`.

## Avoiding duplicate/ambiguous steps (Cucumber fails on these)
- **Prefix your step text with your domain noun** so it is globally unique, e.g.
  `When I add product {string} to the cart`, `When I place an order for {int} units of {string}` —
  NOT bare `When I add {string}`.
- Do NOT redefine the shared steps above or any step another domain owns. One domain owns its nouns.
- One step-definition method per unique phrase. Reuse your own steps across your features.

## Quality bar
- Positive, negative, boundary, validation, business-rule, and (where relevant) authorization
  scenarios per module. Every `Then` asserts a real value or error code — nothing trivially true.
- No `Thread.sleep`, no randomness in assertions (seed if needed), no `@ui` browser steps.
- Tests must run offline:
  `mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@domain'`.

## Verify before returning
Run: `source $HOME/.local/tools/env.sh && mvn -q test-compile` (must pass), then
`mvn -B test -Dtest=CucumberTestRunner -Dcucumber.filter.tags='@<your-area-tag>' -DfailIfNoTests=false`
and ensure 0 undefined/ambiguous steps and 0 failures. Report feature files + scenario count.
