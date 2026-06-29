# OmiinQA — AI / Engineer Handover

State of the framework and how to continue it safely.

## Current state (v1.0 foundation, build green)
- **Compiles:** `mvn clean test-compile` → SUCCESS (109 main + 19 test sources).
- **Runs:** `mvn test -Dsuite.file=testng-smoke.xml` → 9/9 passing (fully offline).
- All foundational layers, UI/API/DB/data modules, BDD, reporting, Docker/Grid and
  CI/CD pipelines are in place.

## Environment notes
- JDK 21 + Maven were installed under `~/.local/tools` (no root); `~/.local/tools/env.sh`
  is sourced from `~/.bashrc`. Docker is **not** installed — Grid/UI runs need it (or a remote hub).
- No browsers installed locally → UI/E2E suites need WebDriverManager-provisioned browsers
  or a Grid. API suites need outbound network. DB suites need a live PostgreSQL/MySQL.

## Stable contracts (don't break)
See [internal/FOUNDATION_CONTRACT.md](internal/FOUNDATION_CONTRACT.md): `FrameworkConfig`,
`DriverManager`, `BasePage`/`BaseComponent`/`BaseTest`, `WaitUtils`. Changing these ripples
across every layer.

## Highest-value next steps (see ROADMAP)
1. Grow scenarios toward 500+ (UI breadth on SauceDemo/OrangeHRM, more API endpoints,
   GraphQL Countries, Petstore, JSONPlaceholder, httpbin).
2. Implement the scaffolded specialized layers: accessibility (axe), visual, performance, security.
3. Stand up a containerized PostgreSQL/MySQL so the `database` suite runs in CI.
4. Wire Allure history retention in CI for trends.

## Conventions
- Page objects: no assertions. Waits only via `WaitUtils` (no `Thread.sleep`).
- New browser/auth/data-source = new Strategy class, not a `switch` edit.
- Quality gates lenient locally, strict in CI (`-P quality`, `-P security`). Convergence
  enforcement lives in the `quality` profile by design (see ARCHITECTURE §11).

## Gotchas
- `dependencyConvergence` is intentionally not on the always-on build (REST Assured's legacy
  json-tools tree + axe-core). Re-enabling it globally will fail the build — keep it in `quality`.
- AspectJ weaver `argLine` in Surefire powers Allure `@Step`; keep it when editing the POM.
