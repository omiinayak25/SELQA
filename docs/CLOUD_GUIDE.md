# OmiinQA — Cloud Execution Guide

Run the UI suite on a cloud browser grid (BrowserStack, Sauce Labs, LambdaTest) using the
`com.omiinqa.cloud` package. Capabilities are built by a per-vendor **Strategy**; the driver
is a standard `RemoteWebDriver` against the vendor hub. **Execution requires a paid account**
— the framework builds and submits the session; the cloud runs the browser.

## 1. Credentials (never commit)
Provide via `-D`, environment, or an untracked `secrets.properties`:
```bash
# BrowserStack
-Dcloud.browserstack.username=USER -Dcloud.browserstack.accesskey=KEY
# Sauce Labs
-Dcloud.saucelabs.username=USER -Dcloud.saucelabs.accesskey=KEY
# LambdaTest
-Dcloud.lambdatest.username=USER -Dcloud.lambdatest.accesskey=KEY
```
Optional hub override: `-Dcloud.<provider>.url=https://...`. Sensible vendor defaults are
built in.

## 2. Build capabilities
```java
MutableCapabilities caps = new CloudCapabilityFactory().build(
        CloudProvider.BROWSERSTACK, BrowserType.CHROME,
        "Windows", "11", "latest",
        "OmiinQA Checkout", "build-#42");
```
Each provider injects its W3C namespace (`bstack:options` / `sauce:options` / `LT:Options`)
with credentials + metadata (os, version, session/build names) merged onto the base browser
options from `OptionsStrategyFactory`.

## 3. Start a cloud session
```java
RemoteWebDriver driver = new CloudDriverProvider(CloudProvider.SAUCELABS)
        .createDriver(BrowserType.FIREFOX, "macOS", "14", "latest",
                      "Login regression", "nightly");
```

## 4. Matrix
Drive an OS × browser matrix from a TestNG `@DataProvider` or a suite per provider; tag the
build name with the CI run id for traceability in the vendor dashboard.

## 5. Notes
- Capability construction is fully unit-tested offline (`testng` group `cloud`), so the
  integration is verified even without an account.
- Keep credentials out of logs — the logging layer never emits auth headers.
- See [EXECUTION_GUIDE](EXECUTION_GUIDE.md) for local/Grid execution.
