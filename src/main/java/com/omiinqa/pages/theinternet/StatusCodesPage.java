package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;

/**
 * Page Object for the The-Internet status codes page at {@code /status_codes}
 * and the redirect example at {@code /redirector}.
 *
 * <p>The status codes page presents links for HTTP codes 200, 301, 404, and 500.
 * Clicking any link navigates to a sub-page that explains the code and provides
 * a "here" link to return. This class also covers the {@code /redirector} page
 * where clicking a button performs a client-side redirect to a different URL.</p>
 *
 * <p><b>Page Object Model contract:</b> No assertions. Locators are
 * {@code private static final} {@link By} fields.</p>
 */
public class StatusCodesPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By PAGE_TEXT    = By.tagName("p");
    private static final By REDIRECT_BTN = By.cssSelector("#redirect");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates to {@code /status_codes} using the configured base URL.
     *
     * @return this page for method chaining
     */
    public StatusCodesPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/status_codes";
        log.info("Opening Status Codes page: {}", url);
        driver().get(url);
        waitForUrlContains("/status_codes");
        return this;
    }

    /**
     * Navigates to {@code /redirector} using the configured base URL.
     *
     * @return this page for method chaining
     */
    public StatusCodesPage openRedirector() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/redirector";
        log.info("Opening Redirector page: {}", url);
        driver().get(url);
        waitForUrlContains("/redirector");
        return this;
    }

    /**
     * Clicks the link on the status codes page whose anchor text exactly matches
     * the string representation of the given HTTP status code.
     *
     * @param code the HTTP status code to navigate to (e.g. {@code 200}, {@code 301},
     *             {@code 404}, {@code 500})
     */
    public void clickStatusCode(final int code) {
        log.info("Clicking status code link for: {}", code);
        final By codeLink = By.linkText(String.valueOf(code));
        click(codeLink);
    }

    /**
     * Clicks the redirect trigger element ({@code #redirect}) on the redirector page,
     * causing the browser to navigate to the redirect target URL.
     */
    public void clickRedirectLink() {
        log.info("Clicking redirect link on /redirector");
        click(REDIRECT_BTN);
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text of the first {@code <p>} element on the current page.
     *
     * @return paragraph text explaining the current HTTP status code
     */
    public String getPageText() {
        return getText(PAGE_TEXT);
    }

    /**
     * Returns {@code true} when the current URL contains the string representation
     * of the given HTTP status code, confirming navigation to the code's sub-page.
     *
     * @param code HTTP status code to verify (e.g. {@code 200})
     * @return {@code true} if the current URL contains the code
     */
    public boolean isOnStatusPage(final int code) {
        return currentUrl().contains(String.valueOf(code));
    }

    /**
     * Extracts the URL path segment that follows the configured base URL.
     *
     * @return the path portion of the current URL (e.g. {@code "/status_codes/200"})
     */
    public String getCurrentPath() {
        final String base = FrameworkConfig.get().appUrl("theinternet");
        final String current = currentUrl();
        return current.startsWith(base) ? current.substring(base.length()) : current;
    }
}
