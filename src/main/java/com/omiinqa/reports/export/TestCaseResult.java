package com.omiinqa.reports.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable value object representing the result of a single test case execution.
 *
 * <p>Instances are produced by {@link ResultsAggregator} as each TestNG lifecycle
 * callback fires, then collected into a {@link TestRunResult} for the entire suite.
 * All fields are set at construction time; the object is effectively immutable when
 * used via the Lombok-generated builder.</p>
 *
 * <p>Typical lifecycle:
 * <pre>
 *   TestCaseResult r = TestCaseResult.builder()
 *       .name("loginTest")
 *       .className("LoginTest")
 *       .status("PASSED")
 *       .durationMs(1200L)
 *       .build();
 * </pre>
 * </p>
 *
 * @see TestRunResult
 * @see ResultsAggregator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {

    /**
     * The simple method name of the test (e.g., {@code "loginTest"}).
     */
    private String name;

    /**
     * The simple class name containing the test (e.g., {@code "LoginTest"}).
     */
    private String className;

    /**
     * Execution outcome: one of {@code "PASSED"}, {@code "FAILED"}, or {@code "SKIPPED"}.
     */
    private String status;

    /**
     * Wall-clock execution time in milliseconds.
     */
    private long durationMs;

    /**
     * Human-readable error message when {@link #status} is {@code "FAILED"};
     * {@code null} for passing or skipped tests.
     */
    private String errorMessage;
}
