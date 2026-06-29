package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * SauceDemo top-of-page header component.
 *
 * <p><b>Why a component, not inline page code?</b> The header (burger menu,
 * cart icon, badge) appears on every authenticated SauceDemo page. Isolating it
 * in a {@link BaseComponent} avoids copy-paste across ProductsPage, CartPage,
 * and Checkout pages — a textbook application of the <em>Composition over
 * Inheritance</em> principle. Pages instantiate HeaderComponent with the header
 * root element; all locators resolve relative to that root, so they cannot
 * accidentally match sibling DOM nodes.</p>
 *
 * <p>HeaderComponent is constructed once by each page that hosts it and stored
 * as a final field — it is not re-created on every method call.</p>
 */
public class HeaderComponent extends BaseComponent {

    // Locators relative to the header root element
    private static final By BURGER_MENU_BTN   = By.id("react-burger-menu-btn");
    private static final By BURGER_MENU_CLOSE = By.id("react-burger-cross-btn");
    private static final By CART_ICON         = By.className("shopping_cart_link");
    private static final By CART_BADGE        = By.className("shopping_cart_badge");
    private static final By MENU_ITEM_LOGOUT  = By.id("logout_sidebar_link");
    private static final By MENU_ITEM_ABOUT   = By.id("about_sidebar_link");
    private static final By MENU_ITEM_RESET   = By.id("reset_sidebar_link");
    private static final By MENU_ITEM_ALL_ITEMS = By.id("inventory_sidebar_link");

    /**
     * @param root the {@code <div class="primary_header">} element; provided by
     *             the hosting page so this component is always scoped to that DOM
     *             subtree.
     */
    public HeaderComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the numeric cart badge count, or {@code 0} when the badge is not
     * present (empty cart).
     *
     * @return item count shown on the cart icon badge
     */
    public int getCartBadgeCount() {
        if (!existsInRoot(CART_BADGE)) {
            return 0;
        }
        final String text = findInRoot(CART_BADGE).getText().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    /**
     * @return {@code true} when the cart badge element is visible (cart has at
     *         least one item)
     */
    public boolean isCartBadgeVisible() {
        return existsInRoot(CART_BADGE);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Opens the hamburger (burger) side menu.
     */
    public void openBurgerMenu() {
        log.debug("Opening burger menu");
        findInRoot(BURGER_MENU_BTN).click();
    }

    /**
     * Closes the hamburger side menu.
     */
    public void closeBurgerMenu() {
        log.debug("Closing burger menu");
        findInRoot(BURGER_MENU_CLOSE).click();
    }

    /**
     * Clicks the cart icon in the header.
     *
     * <p>Navigation is handled by the hosting page's {@code openCart()} method
     * (which returns the next page object), so this helper is intentionally
     * package-private to avoid bypassing the page API.</p>
     */
    void clickCart() {
        log.debug("Clicking cart icon");
        findInRoot(CART_ICON).click();
    }

    /**
     * Clicks the «All Items» burger-menu link and returns to the inventory page.
     * Calling code must supply the page-navigation wrapper.
     */
    public void clickAllItems() {
        openBurgerMenu();
        findInRoot(MENU_ITEM_ALL_ITEMS).click();
    }

    /**
     * Logs out via the burger menu.
     */
    public void logout() {
        log.debug("Logging out via burger menu");
        openBurgerMenu();
        findInRoot(MENU_ITEM_LOGOUT).click();
    }

    /**
     * Triggers the «Reset App State» burger-menu action, which clears the cart
     * and any transient session data in SauceDemo.
     */
    public void resetAppState() {
        log.debug("Resetting SauceDemo app state");
        openBurgerMenu();
        findInRoot(MENU_ITEM_RESET).click();
        closeBurgerMenu();
    }
}
