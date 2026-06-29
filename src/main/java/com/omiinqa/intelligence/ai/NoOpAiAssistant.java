package com.omiinqa.intelligence.ai;

import java.util.Optional;

/**
 * No-operation implementation of {@link AiAssistant}.
 *
 * <h3>Role</h3>
 * <p>This is the <strong>default</strong> {@link AiAssistant} returned by
 * {@link AiAssistantFactory} when no AI provider credentials are configured. It
 * always returns {@link Optional#empty()} and performs no computation, no
 * network calls, and allocates no resources beyond this method stack frame.</p>
 *
 * <h3>Why this exists</h3>
 * <p>Following the <em>Null Object</em> pattern, {@code NoOpAiAssistant}
 * eliminates null checks in callers. Code that calls {@code ai.suggestLocator()}
 * will always receive an {@code Optional} — callers simply fall back to
 * heuristic logic when it is empty.</p>
 *
 * <h3>Honesty disclaimer</h3>
 * <p>This class does <strong>NOT</strong> perform any AI or ML computation. All
 * methods are stubs. Real AI assistance requires an {@link HttpAiAssistant}
 * wired with valid API credentials via {@link AiAssistantFactory}.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Stateless singleton — fully thread-safe.</p>
 */
public final class NoOpAiAssistant implements AiAssistant {

    /**
     * Shared singleton instance. Use via {@link AiAssistantFactory}, not
     * directly.
     */
    static final NoOpAiAssistant INSTANCE = new NoOpAiAssistant();

    /** Package-private: use {@link AiAssistantFactory#getDefault()} instead. */
    NoOpAiAssistant() {}

    /**
     * {@inheritDoc}
     *
     * <p><b>This implementation always returns {@link Optional#empty()}.</b>
     * No AI is invoked.</p>
     */
    @Override
    public Optional<String> suggestLocator(final String description) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>This implementation always returns {@link Optional#empty()}.</b>
     * No AI is invoked.</p>
     */
    @Override
    public Optional<String> categorizeFailure(final String failureText) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>This implementation always returns {@link Optional#empty()}.</b>
     * No AI is invoked.</p>
     */
    @Override
    public Optional<String> summarize(final String text) {
        return Optional.empty();
    }
}
