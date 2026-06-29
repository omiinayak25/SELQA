# OmiinQA — Architecture & Flow Diagrams

Rendered Mermaid diagrams of the framework's structure and runtime behavior.
(GitHub renders Mermaid natively.)

## 1. Layered architecture
```mermaid
flowchart TB
    subgraph TEST["Test Layer (assertions live here)"]
      UI[ui] & API[api] & DB[database] & E2E[e2e] & BDD[bdd]
      A11Y[accessibility] & VIS[visual] & PERF[performance] & SEC[security] & RESP[responsive]
    end
    subgraph ORCH["Orchestration"]
      FLOWS[businessflows • Facade] & SVC[api.services] & REPO[database.repositories]
    end
    subgraph DOMAIN["Domain / Interaction"]
      PAGES[pages] & COMP[components] & BUILD[api.builder/validator] & DATA[data]
    end
    subgraph CORE["Core (stable)"]
      CFG[config] & DRV[driver • ThreadLocal] & WAIT[utils/waits] & LOG[logging]
      EXC[exceptions] & LIS[listeners] & REP[reports] & OBS[observability]
    end
    TEST --> ORCH --> DOMAIN --> CORE
    TEST -.-> CORE
```

## 2. Driver lifecycle (per test method)
```mermaid
sequenceDiagram
    participant T as Test (BaseTest)
    participant DM as DriverManager (ThreadLocal)
    participant DF as DriverFactory
    participant OS as OptionsStrategy
    participant WD as WebDriver
    T->>DM: @BeforeMethod startDriver()
    DM->>DF: create(browser, mode, headless)
    DF->>OS: forBrowser(browser).build(headless)
    OS-->>DF: capabilities
    alt local
      DF->>WD: WebDriverManager.setup() + new XDriver(caps)
    else remote/grid/cloud
      DF->>WD: new RemoteWebDriver(hubUrl, caps)
    end
    DF-->>DM: driver (bound to thread, retry-wrapped)
    T->>WD: page/flow actions (wait-then-act)
    T->>DM: @AfterMethod quitDriver()
    DM->>WD: quit() + ThreadLocal.remove()
```

## 3. Configuration resolution (precedence)
```mermaid
flowchart LR
    CLI["-Dkey=value (CLI / CI)"] --> R{ConfigManager.get}
    ENV["config/env/&lt;env&gt;.properties"] --> R
    BASE["config/config.properties"] --> R
    R --> FC[FrameworkConfig • typed Facade]
    FC --> APP[browser • timeouts • urls • …]
```

## 4. API request flow
```mermaid
sequenceDiagram
    participant TST as API Test
    participant SVC as Service (Facade)
    participant RB as RequestBuilder (Builder)
    participant AUTH as AuthStrategy
    participant AC as ApiClient
    participant RV as ResponseValidator
    TST->>SVC: businessOperation(args)
    SVC->>RB: baseUri/path/headers/body
    RB->>AUTH: apply(spec)
    AUTH-->>RB: authed spec
    RB-->>SVC: RequestSpecification
    SVC->>AC: get/post/put/delete(spec)
    AC-->>SVC: Response (logged + Allure)
    TST->>RV: of(resp).statusCode().bodyJsonPath().matchesSchema()
```

## 5. Database flow
```mermaid
flowchart LR
    RT[DB Test] --> RP[Repository]
    RP --> QE[QueryExecutor • PreparedStatement only]
    RP --> TX[TransactionManager • commit/rollback]
    QE --> CM[ConnectionManager • HikariCP pool]
    TX --> CM
    CM --> H2[(H2 embedded — CI)]
    CM --> PG[(PostgreSQL / MySQL — live)]
    RT --> DA[DatabaseAssertions]
```

## 6. Parallel execution model
```mermaid
flowchart TB
    SUITE[TestNG suite • parallel=methods] --> P((thread pool))
    P --> T1[thread-1] --> D1[ThreadLocal driver-1] --> B1[browser-1]
    P --> T2[thread-2] --> D2[ThreadLocal driver-2] --> B2[browser-2]
    P --> T3[thread-3] --> D3[ThreadLocal driver-3] --> B3[browser-3]
    T1 -. MDC token .-> LOG[(omiinqa.log)]
    T2 -. MDC token .-> LOG
    T3 -. MDC token .-> LOG
```

## 7. CI/CD pipeline (GitHub Actions)
```mermaid
flowchart LR
    PUSH[push / PR] --> B[Build & Smoke + Embedded DB]
    B --> Q[Static Analysis]
    B --> A[API Smoke + Full]
    B --> U[UI on Selenium Grid]
    Q --> R[Publish Allure Report]
    A --> R
    U --> R
    PUSH --> CQ[CodeQL SAST]
    PUSH --> SEC[Secret Scan • OWASP • SBOM]
```

## 8. Reporting & observability
```mermaid
flowchart LR
    RUN[Test run] --> LIS[TestListener]
    RUN --> EXT[ExtentReportListener]
    RUN --> AGG[ResultsAggregator]
    LIS --> ALLURE[(Allure)]
    LIS --> SHOT[screenshots]
    EXT --> HTML[(Extent HTML)]
    AGG --> JSON[(JSON)] & CSV[(CSV)] & XML[(XML)] & MD[(GitHub Summary)]
    AGG --> SLACK[Slack] & TEAMS[Teams] & MAIL[Email]
    RUN --> OBS[observability: correlationId • metrics • timeline]
    OBS --> PROM[(Prometheus exposition)]
```

## 9. Component composition (page objects)
```mermaid
classDiagram
    class BasePage
    class BaseComponent
    class BaseTest
    BasePage <|-- ProductsPage
    BasePage <|-- LoginPage
    BaseComponent <|-- HeaderComponent
    BaseComponent <|-- TableComponent
    BasePage <|.. BaseComponent : extends
    ProductsPage o-- HeaderComponent : composes
    BaseTest ..> DriverManager : uses
```
