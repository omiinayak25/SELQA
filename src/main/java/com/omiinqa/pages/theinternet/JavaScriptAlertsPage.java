package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the JavaScript Alerts page of The Internet practice site
 * ({@code https://the-internet.herokuapp.com/javascript_alerts}).
 *
 * <p>Covers the three native browser dialog types — {@code window.alert},
 * {@code window.confirm}, and {@code window.prompt} — and the result message
 * rendered after each dialog is handled.</p>
 *
 * <p><b>POM contract:</b> contains no assertions; alert text and result text
 * are surfaced via getters so callers decide what to verify. All Selenium
 * {@link org.openqa.selenium.Alert} interactions are encapsulated here,
 * keeping test code free of driver-level details.</p>
 */
public class JavaScriptAlertsPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By ALERT_BUTTON   = By.cssSelector("button[onclick='jsAlert()']");
    private static final By CONFIRM_BUTTON = By.cssSelector("button[onclick='jsConfirm()']");
    private static final By PROMPT_BUTTON  = By.cssSelector("button[onclick='jsPrompt()']");
    private static final By RESULT         = By.id("result");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the JavaScript Alerts page as configured in
     * {@link FrameworkConfig#appUrl(String)} with key {@code "theinternet"}.
     *
     * @return this {@link JavaScriptAlertsPage} for method chaining
     */
    public JavaScriptAlertsPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/javascript_alerts";
        log.info("Opening JavaScript Alerts page: {}", url);
        driver().get(url);
        waitForUrlContains("javascript_alerts");
        return this;
    }

    /**
     * Clicks the «Click for JS Alert» button, which triggers a native
     * {@code window.alert} dialog.
     *
     * <p>The dialog must subsequently be handled by {@link #acceptAlert()} or
     * {@link #getAlertText()} before any further page interaction.</p>
     *
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage clickAlertButton() {
        log.info("Clicking JS Alert button");
        click(ALERT_BUTTON);
        return this;
    }

    /**
     * Clicks the «Click for JS Confirm» button, which triggers a native
     * {@code window.confirm} dialog.
     *
     * <p>The dialog must subsequently be handled by {@link #acceptAlert()} or
     * {@link #dismissAlert()} before any further page interaction.</p>
     *
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage clickConfirmButton() {
        log.info("Clicking JS Confirm button");
        click(CONFIRM_BUTTON);
        return this;
    }

    /**
     * Clicks the «Click for JS Prompt» button, which triggers a native
     * {@code window.prompt} dialog that accepts text input.
     *
     * <p>Use {@link #sendTextToPrompt(String)} to type into the prompt and
     * accept it in a single call.</p>
     *
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage clickPromptButton() {
        log.info("Clicking JS Prompt button");
        click(PROMPT_BUTTON);
        return this;
    }

    /**
     * Accepts (clicks «OK» on) the currently open browser dialog.
     *
     * <p>Safe to call after any of the three trigger buttons — alert,
     * confirm, or prompt (when no input is needed).</p>
     *
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage acceptAlert() {
        log.info("Accepting browser alert");
        driver().switchTo().alert().accept();
        return this;
    }

    /**
     * Dismisses (clicks «Cancel» on) the currently open browser dialog.
     *
     * <p>Meaningful only for confirm and prompt dialogs; dismiss on an alert
     * behaves the same as accept.</p>
     *
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage dismissAlert() {
        log.info("Dismissing browser alert");
        driver().switchTo().alert().dismiss();
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the text currently displayed inside the open browser dialog
     * without accepting or dismissing it.
     *
     * <p>Call this before {@link #acceptAlert()} or {@link #dismissAlert()} when
     * the test needs to verify the dialog message.</p>
     *
     * @return the raw text of the native browser dialog
     */
    public String getAlertText() {
        final String text = driver().switchTo().alert().getText();
        log.debug("Alert text: '{}'", text);
        return text;
    }

    /**
     * Types {@code text} into the currently open prompt dialog and then
     * accepts it, dismissing the dialog and submitting the entered value.
     *
     * @param text the value to enter into the prompt input field
     * @return this page for method chaining
     */
    public JavaScriptAlertsPage sendTextToPrompt(final String text) {
        log.info("Sending text '{}' to JS prompt", text);
        driver().switchTo().alert().sendKeys(text);
        driver().switchTo().alert().accept();
        return this;
    }

    /**
     * Returns the text content of the result paragraph rendered on the page
     * after a dialog has been accepted or dismissed.
     *
     * <p>The element id is {@code #result}. Typical values include
     * {@code "You successfully clicked an alert"},
     * {@code "You clicked: Ok"}, {@code "You clicked: Cancel"},
     * and {@code "You entered: <prompt input>"}.</p>
     *
     * @return trimmed text of the {@code #result} element
     */
    public String getResultText() {
        return getText(RESULT);
    }
}
