# OmiinQA — FAQ

**Q: What makes this more than a "Selenium POM sample"?**
A stable core (config, ThreadLocal driver, waits, logging, observability) wrapped by
interaction layers (pages, components, API services, DB repositories), orchestrated by
business-flow facades, exercised by 569+ tests across UI/API/DB/E2E/BDD plus accessibility,
visual, performance, security, responsive, cloud and protocol (SOAP/WS/SSE/OAuth2) layers —
with full CI/CD, reporting, and SBOM/SAST/secret-scanning.

**Q: Can I run it without a browser or database?**
Yes. `testng-smoke.xml` (unit core) and `testng-db-embedded.xml` (H2) run fully offline.
`testng-api-smoke.xml` needs only network.

**Q: How do I run on a real Selenium Grid?**
`docker compose -f docker/docker-compose-grid.yml up -d` then
`mvn test -Dsuite.file=testng-ui.xml -Dexecution.mode=grid -Dbrowser=remote-chrome`.

**Q: How do I run on BrowserStack / Sauce Labs / LambdaTest?**
Set `cloud.<provider>.username/.accesskey` (and optionally `.url`), then use the
`com.omiinqa.cloud` provider. See [Cloud Guide](CLOUD_GUIDE.md). Capabilities are built
by a per-vendor Strategy; execution requires a paid account.

**Q: How is configuration resolved?**
`-Dkey=value` (CLI) > `config/env/<env>.properties` > `config/config.properties`.

**Q: Why no assertions in page objects?**
Pages model interaction; tests own verification. This keeps pages reusable across
positive/negative/boundary scenarios and points failures at the test's intent.

**Q: How do you keep parallel runs safe?**
A `ThreadLocal<WebDriver>` per method; `quitDriver()` always `remove()`s. Logs carry a
per-test MDC token + correlationId so parallel output stays readable.

**Q: Are the "AI" features calling an LLM?**
The self-healing locator and failure categorizer work **heuristically by default** (no
network). An optional AI provider hook can be enabled with credentials; without them the
framework degrades gracefully to deterministic behavior. Nothing is faked.

**Q: How do I add a browser / auth scheme / data source?**
Add a new Strategy class (and register it) — never edit a `switch`. See
[Developer Guide](DEVELOPER_GUIDE.md).

**Q: What's verified vs. needs infrastructure?**
Offline gates (unit, embedded DB) and stable live API run green in CI. UI-on-Grid runs in
CI. DB-on-live, cloud providers, and some protocol endpoints need external infra/accounts —
clearly marked and documented, never silently skipped.

**Q: How do I generate an SBOM / run mutation testing?**
`mvn -P sbom package -DskipTests` (CycloneDX `target/bom.json`); `mvn -P pitest test
pitest:mutationCoverage` (PIT report under `target/pit-reports`).
