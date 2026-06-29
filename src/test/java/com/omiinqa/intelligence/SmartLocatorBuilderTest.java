package com.omiinqa.intelligence;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Offline unit tests for {@link SmartLocator} and its {@link SmartLocator.Builder}.
 *
 * <p><b>No real WebDriver is instantiated.</b> A minimal inline fake
 * {@link WebDriver} implementation is used to simulate locator resolution
 * behaviour without any browser or Selenium infrastructure dependency.</p>
 *
 * <p>A {@link Duration#ofMillis(100)} probe timeout is set on every
 * {@code SmartLocator} under test so that simulated failures do not cause
 * multi-second waits per candidate.</p>
 *
 * <p>All tests are in groups {@code intelligence} and {@code unit} and run
 * entirely offline.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class SmartLocatorBuilderTest {

    /**
     * Short probe timeout used in all test locators to keep the suite fast.
     * Failures resolve in ≤100 ms instead of the production default of 2 s.
     */
    private static final Duration TEST_PROBE = Duration.ofMillis(100);

    /** A minimal no-op WebElement stub. */
    private static final WebElement STUB_ELEMENT = new StubWebElement();

    /**
     * Creates a {@link WebDriver} whose {@code findElement} only resolves the
     * listed {@link By} instances; everything else throws
     * {@link NoSuchElementException}.
     */
    private static WebDriver driverThatFinds(final By... resolvable) {
        return new ControlledDriver(resolvable);
    }

    // =========================================================================
    // Builder tests
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void builderStoresPrimary() {
        final By primary = By.id("submit");
        final SmartLocator loc = SmartLocator.primary(primary).build();
        assertEquals(loc.getPrimary(), primary);
    }

    @Test(groups = {"intelligence", "unit"})
    public void builderStoresFallbacksInOrder() {
        final By b1 = By.name("submit");
        final By b2 = By.cssSelector(".btn");
        final By b3 = By.xpath("//button");

        final SmartLocator loc = SmartLocator.primary(By.id("x"))
                .orElse(b1).orElse(b2).orElse(b3).build();

        final List<By> fallbacks = loc.getFallbacks();
        assertEquals(fallbacks.size(), 3);
        assertEquals(fallbacks.get(0), b1);
        assertEquals(fallbacks.get(1), b2);
        assertEquals(fallbacks.get(2), b3);
    }

    @Test(groups = {"intelligence", "unit"})
    public void builderWithNoFallbacksProducesEmptyList() {
        final SmartLocator loc = SmartLocator.primary(By.id("solo")).build();
        assertTrue(loc.getFallbacks().isEmpty());
    }

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = NullPointerException.class)
    public void builderRejectsNullPrimary() {
        SmartLocator.primary(null);
    }

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = NullPointerException.class)
    public void builderRejectsNullFallback() {
        SmartLocator.primary(By.id("x")).orElse(null);
    }

    @Test(groups = {"intelligence", "unit"})
    public void builderFallbackListIsImmutable() {
        final SmartLocator loc = SmartLocator.primary(By.id("x"))
                .orElse(By.name("y")).build();
        assertThrows(UnsupportedOperationException.class,
                () -> loc.getFallbacks().add(By.id("z")));
    }

    // =========================================================================
    // Initial state (no resolve called)
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void initiallyNotHealed() {
        final SmartLocator loc = SmartLocator.primary(By.id("a"))
                .orElse(By.name("b")).build();
        assertFalse(loc.isHealed(), "Fresh locator must not report healed");
    }

    @Test(groups = {"intelligence", "unit"})
    public void activeLocatorReturnsPrimaryInitially() {
        final By primary = By.id("foo");
        final SmartLocator loc = SmartLocator.primary(primary).build();
        assertEquals(loc.activeLocator(), primary);
    }

    // =========================================================================
    // Resolution — primary succeeds
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void resolvePrimarySucceeds() {
        final By primary = By.id("login");
        final WebDriver driver = driverThatFinds(primary);

        final SmartLocator loc = SmartLocator.primary(primary)
                .orElse(By.name("login"))
                .withProbeTimeout(TEST_PROBE)
                .build();

        final WebElement el = loc.resolve(driver);
        assertNotNull(el);
        assertFalse(loc.isHealed(), "Should not be healed when primary works");
        assertEquals(loc.activeLocator(), primary);
    }

    // =========================================================================
    // Resolution — fallback heals
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void resolveHealsWithFirstFallback() {
        final By primary  = By.id("old-btn");
        final By fallback = By.name("submit");

        final WebDriver driver = driverThatFinds(fallback); // primary fails

        final SmartLocator loc = SmartLocator.primary(primary)
                .orElse(fallback)
                .withProbeTimeout(TEST_PROBE)
                .build();

        final WebElement el = loc.resolve(driver);
        assertNotNull(el);
        assertTrue(loc.isHealed());
        assertEquals(loc.activeLocator(), fallback);
    }

    @Test(groups = {"intelligence", "unit"})
    public void resolveHealsWithSecondFallbackWhenFirstAlsoFails() {
        final By primary   = By.id("gone");
        final By fallback1 = By.name("also-gone");
        final By fallback2 = By.cssSelector(".still-here");

        final WebDriver driver = driverThatFinds(fallback2);

        final SmartLocator loc = SmartLocator.primary(primary)
                .orElse(fallback1)
                .orElse(fallback2)
                .withProbeTimeout(TEST_PROBE)
                .build();

        assertNotNull(loc.resolve(driver));
        assertEquals(loc.activeLocator(), fallback2);
    }

    // =========================================================================
    // Resolution — cache usage
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void healedStatePersistedAfterSecondCall() {
        final By primary  = By.id("stale");
        final By fallback = By.name("current");

        final WebDriver driver = driverThatFinds(fallback);

        final SmartLocator loc = SmartLocator.primary(primary)
                .orElse(fallback)
                .withProbeTimeout(TEST_PROBE)
                .build();

        loc.resolve(driver); // first call — heals
        assertTrue(loc.isHealed(), "Must still report healed after second call");

        loc.resolve(driver); // second call — uses cache
        assertTrue(loc.isHealed(), "Must still report healed after second call");
        assertEquals(loc.activeLocator(), fallback);
    }

    @Test(groups = {"intelligence", "unit"})
    public void resetCacheForcesReheal() {
        final By primary  = By.id("broken");
        final By fallback = By.name("working");

        final WebDriver driver = driverThatFinds(fallback);

        final SmartLocator loc = SmartLocator.primary(primary)
                .orElse(fallback)
                .withProbeTimeout(TEST_PROBE)
                .build();

        loc.resolve(driver); // heals
        assertTrue(loc.isHealed());

        loc.resetCache();
        assertFalse(loc.isHealed());
        assertEquals(loc.activeLocator(), primary); // back to primary
    }

    // =========================================================================
    // Exhaustion — all candidates fail → NoSuchElementException with message
    // =========================================================================

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = NoSuchElementException.class,
          expectedExceptionsMessageRegExp = ".*SmartLocator exhausted.*")
    public void exhaustionThrowsDescriptiveError() {
        final WebDriver driver = driverThatFinds(); // nothing resolves

        final SmartLocator loc = SmartLocator.primary(By.id("a"))
                .orElse(By.name("b"))
                .orElse(By.cssSelector(".c"))
                .withProbeTimeout(TEST_PROBE)
                .build();

        loc.resolve(driver);
    }

    @Test(groups = {"intelligence", "unit"})
    public void exhaustionMessageListsAllTriedLocators() {
        final WebDriver driver = driverThatFinds();

        final SmartLocator loc = SmartLocator.primary(By.id("one"))
                .orElse(By.name("two"))
                .withProbeTimeout(TEST_PROBE)
                .build();

        try {
            loc.resolve(driver);
            fail("Should have thrown");
        } catch (final NoSuchElementException e) {
            assertTrue(e.getMessage().contains("one") || e.getMessage().contains("By.id"),
                    "Message should mention primary locator");
        }
    }

    @Test(groups = {"intelligence", "unit"})
    public void exhaustionMessageContainsFallbackInfo() {
        final WebDriver driver = driverThatFinds();

        final SmartLocator loc = SmartLocator.primary(By.id("p"))
                .orElse(By.name("f1"))
                .withProbeTimeout(TEST_PROBE)
                .build();

        try {
            loc.resolve(driver);
            fail("Should have thrown");
        } catch (final NoSuchElementException e) {
            // The helpful message should list attempted locators
            assertTrue(e.getMessage().contains("[1]") || e.getMessage().contains("[2]"),
                    "Exhaustion message must list tried locators with ordinals");
        }
    }

    // =========================================================================
    // Null driver
    // =========================================================================

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = NullPointerException.class)
    public void resolveRejectsNullDriver() {
        SmartLocator.primary(By.id("x")).build().resolve(null);
    }

    // =========================================================================
    // withProbeTimeout builder method
    // =========================================================================

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = NullPointerException.class)
    public void withProbeTimeoutRejectsNull() {
        SmartLocator.primary(By.id("x")).withProbeTimeout(null).build();
    }

    // =========================================================================
    // Fake driver implementations (static inner classes — no external deps)
    // =========================================================================

    /**
     * A WebDriver that only resolves a fixed set of {@link By} locators.
     * All other {@code findElement} calls throw {@link NoSuchElementException}.
     */
    private static final class ControlledDriver extends AbstractFakeDriver {

        private final Set<By> resolvable;

        ControlledDriver(final By... allowed) {
            this.resolvable = new HashSet<>(Arrays.asList(allowed));
        }

        @Override
        public WebElement findElement(final By by) {
            if (resolvable.contains(by)) {
                return STUB_ELEMENT;
            }
            throw new NoSuchElementException("Controlled driver: not in resolvable set: " + by);
        }

        @Override
        public List<WebElement> findElements(final By by) {
            if (resolvable.contains(by)) {
                return Collections.singletonList(STUB_ELEMENT);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Abstract base for fake drivers — stubs out every WebDriver method we
     * don't need, so concrete fakes only override {@code findElement/s}.
     */
    private abstract static class AbstractFakeDriver implements WebDriver {
        @Override public void get(final String url) {}
        @Override public String getCurrentUrl() { return "about:blank"; }
        @Override public String getTitle() { return ""; }
        @Override public String getPageSource() { return ""; }
        @Override public void close() {}
        @Override public void quit() {}
        @Override public java.util.Set<String> getWindowHandles() { return Collections.emptySet(); }
        @Override public String getWindowHandle() { return "fake-handle"; }
        @Override public TargetLocator switchTo() { return null; }
        @Override public Navigation navigate() { return null; }
        @Override public Options manage() { return null; }
    }

    /**
     * Minimal no-op {@link WebElement} stub — {@code isDisplayed()} and
     * {@code isEnabled()} return {@code true} so that WaitUtils visibility
     * conditions pass against this stub.
     */
    private static final class StubWebElement implements WebElement {
        @Override public void click() {}
        @Override public void submit() {}
        @Override public void sendKeys(final CharSequence... keysToSend) {}
        @Override public void clear() {}
        @Override public String getTagName() { return "div"; }
        @Override public String getAttribute(final String name) { return ""; }
        @Override public boolean isSelected() { return false; }
        @Override public boolean isEnabled() { return true; }
        @Override public String getText() { return "stub"; }
        @Override public List<WebElement> findElements(final By by) { return Collections.emptyList(); }
        @Override public WebElement findElement(final By by) {
            throw new NoSuchElementException("stub"); }
        @Override public boolean isDisplayed() { return true; }
        @Override public org.openqa.selenium.Point getLocation() {
            return new org.openqa.selenium.Point(0, 0); }
        @Override public org.openqa.selenium.Dimension getSize() {
            return new org.openqa.selenium.Dimension(10, 10); }
        @Override public org.openqa.selenium.Rectangle getRect() {
            return new org.openqa.selenium.Rectangle(0, 0, 10, 10); }
        @Override public String getCssValue(final String propertyName) { return ""; }
        @Override public <X> X getScreenshotAs(final org.openqa.selenium.OutputType<X> target) {
            return null; }
        @Override public org.openqa.selenium.SearchContext getShadowRoot() { return null; }
    }
}
