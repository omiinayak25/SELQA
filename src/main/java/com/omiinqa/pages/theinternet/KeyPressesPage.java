package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Page Object for the The-Internet key presses feature page at {@code /key_presses}.
 *
 * <p>The page renders a text input ({@code #target}) and a result element
 * ({@code #result}) that displays the name of the last key pressed. This class
 * uses {@link Actions#sendKeys(CharSequence...)} to dispatch individual key events
 * so that non-printable keys (function keys, arrows, escape, etc.) can be tested
 * independently of the character they produce.</p>
 *
 * <p><b>Page Object Model contract:</b> No assertions; all state is surfaced
 * through query methods. Locators are {@code private static final} {@link By}
 * fields.</p>
 */
public class KeyPressesPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By INPUT  = By.cssSelector("#target");
    private static final By RESULT = By.cssSelector("#result");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates to {@code /key_presses} using the configured base URL.
     *
     * @return this page for method chaining
     */
    public KeyPressesPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/key_presses";
        log.info("Opening Key Presses page: {}", url);
        driver().get(url);
        WaitUtils.visible(driver(), INPUT);
        return this;
    }

    /**
     * Sends a single key to the page input via {@link Actions#sendKeys}, triggering
     * the application's {@code keyup} handler and populating {@code #result}.
     *
     * @param key the {@link Keys} constant to send (e.g. {@link Keys#ENTER},
     *            {@link Keys#TAB}, {@link Keys#F5})
     */
    public void pressKey(final Keys key) {
        log.info("Sending key {} to #target input", key.name());
        final WebElement input = WaitUtils.clickable(driver(), INPUT);
        new Actions(driver()).sendKeys(input, key).perform();
    }

    /**
     * Types printable text into the {@code #target} input element.
     *
     * @param text the characters to type
     */
    public void typeInInput(final String text) {
        log.info("Typing '{}' into #target input", text);
        final WebElement input = WaitUtils.clickable(driver(), INPUT);
        new Actions(driver()).sendKeys(input, text).perform();
    }

    /**
     * Clears the current input value and resets the result by focusing the input
     * and sending {@code Ctrl+A} followed by {@link Keys#DELETE}.
     */
    public void clearResult() {
        log.info("Clearing input content");
        final WebElement input = WaitUtils.clickable(driver(), INPUT);
        new Actions(driver())
                .click(input)
                .keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
                .sendKeys(Keys.DELETE)
                .perform();
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text of the {@code #result} element, which displays
     * the name of the last key pressed (e.g. {@code "You entered: RETURN"}).
     *
     * @return result element text; empty string when no key has been pressed yet
     */
    public String getResultText() {
        return getText(RESULT);
    }
}
