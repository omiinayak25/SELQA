package com.omiinqa.intelligence;

import com.omiinqa.intelligence.FailureCategorizer.FailureCategory;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Data-driven offline unit tests for {@link FailureCategorizer}.
 *
 * <p>Each test scenario exercises a specific exception type or message pattern
 * and asserts the expected {@link FailureCategory}. No WebDriver, no browser,
 * no network.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class FailureCategorizerTest {

    // =========================================================================
    // Type-based classification (exact exception type)
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void noSuchElementExceptionIsLocator() {
        assertEquals(
                FailureCategorizer.categorize(new NoSuchElementException("not found")),
                FailureCategory.LOCATOR);
    }

    @Test(groups = {"intelligence", "unit"})
    public void staleElementExceptionIsStaleElement() {
        assertEquals(
                FailureCategorizer.categorize(new StaleElementReferenceException("stale")),
                FailureCategory.STALE_ELEMENT);
    }

    @Test(groups = {"intelligence", "unit"})
    public void timeoutExceptionIsTimeout() {
        assertEquals(
                FailureCategorizer.categorize(new TimeoutException("timed out")),
                FailureCategory.TIMEOUT);
    }

    @Test(groups = {"intelligence", "unit"})
    public void socketTimeoutIsNetwork() {
        assertEquals(
                FailureCategorizer.categorize(new SocketTimeoutException("read timeout")),
                FailureCategory.NETWORK);
    }

    @Test(groups = {"intelligence", "unit"})
    public void connectExceptionIsNetwork() {
        assertEquals(
                FailureCategorizer.categorize(new ConnectException("connection refused")),
                FailureCategory.NETWORK);
    }

    @Test(groups = {"intelligence", "unit"})
    public void assertionErrorIsAssertion() {
        assertEquals(
                FailureCategorizer.categorize(new AssertionError("expected true but was false")),
                FailureCategory.ASSERTION);
    }

    @Test(groups = {"intelligence", "unit"})
    public void nullExceptionIsUnknown() {
        assertEquals(FailureCategorizer.categorize(null), FailureCategory.UNKNOWN);
    }

    // =========================================================================
    // Cause chain — category detected in wrapped cause
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void locatorCategoryDetectedInCauseChain() {
        final RuntimeException wrapper = new RuntimeException("step failed",
                new NoSuchElementException("button not found"));
        assertEquals(FailureCategorizer.categorize(wrapper), FailureCategory.LOCATOR);
    }

    @Test(groups = {"intelligence", "unit"})
    public void networkCategoryDetectedInCauseChain() {
        final RuntimeException wrapper = new RuntimeException("request failed",
                new ConnectException("connection refused"));
        assertEquals(FailureCategorizer.categorize(wrapper), FailureCategory.NETWORK);
    }

    // =========================================================================
    // Message-based classification (data-driven)
    // =========================================================================

    @DataProvider(name = "messageCategories")
    public Object[][] messageCategories() {
        return new Object[][]{
                // LOCATOR patterns
                {"no such element: unable to locate element", FailureCategory.LOCATOR},
                {"Cannot locate element with By.cssSelector", FailureCategory.LOCATOR},
                {"element not found on page",                 FailureCategory.LOCATOR},

                // STALE (must come before LOCATOR in pattern priority)
                {"stale element reference exception",         FailureCategory.STALE_ELEMENT},
                {"element has been detached from the DOM",    FailureCategory.STALE_ELEMENT},

                // TIMEOUT patterns
                {"Timed out after 10 seconds waiting for element", FailureCategory.TIMEOUT},
                {"Expected condition was not met: timeout",         FailureCategory.TIMEOUT},
                {"Fluent wait condition did not complete",          FailureCategory.TIMEOUT},

                // ASSERTION patterns
                {"AssertionError: expected: [true] but was: [false]",  FailureCategory.ASSERTION},
                {"expected [hello] to contain [world]",                FailureCategory.ASSERTION},

                // NETWORK patterns
                {"Connection refused: no route to host",           FailureCategory.NETWORK},
                {"HTTP 503 Service Unavailable",                   FailureCategory.NETWORK},
                {"Broken pipe while reading response",             FailureCategory.NETWORK},

                // DRIVER patterns
                {"invalid session id: webdriver session expired",  FailureCategory.DRIVER},
                {"unknown error chromedriver crashed",             FailureCategory.DRIVER},

                // DATA patterns
                {"DataException: could not parse CSV row",         FailureCategory.DATA},
                {"data provider threw an exception",               FailureCategory.DATA},

                // UNKNOWN
                {"something completely unrelated happened",        FailureCategory.UNKNOWN},
                {"",                                               FailureCategory.UNKNOWN},
        };
    }

    @Test(groups = {"intelligence", "unit"}, dataProvider = "messageCategories")
    public void categorizeMessageMatchesExpected(final String message,
                                                  final FailureCategory expected) {
        final FailureCategory actual = FailureCategorizer.categorizeMessage(message);
        assertEquals(actual, expected,
                "Message '" + message + "' → expected " + expected + " but got " + actual);
    }

    @Test(groups = {"intelligence", "unit"})
    public void categorizeMessageNull() {
        assertEquals(FailureCategorizer.categorizeMessage(null), FailureCategory.UNKNOWN);
    }

    // =========================================================================
    // Enum sanity
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void allCategoryValuesAreNonNull() {
        for (final FailureCategory cat : FailureCategory.values()) {
            assertNotNull(cat);
        }
    }

    @Test(groups = {"intelligence", "unit"})
    public void categorizeAlwaysReturnsNonNull() {
        assertNotNull(FailureCategorizer.categorize(new RuntimeException("anything")));
        assertNotNull(FailureCategorizer.categorize(null));
    }
}
