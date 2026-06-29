package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Generic pagination component for any UI that renders a typical
 * «Previous / [page numbers] / Next» control.
 *
 * <p><b>Composition rationale:</b> Pagination controls appear across multiple
 * pages in enterprise applications. Encoding them in a reusable
 * {@link BaseComponent} means each page object simply constructs an instance
 * rather than duplicating locators and navigation logic. The root-scoped search
 * ensures that if a page has multiple paginator regions, each instance operates
 * on the correct one.</p>
 */
public class PaginationComponent extends BaseComponent {

    private static final By NEXT_BTN     = By.cssSelector("[aria-label='Next'], .next");
    private static final By PREV_BTN     = By.cssSelector("[aria-label='Previous'], .previous");
    private static final By ACTIVE_PAGE  = By.cssSelector(".active, [aria-current='page']");
    private static final By PAGE_NUMBERS = By.cssSelector("li.page-item:not(.disabled)");

    /**
     * @param root the element wrapping the pagination control
     */
    public PaginationComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the currently active page number as displayed, or {@code "1"} when
     *         the active indicator is absent
     */
    public String getCurrentPage() {
        if (existsInRoot(ACTIVE_PAGE)) {
            return findInRoot(ACTIVE_PAGE).getText().trim();
        }
        return "1";
    }

    /**
     * @return {@code true} when the «Next» button is present and not disabled
     */
    public boolean hasNextPage() {
        if (!existsInRoot(NEXT_BTN)) {
            return false;
        }
        final WebElement next = findInRoot(NEXT_BTN);
        final String cls = next.getAttribute("class");
        return cls == null || !cls.contains("disabled");
    }

    /**
     * @return {@code true} when the «Previous» button is present and not disabled
     */
    public boolean hasPreviousPage() {
        if (!existsInRoot(PREV_BTN)) {
            return false;
        }
        final WebElement prev = findInRoot(PREV_BTN);
        final String cls = prev.getAttribute("class");
        return cls == null || !cls.contains("disabled");
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks the «Next» button to advance to the next page.
     *
     * @throws IllegalStateException when there is no next page
     */
    public void goToNextPage() {
        if (!hasNextPage()) {
            throw new IllegalStateException("No next page available");
        }
        log.debug("Paginating: next page");
        findInRoot(NEXT_BTN).click();
    }

    /**
     * Clicks the «Previous» button to return to the previous page.
     *
     * @throws IllegalStateException when there is no previous page
     */
    public void goToPreviousPage() {
        if (!hasPreviousPage()) {
            throw new IllegalStateException("No previous page available");
        }
        log.debug("Paginating: previous page");
        findInRoot(PREV_BTN).click();
    }

    /**
     * Clicks the page-number link matching {@code pageNumber}.
     *
     * @param pageNumber 1-based target page number
     * @throws org.openqa.selenium.NoSuchElementException when that page number
     *                                                    is not rendered
     */
    public void goToPage(final int pageNumber) {
        log.debug("Paginating: jumping to page {}", pageNumber);
        findAllInRoot(PAGE_NUMBERS).stream()
                .filter(el -> el.getText().trim().equals(String.valueOf(pageNumber)))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Page number not found: " + pageNumber))
                .click();
    }
}
