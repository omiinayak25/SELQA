package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;

/**
 * Page Object for the Dynamic Loading examples on The-Internet
 * ({@code https://the-internet.herokuapp.com/dynamic_loading/1} and
 * {@code /dynamic_loading/2}).
 *
 * <p>Example 1 hides an already-rendered element behind a loading spinner;
 * Example 2 renders the element only after the loading spinner disappears.
 * Both variants share the same Start button and #finish result element, so a
 * single page object covers them both — the caller selects the variant via
 * {@link #open(int)}.</p>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through getters so this class is
 *       reusable across positive, negative, and boundary test scenarios.</li>
 *   <li>Locators are {@code private static final} {@link By} fields declared
 *       once and named for intent.</li>
 *   <li>Synchronisation is delegated to {@link WaitUtils} and {@link com.omiinqa.utils.WaitUtils}
 *       using the explicit timeout configured in {@link FrameworkConfig}.</li>
 * </ul>
 */
public class DynamicLoadingPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** «Start» button that triggers the loading sequence. */
    private static final By START_BUTTON = By.cssSelector("#start button");

    /**
     * Result element revealed after loading completes.
     * Present in the DOM for Example 1 (hidden initially); injected for Example 2.
     */
    private static final By FINISH_ELEMENT = By.cssSelector("#finish");

    /** Spinner shown while the async load is in progress. */
    private static final By LOADING_SPINNER = By.cssSelector("#loading");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the selected dynamic-loading example.
     *
     * <p>The page appends the numeric example identifier to the base URL so that
     * both variants ({@code /dynamic_loading/1} and {@code /dynamic_loading/2})
     * are reachable through a single method.</p>
     *
     * @param example the example number to open — {@code 1} (element hidden) or
     *                {@code 2} (element rendered after load)
     * @return this {@link DynamicLoadingPage} for method chaining
     * @throws IllegalArgumentException if {@code example} is not 1 or 2
     */
    public DynamicLoadingPage open(final int example) {
        if (example != 1 && example != 2) {
            throw new IllegalArgumentException(
                    "Dynamic Loading only has examples 1 and 2; received: " + example);
        }
        final String url = FrameworkConfig.get().appUrl("theinternet")
                + "/dynamic_loading/" + example;
        log.info("Opening Dynamic Loading example {}: {}", example, url);
        driver().get(url);
        waitForUrlContains("/dynamic_loading/" + example);
        return this;
    }

    /**
     * Clicks the {@code #start} button to begin the loading sequence.
     *
     * <p>The method waits for the button to be clickable before interacting,
     * ensuring the page is ready even after a fresh navigation.</p>
     *
     * @return this {@link DynamicLoadingPage} for method chaining
     */
    public DynamicLoadingPage clickStart() {
        log.info("Clicking Start button on Dynamic Loading page");
        click(START_BUTTON);
        return this;
    }

    /**
     * Blocks until the {@code #finish} element is visible in the viewport.
     *
     * <p>Uses {@link WaitUtils#visible(org.openqa.selenium.WebDriver, By)} with
     * the framework's configured explicit timeout, so the poll interval and
     * maximum wait duration are controlled centrally through
     * {@link FrameworkConfig}.</p>
     *
     * @return this {@link DynamicLoadingPage} for method chaining
     */
    public DynamicLoadingPage waitForFinish() {
        log.info("Waiting for #finish element to become visible");
        WaitUtils.visible(driver(), FINISH_ELEMENT);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text content of the {@code #finish} result element.
     *
     * <p>Call {@link #waitForFinish()} before invoking this method to avoid
     * reading from an element that has not yet appeared or become visible.</p>
     *
     * @return trimmed result text; never {@code null}
     */
    public String getFinishText() {
        return getText(FINISH_ELEMENT);
    }

    /**
     * Returns {@code true} when the loading spinner ({@code #loading}) is
     * currently visible in the viewport.
     *
     * <p>Uses a short implicit probe window (3 seconds) so that tests polling
     * this method during an async operation do not wait too long when the
     * spinner is absent.</p>
     *
     * @return {@code true} if the spinner is visible, {@code false} otherwise
     */
    public boolean isLoadingSpinnerVisible() {
        return isDisplayed(LOADING_SPINNER);
    }
}
