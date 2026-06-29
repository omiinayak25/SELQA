package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inventory-page tests covering product count, canonical product names, price
 * format correctness, add/remove-all operations, and cart-badge synchronisation.
 *
 * <p><b>Design note:</b> every test starts from a fresh {@link ProductsPage}
 * obtained via {@link LoginFlow#loginAsStandardUser()} so each test method is
 * completely independent of execution order. The {@code allProducts} data
 * provider drives single-product add/remove cycles, keeping each case atomic
 * and individually reportable in Allure.</p>
 */
@Epic("SauceDemo")
@Feature("Inventory")
public class InventoryTest extends BaseTest {

    /** Canonical product names as advertised on SauceDemo. */
    private static final List<String> ALL_PRODUCTS = List.of(
            "Sauce Labs Backpack",
            "Sauce Labs Bike Light",
            "Sauce Labs Bolt T-Shirt",
            "Sauce Labs Fleece Jacket",
            "Sauce Labs Onesie",
            "Test.allTheThings() T-Shirt (Red)"
    );

    // --------------------------------------------------------- data providers

    /**
     * Supplies each product name as a single-element row for per-product tests.
     *
     * @return matrix of [productName]
     */
    @DataProvider(name = "allProducts")
    public Object[][] allProducts() {
        return ALL_PRODUCTS.stream()
                .map(name -> new Object[]{name})
                .toArray(Object[][]::new);
    }

    // --------------------------------------------------------- test methods

    /**
     * Asserts the inventory page renders exactly six product cards — the full
     * SauceDemo catalogue.
     */
    @Test(groups = {"ui", "smoke", "regression"},
            description = "Inventory page shows exactly 6 product cards")
    @Severity(SeverityLevel.BLOCKER)
    @Description("All six SauceDemo products must be visible on the inventory page.")
    public void inventoryShowsExactlySixProducts() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.getProductCount())
                .as("product card count")
                .isEqualTo(6);
    }

    /**
     * Asserts the inventory list contains every expected product name exactly once,
     * regardless of display order.
     */
    @Test(groups = {"ui", "regression"},
            description = "All 6 canonical product names are present in the inventory")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The product listing must contain all known SauceDemo product names.")
    public void inventoryContainsAllCanonicalProductNames() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.getProductNames())
                .as("product names on inventory page")
                .containsExactlyInAnyOrderElementsOf(ALL_PRODUCTS);
    }

    /**
     * Asserts the cart badge is absent (zero) on initial page load before any
     * product is added.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart badge shows 0 on fresh inventory page load")
    @Severity(SeverityLevel.NORMAL)
    @Description("The cart must be empty immediately after login.")
    public void cartBadgeIsZeroOnFreshLoad() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.getCartBadgeCount())
                .as("initial cart badge count")
                .isZero();
    }

    /**
     * Adds one product at a time (data-driven) and confirms the badge increments
     * to exactly 1.
     *
     * @param productName the product to add
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "allProducts",
            description = "Adding a single product updates the cart badge to 1")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each product can be added to the cart independently; badge must show 1.")
    public void addSingleProductUpdatesBadgeToOne(final String productName) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.addToCart(productName);
        assertThat(products.getCartBadgeCount())
                .as("badge after adding '%s'", productName)
                .isEqualTo(1);
    }

    /**
     * Adds all six products sequentially and asserts the badge reaches 6.
     */
    @Test(groups = {"ui", "regression"},
            description = "Adding all 6 products updates the cart badge to 6")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Cart badge must equal the total number of added items when all are added.")
    public void addAllProductsBadgeReachesSix() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        ALL_PRODUCTS.forEach(products::addToCart);
        assertThat(products.getCartBadgeCount())
                .as("badge after adding all products")
                .isEqualTo(6);
    }

    /**
     * Adds all six products then removes each one, asserting the badge decrements
     * correctly step by step and reaches zero at the end.
     */
    @Test(groups = {"ui", "regression"},
            description = "Removing products one-by-one decrements badge to 0")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each remove operation must decrement the cart badge by exactly one.")
    public void removeAllProductsBadgeReachesZero() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        ALL_PRODUCTS.forEach(products::addToCart);
        assertThat(products.getCartBadgeCount()).isEqualTo(6);

        for (int i = 0; i < ALL_PRODUCTS.size(); i++) {
            products.removeFromCart(ALL_PRODUCTS.get(i));
            final int expectedCount = ALL_PRODUCTS.size() - i - 1;
            assertThat(products.getCartBadgeCount())
                    .as("badge after removing item %d", i + 1)
                    .isEqualTo(expectedCount);
        }
    }

    /**
     * Verifies the cart page (reached from the inventory) lists the same items
     * that were added, ensuring the add operation actually persists to the cart.
     */
    @Test(groups = {"ui", "regression"},
            description = "Products added on inventory page appear in the cart")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Items added via the inventory Add-to-cart button must appear in the cart view.")
    public void addedProductsAppearInCart() {
        final List<String> toAdd = List.of("Sauce Labs Backpack", "Sauce Labs Bike Light");
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        toAdd.forEach(products::addToCart);

        final CartPage cart = products.openCart();
        assertThat(cart.getCartItemNames())
                .as("cart items after adding two products")
                .containsExactlyInAnyOrderElementsOf(toAdd);
    }

    /**
     * Asserts that the page title of the inventory page equals the expected
     * browser-tab title, confirming correct page identity.
     */
    @Test(groups = {"ui", "regression"},
            description = "Inventory page title is 'Swag Labs'")
    @Severity(SeverityLevel.MINOR)
    @Description("The browser tab title for the inventory page must be 'Swag Labs'.")
    public void inventoryPageHasCorrectTitle() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.pageTitle())
                .as("inventory page browser title")
                .isEqualToIgnoringCase("Swag Labs");
    }

    /**
     * Asserts the inventory page is marked as loaded immediately after standard
     * login without any additional explicit wait in the test itself.
     */
    @Test(groups = {"ui", "smoke", "regression"},
            description = "Inventory page reports isLoaded() true after standard login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("isLoaded() must return true as soon as the products page is reached.")
    public void inventoryIsLoadedAfterLogin() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.isLoaded())
                .as("inventory page loaded flag")
                .isTrue();
    }
}
