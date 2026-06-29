package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Page Object for the Dynamic Controls example on The-Internet
 * ({@code https://the-internet.herokuapp.com/dynamic_controls}).
 *
 * <p>The page contains two independently controlled forms:</p>
 * <ol>
 *   <li><b>Checkbox form</b> — a button removes or re-adds a checkbox via an
 *       asynchronous DOM mutation ({@code #checkbox-example}).</li>
 *   <li><b>Input form</b> — a button enables or disables a text input field
 *       via an asynchronous DOM state change ({@code #input-example}).</li>
 * </ol>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through query methods so this
 *       class is reusable across positive, negative, and boundary scenarios.</li>
 *   <li>All synchronisation is delegated to {@link WaitUtils}, keeping
 *       polling policy in one place.</li>
 *   <li>Locators are {@code private static final} {@link By} fields declared
 *       once and named for intent.</li>
 * </ul>
 */
public class DynamicControlsPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** Remove/Add button in the checkbox form. */
    private static final By CHECKBOX_FORM_BUTTON = By.cssSelector("#checkbox-example button");

    /** Enable/Disable button in the input form. */
    private static final By INPUT_FORM_BUTTON = By.cssSelector("#input-example button");

    /**
     * The checkbox element. Present in the DOM only when it has not been removed;
     * use {@link #isCheckboxPresent()} for a safe probe.
     */
    private static final By CHECKBOX = By.cssSelector("#checkbox");

    /** The text input field inside the input example form. */
    private static final By INPUT_FIELD = By.cssSelector("#input-example input[type=text]");

    /**
     * Status / feedback message shown below each form after an async operation
     * completes (e.g. "It's gone!", "It's back!", "It's enabled!", "It's disabled!").
     */
    private static final By STATUS_MESSAGE = By.cssSelector("#message");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Dynamic Controls page URL as configured in
     * {@link FrameworkConfig#appUrl(String)} with the {@code /dynamic_controls} path.
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/dynamic_controls";
        log.info("Opening Dynamic Controls page: {}", url);
        driver().get(url);
        waitForUrlContains("/dynamic_controls");
        return this;
    }

    /**
     * Clicks the Remove / Add button in the checkbox form ({@code #checkbox-example button}).
     *
     * <p>The button toggles between removing and re-adding the checkbox element.
     * After clicking, call either {@link #waitForCheckboxGone()} or
     * {@link #waitForCheckboxPresent()} to synchronise before any subsequent
     * assertion.</p>
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage clickRemoveAddCheckboxButton() {
        log.info("Clicking Remove/Add button in checkbox form");
        click(CHECKBOX_FORM_BUTTON);
        return this;
    }

    /**
     * Clicks the Enable / Disable button in the input form ({@code #input-example button}).
     *
     * <p>After clicking, call {@link #waitForInputEnabled()} or
     * {@link #waitForInputDisabled(String)} to synchronise before querying
     * {@link #isInputEnabled()}.</p>
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage clickEnableDisableInputButton() {
        log.info("Clicking Enable/Disable button in input form");
        click(INPUT_FORM_BUTTON);
        return this;
    }

    /**
     * Blocks until the {@code #checkbox} element is no longer present in the DOM.
     *
     * <p>Uses {@link WaitUtils#invisible(org.openqa.selenium.WebDriver, By)}, which
     * waits for invisibility (which also covers DOM removal) within the framework's
     * configured explicit timeout.</p>
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage waitForCheckboxGone() {
        log.info("Waiting for #checkbox to be removed from the DOM");
        WaitUtils.invisible(driver(), CHECKBOX);
        return this;
    }

    /**
     * Blocks until the {@code #checkbox} element is present in the DOM.
     *
     * <p>Uses {@link WaitUtils#present(org.openqa.selenium.WebDriver, By)} so the
     * element need not be visible, only accessible in the DOM tree.</p>
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage waitForCheckboxPresent() {
        log.info("Waiting for #checkbox to be present in the DOM");
        WaitUtils.present(driver(), CHECKBOX);
        return this;
    }

    /**
     * Blocks until the text input field inside {@code #input-example} is clickable
     * (i.e. both visible and enabled).
     *
     * <p>Uses {@link ExpectedConditions#elementToBeClickable(By)} via
     * {@link WaitUtils#until(org.openqa.selenium.WebDriver,
     * org.openqa.selenium.support.ui.ExpectedCondition)} so the wait policy
     * remains centralised.</p>
     *
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage waitForInputEnabled() {
        log.info("Waiting for input field to become enabled (clickable)");
        WaitUtils.until(driver(), ExpectedConditions.elementToBeClickable(INPUT_FIELD));
        return this;
    }

    /**
     * Blocks until the status message area ({@code #message}) contains the
     * specified text, confirming that the async enable/disable operation finished.
     *
     * <p>Typical values are {@code "It's enabled!"} and {@code "It's disabled!"}.
     * Uses {@link WaitUtils#textPresent(org.openqa.selenium.WebDriver, By, String)}
     * so polling interval and timeout are controlled centrally.</p>
     *
     * @param expectedText the text fragment to wait for in the message element
     * @return this {@link DynamicControlsPage} for method chaining
     */
    public DynamicControlsPage waitForInputDisabled(final String expectedText) {
        log.info("Waiting for status message to contain: '{}'", expectedText);
        WaitUtils.textPresent(driver(), STATUS_MESSAGE, expectedText);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text of the status / feedback message element
     * ({@code #message}).
     *
     * <p>The message is populated asynchronously after a Remove, Add, Enable, or
     * Disable operation. Call an appropriate {@code waitFor*} method before
     * reading this value to ensure the operation has completed.</p>
     *
     * @return trimmed status message text; never {@code null}
     */
    public String getStatusMessage() {
        return getText(STATUS_MESSAGE);
    }

    /**
     * Returns {@code true} when the {@code #checkbox} element is currently
     * displayed in the viewport.
     *
     * <p>Uses the inherited {@link #isDisplayed(By)} probe which applies a short
     * timeout, making it suitable for both positive and negative assertions in
     * tests.</p>
     *
     * @return {@code true} if the checkbox is visible, {@code false} otherwise
     */
    public boolean isCheckboxPresent() {
        return isDisplayed(CHECKBOX);
    }

    /**
     * Returns {@code true} when the text input field inside {@code #input-example}
     * is enabled at the WebDriver level.
     *
     * <p>Finds the element via {@link WaitUtils#present(org.openqa.selenium.WebDriver, By)}
     * and delegates to {@link WebElement#isEnabled()}, which reflects the HTML
     * {@code disabled} attribute state independently of CSS visibility.</p>
     *
     * @return {@code true} if the input is enabled, {@code false} if it is disabled
     */
    public boolean isInputEnabled() {
        final WebElement input = WaitUtils.present(driver(), INPUT_FIELD);
        return input.isEnabled();
    }
}
