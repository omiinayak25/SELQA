package com.omiinqa.intelligence.ai;

import java.util.Optional;

/**
 * Optional AI provider hook for the OmiinQA intelligence layer.
 *
 * <h3>Design intent</h3>
 * <p>This interface defines the contract for an external AI service that can
 * assist with intelligent testing tasks. The <strong>default implementation is
 * {@link NoOpAiAssistant}</strong>, which returns {@link Optional#empty()} for
 * every method — making AI entirely optional and safe to ignore.</p>
 *
 * <h3>IMPORTANT — honesty about AI</h3>
 * <p>The OmiinQA framework does <strong>NOT</strong> include a built-in AI
 * model. All "intelligent" behaviour in {@code com.omiinqa.intelligence.*} is
 * heuristic and deterministic by default. If you want genuine AI assistance,
 * you must supply credentials for a real external service and wire it through
 * {@link AiAssistantFactory}. Without credentials the factory returns
 * {@link NoOpAiAssistant} and no network calls are ever made.</p>
 *
 * <h3>Graceful degradation</h3>
 * <p>Callers should treat {@link Optional#empty()} as "AI not available" and
 * fall back to heuristic logic. Never assume this returns a value.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be thread-safe.</p>
 */
public interface AiAssistant {

    /**
     * Asks the AI to suggest a CSS or XPath locator for an element described in
     * plain English.
     *
     * <p>The description may include the element's role, label, surrounding
     * context, etc. The AI is expected to return a single selector string.</p>
     *
     * <p>Returns {@link Optional#empty()} if the AI is not configured or if the
     * AI cannot produce a confident suggestion.</p>
     *
     * @param description human-readable description of the element to locate
     * @return an {@link Optional} containing a CSS or XPath selector string, or
     *         empty if unavailable
     */
    Optional<String> suggestLocator(String description);

    /**
     * Asks the AI to categorize a test failure described by its message/stack.
     *
     * <p>The expected return value is a category label (e.g. {@code "LOCATOR"},
     * {@code "TIMEOUT"}) consistent with
     * {@link com.omiinqa.intelligence.FailureCategorizer.FailureCategory}
     * names, but callers should not assume the string is a valid enum name.</p>
     *
     * <p>Returns {@link Optional#empty()} if the AI is not configured.</p>
     *
     * @param failureText the exception message, stack trace, or log excerpt
     * @return an {@link Optional} containing a category label, or empty
     */
    Optional<String> categorizeFailure(String failureText);

    /**
     * Asks the AI to produce a concise human-readable summary of the given text
     * (e.g. a stack trace, a test run log, or a diff).
     *
     * <p>Returns {@link Optional#empty()} if the AI is not configured.</p>
     *
     * @param text the text to summarize
     * @return an {@link Optional} containing a short summary, or empty
     */
    Optional<String> summarize(String text);
}
