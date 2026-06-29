package com.omiinqa.reports.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Builds a self-contained HTML email body for a {@link TestRunResult}.
 *
 * <h2>Builder Pattern</h2>
 * <p>Produces a single-method surface — {@link #buildHtml(TestRunResult)} —
 * that assembles a complete {@code <html>...</html>} document with inline
 * styles.  No external template engine or CSS framework is required; the
 * output is intentionally simple to maximise compatibility with corporate
 * email clients (Outlook, Gmail, Apple Mail) that strip external stylesheets.</p>
 *
 * <h2>Inline Styles</h2>
 * <ul>
 *   <li>Passed count cell — green background ({@code #DFF0D8})</li>
 *   <li>Failed count cell — red background ({@code #F2DEDE})</li>
 *   <li>Skipped count cell — yellow background ({@code #FCF8E3})</li>
 *   <li>PASSED status cell — green text</li>
 *   <li>FAILED status cell — red text, bold</li>
 *   <li>SKIPPED status cell — grey text</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 *   EmailSummaryBuilder builder = new EmailSummaryBuilder();
 *   String html = builder.buildHtml(result);
 *   // pass html to your JavaMail / SMTP helper as the message body
 * </pre>
 *
 * @see ReportExporterFacade
 */
public class EmailSummaryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(EmailSummaryBuilder.class);

    // Inline style constants
    private static final String STYLE_TABLE =
            "border-collapse:collapse;width:100%;font-family:Arial,sans-serif;font-size:14px;";
    private static final String STYLE_TH =
            "background:#4A4A4A;color:#fff;padding:8px 12px;text-align:left;";
    private static final String STYLE_TD =
            "padding:8px 12px;border-bottom:1px solid #ddd;";
    private static final String STYLE_TD_PASS = STYLE_TD + "color:#2E7D32;";
    private static final String STYLE_TD_FAIL = STYLE_TD + "color:#C62828;font-weight:bold;";
    private static final String STYLE_TD_SKIP = STYLE_TD + "color:#757575;";

    private static final String BOX_PASS =
            "display:inline-block;padding:10px 20px;background:#DFF0D8;color:#3C763D;"
            + "font-size:18px;font-weight:bold;border-radius:4px;margin:4px;";
    private static final String BOX_FAIL =
            "display:inline-block;padding:10px 20px;background:#F2DEDE;color:#A94442;"
            + "font-size:18px;font-weight:bold;border-radius:4px;margin:4px;";
    private static final String BOX_SKIP =
            "display:inline-block;padding:10px 20px;background:#FCF8E3;color:#8A6D3B;"
            + "font-size:18px;font-weight:bold;border-radius:4px;margin:4px;";

    /**
     * Builds a self-contained HTML email body for the supplied {@link TestRunResult}.
     *
     * @param result the test run data; must not be {@code null}
     * @return a complete {@code <html>...</html>} document string with inline styles
     */
    public String buildHtml(final TestRunResult result) {
        final StringBuilder sb = new StringBuilder(2048);

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\"/>\n")
          .append("<title>Test Run Report</title>\n")
          .append("</head>\n<body style=\"margin:0;padding:20px;background:#f5f5f5;")
          .append("font-family:Arial,sans-serif;\">\n");

        // Header banner
        sb.append("<div style=\"background:#1565C0;color:#fff;padding:16px 24px;")
          .append("border-radius:4px 4px 0 0;\">\n")
          .append("<h1 style=\"margin:0;font-size:22px;\">Test Run Summary</h1>\n")
          .append("<p style=\"margin:4px 0 0;font-size:14px;\">Suite: ")
          .append(htmlEscape(result.getSuiteName()))
          .append("</p>\n</div>\n");

        // Summary boxes
        sb.append("<div style=\"background:#fff;padding:16px 24px;border:1px solid #ddd;\">\n");
        sb.append("<div style=\"").append(BOX_PASS).append("\">")
          .append("Passed: ").append(result.getTotalPassed()).append("</div>\n");
        sb.append("<div style=\"").append(BOX_FAIL).append("\">")
          .append("Failed: ").append(result.getTotalFailed()).append("</div>\n");
        sb.append("<div style=\"").append(BOX_SKIP).append("\">")
          .append("Skipped: ").append(result.getTotalSkipped()).append("</div>\n");
        sb.append("<p style=\"font-size:13px;color:#555;margin-top:12px;\">")
          .append("Total duration: ").append(result.getTotalDurationMs()).append(" ms")
          .append("</p>\n</div>\n");

        // Per-test table
        sb.append("<div style=\"background:#fff;padding:0 24px 16px;border:1px solid #ddd;")
          .append("border-top:none;\">\n");
        sb.append("<h2 style=\"font-size:16px;color:#333;padding-top:12px;\">Test Cases</h2>\n");
        sb.append("<table style=\"").append(STYLE_TABLE).append("\">\n");
        sb.append("<thead><tr>\n");
        th(sb, "Test Name");
        th(sb, "Class");
        th(sb, "Status");
        th(sb, "Duration (ms)");
        th(sb, "Error");
        sb.append("</tr></thead>\n<tbody>\n");

        final List<TestCaseResult> cases = result.getTestCases();
        if (cases != null) {
            for (final TestCaseResult tc : cases) {
                sb.append("<tr>\n");
                td(sb, htmlEscape(tc.getName()),      STYLE_TD);
                td(sb, htmlEscape(tc.getClassName()), STYLE_TD);
                td(sb, statusCell(tc.getStatus()), statusStyle(tc.getStatus()));
                td(sb, String.valueOf(tc.getDurationMs()), STYLE_TD);
                td(sb, htmlEscape(tc.getErrorMessage()),   STYLE_TD);
                sb.append("</tr>\n");
            }
        }

        sb.append("</tbody>\n</table>\n</div>\n");
        sb.append("</body>\n</html>\n");

        LOG.debug("HTML email body built ({} chars)", sb.length());
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static void th(final StringBuilder sb, final String text) {
        sb.append("<th style=\"").append(STYLE_TH).append("\">").append(text).append("</th>\n");
    }

    private static void td(final StringBuilder sb, final String text, final String style) {
        sb.append("<td style=\"").append(style).append("\">")
          .append(text == null ? "" : text).append("</td>\n");
    }

    private static String statusStyle(final String status) {
        if (status == null) { return STYLE_TD; }
        switch (status) {
            case "PASSED":  return STYLE_TD_PASS;
            case "FAILED":  return STYLE_TD_FAIL;
            case "SKIPPED": return STYLE_TD_SKIP;
            default:        return STYLE_TD;
        }
    }

    private static String statusCell(final String status) {
        return status == null ? "" : status;
    }

    /**
     * Escapes the four HTML special characters so that user-supplied strings
     * cannot inject markup into the email body.
     *
     * @param value raw string; {@code null} returns empty string
     * @return HTML-safe string
     */
    private static String htmlEscape(final String value) {
        if (value == null) { return ""; }
        return value
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;");
    }
}
