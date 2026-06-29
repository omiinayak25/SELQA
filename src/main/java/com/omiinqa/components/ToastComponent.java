package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Generic toast / snackbar notification component.
 *
 * <p><b>Composition rationale:</b> Toast messages appear on many pages in
 * response to user actions (form submission, item deletion, etc.). Centralising
 * their location and retrieval in a {@link BaseComponent} lets tests verify
 * notifications without each page duplicating locators. Because toasts are
 * ephemeral, this component exposes a {@link #waitForMessage(Duration)} helper
 * that tolerates the transient nature of the element.</p>
 */
public class ToastComponent extends BaseComponent {

    private static final By TOAST_CONTAINER = By.cssSelector(".toast, .alert, [role='alert'], .notification");
    private static final By TOAST_MESSAGE   = By.cssSelector(".toast-body, .toast-message, .alert-message, [role='alert']");
    private static final By CLOSE_BTN       = By.cssSelector(".toast .close, .alert .close");

    /**
     * @param root the element that contains or is the toast container
     */
    public ToastComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the visible toast message text, trimmed
     */
    public String getMessage() {
        return existsInRoot(TOAST_MESSAGE)
                ? findInRoot(TOAST_MESSAGE).getText().trim()
                : root.getText().trim();
    }

    /**
     * @return {@code true} when any toast container element is currently visible
     */
    @Override
    public boolean isLoaded() {
        return root != null && root.isDisplayed();
    }

    // ----------------------------------------------------------------- actions

    /**
     * Waits up to {@code timeout} for the toast to become visible, then returns
     * its message text. Useful for asserting on async notifications.
     *
     * @param timeout maximum time to wait
     * @return the toast message text
     */
    public String waitForMessage(final Duration timeout) {
        WaitUtils.visible(driver(), root);
        return getMessage();
    }

    /**
     * Dismisses the toast by clicking its close button, if present.
     */
    public void dismiss() {
        if (existsInRoot(CLOSE_BTN)) {
            log.debug("Dismissing toast notification");
            findInRoot(CLOSE_BTN).click();
        }
    }
}
