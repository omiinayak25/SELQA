package com.omiinqa.reports.export;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Writes a Markdown test-run summary to the GitHub Actions Step Summary file.
 *
 * <h2>GitHub Actions Integration</h2>
 * <p>When a job step runs inside GitHub Actions, the runner sets the environment
 * variable {@code GITHUB_STEP_SUMMARY} to a file path that the runner reads and
 * renders as Markdown in the workflow summary UI.  This class checks for that
 * variable at runtime:
 * <ul>
 *   <li>If set — the rendered Markdown is <em>appended</em> to the file so that
 *       multiple steps can contribute to the same summary page.</li>
 *   <li>If not set — no file I/O is performed; the Markdown string is still
 *       returned so callers can log or display it locally.</li>
 * </ul>
 *
 * <h2>Delegation</h2>
 * <p>Markdown generation is fully delegated to {@link MarkdownSummaryExporter}.
 * This class is responsible only for the file-append side-effect and the
 * environment-variable check.</p>
 *
 * <pre>
 *   // Inside a TestNG AfterSuite listener:
 *   GitHubStepSummaryWriter writer = new GitHubStepSummaryWriter();
 *   String markdown = writer.write(aggregator.buildResult(ctx));
 * </pre>
 *
 * @see MarkdownSummaryExporter
 * @see ReportExporterFacade
 */
public class GitHubStepSummaryWriter {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubStepSummaryWriter.class);

    /** GitHub Actions environment variable that points to the step-summary file. */
    static final String ENV_VAR = "GITHUB_STEP_SUMMARY";

    private final MarkdownSummaryExporter markdownExporter;

    /**
     * Creates a writer backed by a default {@link MarkdownSummaryExporter}.
     */
    public GitHubStepSummaryWriter() {
        this(new MarkdownSummaryExporter());
    }

    /**
     * Creates a writer backed by the supplied {@link MarkdownSummaryExporter}.
     *
     * @param markdownExporter the exporter to use for Markdown generation; must not be {@code null}
     */
    public GitHubStepSummaryWriter(final MarkdownSummaryExporter markdownExporter) {
        if (markdownExporter == null) {
            throw new IllegalArgumentException("markdownExporter must not be null");
        }
        this.markdownExporter = markdownExporter;
    }

    /**
     * Renders the test run as Markdown and, when running inside GitHub Actions,
     * appends that Markdown to the step-summary file.
     *
     * <p>The Markdown string is always returned regardless of whether the
     * environment variable is set, allowing callers to log it or write it to an
     * alternative destination.</p>
     *
     * @param result the test run data; must not be {@code null}
     * @return the rendered Markdown string (never {@code null})
     * @throws FrameworkException if the environment variable is set but the file
     *                            append fails
     */
    public String write(final TestRunResult result) {
        final String markdown = markdownExporter.toMarkdown(result);
        final String summaryFile = System.getenv(ENV_VAR);

        if (summaryFile != null && !summaryFile.isBlank()) {
            final Path path = Paths.get(summaryFile);
            try {
                Files.write(path, markdown.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                LOG.info("GitHub Step Summary written to: {}", path.toAbsolutePath());
            } catch (final IOException ex) {
                throw new FrameworkException(
                        "Failed to append Markdown to GitHub Step Summary file: " + summaryFile, ex);
            }
        } else {
            LOG.debug("GITHUB_STEP_SUMMARY not set — skipping file write.");
        }

        return markdown;
    }
}
