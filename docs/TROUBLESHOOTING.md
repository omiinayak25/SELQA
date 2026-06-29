# OmiinQA — Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `No WebDriver on thread …` | `getDriver()` called before `startDriver()` | Extend `BaseTest`, or call `DriverManager.startDriver()` in setup |
| Driver fails to start locally | Browser not installed / driver mismatch | WebDriverManager auto-provisions; ensure the browser binary exists, or run on Grid |
| `Listener … not found in project's classpath` | A source package excluded by an unanchored `.gitignore` rule | Anchor the ignore (`/reports/` not `reports/`); confirm `git ls-files` lists the class |
| Tests hang then time out | Implicit + explicit wait mix | All waits go through `WaitUtils`; implicit wait is pinned to 0 |
| `dependencyConvergence` build failure | Conflicting transitive versions | Convergence runs only in the `quality` profile; pin the version in `dependencyManagement` |
| API tests 401 (reqres) | reqres.in is now key-gated | Pass `-Dapi.reqres.apikey=<key>`; reqres is excluded from `testng-api-smoke.xml` |
| API tests 503 (httpbin) | httpbin.org overloaded | Environmental; not in the smoke gate. Retry later or self-host httpbin |
| DB tests skipped/failing | No live PostgreSQL/MySQL | Use `testng-db-embedded.xml` (H2, zero-infra) or provision a DB + apply `schema.sql` |
| Grid run can't reach hub | Hub not ready | `curl http://localhost:4444/wd/hub/status` until 200 before running |
| Allure report empty | No results / agent missing | Ensure `target/allure-results` exists; AspectJ weaver `argLine` enables `@Step` |
| Flaky UI test | Real network/UI flakiness | RetryAnalyzer retries (configurable `retry.count`); quarantine chronic offenders |
| Lombok methods "missing" in IDE | Annotation processing off | Enable annotation processing; the Maven build configures the Lombok processor path |
| `@{argLine}` literal in JVM args | JaCoCo agent property unset | Surefire `argLine` uses `@{argLine}` to append to JaCoCo; keep JaCoCo `prepare-agent` bound |

## Useful commands
```bash
mvn -B clean test-compile                         # compile everything
mvn -B test -Dsuite.file=testng-smoke.xml         # offline core
mvn -B test -Dsuite.file=testng-db-embedded.xml   # embedded DB (H2)
mvn -B test -Dsuite.file=testng-api-smoke.xml     # stable live APIs
mvn -B -P quality verify -DskipTests              # static analysis
mvn -X ...                                         # debug logging
```

## Where to look
- `logs/omiinqa.log` (rolling, per-test MDC token + correlationId)
- `target/surefire-reports/*.dumpstream` (forked-process crashes)
- `screenshots/` (failure evidence) · `extent-reports/OmiinQA-Report.html` · `mvn allure:serve`
