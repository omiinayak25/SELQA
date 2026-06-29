package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OrangeHRM left-hand navigation sidebar component.
 *
 * <p><b>Why a component?</b> The sidebar is present on every authenticated
 * OrangeHRM page. Encapsulating its interaction in a dedicated
 * {@link BaseComponent} lets DashboardPage (and future page objects) obtain
 * the sidebar via composition rather than re-declaring the same locators.
 * Constructing the component with the sidebar root element limits locator scope
 * so that menu items from the body cannot accidentally match.</p>
 */
public class NavigationSidebar extends BaseComponent {

    private static final By NAV_ITEMS     = By.cssSelector("ul.oxd-main-menu li");
    private static final By ACTIVE_ITEM   = By.cssSelector("li.oxd-main-menu-item--active");
    private static final By MENU_LABEL    = By.cssSelector(".oxd-main-menu-item span");
    private static final By BRAND_LOGO    = By.cssSelector(".oxd-brand-banner");

    /**
     * @param root the top-level sidebar element (e.g. {@code <nav class="oxd-sidepanel">})
     */
    public NavigationSidebar(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return list of visible sidebar menu item labels (trimmed)
     */
    public List<String> getMenuItemLabels() {
        return findAllInRoot(MENU_LABEL).stream()
                .map(el -> el.getText().trim())
                .filter(text -> !text.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @return the label of the currently active/selected menu item
     */
    public String getActiveMenuLabel() {
        return findInRoot(ACTIVE_ITEM).getText().trim();
    }

    /**
     * @return {@code true} when the sidebar brand/logo area is visible, used as
     *         a fast {@code isLoaded()} proxy
     */
    public boolean isBrandLogoVisible() {
        return existsInRoot(BRAND_LOGO);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks a menu item by its visible label text (case-sensitive).
     *
     * @param label the exact menu text, e.g. {@code "Dashboard"}, {@code "PIM"}
     * @throws org.openqa.selenium.NoSuchElementException when no item matches
     */
    public void navigateTo(final String label) {
        log.debug("Navigating sidebar to '{}'", label);
        findAllInRoot(NAV_ITEMS).stream()
                .filter(item -> item.getText().contains(label))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Sidebar menu item not found: " + label))
                .click();
    }
}
