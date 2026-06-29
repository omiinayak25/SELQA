# OmiinQA — Execution Guide

## Prerequisites
- JDK 21, Maven 3.9+. Docker only for Grid runs. Browsers auto-provisioned (WebDriverManager).

## Common commands
```bash
mvn clean test-compile                                   # build only
mvn test -Dsuite.file=testng-smoke.xml                   # offline core smoke
mvn test -Dsuite.file=testng-api.xml                     # API (network)
mvn test -Dsuite.file=testng-ui.xml -Dbrowser=chrome     # UI local
mvn test -Dsuite.file=testng-regression.xml              # full regression
mvn test -Dtest=CucumberTestRunner                       # BDD
mvn test -Dsuite.file=testng-database.xml                # DB (needs live DB)
```

## Runtime switches (all `-D…`)
| Property | Values | Default |
|---|---|---|
| `suite.file` | any file in `src/test/resources/suites` | `testng-smoke.xml` |
| `browser` | chrome, firefox, edge, remote-chrome/-firefox/-edge | chrome |
| `headless` | true, false | true |
| `execution.mode` | local, remote, grid | local |
| `grid.url` | hub URL | `http://localhost:4444/wd/hub` |
| `env` | qa, staging, dev | qa |
| `thread.count` | integer | 4 |

## Maven profiles
| Profile | Effect |
|---|---|
| `-P quality` | Checkstyle + PMD + SpotBugs on `verify` |
| `-P security` | OWASP Dependency-Check (fails on CVSS ≥ 8) |
| `-P firefox` / `-P edge` / `-P grid` | convenience browser selection |

## Selenium Grid
```bash
docker compose -f docker/docker-compose-grid.yml up -d --scale chrome=3
# console http://localhost:4444
mvn test -Dsuite.file=testng-ui.xml -Dexecution.mode=grid \
         -Dbrowser=remote-chrome -Dgrid.url=http://localhost:4444/wd/hub
docker compose -f docker/docker-compose-grid.yml down
```

## Reports & evidence after a run
- Allure: `mvn allure:serve` (or `allure:report` → `target/site/allure-maven-plugin`).
- Extent: `extent-reports/OmiinQA-Report.html`.
- Logs: `logs/omiinqa.log`. Screenshots: `screenshots/`. JUnit XML: `target/surefire-reports`.

## Database tests
Provision PostgreSQL/MySQL with `users` and `products` tables, set credentials via
`-Ddb.postgres.user=… -Ddb.postgres.password=…` (or env/secrets), then run the database suite.
