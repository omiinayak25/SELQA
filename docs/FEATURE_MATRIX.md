# OmiinQA — Feature Matrix

Legend: ✅ implemented & verified · 🟢 implemented, needs external infra/account to execute ·
🧩 framework/hooks provided (deterministic default), AI-backed optional.

## Test types
| Capability | Status | Where |
|---|---|---|
| UI automation (POM + components + flows) | ✅ | `pages`, `components`, `businessflows`, `ui` tests |
| API automation (REST) | ✅ | `api.*`, 7 public apps |
| API — GraphQL | ✅ | `api.graphql` |
| API — SOAP | ✅ | `api.soap` |
| API — WebSocket | 🟢 | `api.websocket` (live echo) |
| API — SSE | ✅/🟢 | `api.sse` (offline parser + live) |
| API — multipart / file upload | 🟢 | `api.multipart` (httpbin) |
| API — OAuth2 (client-creds/password) + token cache | ✅ | `api.oauth` |
| Database validation (SQL) | ✅ | `database` + embedded-H2 gate |
| Database — embedded zero-infra gate | ✅ | `EmbeddedDbIntegrationTest` (H2) |
| E2E (UI+API+DB) | ✅/🟢 | `e2e` |
| BDD (Cucumber) | ✅ | `bdd`, 13 features / 151 scenarios |
| Accessibility (axe WCAG 2.1 AA) | 🟢 | `accessibility` |
| Visual regression (pixel diff + ignore regions) | 🟢 | `visual` |
| Performance smoke (navigation timing budgets) | 🟢 | `performance` |
| Security (SQLi/XSS/session/headers + payloads) | 🟢 | `security` |
| Responsive (viewport matrix) | 🟢 | `responsive` |
| Cross-browser (Chrome/Firefox/Edge) | 🟢 | suites + options strategies |
| Parallel execution (ThreadLocal) | ✅ | `driver`, suites |
| Data-driven (JSON/CSV/Excel/YAML/XML/Faker) | ✅ | `utils.data`, `data` |
| Synthetic / relational data + masking | ✅ | `data.synthetic` |
| Localization / i18n | ✅ | `i18n` |
| Resilience (retry/circuit-breaker/bulkhead/chaos) | ✅ | `resilience` |
| Self-healing locators (heuristic) | ✅ | `intelligence` |
| Failure categorization / flaky detection | ✅ | `intelligence` |
| AI assist (optional, credentialed) | 🧩 | `intelligence.ai` |

## Execution & infra
| Capability | Status |
|---|---|
| Local / headless | ✅ |
| Selenium Grid (Docker Compose) | 🟢 (runs in CI) |
| Cloud: BrowserStack / Sauce Labs / LambdaTest | 🟢 (caps unit-tested) |
| Docker runner image | 🟢 |

## Reporting & observability
| Capability | Status |
|---|---|
| Allure | ✅ |
| Extent HTML | ✅ |
| JSON / CSV / XML / Markdown exporters | ✅ |
| GitHub Step Summary / Slack / Teams / Email | ✅ |
| JUnit XML (surefire) | ✅ |
| Correlation IDs + execution timeline | ✅ |
| Metrics + Prometheus exposition | ✅ |
| Tracing (OTel-shaped spans) | ✅ |

## DevOps / quality / security
| Capability | Status |
|---|---|
| GitHub Actions / Jenkins / Azure / GitLab CI | ✅ |
| Checkstyle / PMD / SpotBugs / JaCoCo | ✅ |
| OWASP Dependency-Check | ✅ |
| Maven Enforcer | ✅ |
| Mutation testing (PIT) | ✅ (profile) |
| SBOM (CycloneDX) | ✅ (profile) |
| CodeQL SAST | ✅ (workflow) |
| Secret scanning (gitleaks) | ✅ (workflow) |
| Dependabot | ✅ |
| Release drafter | ✅ |

> "Needs infra" items are real, compiling, unit-verified where possible; they require a
> browser/Grid/DB/cloud-account/credential to *execute* — documented, never silently skipped.
