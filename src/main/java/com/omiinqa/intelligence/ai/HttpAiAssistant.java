package com.omiinqa.intelligence.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

/**
 * HTTP-backed {@link AiAssistant} that calls an external REST AI service.
 *
 * <h3>IMPORTANT — credentials required</h3>
 * <p>This class is a documented placeholder for a real AI integration. It is
 * <strong>only instantiated</strong> by {@link AiAssistantFactory} when a valid
 * API key is present in the environment variable {@code OMIINQA_AI_API_KEY}.
 * Without credentials, {@link AiAssistantFactory} returns
 * {@link NoOpAiAssistant} and this class is never instantiated — no network
 * call ever occurs.</p>
 *
 * <h3>Wiring</h3>
 * <p>To activate real AI assistance:
 * <ol>
 *   <li>Set environment variable {@code OMIINQA_AI_API_KEY} to a valid API key.</li>
 *   <li>Optionally set {@code OMIINQA_AI_API_URL} to point to your AI endpoint
 *       (defaults to a placeholder URL).</li>
 *   <li>Optionally set {@code OMIINQA_AI_MODEL} to select the model.</li>
 * </ol>
 * </p>
 *
 * <h3>Protocol</h3>
 * <p>The current implementation sends a simple JSON request to the configured
 * endpoint and expects a JSON response with a {@code "content"} field. Adapt
 * the request/response parsing to match your actual AI provider's API contract.</p>
 *
 * <h3>Failure handling</h3>
 * <p>Any network or parsing error returns {@link Optional#empty()} — AI
 * assistance degrades gracefully without breaking the test run.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Stateless per-call; the shared {@link ObjectMapper} is thread-safe after
 * construction.</p>
 */
public final class HttpAiAssistant implements AiAssistant {

    private static final Logger log = LoggerFactory.getLogger(HttpAiAssistant.class);

    /**
     * Default AI API endpoint URL (placeholder — must be overridden via
     * {@code OMIINQA_AI_API_URL} environment variable for real use).
     */
    private static final String DEFAULT_API_URL = "https://ai-provider-placeholder.example.com/v1/chat";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper mapper;

    /**
     * Package-private: instantiated only by {@link AiAssistantFactory}.
     *
     * @param apiKey  the API key for authenticating with the AI service
     * @param apiUrl  the base URL of the AI REST endpoint; {@code null} → default
     * @param model   the model identifier string; {@code null} → default
     */
    HttpAiAssistant(final String apiKey, final String apiUrl, final String model) {
        this.apiKey  = apiKey;
        this.apiUrl  = (apiUrl  != null && !apiUrl.isBlank())  ? apiUrl  : DEFAULT_API_URL;
        this.model   = (model   != null && !model.isBlank())   ? model   : "default";
        this.mapper  = new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the element description to the configured AI endpoint and
     * requests a CSS or XPath selector. Returns empty on any error.</p>
     */
    @Override
    public Optional<String> suggestLocator(final String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        final String prompt = "You are an expert Selenium test engineer. "
                + "Generate a single, stable CSS or XPath selector for the following element. "
                + "Return ONLY the selector string, no explanation.\nElement: " + description;
        return call(prompt);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the failure text to the AI endpoint for categorization. Returns
     * empty on any error.</p>
     */
    @Override
    public Optional<String> categorizeFailure(final String failureText) {
        if (failureText == null || failureText.isBlank()) {
            return Optional.empty();
        }
        final String prompt = "Classify the following test failure into exactly one category: "
                + "LOCATOR, TIMEOUT, ASSERTION, NETWORK, STALE_ELEMENT, DRIVER, DATA, UNKNOWN. "
                + "Return ONLY the category name.\nFailure:\n" + failureText;
        return call(prompt);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the text to the AI endpoint for summarization. Returns empty on
     * any error.</p>
     */
    @Override
    public Optional<String> summarize(final String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        final String prompt = "Summarize the following test log in 2-3 sentences. "
                + "Focus on what failed and why.\nLog:\n" + text;
        return call(prompt);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Executes a single HTTP POST to the AI endpoint with the given prompt.
     * All network and JSON errors are caught and return {@link Optional#empty()}.
     */
    private Optional<String> call(final String prompt) {
        try {
            final URL url = URI.create(apiUrl).toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            // Build minimal request body
            final ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            final ObjectNode message = mapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            body.putArray("messages").add(message);

            final byte[] payload = mapper.writeValueAsBytes(body);
            try (final OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }

            final int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("HttpAiAssistant: AI endpoint returned HTTP {} — treating as unavailable", status);
                return Optional.empty();
            }

            final JsonNode response = mapper.readTree(conn.getInputStream());
            // Attempt common response shapes: { "content": "..." } or OpenAI-style
            String content = null;
            if (response.has("content")) {
                content = response.get("content").asText();
            } else if (response.path("choices").isArray()
                    && response.path("choices").size() > 0) {
                content = response.path("choices").get(0)
                        .path("message").path("content").asText(null);
            }

            if (content == null || content.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(content.trim());

        } catch (final IOException e) {
            log.debug("HttpAiAssistant: network/IO error calling AI endpoint — degrading to no-op: {}",
                    e.getMessage());
            return Optional.empty();
        } catch (final Exception e) {
            log.warn("HttpAiAssistant: unexpected error — degrading to no-op", e);
            return Optional.empty();
        }
    }
}
