# OmiinQA — Framework Flow

End-to-end control flow of a single test run.

## 1. Suite start
```
mvn test -Dsuite.file=testng-ui.xml -Dbrowser=chrome
  └─ Surefire reads suites/testng-ui.xml
       └─ registers listeners: RetryTransformer, TestListener, ExtentReportListener
            └─ AspectJ weaver attached for Allure @Step
```

## 2. First config touch
```
FrameworkConfig.get() → ConfigManager.get()
  ├─ load config.properties (base)
  ├─ resolve env (-Denv or base) → load config/env/<env>.properties (overlay)
  └─ subsequent get(key): -D system property > overlay > base
```

## 3. Per-test lifecycle (BaseTest, Template Method)
```
@BeforeMethod setUp()
  └─ DriverManager.startDriver()
       └─ DriverFactory.create(browser, mode, headless)
            ├─ OptionsStrategyFactory.forBrowser(...) → BrowserOptionsStrategy.build(headless)
            ├─ local → WebDriverManager.setup() + new XDriver(options)
            │   remote/grid → new RemoteWebDriver(gridUrl, caps)
            ├─ DriverRetry wraps creation (bounded retries)
            └─ apply session policy (timeouts, window) → bind to ThreadLocal

@Test
  └─ test calls page/flow methods
       └─ BasePage.click/type/getText → WaitUtils.fluent(...).until(...)
            └─ assertions in the TEST (AssertJ)

@AfterMethod tearDown()
  └─ DriverManager.quitDriver()  (driver.quit() + ThreadLocal.remove())
```

## 4. Listener side-effects (Observer/Adapter)
```
onTestStart   → MDC.put(testName), log ▶, Extent createTest
onTestSuccess → log ✔, Extent PASS
onTestFailure → log ✘, screenshot → Allure + screenshots/, Extent FAIL
RetryAnalyzer → re-run up to retry.count on failure
onFinish      → suite summary; ExtentManager.flush()
```

## 5. Parallel execution
TestNG runs `@Test` methods across a thread pool (`parallel="methods"`). Each thread has its
own ThreadLocal driver and MDC token, so browsers and logs never cross between tests.

## 6. API / DB / BDD variants
- **API** tests skip the driver entirely — `ApiClient` builds a `RequestSpecification`
  (RequestBuilder + AuthenticationStrategy), sends, and `ResponseValidator`/`SchemaValidator` assert.
- **DB** tests borrow a pooled connection (`ConnectionManager`), run parameterized queries
  (`QueryExecutor`) inside transactions (`TransactionManager`), and verify with `DatabaseAssertions`.
- **BDD** replaces BaseTest with `CucumberHooks` (@Before/@After) and shares page state through
  `ScenarioContext`.
```
