package com.omiinqa.core;

import com.omiinqa.driver.DriverManager;
import com.omiinqa.utils.JavaScriptUtils;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Abstract base for every page object and UI component.
 *
 * <p><b>Responsibilities (and only these):</b> safe, synchronized element
 * interaction — wait then act. It deliberately exposes <em>no assertions</em>;
 * verification belongs in tests and business flows, never in page objects.
 * This keeps page objects reusable across positive, negative and boundary
 * scenarios (Single Responsibility).</p>
 *
 * <p>Pages obtain the driver from {@link DriverManager} rather than receiving
 * it through constructors, so component composition stays terse while remaining
 * thread-safe.</p>
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    // --------------------------------------------------------------- actions

    protected void click(final By locator) {
        log.debug("Click {}", locator);
        WaitUtils.clickable(driver(), locator).click();
    }

    protected void jsClick(final By locator) {
        final WebElement element = WaitUtils.present(driver(), locator);
        JavaScriptUtils.scrollIntoView(driver(), element);
        JavaScriptUtils.click(driver(), element);
    }

    protected void type(final By locator, final String text) {
        log.debug("Type '{}' into {}", text, locator);
        final WebElement field = WaitUtils.visible(driver(), locator);
        field.clear();
        field.sendKeys(text);
    }

    protected String getText(final By locator) {
        return WaitUtils.visible(driver(), locator).getText().trim();
    }

    protected String getAttribute(final By locator, final String attribute) {
        return WaitUtils.present(driver(), locator).getAttribute(attribute);
    }

    protected List<WebElement> findAll(final By locator) {
        return WaitUtils.allVisible(driver(), locator);
    }

    protected void selectFromDropdownByVisibleText(final By locator, final String text) {
        new org.openqa.selenium.support.ui.Select(WaitUtils.visible(driver(), locator))
                .selectByVisibleText(text);
    }

    // --------------------------------------------------------------- queries

    protected boolean isDisplayed(final By locator) {
        return WaitUtils.isVisible(driver(), locator, Duration.ofSeconds(3));
    }

    protected boolean isDisplayed(final By locator, final Duration timeout) {
        return WaitUtils.isVisible(driver(), locator, timeout);
    }

    public String currentUrl() {
        return driver().getCurrentUrl();
    }

    public String pageTitle() {
        return driver().getTitle();
    }

    protected void waitForUrlContains(final String fragment) {
        WaitUtils.urlContains(driver(), fragment);
    }
}
