package com.omiinqa.reports.export;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Coordinates all report exporters for a {@link TestRunResult}.
 *
 * <h2>Facade Pattern</h2>
 * <p>Hides the complexity of instantiating and invoking seven individual
 * exporters behind a single, cohesive API.  Callers need only:
 * <ol>
 *   <li>Obtain a {@code ReportExporterFacade} (via constructor or
 *       {@link #withDefaults()}).</li>
 *   <li>Call {@link #exportAll(TestRunResult)} to write JSON, CSV, XML, and
 *       Markdown files in one shot, <em>or</em> call the individual
 *       {@code export*()} methods to retrieve specific format strings.</li>
 * </ol>
 *
 * <h2>Error Handling</h2>
 * <p>{@link #exportAll(TestRunResult)} never propagates I/O exceptions to the
 * caller — each file write is wrapped in its own try/catch so that a failure
 * writing one format (e.g. disk full) does not prevent other formats from being
 * written.  Errors are logged at {@code ERROR} level; if signalling is
 * required, they are also thrown as {@link FrameworkException}.</p>
 *
 * <h2>Output Directory</h2>
 * <p>The output directory is created (including any missing parent directories)
 * the first time {@link #exportAll(TestRunResult)} is called.  File names are
 * fixed: {@code test-run.json}, {@code test-run.csv}, {@code test-run.xml},
 * {@code test-run.md}.</p>
 *
 * <pre>
 *   // Simplest usage
 *   ReportExporterFacade facade = ReportExporterFacade.withDefaults();
 *   facade.exportAll(result);
 *
 *   // Custom output directory
 *   ReportExporterFacade facade = new ReportExporterFacade("target/test-reports");
 *   String json = facade.exportJson(result);
 * </pre>
 *
 * @see JsonResultExporter
 * @see CsvResultExporter
 * @see XmlResultExporter
 * @see MarkdownSummaryExporter
 * @see SlackMessageBuilder
 * @see TeamsMessageBuilder
 * @see EmailSummaryBuilder
 */
public class ReportExporterFacade {

    private static final Logger LOG = LoggerFactory.getLogger(ReportExporterFacade.class);

    private static final String DEFAULT_OUTPUT_DIR = "reports/export";

    private final String outputDir;

    // Exporter instances — instantiated once and reused
    private final JsonResultExporter     jsonExporter     = new JsonResultExporter();
    private final CsvResultExporter      csvExporter      = new CsvResultExporter();
    private final XmlResultExporter      xmlExporter      = new XmlResultExporter();
    private final MarkdownSummaryExporter mdExporter      = new MarkdownSummaryExporter();
    private final SlackMessageBuilder    slackBuilder     = new SlackMessageBuilder();
    private final TeamsMessageBuilder    teamsBuilder     = new TeamsMessageBuilder();
    private final EmailSummaryBuilder    emailBuilder     = new EmailSummaryBuilder();

    /**
     * Creates a facade that writes files to {@code outputDir}.
     *
     * @param outputDir relative or absolute path of the directory where
     *                  {@link #exportAll(TestRunResult)} writes files;
     *                  must not be {@code null} or blank
     */
    public ReportExporterFacade(final String outputDir) {
        if (outputDir == null || outputDir.isBlank()) {
            throw new IllegalArgumentException("outputDir must not be null or blank");
        }
        this.outputDir = outputDir;
    }

    /**
     * Factory method that creates a facade using the default output directory
     * ({@value #DEFAULT_OUTPUT_DIR}).
     *
     * @return a new {@code ReportExporterFacade} targeting the default directory
     */
    public static ReportExporterFacade withDefaults() {
        return new ReportExporterFacade(DEFAULT_OUTPUT_DIR);
    }

    /**
     * Writes JSON, CSV, XML, and Markdown reports to the configured output directory.
     *
     * <p>File names: {@code test-run.json}, {@code test-run.csv},
     * {@code test-run.xml}, {@code test-run.md}. The output directory and any
     * missing parents are created automatically.</p>
     *
     * <p>Each format is exported independently; a failure in one format is
     * logged at {@code ERROR} level and does not abort the remaining exports.</p>
     *
     * @param result the test run data to export; must not be {@code null}
     */
    public void exportAll(final TestRunResult result) {
        final Path dir = Paths.get(outputDir);
        try {
            Files.createDirectories(dir);
        } catch (final IOException ex) {
            final String msg = "Failed to create export directory: " + dir.toAbsolutePath();
            LOG.error(msg, ex);
            throw new FrameworkException(msg, ex);
        }

        writeQuietly(dir.resolve("test-run.json"), () -> jsonExporter.writeToFile(result, dir.resolve("test-run.json")));
        writeQuietly(dir.resolve("test-run.csv"),  () -> csvExporter.writeToFile(result,  dir.resolve("test-run.csv")));
        writeQuietly(dir.resolve("test-run.xml"),  () -> xmlExporter.writeToFile(result,  dir.resolve("test-run.xml")));
        writeQuietly(dir.resolve("test-run.md"),   () -> mdExporter.writeToFile(result,   dir.resolve("test-run.md")));
    }

    /**
     * Serialises the result to JSON and returns it as a string.
     *
     * @param result the test run data; must not be {@code null}
     * @return pretty-printed JSON string
     */
    public String exportJson(final TestRunResult result) {
        return jsonExporter.toJson(result);
    }

    /**
     * Serialises the result to CSV and returns it as a string.
     *
     * @param result the test run data; must not be {@code null}
     * @return RFC-4180 CSV string with header row
     */
    public String exportCsv(final TestRunResult result) {
        return csvExporter.toCsv(result);
    }

    /**
     * Serialises the result to XML and returns it as a string.
     *
     * @param result the test run data; must not be {@code null}
     * @return well-formed XML string
     */
    public String exportXml(final TestRunResult result) {
        return xmlExporter.toXml(result);
    }

    /**
     * Renders the result as GitHub-Flavored Markdown and returns the string.
     *
     * @param result the test run data; must not be {@code null}
     * @return Markdown string
     */
    public String exportMarkdown(final TestRunResult result) {
        return mdExporter.toMarkdown(result);
    }

    /**
     * Builds a Slack Block Kit JSON payload for the result.
     *
     * @param result the test run data; must not be {@code null}
     * @return Slack Block Kit JSON string
     */
    public String exportSlack(final TestRunResult result) {
        return slackBuilder.build(result);
    }

    /**
     * Builds a Microsoft Teams MessageCard JSON payload for the result.
     *
     * @param result the test run data; must not be {@code null}
     * @return Teams MessageCard JSON string
     */
    public String exportTeams(final TestRunResult result) {
        return teamsBuilder.build(result);
    }

    /**
     * Builds an HTML email body for the result.
     *
     * @param result the test run data; must not be {@code null}
     * @return self-contained HTML string
     */
    public String exportEmail(final TestRunResult result) {
        return emailBuilder.buildHtml(result);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Executes the supplied write action; logs any {@link Exception} at ERROR
     * level without re-throwing, so one format failure cannot block others.
     */
    private void writeQuietly(final Path target, final Runnable action) {
        try {
            action.run();
            LOG.info("Exported: {}", target.toAbsolutePath());
        } catch (final Exception ex) {
            LOG.error("Failed to export {}: {}", target.toAbsolutePath(), ex.getMessage(), ex);
        }
    }
}
