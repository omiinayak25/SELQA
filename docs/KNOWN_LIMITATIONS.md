# OmiinQA — Known Limitations

Honest scope statement. Nothing here is hidden or silently skipped.

## Requires external infrastructure to *execute* (code is real + compiles)
- **UI / accessibility / visual / performance / responsive suites** need a real browser
  (local binary, Selenium Grid, or cloud). They run in CI on a Dockerized Grid; locally they
  need a browser installed.
- **Live `database` suite (88 tests)** needs PostgreSQL/MySQL. The **embedded-H2 gate** proves
  the stack with zero infra (11/11 in CI); vendor-specific SQL behavior still needs a live DB.
- **Cloud providers (BrowserStack/Sauce/LambdaTest)** need paid accounts + credentials.
  Capability construction is unit-tested offline; session execution is not exercised in CI.
- **Some API protocol tests** hit public endpoints that flake or gate:
  - `httpbin.org` periodically returns 503 — excluded from the API smoke gate.
  - `reqres.in` now requires an account key — excluded from the smoke gate; pass
    `-Dapi.reqres.apikey=<key>`.
  - Petstore is occasionally down; RetryAnalyzer absorbs transient failures.
  - WebSocket/SSE live tests depend on public echo servers being up.

## Intentional design boundaries
- **"AI" features are heuristic by default.** Self-healing locators, failure categorization,
  flaky detection and locator suggestion are deterministic. The `AiAssistant` hook only calls a
  model when credentials are configured — it never invents results or calls the network silently.
- **Performance layer is "smoke" grade** — navigation-timing budgets, not full load testing.
  Load/stress/soak/spike are modeled via the `resilience` + chaos utilities and documented;
  a dedicated load tool (Gatling/JMeter) is a future integration.
- **Visual regression is pixel-diff** with ignore-regions, not a hosted visual-AI service.
- **Contract testing** is JSON-schema based; consumer-driven contracts (Pact broker) are a
  documented future addition (would add the `pact-jvm` dependency + a broker).

## Not yet implemented (candidate future work)
gRPC client · Oracle/SQLServer/MongoDB/Redis live clients · Lighthouse/Core-Web-Vitals via CDP ·
Pact broker CDC · Gatling load profiles · Allure history/trend retention in CI · test-impact
selection. See [ROADMAP.md](ROADMAP.md).

## Environmental notes (this build)
- Toolchain (JDK 21 + Maven) was installed under `~/.local/tools` without root; Docker is not
  installed in the dev sandbox (Grid/cloud verified via CI, not locally).
