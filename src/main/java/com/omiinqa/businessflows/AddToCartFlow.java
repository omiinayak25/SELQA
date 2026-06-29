package com.omiinqa.businessflows;

import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Facade for the «add products to cart» portion of the SauceDemo shopping
 * journey.
 *
 * <p><b>Facade rationale:</b> Adding multiple items to the cart involves
 * iterating over product names and calling {@link ProductsPage#addToCart(String)}
 * for each. Tests that use this as setup code should not care about that
 * iteration detail — they care about the resulting {@link CartPage} state.
 * Encapsulating the loop here means tests read as business scenarios
 * (add a list of products → get cart) rather than low-level Selenium steps.</p>
 *
 * <p>This flow assumes the caller is already on the {@link ProductsPage}.
 * If the test starts from the login screen, compose with {@link LoginFlow}
 * first (see {@link CheckoutFlow} for an example).</p>
 */
public final class AddToCartFlow {

    private static final Logger log = LoggerFactory.getLogger(AddToCartFlow.class);

    private AddToCartFlow() {
        // Utility class — instantiation prevented
    }

    // ----------------------------------------------------------------- flows

    /**
     * Adds each product in {@code productNames} to the cart (in list order),
     * then opens the cart.
     *
     * <p>The flow assumes the driver is currently on the
     * {@link ProductsPage}.</p>
     *
     * @param productsPage  the currently active products page
     * @param productNames  ordered list of exact product names to add
     * @return the {@link CartPage} opened after all items are added
     */
    public static CartPage addProducts(final ProductsPage productsPage,
                                       final List<String> productNames) {
        log.info("AddToCartFlow: adding {} product(s)", productNames.size());
        productNames.forEach(name -> {
            log.debug("AddToCartFlow: adding '{}'", name);
            productsPage.addToCart(name);
        });
        return productsPage.openCart();
    }

    /**
     * Convenience overload: logs in as the standard user, adds all named
     * products, and opens the cart — combining {@link LoginFlow} and the add
     * loop in one call.
     *
     * @param productNames ordered list of exact product names to add
     * @return the {@link CartPage}
     */
    public static CartPage loginAndAddProducts(final List<String> productNames) {
        log.info("AddToCartFlow: login + add {} product(s)", productNames.size());
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        return addProducts(products, productNames);
    }
}
