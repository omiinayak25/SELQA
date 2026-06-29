package com.omiinqa.pages.saucedemo;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the SauceDemo order confirmation screen
 * ({@code /checkout-complete.html}).
 *
 * <p>This is the terminal page of the purchase flow. It exposes the completion
 * header and body text as getters so tests can assert the expected confirmation
 * messages without coupling assertions to locator strings. No assertions live
 * here — the page simply surfaces state (Single Responsibility).</p>
 */
public class CheckoutCompletePage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By COMPLETE_HEADER  = By.className("complete-header");
    private static final By COMPLETE_TEXT    = By.className("complete-text");
    private static final By BACK_HOME_BTN    = By.id("back-to-products");

    // ----------------------------------------------------------------- queries

    /**
     * Returns the large confirmation header, e.g.
     * {@code "Thank you for your order!"}.
     *
     * @return confirmation header text, trimmed
     */
    public String getCompleteHeaderText() {
        return getText(COMPLETE_HEADER);
    }

    /**
     * Returns the descriptive body text shown below the header, e.g.
     * {@code "Your order has been dispatched, …"}.
     *
     * @return confirmation body text, trimmed
     */
    public String getCompleteText() {
        return getText(COMPLETE_TEXT);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks the «Back Home» button to return to the inventory.
     *
     * @return a {@link ProductsPage}
     */
    public ProductsPage backToProducts() {
        log.info("Navigating back to products from order confirmation");
        click(BACK_HOME_BTN);
        return new ProductsPage();
    }
}
