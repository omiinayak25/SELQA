package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.JavaScriptUtils;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Page Object for the Drag and Drop example on The-Internet
 * ({@code https://the-internet.herokuapp.com/drag_and_drop}).
 *
 * <p>The page contains two side-by-side columns ({@code #column-a} and
 * {@code #column-b}) that can be reordered by dragging one onto the other.
 * The page uses native HTML5 drag-and-drop events, which WebDriver's
 * {@link Actions#dragAndDrop(WebElement, WebElement)} may not trigger reliably
 * in all browsers. A JavaScript fallback ({@link #dragAtoBViaJs()}) dispatches
 * the required {@link DragEvent} sequence directly to work around this
 * limitation.</p>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through query methods.</li>
 *   <li>Locators are {@code private static final} {@link By} fields declared
 *       once and named for intent.</li>
 *   <li>JS drag simulation is provided as an explicit alternative method
 *       ({@link #dragAtoBViaJs()}, {@link #dragBtoAViaJs()}) rather than a
 *       silent fallback inside the Actions methods, so callers choose the
 *       interaction strategy deliberately.</li>
 * </ul>
 */
public class DragAndDropPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** Left-hand draggable column (initially labelled «A»). */
    private static final By COLUMN_A = By.cssSelector("#column-a");

    /** Right-hand draggable column (initially labelled «B»). */
    private static final By COLUMN_B = By.cssSelector("#column-b");

    /** Header element inside the left-hand column. */
    private static final By COLUMN_A_HEADER = By.cssSelector("#column-a header");

    /** Header element inside the right-hand column. */
    private static final By COLUMN_B_HEADER = By.cssSelector("#column-b header");

    // ---------------------------------------------- JavaScript drag sequence

    /**
     * Multi-step JavaScript drag-and-drop sequence compatible with HTML5 native
     * drag events. The script:
     * <ol>
     *   <li>Dispatches {@code dragstart} on the source element.</li>
     *   <li>Dispatches {@code dragenter} and {@code dragover} on the target.</li>
     *   <li>Dispatches {@code drop} on the target.</li>
     *   <li>Dispatches {@code dragend} on the source.</li>
     * </ol>
     * Arguments: {@code arguments[0]} = source element, {@code arguments[1]} = target element.
     */
    private static final String JS_DRAG_SCRIPT =
            "var src = arguments[0];"
            + "var tgt = arguments[1];"
            + "function fire(el, type) {"
            + "  var evt = new DragEvent(type, {bubbles:true, cancelable:true});"
            + "  el.dispatchEvent(evt);"
            + "}"
            + "fire(src, 'dragstart');"
            + "fire(tgt, 'dragenter');"
            + "fire(tgt, 'dragover');"
            + "fire(tgt, 'drop');"
            + "fire(src, 'dragend');";

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Drag and Drop page URL as configured in
     * {@link FrameworkConfig#appUrl(String)} with the {@code /drag_and_drop} path.
     *
     * @return this {@link DragAndDropPage} for method chaining
     */
    public DragAndDropPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/drag_and_drop";
        log.info("Opening Drag and Drop page: {}", url);
        driver().get(url);
        waitForUrlContains("/drag_and_drop");
        return this;
    }

    /**
     * Drags {@code #column-a} onto {@code #column-b} using the Selenium
     * {@link Actions#dragAndDrop(WebElement, WebElement)} API.
     *
     * <p><b>Note:</b> HTML5 native drag events are notoriously unreliable in
     * certain browser/driver combinations. If this method does not produce the
     * expected column swap, use {@link #dragAtoBViaJs()} instead, which fires
     * the drag event sequence via JavaScript.</p>
     *
     * @return this {@link DragAndDropPage} for method chaining
     */
    public DragAndDropPage dragAtoB() {
        log.info("Dragging column A onto column B via Actions");
        final WebElement source = WaitUtils.visible(driver(), COLUMN_A);
        final WebElement target = WaitUtils.visible(driver(), COLUMN_B);
        new Actions(driver())
                .dragAndDrop(source, target)
                .perform();
        return this;
    }

    /**
     * Drags {@code #column-b} onto {@code #column-a} using the Selenium
     * {@link Actions#dragAndDrop(WebElement, WebElement)} API.
     *
     * <p>See the caveat on HTML5 drag reliability in {@link #dragAtoB()}.
     * Use {@link #dragBtoAViaJs()} for a more reliable alternative.</p>
     *
     * @return this {@link DragAndDropPage} for method chaining
     */
    public DragAndDropPage dragBtoA() {
        log.info("Dragging column B onto column A via Actions");
        final WebElement source = WaitUtils.visible(driver(), COLUMN_B);
        final WebElement target = WaitUtils.visible(driver(), COLUMN_A);
        new Actions(driver())
                .dragAndDrop(source, target)
                .perform();
        return this;
    }

    /**
     * Drags {@code #column-a} onto {@code #column-b} by dispatching a
     * sequence of native HTML5 {@link DragEvent DragEvents} via JavaScript.
     *
     * <p>The JS helper fires {@code dragstart} on the source, then
     * {@code dragenter}, {@code dragover}, and {@code drop} on the target,
     * and finally {@code dragend} on the source — replicating what a real
     * browser drag gesture produces. Use this method when
     * {@link Actions#dragAndDrop(WebElement, WebElement)} fails to trigger
     * the application's drag handler.</p>
     *
     * @return this {@link DragAndDropPage} for method chaining
     */
    public DragAndDropPage dragAtoBViaJs() {
        log.info("Dragging column A onto column B via JavaScript DragEvent sequence");
        final WebElement source = WaitUtils.present(driver(), COLUMN_A);
        final WebElement target = WaitUtils.present(driver(), COLUMN_B);
        JavaScriptUtils.execute(driver(), JS_DRAG_SCRIPT, source, target);
        return this;
    }

    /**
     * Drags {@code #column-b} onto {@code #column-a} by dispatching a
     * sequence of native HTML5 {@link DragEvent DragEvents} via JavaScript.
     *
     * <p>See {@link #dragAtoBViaJs()} for a full description of the event
     * sequence and when to prefer this method over the {@link Actions} variant.</p>
     *
     * @return this {@link DragAndDropPage} for method chaining
     */
    public DragAndDropPage dragBtoAViaJs() {
        log.info("Dragging column B onto column A via JavaScript DragEvent sequence");
        final WebElement source = WaitUtils.present(driver(), COLUMN_B);
        final WebElement target = WaitUtils.present(driver(), COLUMN_A);
        JavaScriptUtils.execute(driver(), JS_DRAG_SCRIPT, source, target);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text of the header element inside {@code #column-a}.
     *
     * <p>After a successful drag the header labels swap positions. Reading this
     * value before and after a drag operation lets callers verify the outcome
     * without hardcoding expected text in the page object.</p>
     *
     * @return trimmed column-A header text (e.g. {@code "A"} or {@code "B"})
     */
    public String getColumnAHeader() {
        return getText(COLUMN_A_HEADER);
    }

    /**
     * Returns the trimmed text of the header element inside {@code #column-b}.
     *
     * @return trimmed column-B header text (e.g. {@code "B"} or {@code "A"})
     */
    public String getColumnBHeader() {
        return getText(COLUMN_B_HEADER);
    }
}
