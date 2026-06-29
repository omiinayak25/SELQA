# Contributing to OmiinQA

Thanks for investing in the framework's quality. This guide keeps the codebase
consistent and the build green.

## Ground rules
- **Java 21**, Maven 3.9+. Run `mvn -B clean test-compile` before pushing.
- Page objects contain **no assertions** — verification lives in tests/flows.
- All synchronization goes through `WaitUtils`; never add `Thread.sleep`.
- New browsers/auth schemes/data sources are added as **new Strategy classes**, not `switch` edits.
- Every public type gets Javadoc explaining its role (and pattern, where relevant).

## Branching & commits
- Branch from `main`: `feature/<area>-<short-desc>` or `fix/<area>-<short-desc>`.
- Conventional commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- Keep PRs focused; update the relevant doc in the same PR (docs and code ship together).

## Before opening a PR
```bash
mvn -B clean test-compile             # compiles
mvn -B test -Dsuite.file=testng-smoke.xml   # offline smoke passes
mvn -B -P quality verify -DskipTests  # Checkstyle / PMD / SpotBugs clean
```

## Adding tests
- Place by layer: `src/test/java/com/omiinqa/{ui,api,database,e2e,...}`.
- Tag with `@Test(groups = {...})` — at minimum one of `smoke|regression` plus the layer.
- UI tests extend `BaseTest`; API/DB tests do **not** (no browser needed).
- Data-driven tests source from `data.provider.DataProviders` or `testdata/` files.
- Update [docs/TEST_CATALOG.md](docs/TEST_CATALOG.md) and the traceability matrix.

## Code style
Enforced by Checkstyle (`config/checkstyle/checkstyle.xml`), PMD and SpotBugs in the
`quality` profile. The `.editorconfig` handles formatting (4-space indent, LF, UTF-8).

## Reporting bugs / requesting features
Open a GitHub issue with reproduction steps, expected vs actual, environment, and logs
(`logs/omiinqa.log`) or the Allure/Extent report where useful.
