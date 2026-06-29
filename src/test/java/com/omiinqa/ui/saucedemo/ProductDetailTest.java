package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.ProductDetailPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Product detail page tests covering field visibility, price accuracy per the
 * canonical catalogue, add-to-cart and remove-from-cart on the detail view, and
 * the back-navigation flow back to the inventory.
 *
 * <p><b>Data-driven approach:</b> the {@code productCatalogue} data provider
 * emits one row per SauceDemo product containing name and expected price.
 * Each test that needs per-product coverage receives a row, keeping individual
 * failures attributable to a specific product without obscuring others.</p>
 *
 * <p>Back-navigation is tested once for the first product to avoid repeating the
 * same check six times; the detail page's {@code backToProducts()} is the only
 * navigation path so a single representative check is sufficient.</p>
 */
@Epic("SauceDemo")
@Feature("Product Detail")
public class ProductDetailTest extends BaseTest {

    // --------------------------------------------------------- data providers

    /**
     * Supplies each product's name and expected price string (including the
     * leading {@code $}) as per the FOUNDATION_CONTRACT.
     *
     * @return matrix of [productName, expectedPrice]
     */
    @DataProvider(name = "productCatalogue")
    public Object[][] productCatalogue() {
        return new Object[][] {
                {"Sauce Labs Backpack",                  "$29.99"},
                {"Sauce Labs Bike Light",                "$9.99"},
                {"Sauce Labs Bolt T-Shirt",              "$15.99"},
                {"Sauce Labs Fleece Jacket",             "$49.99"},
                {"Sauce Labs Onesie",                    "$7.99"},
                {"Test.allTheThings() T-Shirt (Red)",    "$15.99"},
        };
    }

    // --------------------------------------------------------- test methods

    /**
     * Opens each product's detail page and asserts the displayed name matches the
     * name used to navigate to it.
     *
     * @param productName   exact product name on the inventory listing
     * @param expectedPrice expected price string (unused in this test)
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productCatalogue",
            description = "Product detail page shows the correct product name")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The product name on the detail page must match the name clicked in the inventory.")
    public void detailPageShowsCorrectProductName(final String productName,
                                                   final String expectedPrice) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct(productName);

        assertThat(detail.getProductName())
                .as("product name on detail page for '%s'", productName)
                .isEqualTo(productName);
    }

    /**
     * Opens each product's detail page and asserts the displayed price matches the
     * canonical price from the FOUNDATION_CONTRACT.
     *
     * @param productName   exact product name
     * @param expectedPrice expected price string, e.g. {@code "$29.99"}
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productCatalogue",
            description = "Product detail page shows the correct price")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The price on the detail page must equal the canonical catalogue price.")
    public void detailPageShowsCorrectPrice(final String productName,
                                             final String expectedPrice) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct(productName);

        assertThat(detail.getProductPrice())
                .as("price for '%s' on detail page", productName)
                .isEqualTo(expectedPrice);
    }

    /**
     * Asserts the description field on each product's detail page is non-blank,
     * ensuring content is always rendered regardless of product.
     *
     * @param productName   product to open
     * @param expectedPrice unused in this assertion
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productCatalogue",
            description = "Product detail page description is non-blank for every product")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each product must have a non-empty description on its detail page.")
    public void detailPageDescriptionIsNonBlank(final String productName,
                                                 final String expectedPrice) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct(productName);

        assertThat(detail.getProductDescription())
                .as("description for '%s'", productName)
                .isNotBlank();
    }

    /**
     * Adds a product from its detail page and asserts the button label changes to
     * {@code "Remove"}, confirming the cart-state transition is reflected in the UI.
     */
    @Test(groups = {"ui", "regression"},
            description = "Add to cart on detail page changes button label to Remove")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking 'Add to cart' on the detail page must change the button label to 'Remove'.")
    public void addToCartOnDetailPageChangesButtonLabel() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct("Sauce Labs Backpack");

        detail.addToCart();

        assertThat(detail.getAddRemoveButtonLabel())
                .as("button label after add")
                .containsIgnoringCase("Remove");
    }

    /**
     * Adds a product from the detail page, then removes it, asserting the button
     * label reverts to the add-to-cart label.
     */
    @Test(groups = {"ui", "regression"},
            description = "Remove on detail page reverts button label to Add to cart")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking 'Remove' on the detail page must restore the 'Add to cart' button label.")
    public void removeFromCartOnDetailPageRevertsButtonLabel() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct("Sauce Labs Backpack");

        detail.addToCart();
        detail.removeFromCart();

        assertThat(detail.getAddRemoveButtonLabel())
                .as("button label after remove")
                .containsIgnoringCase("Add to cart");
    }

    /**
     * Navigates to a product detail page and clicks back; asserts the inventory
     * page is restored (isLoaded returns true).
     */
    @Test(groups = {"ui", "regression"},
            description = "Back navigation from detail page returns to inventory")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking the Back button on the detail page must return to the loaded inventory.")
    public void backNavigationFromDetailReturnsToInventory() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct("Sauce Labs Bike Light");

        final ProductsPage inventoryAfterBack = detail.backToProducts();

        assertThat(inventoryAfterBack.isLoaded())
                .as("inventory loaded after back navigation")
                .isTrue();
        assertThat(inventoryAfterBack.getProductCount())
                .as("all products still visible after back navigation")
                .isEqualTo(6);
    }

    /**
     * Asserts the detail page URL contains the expected path segment, confirming
     * proper navigation occurred.
     */
    @Test(groups = {"ui", "regression"},
            description = "Detail page URL contains inventory-item path segment")
    @Severity(SeverityLevel.MINOR)
    @Description("The URL when viewing a product detail page must contain 'inventory-item'.")
    public void detailPageUrlContainsInventoryItemPath() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final ProductDetailPage detail = products.openProduct("Sauce Labs Onesie");

        assertThat(detail.currentUrl())
                .as("detail page URL")
                .contains("inventory-item");
    }
}
