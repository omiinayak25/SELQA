# OmiinQA — Roadmap

Status legend: ✅ done · 🚧 in progress · ⏭️ planned

## v1.0 — Foundation & breadth (current)
- ✅ Build, dependency management, quality/security profiles
- ✅ Config, driver (ThreadLocal/Grid), core base classes, waits, logging
- ✅ UI POM (SauceDemo + OrangeHRM), components, business flows
- ✅ API framework (client, builder, auth, validators, services, models)
- ✅ Database layer (pool, executor, transactions, repositories, assertions)
- ✅ Data layer (readers, faker, builders, factories, providers)
- ✅ Reporting (Allure + Extent), retry + lifecycle listeners
- ✅ TestNG suites, BDD (Cucumber), offline smoke
- ✅ Docker + Selenium Grid, GitHub Actions / Jenkins / Azure pipelines
- 🚧 Grow to 500+ meaningful scenarios across layers (no duplicates)

## v1.1 — Specialized layers (depth)
- ⏭️ Accessibility: axe-core scans wired into UI tests with violation budgets
- ⏭️ Visual: baseline capture + diff with dynamic-region masking
- ⏭️ Performance: navigation-timing budgets + optional Lighthouse hook
- ⏭️ Security: auth/session/cookie/header checks, SQLi/XSS payload probes

## v1.2 — Reliability & insight
- ⏭️ Flaky-test detector (history-based) and quarantine group
- ⏭️ Allure history + trend retention in CI
- ⏭️ Test-impact selection (run only suites touched by a change)

## v1.3 — Scale & integrations
- ⏭️ Cloud Grid (LambdaTest/BrowserStack) execution profile
- ⏭️ Jira/Xray traceability sync from the traceability matrix
- ⏭️ Containerized full-stack E2E (app + DB via Compose) for DB-backed tests

## Definition of done (per scenario)
Deterministic, isolated, asserted in the test (not the page), tagged for the right
suite, reflected in the test catalog + traceability matrix, and green in CI.
