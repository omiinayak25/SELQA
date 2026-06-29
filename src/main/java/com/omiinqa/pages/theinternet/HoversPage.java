package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

/**
 * Page Object for the Hovers example on The-Internet
 * ({@code https://the-internet.herokuapp.com/hovers}).
 *
 * <p>The page presents three user-profile figures arranged horizontally.
 * Each figure reveals a caption overlay ({@code .figcaption}) when the
 * pointer hovers over the image. The caption contains a heading ({@code h5})
 * and a «View profile» link.</p>
 *
 * <p>All hover interactions are performed with Selenium 4's
 * {@link Actions#moveToElement(WebElement)} API, which generates a real
 * {@code mousemove} event that triggers CSS {@code :hover} rules in supporting
 * browsers.</p>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through query methods.</li>
 *   <li>Figure indices are 1-based to match the human-readable label on the
 *       page (figure 1, 2, 3). Methods convert to 0-based internally via
 *       {@code get(index - 1)}.</li>
 *   <li>Locators are {@code private static final} {@link By} fields declared
 *       once and named for intent.</li>
 * </ul>
 */
public class HoversPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** All figure containers; use {@code findAll(FIGURES).get(index - 1)}. */
    private static final By FIGURES = By.cssSelector(".figure");

    /**
     * Caption overlay relative to a specific figure element.
     * Used as a sub-locator via {@link WebElement#findElement(By)}.
     */
    private static final By FIGCAPTION = By.cssSelector(".figcaption");

    /** Heading inside a figure's caption overlay. */
    private static final By CAPTION_HEADING = By.cssSelector(".figcaption h5");

    /** «View profile» link inside a figure's caption overlay. */
    private static final By VIEW_PROFILE_LINK = By.cssSelector(".figcaption a");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Hovers page URL as configured in
     * {@link FrameworkConfig#appUrl(String)} with the {@code /hovers} path.
     *
     * @return this {@link HoversPage} for method chaining
     */
    public HoversPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/hovers";
        log.info("Opening Hovers page: {}", url);
        driver().get(url);
        waitForUrlContains("/hovers");
        return this;
    }

    /**
     * Moves the mouse pointer over the figure at the given 1-based index,
     * triggering the CSS {@code :hover} state and revealing the caption overlay.
     *
     * <p>The method retrieves the full list of {@code .figure} elements with
     * {@link #findAll(By)} and selects the target by {@code index - 1}. After
     * moving to the element, {@link Actions#perform()} is called synchronously.</p>
     *
     * @param index 1-based figure index (valid values: 1, 2, 3)
     * @return this {@link HoversPage} for method chaining
     * @throws IndexOutOfBoundsException if {@code index} exceeds the number of figures
     */
    public HoversPage hoverOverFigure(final int index) {
        log.info("Hovering over figure {}", index);
        final WebElement figure = getFigureElement(index);
        new Actions(driver())
                .moveToElement(figure)
                .perform();
        return this;
    }

    /**
     * Hovers over the figure at the given 1-based index and then clicks the
     * «View profile» link revealed in its caption overlay.
     *
     * <p>The hover is performed first so that the caption becomes visible before
     * the click is attempted. {@link WaitUtils#visible(org.openqa.selenium.WebDriver, WebElement)}
     * confirms the caption has transitioned to the visible state.</p>
     *
     * @param index 1-based figure index (valid values: 1, 2, 3)
     * @return this {@link HoversPage} for method chaining
     */
    public HoversPage clickFigureLink(final int index) {
        log.info("Clicking 'View profile' link on figure {}", index);
        final WebElement figure = getFigureElement(index);
        new Actions(driver())
                .moveToElement(figure)
                .perform();
        final WebElement link = WaitUtils.visible(driver(), figure.findElement(VIEW_PROFILE_LINK));
        link.click();
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text of the {@code h5} heading inside the caption
     * overlay for the figure at the given 1-based index.
     *
     * <p>The caption overlay must already be visible before calling this method.
     * Invoke {@link #hoverOverFigure(int)} first to ensure the caption is shown.</p>
     *
     * @param index 1-based figure index (valid values: 1, 2, 3)
     * @return trimmed heading text; never {@code null}
     */
    public String getCaptionText(final int index) {
        final WebElement figure = getFigureElement(index);
        return figure.findElement(CAPTION_HEADING).getText().trim();
    }

    /**
     * Returns {@code true} when the {@code .figcaption} overlay of the figure at
     * the given 1-based index is currently visible in the viewport.
     *
     * <p>Uses a short explicit wait (via {@link #isDisplayed(By)}) so the method
     * is suitable for both asserting presence and confirming absence.</p>
     *
     * @param index 1-based figure index (valid values: 1, 2, 3)
     * @return {@code true} if the figcaption is displayed, {@code false} otherwise
     */
    public boolean isCaptionVisible(final int index) {
        final WebElement figure = getFigureElement(index);
        final WebElement caption = figure.findElement(FIGCAPTION);
        return caption.isDisplayed();
    }

    // -------------------------------------------------------------- internals

    /**
     * Resolves the {@link WebElement} for the figure at the given 1-based index.
     *
     * <p>Uses {@link #findAll(By)} so all {@code .figure} elements are waited on
     * before the list is accessed, preventing flaky {@code NoSuchElementException}
     * on slow renders.</p>
     *
     * @param index 1-based figure index
     * @return the resolved {@link WebElement} for the target figure
     */
    private WebElement getFigureElement(final int index) {
        final List<WebElement> figures = findAll(FIGURES);
        return figures.get(index - 1);
    }
}
