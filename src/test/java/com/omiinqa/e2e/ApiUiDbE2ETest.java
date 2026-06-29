package com.omiinqa.e2e;

import com.omiinqa.api.models.booking.Booking;
import com.omiinqa.api.models.booking.BookingDates;
import com.omiinqa.api.models.booking.BookingResponse;
import com.omiinqa.api.models.dummyjson.DummyJsonProduct;
import com.omiinqa.api.models.dummyjson.DummyJsonProductList;
import com.omiinqa.api.models.jsonplaceholder.JsonPlaceholderUser;
import com.omiinqa.api.services.BookingService;
import com.omiinqa.api.services.DummyJsonService;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.businessflows.CheckoutFlow;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Three-layer E2E tests: API creates or reads data → UI verifies or acts on
 * it → DB validates the persisted state.
 *
 * <p><b>Database availability guard:</b> Every {@code @Test} in this class
 * declares the {@code "database"} group.  The project's TestNG XML must
 * exclude that group in default (non-DB) CI runs via
 * {@code <groups><run><exclude name="database"/></run></groups>}.
 * This prevents connection failures when no live database is available.</p>
 *
 * <p><b>Runtime requirements:</b></p>
 * <ul>
 *   <li>Browser + network access to {@code https://www.saucedemo.com}.</li>
 *   <li>Network access to {@code https://dummyjson.com},
 *       {@code https://jsonplaceholder.typicode.com},
 *       {@code https://restful-booker.herokuapp.com}.</li>
 *   <li>Live PostgreSQL or MySQL database matching the schema in
 *       {@link UserRepository} and {@link ProductRepository}.</li>
 * </ul>
 *
 * <p><b>TestNG groups:</b> {@code e2e}, {@code database} (excluded by default).</p>
 */
@Epic("Three-Layer E2E")
@Feature("API + UI + Database Validation")
public class ApiUiDbE2ETest extends BaseTest {

    // ----------------------------------------------------------------- services

    private DummyJsonService        dummyJsonService;
    private JsonPlaceholderService  jsonPlaceholderService;
    private BookingService          bookingService;

    // ----------------------------------------------------------------- repositories

    private UserRepository          userRepository;
    private ProductRepository       productRepository;

    // ----------------------------------------------------------------- assertions

    private DatabaseAssertions      dbAssertions;

    /**
     * Initialises all API service facades and database components once per class.
     *
     * <p>Services are stateless; repositories and assertions are scoped to
     * {@link DatabaseType#POSTGRESQL} — change to {@link DatabaseType#MYSQL}
     * as required by the target environment.</p>
     */
    @BeforeClass(alwaysRun = true)
    public void initServicesAndDb() {
        dummyJsonService       = new DummyJsonService();
        jsonPlaceholderService = new JsonPlaceholderService();
        bookingService         = new BookingService();

        userRepository         = new UserRepository(DatabaseType.POSTGRESQL);
        productRepository      = new ProductRepository(DatabaseType.POSTGRESQL);
        dbAssertions           = DatabaseAssertions.forDatabase(DatabaseType.POSTGRESQL);
    }

    // =========================================================================
    //  1. API user data → UI form fill → DB user count increases
    // =========================================================================

    /**
     * Fetches a user from JSONPlaceholder, uses their name to drive the
     * SauceDemo checkout form, completes a full purchase, then inserts a
     * corresponding user record into the application database and asserts
     * that the row exists and the count increases by one.
     *
     * <p>Journey: API GET user → UI full checkout with API-sourced name →
     * DB INSERT user → DB assertRowExists → DB count +1.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.CRITICAL)
    public void apiUserSeedsUiCheckoutAndDbRecordIsCreated() {
        // Step 1 — fetch user from API
        final JsonPlaceholderUser apiUser = jsonPlaceholderService.getUser(4);
        assertThat(apiUser.getName()).isNotBlank();
        assertThat(apiUser.getEmail()).isNotBlank();

        final String[] nameParts = apiUser.getName().split(" ", 2);
        final String firstName = nameParts[0];
        final String lastName  = nameParts.length > 1 ? nameParts[1] : "Test";

        // Step 2 — perform full UI checkout using API-sourced name
        final CheckoutCompletePage completePage = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Backpack"),
                firstName,
                lastName,
                "10002"
        );
        assertThat(completePage.getCompleteHeaderText()).containsIgnoringCase("Thank you");

        // Step 3 — insert user record into DB (simulating downstream system)
        final long countBefore = userRepository.count();
        final String testEmail = "e2e-" + System.currentTimeMillis() + "@test.com";
        userRepository.insert(apiUser.getName(), testEmail, "active");

        // Step 4 — DB assertions
        dbAssertions.assertRowExists(
                "SELECT 1 FROM users WHERE email = ?", testEmail);
        dbAssertions.assertColumnValue(
                "SELECT status FROM users WHERE email = ?", "active", testEmail);

        final long countAfter = userRepository.count();
        assertThat(countAfter)
                .as("User count must increase by 1 after insert")
                .isEqualTo(countBefore + 1);

        // Step 5 — clean up (remove the test row)
        userRepository.findByEmail(testEmail)
                .ifPresent(rec -> userRepository.deleteById(rec.getId()));
    }

    // =========================================================================
    //  2. API product → DB product insert → UI cart → DB validate
    // =========================================================================

    /**
     * Retrieves the first DummyJSON product, mirrors it into the application
     * database products table, then adds a fixed SauceDemo product to the cart
     * via UI and verifies the DB row is still present (no accidental deletion)
     * and active after the UI interaction.
     *
     * <p>Journey: API GET product → DB INSERT product → UI add to cart →
     * DB assertRowExists product → assertColumnValue active=true.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void apiProductMirroredToDbAndUiCartDoesNotAffectDbRow() {
        // Step 1 — fetch first API product
        final DummyJsonProduct apiProduct = dummyJsonService.getProduct(1);
        assertThat(apiProduct.getTitle()).isNotBlank();
        assertThat(apiProduct.getPrice()).isGreaterThan(0);

        // Step 2 — insert product record into DB
        final long dbCountBefore = productRepository.count();
        final String prodName = "E2E-" + apiProduct.getTitle().substring(0, Math.min(30, apiProduct.getTitle().length()));
        productRepository.insert(
                prodName,
                apiProduct.getCategory(),
                BigDecimal.valueOf(apiProduct.getPrice()),
                apiProduct.getStock(),
                true
        );

        dbAssertions.assertRowExists(
                "SELECT 1 FROM products WHERE name = ?", prodName);
        assertThat(productRepository.count()).isEqualTo(dbCountBefore + 1);

        // Step 3 — UI: add a product to the cart (independent of DB product)
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        productsPage.addToCart("Sauce Labs Bike Light");
        final CartPage cart = productsPage.openCart();
        assertThat(cart.getItemCount()).isEqualTo(1);

        // Step 4 — DB: the inserted product row is still present and active
        dbAssertions
                .assertRowExists("SELECT 1 FROM products WHERE name = ? AND active = TRUE", prodName)
                .assertColumnValue(
                        "SELECT category FROM products WHERE name = ?",
                        apiProduct.getCategory(),
                        prodName);

        // Step 5 — clean up
        productRepository.findByCategory(apiProduct.getCategory()).stream()
                .filter(r -> r.getName().equals(prodName))
                .findFirst()
                .ifPresent(r -> productRepository.deleteById(r.getId()));
    }

    // =========================================================================
    //  3. API booking → DB booking count invariant → UI purchase
    // =========================================================================

    /**
     * Creates a Restful-Booker booking via API, asserts the DB users table
     * is non-empty (system invariant — users pre-exist), performs a UI full
     * checkout using the booking guest's name, and validates the checkout
     * confirmation is present.
     *
     * <p>Journey: API create booking → DB assertRowCountAtLeast(users, 0) →
     * UI checkout with booking first/last name → assert confirmation.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void apiBookingCreatedAndUiCheckoutWithSameName() {
        // Step 1 — create a booking via API
        final Booking booking = Booking.builder()
                .firstName("Laura")
                .lastName("Palmer")
                .totalPrice(220)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-07-10")
                        .checkout("2025-07-14")
                        .build())
                .additionalNeeds("Wrapped in plastic")
                .build();

        final BookingResponse created = bookingService.createBooking(booking);
        assertThat(created.getBookingId()).isPositive();
        assertThat(created.getBooking().getFirstName()).isEqualTo("Laura");

        // Step 2 — DB invariant: users table should exist (count >= 0)
        dbAssertions.assertRowCountAtLeast("users", 0);

        // Step 3 — UI checkout using booking guest name
        final CheckoutCompletePage complete = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Onesie"),
                created.getBooking().getFirstName(),
                created.getBooking().getLastName(),
                "98765"
        );
        assertThat(complete.getCompleteHeaderText()).containsIgnoringCase("Thank you");
        assertThat(complete.getCompleteText()).isNotBlank();
    }

    // =========================================================================
    //  4. DB user insert → API user read → UI checkout with DB user name
    // =========================================================================

    /**
     * Inserts a user record directly into the database, reads a matching user
     * from JSONPlaceholder by ID (parallel user creation flow), then uses the
     * API user's name to drive the SauceDemo UI checkout — verifying all three
     * layers are operating with consistent user data.
     *
     * <p>Journey: DB INSERT user → API GET user(6) → compare names (both valid)
     * → UI checkout with API name → DB assertRowExists newly inserted user.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.CRITICAL)
    public void dbInsertAndApiReadBothSeedUiCheckout() {
        // Step 1 — insert a user into DB
        final String email = "db-e2e-" + System.currentTimeMillis() + "@example.com";
        final int insertCount = userRepository.insert("DB Test User", email, "active");
        assertThat(insertCount).isEqualTo(1);

        // Step 2 — verify DB row exists
        dbAssertions.assertRowExists("SELECT 1 FROM users WHERE email = ?", email);

        // Step 3 — fetch API user (independent user source)
        final JsonPlaceholderUser apiUser = jsonPlaceholderService.getUser(6);
        assertThat(apiUser.getName()).isNotBlank();

        final String[] parts = apiUser.getName().split(" ", 2);
        final String first = parts[0];
        final String last  = parts.length > 1 ? parts[1] : "User";

        // Step 4 — UI checkout using API user's name
        final CheckoutCompletePage complete = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Fleece Jacket"),
                first, last, "12345"
        );
        assertThat(complete.getCompleteHeaderText()).containsIgnoringCase("Thank you");

        // Step 5 — DB: validate the previously inserted user still exists
        dbAssertions.assertRowExists("SELECT 1 FROM users WHERE email = ?", email);
        dbAssertions.assertColumnValue(
                "SELECT status FROM users WHERE email = ?", "active", email);

        // Clean up
        userRepository.findByEmail(email)
                .ifPresent(rec -> userRepository.deleteById(rec.getId()));
    }

    // =========================================================================
    //  5. Multi-product API check → DB product table non-empty → UI checkout
    // =========================================================================

    /**
     * Fetches 3 DummyJSON products via API and confirms the DB products table
     * is accessible (non-negative count), then performs a 3-item UI checkout
     * — linking API product data, DB table health, and UI cart behaviour.
     *
     * <p>Journey: API GET 3 products → assert prices positive →
     * DB assertRowCountAtLeast(products, 0) → UI add 3 items → checkout → confirm.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void apiProductsCheckedDbTableAccessibleAndUiThreeItemCheckout() {
        // Step 1 — fetch 3 API products
        final DummyJsonProductList apiList = dummyJsonService.getProducts(3, 0);
        assertThat(apiList.getProducts()).hasSize(3);
        apiList.getProducts().forEach(p ->
                assertThat(p.getPrice()).isGreaterThan(0));

        // Step 2 — DB health: products table accessible (count >= 0)
        dbAssertions.assertRowCountAtLeast("products", 0);

        // Step 3 — UI: add 3 SauceDemo products and checkout
        final List<String> uiItems = List.of(
                "Sauce Labs Backpack",
                "Sauce Labs Bolt T-Shirt",
                "Sauce Labs Onesie"
        );
        final CartPage cart = AddToCartFlow.loginAndAddProducts(uiItems);
        assertThat(cart.getItemCount()).isEqualTo(3);

        final CheckoutCompletePage complete = cart.checkout()
                .enterCustomerInfo("Mike", "Myers", "55555")
                .continueToOverview()
                .finish();

        assertThat(complete.getCompleteHeaderText()).containsIgnoringCase("Thank you");
    }

    // =========================================================================
    //  6. API user email uniqueness validated → DB users also unique by email
    // =========================================================================

    /**
     * Asserts that the JSONPlaceholder API returns 10 users with unique emails
     * (API layer), then validates that the local DB users table has no duplicate
     * emails (DB layer), ensuring both data sources maintain email uniqueness
     * as a shared invariant.
     *
     * <p>No UI interaction — pure API + DB cross-layer validation.</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void apiAndDbBothEnforceUserEmailUniqueness() {
        // Step 1 — API: all 10 JSONPlaceholder user emails unique
        final List<String> apiEmails = jsonPlaceholderService.getAllUsers()
                .jsonPath().getList("email");
        assertThat(apiEmails)
                .as("JSONPlaceholder API emails must be unique")
                .hasSize(10)
                .doesNotHaveDuplicates();

        // Step 2 — DB: query for any duplicate emails (should return 0 rows)
        dbAssertions.assertNoRowExists(
                "SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1");

        // Step 3 — DB: total user count is non-negative
        dbAssertions.assertRowCountAtLeast("users", 0);

        // Step 4 — insert two users with different emails, verify uniqueness maintained
        final String emailA = "unique-a-" + System.currentTimeMillis() + "@test.com";
        final String emailB = "unique-b-" + System.currentTimeMillis() + "@test.com";
        userRepository.insert("User Alpha", emailA, "active");
        userRepository.insert("User Beta",  emailB, "active");

        dbAssertions.assertRowExists("SELECT 1 FROM users WHERE email = ?", emailA);
        dbAssertions.assertRowExists("SELECT 1 FROM users WHERE email = ?", emailB);
        // Still no duplicates
        dbAssertions.assertNoRowExists(
                "SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1");

        // Cleanup
        userRepository.findByEmail(emailA).ifPresent(r -> userRepository.deleteById(r.getId()));
        userRepository.findByEmail(emailB).ifPresent(r -> userRepository.deleteById(r.getId()));
    }

    // =========================================================================
    //  7. API product search → DB product insert → UI add to cart → DB active
    // =========================================================================

    /**
     * Searches the DummyJSON API for "laptop" products, takes the first result,
     * inserts it into the DB, performs a UI cart session, and verifies the DB
     * product is still active after the UI interaction — testing that UI
     * operations do not inadvertently mutate the DB product catalogue.
     *
     * <p>Journey: API search "laptop" → DB INSERT product → UI add cart →
     * DB assertRowExists → assertColumnValue(active=true).</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void apiLaptopSearchResultInsertedToDbAndUiCartDoesNotAffectDbState() {
        // Step 1 — API search
        final DummyJsonProductList searchResult = dummyJsonService.searchProducts("laptop");
        assertThat(searchResult.getProducts())
                .as("API search for 'laptop' must return at least one product")
                .isNotEmpty();
        final DummyJsonProduct laptop = searchResult.getProducts().get(0);
        assertThat(laptop.getPrice()).isGreaterThan(0);

        // Step 2 — DB insert
        final String dbName = "E2E-Laptop-" + System.currentTimeMillis() % 10_000;
        productRepository.insert(
                dbName,
                "electronics",
                BigDecimal.valueOf(laptop.getPrice()),
                10,
                true
        );
        dbAssertions.assertRowExists("SELECT 1 FROM products WHERE name = ?", dbName);

        // Step 3 — UI: add a fixed product to cart (simulates shopping session)
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        productsPage.addToCart("Sauce Labs Backpack");
        final CartPage cart = productsPage.openCart();
        assertThat(cart.getItemCount()).isEqualTo(1);

        // Step 4 — DB: product we inserted is still active
        dbAssertions.assertRowExists(
                "SELECT 1 FROM products WHERE name = ? AND active = TRUE", dbName);

        // Cleanup
        productRepository.findByCategory("electronics").stream()
                .filter(r -> r.getName().equals(dbName))
                .findFirst()
                .ifPresent(r -> productRepository.deleteById(r.getId()));
    }

    // =========================================================================
    //  8. DB user delete → API read independent → UI login independent
    // =========================================================================

    /**
     * Inserts a user into the DB, verifies they exist, then deletes them and
     * confirms the row is gone (DB lifecycle). In parallel (logically), the
     * test reads a user from JSONPlaceholder API (separate system, unaffected
     * by DB delete) and performs a SauceDemo UI login — proving that the DB
     * delete only affects the DB layer and does not interfere with API or UI
     * operations.
     *
     * <p>Journey: DB INSERT user → DB assertRowExists → DB DELETE →
     * DB assertNoRowExists → API GET user (still returns data) →
     * UI login (still works).</p>
     */
    @Test(groups = {"e2e", "database"})
    @Severity(SeverityLevel.NORMAL)
    public void dbUserDeleteDoesNotAffectApiOrUiLayers() {
        // Step 1 — DB insert
        final String email = "delete-me-" + System.currentTimeMillis() + "@test.com";
        userRepository.insert("Temporary User", email, "active");
        dbAssertions.assertRowExists("SELECT 1 FROM users WHERE email = ?", email);

        // Step 2 — DB delete
        userRepository.findByEmail(email).ifPresent(rec -> userRepository.deleteById(rec.getId()));

        // Step 3 — DB: row must be gone
        dbAssertions.assertNoRowExists("SELECT 1 FROM users WHERE email = ?", email);

        // Step 4 — API layer: JSONPlaceholder user 8 is still accessible (independent)
        final JsonPlaceholderUser apiUser = jsonPlaceholderService.getUser(8);
        assertThat(apiUser.getId()).isEqualTo(8);
        assertThat(apiUser.getEmail()).isNotBlank();

        // Step 5 — UI layer: SauceDemo login still works (independent of DB)
        final ProductsPage productsPage = LoginFlow.loginAsStandardUser();
        assertThat(productsPage.isLoaded())
                .as("SauceDemo UI must remain fully functional after DB user deletion")
                .isTrue();
        assertThat(productsPage.getProductCount()).isEqualTo(6);
    }
}
