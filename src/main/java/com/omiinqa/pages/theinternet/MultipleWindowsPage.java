package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

import java.util.ArrayList;

/**
 * Page Object for the Multiple Windows page of The Internet practice site
 * ({@code https://the-internet.herokuapp.com/windows}).
 *
 * <p>Encapsulates all window-handle bookkeeping — opening new windows,
 * switching between them by index, closing the current window, and restoring
 * focus to the main window — so test code never touches the raw
 * {@code WebDriver} window-management API directly.</p>
 *
 * <p><b>Typical usage pattern in a test:</b></p>
 * <pre>{@code
 * MultipleWindowsPage page = new MultipleWindowsPage().open();
 * String main = page.getMainWindowHandle();
 * page.clickOpenNewWindow();
 * page.switchToWindowByIndex(1);
 * String bodyText = page.getPageBodyText();
 * page.closeCurrentWindow();
 * page.switchToMainWindow(main);
 * }</pre>
 *
 * <p><b>POM contract:</b> contains no assertions; all state is exposed through
 * getters so tests decide what to verify.</p>
 */
public class MultipleWindowsPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By OPEN_NEW_WINDOW_LINK = By.linkText("Click Here");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Multiple Windows page as configured in
     * {@link FrameworkConfig#appUrl(String)} with key {@code "theinternet"}.
     *
     * @return this {@link MultipleWindowsPage} for method chaining
     */
    public MultipleWindowsPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/windows";
        log.info("Opening Multiple Windows page: {}", url);
        driver().get(url);
        waitForUrlContains("/windows");
        return this;
    }

    /**
     * Clicks the «Click Here» link that opens a new browser window or tab.
     *
     * <p>After this call a second window handle will be present in
     * {@link #getAllWindowHandles()}. Switch to it with
     * {@link #switchToWindowByIndex(int)} before interacting with its content.</p>
     *
     * @return this page for method chaining
     */
    public MultipleWindowsPage clickOpenNewWindow() {
        log.info("Clicking link to open new window");
        click(OPEN_NEW_WINDOW_LINK);
        return this;
    }

    /**
     * Switches the WebDriver focus to the window at the given index within the
     * ordered list of all current window handles.
     *
     * <p>The order of handles in the list is driver-specific and not guaranteed
     * to match visual z-order; index {@code 0} is typically the original (main)
     * window and index {@code 1} the first newly opened window.</p>
     *
     * @param index zero-based position in the list returned by
     *              {@link #getAllWindowHandles()}
     * @return this page for method chaining
     * @throws IndexOutOfBoundsException when {@code index} exceeds the number of
     *                                   open windows
     */
    public MultipleWindowsPage switchToWindowByIndex(final int index) {
        final String handle = getAllWindowHandles().get(index);
        log.info("Switching to window at index {} (handle={})", index, handle);
        driver().switchTo().window(handle);
        return this;
    }

    /**
     * Closes the currently focused window or tab.
     *
     * <p>After closing, the driver has no focused context. Call
     * {@link #switchToMainWindow(String)} with the previously captured main
     * handle to restore focus.</p>
     *
     * @return this page for method chaining
     */
    public MultipleWindowsPage closeCurrentWindow() {
        log.info("Closing current window");
        driver().close();
        return this;
    }

    /**
     * Switches WebDriver focus back to the window identified by
     * {@code mainHandle}.
     *
     * <p>Obtain {@code mainHandle} before opening new windows via
     * {@link #getMainWindowHandle()}.</p>
     *
     * @param mainHandle the opaque window handle string of the main window
     * @return this page for method chaining
     */
    public MultipleWindowsPage switchToMainWindow(final String mainHandle) {
        log.info("Switching back to main window (handle={})", mainHandle);
        driver().switchTo().window(mainHandle);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the opaque handle of the currently focused browser window.
     *
     * <p>Capture this immediately after {@link #open()} before any new windows
     * are opened so that {@link #switchToMainWindow(String)} can restore focus
     * later.</p>
     *
     * @return the current window handle string
     */
    public String getMainWindowHandle() {
        return driver().getWindowHandle();
    }

    /**
     * Returns a snapshot of all currently open window handles as an ordered
     * {@link ArrayList}.
     *
     * <p>The list is a new copy on every call; store it before and after opening
     * a window to detect the newly added handle by set-difference if ordering is
     * not deterministic in the target browser.</p>
     *
     * @return mutable ordered list of all window handle strings
     */
    public ArrayList<String> getAllWindowHandles() {
        return new ArrayList<>(driver().getWindowHandles());
    }

    /**
     * Returns the full visible text of the {@code <body>} element in the
     * currently focused window.
     *
     * <p>Switch to the desired window first with
     * {@link #switchToWindowByIndex(int)} or
     * {@link #switchToMainWindow(String)}.</p>
     *
     * @return trimmed body text of the active window
     */
    public String getPageBodyText() {
        return getText(By.tagName("body"));
    }
}
