package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;

/**
 * Page object for the The-Internet context menu feature page at {@code /context_menu}.
 *
 * <p>Demonstrates right-click (context menu) interaction via
 * {@link Actions#contextClick(org.openqa.selenium.WebElement)} and
 * native browser alert handling. The page exposes a hot-spot element that,
 * when right-clicked, shows a JavaScript alert with a predefined message.</p>
 *
 * <p>No assertions are made here; tests query {@link #getAlertText()} and
 * {@link #acceptAlert()} to verify the alert content independently.</p>
 */
public class ContextMenuPage extends BasePage {

    private static final By HOT_SPOT = By.cssSelector("#hot-spot");

    /**
     * Navigates to {@code /context_menu} using the configured base URL.
     *
     * @return this page (fluent/self-return)
     */
    public ContextMenuPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/context_menu";
        log.info("Opening context menu page: {}", url);
        driver().get(url);
        WaitUtils.visible(driver(), HOT_SPOT);
        return this;
    }

    /**
     * Right-clicks on the hot-spot element to trigger the context menu,
     * which immediately raises a JavaScript {@code alert} dialog.
     *
     * <p>Call {@link #getAlertText()} or {@link #acceptAlert()} immediately
     * after to interact with the resulting alert before it times out.</p>
     */
    public void rightClickHotSpot() {
        log.info("Right-clicking the hot-spot to trigger context menu alert");
        new Actions(driver())
                .contextClick(WaitUtils.visible(driver(), HOT_SPOT))
                .perform();
    }

    /**
     * Returns the text of the currently active browser alert without dismissing it.
     *
     * @return the alert message string
     */
    public String getAlertText() {
        return driver().switchTo().alert().getText();
    }

    /**
     * Accepts (clicks OK on) the currently active browser alert.
     */
    public void acceptAlert() {
        log.debug("Accepting context menu alert");
        driver().switchTo().alert().accept();
    }
}
