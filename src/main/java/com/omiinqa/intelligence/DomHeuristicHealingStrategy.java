package com.omiinqa.intelligence;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A heuristic, purely offline implementation of {@link LocatorHealingStrategy}
 * that analyses the raw page-source HTML and the shape of the failed locator to
 * propose plausible alternative {@link By} candidates.
 *
 * <h3>Heuristics applied (in priority order)</h3>
 * <ol>
 *   <li><b>ID ↔ name ↔ CSS bridge</b> – if the failed locator targets an
 *       {@code id}, also try the same value as {@code name} attribute and as
 *       a CSS selector, and vice-versa.</li>
 *   <li><b>Dynamic suffix stripping</b> – removes trailing numeric suffixes or
 *       UUID fragments (e.g. {@code submit_btn_3f8a} → {@code submit_btn}) and
 *       builds a CSS attribute-starts-with or XPath {@code starts-with()}
 *       expression.</li>
 *   <li><b>XPath relaxation</b> – converts an absolute XPath
 *       (starting with {@code /html}) to a relative search ({@code //}) so the
 *       locator is robust to structural shifts above the target element.</li>
 *   <li><b>Partial-text fallback</b> – if the failed locator is a
 *       {@code linkText}, also try {@code partialLinkText} with the same
 *       value.</li>
 *   <li><b>DOM scan</b> – searches the raw page source for elements whose
 *       {@code id}, {@code name}, or {@code data-testid} attributes contain the
 *       key term extracted from the failed locator, and emits CSS selectors for
 *       the top matches.</li>
 * </ol>
 *
 * <h3>No AI involved</h3>
 * <p>Every decision in this class is rule-based and deterministic. The output
 * for identical inputs is always identical. This class does not call any
 * network endpoint or external service.</p>
 *
 * <h3>Thread safety</h3>
 * <p>This class is stateless; a single instance may be shared across threads.</p>
 */
public class DomHeuristicHealingStrategy implements LocatorHealingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DomHeuristicHealingStrategy.class);

    /** Trailing dynamic suffix patterns: digits, UUID fragments, hashes. */
    private static final Pattern DYNAMIC_SUFFIX =
            Pattern.compile("[-_]?([0-9a-f]{4,}|\\d+)$", Pattern.CASE_INSENSITIVE);

    /** Extracts the "key term" from a CSS selector: last segment after space or >. */
    private static final Pattern CSS_KEY_TERM = Pattern.compile("[\\s>+~]?(\\S+)$");

    /** Finds attribute values in raw HTML that contain a given substring. */
    private static Pattern attrContains(final String attr, final String term) {
        return Pattern.compile(
                attr + "\\s*=\\s*[\"']([^\"']*" + Pattern.quote(term) + "[^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE);
    }

    @Override
    public List<By> propose(final By failedLocator, final String pageSource) {
        final List<By> candidates = new ArrayList<>();
        final String locatorStr = failedLocator.toString();

        log.debug("DomHeuristicHealingStrategy: analysing failed locator [{}]", locatorStr);

        applyIdNameCssBridge(failedLocator, locatorStr, candidates);
        applyDynamicSuffixStripping(locatorStr, candidates);
        applyXpathRelaxation(locatorStr, candidates);
        applyLinkTextFallback(failedLocator, locatorStr, candidates);
        applyDomScan(locatorStr, pageSource, candidates);

        log.debug("Proposed {} alternative(s) for [{}]", candidates.size(), locatorStr);
        return candidates;
    }

    // -------------------------------------------------------------------------
    // Heuristic 1 — ID ↔ Name ↔ CSS bridge
    // -------------------------------------------------------------------------

    private void applyIdNameCssBridge(final By locator,
                                      final String locatorStr,
                                      final List<By> out) {
        final String value = extractValue(locatorStr);
        if (value == null || value.isBlank()) {
            return;
        }

        if (locatorStr.startsWith("By.id:")) {
            out.add(By.name(value));
            out.add(By.cssSelector("[id='" + value + "']"));
            out.add(By.cssSelector("[name='" + value + "']"));
        } else if (locatorStr.startsWith("By.name:")) {
            out.add(By.id(value));
            out.add(By.cssSelector("[name='" + value + "']"));
            out.add(By.cssSelector("[id='" + value + "']"));
        } else if (locatorStr.startsWith("By.cssSelector:")) {
            // If the CSS looks like a simple #id, also try By.id
            if (value.startsWith("#") && !value.contains(" ")) {
                out.add(By.id(value.substring(1)));
                out.add(By.name(value.substring(1)));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Heuristic 2 — Dynamic suffix stripping
    // -------------------------------------------------------------------------

    private void applyDynamicSuffixStripping(final String locatorStr,
                                              final List<By> out) {
        final String value = extractValue(locatorStr);
        if (value == null || value.isBlank()) {
            return;
        }

        final Matcher m = DYNAMIC_SUFFIX.matcher(value);
        if (!m.find()) {
            return;
        }

        final String stripped = value.substring(0, m.start());
        if (stripped.isBlank()) {
            return;
        }

        if (locatorStr.startsWith("By.id:")) {
            out.add(By.cssSelector("[id^='" + stripped + "']"));
            out.add(By.xpath("//*[starts-with(@id,'" + stripped + "')]"));
        } else if (locatorStr.startsWith("By.name:")) {
            out.add(By.cssSelector("[name^='" + stripped + "']"));
            out.add(By.xpath("//*[starts-with(@name,'" + stripped + "')]"));
        } else if (locatorStr.startsWith("By.cssSelector:")) {
            out.add(By.cssSelector("[class^='" + stripped + "']"));
        } else if (locatorStr.startsWith("By.xpath:")) {
            // Replace the exact dynamic token inside the xpath with starts-with
            final String relaxed = value.replace(m.group(0), "')][not(self::x)] | //*"); // fallback
            out.add(By.xpath("//*[starts-with(@id,'" + stripped + "')]"));
        }
    }

    // -------------------------------------------------------------------------
    // Heuristic 3 — Absolute XPath → relative XPath
    // -------------------------------------------------------------------------

    private void applyXpathRelaxation(final String locatorStr,
                                       final List<By> out) {
        if (!locatorStr.startsWith("By.xpath:")) {
            return;
        }
        final String value = extractValue(locatorStr);
        if (value == null) {
            return;
        }
        // Convert absolute path /html/body/... to relative //...
        if (value.startsWith("/html") || value.startsWith("/HTML")) {
            // Extract the last meaningful segment: everything after the last /tag[
            final String[] parts = value.split("/");
            if (parts.length > 2) {
                // Build //lastSegment style
                final String last = parts[parts.length - 1];
                if (!last.isBlank()) {
                    out.add(By.xpath("//" + last));
                }
                // Also try the last two segments
                if (parts.length > 3) {
                    final String secondLast = parts[parts.length - 2];
                    if (!secondLast.isBlank()) {
                        out.add(By.xpath("//" + secondLast + "/" + last));
                    }
                }
            }
        }
        // Exact index predicates like [1] make XPath brittle — add sibling search
        if (value.contains("[1]") || value.contains("[2]") || value.contains("[3]")) {
            final String noIndex = value.replaceAll("\\[\\d+]", "");
            if (!noIndex.equals(value)) {
                out.add(By.xpath(noIndex));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Heuristic 4 — linkText → partialLinkText
    // -------------------------------------------------------------------------

    private void applyLinkTextFallback(final By locator,
                                        final String locatorStr,
                                        final List<By> out) {
        if (!locatorStr.startsWith("By.linkText:")) {
            return;
        }
        final String value = extractValue(locatorStr);
        if (value != null && !value.isBlank()) {
            out.add(By.partialLinkText(value));
            // Also try an XPath with normalize-space to handle whitespace differences
            out.add(By.xpath("//a[normalize-space(.)='" + value + "']"));
        }
    }

    // -------------------------------------------------------------------------
    // Heuristic 5 — DOM scan for partial attribute matches
    // -------------------------------------------------------------------------

    private void applyDomScan(final String locatorStr,
                               final String pageSource,
                               final List<By> out) {
        if (pageSource == null || pageSource.isBlank()) {
            return;
        }

        final String term = extractKeyTerm(locatorStr);
        if (term == null || term.length() < 3) {
            return;
        }

        // Search id attributes
        findAttributeMatches("id", term, pageSource, out, 2);
        // Search name attributes
        findAttributeMatches("name", term, pageSource, out, 1);
        // Search data-testid
        findAttributeMatches("data-testid", term, pageSource, out, 1);
    }

    private void findAttributeMatches(final String attr,
                                       final String term,
                                       final String html,
                                       final List<By> out,
                                       final int maxResults) {
        final Matcher m = attrContains(attr, term).matcher(html);
        int count = 0;
        while (m.find() && count < maxResults) {
            final String found = m.group(1);
            if ("id".equals(attr)) {
                out.add(By.id(found));
            } else if ("name".equals(attr)) {
                out.add(By.name(found));
            } else {
                out.add(By.cssSelector("[" + attr + "='" + found + "']"));
            }
            count++;
        }
    }

    // -------------------------------------------------------------------------
    // Utility: extract the raw "value" from a By.toString() string
    // -------------------------------------------------------------------------

    /**
     * Extracts the selector value from Selenium's {@code By.toString()} output.
     * Examples:
     * <ul>
     *   <li>{@code "By.id: submit"} → {@code "submit"}</li>
     *   <li>{@code "By.xpath: //div[@id='x']"} → {@code "//div[@id='x']"}</li>
     * </ul>
     */
    static String extractValue(final String locatorStr) {
        final int colon = locatorStr.indexOf(':');
        if (colon < 0 || colon >= locatorStr.length() - 1) {
            return null;
        }
        return locatorStr.substring(colon + 1).trim();
    }

    /**
     * Extracts a short "key term" from the locator string suitable for DOM
     * scanning. For example, from {@code By.cssSelector: .btn-submit} this
     * returns {@code "btn-submit"}.
     */
    private static String extractKeyTerm(final String locatorStr) {
        final String value = extractValue(locatorStr);
        if (value == null) {
            return null;
        }
        // Strip common selector prefixes and operators
        String term = value
                .replaceAll("^[#.\\['\"]", "")
                .replaceAll("[\"'\\]]+$", "");
        // Take the last meaningful word
        final Matcher m = CSS_KEY_TERM.matcher(term);
        if (m.find()) {
            term = m.group(1)
                    .replaceAll("[^a-zA-Z0-9_\\-]", "");
        }
        return term.isBlank() ? null : term;
    }
}
