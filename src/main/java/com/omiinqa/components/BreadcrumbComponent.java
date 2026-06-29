package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic breadcrumb navigation component.
 *
 * <p><b>Composition rationale:</b> Breadcrumbs communicate the user's current
 * location in an application hierarchy and may appear on many pages.
 * Encapsulating breadcrumb interaction in a {@link BaseComponent} provides a
 * single, testable unit that any page object can compose rather than duplicating
 * locators. The component's root-scoped locators prevent collision with
 * unrelated navigation elements elsewhere on the page.</p>
 */
public class BreadcrumbComponent extends BaseComponent {

    private static final By CRUMB_ITEMS  = By.cssSelector("li, .breadcrumb-item");
    private static final By ACTIVE_CRUMB = By.cssSelector("li.active, .breadcrumb-item.active, [aria-current='page']");
    private static final By CRUMB_LINKS  = By.cssSelector("a");

    /**
     * @param root the {@code <nav aria-label="breadcrumb">} or wrapping element
     */
    public BreadcrumbComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return ordered list of all breadcrumb labels (including the active leaf)
     */
    public List<String> getCrumbs() {
        return findAllInRoot(CRUMB_ITEMS).stream()
                .map(el -> el.getText().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @return the text of the current (active / non-linked) breadcrumb leaf
     */
    public String getActiveCrumb() {
        return findInRoot(ACTIVE_CRUMB).getText().trim();
    }

    /**
     * @return the total number of breadcrumb segments (depth)
     */
    public int getDepth() {
        return findAllInRoot(CRUMB_ITEMS).size();
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks the breadcrumb link whose label matches the supplied text exactly.
     *
     * @param label the crumb label to click (must be a linked, non-active crumb)
     * @throws org.openqa.selenium.NoSuchElementException when no matching link
     *                                                    is found
     */
    public void clickCrumb(final String label) {
        log.debug("Clicking breadcrumb: '{}'", label);
        findAllInRoot(CRUMB_LINKS).stream()
                .filter(a -> a.getText().trim().equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Breadcrumb link not found: " + label))
                .click();
    }
}
