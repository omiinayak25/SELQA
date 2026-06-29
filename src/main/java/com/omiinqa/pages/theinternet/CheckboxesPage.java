package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page Object for the The-Internet checkboxes exercise
 * ({@code https://the-internet.herokuapp.com/checkboxes}).
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>All checkboxes are retrieved through the single {@link #CHECKBOXES}
 *       locator; index-based helpers delegate to it so the DOM is queried once
 *       per operation.</li>
 *   <li>No assertions are included — callers use the boolean getters to build
 *       their own verification logic.</li>
 *   <li>0-based indexing is used throughout to align with Java list conventions
 *       and minimise off-by-one errors in tests.</li>
 * </ul>
 *
 * <p>Always call {@link #open()} at the start of a test to navigate to a known
 * state.</p>
 */
public class CheckboxesPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** All {@code <input type="checkbox">} elements on the page. */
    private static final By CHECKBOXES = By.cssSelector("input[type='checkbox']");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the checkboxes page URL as configured in
     * {@code FrameworkConfig.get().appUrl("theinternet")} with the
     * {@code /checkboxes} path appended.
     *
     * @return this {@link CheckboxesPage} for method chaining
     */
    public CheckboxesPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/checkboxes";
        log.info("Opening The-Internet checkboxes page: {}", url);
        driver().get(url);
        waitForUrlContains("/checkboxes");
        return this;
    }

    /**
     * Toggles (clicks) the checkbox at the specified 0-based index.
     *
     * <p>The checkbox list is re-fetched from the DOM on each call to avoid
     * stale-element issues when state changes between clicks.</p>
     *
     * @param index 0-based position of the checkbox to toggle
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public void toggle(final int index) {
        log.info("Toggling checkbox at index {}", index);
        getCheckboxes().get(index).click();
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns all checkbox {@link WebElement}s currently present on the page.
     *
     * <p>The list is ordered as they appear in the DOM (top-to-bottom).
     * Index {@code 0} maps to checkbox 1, index {@code 1} maps to checkbox 2,
     * and so on.</p>
     *
     * @return unmodifiable-safe live list of checkbox elements; never {@code null}
     */
    public List<WebElement> getCheckboxes() {
        return findAll(CHECKBOXES);
    }

    /**
     * Returns {@code true} when the checkbox at the given 0-based index is
     * currently in the checked state.
     *
     * @param index 0-based position of the checkbox to inspect
     * @return {@code true} if checked, {@code false} if unchecked
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public boolean isChecked(final int index) {
        return getCheckboxes().get(index).isSelected();
    }

    /**
     * Returns the total number of checkboxes rendered on the page.
     *
     * @return count of {@code <input type="checkbox">} elements; {@code 0} if
     *         none are present
     */
    public int getCheckboxCount() {
        return getCheckboxes().size();
    }
}
