package com.omiinqa.reports.export;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports a {@link TestRunResult} to GitHub-Flavored Markdown (GFM).
 *
 * <h2>Output Structure</h2>
 * <ol>
 *   <li>H1 heading: {@code # Test Run Summary}</li>
 *   <li>Suite name as bold text</li>
 *   <li>Totals table: Passed / Failed / Skipped / Total / Duration (ms)</li>
 *   <li>Per-test GFM table: Test Name | Class | Status | Duration (ms) | Error</li>
 * </ol>
 *
 * <p>Status values are plain text labels ({@code PASSED}, {@code FAILED},
 * {@code SKIPPED}) — no emoji, ensuring compatibility with all Markdown renderers
 * and plain-text readers.</p>
 *
 * <h2>Strategy Pattern</h2>
 * <p>Plays the <em>ConcreteStrategy</em> role in the exporter family.
 * {@link GitHubStepSummaryWriter} and {@link ReportExporterFacade} both delegate
 * to this class for Markdown generation.</p>
 *
 * @see GitHubStepSummaryWriter
 * @see ReportExporterFacade
 */
public class MarkdownSummaryExporter {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownSummaryExporter.class);

    /**
     * Converts the supplied {@link TestRunResult} to a GFM Markdown string.
     *
     * @param result the test run data; must not be {@code null}
     * @return non-null, non-empty Markdown string
     */
    public String toMarkdown(final TestRunResult result) {
        final int total = result.getTotalPassed() + result.getTotalFailed() + result.getTotalSkipped();
        final StringBuilder sb = new StringBuilder(512);

        sb.append("# Test Run Summary\n\n");
        sb.append("**Suite:** ").append(mdEscape(result.getSuiteName())).append("\n\n");

        // Totals table
        sb.append("## Totals\n\n");
        sb.append("| Passed | Failed | Skipped | Total | Duration (ms) |\n");
        sb.append("|--------|--------|---------|-------|---------------|\n");
        sb.append("| ").append(result.getTotalPassed())
          .append(" | ").append(result.getTotalFailed())
          .append(" | ").append(result.getTotalSkipped())
          .append(" | ").append(total)
          .append(" | ").append(result.getTotalDurationMs())
          .append(" |\n\n");

        // Per-test table
        sb.append("## Test Cases\n\n");
        sb.append("| Test Name | Class | Status | Duration (ms) | Error |\n");
        sb.append("|-----------|-------|--------|---------------|-------|\n");

        final List<TestCaseResult> cases = result.getTestCases();
        if (cases != null) {
            for (final TestCaseResult tc : cases) {
                sb.append("| ").append(mdEscape(tc.getName()))
                  .append(" | ").append(mdEscape(tc.getClassName()))
                  .append(" | ").append(tc.getStatus())
                  .append(" | ").append(tc.getDurationMs())
                  .append(" | ").append(mdEscape(tc.getErrorMessage()))
                  .append(" |\n");
            }
        }

        return sb.toString();
    }

    /**
     * Converts the supplied {@link TestRunResult} and writes the Markdown to {@code outputPath}.
     *
     * @param result     the test run data; must not be {@code null}
     * @param outputPath destination file path; must not be {@code null}
     * @throws FrameworkException if file I/O fails
     */
    public void writeToFile(final TestRunResult result, final Path outputPath) {
        final String md = toMarkdown(result);
        try {
            Files.writeString(outputPath, md, StandardCharsets.UTF_8);
            LOG.info("Markdown report written to: {}", outputPath.toAbsolutePath());
        } catch (final IOException ex) {
            throw new FrameworkException("Failed to write Markdown report to " + outputPath, ex);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Escapes pipe characters so they do not break GFM table cells, and returns
     * an empty string for {@code null} inputs.
     *
     * @param value raw string, may be {@code null}
     * @return Markdown-safe string
     */
    private static String mdEscape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|");
    }
}
