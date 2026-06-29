package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cart page tests covering item addition, removal, continuation from cart back
 * to the inventory, and cart-to-inventory badge synchronisation.
 *
 * <p><b>Design:</b> {@link AddToCartFlow} is used to reach a populated cart
 * state efficiently. From that state each test exercises a distinct cart
 * behaviour. The {@link CartPage#continueShopping()} navigation is tested to
 * confirm a round-trip (inventory → cart → inventory) leaves the inventory in
 * the correct state, including the badge count.</p>
 */
@Epic("SauceDemo")
@Feature("Cart")
public class CartTest extends BaseTest {

    // --------------------------------------------------------- test methods

    /**
     * Asserts the cart opens empty (zero items) when no product has been added
     * in the current session.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart is empty when no products have been added")
    @Severity(SeverityLevel.NORMAL)
    @Description("Navigating to the cart without adding any products must show an empty cart.")
    public void cartIsEmptyWithNoProductsAdded() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        final CartPage cart = products.openCart();

        assertThat(cart.getItemCount())
                .as("cart item count with nothing added")
                .isZero();
        assertThat(cart.getCartItemNames())
                .as("cart names should be empty")
                .isEmpty();
    }

    /**
     * Adds two products from the inventory and asserts both appear in the cart
     * with the correct names.
     */
    @Test(groups = {"ui", "regression"},
            description = "Multiple products added from inventory all appear in the cart")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Every product added via Add-to-cart must appear in the cart view.")
    public void multipleProductsAddedAllAppearInCart() {
        final List<String> toAdd = List.of("Sauce Labs Backpack", "Sauce Labs Onesie");
        final CartPage cart = AddToCartFlow.loginAndAddProducts(toAdd);

        assertThat(cart.getCartItemNames())
                .as("cart items after adding two products")
                .containsExactlyInAnyOrderElementsOf(toAdd);
        assertThat(cart.getItemCount())
                .as("cart item count")
                .isEqualTo(2);
    }

    /**
     * Adds three products, removes one from the cart view, and asserts only the
     * two remaining products are still present.
     */
    @Test(groups = {"ui", "regression"},
            description = "Removing an item from the cart reduces item count by one")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Remove from cart must decrease the item count and remove the item's name.")
    public void removingItemFromCartReducesCount() {
        final List<String> toAdd = List.of(
                "Sauce Labs Backpack",
                "Sauce Labs Bike Light",
                "Sauce Labs Bolt T-Shirt"
        );
        final CartPage cart = AddToCartFlow.loginAndAddProducts(toAdd);

        cart.removeItem("Sauce Labs Bike Light");

        assertThat(cart.getItemCount())
                .as("cart item count after removal")
                .isEqualTo(2);
        assertThat(cart.getCartItemNames())
                .as("remaining cart items")
                .containsExactlyInAnyOrder("Sauce Labs Backpack", "Sauce Labs Bolt T-Shirt");
    }

    /**
     * Adds a product, navigates to the cart, clicks «Continue Shopping», and
     * asserts the inventory page is loaded again with 6 products visible.
     */
    @Test(groups = {"ui", "regression"},
            description = "Continue Shopping returns to a loaded inventory page")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking Continue Shopping must return to the loaded inventory with all products.")
    public void continueShoppingReturnsToInventory() {
        final CartPage cart = AddToCartFlow.loginAndAddProducts(
                List.of("Sauce Labs Backpack"));

        final ProductsPage products = cart.continueShopping();

        assertThat(products.isLoaded())
                .as("inventory loaded after continuing shopping")
                .isTrue();
        assertThat(products.getProductCount())
                .as("all products still visible after returning from cart")
                .isEqualTo(6);
    }

    /**
     * Adds a product, continues shopping (returns to inventory), then opens the
     * cart again — asserts the previously added item is still in the cart,
     * confirming the session persists the cart state.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart items persist when returning to inventory via Continue Shopping")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Items in the cart must remain after navigating away and back via Continue Shopping.")
    public void cartItemsPersistAfterContinueShopping() {
        final String addedProduct = "Sauce Labs Fleece Jacket";
        final CartPage cart = AddToCartFlow.loginAndAddProducts(List.of(addedProduct));

        final ProductsPage products = cart.continueShopping();
        final CartPage cartRevisited = products.openCart();

        assertThat(cartRevisited.getCartItemNames())
                .as("item persists in cart after continue shopping round-trip")
                .contains(addedProduct);
    }

    /**
     * Asserts the cart badge on the inventory page reflects the number of items
     * added, and after removing one item from the cart and returning, the badge
     * is decremented accordingly.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart badge on inventory stays in sync with cart item count")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The cart badge count must equal the actual number of items in the cart at all times.")
    public void cartBadgeSyncedWithCartItemCount() {
        final List<String> toAdd = List.of("Sauce Labs Backpack", "Sauce Labs Bike Light");
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        toAdd.forEach(products::addToCart);

        assertThat(products.getCartBadgeCount())
                .as("badge before visiting cart")
                .isEqualTo(2);

        final CartPage cart = products.openCart();
        cart.removeItem("Sauce Labs Backpack");

        final ProductsPage inventoryAfterRemoval = cart.continueShopping();
        assertThat(inventoryAfterRemoval.getCartBadgeCount())
                .as("badge after removing one item via cart")
                .isEqualTo(1);
    }

    /**
     * Removes all items from a populated cart one by one and asserts the cart
     * is empty afterwards.
     */
    @Test(groups = {"ui", "regression"},
            description = "Removing all items one by one empties the cart")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sequentially removing every item must result in an empty cart.")
    public void removingAllItemsLeavesCartEmpty() {
        final List<String> toAdd = List.of("Sauce Labs Backpack", "Sauce Labs Onesie");
        final CartPage cart = AddToCartFlow.loginAndAddProducts(toAdd);

        cart.removeItem("Sauce Labs Backpack");
        cart.removeItem("Sauce Labs Onesie");

        assertThat(cart.getItemCount())
                .as("cart should be empty after removing all items")
                .isZero();
    }
}
