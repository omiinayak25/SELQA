package com.omiinqa.intelligence;

import org.openqa.selenium.By;

import java.util.List;

/**
 * Strategy interface for generating alternative {@link By} locator candidates
 * when a primary locator has failed.
 *
 * <h3>Design pattern</h3>
 * <p>This is a pure <em>Strategy</em> pattern (GoF). Different implementations
 * can provide different healing algorithms (DOM heuristics, ML-based, snapshot
 * diffing, etc.) without altering the {@link SmartLocator} that consumes them.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Implementations must be <strong>stateless and thread-safe</strong>.</li>
 *   <li>Implementations must be <strong>fully offline</strong> — no network
 *       calls, no driver interaction. They receive only the failed locator and
 *       the raw page-source HTML string.</li>
 *   <li>The returned list is ordered by descending likelihood of success;
 *       callers try candidates in list order.</li>
 *   <li>An empty list is a valid return — it signals that this strategy could
 *       not derive any alternatives.</li>
 * </ul>
 *
 * <h3>Heuristic vs. AI</h3>
 * <p>The default implementation ({@link DomHeuristicHealingStrategy}) is
 * entirely rule-based and deterministic. No external AI service is involved
 * unless you wire a custom strategy that wraps {@link ai.AiAssistant}.</p>
 */
public interface LocatorHealingStrategy {

    /**
     * Proposes alternative {@link By} locators for a locator that could not be
     * resolved on the current page.
     *
     * @param failedLocator the {@link By} that produced no match
     * @param pageSource    the raw HTML source of the page at the time of failure;
     *                      may be empty but never {@code null}
     * @return an ordered (best-first) list of candidate replacements; never
     *         {@code null}, may be empty
     */
    List<By> propose(By failedLocator, String pageSource);
}
