package com.omiinqa.pages.saucedemo;

import com.omiinqa.components.HeaderComponent;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the SauceDemo inventory / products listing page
 * ({@code /inventory.html}).
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>{@link HeaderComponent} is composed in (not inherited) — it is
 *       instantiated lazily via {@link #header()} so the driver is always ready
 *       when the component is accessed.</li>
 *   <li>Product-specific actions ({@link #addToCart(String)},
 *       {@link #removeFromCart(String)}) locate items by visible product name,
 *       making tests resilient to position changes caused by sorting.</li>
 *   <li>No assertions: {@link #isLoaded()} returns a boolean; tests decide
 *       what to assert about it.</li>
 * </ul>
 */
public class ProductsPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By PAGE_HEADER       = By.className("title");
    private static final By PRODUCT_ITEMS     = By.className("inventory_item");
    private static final By PRODUCT_NAMES     = By.className("inventory_item_name");
    private static final By SORT_DROPDOWN     = By.className("product_sort_container");
    private static final By HEADER_ROOT       = By.cssSelector(".primary_header");
    private static final By CART_LINK         = By.cssSelector(".shopping_cart_link");

    // Item-level locators (resolved relative to a product WebElement)
    private static final By ITEM_NAME         = By.className("inventory_item_name");
    private static final By ITEM_ADD_BTN      = By.cssSelector("button.btn_inventory");

    // ----------------------------------------------------------------- queries

    /**
     * @return {@code true} when the inventory page title ("Products") is visible,
     *         indicating a successful load
     */
    public boolean isLoaded() {
        return isDisplayed(PAGE_HEADER);
    }

    /**
     * @return the count of product cards currently rendered on the page
     */
    public int getProductCount() {
        return findAll(PRODUCT_ITEMS).size();
    }

    /**
     * @return ordered list of all visible product names on the current page
     */
    public List<String> getProductNames() {
        return findAll(PRODUCT_NAMES).stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * Returns the numeric cart badge count, delegating to {@link HeaderComponent}.
     *
     * @return items in cart; {@code 0} when cart is empty
     */
    public int getCartBadgeCount() {
        return header().getCartBadgeCount();
    }

    // ----------------------------------------------------------------- actions

    /**
     * Adds the named product to the cart by clicking its «Add to cart» button.
     *
     * <p>The product is located by name rather than index so the action is
     * sorting-order independent.</p>
     *
     * @param productName the exact product name as displayed on the card
     * @throws org.openqa.selenium.NoSuchElementException when the product is not
     *                                                    found on the current page
     */
    public void addToCart(final String productName) {
        log.info("Adding '{}' to cart", productName);
        findProductItem(productName)
                .findElement(ITEM_ADD_BTN)
                .click();
    }

    /**
     * Removes the named product from the cart by clicking its «Remove» button.
     *
     * @param productName the exact product name as displayed on the card
     */
    public void removeFromCart(final String productName) {
        log.info("Removing '{}' from cart", productName);
        findProductItem(productName)
                .findElement(ITEM_ADD_BTN)
                .click();
    }

    /**
     * Selects a sort option from the dropdown by its visible text.
     *
     * <p>Valid visible-text options on SauceDemo:
     * {@code "Name (A to Z)"}, {@code "Name (Z to A)"},
     * {@code "Price (low to high)"}, {@code "Price (high to low)"}.</p>
     *
     * @param optionText the exact visible text of the sort option
     */
    public void sortBy(final String optionText) {
        log.info("Sorting products by: '{}'", optionText);
        selectFromDropdownByVisibleText(SORT_DROPDOWN, optionText);
    }

    /**
     * Clicks a product name to open its detail page.
     *
     * @param productName the exact product name
     * @return the {@link ProductDetailPage} representing the item detail view
     */
    public ProductDetailPage openProduct(final String productName) {
        log.info("Opening product detail for '{}'", productName);
        findProductItem(productName)
                .findElement(ITEM_NAME)
                .click();
        return new ProductDetailPage();
    }

    /**
     * Clicks the cart icon to navigate to the cart.
     *
     * @return the {@link CartPage}
     */
    public CartPage openCart() {
        log.info("Opening cart");
        click(CART_LINK);
        return new CartPage();
    }

    // ----------------------------------------------------------- header access

    /**
     * Provides access to the {@link HeaderComponent} for burger-menu actions,
     * logout, reset, etc.
     *
     * @return a lazily constructed {@link HeaderComponent} bound to the header
     *         root element
     */
    public HeaderComponent header() {
        final WebElement headerRoot = WaitUtils.visible(driver(), HEADER_ROOT);
        return new HeaderComponent(headerRoot);
    }

    // --------------------------------------------------------- private helpers

    private WebElement findProductItem(final String name) {
        return findAll(PRODUCT_ITEMS).stream()
                .filter(item -> item.findElement(ITEM_NAME).getText().trim().equals(name))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Product not found: " + name));
    }
}
