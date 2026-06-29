package com.omiinqa.pages.saucedemo;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for SauceDemo Checkout Step 2 — order overview / summary
 * ({@code /checkout-step-two.html}).
 *
 * <p><b>POM contract:</b> all monetary values are returned as raw strings
 * (e.g. {@code "$29.99"}) so the test layer retains control over parsing and
 * tolerance checks. No arithmetic or assertions are performed here.</p>
 *
 * <p>Fluent navigation: {@link #finish()} completes the order and returns
 * {@link CheckoutCompletePage}; {@link #cancel()} aborts and returns
 * {@link ProductsPage}.</p>
 */
public class CheckoutStepTwoPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By ORDER_ITEM_NAMES  = By.className("inventory_item_name");
    private static final By SUBTOTAL_LABEL    = By.className("summary_subtotal_label");
    private static final By TAX_LABEL         = By.className("summary_tax_label");
    private static final By TOTAL_LABEL       = By.className("summary_total_label");
    private static final By FINISH_BTN        = By.id("finish");
    private static final By CANCEL_BTN        = By.id("cancel");

    // ----------------------------------------------------------------- queries

    /**
     * @return ordered list of product names in the order summary
     */
    public List<String> getItemNames() {
        return findAll(ORDER_ITEM_NAMES).stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * Returns the subtotal label text, e.g. {@code "Item total: $29.99"}.
     * Parse the numeric value in the test if precision comparison is required.
     *
     * @return subtotal label as displayed
     */
    public String getSummarySubtotal() {
        return getText(SUBTOTAL_LABEL);
    }

    /**
     * Returns the tax label text, e.g. {@code "Tax: $2.40"}.
     *
     * @return tax label as displayed
     */
    public String getTax() {
        return getText(TAX_LABEL);
    }

    /**
     * Returns the total label text, e.g. {@code "Total: $32.39"}.
     *
     * @return total label as displayed
     */
    public String getTotal() {
        return getText(TOTAL_LABEL);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks «Finish» to complete and submit the order.
     *
     * @return a {@link CheckoutCompletePage}
     */
    public CheckoutCompletePage finish() {
        log.info("Finishing order");
        click(FINISH_BTN);
        return new CheckoutCompletePage();
    }

    /**
     * Clicks «Cancel» to abort the order and return to the inventory.
     *
     * @return a {@link ProductsPage}
     */
    public ProductsPage cancel() {
        log.info("Cancelling order from overview");
        click(CANCEL_BTN);
        return new ProductsPage();
    }
}
