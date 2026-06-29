# 0002 — No assertions in page objects

- Status: Accepted
- Context: Page objects are reused across positive, negative and boundary scenarios.
- Decision: Page objects model *interaction* only and expose state via getters; *verification*
  lives in tests and business flows.
- Consequences: The same page serves every scenario; failures point at the test's intent, not
  the page. Enforced by review + Checkstyle/PMD guidance. Trade-off: slightly more code in
  tests, which is the correct place for it.
