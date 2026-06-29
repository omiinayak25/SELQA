package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Page Object for the The-Internet number-input exercise
 * ({@code https://the-internet.herokuapp.com/inputs}).
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>{@link Actions} is used for arrow-key presses so the interaction fully
 *       mirrors real keyboard events — the browser's native spinner behaviour
 *       fires identically to a physical key-press.</li>
 *   <li>{@link #setNumber(String)} performs an explicit {@link WebElement#clear()}
 *       before sending keys because {@code type()} in {@link com.omiinqa.core.BasePage}
 *       already does so, but is also available via the protected helper for
 *       consistency with the rest of the framework.</li>
 *   <li>No assertions are included — callers use {@link #getInputValue()} to
 *       build their own verification logic.</li>
 * </ul>
 *
 * <p>Always call {@link #open()} at the start of a test to navigate to a known
 * state.</p>
 */
public class InputsPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** The {@code <input type="number">} element that accepts numeric keyboard input. */
    private static final By NUMBER_INPUT = By.cssSelector("input[type='number']");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the inputs page URL as configured in
     * {@code FrameworkConfig.get().appUrl("theinternet")} with the
     * {@code /inputs} path appended.
     *
     * @return this {@link InputsPage} for method chaining
     */
    public InputsPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/inputs";
        log.info("Opening The-Internet inputs page: {}", url);
        driver().get(url);
        waitForUrlContains("/inputs");
        return this;
    }

    /**
     * Clears the number input and types the given value into it.
     *
     * <p>The field is cleared before typing to remove any pre-existing value
     * (including one set by a prior arrow-key interaction in the same test).</p>
     *
     * @param value the numeric string to type (e.g. {@code "42"})
     */
    public void setNumber(final String value) {
        log.info("Setting number input value to '{}'", value);
        type(NUMBER_INPUT, value);
    }

    /**
     * Clears the current content of the number input field without typing any
     * replacement value.
     */
    public void clearInput() {
        log.info("Clearing number input");
        WaitUtils.visible(driver(), NUMBER_INPUT).clear();
    }

    /**
     * Sends the {@link Keys#ARROW_UP} key to the number input {@code n} times,
     * incrementing the spinner value by one per key-press.
     *
     * <p>The element is focused via {@link Actions#click(WebElement)} before
     * key delivery to guarantee the key events are directed to the correct
     * target.</p>
     *
     * @param n the number of times to press the ARROW_UP key; must be &ge; 0
     */
    public void sendArrowUp(final int n) {
        log.info("Sending ARROW_UP {} time(s) to number input", n);
        sendArrowKey(Keys.ARROW_UP, n);
    }

    /**
     * Sends the {@link Keys#ARROW_DOWN} key to the number input {@code n} times,
     * decrementing the spinner value by one per key-press.
     *
     * <p>The element is focused via {@link Actions#click(WebElement)} before
     * key delivery to guarantee the key events are directed to the correct
     * target.</p>
     *
     * @param n the number of times to press the ARROW_DOWN key; must be &ge; 0
     */
    public void sendArrowDown(final int n) {
        log.info("Sending ARROW_DOWN {} time(s) to number input", n);
        sendArrowKey(Keys.ARROW_DOWN, n);
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the current {@code value} attribute of the number input, which
     * represents the raw numeric string held by the field (e.g. {@code "5"}).
     *
     * <p>An empty string is returned when the field has been cleared or never
     * populated.</p>
     *
     * @return the {@code value} attribute string; never {@code null}
     */
    public String getInputValue() {
        return getAttribute(NUMBER_INPUT, "value");
    }

    // --------------------------------------------------------- private helpers

    /**
     * Focuses the number input and sends the specified {@link Keys} stroke
     * {@code n} times using {@link Actions}.
     *
     * @param key the key to press (must be a navigation key constant)
     * @param n   the repetition count; a value of {@code 0} is a no-op
     */
    private void sendArrowKey(final Keys key, final int n) {
        if (n <= 0) {
            return;
        }
        final WebElement input = WaitUtils.visible(driver(), NUMBER_INPUT);
        final Actions actions = new Actions(driver());
        actions.click(input);
        for (int i = 0; i < n; i++) {
            actions.sendKeys(key);
        }
        actions.perform();
    }
}
