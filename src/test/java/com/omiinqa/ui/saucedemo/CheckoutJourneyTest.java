package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.businessflows.CheckoutFlow;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.CheckoutStepTwoPage;
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
 * End-to-end checkout journey tests: single-item purchase, multi-item purchase,
 * full-catalogue (max-cart) purchase, and cancel-at-each-step scenarios.
 *
 * <p><b>Design:</b> {@link CheckoutFlow} is used for happy-path journeys where
 * the test goal is to assert on the {@link CheckoutCompletePage} state.
 * Cancel scenarios drive the page objects directly to intercept at the target
 * step and verify the correct page is returned.</p>
 *
 * <p>All assertions are in this test class — no assertions exist in the flows
 * or page objects, per the OmiinQA framework contract.</p>
 */
@Epic("SauceDemo")
@Feature("Checkout Journey")
public class CheckoutJourneyTest extends BaseTest {

    private static final String FIRST = "Test";
    private static final String LAST  = "User";
    private static final String ZIP   = "10001";

    /** All six SauceDemo products (max cart scenario). */
    private static final List<String> ALL_PRODUCTS = List.of(
            "Sauce Labs Backpack",
            "Sauce Labs Bike Light",
            "Sauce Labs Bolt T-Shirt",
            "Sauce Labs Fleece Jacket",
            "Sauce Labs Onesie",
            "Test.allTheThings() T-Shirt (Red)"
    );

    // --------------------------------------------------------- happy-path tests

    /**
     * Completes a purchase with a single product and asserts the confirmation
     * header contains the expected thank-you text.
     */
    @Test(groups = {"ui", "smoke", "regression"},
            description = "Single-item purchase completes with thank-you confirmation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("The complete purchase flow for one item must land on the order confirmation page.")
    public void singleItemPurchaseCompletesSuccessfully() {
        final CheckoutCompletePage confirmation = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Backpack"), FIRST, LAST, ZIP);

        assertThat(confirmation.getCompleteHeaderText())
                .as("confirmation header")
                .containsIgnoringCase("Thank you");
    }

    /**
     * Completes a purchase with three products and asserts the confirmation
     * body text is non-blank (indicating a dispatched-order message).
     */
    @Test(groups = {"ui", "regression"},
            description = "Multi-item purchase completes and shows order dispatched message")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A purchase with multiple items must produce the order-dispatched confirmation text.")
    public void multiItemPurchaseShowsDispatchedMessage() {
        final CheckoutCompletePage confirmation = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Bike Light", "Sauce Labs Onesie", "Sauce Labs Bolt T-Shirt"),
                FIRST, LAST, ZIP);

        assertThat(confirmation.getCompleteText())
                .as("confirmation body text")
                .isNotBlank();
    }

    /**
     * Adds all six products (maximum cart), completes the purchase, and asserts
     * the confirmation page is reached with a thank-you header.
     */
    @Test(groups = {"ui", "regression"},
            description = "Full-catalogue (6-item) purchase completes successfully")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Adding all 6 products and completing checkout must produce a confirmation page.")
    public void maxCartPurchaseCompletesSuccessfully() {
        final CheckoutCompletePage confirmation = CheckoutFlow.completePurchase(
                ALL_PRODUCTS, FIRST, LAST, ZIP);

        assertThat(confirmation.getCompleteHeaderText())
                .as("confirmation header for max cart purchase")
                .containsIgnoringCase("Thank you");
    }

    /**
     * Asserts that after a successful purchase, clicking «Back to Products» returns
     * to a loaded inventory page with all 6 products visible.
     */
    @Test(groups = {"ui", "regression"},
            description = "Back to Products after purchase returns to a loaded inventory")
    @Severity(SeverityLevel.NORMAL)
    @Description("After order completion, clicking Back to Products must restore the inventory.")
    public void backToProductsAfterPurchaseRestoresInventory() {
        final CheckoutCompletePage confirmation = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Fleece Jacket"), FIRST, LAST, ZIP);

        final ProductsPage products = confirmation.backToProducts();

        assertThat(products.isLoaded())
                .as("inventory loaded after back-to-products")
                .isTrue();
        assertThat(products.getProductCount())
                .as("all 6 products visible after purchase")
                .isEqualTo(6);
    }

