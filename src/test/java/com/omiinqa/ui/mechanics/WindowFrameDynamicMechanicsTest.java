package com.omiinqa.ui.mechanics;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.theinternet.DynamicControlsPage;
import com.omiinqa.pages.theinternet.DynamicLoadingPage;
import com.omiinqa.pages.theinternet.FramesPage;
import com.omiinqa.pages.theinternet.MultipleWindowsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for browser window management, frame/iframe switching, dynamic
 * loading, and dynamic DOM controls on The-Internet practice site
 * ({@code https://the-internet.herokuapp.com}).
 *
 * <p>Each test method instantiates its own page objects so that test methods are
 * completely independent and safe to run in any order or in parallel.  No shared
 * mutable state exists at the class level.</p>
 *
 * <p>The class is organised into four logical sections:</p>
 * <ol>
 *   <li><b>Window mechanics</b> — opening, switching, and closing browser windows</li>
 *   <li><b>Frame / iframe mechanics</b> — nested frames and TinyMCE iframe switching</li>
 *   <li><b>Dynamic loading</b> — both examples that reveal hidden content asynchronously</li>
 *   <li><b>Dynamic controls</b> — asynchronous checkbox removal and input enable/disable</li>
 * </ol>
 *
 * <p>Assertions use AssertJ throughout; no assertion logic lives inside page objects.</p>
 */
@Epic("The-Internet")
@Feature("Window, Frame, and Dynamic Mechanics")
public class WindowFrameDynamicMechanicsTest extends BaseTest {

    // ====================================================================
    // Window mechanics
    // ====================================================================

    /**
     * Opening a new window from the Multiple Windows page must increase the
     * total number of driver-visible window handles from one to two.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Multiple Windows")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking 'Click Here' opens a second browser window; handle count must become 2.")
    public void openNewWindowIncreasesHandleCount() {
        final MultipleWindowsPage page = new MultipleWindowsPage().open();
        page.getMainWindowHandle(); // capture current handle (not yet used in assertion)
        page.clickOpenNewWindow();
        assertThat(page.getAllWindowHandles())
                .as("window handle count after opening new window")
                .hasSize(2);
    }

    /**
     * The newly opened window must contain the text "New Window" in its body,
     * confirming the correct page was loaded.  Focus is restored to the main
     * window before the test ends.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Multiple Windows")
    @Severity(SeverityLevel.NORMAL)
    @Description("Switching to the new window and reading its body text must contain 'New Window'.")
    public void newWindowHasCorrectTitle() {
        final MultipleWindowsPage page = new MultipleWindowsPage().open();
        final String mainHandle = page.getMainWindowHandle();

        page.clickOpenNewWindow();
        page.switchToWindowByIndex(1);

        final String newWindowBody = page.getPageBodyText();
        assertThat(newWindowBody)
                .as("body text of newly opened window")
                .containsIgnoringCase("New Window");

        page.closeCurrentWindow();
        page.switchToMainWindow(mainHandle);
    }

    /**
     * After closing the new window and switching back to the main window, only
     * one window handle must remain — confirming the test is back in the
     * original context.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Multiple Windows")
    @Severity(SeverityLevel.NORMAL)
    @Description("Closing the new window and switching back must leave exactly one handle open.")
    public void closingNewWindowReturnsFocus() {
        final MultipleWindowsPage page = new MultipleWindowsPage().open();
        final String mainHandle = page.getMainWindowHandle();

        page.clickOpenNewWindow();
        page.switchToWindowByIndex(1);
        page.closeCurrentWindow();
        page.switchToMainWindow(mainHandle);

        assertThat(page.getAllWindowHandles())
                .as("remaining window handles after closing the new window")
                .hasSize(1);
    }

    /**
     * Switching from the new window back to the main window must restore the
     * URL to the /windows page, proving that window-switch round-trips work
     * correctly.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Multiple Windows")
    @Severity(SeverityLevel.NORMAL)
    @Description("After switching to the new window and back, the current URL must contain '/windows'.")
    public void switchBetweenWindowsWorks() {
        final MultipleWindowsPage page = new MultipleWindowsPage().open();
        final String mainHandle = page.getMainWindowHandle();

        page.clickOpenNewWindow();
        page.switchToWindowByIndex(1);
        // We are now in the new window; switch back
        page.switchToMainWindow(mainHandle);

        assertThat(driver().getCurrentUrl())
                .as("URL after switching back to main window")
                .contains("/windows");
    }

    // ====================================================================
    // Frame / iframe mechanics
    // ====================================================================

    /**
     * Navigating to the Nested Frames page must result in a URL that contains
     * the "nested_frames" path segment.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Frames")
    @Severity(SeverityLevel.NORMAL)
    @Description("The Nested Frames page URL must contain 'nested_frames' after navigation.")
    public void nestedFramesPageLoads() {
        new FramesPage().openNestedFrames();
        assertThat(driver().getCurrentUrl())
                .as("URL of the Nested Frames page")
                .contains("nested_frames");
    }

    /**
     * After switching into the top-level frame named "frame-top", reading the
     * body text must return a non-empty string, confirming the frame context
     * switch succeeded.  The driver is returned to default content afterwards.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Frames")
    @Severity(SeverityLevel.NORMAL)
    @Description("Switching to 'frame-top' by name must yield non-empty body text.")
    public void switchToTopFrameAndReadContent() {
        final FramesPage frames = new FramesPage().openNestedFrames();

        frames.switchToFrameByNameOrId("frame-top");
        final String topBody = frames.getFrameBodyText();
        frames.switchToDefaultContent();

        assertThat(topBody)
                .as("body text read from frame-top")
                .isNotBlank();
    }

    /**
     * Within the nested frame hierarchy, switching first to "frame-top" and
     * then to the child frame "frame-left" must expose non-empty body content,
     * demonstrating correct two-level frame traversal.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Frames")
    @Severity(SeverityLevel.NORMAL)
    @Description("Switching into 'frame-top' then 'frame-left' must yield non-empty body text.")
    public void switchToLeftFrameInNested() {
        final FramesPage frames = new FramesPage().openNestedFrames();

        frames.switchToFrameByNameOrId("frame-top");
        frames.switchToFrameByNameOrId("frame-left");
        final String leftBody = frames.getFrameBodyText();
        frames.switchToDefaultContent();

        assertThat(leftBody)
                .as("body text read from nested frame-left")
                .isNotBlank();
    }

    /**
     * Navigating to the iFrame page must result in a URL that contains the
     * "/iframe" path segment.
     */
    @Test(groups = {"ui", "regression"})
    @Story("iFrame")
    @Severity(SeverityLevel.NORMAL)
    @Description("The iFrame page URL must contain '/iframe' after navigation.")
    public void iframePageLoads() {
        new FramesPage().openIframePage();
        assertThat(driver().getCurrentUrl())
                .as("URL of the iFrame / TinyMCE page")
                .contains("/iframe");
    }

    /**
     * After switching into the TinyMCE iframe, typing a text string via the
     * page method must not throw an exception, and the returned content from
     * the editor must contain the typed text.
     */
    @Test(groups = {"ui", "regression"})
    @Story("iFrame")
    @Severity(SeverityLevel.NORMAL)
    @Description("Typing text into TinyMCE must succeed and the editor content must reflect the input.")
    public void typeInTinyMceEditor() {
        final String expectedText = "Hello TinyMCE";
        final FramesPage frames = new FramesPage().openIframePage();

        frames.switchToIframeById(FramesPage.TINYMCE_IFRAME_ID);
        frames.typeInTinyMce(expectedText);
        final String editorContent = frames.getTinyMceContent();
        frames.switchToDefaultContent();

        assertThat(editorContent)
                .as("TinyMCE editor HTML content after typing")
                .containsIgnoringCase(expectedText);
    }

    /**
     * Switching to iframe index 0 on the iFrame page and then returning to
     * default content must complete without throwing any exception, proving
     * that index-based frame switching is functional.
     */
    @Test(groups = {"ui", "regression"})
    @Story("iFrame")
    @Severity(SeverityLevel.MINOR)
    @Description("Switching to iframe by index 0 and back to default must not throw any exception.")
    public void switchToIframeByIndexWorks() {
        final FramesPage frames = new FramesPage().openIframePage();

        frames.switchToFrameByIndex(0);
        frames.switchToDefaultContent();

        // If we reach this point without an exception, the switch succeeded.
        assertThat(driver().getCurrentUrl())
                .as("URL after switching back to default content")
                .contains("/iframe");
    }

    // ====================================================================
    // Dynamic loading
    // ====================================================================

    /**
     * Dynamic Loading Example 1 (element hidden before load): after clicking
     * Start and waiting for the finish element to appear, the displayed text
     * must be "Hello World!".
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Loading")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Example 1: waiting for finish must reveal the 'Hello World!' text.")
    public void dynamicExample1ShowsHelloWorld() {
        final DynamicLoadingPage page = new DynamicLoadingPage().open(1);
        page.clickStart().waitForFinish();
        assertThat(page.getFinishText())
                .as("finish element text for Dynamic Loading example 1")
                .containsIgnoringCase("Hello World");
    }

    /**
     * Dynamic Loading Example 2 (element rendered after load): after clicking
     * Start and waiting for the finish element to appear, the displayed text
     * must be "Hello World!".
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Loading")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Example 2: waiting for finish must reveal the 'Hello World!' text.")
    public void dynamicExample2ShowsHelloWorld() {
        final DynamicLoadingPage page = new DynamicLoadingPage().open(2);
        page.clickStart().waitForFinish();
        assertThat(page.getFinishText())
                .as("finish element text for Dynamic Loading example 2")
                .containsIgnoringCase("Hello World");
    }

    /**
     * After the Dynamic Loading Example 1 sequence completes, the loading
     * spinner must no longer be visible, confirming the spinner lifecycle is
     * correctly managed.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Loading")
    @Severity(SeverityLevel.NORMAL)
    @Description("Loading spinner must not be visible once the finish element is displayed.")
    public void loadingSpinnerDisappearsAfterLoad() {
        final DynamicLoadingPage page = new DynamicLoadingPage().open(1);
        page.clickStart().waitForFinish();
        assertThat(page.isLoadingSpinnerVisible())
                .as("loading spinner visibility after load completes")
                .isFalse();
    }

    /**
     * After completing the Dynamic Loading Example 2 sequence, the finish
     * element text must be a non-blank string, confirming that content was
     * injected into the DOM.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Loading")
    @Severity(SeverityLevel.NORMAL)
    @Description("Finish element text must not be blank after Dynamic Loading example 2 completes.")
    public void finishTextIsNotEmptyAfterLoad() {
        final DynamicLoadingPage page = new DynamicLoadingPage().open(2);
        page.clickStart().waitForFinish();
        assertThat(page.getFinishText())
                .as("finish element text for Dynamic Loading example 2")
                .isNotBlank();
    }

    // ====================================================================
    // Dynamic controls
    // ====================================================================

    /**
     * Clicking the Remove button on the Dynamic Controls page must remove the
     * checkbox from the DOM.  The status message must read "It's gone!" and
     * {@code isCheckboxPresent()} must return {@code false}.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Controls")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking Remove must remove the checkbox and display \"It's gone!\" status.")
    public void removeCheckboxWorks() {
        final DynamicControlsPage page = new DynamicControlsPage().open();

        assertThat(page.isCheckboxPresent())
                .as("checkbox must be present before clicking Remove")
                .isTrue();

        page.clickRemoveAddCheckboxButton().waitForCheckboxGone();

        assertThat(page.isCheckboxPresent())
                .as("checkbox must be absent after Remove completes")
                .isFalse();
        assertThat(page.getStatusMessage())
                .as("status message after removing checkbox")
                .containsIgnoringCase("It's gone!");
    }

    /**
     * Clicking the Enable button on the Dynamic Controls page must enable the
     * text input field.  The status message must contain "It's enabled!" and
     * {@code isInputEnabled()} must return {@code true}.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Controls")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking Enable must enable the input field and display \"It's enabled!\" status.")
    public void enableInputWorks() {
        final DynamicControlsPage page = new DynamicControlsPage().open();

        page.clickEnableDisableInputButton().waitForInputEnabled();

        assertThat(page.isInputEnabled())
                .as("input field must be enabled after clicking Enable")
                .isTrue();
        assertThat(page.getStatusMessage())
                .as("status message after enabling input")
                .containsIgnoringCase("It's enabled!");
    }

    /**
     * After enabling the input field, clicking the Disable button must disable
     * the field again.  The status message must contain "It's disabled!" and
     * {@code isInputEnabled()} must return {@code false}.
     */
    @Test(groups = {"ui", "regression"})
    @Story("Dynamic Controls")
    @Severity(SeverityLevel.NORMAL)
    @Description("After enabling, clicking Disable must disable the input and show \"It's disabled!\".")
    public void disableInputAfterEnable() {
        final DynamicControlsPage page = new DynamicControlsPage().open();

        // First enable the input
        page.clickEnableDisableInputButton().waitForInputEnabled();

        // Now disable it again
        page.clickEnableDisableInputButton()
                .waitForInputDisabled("It's disabled!");

        assertThat(page.isInputEnabled())
                .as("input field must be disabled after clicking Disable")
                .isFalse();
        assertThat(page.getStatusMessage())
                .as("status message after disabling input")
                .containsIgnoringCase("It's disabled!");
    }
}
