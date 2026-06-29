package com.omiinqa.intelligence;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Offline unit tests for {@link LocatorSuggester}.
 *
 * <p>All inputs are pure Java values; no WebDriver, no browser, no network.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class LocatorSuggesterTest {

    private LocatorSuggester suggester;

    @BeforeClass
    public void setup() {
        suggester = new LocatorSuggester();
    }

    // =========================================================================
    // ID attribute — highest priority
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void idAttributeProducesIdCssSelector() {
        final List<String> selectors = suggester.suggest("button", null,
                Map.of("id", "submit-btn"));
        assertTrue(selectors.stream().anyMatch(s -> s.startsWith("#")),
                "Expected a CSS #id selector");
    }

    @Test(groups = {"intelligence", "unit"})
    public void idAttributeProducesXpathById() {
        final List<String> selectors = suggester.suggest("input", null,
                Map.of("id", "username"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("@id") && s.contains("username")),
                "Expected an XPath @id selector");
    }

    @Test(groups = {"intelligence", "unit"})
    public void idAndTagProducesTagPrefixedCss() {
        final List<String> selectors = suggester.suggest("button", null,
                Map.of("id", "ok"));
        assertTrue(selectors.stream().anyMatch(s -> s.startsWith("button#")),
                "Expected 'button#ok' style selector when tag is provided");
    }

    // =========================================================================
    // data-testid / test hook attributes
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void dataTestidProducesCssAttributeSelector() {
        final List<String> selectors = suggester.suggest("div", null,
                Map.of("data-testid", "login-form"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("data-testid") && s.contains("login-form")),
                "Expected [data-testid='login-form'] selector");
    }

    @Test(groups = {"intelligence", "unit"})
    public void dataCyAttributeIsRecognisedAsTestHook() {
        final List<String> selectors = suggester.suggest(null, null,
                Map.of("data-cy", "checkout-btn"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("data-cy") && s.contains("checkout-btn")),
                "Expected [data-cy='checkout-btn'] selector");
    }

    // =========================================================================
    // name attribute
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void nameAttributeProducesCssNameSelector() {
        final List<String> selectors = suggester.suggest("input", null,
                Map.of("name", "email"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("[name='email']")),
                "Expected [name='email'] CSS selector");
    }

    // =========================================================================
    // aria-label
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void ariaLabelProducesAttributeSelector() {
        final List<String> selectors = suggester.suggest("button", null,
                Map.of("aria-label", "Close modal"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("aria-label")),
                "Expected [aria-label='...'] selector");
    }

    // =========================================================================
    // Visible text — XPath only
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void visibleTextProducesXpathWithNormalizeSpace() {
        final List<String> selectors = suggester.suggest("button", "Submit Order", null);
        assertTrue(selectors.stream().anyMatch(
                s -> s.contains("normalize-space") && s.contains("Submit Order")),
                "Expected normalize-space(.) XPath for visible text");
    }

    @Test(groups = {"intelligence", "unit"})
    public void visibleTextWithTagProducesCorrectXpathTag() {
        final List<String> selectors = suggester.suggest("a", "Login", null);
        assertTrue(selectors.stream().anyMatch(s -> s.startsWith("//a") && s.contains("Login")),
                "Expected //a[...] XPath when tag is 'a'");
    }

    @Test(groups = {"intelligence", "unit"})
    public void visibleTextWithNoTagProducesWildcardXpath() {
        final List<String> selectors = suggester.suggest(null, "Checkout", null);
        assertTrue(selectors.stream().anyMatch(s -> s.startsWith("//*") && s.contains("Checkout")),
                "Expected //*[...] XPath when no tag provided");
    }

    // =========================================================================
    // type attribute
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void typeAttributeForInputProducesTypeSelector() {
        final List<String> selectors = suggester.suggest("input", null,
                Map.of("type", "password"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("input") && s.contains("type='password'")),
                "Expected input[type='password'] selector");
    }

    // =========================================================================
    // href for anchors
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void hrefAttributeForAnchorProducesHrefSelector() {
        final List<String> selectors = suggester.suggest("a", null,
                Map.of("href", "/dashboard"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("href")),
                "Expected href selector for anchor tag");
    }

    // =========================================================================
    // Placeholder
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void placeholderProducesAttributeSelector() {
        final List<String> selectors = suggester.suggest("input", null,
                Map.of("placeholder", "Enter email"));
        assertTrue(selectors.stream().anyMatch(s -> s.contains("placeholder")),
                "Expected [placeholder='...'] selector");
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void nullInputsProduceEmptyList() {
        final List<String> selectors = suggester.suggest(null, null, null);
        assertNotNull(selectors);
        assertTrue(selectors.isEmpty(), "Expected empty list for all-null inputs");
    }

    @Test(groups = {"intelligence", "unit"})
    public void emptyAttributeMapProducesEmptyList() {
        final List<String> selectors = suggester.suggest("span", null, Map.of());
        assertNotNull(selectors);
        assertTrue(selectors.isEmpty(), "Expected empty list when no useful attributes");
    }

    @Test(groups = {"intelligence", "unit"})
    public void returnedListIsImmutable() {
        final List<String> selectors = suggester.suggest("button", "OK", Map.of("id", "ok-btn"));
        assertThrows(UnsupportedOperationException.class, () -> selectors.add("hack"));
    }

    // =========================================================================
    // XPath escape utility
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void xpathEscapeHandlesNoQuote() {
        assertEquals(LocatorSuggester.xpathEscape("simple"), "simple");
    }

    @Test(groups = {"intelligence", "unit"})
    public void xpathEscapeHandlesSingleQuote() {
        final String result = LocatorSuggester.xpathEscape("O'Brien");
        // Must use concat() idiom
        assertTrue(result.startsWith("concat("),
                "Single-quoted value must use XPath concat() idiom");
        assertTrue(result.contains("O"), "concat result must contain original value parts");
    }

    // =========================================================================
    // CSS escape utility
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void cssEscapeHandlesPlainId() {
        assertEquals(LocatorSuggester.cssEscape("submit"), "submit");
    }

    @Test(groups = {"intelligence", "unit"})
    public void cssEscapeHandlesSpecialChars() {
        final String escaped = LocatorSuggester.cssEscape("btn:primary");
        assertTrue(escaped.contains("\\"), "Special CSS chars should be escaped with backslash");
    }
}