    /**
     * Asserts the cart badge is zero after a completed purchase (cart is cleared
     * by SauceDemo on order completion).
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart badge is zero on inventory after completing a purchase")
    @Severity(SeverityLevel.NORMAL)
    @Description("After checkout completion the cart must be empty and badge must show 0.")
    public void cartBadgeIsZeroAfterPurchaseCompletion() {
        final CheckoutCompletePage confirmation = CheckoutFlow.completePurchase(
                List.of("Sauce Labs Onesie"), FIRST, LAST, ZIP);

        final ProductsPage products = confirmation.backToProducts();

        assertThat(products.getCartBadgeCount())
                .as("cart badge after completed purchase")
                .isZero();
    }

    // --------------------------------------------------------- cancel scenarios

    /**
     * Cancels at checkout step one (info form) and asserts the cart is returned
     * with the originally added item still present.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cancel at checkout step 1 returns to cart with items")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking Cancel on the checkout info form must return to the cart with items intact.")
    public void cancelAtStepOneReturnsToCart() {
        final CartPage cart = AddToCartFlow.loginAndAddProducts(
                List.of("Sauce Labs Backpack"));
        final CheckoutStepOnePage step1 = cart.checkout();

        final CartPage returnedCart = step1.cancel();

        assertThat(returnedCart.getItemCount())
                .as("cart item count after cancel at step 1")
                .isEqualTo(1);
    }

    /**
     * Proceeds to checkout step two (overview) then cancels, asserting the
     * inventory page is returned.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cancel at checkout step 2 returns to inventory page")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking Cancel on the order overview must navigate back to the inventory.")
    public void cancelAtStepTwoReturnsToInventory() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.addToCart("Sauce Labs Bike Light");

        final CartPage cart = products.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();
        step1.enterCustomerInfo(FIRST, LAST, ZIP);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();

        final ProductsPage inventoryAfterCancel = step2.cancel();

        assertThat(inventoryAfterCancel.isLoaded())
                .as("inventory loaded after cancel at step 2")
                .isTrue();
    }

    /**
     * Asserts that cancelling at step two after adding two items results in the
     * inventory badge being preserved (the cart still contains the items).
     */
    @Test(groups = {"ui", "regression"},
            description = "Cart items are preserved when cancelling at checkout step 2")
    @Severity(SeverityLevel.NORMAL)
    @Description("Items added to the cart must still be present after cancelling at step 2.")
    public void cartItemsPreservedAfterCancelAtStepTwo() {
        final List<String> toAdd = List.of("Sauce Labs Backpack", "Sauce Labs Onesie");
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        toAdd.forEach(products::addToCart);

        final CartPage cart = products.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();
        step1.enterCustomerInfo(FIRST, LAST, ZIP);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();

        final ProductsPage inventoryAfterCancel = step2.cancel();

        assertThat(inventoryAfterCancel.getCartBadgeCount())
                .as("cart badge still shows 2 after cancel at step 2")
                .isEqualTo(2);
    }

    /**
     * Asserts the checkout URL follows each step transition:
     * cart → checkout-step-one → checkout-step-two → checkout-complete.
     */
    @Test(groups = {"ui", "regression"},
            description = "URL changes correctly through each checkout step")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each checkout step must have its own distinct URL path segment.")
    public void checkoutUrlChangesAtEachStep() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.addToCart("Sauce Labs Bolt T-Shirt");

        final CartPage cart = products.openCart();
        assertThat(cart.currentUrl()).contains("cart");

        final CheckoutStepOnePage step1 = cart.checkout();
        assertThat(step1.currentUrl()).contains("checkout-step-one");

        step1.enterCustomerInfo(FIRST, LAST, ZIP);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();
        assertThat(step2.currentUrl()).contains("checkout-step-two");

        final CheckoutCompletePage complete = step2.finish();
        assertThat(complete.currentUrl()).contains("checkout-complete");
    }
}
