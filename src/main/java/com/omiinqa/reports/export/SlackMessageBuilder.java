package com.omiinqa.reports.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a Slack Block Kit JSON payload for a {@link TestRunResult}.
 *
 * <h2>Builder Pattern</h2>
 * <p>Encapsulates the Block Kit message structure so callers need only call
 * {@link #build(TestRunResult)} — all JSON assembly is hidden behind this
 * facade.  Uses Jackson's {@link ObjectNode}/{@link ArrayNode} API rather than
 * string concatenation to guarantee well-formed JSON.</p>
 *
 * <h2>Message Structure</h2>
 * <ol>
 *   <li><strong>Header block</strong> — plain-text header with the suite name.</li>
 *   <li><strong>Section block</strong> — two-column field list: Passed / Failed /
 *       Skipped / Duration.</li>
 *   <li><strong>Divider + Context block</strong> (only when failures exist) —
 *       lists the names of the first five failed test cases.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 *   SlackMessageBuilder builder = new SlackMessageBuilder();
 *   String payload = builder.build(result);
 *   // POST payload to a Slack incoming-webhook URL
 * </pre>
 *
 * @see ReportExporterFacade
 */
public class SlackMessageBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SlackMessageBuilder.class);
    private static final int MAX_FAILED_LIST = 5;

    private final ObjectMapper mapper;

    /**
     * Creates a builder backed by a default {@link ObjectMapper}.
     */
    public SlackMessageBuilder() {
        this(new ObjectMapper());
    }

    /**
     * Creates a builder backed by the supplied {@link ObjectMapper}.
     *
     * @param mapper pre-configured Jackson mapper; must not be {@code null}
     */
    public SlackMessageBuilder(final ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.mapper = mapper;
    }

    /**
     * Builds and returns the Slack Block Kit JSON string for the given result.
     *
     * @param result the test run data; must not be {@code null}
     * @return JSON string suitable for posting to a Slack incoming-webhook endpoint
     * @throws FrameworkException if Jackson serialisation fails
     */
    public String build(final TestRunResult result) {
        try {
            final ObjectNode root   = mapper.createObjectNode();
            final ArrayNode  blocks = root.putArray("blocks");

            // 1. Header block
            final ObjectNode header = blocks.addObject();
            header.put("type", "header");
            header.putObject("text")
                  .put("type", "plain_text")
                  .put("text", "Test Run: " + nullSafe(result.getSuiteName()))
                  .put("emoji", false);

            // 2. Section block with fields
            final ObjectNode section = blocks.addObject();
            section.put("type", "section");
            final ArrayNode fields = section.putArray("fields");

            addMarkdownField(fields, "*Passed*", String.valueOf(result.getTotalPassed()));
            addMarkdownField(fields, "*Failed*",  String.valueOf(result.getTotalFailed()));
            addMarkdownField(fields, "*Skipped*", String.valueOf(result.getTotalSkipped()));
            addMarkdownField(fields, "*Duration*", result.getTotalDurationMs() + " ms");

            // 3. Optional: divider + failed test list
            if (result.getTotalFailed() > 0 && result.getTestCases() != null) {
                // Divider
                blocks.addObject().put("type", "divider");

                // Collect failed test names (up to MAX_FAILED_LIST)
                final List<String> failedNames = result.getTestCases().stream()
                        .filter(tc -> "FAILED".equals(tc.getStatus()))
                        .limit(MAX_FAILED_LIST)
                        .map(TestCaseResult::getName)
                        .collect(Collectors.toList());

                final String contextText = "Failed tests: " + String.join(", ", failedNames)
                        + (result.getTotalFailed() > MAX_FAILED_LIST
                           ? " (+" + (result.getTotalFailed() - MAX_FAILED_LIST) + " more)" : "");

                final ObjectNode context = blocks.addObject();
                context.put("type", "context");
                final ArrayNode elements = context.putArray("elements");
                elements.addObject()
                        .put("type", "mrkdwn")
                        .put("text", contextText);
            }

            final String json = mapper.writeValueAsString(root);
            LOG.debug("Slack payload built ({} chars)", json.length());
            return json;
        } catch (final Exception ex) {
            throw new FrameworkException("Failed to build Slack Block Kit JSON", ex);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static void addMarkdownField(final ArrayNode fields,
                                         final String label,
                                         final String value) {
        fields.addObject()
              .put("type", "mrkdwn")
              .put("text", label + "\n" + value);
    }

    private static String nullSafe(final String value) {
        return value == null ? "" : value;
    }
}
