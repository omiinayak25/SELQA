# 0004 — Embedded H2 for a zero-infra database gate

- Status: Accepted
- Context: The `database` test group needs a live PostgreSQL/MySQL, so it was compile-only in CI.
- Decision: Add an in-memory H2 (PostgreSQL-compat mode) that redirects `db.postgres.*` config
  and bootstraps `embedded-h2.sql`, exercising the real ConnectionManager/QueryExecutor/
  TransactionManager/repository/assertion stack with no external service.
- Consequences: The DB layer is genuinely executed in CI (`testng-db-embedded.xml`, 11/11).
  Trade-off: H2's PostgreSQL mode is not 100% identical to a real server; vendor-specific
  behavior is still covered by the live `database` suite when infra is available.
