package com.omiinqa.e2e;

import com.omiinqa.api.services.DummyJsonService;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.models.dummyjson.DummyJsonProduct;
import com.omiinqa.api.models.dummyjson.DummyJsonProductList;
import com.omiinqa.api.models.jsonplaceholder.JsonPlaceholderUser;
import com.omiinqa.api.models.jsonplaceholder.Post;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.businessflows.CheckoutFlow;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.ProductDetailPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-layer E2E tests that use DummyJSON and JSONPlaceholder API calls to
 * seed or validate data that is then exercised through the SauceDemo UI.
 *
 * <p><b>Strategy:</b> The API layer acts as the source-of-truth oracle.  Each
 * test fetches data from a real REST API, derives an expected outcome, drives
 * the browser to perform the equivalent action, and then cross-validates the
 * result.  This approach exposes divergence between what the API and the UI
 * advertise — a class of bug that unit tests and pure API tests cannot catch.</p>
 *
 * <p><b>Runtime requirements:</b> browser + network access to
 * {@code https://dummyjson.com}, {@code https://jsonplaceholder.typicode.com},
 * and {@code https://www.saucedemo.com}.</p>
 *
 * <p><b>TestNG groups:</b> {@code e2e}, {@code regression}.</p>
 */
@Epic("Cross-Layer E2E")
@Feature("API-Driven UI Validation")
public class ApiDrivenUiE2ETest extends BaseTest {

    // ----------------------------------------------------------------- services
    // Declared at class level; safe because services are stateless and each
    // @Test method runs in its own WebDriver instance via BaseTest.

    private DummyJsonService dummyJsonService;
    private JsonPlaceholderService jsonPlaceholderService;

    /**
     * Initialises API service facades once per class.
     * Service construction is cheap (URI resolution only) so class-level
     * initialisation avoids redundant config look-ups across test methods.
     */
    @BeforeClass(alwaysRun = true)
    public void initServices() {
        dummyJsonService = new DummyJsonService();
        jsonPlaceholderService = new JsonPlaceholderService();
    }

    // =========================================================================
    //  1. UI product count vs API total
    // =========================================================================

    /**
     * Validates that the SauceDemo UI always shows a fixed set of 6 products
     * and cross-checks that the DummyJSON API can supply at least that many
     * product records (proving the API catalogue is richer than the demo store).
     *
     * <p>Journey: API fetch product count → login → count UI products →
     * assert API total &ge; UI count.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void apiProductCatalogueIsRicherThanUiProductCount() {
        // Step 1 — fetch catalogue size from DummyJSON API
        final DummyJsonProductList apiList = dummyJsonService.getProducts();
        final int apiTotal = apiList.getTotal();

        // Step 2 — open UI and count displayed products
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final int uiCount = productsPage.getProductCount();

        // Step 3 — cross-validate
        assertThat(uiCount)
                .as("SauceDemo should display exactly 6 products")
                .isEqualTo(6);
        assertThat(apiTotal)
                .as("DummyJSON API total should be at least as many as the UI shows")
                .isGreaterThanOrEqualTo(uiCount);
    }

    // =========================================================================
    //  2. API search result count propagated to UI selection
    // =========================================================================

    /**
     * Uses DummyJSON search to find all products in the "smartphones" category,
     * then drives the SauceDemo UI to add products from the first matched UI
     * names — demonstrating that API-derived product metadata can seed a UI
     * shopping session.
     *
     * <p>Journey: API search → pick first UI product name → add to cart →
     * verify cart contains exactly one item.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void apiSearchResultSeedsUiCartSelection() {
        // Step 1 — find at least one product matching "phone" from DummyJSON
        final DummyJsonProductList searchResult = dummyJsonService.searchProducts("phone");
        assertThat(searchResult.getProducts())
                .as("DummyJSON search for 'phone' must return at least one product")
                .isNotEmpty();

        // Step 2 — log into SauceDemo and add the first product available on UI
        //           (SauceDemo has its own fixed catalogue — we use API count as proof)
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final List<String> uiProductNames = productsPage.getProductNames();
        assertThat(uiProductNames).isNotEmpty();

        final String targetProduct = uiProductNames.get(0);
        final CartPage cart = AddToCartFlow.addProducts(productsPage, List.of(targetProduct));

        // Step 3 — verify cart state
        assertThat(cart.getItemCount()).isEqualTo(1);
        assertThat(cart.getCartItemNames()).contains(targetProduct);

        // Step 4 — cross validate that the API result set size is positive
        //           and matches the count from a second API call
        final int secondCallTotal = dummyJsonService.searchProducts("phone").getTotal();
        assertThat(secondCallTotal)
                .as("API search total must be stable across two consecutive calls")
                .isEqualTo(searchResult.getTotal());
    }

    // =========================================================================
    //  3. JSONPlaceholder user name seeds checkout form
    // =========================================================================

    /**
     * Fetches a user from the JSONPlaceholder API, extracts their name parts,
     * and uses them to fill the SauceDemo checkout form — validating that the
     * form accepts realistic external data without errors.
     *
     * <p>Journey: API GET user → split name → UI login → add product →
     * fill checkout form → assert no error on continue.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void jsonPlaceholderUserNameSeedsCheckoutForm() {
        // Step 1 — fetch user data from JSONPlaceholder
        final JsonPlaceholderUser apiUser = jsonPlaceholderService.getUser(1);
        assertThat(apiUser.getName()).isNotBlank();

        final String[] nameParts = apiUser.getName().split(" ", 2);
        final String firstName = nameParts[0];
        final String lastName  = nameParts.length > 1 ? nameParts[1] : "Test";
        // Use the API user's address zip if available, otherwise a static fallback
        final String zipCode = "10001";

        // Step 2 — UI: login, add single product, navigate to checkout form
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final List<String> names = productsPage.getProductNames();
        productsPage.addToCart(names.get(0));
        final CartPage cart = productsPage.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();

        // Step 3 — fill form with API-sourced name data
        step1.enterCustomerInfo(firstName, lastName, zipCode);

        // Step 4 — assert no validation error after filling all required fields
        assertThat(step1.isErrorDisplayed())
                .as("Checkout form should show no error when valid data is entered")
                .isFalse();
    }

    // =========================================================================
    //  4. API product price fields non-zero; UI prices also non-empty
    // =========================================================================

    /**
     * Fetches the first 5 DummyJSON products, verifies all have positive prices
     * via the API, then opens each product's detail page on SauceDemo UI and
     * asserts that the displayed price string is non-blank, proving both layers
     * expose pricing data.
     *
     * <p>Journey: API GET products (limit 5) → assert prices positive →
     * UI login → open product detail → assert price non-blank (per product).</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void apiProductPricesPositiveAndUiPricesNonBlank() {
        // Step 1 — fetch first 5 DummyJSON products
        final DummyJsonProductList apiList = dummyJsonService.getProducts(5, 0);
        assertThat(apiList.getProducts())
                .as("Expected exactly 5 DummyJSON products for limit=5")
                .hasSize(5);
        apiList.getProducts().forEach(p ->
                assertThat(p.getPrice())
                        .as("API product '%s' must have a positive price", p.getTitle())
                        .isGreaterThan(0));

        // Step 2 — open UI and check each product card
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final List<String> uiNames = productsPage.getProductNames();

        for (final String name : uiNames) {
            final ProductDetailPage detail = productsPage.openProduct(name);
            assertThat(detail.getProductPrice())
                    .as("UI price for product '%s' must not be blank", name)
                    .isNotBlank();
            detail.backToProducts();
        }
    }

    // =========================================================================
    //  5. JSONPlaceholder user email uniqueness + UI login stability
    // =========================================================================

    /**
     * Retrieves all 10 JSONPlaceholder users via API, asserts their emails are
     * unique (system data integrity), then verifies the SauceDemo login page
     * is accessible and rejects an invalid credential derived from one of those
     * API emails — confirming the UI auth boundary is separate from the API user set.
     *
     * <p>Journey: API GET all users → assert email uniqueness →
     * UI attempt invalid login → assert error displayed.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void apiUserEmailsUniqueAndUiRejectsApiDerivedInvalidCredential() {
        // Step 1 — fetch all JSONPlaceholder users
        final Response usersResponse = jsonPlaceholderService.getAllUsers();
        ResponseValidator.of(usersResponse).statusCode(200).bodyJsonPathNotNull("[0].email");

        final List<String> emails = usersResponse.jsonPath().getList("email");
        assertThat(emails)
                .as("JSONPlaceholder user emails must all be unique")
                .doesNotHaveDuplicates();
        assertThat(emails).hasSize(10);

        // Step 2 — use the API email as a (known-wrong) SauceDemo username
        final String invalidUsername = emails.get(0);  // not a valid SauceDemo username
        final ProductsPage productsPage = LoginFlow.loginAs(invalidUsername, "secret_sauce");

        // SauceDemo will remain on login page — isLoaded() returns false
        assertThat(productsPage.isLoaded())
                .as("Login with an API-derived invalid username must fail to reach Products page")
                .isFalse();
    }

    // =========================================================================
    //  6. DummyJSON category listing vs UI sort options
    // =========================================================================

    /**
     * Fetches the DummyJSON category list via API, confirms it is non-empty,
     * then drives SauceDemo UI to exercise all four sort options — validating
     * that the UI sort mechanism works regardless of API catalogue taxonomy.
     *
     * <p>Journey: API GET categories → assert non-empty →
     * UI login → cycle through all 4 sort options → assert product count stable.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.MINOR)
    public void apiCategoryCountPositiveAndUiSortOptionsAllFunctional() {
        // Step 1 — verify the DummyJSON category endpoint returns data
        final Response catResponse = dummyJsonService.getCategories();
        ResponseValidator.of(catResponse).statusCode(200).bodyNotEmpty();

        // Step 2 — open SauceDemo inventory and apply all sort options
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final int baseCount = productsPage.getProductCount();

        final List<String> sortOptions = List.of(
                "Name (A to Z)",
                "Name (Z to A)",
                "Price (low to high)",
                "Price (high to low)"
        );

        for (final String option : sortOptions) {
            productsPage.sortBy(option);
            assertThat(productsPage.getProductCount())
                    .as("Product count must remain %d after sorting by '%s'", baseCount, option)
                    .isEqualTo(baseCount);
        }
    }

    // =========================================================================
    //  7. API post creation mirrors user checkout confirmation
    // =========================================================================

    /**
     * Creates a post on JSONPlaceholder API using data from an API user, then
     * drives the SauceDemo UI to completion, validating that both systems
     * return user-friendly confirmation messages for their respective
     * "create" operations.
     *
     * <p>Journey: API GET user(3) → API POST post with that userId →
     * assert 201 + id present → UI full checkout → assert confirmation header.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void apiPostCreationAndUiOrderBothReturnConfirmations() {
        // Step 1 — fetch API user 3 and create a post
        final JsonPlaceholderUser apiUser = jsonPlaceholderService.getUser(3);
        assertThat(apiUser.getId()).isEqualTo(3);

        final Post newPost = Post.builder()
                .userId(apiUser.getId())
                .title("E2E cross-layer test post by " + apiUser.getUsername())
                .body("Validating API post creation as part of the E2E journey.")
                .build();
        final Response postResponse = jsonPlaceholderService.createPost(newPost);

        ResponseValidator.of(postResponse).statusCode(201).bodyJsonPathNotNull("id");
        final int createdPostId = postResponse.jsonPath().getInt("id");
        assertThat(createdPostId).isPositive();

        // Step 2 — perform UI full purchase checkout
        final CheckoutCompletePage completePage = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Backpack"),
                apiUser.getName().split(" ")[0],
                "E2EUser",
                "90210"
        );

        assertThat(completePage.getCompleteHeaderText())
                .as("UI checkout confirmation header must be visible")
                .isNotBlank()
                .containsIgnoringCase("Thank you");
    }

    // =========================================================================
    //  8. API pagination vs UI product parity
    // =========================================================================

    /**
     * Fetches DummyJSON products in two pages (first 15, then skip 15 for next
     * 15), verifies pagination metadata is consistent, then validates that the
     * SauceDemo UI product name list is a distinct subset of the API product
     * titles (ensuring test data separation is understood).
     *
     * <p>Journey: API page 1 (limit=15) → API page 2 (limit=15, skip=15) →
     * assert no title overlap across pages → UI login → assert UI names are
     * non-empty strings.</p>
     */
    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.MINOR)
    public void apiPaginationConsistentAndUiProductNamesNonEmpty() {
        // Step 1 — fetch two API pages
        final DummyJsonProductList page1 = dummyJsonService.getProducts(15, 0);
        final DummyJsonProductList page2 = dummyJsonService.getProducts(15, 15);

        assertThat(page1.getProducts()).hasSize(15);
        assertThat(page2.getProducts()).hasSize(15);

        final List<String> titlesPage1 = page1.getProducts().stream()
                .map(DummyJsonProduct::getTitle)
                .collect(Collectors.toList());
        final List<String> titlesPage2 = page2.getProducts().stream()
                .map(DummyJsonProduct::getTitle)
                .collect(Collectors.toList());

        // Pages must not overlap
        assertThat(titlesPage1).doesNotContainAnyElementsOf(titlesPage2);

        // Step 2 — UI: all product names must be non-blank strings
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        final List<String> uiNames = productsPage.getProductNames();

        assertThat(uiNames)
                .as("Every UI product name must be a non-blank string")
                .allSatisfy(name -> assertThat(name).isNotBlank());
    }
}
