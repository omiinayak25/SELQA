package com.omiinqa.accessibility;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fluent assertions over axe-core {@link Results}.
 *
 * <p>Separating judging from scanning lets tests choose their bar: some gate on
 * "zero critical/serious", others track a violation budget while debt is paid
 * down. Failure messages list the offending rule ids and help text so triage is
 * immediate.</p>
 */
public final class AccessibilityValidator {

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilityValidator.class);

    private final Results results;

    private AccessibilityValidator(final Results results) {
        this.results = results;
    }

    public static AccessibilityValidator of(final Results results) {
        return new AccessibilityValidator(results);
    }

    public List<Rule> violations() {
        return results.getViolations() == null ? List.of() : results.getViolations();
    }

    /** No violations at all (strictest bar). */
    public AccessibilityValidator hasNoViolations() {
        Assertions.assertThat(violations())
                .as("accessibility violations:%n%s", summarize(violations()))
                .isEmpty();
        return this;
    }

    /** No violations whose impact is critical or serious (pragmatic enterprise bar). */
    public AccessibilityValidator hasNoCriticalOrSeriousViolations() {
        final Set<String> blocking = Set.of("critical", "serious");
        final List<Rule> severe = violations().stream()
                .filter(r -> r.getImpact() != null && blocking.contains(r.getImpact().toLowerCase()))
                .collect(Collectors.toList());
        Assertions.assertThat(severe)
                .as("critical/serious accessibility violations:%n%s", summarize(severe))
                .isEmpty();
        return this;
    }

    /** Allow a capped number of violations while debt is being paid down. */
    public AccessibilityValidator violationsAtMost(final int max) {
        Assertions.assertThat(violations().size())
                .as("accessibility violation count within budget %d", max)
                .isLessThanOrEqualTo(max);
        return this;
    }

    private String summarize(final List<Rule> rules) {
        if (rules.isEmpty()) {
            return "  (none)";
        }
        return rules.stream()
                .map(r -> String.format("  - [%s] %s :: %s", r.getImpact(), r.getId(), r.getHelp()))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
