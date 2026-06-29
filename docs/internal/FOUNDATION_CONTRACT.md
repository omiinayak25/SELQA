# OmiinQA Foundation Contract (READ-ONLY — already implemented)

You are extending an existing enterprise Selenium+Java framework. The foundation
below is **already written and compiling**. Do NOT recreate, modify, or import
anything that conflicts with it. Build only your assigned package(s).

## Build / environment
- Java 21, Maven. Group/base package: `com.omiinqa`.
- To compile (only if asked): `source $HOME/.local/tools/env.sh && mvn -q test-compile`.
- **Do NOT modify `pom.xml`, foundation classes, or other agents' packages.**
- **Do NOT run `git`. Do NOT run `mvn` unless explicitly told.**
- All listed dependencies are already on the classpath (Selenium 4.27, REST Assured 5.5,
  Jackson 2.18, TestNG 7.10 [compile scope], Lombok 1.18, Log4j2/SLF4J, AssertJ 3.27,
  datafaker 2.4 [`net.datafaker.Faker`], HikariCP 6.2, postgresql + mysql-connector-j,
  Apache POI 5.3, OpenCSV 5.9, Allure, awaitility, commons-lang3).

## `com.omiinqa.config.FrameworkConfig` (singleton facade)
`FrameworkConfig.get()` then: `browser()`, `headless()`, `executionMode()`, `gridUrl()`,
`environment()`, `implicitTimeout()/explicitTimeout()/pageLoadTimeout()/scriptTimeout()/pollingInterval()`
(all `java.time.Duration`), `retryCount()`, `driverRetryCount()`, `maximize()`,
`windowWidth()`, `windowHeight()`, `screenshotOnFailure()`, `screenshotOnSuccess()`,
`appUrl(String key)`, `apiUrl(String key)`, `raw(String key)`, `raw(String key, String default)`.
- `appUrl` keys: `saucedemo`, `orangehrm`.
- `apiUrl` keys: `restfulbooker`, `reqres`, `dummyjson`, `petstore`, `jsonplaceholder`, `httpbin`, `countries.graphql`.
- Low-level: `com.omiinqa.config.ConfigManager.get().get(key)/getInt(key)/getBoolean(key,def)`.

## `com.omiinqa.enums`: `BrowserType`, `Environment`, `ExecutionMode` (each has `from(String)`).

## `com.omiinqa.exceptions` (all unchecked, extend `FrameworkException`)
`FrameworkException`, `ConfigurationException`, `DriverInitializationException`,
`DataException`, `DatabaseException`, `ApiException`. Each: `(String)` and `(String, Throwable)` ctors.

## `com.omiinqa.driver.DriverManager` (static)
`startDriver()`, `getDriver()` → `WebDriver`, `quitDriver()`, `hasDriver()`.

## `com.omiinqa.core` base classes
- `abstract BasePage`: `protected WebDriver driver()`; `protected void click(By)`, `jsClick(By)`,
  `type(By,String)`; `protected String getText(By)`, `getAttribute(By,String)`;
  `protected List<WebElement> findAll(By)`; `protected void selectFromDropdownByVisibleText(By,String)`;
  `protected boolean isDisplayed(By)` / `isDisplayed(By,Duration)`; `public String currentUrl()`,
  `pageTitle()`; `protected void waitForUrlContains(String)`; `protected final Logger log`.
- `abstract BaseComponent extends BasePage`: ctor `BaseComponent(WebElement root)`; `protected WebElement root`;
  `findInRoot(By)`, `findAllInRoot(By)`, `existsInRoot(By)`, `isLoaded()`.
- `abstract BaseTest` (`@Listeners(TestListener.class)`): `@BeforeMethod setUp()` starts driver,
  `@AfterMethod tearDown()` quits; `protected WebDriver driver()`; `protected String open(String url)`;
  `protected FrameworkConfig config`; `protected final Logger log`.

## `com.omiinqa.utils` (static helpers)
- `WaitUtils`: `visible(driver,By)`, `clickable(driver,By)`, `present(driver,By)`, `allVisible(driver,By)`,
  `invisible(driver,By)`, `textPresent(driver,By,String)`, `urlContains(driver,String)`,
  `isVisible(driver,By,Duration)`, `until(driver,condition[,Duration])`.
- `JavaScriptUtils`: `execute`, `scrollIntoView`, `click`, `scrollToBottom`, `documentReadyState`, `pageLoadTimeMillis`.
- `ScreenshotUtils`: `captureBytes(driver)`, `capture(driver,name)`.

## Conventions you MUST follow
- Production-grade: Javadoc on every public type explaining its role and (where relevant)
  the design pattern it implements and WHY.
- No deprecated Selenium APIs. Use `Duration`-based waits. Page objects contain NO assertions.
- TestNG tests: group via `@Test(groups = {...})`. UI tests extend `BaseTest`. Tests must COMPILE;
  they need not run (no browsers/DB installed in this env). Guard DB/live tests behind groups.
- Use `org.slf4j.Logger` / `LoggerFactory` for logging. Use Lombok where it removes boilerplate.
- Return, as your final message, a concise manifest: files created + one-line purpose each,
  plus the public API (class + key method signatures) other layers can call.
