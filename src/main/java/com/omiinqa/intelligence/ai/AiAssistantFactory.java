package com.omiinqa.intelligence.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for obtaining the active {@link AiAssistant} implementation.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li><b>Default (no credentials)</b>: returns {@link NoOpAiAssistant} — all
 *       methods return {@code Optional.empty()}, zero network calls.</li>
 *   <li><b>With credentials</b>: when the environment variable
 *       {@code OMIINQA_AI_API_KEY} is set to a non-blank value, returns an
 *       {@link HttpAiAssistant} wired to the configured endpoint. The assistant
 *       still degrades gracefully to empty on network errors.</li>
 * </ul>
 *
 * <h3>Environment variables</h3>
 * <table border="1">
 *   <caption>Supported environment variables</caption>
 *   <tr><th>Variable</th><th>Required?</th><th>Description</th></tr>
 *   <tr><td>{@code OMIINQA_AI_API_KEY}</td><td>Yes (to enable AI)</td>
 *       <td>API key for the external AI service.</td></tr>
 *   <tr><td>{@code OMIINQA_AI_API_URL}</td><td>No</td>
 *       <td>Base URL of the AI REST endpoint; defaults to placeholder.</td></tr>
 *   <tr><td>{@code OMIINQA_AI_MODEL}</td><td>No</td>
 *       <td>Model identifier string (e.g. {@code "gpt-4o"}).</td></tr>
 * </table>
 *
 * <h3>Honesty disclaimer</h3>
 * <p>This factory does <strong>NOT</strong> bundle any AI model or API key.
 * The OmiinQA framework is heuristic by default. AI assistance is purely
 * opt-in and requires a valid key from a third-party AI provider.</p>
 *
 * <h3>Thread safety</h3>
 * <p>All methods are stateless and thread-safe.</p>
 */
public final class AiAssistantFactory {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantFactory.class);

    /** Environment variable name that enables AI integration. */
    public static final String ENV_API_KEY = "OMIINQA_AI_API_KEY";
    /** Environment variable name for the AI endpoint URL. */
    public static final String ENV_API_URL = "OMIINQA_AI_API_URL";
    /** Environment variable name for the AI model identifier. */
    public static final String ENV_MODEL   = "OMIINQA_AI_MODEL";

    private AiAssistantFactory() {}

    /**
     * Returns the appropriate {@link AiAssistant} based on the current
     * environment.
     *
     * <ul>
     *   <li>If {@code OMIINQA_AI_API_KEY} is set → {@link HttpAiAssistant}.</li>
     *   <li>Otherwise → {@link NoOpAiAssistant} (the default).</li>
     * </ul>
     *
     * @return a ready-to-use {@link AiAssistant}; never {@code null}
     */
    public static AiAssistant getDefault() {
        final String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("AiAssistantFactory: {} not set — returning NoOpAiAssistant. "
                    + "All intelligence features operate in heuristic-only mode.", ENV_API_KEY);
            return NoOpAiAssistant.INSTANCE;
        }

        final String apiUrl = System.getenv(ENV_API_URL);
        final String model  = System.getenv(ENV_MODEL);
        log.info("AiAssistantFactory: {} is set — returning HttpAiAssistant (url={}, model={})",
                ENV_API_KEY, apiUrl != null ? apiUrl : "default", model != null ? model : "default");
        return new HttpAiAssistant(apiKey, apiUrl, model);
    }

    /**
     * Convenience method: returns {@code true} if an AI key is configured in
     * the current environment.
     *
     * @return {@code true} if AI is available
     */
    public static boolean isAiAvailable() {
        final String key = System.getenv(ENV_API_KEY);
        return key != null && !key.isBlank();
    }

    /**
     * Returns a {@link NoOpAiAssistant} unconditionally, bypassing environment
     * detection. Useful in tests or when you explicitly want no-op behaviour.
     *
     * @return a shared {@link NoOpAiAssistant} instance
     */
    public static AiAssistant noOp() {
        return NoOpAiAssistant.INSTANCE;
    }
}
