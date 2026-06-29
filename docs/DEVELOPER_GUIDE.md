# OmiinQA — Developer Guide

How to extend the framework without breaking its grain.

## Add a UI test
1. Create the page object under `pages/<app>/` extending `BasePage` (locators as
   `private static final By`, actions return next page or values, **no assertions**).
2. Reuse/add components under `components/` (extend `BaseComponent`, scoped to a root).
3. Write the test under `src/test/java/com/omiinqa/ui/<app>/` extending `BaseTest`.
   Assert with AssertJ. Tag `@Test(groups = {"ui","regression"[,"smoke"]})`.
4. For multi-page journeys, add a Facade in `businessflows/` and call it from the test.

```java
public class CartTest extends BaseTest {
    @Test(groups = {"ui","regression"})
    public void cartShowsAddedItem() {
        ProductsPage products = LoginFlow.loginAsStandardUser();
        products.addToCart("Sauce Labs Backpack");
        CartPage cart = products.openCart();
        assertThat(cart.getCartItemNames()).contains("Sauce Labs Backpack");
    }
}
```

## Add an API test
- Use a service Facade (`ReqResService`, `BookingService`, `DummyJsonService`) or compose a
  request with `RequestBuilder` + an `AuthenticationStrategy`, send via `ApiClient`,
  assert with `ResponseValidator` / `SchemaValidator`. API tests do **not** extend `BaseTest`.

```java
Response r = ApiClient.get(new RequestBuilder()
        .baseUri(config.apiUrl("reqres")).basePath("/users/2").build());
ResponseValidator.of(r).statusCode(200).bodyJsonPathNotNull("data.email");
```

## Add a DB test
- Borrow a connection via `ConnectionManager`, query through `QueryExecutor`
  (parameterized only), wrap writes in `TransactionManager`, verify with `DatabaseAssertions`.
  Tag `@Test(groups = "database")` (excluded from default runs).

## Add test data
- Static: drop a file in `src/test/resources/testdata/` and read via a `utils.data` reader.
- Dynamic: `TestDataFaker` or a `data.builder` / `data.factory`.
- Expose to tests via a named `@DataProvider` in `data.provider.DataProviders`.

## Add a BDD scenario
- Write Gherkin in `src/test/resources/features/`, implement steps in `bdd/steps/`
  (share page state through `ScenarioContext`), run via `CucumberTestRunner`.

## Add a browser
- Implement `BrowserOptionsStrategy`, register it in `OptionsStrategyFactory` and add the
  enum constant in `BrowserType` + an arm in `DriverFactory`.

## Conventions checklist (before PR)
- [ ] No assertions in page objects; no `Thread.sleep`.
- [ ] New public types have Javadoc (role + pattern where relevant).
- [ ] Test tagged for the correct suite/group.
- [ ] `mvn test -Dsuite.file=testng-smoke.xml` green; `-P quality verify` clean.
- [ ] Catalog / traceability updated.
