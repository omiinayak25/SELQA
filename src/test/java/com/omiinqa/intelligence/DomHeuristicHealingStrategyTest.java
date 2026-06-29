package com.omiinqa.intelligence;

import org.openqa.selenium.By;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Offline unit tests for {@link DomHeuristicHealingStrategy}.
 *
 * <p>All tests feed raw HTML strings and failed {@link By} instances and assert
 * on the proposed candidate list. No WebDriver, no browser, no network.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class DomHeuristicHealingStrategyTest {

    private DomHeuristicHealingStrategy strategy;

    @BeforeClass
    public void setup() {
        strategy = new DomHeuristicHealingStrategy();
    }

    // =========================================================================
    // Heuristic 1 — ID / Name / CSS bridge
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void idLocatorProposesCssFallbacks() {
        final List<By> proposals = strategy.propose(By.id("submit"), "");
        assertFalse(proposals.isEmpty(), "Should propose alternatives for By.id");
        // Should include a name fallback
        assertTrue(proposals.stream().anyMatch(b -> b.equals(By.name("submit"))),
                "Expected By.name('submit') in proposals");
    }

    @Test(groups = {"intelligence", "unit"})
    public void nameLocatorProposesIdFallback() {
        final List<By> proposals = strategy.propose(By.name("username"), "");
        assertTrue(proposals.stream().anyMatch(b -> b.equals(By.id("username"))),
                "Expected By.id('username') in proposals for a By.name locator");
    }

    @Test(groups = {"intelligence", "unit"})
    public void cssIdSelectorProposesById() {
        final List<By> proposals = strategy.propose(By.cssSelector("#login-btn"), "");
        assertTrue(proposals.stream().anyMatch(b -> b.equals(By.id("login-btn"))),
                "Expected By.id for #login-btn CSS selector");
    }

    // =========================================================================
    // Heuristic 2 — Dynamic suffix stripping
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void dynamicNumericSuffixIsStrippedForId() {
        // e.g. id="submit_3" → propose [id^='submit_']
        final List<By> proposals = strategy.propose(By.id("submit_3"), "");
        assertTrue(proposals.stream().anyMatch(b -> b.toString().contains("submit_")),
                "Should propose a starts-with variant stripping the numeric suffix");
    }

    @Test(groups = {"intelligence", "unit"})
    public void dynamicHexSuffixIsStrippedForId() {
        final List<By> proposals = strategy.propose(By.id("btn_3f8a"), "");
        assertTrue(proposals.stream().anyMatch(b -> b.toString().contains("btn")),
                "Should strip the hex suffix and produce a partial match selector");
    }

    @Test(groups = {"intelligence", "unit"})
    public void cleanIdWithNoDynamicSuffixProducesNoStripVariant() {
        // "submit" has no trailing digits → no starts-with variant expected from stripping
        final List<By> proposals = strategy.propose(By.id("submit"), "");
        // Just asserting it does not throw and returns a list
        assertNotNull(proposals);
    }

    // =========================================================================
    // Heuristic 3 — XPath relaxation
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void absoluteXpathIsRelaxedToRelative() {
        final By absolute = By.xpath("/html/body/div[1]/form/button");
        final List<By> proposals = strategy.propose(absolute, "");
        // Should produce a // variant
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.toString().contains("//button") || b.toString().contains("//form")),
                "Absolute XPath should produce a relative // alternative");
    }

    @Test(groups = {"intelligence", "unit"})
    public void indexedXpathHasIndexStripped() {
        final By indexed = By.xpath("//ul/li[2]/a");
        final List<By> proposals = strategy.propose(indexed, "");
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.toString().contains("//ul/li/a")),
                "Indexed XPath should produce an index-free variant");
    }

    @Test(groups = {"intelligence", "unit"})
    public void relativeXpathWithNoIndexProducesNoStripVariant() {
        final By rel = By.xpath("//button[@type='submit']");
        final List<By> proposals = strategy.propose(rel, "");
        assertNotNull(proposals); // must not throw
    }

    // =========================================================================
    // Heuristic 4 — linkText → partialLinkText
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void linkTextProposesPartialLinkText() {
        final By link = By.linkText("Click here to continue");
        final List<By> proposals = strategy.propose(link, "");
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.equals(By.partialLinkText("Click here to continue"))),
                "By.linkText should propose By.partialLinkText with same value");
    }

    @Test(groups = {"intelligence", "unit"})
    public void linkTextProposesXpathAnchor() {
        final By link = By.linkText("Sign In");
        final List<By> proposals = strategy.propose(link, "");
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.toString().contains("//a") && b.toString().contains("Sign In")),
                "By.linkText should propose an XPath anchor alternative");
    }

    // =========================================================================
    // Heuristic 5 — DOM scan from page source
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void domScanFindsIdInPageSource() {
        // The key term extracted from By.id("submit") is "submit".
        // The DOM contains id="submit-form-btn" which includes "submit" as a substring.
        final String html = "<html><body>"
                + "<button id=\"submit-form-btn\" type=\"submit\">Submit</button>"
                + "</body></html>";
        final List<By> proposals = strategy.propose(By.id("submit"), html);
        // Should find "submit-form-btn" via partial id scan (regex [^"']*submit[^"']*)
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.toString().contains("submit-form-btn")),
                "DOM scan should find partial id match 'submit-form-btn' for term 'submit'");
    }

    @Test(groups = {"intelligence", "unit"})
    public void domScanFindsDataTestidInPageSource() {
        final String html = "<div data-testid=\"login-panel\">"
                + "<input data-testid=\"login-username\" />"
                + "</div>";
        final List<By> proposals = strategy.propose(By.id("login-user"), html);
        assertTrue(proposals.stream()
                        .anyMatch(b -> b.toString().contains("login-username")),
                "DOM scan should find data-testid attribute containing 'login'");
    }

    @Test(groups = {"intelligence", "unit"})
    public void emptyPageSourceProducesNoCrash() {
        final List<By> proposals = strategy.propose(By.id("missing"), "");
        assertNotNull(proposals); // must not throw
    }

    // =========================================================================
    // extractValue utility (package-accessible)
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void extractValueHandlesIdLocator() {
        final String value = DomHeuristicHealingStrategy.extractValue("By.id: submit");
        assertEquals(value, "submit");
    }

    @Test(groups = {"intelligence", "unit"})
    public void extractValueHandlesXpathLocator() {
        final String value = DomHeuristicHealingStrategy.extractValue(
                "By.xpath: //button[@type='submit']");
        assertEquals(value, "//button[@type='submit']");
    }

    @Test(groups = {"intelligence", "unit"})
    public void extractValueReturnsNullForNoColon() {
        final String value = DomHeuristicHealingStrategy.extractValue("ByWithNoColon");
        assertNull(value);
    }

    // =========================================================================
    // General
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void proposeNeverReturnsNull() {
        assertNotNull(strategy.propose(By.id("anything"), null));
        assertNotNull(strategy.propose(By.name("anything"), ""));
        assertNotNull(strategy.propose(By.xpath("//div"), "<html></html>"));
    }
}
