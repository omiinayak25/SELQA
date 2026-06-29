package com.omiinqa.pages.saucedemo;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the SauceDemo single-product detail page
 * ({@code /inventory-item.html}).
 *
 * <p>This page is reached by clicking a product name or image on
 * {@link ProductsPage}. It provides read access to product details and the
 * ability to add/remove items or navigate back to the inventory.</p>
 *
 * <p><b>POM contract:</b> no assertions — callers inspect getters and assert in
 * tests or business flows.</p>
 */
public class ProductDetailPage extends BasePage {

    private static final By PRODUCT_NAME   = By.className("inventory_details_name");
    private static final By PRODUCT_DESC   = By.className("inventory_details_desc");
    private static final By PRODUCT_PRICE  = By.className("inventory_details_price");
    private static final By ADD_REMOVE_BTN = By.cssSelector("button.btn_inventory");
    private static final By BACK_BTN       = By.id("back-to-products");

    // ----------------------------------------------------------------- queries

    /**
     * @return the product name shown on the detail page
     */
    public String getProductName() {
        return getText(PRODUCT_NAME);
    }

    /**
     * @return the product description text
     */
    public String getProductDescription() {
        return getText(PRODUCT_DESC);
    }

    /**
     * @return the price string as displayed, e.g. {@code "$9.99"}
     */
    public String getProductPrice() {
        return getText(PRODUCT_PRICE);
    }

    /**
     * @return the label of the add/remove button, e.g. {@code "Add to cart"} or
     *         {@code "Remove"}
     */
    public String getAddRemoveButtonLabel() {
        return getText(ADD_REMOVE_BTN);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks the «Add to cart» button on the detail page.
     */
    public void addToCart() {
        log.info("Adding product to cart from detail page");
        click(ADD_REMOVE_BTN);
    }

    /**
     * Clicks «Remove» to remove the item from the cart.
     */
    public void removeFromCart() {
        log.info("Removing product from cart from detail page");
        click(ADD_REMOVE_BTN);
    }

    /**
     * Navigates back to the products/inventory listing.
     *
     * @return a new {@link ProductsPage}
     */
    public ProductsPage backToProducts() {
        log.info("Navigating back to products page");
        click(BACK_BTN);
        return new ProductsPage();
    }
}
