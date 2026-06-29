# OmiinQA — Technology Matrix

| Concern | Technology | Version | Notes |
|---|---|---|---|
| Language | Java (Temurin) | 21 LTS | records, sealed types, switch patterns, virtual-thread-ready |
| Build | Maven | 3.9.9 | BOM imports, profiles, enforcer |
| UI automation | Selenium | 4.27 | W3C, no deprecated APIs |
| Driver mgmt | WebDriverManager | 5.9 | auto-provisioning |
| Test runner | TestNG | 7.10 | parallel methods, suites, retry |
| API | REST Assured | 5.5 | + JSON schema validator |
| API (extra protocols) | JDK `java.net.http` | 21 | SOAP / WebSocket / SSE / OAuth2 (zero extra deps) |
| BDD | Cucumber | 7.20 | TestNG runner, Allure plugin |
| Serialization | Jackson | 2.18 | JSON + YAML |
| Boilerplate | Lombok | 1.18 | annotation processing |
| Logging | Log4j2 + SLF4J | 2.24 / 2.0 | rolling, MDC, correlation ids |
| Files | Apache POI / OpenCSV | 5.3 / 5.9 | Excel / CSV; XML via JDK (XXE-safe) |
| Fake/synthetic data | datafaker | 2.4 | seedable; synthetic + masking layer |
| Database (SQL) | PostgreSQL / MySQL JDBC | 42.7 / 9.1 | + HikariCP 6.2 pool |
| Database (embedded) | H2 | 2.3 | PostgreSQL-compat, CI gate |
| Accessibility | axe-core selenium | 4.10 | WCAG 2.1 AA |
| Reporting | Allure / Extent | 2.29 / 5.1 | + custom JSON/CSV/XML/MD/Slack/Teams/Email |
| Observability | (in-house, zero-dep) | — | metrics, Prometheus exposition, OTel-shaped spans |
| Containers | Docker / Selenium Grid | 4.27 | hub + chrome/firefox/edge nodes |
| Cloud | BrowserStack / Sauce / LambdaTest | — | W3C vendor options strategies |
| CI/CD | GitHub Actions / Jenkins / Azure / GitLab | — | four providers |
| Static analysis | Checkstyle / PMD / SpotBugs | 10.21 / 3.26 / 4.8 | `quality` profile |
| Coverage | JaCoCo | 0.8.12 | report on verify |
| Mutation | PIT | 1.17 | `pitest` profile |
| SBOM | CycloneDX | 2.9 | `sbom` profile (JSON+XML) |
| Security scan | OWASP DC / CodeQL / gitleaks | 11.1 / v3 / v2 | deps / SAST / secrets |
| Dep hygiene | Maven Enforcer / Dependabot | 3.5 / v2 | version baseline + auto-bumps |

## Design patterns in use
Singleton · Factory · Strategy · Builder · Repository · Facade · Template Method ·
Adapter · Decorator · Command · Observer (listeners) · Null Object (NoOpAiAssistant) ·
Circuit Breaker / Bulkhead / Retry (resilience).
