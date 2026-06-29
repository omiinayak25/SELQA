package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the HTTP authentication exercises on The-Internet:
 * Basic Auth ({@code /basic_auth}) and Digest Auth ({@code /digest_auth}).
 *
 * <p>Both authentication schemes are handled by embedding the credentials
 * directly in the URL (e.g. {@code https://admin:admin@host/basic_auth}),
 * which Selenium-controlled Chromium and Firefox honour without requiring
 * a separate browser extension or proxy. The credentials for these demo
 * pages are {@code admin / admin}.</p>
 *
 * <p>The base URL is read from {@link FrameworkConfig#appUrl(String)} with
 * the key {@code "theinternet"} and then modified to inject credentials
 * immediately after the scheme prefix ({@code "https://"}).</p>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — state is surfaced through query methods so this class
 *       is reusable across positive, negative, and boundary test cases.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 * </ul>
 */
public class BasicAuthPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /**
     * Paragraph element inside {@code .example} that contains the
     * «Congratulations! You must have the proper credentials.» success message.
     */
    private static final By SUCCESS_MESSAGE = By.cssSelector(".example p");

    /** Generic paragraph element used for a broader body-text read. */
    private static final By BODY_PARAGRAPH = By.tagName("p");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates to the Basic Authentication page by constructing a URL with
     * embedded credentials.
     *
     * <p>The base URL ({@code https://the-internet.herokuapp.com}) is read from
     * configuration; the scheme prefix is replaced to produce
     * {@code https://admin:admin@the-internet.herokuapp.com/basic_auth}.</p>
     *
     * @return this {@link BasicAuthPage} for method chaining
     */
    public BasicAuthPage openBasicAuth() {
        final String url = buildAuthUrl("admin", "admin", "/basic_auth");
        log.info("Opening Basic Auth page with embedded credentials: {}", url);
        driver().get(url);
        waitForUrlContains("/basic_auth");
        return this;
    }

    /**
     * Navigates to the Digest Authentication page by constructing a URL with
     * embedded credentials.
     *
     * <p>The base URL is read from configuration and modified identically to
     * {@link #openBasicAuth()}, producing
     * {@code https://admin:admin@the-internet.herokuapp.com/digest_auth}.</p>
     *
     * @return this {@link BasicAuthPage} for method chaining
     */
    public BasicAuthPage openDigestAuth() {
        final String url = buildAuthUrl("admin", "admin", "/digest_auth");
        log.info("Opening Digest Auth page with embedded credentials: {}", url);
        driver().get(url);
        waitForUrlContains("/digest_auth");
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns {@code true} when the {@code .example p} success message is
     * visible in the viewport after authentication.
     *
     * <p>Uses a short 3-second probe window so tests do not wait excessively
     * when the page is not authenticated.</p>
     *
     * @return {@code true} if the success message element is visible,
     *         {@code false} otherwise
     */
    public boolean isSuccessMessageDisplayed() {
        return isDisplayed(SUCCESS_MESSAGE);
    }

    /**
     * Returns the trimmed text content of the first {@code <p>} element in the
     * document body.
     *
     * <p>On a successfully authenticated page this typically contains the
     * congratulatory message. On a failed authentication the browser renders its
     * own error page, in which case this method may still return text from that
     * browser page.</p>
     *
     * @return trimmed paragraph text; never {@code null}
     */
    public String getBodyText() {
        return getText(BODY_PARAGRAPH);
    }

    // --------------------------------------------------------------- internals

    /**
     * Constructs an authentication URL by injecting {@code username:password@}
     * between the HTTPS scheme and the rest of the configured base URL.
     *
     * @param username the HTTP auth username
     * @param password the HTTP auth password
     * @param path     the path to append (must start with {@code /})
     * @return the fully qualified URL with embedded credentials
     */
    private String buildAuthUrl(final String username,
                                final String password,
                                final String path) {
        final String baseUrl = FrameworkConfig.get().appUrl("theinternet");
        final String authedBase = baseUrl.replace(
                "https://",
                "https://" + username + ":" + password + "@");
        return authedBase + path;
    }
}
