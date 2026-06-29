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
 * Exports a {@link TestRunResult} to well-formed XML using a hand-built
 * {@link StringBuilder} — no XML library dependency required.
 *
 * <h2>Output Structure</h2>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <testRun suiteName="..." startedAt="..." finishedAt="...">
 *   <summary passed="..." failed="..." skipped="..." totalDurationMs="..."/>
 *   <testCases>
 *     <testCase name="..." className="..." status="..." durationMs="...">
 *       <errorMessage>...</errorMessage>
 *     </testCase>
 *   </testCases>
 * </testRun>
 * }</pre>
 *
 * <h2>XML Escaping</h2>
 * <p>All attribute values and text content are passed through
 * {@link #xmlEscape(String)}, which escapes the five predefined XML entities
 * ({@code &amp; &lt; &gt; &quot; &apos;}).  Null values are rendered as
 * empty strings.</p>
 *
 * <h2>Strategy Pattern</h2>
 * <p>Plays the <em>ConcreteStrategy</em> role alongside {@link JsonResultExporter}
 * and {@link CsvResultExporter}.</p>
 *
 * @see ReportExporterFacade
 */
public class XmlResultExporter {

    private static final Logger LOG = LoggerFactory.getLogger(XmlResultExporter.class);

    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String INDENT   = "  ";

    /**
     * Converts the supplied {@link TestRunResult} to a well-formed XML string.
     *
     * @param result the test run data; must not be {@code null}
     * @return XML document string beginning with the XML declaration
     */
    public String toXml(final TestRunResult result) {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append(XML_DECL);

        // Root element
        sb.append("<testRun")
          .append(attr("suiteName",  result.getSuiteName()))
          .append(attr("startedAt",  String.valueOf(result.getStartedAt())))
          .append(attr("finishedAt", String.valueOf(result.getFinishedAt())))
          .append(">\n");

        // Summary element
        sb.append(INDENT)
          .append("<summary")
          .append(attr("passed",          String.valueOf(result.getTotalPassed())))
          .append(attr("failed",          String.valueOf(result.getTotalFailed())))
          .append(attr("skipped",         String.valueOf(result.getTotalSkipped())))
          .append(attr("totalDurationMs", String.valueOf(result.getTotalDurationMs())))
          .append("/>\n");

        // testCases wrapper
        sb.append(INDENT).append("<testCases>\n");

        final List<TestCaseResult> cases = result.getTestCases();
        if (cases != null) {
            for (final TestCaseResult tc : cases) {
                sb.append(INDENT).append(INDENT)
                  .append("<testCase")
                  .append(attr("name",       tc.getName()))
                  .append(attr("className",  tc.getClassName()))
                  .append(attr("status",     tc.getStatus()))
                  .append(attr("durationMs", String.valueOf(tc.getDurationMs())))
                  .append(">\n");

                // errorMessage child element (always present, possibly empty)
                sb.append(INDENT).append(INDENT).append(INDENT)
                  .append("<errorMessage>")
                  .append(xmlEscape(tc.getErrorMessage()))
                  .append("</errorMessage>\n");

                sb.append(INDENT).append(INDENT).append("</testCase>\n");
            }
        }

        sb.append(INDENT).append("</testCases>\n");
        sb.append("</testRun>\n");
        return sb.toString();
    }

    /**
     * Converts the supplied {@link TestRunResult} and writes the XML to {@code outputPath}.
     *
     * @param result     the test run data; must not be {@code null}
     * @param outputPath destination file path; must not be {@code null}
     * @throws FrameworkException if file I/O fails
     */
    public void writeToFile(final TestRunResult result, final Path outputPath) {
        final String xml = toXml(result);
        try {
            Files.writeString(outputPath, xml, StandardCharsets.UTF_8);
            LOG.info("XML report written to: {}", outputPath.toAbsolutePath());
        } catch (final IOException ex) {
            throw new FrameworkException("Failed to write XML report to " + outputPath, ex);
        }
    }

    /**
     * Escapes the five predefined XML entities in {@code value}.
     *
     * <table border="1">
     *   <caption>Escape table</caption>
     *   <tr><th>Raw</th><th>Escaped</th></tr>
     *   <tr><td>&amp;</td><td>&amp;amp;</td></tr>
     *   <tr><td>&lt;</td><td>&amp;lt;</td></tr>
     *   <tr><td>&gt;</td><td>&amp;gt;</td></tr>
     *   <tr><td>"</td><td>&amp;quot;</td></tr>
     *   <tr><td>'</td><td>&amp;apos;</td></tr>
     * </table>
     *
     * @param value raw string; {@code null} returns an empty string
     * @return XML-safe representation
     */
    String xmlEscape(final String value) {
        if (value == null) {
            return "";
        }
        // Process in a single pass over the character array for efficiency
        final StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:   sb.append(c);        break;
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Builds a single XML attribute string {@code  name="value"} (note leading space). */
    private String attr(final String name, final String value) {
        return " " + name + "=\"" + xmlEscape(value) + "\"";
    }
}
