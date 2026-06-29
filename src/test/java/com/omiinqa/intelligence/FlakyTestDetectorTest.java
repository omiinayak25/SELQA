package com.omiinqa.intelligence;

import com.omiinqa.intelligence.FlakyTestDetector.FlakinessReport;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Offline unit tests for {@link FlakyTestDetector}.
 *
 * <p>All tests are purely statistical — no WebDriver, no browser, no network.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class FlakyTestDetectorTest {

    private FlakyTestDetector detector;

    @BeforeClass
    public void setup() {
        detector = new FlakyTestDetector(); // default threshold = 0.20
    }

    // =========================================================================
    // Score computation
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void allPassScoresZero() {
        final List<Boolean> results = Arrays.asList(true, true, true, true, true);
        assertEquals(detector.score(results), 0.0, 0.001,
                "All-pass history must score 0.0 (not flaky, just stable)");
    }

    @Test(groups = {"intelligence", "unit"})
    public void allFailScoresZero() {
        final List<Boolean> results = Arrays.asList(false, false, false, false);
        assertEquals(detector.score(results), 0.0, 0.001,
                "All-fail history must score 0.0 (broken, not flaky)");
    }

    @Test(groups = {"intelligence", "unit"})
    public void alternatingPassFailScoresOne() {
        // T F T F T F — 5 transitions out of 5 pairs → score = 1.0
        final List<Boolean> results = Arrays.asList(true, false, true, false, true, false);
        assertEquals(detector.score(results), 1.0, 0.001,
                "Perfectly alternating history must score 1.0");
    }

    @Test(groups = {"intelligence", "unit"})
    public void singleTransitionScoresCorrectly() {
        // P P P F F — 1 transition out of 4 pairs → score = 0.25
        final List<Boolean> results = Arrays.asList(true, true, true, false, false);
        assertEquals(detector.score(results), 0.25, 0.001);
    }

    @Test(groups = {"intelligence", "unit"})
    public void twoTransitionsInFiveRunsScoresCorrectly() {
        // P F F F P — 2 transitions out of 4 pairs → score = 0.50
        final List<Boolean> results = Arrays.asList(true, false, false, false, true);
        assertEquals(detector.score(results), 0.50, 0.001);
    }

    @Test(groups = {"intelligence", "unit"})
    public void nullResultsScoreZero() {
        assertEquals(detector.score(null), 0.0, 0.001);
    }

    @Test(groups = {"intelligence", "unit"})
    public void emptyResultsScoreZero() {
        assertEquals(detector.score(Collections.emptyList()), 0.0, 0.001);
    }

    @Test(groups = {"intelligence", "unit"})
    public void fewerThanMinRunsScoreZero() {
        // MIN_RUNS = 3; give only 2 results
        final List<Boolean> results = Arrays.asList(true, false);
        assertEquals(detector.score(results), 0.0, 0.001,
                "Fewer than MIN_RUNS results must score 0.0");
    }

    // =========================================================================
    // isFlaky threshold
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void scoreBelowDefaultThresholdIsNotFlaky() {
        // One isolated failure creates 2 transitions (pass→fail, fail→pass). Over a long
        // history that is a low transition rate: 2 transitions / 19 pairs ≈ 0.105 < 0.20.
        final List<Boolean> results = Arrays.asList(
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, false, true, true, true, true, true);
        assertFalse(detector.isFlaky(results),
                "Score below 0.20 must not be flagged as flaky");
    }

    @Test(groups = {"intelligence", "unit"})
    public void scoreAboveDefaultThresholdIsFlaky() {
        // Alternating: score = 1.0 > 0.20
        final List<Boolean> results = Arrays.asList(true, false, true, false, true);
        assertTrue(detector.isFlaky(results),
                "Highly alternating history must be flagged as flaky");
    }

    @Test(groups = {"intelligence", "unit"})
    public void customThresholdIsRespected() {
        final FlakyTestDetector strict = new FlakyTestDetector(0.10);
        // 1 transition in 4 pairs → score = 0.25 > 0.10
        final List<Boolean> results = Arrays.asList(true, true, false, true, true);
        assertTrue(strict.isFlaky(results), "Score 0.25 must exceed strict threshold 0.10");
    }

    @Test(groups = {"intelligence", "unit"})
    public void highThresholdAcceptsModerateFlakiness() {
        final FlakyTestDetector lenient = new FlakyTestDetector(0.60);
        // score = 0.50, threshold = 0.60 → not flagged
        final List<Boolean> results = Arrays.asList(true, false, false, false, true);
        assertFalse(lenient.isFlaky(results), "Score 0.50 must not exceed threshold 0.60");
    }

    // =========================================================================
    // analyseAll
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void analyseAllReturnsAllTests() {
        final Map<String, List<Boolean>> histories = new HashMap<>();
        histories.put("stableTest",  Arrays.asList(true, true, true, true));
        histories.put("flakyTest",   Arrays.asList(true, false, true, false, true));
        histories.put("brokenTest",  Arrays.asList(false, false, false, false));

        final Map<String, FlakinessReport> reports = detector.analyseAll(histories);
        assertEquals(reports.size(), 3, "Should return a report for every test");
        assertNotNull(reports.get("stableTest"));
        assertNotNull(reports.get("flakyTest"));
        assertNotNull(reports.get("brokenTest"));
    }

    @Test(groups = {"intelligence", "unit"})
    public void analyseAllFlagsOnlyFlakyTests() {
        final Map<String, List<Boolean>> histories = new HashMap<>();
        histories.put("stable", Arrays.asList(true, true, true, true, true));
        histories.put("flaky",  Arrays.asList(true, false, true, false, true));
        histories.put("broken", Arrays.asList(false, false, false, false, false));

        final Map<String, FlakinessReport> reports = detector.analyseAll(histories);

        assertFalse(reports.get("stable").isFlagged(), "Stable test must not be flagged");
        assertTrue(reports.get("flaky").isFlagged(),   "Flaky test must be flagged");
        assertFalse(reports.get("broken").isFlagged(), "Broken test must not be flagged as flaky");
    }

    @Test(groups = {"intelligence", "unit"})
    public void analyseAllReportIncludesRunCount() {
        final Map<String, List<Boolean>> histories = Map.of(
                "myTest", Arrays.asList(true, false, true, false, true, false));
        final Map<String, FlakinessReport> reports = detector.analyseAll(histories);
        assertEquals(reports.get("myTest").getRunCount(), 6);
    }

    @Test(groups = {"intelligence", "unit"})
    public void analyseAllNullInputReturnsEmpty() {
        assertTrue(detector.analyseAll(null).isEmpty());
    }

    @Test(groups = {"intelligence", "unit"})
    public void analyseAllEmptyInputReturnsEmpty() {
        assertTrue(detector.analyseAll(Collections.emptyMap()).isEmpty());
    }

    // =========================================================================
    // flaggedTests
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void flaggedTestsReturnsOnlyFlagged() {
        final Map<String, List<Boolean>> histories = new HashMap<>();
        histories.put("good",    Arrays.asList(true, true, true, true, true));
        histories.put("flaky1",  Arrays.asList(true, false, true, false, true));
        histories.put("flaky2",  Arrays.asList(false, true, false, true, false));

        final Map<String, FlakinessReport> flagged = detector.flaggedTests(histories);
        assertFalse(flagged.containsKey("good"),   "Stable test must not appear in flagged set");
        assertTrue(flagged.containsKey("flaky1"),  "flaky1 must appear in flagged set");
        assertTrue(flagged.containsKey("flaky2"),  "flaky2 must appear in flagged set");
    }

    // =========================================================================
    // FlakinessReport value object
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void flakinessReportToStringContainsEssentials() {
        final Map<String, List<Boolean>> histories = Map.of(
                "reportTest", Arrays.asList(true, false, true));
        final FlakinessReport report = detector.analyseAll(histories).get("reportTest");
        final String str = report.toString();
        assertTrue(str.contains("reportTest"), "toString must include test name");
        assertTrue(str.contains("score"),      "toString must include score label");
    }

    // =========================================================================
    // Constructor validation
    // =========================================================================

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = IllegalArgumentException.class)
    public void zeroThresholdIsRejected() {
        new FlakyTestDetector(0.0);
    }

    @Test(groups = {"intelligence", "unit"},
          expectedExceptions = IllegalArgumentException.class)
    public void negativeThresholdIsRejected() {
        new FlakyTestDetector(-0.1);
    }

    @Test(groups = {"intelligence", "unit"})
    public void thresholdOneIsAccepted() {
        final FlakyTestDetector d = new FlakyTestDetector(1.0);
        assertEquals(d.getThreshold(), 1.0, 0.001);
    }
}
