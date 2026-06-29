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
 * Exports a {@link TestRunResult} to RFC-4180-compliant CSV.
 *
 * <h2>RFC-4180 Compliance</h2>
 * <p>Fields that contain a comma, double-quote, or newline character are
 * wrapped in double-quotes. Literal double-quote characters within such
 * fields are escaped by doubling them ({@code ""}).  The header row is always
 * present as the first line.</p>
 *
 * <p>Column order: {@code testName,className,status,durationMs,errorMessage}</p>
 *
 * <h2>Strategy Pattern</h2>
 * <p>Plays the <em>ConcreteStrategy</em> role in the exporter family alongside
 * {@link JsonResultExporter} and {@link XmlResultExporter}.</p>
 *
 * <pre>
 *   CsvResultExporter exporter = new CsvResultExporter();
 *   String csv = exporter.toCsv(result);
 *   exporter.writeToFile(result, Path.of("reports/test-run.csv"));
 * </pre>
 *
 * @see ReportExporterFacade
 */
public class CsvResultExporter {

    private static final Logger LOG = LoggerFactory.getLogger(CsvResultExporter.class);

    private static final String HEADER = "testName,className,status,durationMs,errorMessage";
    private static final String CRLF   = "\r\n";

    /**
     * Converts the supplied {@link TestRunResult} to a RFC-4180 CSV string.
     *
     * @param result the test run data; must not be {@code null}
     * @return CSV string with header row followed by one row per test case
     */
    public String toCsv(final TestRunResult result) {
        final StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(CRLF);

        final List<TestCaseResult> cases = result.getTestCases();
        if (cases != null) {
            for (final TestCaseResult tc : cases) {
                sb.append(escapeField(tc.getName())).append(',')
                  .append(escapeField(tc.getClassName())).append(',')
                  .append(escapeField(tc.getStatus())).append(',')
                  .append(tc.getDurationMs()).append(',')
                  .append(escapeField(tc.getErrorMessage()))
                  .append(CRLF);
            }
        }
        return sb.toString();
    }

    /**
     * Converts the supplied {@link TestRunResult} and writes the CSV to {@code outputPath}.
     *
     * @param result     the test run data; must not be {@code null}
     * @param outputPath destination file path; must not be {@code null}
     * @throws FrameworkException if file I/O fails
     */
    public void writeToFile(final TestRunResult result, final Path outputPath) {
        final String csv = toCsv(result);
        try {
            Files.writeString(outputPath, csv, StandardCharsets.UTF_8);
            LOG.info("CSV report written to: {}", outputPath.toAbsolutePath());
        } catch (final IOException ex) {
            throw new FrameworkException("Failed to write CSV report to " + outputPath, ex);
        }
    }

    /**
     * Applies RFC-4180 quoting rules to a single field value.
     *
     * <ul>
     *   <li>If {@code value} is {@code null}, returns an empty string.</li>
     *   <li>If {@code value} contains a comma, double-quote, or newline, the
     *       entire value is wrapped in double-quotes and any embedded
     *       double-quotes are doubled.</li>
     *   <li>Otherwise, the value is returned as-is.</li>
     * </ul>
     *
     * @param value the raw field value, may be {@code null}
     * @return RFC-4180-safe representation of the value
     */
    String escapeField(final String value) {
        if (value == null) {
            return "";
        }
        // Check whether quoting is required
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        // Wrap in quotes, doubling embedded double-quotes
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
