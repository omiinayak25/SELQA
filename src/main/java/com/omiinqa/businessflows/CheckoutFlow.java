package com.omiinqa.businessflows;

import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.CheckoutStepTwoPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Facade for the complete SauceDemo end-to-end purchase journey.
 *
 * <p><b>Facade pattern:</b> the full checkout flow crosses six page objects
 * ({@link com.omiinqa.pages.saucedemo.LoginPage}, {@link ProductsPage},
 * {@link CartPage}, {@link CheckoutStepOnePage}, {@link CheckoutStepTwoPage},
 * {@link CheckoutCompletePage}). Expressing that six-step chain inline in every
 * end-to-end test creates massive duplication — a change to one step requires
 * editing every test. This Facade composes the individual page objects into a
 * named, parameterised journey that returns only the final page object. Tests
 * remain short, readable, and maintenance-cheap.</p>
 *
 * <p><b>No assertions:</b> the flow orchestrates navigation; the test asserts
 * on the returned {@link CheckoutCompletePage}.</p>
 */
public final class CheckoutFlow {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFlow.class);

    private CheckoutFlow() {
        // Utility class — instantiation prevented
    }

    // ----------------------------------------------------------------- flows

    /**
     * Executes the full happy-path purchase journey as the standard user:
     * <ol>
     *   <li>Login as {@code standard_user}</li>
     *   <li>Add each product in {@code productNames} to the cart</li>
     *   <li>Proceed to cart → checkout step 1 → step 2 → finish</li>
     * </ol>
     *
     * @param productNames  one or more exact product name(s) to purchase
     * @param firstName     customer first name for the info form
     * @param lastName      customer last name
     * @param zipCode       postal/zip code
     * @return the {@link CheckoutCompletePage} shown after a successful order
     */
    public static CheckoutCompletePage completePurchase(final List<String> productNames,
                                                         final String firstName,
                                                         final String lastName,
                                                         final String zipCode) {
        log.info("CheckoutFlow: starting full purchase for {} product(s)", productNames.size());

        // Step 1 — authenticate
        final ProductsPage products = LoginFlow.loginAsStandardUser();

        // Step 2 — add products
        productNames.forEach(products::addToCart);

        // Step 3 — cart → checkout form → overview → finish
        final CartPage cart = products.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();

        step1.enterCustomerInfo(firstName, lastName, zipCode);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();

        final CheckoutCompletePage confirmation = step2.finish();
        log.info("CheckoutFlow: purchase complete");
        return confirmation;
    }

    /**
     * Executes the purchase flow using a caller-supplied {@link ProductsPage}
     * (i.e. the driver is already logged in). Useful in tests that have already
     * navigated to the inventory and pre-configured sorting or filtering.
     *
     * @param products      an already-loaded {@link ProductsPage}
     * @param productNames  product names to add
     * @param firstName     customer first name
     * @param lastName      customer last name
     * @param zipCode       postal/zip code
     * @return the {@link CheckoutCompletePage}
     */
    public static CheckoutCompletePage completePurchaseFrom(final ProductsPage products,
                                                              final List<String> productNames,
                                                              final String firstName,
                                                              final String lastName,
                                                              final String zipCode) {
        log.info("CheckoutFlow: completing purchase from an active ProductsPage");

        productNames.forEach(products::addToCart);

        final CartPage cart = products.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();

        step1.enterCustomerInfo(firstName, lastName, zipCode);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();

        return step2.finish();
    }
}
