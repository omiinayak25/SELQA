package com.omiinqa.accessibility;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runs axe-core accessibility scans against the live page (Facade over
 * {@link AxeBuilder}).
 *
 * <p>Centralizes axe configuration — WCAG tag selection, rule disabling — so the
 * accessibility test layer asserts against a consistent ruleset. Returns the raw
 * violation list; assertion policy (e.g. "no critical/serious violations") lives
 * in {@link AccessibilityValidator} so scanning and judging stay separable.</p>
 */
public final class AccessibilityScanner {

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilityScanner.class);

    /** WCAG 2.1 A & AA tag set — the common enterprise baseline. */
    public static final List<String> WCAG_21_AA =
            List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    private final AxeBuilder builder;

    private AccessibilityScanner(final AxeBuilder builder) {
        this.builder = builder;
    }

    public static AccessibilityScanner create() {
        return new AccessibilityScanner(new AxeBuilder());
    }

    /** Scan only against the WCAG 2.1 A/AA ruleset. */
    public static AccessibilityScanner wcag21aa() {
        return new AccessibilityScanner(new AxeBuilder().withTags(WCAG_21_AA));
    }

    public AccessibilityScanner withTags(final List<String> tags) {
        builder.withTags(tags);
        return this;
    }

    public AccessibilityScanner disableRules(final List<String> ruleIds) {
        builder.disableRules(ruleIds);
        return this;
    }

    /** Limit the scan to a CSS selector (e.g. a single component). */
    public AccessibilityScanner include(final String cssSelector) {
        builder.include(cssSelector);
        return this;
    }

    /** Execute the scan and return the full axe result set. */
    public Results scan(final WebDriver driver) {
        final Results results = builder.analyze(driver);
        final List<Rule> violations = results.getViolations();
        LOG.info("Accessibility scan @ {} -> {} violation rule(s)",
                results.getUrl(), violations == null ? 0 : violations.size());
        return results;
    }
}
