# OmiinQA — Best Practices & Coding Standards

## Test design
- **Assert in tests, not pages.** Page objects model interaction only.
- **One reason to fail.** A test verifies one behavior; use data providers for variants.
- **Deterministic & isolated.** No order dependence; mutating tests clean up after themselves.
- **Apply formal techniques:** boundary-value analysis, equivalence partitioning, decision
  tables, state transition — and record them in the test name/Javadoc.
- **Tag every test** with `@Test(groups = {...})`: a layer (`ui/api/database/...`) plus
  `smoke`/`regression`.

## Synchronization
- All waits go through `WaitUtils` (FluentWait). **Never** `Thread.sleep`.
- Implicit wait stays at 0 — never mix implicit and explicit waits.

## Locators & pages
- Locators are `private static final By` fields.
- Page actions return the next page (fluent navigation) or a value — never a boolean named
  like an assertion.
- Compose reusable **components** (`BaseComponent`) into pages; don't grow mega-pages.

## Extensibility (Open/Closed)
- New browser → new `BrowserOptionsStrategy`. New auth → new `AuthenticationStrategy`.
  New cloud vendor → new `CloudCapabilityStrategy`. New data source → new reader.
  Add a class; don't edit a `switch`.

## API
- Use a service Facade or `RequestBuilder` + `AuthenticationStrategy`; assert with
  `ResponseValidator` / `SchemaValidator`. Validate contracts (JSON schema) for critical APIs.

## Database
- `PreparedStatement` only — never string-concatenate SQL (injection safety).
- Wrap writes in `TransactionManager`; verify with `DatabaseAssertions`.

## Errors & logging
- Throw from the framework's unchecked `FrameworkException` hierarchy.
- Log via SLF4J; never log secrets/auth headers. Use `CorrelationId` for traceability.

## Code style
- Java 21, 4-space indent, UTF-8, LF (`.editorconfig`). Enforced by Checkstyle/PMD/SpotBugs
  (`-P quality`).
- Every public type/method has Javadoc; complex blocks explain **why**, not just what.
- No dead code, no TODOs, no placeholders in `main`.

## Reliability
- Retries (`RetryAnalyzer`) shield genuine flakiness, not product bugs — keep the budget small
  and quarantine chronic offenders.
- Separate environmentally-unstable suites (httpbin, reqres) from the CI-reliable gates.

## Pull requests
- Small and focused; docs ship with code. Green smoke + embedded-DB + quality before review.
