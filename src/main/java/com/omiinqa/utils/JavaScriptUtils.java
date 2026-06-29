package com.omiinqa.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Thin wrapper over {@link JavascriptExecutor} for the handful of actions that
 * legitimately need JS (scrolling into view, robust clicks on flaky overlays,
 * reading page performance timings). Kept narrow on purpose — JS interaction
 * should be the exception, not the default, or tests stop reflecting real users.
 */
public final class JavaScriptUtils {

    private JavaScriptUtils() {
    }

    private static JavascriptExecutor js(final WebDriver driver) {
        return (JavascriptExecutor) driver;
    }

    public static Object execute(final WebDriver driver, final String script, final Object... args) {
        return js(driver).executeScript(script, args);
    }

    public static void scrollIntoView(final WebDriver driver, final WebElement element) {
        js(driver).executeScript(
                "arguments[0].scrollIntoView({block:'center',inline:'center'});", element);
    }

    public static void click(final WebDriver driver, final WebElement element) {
        js(driver).executeScript("arguments[0].click();", element);
    }

    public static void scrollToBottom(final WebDriver driver) {
        js(driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    public static String documentReadyState(final WebDriver driver) {
        return String.valueOf(js(driver).executeScript("return document.readyState;"));
    }

    /** Navigation timing in milliseconds — used by the performance smoke layer. */
    public static long pageLoadTimeMillis(final WebDriver driver) {
        final Object value = js(driver).executeScript(
                "return (window.performance.timing.loadEventEnd"
                        + " - window.performance.timing.navigationStart);");
        return value instanceof Number number ? number.longValue() : -1L;
    }
}
