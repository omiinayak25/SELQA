package com.omiinqa.pages.saucedemo;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the SauceDemo shopping cart ({@code /cart.html}).
 *
 * <p><b>POM responsibilities:</b></p>
 * <ul>
 *   <li>Exposes cart item names and count as data (no assertions).</li>
 *   <li>Fluent navigation: {@link #continueShopping()} returns
 *       {@link ProductsPage}, {@link #checkout()} returns
 *       {@link CheckoutStepOnePage}.</li>
 *   <li>Item removal is located by name so tests are position-independent.</li>
 * </ul>
 */
public class CartPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By CART_ITEMS        = By.className("cart_item");
    private static final By ITEM_NAME         = By.className("inventory_item_name");
    private static final By REMOVE_BTN        = By.cssSelector("button.cart_button");
    private static final By CONTINUE_SHOPPING = By.id("continue-shopping");
    private static final By CHECKOUT_BTN      = By.id("checkout");

    // ----------------------------------------------------------------- queries

    /**
     * @return ordered list of product names currently in the cart
     */
    public List<String> getCartItemNames() {
        return findAll(CART_ITEMS).stream()
                .map(item -> item.findElement(ITEM_NAME).getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * @return the number of items currently in the cart
     */
    public int getItemCount() {
        return findAll(CART_ITEMS).size();
    }

    // ----------------------------------------------------------------- actions

    /**
     * Removes the named item from the cart.
     *
     * @param productName the exact name of the product to remove
     * @throws org.openqa.selenium.NoSuchElementException when the product is not
     *                                                    in the cart
     */
    public void removeItem(final String productName) {
        log.info("Removing '{}' from cart", productName);
        findCartItem(productName)
                .findElement(REMOVE_BTN)
                .click();
    }

    /**
     * Clicks «Continue Shopping» to return to the inventory.
     *
     * @return a {@link ProductsPage}
     */
    public ProductsPage continueShopping() {
        log.info("Continuing shopping from cart");
        click(CONTINUE_SHOPPING);
        return new ProductsPage();
    }

    /**
     * Clicks «Checkout» to begin the checkout flow.
     *
     * @return the {@link CheckoutStepOnePage} (customer info form)
     */
    public CheckoutStepOnePage checkout() {
        log.info("Proceeding to checkout");
        click(CHECKOUT_BTN);
        return new CheckoutStepOnePage();
    }

    // --------------------------------------------------------- private helpers

    private WebElement findCartItem(final String name) {
        return findAll(CART_ITEMS).stream()
                .filter(item -> item.findElement(ITEM_NAME).getText().trim().equals(name))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Cart item not found: " + name));
    }
}
