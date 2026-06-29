package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.JavaScriptUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Page Object covering all frame-related pages on The Internet practice site:
 *
 * <ul>
 *   <li><b>Hub page</b> — {@code /frames} — links to the nested and iframe demos</li>
 *   <li><b>Nested frames</b> — {@code /nested_frames} — top/bottom/left/middle/right frames</li>
 *   <li><b>Inline frame</b> — {@code /iframe} — a TinyMCE rich-text editor embedded in
 *       an {@code <iframe id="mce_0_ifr">}</li>
 * </ul>
 *
 * <p>All three pages are consolidated here because they share the same
 * frame-switching concerns: switching in, interacting, switching back. A single
 * page object avoids scattering identical {@code driver().switchTo()} boilerplate
 * across multiple classes.</p>
 *
 * <p><b>POM contract:</b> contains no assertions. Frame text and TinyMCE content
 * are exposed via getters. All frame-switch state changes are fully documented so
 * callers can reason about the driver's current context.</p>
 *
 * <p><b>Important context note:</b> every {@code switchTo*} method mutates the
 * driver's frame context. After any frame interaction always call
 * {@link #switchToDefaultContent()} to reset to the top-level document before
 * interacting with elements outside a frame.</p>
 */
public class FramesPage extends BasePage {

    /**
     * TinyMCE iframe id on {@code /iframe} page of The Internet.
     * The value {@code "mce_0_ifr"} is the stable id assigned by TinyMCE 4.x.
     */
    public static final String TINYMCE_IFRAME_ID = "mce_0_ifr";

    // ----------------------------------------------------------------- actions

    /**
     * Navigates to the Frames hub page ({@code /frames}) as configured in
     * {@link FrameworkConfig#appUrl(String)} with key {@code "theinternet"}.
     *
     * @return this {@link FramesPage} for method chaining
     */
    public FramesPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/frames";
        log.info("Opening Frames hub page: {}", url);
        driver().get(url);
        waitForUrlContains("/frames");
        return this;
    }

    /**
     * Navigates directly to the Nested Frames page ({@code /nested_frames}).
     *
     * <p>This page embeds several named frames in a two-level hierarchy. Use
     * {@link #switchToFrameByNameOrId(String)} with names such as
     * {@code "frame-top"}, {@code "frame-bottom"}, {@code "frame-left"},
     * {@code "frame-middle"}, or {@code "frame-right"} to enter them.</p>
     *
     * @return this page for method chaining
     */
    public FramesPage openNestedFrames() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/nested_frames";
        log.info("Opening Nested Frames page: {}", url);
        driver().get(url);
        waitForUrlContains("nested_frames");
        return this;
    }

    /**
     * Navigates directly to the iFrame page ({@code /iframe}) that hosts the
     * TinyMCE rich-text editor.
     *
     * <p>The TinyMCE editor is rendered inside {@code <iframe id="mce_0_ifr">}.
     * Use {@link #switchToIframeById(String)} with {@link #TINYMCE_IFRAME_ID}
     * before calling {@link #typeInTinyMce(String)} or
     * {@link #getTinyMceContent()}.</p>
     *
     * @return this page for method chaining
     */
    public FramesPage openIframePage() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/iframe";
        log.info("Opening iFrame (TinyMCE) page: {}", url);
        driver().get(url);
        waitForUrlContains("/iframe");
        return this;
    }

    /**
     * Switches the WebDriver context into the frame identified by its
     * {@code id} attribute.
     *
     * <p>After this call all subsequent {@code driver().findElement} lookups
     * operate within the nominated frame's DOM subtree.</p>
     *
     * @param frameId the {@code id} attribute of the target {@code <iframe>} or
     *                {@code <frame>} element
     * @return this page for method chaining
     */
    public FramesPage switchToIframeById(final String frameId) {
        log.info("Switching into frame by id='{}'", frameId);
        driver().switchTo().frame(frameId);
        return this;
    }

    /**
     * Switches the WebDriver context into the frame at the given zero-based
     * index within the current document (or current frame's child frames).
     *
     * @param index zero-based position of the {@code <frame>} or {@code <iframe>}
     *              element in document order
     * @return this page for method chaining
     */
    public FramesPage switchToFrameByIndex(final int index) {
        log.info("Switching into frame by index={}", index);
        driver().switchTo().frame(index);
        return this;
    }

    /**
     * Switches the WebDriver context into the frame identified by its
     * {@code name} or {@code id} attribute.
     *
     * <p>Selenium resolves by {@code name} first, then falls back to {@code id}.
     * This is the preferred switch method for named frames on the Nested Frames
     * page (e.g. {@code "frame-top"}, {@code "frame-middle"}).</p>
     *
     * @param nameOrId the {@code name} or {@code id} of the target frame element
     * @return this page for method chaining
     */
    public FramesPage switchToFrameByNameOrId(final String nameOrId) {
        log.info("Switching into frame by name/id='{}'", nameOrId);
        driver().switchTo().frame(nameOrId);
        return this;
    }

    /**
     * Resets the WebDriver context to the top-level document, exiting all
     * frames.
     *
     * <p>Always call this after finishing work inside any frame before
     * interacting with elements in the main page.</p>
     *
     * @return this page for method chaining
     */
    public FramesPage switchToDefaultContent() {
        log.info("Switching to default content (top-level document)");
        driver().switchTo().defaultContent();
        return this;
    }

    /**
     * Moves the WebDriver context one level up to the parent frame.
     *
     * <p>Useful when working inside deeply nested frames and a single level
     * exit is needed rather than a full reset to the top-level document.</p>
     *
     * @return this page for method chaining
     */
    public FramesPage switchToParentFrame() {
        log.info("Switching to parent frame");
        driver().switchTo().parentFrame();
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the visible text content of the {@code <body>} element in the
     * currently active frame context.
     *
     * <p>Must be called <em>after</em> switching into the target frame with one
     * of the {@code switchTo*} methods. For nested frames, switch into each
     * level in order before calling this.</p>
     *
     * @return trimmed body text of the active frame
     */
    public String getFrameBodyText() {
        return getText(By.tagName("body"));
    }

    /**
     * Convenience method that switches into the named/id frame, captures its
     * body text, then returns to the default content in a single call.
     *
     * <p>The driver context is always reset to the top-level document on
     * return, even if text retrieval succeeds — callers do not need to call
     * {@link #switchToDefaultContent()} afterward.</p>
     *
     * @param nameOrId the {@code name} or {@code id} of the target frame element
     * @return the trimmed body text found inside that frame
     */
    public String getNestedFrameText(final String nameOrId) {
        log.info("Reading body text from frame '{}'", nameOrId);
        driver().switchTo().frame(nameOrId);
        final String text = getText(By.tagName("body"));
        driver().switchTo().defaultContent();
        return text;
    }

    /**
     * Types {@code text} into the TinyMCE editor body after first clearing any
     * pre-existing content via JavaScript.
     *
     * <p><b>Pre-condition:</b> the driver context must already be inside the
     * TinyMCE iframe (switched via
     * {@link #switchToIframeById(String) switchToIframeById(TINYMCE_IFRAME_ID)})
     * before this method is called.</p>
     *
     * <p><b>Implementation detail:</b> TinyMCE renders a contenteditable
     * {@code <body>} element. The content is cleared by setting its
     * {@code innerHTML} to an empty string via JS, then the text is typed using
     * {@link Actions#sendKeys(CharSequence...)} on the active element so that
     * TinyMCE's own event handlers fire correctly.</p>
     *
     * @param text the plain text to type into the editor
     * @return this page for method chaining
     */
    public FramesPage typeInTinyMce(final String text) {
        log.info("Typing text into TinyMCE editor");
        final WebElement body = driver().findElement(By.tagName("body"));
        JavaScriptUtils.execute(driver(), "arguments[0].innerHTML='';", body);
        new Actions(driver()).sendKeys(text).perform();
        return this;
    }

    /**
     * Returns the raw HTML content of the TinyMCE editor.
     *
     * <p><b>Pre-condition:</b> the driver context must already be inside the
     * TinyMCE iframe before this method is called.</p>
     *
     * <p>Retrieves the content via JavaScript by reading
     * {@code document.getElementById('tinymce').innerHTML}. If that element is
     * absent (e.g. TinyMCE used a different skin id), falls back to
     * {@code document.querySelector('.mce-content-body').innerHTML}.</p>
     *
     * @return the inner HTML string of the TinyMCE editor body; never
     *         {@code null} — returns an empty string when neither selector
     *         resolves
     */
    public String getTinyMceContent() {
        log.info("Reading TinyMCE editor content via JavaScript");
        final String script =
                "var el = document.getElementById('tinymce');"
                + " if (!el) { el = document.querySelector('.mce-content-body'); }"
                + " return el ? el.innerHTML : '';";
        final Object result = JavaScriptUtils.execute(driver(), script);
        return result != null ? result.toString() : "";
    }
}
