package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.JavaScriptUtils;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

import java.util.Set;

/**
 * Page Object for verifying browser cookie and Web Storage mechanics on
 * The-Internet ({@code https://the-internet.herokuapp.com}).
 *
 * <p>Browser cookies are read via {@code driver().manage().getCookies()} and
 * manipulated through the {@link org.openqa.selenium.WebDriver.Options} interface.
 * {@code localStorage} and {@code sessionStorage} are accessed through
 * {@link JavaScriptUtils#execute(org.openqa.selenium.WebDriver, String, Object...)}
 * because WebDriver does not expose a direct Storage API — JavaScript is the
 * correct and only way to reach them.</p>
 *
 * <p><b>Page Object Model contract:</b> No assertions. All state is returned to
 * the caller. Locators are {@code private static final} {@link By} fields.</p>
 */
public class CookiesAndStoragePage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By USERNAME_INPUT = By.id("username");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON   = By.cssSelector("button[type='submit']");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates to the home page of The-Internet application.
     *
     * @return this page for method chaining
     */
    public CookiesAndStoragePage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet");
        log.info("Opening The-Internet home: {}", url);
        driver().get(url);
        return this;
    }

    /**
     * Navigates to {@code /login}, authenticates with the standard credentials
     * ({@code tomsmith} / {@code SuperSecretPassword!}), and waits for the
     * post-login redirect so that session cookies are set.
     *
     * @return this page for method chaining
     */
    public CookiesAndStoragePage openLoginAndAuthenticate() {
        final String loginUrl = FrameworkConfig.get().appUrl("theinternet") + "/login";
        log.info("Navigating to login and authenticating: {}", loginUrl);
        driver().get(loginUrl);
        WaitUtils.visible(driver(), USERNAME_INPUT).sendKeys("tomsmith");
        WaitUtils.visible(driver(), PASSWORD_INPUT).sendKeys("SuperSecretPassword!");
        click(LOGIN_BUTTON);
        waitForUrlContains("/secure");
        return this;
    }

    /**
     * Adds a cookie with the given name and value to the current browser session.
     *
     * @param name  the cookie name
     * @param value the cookie value
     */
    public void addCookie(final String name, final String value) {
        log.info("Adding cookie: {}={}", name, value);
        driver().manage().addCookie(new Cookie(name, value));
    }

    /**
     * Removes the cookie identified by name from the current browser session.
     *
     * @param name the name of the cookie to delete
     */
    public void deleteCookieByName(final String name) {
        log.info("Deleting cookie: {}", name);
        driver().manage().deleteCookieNamed(name);
    }

    /**
     * Sets a value in {@code localStorage} under the given key.
     *
     * @param key   the storage key
     * @param value the value to store
     */
    public void setLocalStorageItem(final String key, final String value) {
        log.info("Setting localStorage[{}] = {}", key, value);
        JavaScriptUtils.execute(driver(),
                "localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    /**
     * Removes all entries from {@code localStorage}.
     */
    public void clearLocalStorage() {
        log.info("Clearing localStorage");
        JavaScriptUtils.execute(driver(), "localStorage.clear();");
    }

    /**
     * Removes all entries from {@code sessionStorage}.
     */
    public void clearSessionStorage() {
        log.info("Clearing sessionStorage");
        JavaScriptUtils.execute(driver(), "sessionStorage.clear();");
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the complete set of cookies currently available for the browser
     * session and domain.
     *
     * @return an unmodifiable set of {@link Cookie} objects; may be empty but
     *         never {@code null}
     */
    public Set<Cookie> getAllCookies() {
        return driver().manage().getCookies();
    }

    /**
     * Returns the cookie with the given name, or {@code null} when absent.
     *
     * @param name the cookie name to look up
     * @return the {@link Cookie}, or {@code null}
     */
    public Cookie getCookieByName(final String name) {
        return driver().manage().getCookieNamed(name);
    }

    /**
     * Retrieves a value from {@code localStorage} by key.
     *
     * @param key the storage key
     * @return the stored value string, or {@code null} when the key does not exist
     */
    public String getLocalStorageItem(final String key) {
        final Object result = JavaScriptUtils.execute(driver(),
                "return localStorage.getItem(arguments[0]);", key);
        return result != null ? String.valueOf(result) : null;
    }

    /**
     * Retrieves a value from {@code sessionStorage} by key.
     *
     * @param key the storage key
     * @return the stored value string, or {@code null} when the key does not exist
     */
    public String getSessionStorageItem(final String key) {
        final Object result = JavaScriptUtils.execute(driver(),
                "return sessionStorage.getItem(arguments[0]);", key);
        return result != null ? String.valueOf(result) : null;
    }
}
