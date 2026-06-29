package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Generic modal-dialog component for bootstrap-style or custom modal overlays.
 *
 * <p><b>Composition rationale:</b> Modal dialogs share the same structural
 * contract (title, body, confirm/cancel buttons, close-X) across application
 * pages. Centralising this in a {@link BaseComponent} prevents each page from
 * re-implementing modal interaction. Pages pass the modal root element at
 * construction time; this component's locators resolve relative to that root
 * so they never match background page content.</p>
 */
public class ModalComponent extends BaseComponent {

    private static final By TITLE_TEXT   = By.cssSelector(".modal-title, [role='dialog'] h2, h1.modal-header");
    private static final By BODY_TEXT    = By.cssSelector(".modal-body, [role='dialog'] p");
    private static final By CONFIRM_BTN  = By.cssSelector(".modal-footer .btn-primary, button[data-testid='confirm']");
    private static final By CANCEL_BTN   = By.cssSelector(".modal-footer .btn-secondary, button[data-testid='cancel']");
    private static final By CLOSE_BTN    = By.cssSelector(".close, button[aria-label='Close'], [data-dismiss='modal']");

    /**
     * @param root the outermost element of the modal dialog
     */
    public ModalComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the modal's title text, trimmed
     */
    public String getTitle() {
        return findInRoot(TITLE_TEXT).getText().trim();
    }

    /**
     * @return the modal's body text, trimmed
     */
    public String getBodyText() {
        return findInRoot(BODY_TEXT).getText().trim();
    }

    /**
     * @return {@code true} when the confirm/primary button is present
     */
    public boolean hasConfirmButton() {
        return existsInRoot(CONFIRM_BTN);
    }

    /**
     * @return {@code true} when the cancel/secondary button is present
     */
    public boolean hasCancelButton() {
        return existsInRoot(CANCEL_BTN);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Clicks the primary action (confirm/OK) button.
     */
    public void confirm() {
        log.debug("Clicking modal confirm button");
        findInRoot(CONFIRM_BTN).click();
    }

    /**
     * Clicks the secondary (cancel) button.
     */
    public void cancel() {
        log.debug("Clicking modal cancel button");
        findInRoot(CANCEL_BTN).click();
    }

    /**
     * Clicks the «X» close control without confirming or cancelling.
     */
    public void close() {
        log.debug("Clicking modal close (X) button");
        findInRoot(CLOSE_BTN).click();
    }
}
