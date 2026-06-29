# 0001 — ThreadLocal WebDriver for parallel isolation

- Status: Accepted
- Context: TestNG runs `@Test` methods across a thread pool. A shared static `WebDriver`
  would let parallel tests stamp on each other's session, producing nondeterministic failures.
- Decision: Bind the driver to the running thread via `ThreadLocal<WebDriver>` in
  `DriverManager`. `quitDriver()` always calls `remove()`.
- Consequences: Every parallel test gets an isolated browser; the access API stays global and
  parameter-free. Threads are never returned to the pool carrying a dead session (no leak).
  Trade-off: tests must not share a driver across threads intentionally.
