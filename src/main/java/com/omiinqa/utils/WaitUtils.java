package com.omiinqa.utils;

import com.omiinqa.config.FrameworkConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.time.Duration;
import java.util.List;

/**
 * Centralized explicit-wait helpers built on {@link FluentWait}.
 *
 * <p>Implicit waits are deliberately kept at zero (see {@code config}) — mixing
 * implicit and explicit waits in Selenium produces unpredictable, compounding
 * timeouts. All synchronization flows through here so polling, timeout and
 * ignored-exception policy live in exactly one place (DRY).</p>
 */
public final class WaitUtils {

    private WaitUtils() {
    }

    private static Wait<WebDriver> fluent(final WebDriver driver, final Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(FrameworkConfig.get().pollingInterval())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    private static Duration defaultTimeout() {
        return FrameworkConfig.get().explicitTimeout();
    }

    public static <T> T until(final WebDriver driver, final ExpectedCondition<T> condition) {
        return until(driver, condition, defaultTimeout());
    }

    public static <T> T until(final WebDriver driver,
                              final ExpectedCondition<T> condition,
                              final Duration timeout) {
        return fluent(driver, timeout).until(condition);
    }

    public static WebElement visible(final WebDriver driver, final By locator) {
        return until(driver, ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement visible(final WebDriver driver, final WebElement element) {
        return until(driver, ExpectedConditions.visibilityOf(element));
    }

    public static WebElement clickable(final WebDriver driver, final By locator) {
        return until(driver, ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement clickable(final WebDriver driver, final WebElement element) {
        return until(driver, ExpectedConditions.elementToBeClickable(element));
    }

    public static WebElement present(final WebDriver driver, final By locator) {
        return until(driver, ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static List<WebElement> allVisible(final WebDriver driver, final By locator) {
        return until(driver, ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean invisible(final WebDriver driver, final By locator) {
        return until(driver, ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean textPresent(final WebDriver driver, final By locator, final String text) {
        return until(driver, ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public static boolean urlContains(final WebDriver driver, final String fragment) {
        return until(driver, ExpectedConditions.urlContains(fragment));
    }

    /** Non-throwing visibility probe — for optional elements / branching. */
    public static boolean isVisible(final WebDriver driver, final By locator, final Duration timeout) {
        try {
            until(driver, ExpectedConditions.visibilityOfElementLocated(locator), timeout);
            return true;
        } catch (final TimeoutException e) {
            return false;
        }
    }
}
