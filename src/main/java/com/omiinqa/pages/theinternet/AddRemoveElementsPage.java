package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page Object for the Add/Remove Elements exercise on The-Internet
 * ({@code https://the-internet.herokuapp.com/add_remove_elements}).
 *
 * <p>The page renders a single «Add Element» button. Each click appends one
 * dynamically-created {@code .added-manually} delete button to the DOM. Each
 * delete button, when clicked, removes itself. This page object exposes:
 * <ul>
 *   <li>{@link #clickAdd()} — triggers one add cycle</li>
 *   <li>{@link #clickDelete(int)} — removes the n-th delete button (1-based)</li>
 *   <li>{@link #getDeleteButtonCount()} — current number of delete buttons</li>
 *   <li>{@link #isDeleteButtonPresent()} — quick presence probe</li>
 * </ul>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — state is surfaced through getters so this class is
 *       reusable across positive, negative, and boundary test cases.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 * </ul>
 */
public class AddRemoveElementsPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** «Add Element» button that creates a new delete button on each click. */
    private static final By ADD_ELEMENT_BTN = By.xpath("//button[text()='Add Element']");

    /** All dynamically-added delete buttons created by the «Add Element» action. */
    private static final By DELETE_BUTTONS = By.cssSelector(".added-manually");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Add/Remove Elements page URL as configured
     * in {@link FrameworkConfig#appUrl(String)} with the {@code /add_remove_elements}
     * path appended.
     *
     * @return this {@link AddRemoveElementsPage} for method chaining
     */
    public AddRemoveElementsPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/add_remove_elements";
        log.info("Opening Add/Remove Elements page: {}", url);
        driver().get(url);
        waitForUrlContains("/add_remove_elements");
        return this;
    }

    /**
     * Clicks the «Add Element» button once, causing a new
     * {@code .added-manually} delete button to be appended to the page.
     *
     * @return this {@link AddRemoveElementsPage} for method chaining
     */
    public AddRemoveElementsPage clickAdd() {
        log.info("Clicking 'Add Element' button");
        click(ADD_ELEMENT_BTN);
        return this;
    }

    /**
     * Clicks the delete button at the specified 1-based index among all
     * {@code .added-manually} buttons currently present in the DOM.
     *
     * <p>The delete button at the given position removes itself from the DOM
     * when clicked. After this call the remaining buttons re-index; callers
     * should re-query {@link #getDeleteButtonCount()} if the new state matters.</p>
     *
     * @param index 1-based position of the delete button to click
     * @throws IndexOutOfBoundsException if {@code index} is less than 1 or
     *                                   greater than the current button count
     */
    public AddRemoveElementsPage clickDelete(final int index) {
        log.info("Clicking delete button at 1-based index {}", index);
        final List<WebElement> buttons = findAll(DELETE_BUTTONS);
        buttons.get(index - 1).click();
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the current number of {@code .added-manually} delete buttons
     * present in the DOM.
     *
     * <p>The count increases by one for each {@link #clickAdd()} call and
     * decreases by one for each successful {@link #clickDelete(int)} call.</p>
     *
     * @return non-negative count of delete buttons
     */
    public int getDeleteButtonCount() {
        return findAll(DELETE_BUTTONS).size();
    }

    /**
     * Returns {@code true} when at least one {@code .added-manually} delete
     * button is visible in the viewport.
     *
     * <p>Uses a short 3-second probe window so that tests polling this method
     * do not wait too long when no buttons are present.</p>
     *
     * @return {@code true} if one or more delete buttons are visible,
     *         {@code false} otherwise
     */
    public boolean isDeleteButtonPresent() {
        return isDisplayed(DELETE_BUTTONS);
    }
}
