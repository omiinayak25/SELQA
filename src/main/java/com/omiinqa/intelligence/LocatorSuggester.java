package com.omiinqa.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Offline, deterministic CSS and XPath selector generator.
 *
 * <h3>Role</h3>
 * <p>Given a high-level description of an HTML element — its tag, visible text,
 * and/or attribute map — {@code LocatorSuggester} generates a ranked list of
 * CSS selectors and XPath expressions that are likely to match that element on
 * a real page.</p>
 *
 * <h3>Heuristic "AI" — no external calls</h3>
 * <p>Despite the name "suggester", this class uses only deterministic rule-based
 * logic. It does NOT contact any AI service or network endpoint. The output for
 * identical inputs is always identical. If you need genuine AI-assisted
 * generation, wrap this class with an {@link ai.AiAssistant} call and merge
 * results.</p>
 *
 * <h3>Selector quality ordering</h3>
 * <ol>
 *   <li>{@code id} attribute — most stable, highest priority</li>
 *   <li>{@code data-testid} / {@code data-test} / {@code data-qa} — purpose-built
 *       test hooks, very stable</li>
 *   <li>{@code name} attribute</li>
 *   <li>{@code aria-label} — semantic, fairly stable</li>
 *   <li>Tag + exact text (XPath)</li>
 *   <li>Tag + class combination (CSS)</li>
 *   <li>Generic attribute selector (CSS)</li>
 *   <li>Partial text (XPath {@code contains()})</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>Stateless — a single shared instance is safe.</p>
 */
public final class LocatorSuggester {

    private static final Logger log = LoggerFactory.getLogger(LocatorSuggester.class);

    /**
     * Generates candidate CSS/XPath selectors for an element described by its
     * tag, visible text, and/or attributes.
     *
     * @param tag        the HTML tag name (e.g. {@code "button"}, {@code "input"},
     *                   {@code "a"}); may be {@code null} or empty to mean "any tag"
     * @param visibleText the visible text content of the element; may be
     *                   {@code null} or empty
     * @param attributes a map of HTML attribute name → value pairs known for this
     *                   element; may be {@code null} or empty
     * @return an ordered (highest-quality-first) list of CSS/XPath selector
     *         strings; never {@code null}, may be empty
     */
    public List<String> suggest(final String tag,
                                final String visibleText,
                                final Map<String, String> attributes) {
        final List<String> selectors = new ArrayList<>();
        final String tagPart = (tag == null || tag.isBlank()) ? "" : tag.trim().toLowerCase();
        final Map<String, String> attrs = (attributes == null) ? Collections.emptyMap() : attributes;

        // 1. id attribute (highest priority — unique per spec)
        final String id = attrs.get("id");
        if (id != null && !id.isBlank()) {
            selectors.add("#" + cssEscape(id));
            if (!tagPart.isBlank()) {
                selectors.add(tagPart + "#" + cssEscape(id));
            }
            selectors.add("//*[@id='" + xpathEscape(id) + "']");
        }

        // 2. Test-hook attributes (purpose-built, very stable)
        for (final String testAttr : new String[]{"data-testid", "data-test", "data-qa", "data-cy"}) {
            final String val = attrs.get(testAttr);
            if (val != null && !val.isBlank()) {
                selectors.add("[" + testAttr + "='" + val + "']");
                if (!tagPart.isBlank()) {
                    selectors.add(tagPart + "[" + testAttr + "='" + val + "']");
                }
            }
        }

        // 3. name attribute
        final String name = attrs.get("name");
        if (name != null && !name.isBlank()) {
            final String tagPrefix = tagPart.isBlank() ? "" : tagPart;
            selectors.add(tagPrefix + "[name='" + name + "']");
            selectors.add("//*[@name='" + xpathEscape(name) + "']");
        }

        // 4. aria-label
        final String ariaLabel = attrs.get("aria-label");
        if (ariaLabel != null && !ariaLabel.isBlank()) {
            selectors.add("[aria-label='" + ariaLabel + "']");
            selectors.add("//*[@aria-label='" + xpathEscape(ariaLabel) + "']");
        }

        // 5. Exact text — XPath only (CSS cannot match text content)
        if (visibleText != null && !visibleText.isBlank()) {
            final String xTag = tagPart.isBlank() ? "*" : tagPart;
            selectors.add("//" + xTag + "[normalize-space(.)='"
                    + xpathEscape(visibleText.trim()) + "']");
        }

        // 6. Tag + class combination
        final String cssClass = attrs.get("class");
        if (cssClass != null && !cssClass.isBlank() && !tagPart.isBlank()) {
            // Use only the first stable-looking class (skip utility/dynamic ones)
            for (final String cls : cssClass.split("\\s+")) {
                if (!cls.isBlank() && !looksUtility(cls)) {
                    selectors.add(tagPart + "." + cssEscape(cls));
                    break;
                }
            }
        }

        // 7. type attribute (very common for inputs/buttons)
        final String type = attrs.get("type");
        if (type != null && !type.isBlank() && !tagPart.isBlank()) {
            selectors.add(tagPart + "[type='" + type + "']");
        }

        // 8. Partial text via XPath contains()
        if (visibleText != null && visibleText.trim().length() > 3) {
            final String xTag = tagPart.isBlank() ? "*" : tagPart;
            selectors.add("//" + xTag + "[contains(normalize-space(.),'"
                    + xpathEscape(visibleText.trim()) + "')]");
        }

        // 9. href (for anchors)
        if ("a".equals(tagPart)) {
            final String href = attrs.get("href");
            if (href != null && !href.isBlank()) {
                selectors.add("a[href='" + href + "']");
                selectors.add("a[href*='" + href + "']");
            }
        }

        // 10. placeholder (for inputs — visible to screen readers, often stable)
        final String placeholder = attrs.get("placeholder");
        if (placeholder != null && !placeholder.isBlank()) {
            selectors.add("[placeholder='" + placeholder + "']");
        }

        log.debug("LocatorSuggester generated {} candidates for tag='{}' text='{}'",
                selectors.size(), tag, visibleText);
        return Collections.unmodifiableList(selectors);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Minimal CSS identifier escaping: replaces characters that would break a
     * CSS selector with an escaped version. A full implementation would follow
     * CSS.escape() but this covers the common cases.
     */
    static String cssEscape(final String value) {
        return value.replaceAll("([^a-zA-Z0-9_\\-])", "\\\\$1");
    }

    /**
     * Minimal XPath string escaping: if the value contains a single quote,
     * uses the XPath {@code concat()} idiom to produce a valid string literal.
     */
    static String xpathEscape(final String value) {
        if (!value.contains("'")) {
            return value;
        }
        // Split on ' and join with concat
        final String[] parts = value.split("'", -1);
        final StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(",\"'\",");
            }
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Heuristic check for utility/dynamic CSS class names that are poor
     * locator anchors (e.g. BEM modifiers, Tailwind utilities, random hashes).
     */
    private static boolean looksUtility(final String cls) {
        // Very short or pure numeric → utility
        if (cls.length() < 3) return true;
        // Tailwind-style: e.g. "mt-4", "text-gray-500", "flex"
        if (cls.matches("[a-z]+-\\d+")) return true;
        // Hash-like suffix: hex chars dominant
        if (cls.matches("[0-9a-f]{5,}")) return true;
        return false;
    }
}
