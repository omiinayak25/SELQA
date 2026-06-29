package com.omiinqa.reports.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline, deterministic unit tests for the {@code com.omiinqa.reports.export}
 * multi-format reporting package.
 *
 * <p>Every test runs without a browser, database, or network connection — they
 * form part of the {@code unit} smoke gate and the {@code reporting} suite. A
 * single, fixed {@link TestRunResult} fixture is built once in {@link #buildFixture()}
 * so that asserted output is fully reproducible (timestamps are hard-coded, never
 * sourced from the clock).</p>
 *
 * <p>Each exporter is verified for correctness:
 * <ul>
 *   <li>JSON — parses back via Jackson and exposes expected fields.</li>
 *   <li>CSV — header row, row count, RFC-4180 quoting and quote-doubling.</li>
 *   <li>XML — well-formed, escapes the five predefined entities.</li>
 *   <li>Markdown — contains totals and per-test rows.</li>
 *   <li>Slack / Teams — JSON parses and carries the key structural fields.</li>
 *   <li>Email — produces a self-contained HTML document.</li>
 *   <li>GitHubStepSummaryWriter — returns non-empty Markdown when the env var is unset.</li>
 *   <li>Facade — exposes every format and writes the four expected files.</li>
 * </ul>
 * </p>
 *
 * <p>This class intentionally does <strong>not</strong> extend {@code BaseTest}:
 * no driver lifecycle is required.</p>
 */
public class ReportExportTest {

    private static final String[] GROUPS = {"reporting", "unit"};

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Deterministic suite-level fixture shared by all tests. */
    private static TestRunResult fixture;

    @BeforeClass
    public void buildFixture() {
        final List<TestCaseResult> cases = new ArrayList<>();
        cases.add(TestCaseResult.builder()
                .name("loginTest").className("com.omiinqa.LoginTest")
                .status("PASSED").durationMs(1200L).errorMessage(null).build());
        cases.add(TestCaseResult.builder()
                .name("logoutTest").className("com.omiinqa.LoginTest")
                .status("PASSED").durationMs(800L).errorMessage(null).build());
        cases.add(TestCaseResult.builder()
                .name("checkoutFails").className("com.omiinqa.CheckoutTest")
                .status("FAILED").durationMs(2300L).errorMessage("Element not found").build());
        cases.add(TestCaseResult.builder()
                .name("skippedTest").className("com.omiinqa.MiscTest")
                .status("SKIPPED").durationMs(0L).errorMessage(null).build());

        final Map<String, String> env = new LinkedHashMap<>();
        env.put("env", "staging");
        env.put("browser", "chrome");

        fixture = TestRunResult.builder()
                .suiteName("OmiinQA Regression Suite")
                .totalPassed(2).totalFailed(1).totalSkipped(1)
                .totalDurationMs(4300L)
                .testCases(cases)
                .environmentMetadata(env)
                .startedAt(1700000000000L)
                .finishedAt(1700000004300L)
                .build();
    }

    // -----------------------------------------------------------------------
    // JSON
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void jsonExporterProducesValidJson() throws Exception {
        final String json = new JsonResultExporter().toJson(fixture);
        final JsonNode node = MAPPER.readTree(json);
        assertThat(node).isNotNull();
        assertThat(node.isObject()).isTrue();
    }

    @Test(groups = {"reporting", "unit"})
    public void jsonContainsSuiteName() throws Exception {
        final JsonNode node = MAPPER.readTree(new JsonResultExporter().toJson(fixture));
        assertThat(node.get("suiteName").asText()).isEqualTo("OmiinQA Regression Suite");
    }

    @Test(groups = {"reporting", "unit"})
    public void jsonContainsTotals() throws Exception {
        final JsonNode node = MAPPER.readTree(new JsonResultExporter().toJson(fixture));
        assertThat(node.get("totalPassed").asInt()).isEqualTo(2);
        assertThat(node.get("totalFailed").asInt()).isEqualTo(1);
        assertThat(node.get("totalSkipped").asInt()).isEqualTo(1);
        assertThat(node.get("totalDurationMs").asLong()).isEqualTo(4300L);
    }

    @Test(groups = {"reporting", "unit"})
    public void jsonContainsAllTestCases() throws Exception {
        final JsonNode node = MAPPER.readTree(new JsonResultExporter().toJson(fixture));
        assertThat(node.get("testCases").isArray()).isTrue();
        assertThat(node.get("testCases").size()).isEqualTo(4);
    }

    @Test(groups = {"reporting", "unit"})
    public void jsonContainsFailedTestErrorMessage() throws Exception {
        final JsonNode node = MAPPER.readTree(new JsonResultExporter().toJson(fixture));
        String foundError = null;
        for (final JsonNode tc : node.get("testCases")) {
            if ("FAILED".equals(tc.get("status").asText())) {
                foundError = tc.get("errorMessage").asText();
            }
        }
        assertThat(foundError).isEqualTo("Element not found");
    }

    @Test(groups = {"reporting", "unit"})
    public void jsonRoundTripsThroughObjectMapper() throws Exception {
        final String json = new JsonResultExporter().toJson(fixture);
        final TestRunResult back = MAPPER.readValue(json, TestRunResult.class);
        assertThat(back.getSuiteName()).isEqualTo(fixture.getSuiteName());
        assertThat(back.getTestCases()).hasSize(4);
    }

    // -----------------------------------------------------------------------
    // CSV
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void csvHasHeaderRow() {
        final String csv = new CsvResultExporter().toCsv(fixture);
        final String firstLine = csv.split("\\r\\n", 2)[0];
        assertThat(firstLine).isEqualTo("testName,className,status,durationMs,errorMessage");
    }

    @Test(groups = {"reporting", "unit"})
    public void csvHasHeaderPlusOneRowPerTest() {
        final String csv = new CsvResultExporter().toCsv(fixture);
        final String[] lines = csv.split("\\r\\n");
        // 1 header + 4 data rows
        assertThat(lines).hasSize(5);
    }

    @Test(groups = {"reporting", "unit"})
    public void csvQuotesFieldsContainingCommas() {
        final TestRunResult withComma = singleCaseRun(
                TestCaseResult.builder().name("t").className("C").status("FAILED")
                        .durationMs(10L).errorMessage("expected a, but got b").build());
        final String csv = new CsvResultExporter().toCsv(withComma);
        assertThat(csv).contains("\"expected a, but got b\"");
    }

    @Test(groups = {"reporting", "unit"})
    public void csvDoublesEmbeddedDoubleQuotes() {
        final TestRunResult withQuote = singleCaseRun(
                TestCaseResult.builder().name("t").className("C").status("FAILED")
                        .durationMs(10L).errorMessage("got \"weird\" value").build());
        final String csv = new CsvResultExporter().toCsv(withQuote);
        // Embedded quotes doubled and whole field wrapped in quotes
        assertThat(csv).contains("\"got \"\"weird\"\" value\"");
    }

    @Test(groups = {"reporting", "unit"})
    public void csvLeavesPlainFieldsUnquoted() {
        final String csv = new CsvResultExporter().toCsv(fixture);
        assertThat(csv).contains("loginTest,com.omiinqa.LoginTest,PASSED,1200,");
    }

    // -----------------------------------------------------------------------
    // XML
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void xmlIsWellFormedShape() {
        final String xml = new XmlResultExporter().toXml(fixture);
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<testRun");
        assertThat(xml).contains("</testRun>");
    }

    @Test(groups = {"reporting", "unit"})
    public void xmlContainsSuiteNameAttribute() {
        final String xml = new XmlResultExporter().toXml(fixture);
        assertThat(xml).contains("suiteName=\"OmiinQA Regression Suite\"");
    }

    @Test(groups = {"reporting", "unit"})
    public void xmlContainsSummaryElementWithCounts() {
        final String xml = new XmlResultExporter().toXml(fixture);
        assertThat(xml).contains("<summary");
        assertThat(xml).contains("passed=\"2\"");
        assertThat(xml).contains("failed=\"1\"");
        assertThat(xml).contains("skipped=\"1\"");
    }

    @Test(groups = {"reporting", "unit"})
    public void xmlEscapesSpecialCharacters() {
        final TestRunResult tricky = singleCaseRun(
                TestCaseResult.builder().name("t").className("C").status("FAILED")
                        .durationMs(10L).errorMessage("a & b < c > d \" e ' f").build());
        final String xml = new XmlResultExporter().toXml(tricky);
        assertThat(xml).contains("a &amp; b &lt; c &gt; d &quot; e &apos; f");
        // Raw special characters must not leak into the text content
        assertThat(xml).doesNotContain("a & b < c > d");
    }

    @Test(groups = {"reporting", "unit"})
    public void xmlContainsTestCaseElements() {
        final String xml = new XmlResultExporter().toXml(fixture);
        assertThat(xml).contains("<testCase");
        assertThat(xml).contains("name=\"checkoutFails\"");
        assertThat(xml).contains("status=\"FAILED\"");
    }

    // -----------------------------------------------------------------------
    // Markdown
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void markdownHasSuiteHeader() {
        final String md = new MarkdownSummaryExporter().toMarkdown(fixture);
        assertThat(md).contains("# Test Run Summary");
        assertThat(md).contains("OmiinQA Regression Suite");
    }

    @Test(groups = {"reporting", "unit"})
    public void markdownHasTotalsTable() {
        final String md = new MarkdownSummaryExporter().toMarkdown(fixture);
        assertThat(md).contains("| Passed | Failed | Skipped |");
        assertThat(md).contains("| 2 | 1 | 1 |");
    }

    @Test(groups = {"reporting", "unit"})
    public void markdownHasRowForFailedTest() {
        final String md = new MarkdownSummaryExporter().toMarkdown(fixture);
        assertThat(md).contains("checkoutFails");
        assertThat(md).contains("Element not found");
    }

    @Test(groups = {"reporting", "unit"})
    public void markdownContainsAllStatuses() {
        final String md = new MarkdownSummaryExporter().toMarkdown(fixture);
        assertThat(md).contains("PASSED").contains("FAILED").contains("SKIPPED");
    }

    // -----------------------------------------------------------------------
    // GitHub Step Summary
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void githubStepSummaryReturnsMarkdownWhenEnvUnset() {
        // The test environment does not set GITHUB_STEP_SUMMARY, so no file write
        // occurs and the Markdown string is simply returned.
        assertThat(System.getenv(GitHubStepSummaryWriter.ENV_VAR)).isNull();
        final String md = new GitHubStepSummaryWriter().write(fixture);
        assertThat(md).isNotBlank();
        assertThat(md).contains("# Test Run Summary");
    }

    // -----------------------------------------------------------------------
    // Slack
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void slackJsonIsValid() throws Exception {
        final String slack = new SlackMessageBuilder().build(fixture);
        final JsonNode node = MAPPER.readTree(slack);
        assertThat(node).isNotNull();
        assertThat(node.isObject()).isTrue();
    }

    @Test(groups = {"reporting", "unit"})
    public void slackJsonHasBlocksArray() throws Exception {
        final JsonNode node = MAPPER.readTree(new SlackMessageBuilder().build(fixture));
        assertThat(node.get("blocks").isArray()).isTrue();
        assertThat(node.get("blocks").size()).isGreaterThanOrEqualTo(2);
    }

    @Test(groups = {"reporting", "unit"})
    public void slackJsonContainsSuiteNameAndFailedTest() {
        final String slack = new SlackMessageBuilder().build(fixture);
        assertThat(slack).contains("OmiinQA Regression Suite");
        // Fixture has a failure, so the context block lists the failed test.
        assertThat(slack).contains("checkoutFails");
    }

    // -----------------------------------------------------------------------
    // Teams
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void teamsJsonIsValid() throws Exception {
        final String teams = new TeamsMessageBuilder().build(fixture);
        final JsonNode node = MAPPER.readTree(teams);
        assertThat(node).isNotNull();
        assertThat(node.isObject()).isTrue();
    }

    @Test(groups = {"reporting", "unit"})
    public void teamsJsonHasMessageCardType() throws Exception {
        final JsonNode node = MAPPER.readTree(new TeamsMessageBuilder().build(fixture));
        assertThat(node.get("@type").asText()).isEqualTo("MessageCard");
        // Any failure should turn the card red.
        assertThat(node.get("themeColor").asText()).isEqualTo("FF0000");
    }

    @Test(groups = {"reporting", "unit"})
    public void teamsJsonContainsFacts() throws Exception {
        final String teams = new TeamsMessageBuilder().build(fixture);
        assertThat(teams).contains("facts");
        final JsonNode node = MAPPER.readTree(teams);
        assertThat(node.get("sections").get(0).get("facts").isArray()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Email
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void emailContainsHtmlDocument() {
        final String html = new EmailSummaryBuilder().buildHtml(fixture);
        assertThat(html).contains("<html").contains("</html>");
    }

    @Test(groups = {"reporting", "unit"})
    public void emailContainsSuiteNameAndTotals() {
        final String html = new EmailSummaryBuilder().buildHtml(fixture);
        assertThat(html).contains("OmiinQA Regression Suite");
        assertThat(html).contains("Passed: 2");
        assertThat(html).contains("Failed: 1");
    }

    // -----------------------------------------------------------------------
    // Facade
    // -----------------------------------------------------------------------

    @Test(groups = {"reporting", "unit"})
    public void facadeExposesEveryFormatAsNonBlankString() {
        final ReportExporterFacade facade = ReportExporterFacade.withDefaults();
        assertThat(facade.exportJson(fixture)).isNotBlank();
        assertThat(facade.exportCsv(fixture)).isNotBlank();
        assertThat(facade.exportXml(fixture)).isNotBlank();
        assertThat(facade.exportMarkdown(fixture)).isNotBlank();
        assertThat(facade.exportSlack(fixture)).isNotBlank();
        assertThat(facade.exportTeams(fixture)).isNotBlank();
        assertThat(facade.exportEmail(fixture)).isNotBlank();
    }

    @Test(groups = {"reporting", "unit"})
    public void facadeExportAllWritesExpectedFiles() throws Exception {
        final Path dir = Files.createTempDirectory("omiinqa-export-test");
        try {
            new ReportExporterFacade(dir.toString()).exportAll(fixture);
            assertThat(Files.exists(dir.resolve("test-run.json"))).isTrue();
            assertThat(Files.exists(dir.resolve("test-run.csv"))).isTrue();
            assertThat(Files.exists(dir.resolve("test-run.xml"))).isTrue();
            assertThat(Files.exists(dir.resolve("test-run.md"))).isTrue();
            // Spot-check the JSON file parses back.
            final JsonNode node = MAPPER.readTree(dir.resolve("test-run.json").toFile());
            assertThat(node.get("suiteName").asText()).isEqualTo("OmiinQA Regression Suite");
        } finally {
            cleanup(dir);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds a one-test {@link TestRunResult} for focused escaping/quoting assertions. */
    private static TestRunResult singleCaseRun(final TestCaseResult tc) {
        final List<TestCaseResult> cases = new ArrayList<>();
        cases.add(tc);
        return TestRunResult.builder()
                .suiteName("Single")
                .totalPassed("PASSED".equals(tc.getStatus()) ? 1 : 0)
                .totalFailed("FAILED".equals(tc.getStatus()) ? 1 : 0)
                .totalSkipped("SKIPPED".equals(tc.getStatus()) ? 1 : 0)
                .totalDurationMs(tc.getDurationMs())
                .testCases(cases)
                .environmentMetadata(new LinkedHashMap<>())
                .startedAt(0L)
                .finishedAt(tc.getDurationMs())
                .build();
    }

    /** Best-effort recursive delete of a temp directory created by a test. */
    private static void cleanup(final Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (final Exception ignored) {
                    // best effort
                }
            });
        } catch (final Exception ignored) {
            // best effort
        }
    }
}
