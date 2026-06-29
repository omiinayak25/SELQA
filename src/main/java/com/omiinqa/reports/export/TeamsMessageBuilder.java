package com.omiinqa.reports.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a Microsoft Teams MessageCard JSON payload for a {@link TestRunResult}.
 *
 * <h2>Builder Pattern</h2>
 * <p>Encapsulates the legacy Office 365 Connector Card (MessageCard) format so
 * callers need only call {@link #build(TestRunResult)}.  Uses Jackson's
 * {@link ObjectNode}/{@link ArrayNode} API to ensure well-formed JSON without
 * any string-concatenation fragility.</p>
 *
 * <h2>MessageCard Format</h2>
 * <p>Targets the legacy {@code MessageCard} schema
 * ({@code https://schema.org/extensions}) which is universally supported by
 * Microsoft Teams incoming webhook connectors without additional OAuth setup.
 * Fields produced:
 * <ul>
 *   <li>{@code @type} — always {@code "MessageCard"}</li>
 *   <li>{@code @context} — schema URI</li>
 *   <li>{@code summary} — brief one-line summary</li>
 *   <li>{@code themeColor} — {@code "00B050"} (green) when all tests pass,
 *       {@code "FF0000"} (red) when any fail</li>
 *   <li>{@code title} — suite name</li>
 *   <li>{@code sections} — one section with a {@code facts} list</li>
 * </ul>
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   TeamsMessageBuilder builder = new TeamsMessageBuilder();
 *   String payload = builder.build(result);
 *   // POST payload to a Microsoft Teams incoming webhook URL
 * </pre>
 *
 * @see ReportExporterFacade
 */
public class TeamsMessageBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsMessageBuilder.class);

    private static final String SCHEMA_URI   = "https://schema.org/extensions";
    private static final String COLOR_GREEN  = "00B050";
    private static final String COLOR_RED    = "FF0000";

    private final ObjectMapper mapper;

    /**
     * Creates a builder backed by a default {@link ObjectMapper}.
     */
    public TeamsMessageBuilder() {
        this(new ObjectMapper());
    }

    /**
     * Creates a builder backed by the supplied {@link ObjectMapper}.
     *
     * @param mapper pre-configured Jackson mapper; must not be {@code null}
     */
    public TeamsMessageBuilder(final ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.mapper = mapper;
    }

    /**
     * Builds and returns the Teams MessageCard JSON string for the given result.
     *
     * @param result the test run data; must not be {@code null}
     * @return JSON string suitable for posting to a Teams incoming-webhook connector URL
     * @throws FrameworkException if Jackson serialisation fails
     */
    public String build(final TestRunResult result) {
        try {
            final ObjectNode root = mapper.createObjectNode();

            root.put("@type",      "MessageCard");
            root.put("@context",   SCHEMA_URI);
            root.put("summary",    "Test run: " + nullSafe(result.getSuiteName()));
            root.put("themeColor", result.getTotalFailed() > 0 ? COLOR_RED : COLOR_GREEN);
            root.put("title",      "Test Run: " + nullSafe(result.getSuiteName()));

            // Sections array with a single section containing facts
            final ArrayNode  sections = root.putArray("sections");
            final ObjectNode section  = sections.addObject();
            section.put("activityTitle", "Run Summary");

            final ArrayNode facts = section.putArray("facts");
            addFact(facts, "Passed",       String.valueOf(result.getTotalPassed()));
            addFact(facts, "Failed",       String.valueOf(result.getTotalFailed()));
            addFact(facts, "Skipped",      String.valueOf(result.getTotalSkipped()));
            addFact(facts, "Duration (ms)", String.valueOf(result.getTotalDurationMs()));
            addFact(facts, "Started At",   String.valueOf(result.getStartedAt()));
            addFact(facts, "Finished At",  String.valueOf(result.getFinishedAt()));

            final String json = mapper.writeValueAsString(root);
            LOG.debug("Teams payload built ({} chars)", json.length());
            return json;
        } catch (final Exception ex) {
            throw new FrameworkException("Failed to build Teams MessageCard JSON", ex);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static void addFact(final ArrayNode facts, final String name, final String value) {
        final ObjectNode fact = facts.addObject();
        fact.put("name",  name);
        fact.put("value", value);
    }

    private static String nullSafe(final String value) {
        return value == null ? "" : value;
    }
}
