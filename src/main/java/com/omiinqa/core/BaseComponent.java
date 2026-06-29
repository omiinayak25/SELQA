package com.omiinqa.core;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * Base for reusable UI components (header, footer, table, modal, …) that occupy
 * a sub-tree of the page.
 *
 * <p><b>Composition over inheritance:</b> pages <em>contain</em> components
 * rather than extending giant page classes. A component is scoped to a root
 * {@link WebElement}, so the same Table or Pagination component works on any
 * page that embeds it — locators resolve relative to the root, avoiding
 * cross-component collisions.</p>
 */
public abstract class BaseComponent extends BasePage {

    protected final WebElement root;

    protected BaseComponent(final WebElement root) {
        this.root = root;
    }

    /** Resolve a locator within this component's sub-tree only. */
    protected WebElement findInRoot(final By locator) {
        return root.findElement(locator);
    }

    protected java.util.List<WebElement> findAllInRoot(final By locator) {
        return root.findElements(locator);
    }

    protected boolean existsInRoot(final By locator) {
        try {
            return !root.findElements(locator).isEmpty();
        } catch (final NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return root != null && root.isDisplayed();
    }
}
